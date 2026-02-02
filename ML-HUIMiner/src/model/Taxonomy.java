package model;

import java.util.*;

public class Taxonomy {
    private Map<String, String> parent = new HashMap<>();

    public void addRelation(String child, String parentNode) {
        parent.put(child, parentNode);
    }

    public List<String> getAncestors(String item) {
        List<String> list = new ArrayList<>();
        while (parent.containsKey(item)) {
            item = parent.get(item);
            list.add(item);
        }
        return list;
    }
}
