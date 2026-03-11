// Logic của chương trình
import java.util.*;
import java.util.List;

public class MLHUIMiner {
    public interface LogSink {
        void log(String s);
    }

    public static class Result {
        public final long timeMs;
        public final long memoryKb;

        public Result(long timeMs, long memoryKb) {
            this.timeMs = timeMs;
            this.memoryKb = memoryKb;
        }
    }

    // ===== DỮ LIỆU CẤU TRÚC =====
    private List<Transaction> database = new ArrayList<>();
    private Map<String, Integer> extUtil = new HashMap<>();
    private Map<String, List<String>> taxonomy = new HashMap<>();
    private Map<String, String> childToParent = new HashMap<>();
    private Map<String, List<String>> leafDescendants = new HashMap<>();
    private Map<String, Integer> itemLevels = new HashMap<>();
    private List<String> foundHUIs = new ArrayList<>();
    private Map<String, Integer> twuGlobal = new HashMap<>();
    private Map<String, Map<String, Integer>> EUCS = new HashMap<>();

    private String transactionsText = "";
    private String externalUtilityText = "";
    private String taxonomyText = "";
    private LogSink logSink = s -> { };

    public Result run(String transactionsText, String externalUtilityText, String taxonomyText,
                      int minUtil, LogSink logSink) {
        this.transactionsText = transactionsText == null ? "" : transactionsText;
        this.externalUtilityText = externalUtilityText == null ? "" : externalUtilityText;
        this.taxonomyText = taxonomyText == null ? "" : taxonomyText;
        this.logSink = logSink == null ? s -> { } : logSink;
        // Làm sạch dữ liệu trước khi chạy
        database.clear();
        extUtil.clear();
        taxonomy.clear();
        childToParent.clear();
        leafDescendants.clear();
        itemLevels.clear();
        EUCS.clear();
        twuGlobal.clear();
        foundHUIs.clear();
        log(">>> ML-HUI START <<<");
        long startTime = System.nanoTime();
        System.gc();
        parseExternalUtility();
        parseTaxonomy();
        parseTransactions();
        log("Bước 1: I ← tập hợp các mục trong D");
        Set<String> I = new HashSet<>();
        for (Transaction t : database) I.addAll(t.items.keySet());
        log(" I: " + I);
        log("Bước 2: GI ← tập hợp các mục tổng quát trong I");
        Set<String> GT = new HashSet<>(taxonomy.keySet());
        log(" GI: " + GT);
        computeLevelsAndDescendants();
        log("Bước 3-4: Tính TWU của các mặt hàng trong I và GI");
        twuGlobal = computeTWU();
        for (String item : twuGlobal.keySet()) {
            log(" TWU(" + item + ") = " + twuGlobal.get(item));
        }
        Map<Integer, Integer> levelThr = new HashMap<>();
        int maxLevel = 0;
        for (int l : itemLevels.values()) maxLevel = Math.max(maxLevel, l);
        for (int l = 0; l <= maxLevel; l++) levelThr.put(l, minUtil);
        log("Bước 5-6: Lọc các mục (I*) và các mục tổng quát (GT*) dựa trên ngưỡng TWU và cấp độ");
        Map<Integer, List<String>> itemsByLevel = new HashMap<>();
        for (String item : twuGlobal.keySet()) {
            int level = itemLevels.getOrDefault(item, 0);
            if (twuGlobal.get(item) >= levelThr.get(level)) {
                itemsByLevel.computeIfAbsent(level, k -> new ArrayList<>()).add(item);
                log("[KEEP L" + level + "] " + item + " TWU=" + twuGlobal.get(item));
            }
        }
        log("Bước 7: Xây dựng danh sách hữu ích(utility list) ban đầu và cấu trúc EUCS");
        // ===== BƯỚC 8: ĐỆ QUY TẠO TỔ HỢP VÀ KHAI PHÁ =====
        log("Bước 8: Tạo tổ hợp đệ quy và lựa chọn");
       
        List<Integer> sortedLevels = new ArrayList<>(itemsByLevel.keySet());
        Collections.sort(sortedLevels);
        for (int level : sortedLevels) {
            log("\n--- DFS LEVEL " + level + " ---");
            EUCS.clear();
           
            // Xây dựng danh sách cơ sở cho tầng này
            List<UtilityList> ULs = buildUtilityLists(itemsByLevel.get(level));
            logUtilityLists(ULs);
            logEUCS();
           
            // Bắt đầu đệ quy tạo tổ hợp
            mine(new ArrayList<>(), ULs, levelThr.get(level), level);
            // Đưa kết quả ra sau khi đã đệ quy xong toàn bộ tổ hợp của level đó
            log("\n--- KẾT QUẢ TẠI LEVEL " + level + " ---");
            if (foundHUIs.isEmpty()) {
                log("(Không tìm thấy tập mục nào thỏa mãn)");
            } else {
                for (int i = 0; i < foundHUIs.size(); i++) {
                    log((i + 1) + ") " + foundHUIs.get(i));
                }
            }
            foundHUIs.clear(); // Xóa cho level tiếp theo
        }
        long timeMs = (System.nanoTime() - startTime) / 1_000_000;
        long memKb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024;
       
        // Trả về kết quả cho hàm gọi (thường là main sẽ in Performance)
        return new Result(timeMs, memKb);
    }
    private void logUtilityLists(List<UtilityList> ULs) {
        for (UtilityList ul : ULs) log(" Utility(" + ul.item + ")= " + ul.sumIutil);
    }
    private void logEUCS() {
        log(" Cấu trúc Đồng xuất hiện Hữu ích (EUCS) ");
        for (String a : EUCS.keySet()) {
            for (String b : EUCS.get(a).keySet()) {
                log(" TU(" + a + "," + b + ") = " + EUCS.get(a).get(b));
            }
        }
    }
    private void computeLevelsAndDescendants() {
        Set<String> allPrimitives = new HashSet<>();
        for (Transaction t : database) allPrimitives.addAll(t.items.keySet());
        allPrimitives.addAll(extUtil.keySet());
        for (String i : allPrimitives) {
            if (!taxonomy.containsKey(i)) {
                itemLevels.put(i, 0);
                leafDescendants.put(i, Collections.singletonList(i));
            }
        }
        for (String g : taxonomy.keySet()) {
            computeLevel(g);
            computeDescendants(g);
        }
    }
    private int computeLevel(String node) {
        if (itemLevels.containsKey(node)) return itemLevels.get(node);
        if (!taxonomy.containsKey(node)) return 0;
        int minChildLevel = Integer.MAX_VALUE;
        for (String child : taxonomy.get(node)) minChildLevel = Math.min(minChildLevel, computeLevel(child));
        int level = 1 + (minChildLevel == Integer.MAX_VALUE ? -1 : minChildLevel);
        itemLevels.put(node, level);
        return level;
    }
    private List<String> computeDescendants(String node) {
        if (leafDescendants.containsKey(node)) return leafDescendants.get(node);
        if (!taxonomy.containsKey(node)) {
            List<String> list = Collections.singletonList(node);
            leafDescendants.put(node, list);
            return list;
        }
        List<String> desc = new ArrayList<>();
        for (String child : taxonomy.get(node)) desc.addAll(computeDescendants(child));
        leafDescendants.put(node, desc);
        return desc;
    }
    // ===== HÀM MINE ĐỆ QUY TẠO TỔ HỢP =====
    private void mine(List<String> prefix, List<UtilityList> ULs, int minUtil, int level) {
        for (int i = 0; i < ULs.size(); i++) {
            UtilityList X = ULs.get(i);
            List<String> newPrefix = new ArrayList<>(prefix);
            newPrefix.add(X.item);
            // 1. So sánh với ngưỡng minUtil để chọn HUI
            if (X.sumIutil >= minUtil) {
                foundHUIs.add(newPrefix.toString() + " = " + X.sumIutil);
            }
            // 2. Cắt tỉa nhánh dựa trên Upper-bound (iutil + rutil)
            if (X.sumIutil + X.sumRutil < minUtil) continue;
            // 3. Tạo các tổ hợp lớn hơn (n+1)
            List<UtilityList> exULs = new ArrayList<>();
            for (int j = i + 1; j < ULs.size(); j++) {
                UtilityList Y = ULs.get(j);
               
                // KIỂM TRA EUCS TRƯỚC KHI NỐI (JOIN)
                Map<String, Integer> subMap = EUCS.get(X.item);
                Integer eucsVal = (subMap != null) ? subMap.get(Y.item) : null;
                if (eucsVal != null) {
                    if (eucsVal < minUtil) {
                        log("Loại " + X.item + " " + Y.item + " vì (TU =" + eucsVal + " < minUtil)");
                        continue;
                    }
                } else {
                    // Nếu không có trong EUCS (như Water Coke), im lặng bỏ qua vì chúng không bao giờ đi cùng nhau
                    continue;
                }
                // Thực hiện nối để tạo tổ hợp mới
                UtilityList XY = construct(X, Y);
                if (!XY.nodes.isEmpty()) exULs.add(XY);
            }
            // Đệ quy tiếp tục vào sâu hơn
            mine(newPrefix, exULs, minUtil, level);
        }
    }
    private List<UtilityList> buildUtilityLists(List<String> items) {
        items.sort(Comparator.comparingInt(a -> twuGlobal.getOrDefault(a, 0)));
        List<UtilityList> list = new ArrayList<>();
        Map<String, UtilityList> map = new HashMap<>();
        for (String i : items) {
            UtilityList ul = new UtilityList(i);
            list.add(ul);
            map.put(i, ul);
        }
        for (int tid = 0; tid < database.size(); tid++) {
            Transaction t = database.get(tid);
            List<String> present = new ArrayList<>();
            for (String i : items) if (utilityOf(i, t) > 0) present.add(i);
           
            present.sort(Comparator.comparingInt(a -> twuGlobal.getOrDefault(a, 0)));
            for (int i = 0; i < present.size(); i++) {
                String item = present.get(i);
                int iutil = utilityOf(item, t);
                int rutil = 0;
                for (int j = i + 1; j < present.size(); j++) rutil += utilityOf(present.get(j), t);
                map.get(item).add(new ULNode(tid, iutil, rutil));
               
                for (int j = i + 1; j < present.size(); j++) {
                    String b = present.get(j);
                    EUCS.computeIfAbsent(item, k -> new HashMap<>());
                    EUCS.get(item).put(b, EUCS.get(item).getOrDefault(b, 0) + t.tu);
                }
            }
        }
        return list;
    }
    private UtilityList construct(UtilityList X, UtilityList Y) {
        UtilityList XY = new UtilityList(Y.item);
        int i = 0, j = 0;
        while (i < X.nodes.size() && j < Y.nodes.size()) {
            ULNode nx = X.nodes.get(i), ny = Y.nodes.get(j);
            if (nx.tid == ny.tid) {
                // Tính utility của tổ hợp mới tại giao dịch này
                XY.add(new ULNode(nx.tid, nx.iutil + utilityOf(Y.item, database.get(nx.tid)), ny.rutil));
                i++; j++;
            } else if (nx.tid < ny.tid) i++;
            else j++;
        }
        return XY;
    }
    private Map<String, Integer> computeTWU() {
        Map<String, Integer> map = new HashMap<>();
        for (Transaction t : database) {
            Set<String> added = new HashSet<>();
            for (String i : t.items.keySet()) {
                String current = i;
                while (current != null) {
                    if (added.add(current)) map.put(current, map.getOrDefault(current, 0) + t.tu);
                    current = childToParent.get(current);
                }
            }
        }
        return map;
    }
    private int utilityOf(String item, Transaction t) {
        Integer level = itemLevels.get(item);
        if (level == null || level == 0) {
            return t.items.containsKey(item) ? t.items.get(item) * extUtil.getOrDefault(item, 0) : 0;
        } else {
            int s = 0;
            for (String leaf : leafDescendants.getOrDefault(item, Collections.emptyList())) {
                if (t.items.containsKey(leaf)) s += t.items.get(leaf) * extUtil.getOrDefault(leaf, 0);
            }
            return s;
        }
    }
    private void parseTransactions() {
        for (String line : transactionsText.split("\n")) {
            if (line.trim().isEmpty()) continue;
            String[] p = line.split(":");
            Transaction t = new Transaction();
            for (String part : p[1].split(",")) {
                String[] s = part.trim().split(" ");
                t.add(s[0], Integer.parseInt(s[1]));
            }
            database.add(t);
        }
    }
    private void parseExternalUtility() {
        for (String l : externalUtilityText.split("\n")) {
            if (l.trim().isEmpty()) continue;
            String[] p = l.split(":");
            extUtil.put(p[0].trim(), Integer.parseInt(p[1].trim()));
        }
    }
    private void parseTaxonomy() {
        for (String l : taxonomyText.split("\n")) {
            if (l.trim().isEmpty()) continue;
            String[] p = l.split(":");
            String parent = p[0].trim();
            for (String c : p[1].split(",")) {
                String child = c.trim();
                taxonomy.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
                childToParent.put(child, parent);
            }
        }
    }
    private void log(String s) { logSink.log(s); }
    class Transaction {
        Map<String, Integer> items = new HashMap<>();
        int tu = 0;
        void add(String name, int qty) {
            items.put(name, qty);
            tu += qty * extUtil.getOrDefault(name, 0);
        }
    }
    class ULNode {
        int tid, iutil, rutil;
        ULNode(int t, int i, int r) { tid = t; iutil = i; rutil = r; }
    }
    class UtilityList {
        String item;
        List<ULNode> nodes = new ArrayList<>();
        int sumIutil = 0, sumRutil = 0;
        UtilityList(String item) { this.item = item; }
        void add(ULNode n) {
            nodes.add(n);
            sumIutil += n.iutil;
            sumRutil += n.rutil;
        }
    }
}