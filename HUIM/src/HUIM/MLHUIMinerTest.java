package example;

import java.util.*;

public class MLHUIMinerTest {

    // 1. Cấu trúc dữ liệu toàn cục
    static Map<String, Integer> externalUtility = new HashMap<>(); // Bảng 2: Lợi nhuận đơn vị
    static Map<String, List<String>> taxonomy = new HashMap<>();   // Hình 1: Phân loại học
    static List<Transaction> database = new ArrayList<>();         // Bảng 1: Dữ liệu giao dịch

    // Lớp đại diện cho một giao dịch
    static class Transaction {
        String id;
        Map<String, Integer> items = new HashMap<>(); // Tên món -> Số lượng (Internal Utility)

        public Transaction(String id) {
            this.id = id;
        }

        public void addItem(String name, int quantity) {
            items.put(name, quantity);
        }
    }

    public static void main(String[] args) {
        // --- BƯỚC 1: KHỞI TẠO DỮ LIỆU TỪ BÀI BÁO ---

        // Setup Bảng 2: External Utility
        externalUtility.put("Water", 1);
        externalUtility.put("Coke", 5);
        externalUtility.put("Bread", 1);
        externalUtility.put("Pasta", 2);
        externalUtility.put("Steak", 10);

        // Setup Hình 1: Taxonomy
        // Beverage gồm Coke, Water
        taxonomy.put("Beverage", Arrays.asList("Coke", "Water"));
        // Food gồm Bread, Pasta, Steak
        taxonomy.put("Food", Arrays.asList("Bread", "Pasta", "Steak"));

        // Setup Bảng 1: Transactional Dataset
        Transaction t1 = new Transaction("TId1");
        t1.addItem("Coke", 2); t1.addItem("Bread", 2); t1.addItem("Steak", 1);

        Transaction t2 = new Transaction("TId2");
        t2.addItem("Water", 3); t2.addItem("Pasta", 2); t2.addItem("Steak", 1);

        Transaction t3 = new Transaction("TId3");
        t3.addItem("Water", 2); t3.addItem("Bread", 2);

        Transaction t4 = new Transaction("TId4");
        t4.addItem("Coke", 1); t4.addItem("Bread", 2);

        database.addAll(Arrays.asList(t1, t2, t3, t4));

        // --- BƯỚC 2: CHẠY TEST KIỂM TRA KẾT QUẢ ---

        System.out.println("=== TEST 1: KIEM TRA HUI (Muc san pham) ===");
        int minUtilHUI = 17; // Ngưỡng từ Bảng 3
        
        // Kiểm tra tập {Steak, Coke, Bread}
        List<String> huiCandidate = Arrays.asList("Steak", "Coke", "Bread");
        int util = calculateItemsetUtility(huiCandidate);
        printResult(huiCandidate.toString(), util, minUtilHUI);

        // Kiểm tra tập {Coke, Bread}
        List<String> huiCandidate2 = Arrays.asList("Coke", "Bread");
        int util2 = calculateItemsetUtility(huiCandidate2);
        printResult(huiCandidate2.toString(), util2, minUtilHUI);


        System.out.println("\n=== TEST 2: KIEM TRA GHUI (Muc tong quat) ===");
        int minUtilGHUI = 30; // Ngưỡng từ Bảng 4

        // Kiểm tra tập {Food, Beverage} - Đây là ví dụ khó nhất cần giải thích
        List<String> ghuiCandidate = Arrays.asList("Food", "Beverage");
        int util3 = calculateItemsetUtility(ghuiCandidate);
        printResult(ghuiCandidate.toString(), util3, minUtilGHUI);
    }

    // --- BƯỚC 3: LOGIC TÍNH TOÁN (CORE ALGORITHM) ---

    // Tính tổng lợi nhuận của một Itemset trên toàn bộ Database
    public static int calculateItemsetUtility(List<String> itemset) {
        int totalUtility = 0;
        for (Transaction t : database) {
            int transactionUtility = 0;
            boolean allPresent = true;

            // Kiểm tra xem giao dịch này có chứa TOÀN BỘ item trong itemset không
            // Lưu ý: Với item tổng quát (ví dụ Food), "chứa" nghĩa là chứa ít nhất 1 con của nó
            for (String item : itemset) {
                if (getUtilityOfSingleItemInTransaction(item, t) == 0) {
                    allPresent = false;
                    break;
                }
            }

            // Nếu giao dịch chứa đủ bộ, cộng dồn lợi nhuận
            if (allPresent) {
                for (String item : itemset) {
                    transactionUtility += getUtilityOfSingleItemInTransaction(item, t);
                }
            }
            totalUtility += transactionUtility;
        }
        return totalUtility;
    }

    // Hàm đệ quy tính lợi nhuận cho item (Xử lý cả item thường và item tổng quát)
    // Dựa trên định nghĩa "Utility of a generalized item"
    public static int getUtilityOfSingleItemInTransaction(String item, Transaction t) {
        // Trường hợp 1: Item là nhóm hàng (Food, Beverage)
        if (taxonomy.containsKey(item)) {
            int groupUtility = 0;
            List<String> children = taxonomy.get(item);
            for (String child : children) {
                // Cộng dồn lợi nhuận của tất cả con có trong giao dịch
                groupUtility += getUtilityOfSingleItemInTransaction(child, t);
            }
            return groupUtility;
        }
        
        // Trường hợp 2: Item là sản phẩm cụ thể (Coke, Steak...)
        // Công thức: u(i, t_j) = eu(i) * iu(i, t_j)
        if (t.items.containsKey(item)) {
            int quantity = t.items.get(item);
            int price = externalUtility.getOrDefault(item, 0);
            return quantity * price;
        }

        return 0; // Không xuất hiện trong giao dịch
    }

    public static void printResult(String name, int util, int threshold) {
        String status = (util > threshold) ? "DAT CHUAN (High-Utility)" : "KHONG DAT";
        System.out.println("Tap hop: " + name + " | Tong Loi Nhuan: " + util + " | Nguong: " + threshold + " -> " + status);
    }

   
}