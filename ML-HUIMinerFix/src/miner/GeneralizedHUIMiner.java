package miner;

import model.Taxonomy;
import model.Transaction;
import model.TransactionItem;

import java.util.*;

/**
 * Multiple-Level High-Utility Itemset Miner (ML-HUI Miner) for:
 *  - HUIs (level 0, leaf items)
 *  - GHUIs (generalized items) at higher abstraction levels
 *
 * Key constraints implemented (per paper):
 *  - Single-phase mining using utility-lists + DFS
 *  - TWU-based pruning (transaction-weighted utilization)
 *  - Per-level minimum utility thresholds: thr(level) = alpha(level) * minUtil
 *  - Only generate itemsets whose items share the SAME taxonomy level
 *
 * Output format:
 *   {A,B} -> Utility = 123 (level=1, thr=30)
 * 
 * === alphaStep ===
 *alphaStep được dùng để tăng dần ngưỡng utility theo cấp độ taxonomy
 * nhằm tránh việc các item tổng quát luôn vượt ngưỡng do hiệu ứng cộng dồn. 
 *Với αStep = 0.5, mô hình đạt được sự cân bằng giữa việc khai phá tri thức cấp cao
 *và loại bỏ các pattern tổng quát nhưng kém giá trị.
 */
public class GeneralizedHUIMiner {

    private final Taxonomy taxonomy;

    /** alphaStep defines alpha(level)= 1 + alphaStep*level (monotone increasing). */
    private final double alphaStep;

    public GeneralizedHUIMiner(Taxonomy taxonomy) {
        this(taxonomy, 0.5); // default: level1=1.5*minutil, level2=2.0*minutil, ...
    }

    public GeneralizedHUIMiner(Taxonomy taxonomy, double alphaStep) {
        this.taxonomy = taxonomy;
        this.alphaStep = Math.max(0.0, alphaStep);
    }

