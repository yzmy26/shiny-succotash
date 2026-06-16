package com.yzmy.qzs;

import java.util.*;

public class DfsStartLinkMatcher {

    public static void main(String[] args) {
        DfsStartLinkMatcher matcher = new DfsStartLinkMatcher();

        matcher.addPattern(String.class, new String[]{"A", "B", "C"}, "0,11");
        matcher.addPattern(String.class, new String[]{"A", "B"}, "99");
        matcher.addPattern(String.class, new String[]{"A", "B", "D"}, "1");
        matcher.addPattern(String.class, new String[]{"B", "D"}, "2");
        matcher.addPattern(String.class, new String[]{"B", "D", "F"}, "F");
        matcher.addPattern(String.class, new String[]{"B", "F"}, "3");

        matcher.buildFailPointer(String.class);

        System.out.println(matcher.findByStartLink(String.class, "A"));
        System.out.println(matcher.findByStartLink(String.class, "B"));
// 输出 [0, 1]

        System.out.println(matcher.match(String.class, new String[]{"A", "B", "D"}));
// 输出 [1]
    }

    private Map<Class<?>, Map<String, TrieNode>> macherMap = new HashMap<>();

    public static class TrieNode {

        private String linkId;

        private TrieNode fail;

        private TrieNode[] nextNodes;

        /**
         * 完整序列命中时的属性索引。
         * 例如 A-B-C 的 index 挂在 C 节点。
         */
        private String index;

        public TrieNode(String linkId) {
            this.linkId = linkId;
        }
    }

    /**
     * 添加一条 link 序列。
     * <p>
     * 例如：
     * A-B-C -> 0
     * A-B-D -> 1
     */
    public void addPattern(Class<?> dataClass, String[] linkIds, String dataIndex) {
        if (linkIds == null || linkIds.length == 0) {
            return;
        }

        Map<String, TrieNode> rootNextMap = macherMap.computeIfAbsent(dataClass, k -> new HashMap<>());

        TrieNode current = rootNextMap.computeIfAbsent(linkIds[0], TrieNode::new);

        for (int i = 1; i < linkIds.length; i++) {
            current = getOrCreateNextNode(current, linkIds[i]);
        }

        current.index = appendIndex(current.index, dataIndex);
    }

    /**
     * 构建 fail 指针。
     * <p>
     * 注意：这里不把 fail 节点的 index 合并到当前节点。
     * 这样可以保证 DFS 起点查询时不会收集到后缀模式的 index。
     */
    public void buildFailPointer(Class<?> dataClass) {
        Map<String, TrieNode> rootNextMap = macherMap.get(dataClass);

        if (rootNextMap == null || rootNextMap.isEmpty()) {
            return;
        }

        Queue<TrieNode> queue = new ArrayDeque<>();

        for (TrieNode rootChild : rootNextMap.values()) {
            rootChild.fail = null;
            queue.offer(rootChild);
        }

        while (!queue.isEmpty()) {
            TrieNode current = queue.poll();

            if (current.nextNodes == null || current.nextNodes.length == 0) {
                continue;
            }

            for (TrieNode child : current.nextNodes) {
                TrieNode failNode = current.fail;
                TrieNode failNext = null;

                while (failNode != null) {
                    failNext = getNextNode(failNode, child.linkId);
                    if (failNext != null) {
                        break;
                    }
                    failNode = failNode.fail;
                }

                if (failNext == null) {
                    failNext = rootNextMap.get(child.linkId);
                }

                if (failNext == child) {
                    failNext = null;
                }

                child.fail = failNext;

                queue.offer(child);
            }
        }
    }

    /**
     * 方法 1：
     * 根据某个 link 查询所有以它为起点的属性索引。
     * <p>
     * 例如：
     * 已有 A-B-C -> 0
     * 已有 A-B-D -> 1
     * <p>
     * 传入 A，返回 [0, 1]
     */
    public List<String> findByStartLink(Class<?> dataClass, String startLinkId) {
        Map<String, TrieNode> rootNextMap = macherMap.get(dataClass);

        if (rootNextMap == null) {
            return Collections.emptyList();
        }

        TrieNode startNode = rootNextMap.get(startLinkId);

        if (startNode == null) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> result = new LinkedHashSet<>();

        collectSubTreeIndexes(startNode, result);

        return new ArrayList<>(result);
    }

    /**
     * 方法 2：
     * 传入一段 link 数组，查询路径上匹配到的属性索引。
     * <p>
     * 例如：
     * 已有 A-B-C -> 0
     * 已有 A-B-D -> 1
     * <p>
     * 传入 A-B-D，返回 [1]
     */
    public List<String> match(Class<?> dataClass, String[] linkIds) {
        if (linkIds == null || linkIds.length == 0) {
            return Collections.emptyList();
        }

        Map<String, TrieNode> rootNextMap = macherMap.get(dataClass);

        if (rootNextMap == null || rootNextMap.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> result = new LinkedHashSet<>();

        TrieNode current = null;

        for (String linkId : linkIds) {
            while (current != null && getNextNode(current, linkId) == null) {
                current = current.fail;
            }

            if (current == null) {
                current = rootNextMap.get(linkId);
            } else {
                current = getNextNode(current, linkId);
            }

            if (current == null) {
                continue;
            }

            collectCurrentAndFailIndexes(current, result);
        }

        return new ArrayList<>(result);
    }

    private void collectSubTreeIndexes(TrieNode node, Set<String> result) {
        if (node == null) {
            return;
        }

        addIndexToResult(node.index, result);

        if (node.nextNodes == null || node.nextNodes.length == 0) {
            return;
        }

        for (TrieNode child : node.nextNodes) {
            collectSubTreeIndexes(child, result);
        }
    }

    private void collectCurrentAndFailIndexes(TrieNode node, Set<String> result) {
        TrieNode current = node;

        while (current != null) {
            addIndexToResult(current.index, result);
            current = current.fail;
        }
    }

    private TrieNode getOrCreateNextNode(TrieNode node, String linkId) {
        TrieNode nextNode = getNextNode(node, linkId);

        if (nextNode != null) {
            return nextNode;
        }

        TrieNode newNode = new TrieNode(linkId);

        if (node.nextNodes == null) {
            node.nextNodes = new TrieNode[]{newNode};
            return newNode;
        }

        TrieNode[] oldNodes = node.nextNodes;
        TrieNode[] newNodes = Arrays.copyOf(oldNodes, oldNodes.length + 1);
        newNodes[newNodes.length - 1] = newNode;
        node.nextNodes = newNodes;

        return newNode;
    }

    private TrieNode getNextNode(TrieNode node, String linkId) {
        if (node == null || node.nextNodes == null) {
            return null;
        }

        for (TrieNode nextNode : node.nextNodes) {
            if (nextNode != null && Objects.equals(nextNode.linkId, linkId)) {
                return nextNode;
            }
        }

        return null;
    }

    private String appendIndex(String oldIndex, String newIndex) {
        if (oldIndex == null || oldIndex.isEmpty()) {
            return newIndex;
        }

        return oldIndex + "," + newIndex;
    }

    private void addIndexToResult(String index, Set<String> result) {
        if (index == null || index.isEmpty()) {
            return;
        }

        String[] indexes = index.split(",");

        for (String item : indexes) {
            if (item != null && !item.isEmpty()) {
                result.add(item);
            }
        }
    }
}
