package miner;

import model.Taxonomy;
import model.Transaction;
import model.TransactionItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeneralizedHUIMiner {

    private Taxonomy taxonomy;

    public GeneralizedHUIMiner(Taxonomy taxonomy) {
        this.taxonomy = taxonomy;
    }

    public List<String> mine(List<Transaction> db, int minUtil) {

        Map<String, Integer> map = new HashMap<>();

        for (Transaction t : db) {
            for (TransactionItem ti : t.getItems()) {

                String item = ti.getItem().getName();
                int util = ti.getUtility();

                map.put(item, map.getOrDefault(item, 0) + util);

                for (String parent : taxonomy.getAncestors(item)) {
                    map.put(parent, map.getOrDefault(parent, 0) + util);
                }
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
