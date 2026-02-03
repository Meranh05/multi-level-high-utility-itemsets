package model;

import java.util.*;

/**
 * Simple taxonomy (is-a hierarchy).
 * Each node can have at most one parent (tree / forest), matching the paper's simplifying assumption.
 */
public class Taxonomy {

    private final Map<String, String> parent = new HashMap<>();
    private final Map<String, Set<String>> children = new HashMap<>();

    public void addRelation(String child, String parentNode) {
        parent.put(child, parentNode);
        children.computeIfAbsent(parentNode, k -> new HashSet<>()).add(child);
        children.computeIfAbsent(child, k -> new HashSet<>()); // ensure node exists
    }

    /** Returns ancestors from immediate parent up to the root. */
    public List<String> getAncestors(String item) {
        List<String> list = new ArrayList<>();
        String cur = item;
        while (parent.containsKey(cur)) {
            cur = parent.get(cur);
            list.add(cur);
        }
        return list;
    }

    public boolean hasParent(String item) {
        return parent.containsKey(item);
    }

    public String getParent(String item) {
        return parent.get(item);
    }

    public Set<String> getChildren(String node) {
        return children.getOrDefault(node, Collections.emptySet());
    }

    /** Returns all taxonomy nodes (including leaves and generalized nodes). */
    public Set<String> getAllNodes() {
        Set<String> all = new HashSet<>();
        all.addAll(parent.keySet());
        all.addAll(parent.values());
        all.addAll(children.keySet());
        return all;
    }

    /**
     * Level definition aligned with the paper:
     * level(node) = length of the shortest path from node to any leaf (level 0 items).
     *
     * @param node taxonomy node or item
     * @param leafItems the set of leaf items (items appearing in the transactional DB)
     */
    public int getLevel(String node, Set<String> leafItems) {
        if (leafItems.contains(node)) return 0;

        // BFS downward until reaching any leaf item
        Queue<String> q = new ArrayDeque<>();
        Map<String, Integer> dist = new HashMap<>();
        q.add(node);
        dist.put(node, 0);

        while (!q.isEmpty()) {
            String cur = q.poll();
            int d = dist.get(cur);

            Set<String> ch = getChildren(cur);
            if (ch.isEmpty()) continue;

            for (String c : ch) {
                int nd = d + 1;
                if (leafItems.contains(c)) return nd;
                if (!dist.containsKey(c)) {
                    dist.put(c, nd);
                    q.add(c);
                }
            }
        }
        // If disconnected or no leaf found, treat as very high level.
        return Integer.MAX_VALUE / 4;
    }

    /** Collect descendants (leaf items) of a generalized node. */
    public Set<String> getDescendants(String node, Set<String> leafItems) {
        Set<String> out = new HashSet<>();
        if (leafItems.contains(node)) {
            out.add(node);
            return out;
        }
        Queue<String> q = new ArrayDeque<>();
        q.add(node);
        while (!q.isEmpty()) {
            String cur = q.poll();
            for (String c : getChildren(cur)) {
                if (leafItems.contains(c)) out.add(c);
                else q.add(c);
            }
        }
        return out;
    }
}