    public List<String> mine(List<Transaction> db, int minUtil) {
        if (db == null || db.isEmpty()) return Collections.emptyList();

        // ------------------ PREP: leaf items, TU, TWU ------------------
        Set<String> leafItems = collectLeafItems(db);

        // TU per transaction (sum of leaf utilities) - used for TWU bounds
        Map<Integer, Integer> tuByTid = new HashMap<>();
        for (Transaction t : db) {
            int tu = 0;
            for (TransactionItem ti : t.getItems()) tu += ti.getUtility();
            tuByTid.put(t.getTid(), tu);
        }

        // Build universe of nodes to consider: leaf items + generalized nodes on their ancestor chains
        Set<String> allNodes = new HashSet<>(leafItems);
        for (String it : leafItems) allNodes.addAll(taxonomy.getAncestors(it));

        // Level for each node
        Map<String, Integer> levelOf = new HashMap<>();
        int maxLevel = 0;
        for (String n : allNodes) {
            int lv = taxonomy.getLevel(n, leafItems);
            if (lv >= Integer.MAX_VALUE / 8) continue;
            levelOf.put(n, lv);
            maxLevel = Math.max(maxLevel, lv);
        }

        // TWU for each node (leaf or generalized)
        Map<String, Integer> twu = new HashMap<>();
        for (Transaction t : db) {
            int tid = t.getTid();
            int tu = tuByTid.getOrDefault(tid, 0);

            // distinct leaf items in transaction
            Set<String> seenLeaf = new HashSet<>();
            for (TransactionItem ti : t.getItems()) seenLeaf.add(ti.getItem().getName());

            // nodes present in transaction: each leaf + its ancestors
            Set<String> present = new HashSet<>();
            for (String leaf : seenLeaf) {
                present.add(leaf);
                present.addAll(taxonomy.getAncestors(leaf));
            }
            for (String n : present) {
                if (levelOf.containsKey(n)) {
                    twu.put(n, twu.getOrDefault(n, 0) + tu);
                }
            }
        }

        // ------------------ FILTER BY PER-LEVEL THRESHOLD ------------------
        Map<Integer, List<String>> levelItems = new HashMap<>();
        for (String n : allNodes) {
            Integer lv = levelOf.get(n);
            if (lv == null) continue;
            int thr = threshold(minUtil, lv);

            if (twu.getOrDefault(n, 0) >= thr) {
                levelItems.computeIfAbsent(lv, k -> new ArrayList<>()).add(n);
            }
        }

        // Sort items within each level by TWU to get a deterministic order (helps rutil)
        for (List<String> list : levelItems.values()) {
            list.sort(Comparator.comparingInt(a -> twu.getOrDefault(a, 0)));
        }

        // ------------------ BUILD UTILITY LISTS PER LEVEL ------------------
        Map<Integer, Map<String, UtilityList>> ulByLevel = new HashMap<>();
        for (Map.Entry<Integer, List<String>> e : levelItems.entrySet()) {
            int lv = e.getKey();
            Map<String, UtilityList> map = new HashMap<>();
            for (String it : e.getValue()) map.put(it, new UtilityList(it));
            ulByLevel.put(lv, map);
        }

        // For each transaction: compute utility per node (by level), then fill utility-lists with rutil
        for (Transaction t : db) {
            int tid = t.getTid();

            // leaf utility map (aggregated if repeated)
            Map<String, Integer> leafUtil = new HashMap<>();
            for (TransactionItem ti : t.getItems()) {
                String leaf = ti.getItem().getName();
                if (leafItems.contains(leaf)) {
                    leafUtil.put(leaf, leafUtil.getOrDefault(leaf, 0) + ti.getUtility());
                }
            }
            if (leafUtil.isEmpty()) continue;

            // compute utilities for all nodes present in this transaction
            Map<String, Integer> nodeUtil = new HashMap<>(leafUtil);
            for (String leaf : leafUtil.keySet()) {
                int u = leafUtil.get(leaf);
                for (String anc : taxonomy.getAncestors(leaf)) {
                    nodeUtil.put(anc, nodeUtil.getOrDefault(anc, 0) + u);
                }
            }

            // For each level separately, compute suffix sums by the level's item order
            for (Map.Entry<Integer, Map<String, UtilityList>> e : ulByLevel.entrySet()) {
                int lv = e.getKey();
                List<String> order = levelItems.getOrDefault(lv, Collections.emptyList());
                if (order.isEmpty()) continue;

                // collect utility-in-transaction for this level
                Map<String, Integer> utilInT = new HashMap<>();
                for (String it : order) {
                    int iu = nodeUtil.getOrDefault(it, 0);
                    if (iu > 0) utilInT.put(it, iu);
                }
                if (utilInT.isEmpty()) continue;

                int suffixSum = 0;
                for (int i = order.size() - 1; i >= 0; i--) {
                    String it = order.get(i);
                    int iu = utilInT.getOrDefault(it, 0);
                    if (iu > 0) {
                        ulByLevel.get(lv).get(it).addElement(new ULElement(tid, iu, suffixSum));
                    }
                    suffixSum += iu;
                }
            }
        }

        // ------------------ DFS MINING (SINGLE-PHASE, BY LEVEL) ------------------
        List<String> out = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> e : levelItems.entrySet()) {
            int lv = e.getKey();
            int thr = threshold(minUtil, lv);

            List<UtilityList> start = new ArrayList<>();
            for (String it : e.getValue()) {
                UtilityList ul = ulByLevel.get(lv).get(it);
                if (!ul.elements.isEmpty()) start.add(ul);
            }

            dfs(new ArrayList<>(), null, start, lv, thr, out);
        }

        // optional: sort results by level then utility desc for nicer UI
        out.sort((a, b) -> {
            int la = extractLevel(a);
            int lb = extractLevel(b);
            if (la != lb) return Integer.compare(la, lb);
            int ua = extractUtility(a);
            int ub = extractUtility(b);
            return Integer.compare(ub, ua);
        });

