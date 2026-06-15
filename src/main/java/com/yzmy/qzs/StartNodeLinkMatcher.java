package com.yzmy.qzs;

import java.util.*;

public class StartNodeLinkMatcher {
    public static void main(String[] args) {
        StartNodeLinkMatcher matcher = new StartNodeLinkMatcher();

        matcher.addPattern(String.class, new String[] {"A", "B", "C"}, "0,11");
        matcher.addPattern(String.class, new String[] {"A", "B"}, "99");
        matcher.addPattern(String.class, new String[] {"A", "B", "D"}, "1");
        matcher.addPattern(String.class, new String[] {"B", "D"}, "2");
        matcher.addPattern(String.class, new String[] {"B", "D","F"}, "F");
        matcher.addPattern(String.class, new String[] {"B", "F"}, "3");

        matcher.buildFailPointer(String.class);

        System.out.println(matcher.findByStartLink(String.class, "A"));
        System.out.println(matcher.findByStartLink(String.class, "B"));
// 输出 [0, 1]

        System.out.println(matcher.match(String.class, new String[] {"A", "B", "D"}));
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
     * 只有 root 第一层节点才用这个类型。
     */
    public static class StartTrieNode extends TrieNode {

        /**
         * 所有以当前 link 为起点的属性索引。
         *
         * 例如：
         * A-B-C -> 0
         * A-B-D -> 1
         *
         * 那么 A.startIndex = "0,1"
         */
        private String startIndex;

        public StartTrieNode(String linkId) {
            super(linkId);
        }
    }

    /**
     * 添加一条 link 序列。
     */
    public void addPattern(
            Class<?> dataClass,
            String[] linkIds,
            String dataIndex
    ) {
        if (linkIds == null || linkIds.length == 0) {
            return;
        }

        Map<String, TrieNode> rootNextMap = macherMap.computeIfAbsent(
                dataClass,
                k -> new HashMap<>()
        );

        TrieNode current = rootNextMap.computeIfAbsent(
                linkIds[0],
                StartTrieNode::new
        );

        StartTrieNode startNode = (StartTrieNode) current;

        for (int i = 1; i < linkIds.length; i++) {
            current = getOrCreateNextNode(current, linkIds[i]);
        }

        // 完整路径命中的 index，挂在最后一个节点
        current.index = appendIndex(current.index, dataIndex);

        // 以起点 link 查询的 index，只挂在第一层 StartTrieNode 上
        startNode.startIndex = appendIndex(startNode.startIndex, dataIndex);
    }

    /**
     * 构建 fail 指针。
     *
     * 注意：这里同样不把 fail 节点的 index 合并到当前节点。
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
     *
     * 例如：
     * A-B-C -> 0
     * A-B-D -> 1
     *
     * 传入 A，返回 [0, 1]
     */
    public List<String> findByStartLink(
            Class<?> dataClass,
            String startLinkId
    ) {
        Map<String, TrieNode> rootNextMap = macherMap.get(dataClass);

        if (rootNextMap == null) {
            return Collections.emptyList();
        }

        TrieNode node = rootNextMap.get(startLinkId);

        if (!(node instanceof StartTrieNode startNode)) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> result = new LinkedHashSet<>();
        addIndexToResult(startNode.startIndex, result);

        return new ArrayList<>(result);
    }

    /**
     * 方法 2：
     * 传入一段 link 数组，查询路径上匹配到的属性索引。
     *
     * 例如：
     * A-B-C -> 0
     * A-B-D -> 1
     *
     * 传入 A-B-D，返回 [1]
     */
    public List<String> match(
            Class<?> dataClass,
            String[] linkIds
    ) {
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

    private void collectCurrentAndFailIndexes(
            TrieNode node,
            Set<String> result
    ) {
        TrieNode current = node;

        while (current != null) {
            addIndexToResult(current.index, result);
            current = current.fail;
        }
    }

    private TrieNode getOrCreateNextNode(
            TrieNode node,
            String linkId
    ) {
        TrieNode nextNode = getNextNode(node, linkId);

        if (nextNode != null) {
            return nextNode;
        }

        TrieNode newNode = new TrieNode(linkId);

        if (node.nextNodes == null) {
            node.nextNodes = new TrieNode[] { newNode };
            return newNode;
        }

        TrieNode[] oldNodes = node.nextNodes;
        TrieNode[] newNodes = Arrays.copyOf(oldNodes, oldNodes.length + 1);
        newNodes[newNodes.length - 1] = newNode;
        node.nextNodes = newNodes;

        return newNode;
    }

    private TrieNode getNextNode(
            TrieNode node,
            String linkId
    ) {
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

    private String appendIndex(
            String oldIndex,
            String newIndex
    ) {
        if (oldIndex == null || oldIndex.isEmpty()) {
            return newIndex;
        }

        return oldIndex + "," + newIndex;
    }

    private void addIndexToResult(
            String index,
            Set<String> result
    ) {
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
