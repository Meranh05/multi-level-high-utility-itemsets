package miner;

import model.Transaction;
import model.TransactionItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicHUIMiner {

    public List<String> mine(List<Transaction> db, int minUtil) {
        Map<String, Integer> map = new HashMap<>();

        for (Transaction t : db) {
            for (TransactionItem ti : t.getItems()) {
                String name = ti.getItem().getName();
                int util = ti.getUtility();
                map.put(name, map.getOrDefault(name, 0) + util);
            }
        }

        List<String> result = new ArrayList<>();
        for (String k : map.keySet()) {
            if (map.get(k) >= minUtil) {
                result.add("{" + k + "} -> Utility = " + map.get(k));
            }
        }
        return result;
    }
}