        return out;
    }

    // ------------------ DFS & JOIN ------------------

    private void dfs(List<String> prefixItems,
                     UtilityList prefixUL,
                     List<UtilityList> extensions,
                     int level,
                     int threshold,
                     List<String> out) {

        for (int idx = 0; idx < extensions.size(); idx++) {
            UtilityList xUL = extensions.get(idx);

            List<String> newPrefix = new ArrayList<>(prefixItems);
            newPrefix.add(xUL.item);

            UtilityList pxUL = (prefixUL == null) ? xUL : join(prefixUL, xUL);
            if (pxUL.elements.isEmpty()) continue;

            int sumI = pxUL.sumIutil();
            int sumIR = sumI + pxUL.sumRutil();

            if (sumI >= threshold) {
                out.add(formatItemset(newPrefix) + " -> Utility = " + sumI +
                        " (level=" + level + ", thr=" + threshold + ")");
            }
            if (sumIR < threshold) continue; // prune

            List<UtilityList> newExt = new ArrayList<>();
            for (int j = idx + 1; j < extensions.size(); j++) newExt.add(extensions.get(j));

            dfs(newPrefix, pxUL, newExt, level, threshold, out);
        }
    }

    private UtilityList join(UtilityList p, UtilityList x) {
        UtilityList out = new UtilityList(p.item + "," + x.item);

        int i = 0, j = 0;
        while (i < p.elements.size() && j < x.elements.size()) {
            ULElement pe = p.elements.get(i);
            ULElement xe = x.elements.get(j);
            if (pe.tid == xe.tid) {
                out.addElement(new ULElement(pe.tid, pe.iutil + xe.iutil, xe.rutil));
                i++; j++;
            } else if (pe.tid < xe.tid) {
                i++;
            } else {
                j++;
            }
        }
        return out;
    }

    // ------------------ helpers ------------------

    private Set<String> collectLeafItems(List<Transaction> db) {
        Set<String> leaf = new HashSet<>();
        for (Transaction t : db) {
            for (TransactionItem ti : t.getItems()) leaf.add(ti.getItem().getName());
        }
        return leaf;
    }

    /** thr(level) = alpha(level) * minUtil ; alpha(level)=1+alphaStep*level (monotone). */
    private int threshold(int minUtil, int level) {
        double alpha = 1.0 + alphaStep * level;
        return (int) Math.ceil(alpha * minUtil);
    }

    private String formatItemset(List<String> items) {
        return "{" + String.join(", ", items) + "}";
    }

    private int extractLevel(String s) {
        int idx = s.indexOf("level=");
        if (idx < 0) return 0;
        int end = s.indexOf(",", idx);
        if (end < 0) end = s.indexOf(")", idx);
        if (end < 0) return 0;
        try { return Integer.parseInt(s.substring(idx + 6, end).trim()); }
        catch (Exception ex) { return 0; }
    }

    private int extractUtility(String s) {
        int idx = s.indexOf("Utility =");
        if (idx < 0) return 0;
        int end = s.indexOf("(", idx);
        if (end < 0) end = s.length();
        try { return Integer.parseInt(s.substring(idx + 9, end).trim()); }
        catch (Exception ex) { return 0; }
    }

    // ------------------ inner structs ------------------

    private static class ULElement {
        final int tid;
        final int iutil;
        final int rutil;

        ULElement(int tid, int iutil, int rutil) {
            this.tid = tid;
            this.iutil = iutil;
            this.rutil = rutil;
        }
    }

    private static class UtilityList {
        final String item;
        final List<ULElement> elements = new ArrayList<>();

        UtilityList(String item) {
            this.item = item;
        }

        void addElement(ULElement e) {
            elements.add(e);
        }

        int sumIutil() {
            int s = 0;
            for (ULElement e : elements) s += e.iutil;
            return s;
        }

        int sumRutil() {
            int s = 0;
            for (ULElement e : elements) s += e.rutil;
            return s;
        }
    }
}
