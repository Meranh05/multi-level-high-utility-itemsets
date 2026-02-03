package miner;

import model.Taxonomy;
import model.Transaction;
import model.TransactionItem;

import java.util.*;

/**
 * Basic HUIM at level 0 (non-generalized items).
 * Implements a utility-list based depth-first search (HUI-Miner style) with TWU pruning.
 *
 * Output format: {A,B} -> Utility = 123 (level=0)
 */
public class BasicHUIMiner {

    /** Mines HUIs (level 0 only). */
    public List<String> mine(List<Transaction> db, int minUtil) {
        return mine(db, minUtil, null);
    }

    /**
     * Optional taxonomy is accepted only to reuse shared preprocessing (TU/TWU),
     * but this miner will ignore generalized items and mine only leaf items.
     */
    public List<String> mine(List<Transaction> db, int minUtil, Taxonomy taxonomy) {
        if (db == null || db.isEmpty()) return Collections.emptyList();

        // --- 1) Compute TU per transaction (sum utility of leaf items) and TWU per leaf item
        Map<Integer, Integer> tuByTid = new HashMap<>();
        Map<String, Integer> twu = new HashMap<>();
        Set<String> leafItems = new HashSet<>();

        for (Transaction t : db) {
            int tu = 0;
            for (TransactionItem ti : t.getItems()) {
                leafItems.add(ti.getItem().getName());
                tu += ti.getUtility();
            }
            tuByTid.put(t.getTid(), tu);

            // update TWU for each distinct item in this transaction
            Set<String> seen = new HashSet<>();
            for (TransactionItem ti : t.getItems()) {
                String it = ti.getItem().getName();
                if (seen.add(it)) {
                    twu.put(it, twu.getOrDefault(it, 0) + tu);
                }
            }
        }

        // --- 2) Keep promising items by TWU
        List<String> items = new ArrayList<>();
        for (String it : leafItems) {
            if (twu.getOrDefault(it, 0) >= minUtil) items.add(it);
        }
        items.sort(Comparator.comparingInt(a -> twu.getOrDefault(a, 0)));

        // --- 3) Build utility lists for each 1-itemset
        Map<String, UtilityList> ulByItem = new HashMap<>();
        for (String it : items) ulByItem.put(it, new UtilityList(it));

        // For each transaction, compute utilities of items at level 0 then assign rutil by order
        for (Transaction t : db) {
            Map<String, Integer> utilInT = new HashMap<>();
            for (TransactionItem ti : t.getItems()) {
                String it = ti.getItem().getName();
                if (ulByItem.containsKey(it)) utilInT.put(it, utilInT.getOrDefault(it, 0) + ti.getUtility());
            }
            if (utilInT.isEmpty()) continue;

            // remaining utility within the ordered list
            int tid = t.getTid();
            int suffixSum = 0;
            // traverse in reverse order to compute suffix sums
            for (int i = items.size() - 1; i >= 0; i--) {
                String it = items.get(i);
                int iu = utilInT.getOrDefault(it, 0);
                if (iu > 0) {
                    UtilityList ul = ulByItem.get(it);
                    ul.addElement(new ULElement(tid, iu, suffixSum));
                }
                suffixSum += iu;
            }
        }

        // --- 4) DFS mining
        List<String> out = new ArrayList<>();
        List<UtilityList> ulList = new ArrayList<>();
        for (String it : items) {
            UtilityList ul = ulByItem.get(it);
            if (!ul.elements.isEmpty()) ulList.add(ul);
        }

        dfs(new ArrayList<>(), null, ulList, 0, minUtil, out);

        return out;
    }

    // =============== Utility-List machinery (HUI-Miner style) ===============

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
                out.add(formatItemset(newPrefix) + " -> Utility = " + sumI + " (level=" + level + ")");
            }

            // prune if upper bound is below threshold
            if (sumIR < threshold) continue;

            // build new extensions list (items after current)
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
                // Typical utility-list join:
                // iutil = p.iutil + x.iutil ; rutil = x.rutil
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

    private String formatItemset(List<String> items) {
        return "{" + String.join(", ", items) + "}";
    }

    // --------- inner structs ---------

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
        final String item; // for debugging; actual item name for 1-item UL
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
