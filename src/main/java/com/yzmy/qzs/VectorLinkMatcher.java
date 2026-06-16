package com.yzmy.qzs;

import java.util.*;

public class VectorLinkMatcher {
    public static void main(String[] args) {
        VectorLinkMatcher matcher = new VectorLinkMatcher();
        matcher.addPattern(String.class, new String[]{"A", "B", "C"}, "0");
        matcher.addPattern(String.class, new String[]{"B", "C"}, "99");
        matcher.addPattern(String.class, new String[]{"B", "C", "E"}, "21");

        matcher.addPattern(String.class, new String[]{"A", "B", "D"}, "1");
        matcher.addPattern(String.class, new String[]{"A", "B"}, "2");

        matcher.build(String.class);

        System.out.println(matcher.findByStartLink(String.class, "A"));
        // 输出 [0, 1]

        System.out.println(matcher.match(String.class, new String[]{"A", "B", "C"}));
        // 输出 [1]
    }

    /**
     * 按数据类型隔离。
     * 例如 Camera.class 一套索引，LaneConnectivity.class 一套索引。
     */
    private final Map<Class<?>, VectorIndex> matcherMap = new HashMap<>();

    public void addPattern(Class<?> dataClass, String[] linkIds, String dataIndex) {
        if (linkIds == null || linkIds.length == 0) {
            return;
        }

        VectorIndex index = matcherMap.computeIfAbsent(dataClass, k -> new VectorIndex());

        index.addPattern(linkIds, dataIndex);
    }

    public void build(Class<?> dataClass) {
        VectorIndex index = matcherMap.get(dataClass);

        if (index != null) {
            index.build();
        }
    }

    /**
     * 方法 1：
     * 传入一个 linkId，查询所有以它为起点的属性索引。
     * <p>
     * 例如：
     * A-B-C -> 0
     * A-B-D -> 1
     * <p>
     * 传入 A，返回 [0, 1]
     */
    public List<String> findByStartLink(Class<?> dataClass, String startLinkId) {
        VectorIndex index = matcherMap.get(dataClass);

        if (index == null) {
            return Collections.emptyList();
        }

        return index.findByStartLink(startLinkId);
    }

    /**
     * 方法 2：
     * 传入 linkId 数组，查询路径范围内匹配上的属性索引。
     * <p>
     * 例如：
     * A-B-C -> 0
     * A-B-D -> 1
     * <p>
     * 传入 A-B-D，只返回 [1]
     */
    public List<String> match(Class<?> dataClass, String[] queryLinks) {
        VectorIndex index = matcherMap.get(dataClass);

        if (index == null) {
            return Collections.emptyList();
        }

        return index.match(queryLinks);
    }

    /**
     * 每个 Class<?> 对应一个 VectorIndex。
     */
    public static class VectorIndex {

        /**
         * 所有 pattern 直接平铺存储。
         */
        private final List<PathRecord> records = new ArrayList<>();

        private boolean built = false;

        public void addPattern(String[] linkIds, String dataIndex) {
            if (linkIds == null || linkIds.length == 0) {
                return;
            }

            PathRecord record = new PathRecord(Arrays.copyOf(linkIds, linkIds.length), dataIndex);

            records.add(record);

            built = false;
        }

        /**
         * 按 startLink 排序。
         * 后面 findByStartLink 和 match 都依赖这个排序。
         */
        public void build() {
            records.sort(Comparator.comparing(record -> record.startLink));
            built = true;
        }

        /**
         * 查询所有以 startLinkId 为起点的属性索引。
         */
        public List<String> findByStartLink(String startLinkId) {
            if (startLinkId == null || records.isEmpty()) {
                return Collections.emptyList();
            }

            ensureBuilt();

            int from = lowerBound(startLinkId);
            int to = upperBound(startLinkId);

            if (from >= to) {
                return Collections.emptyList();
            }

            LinkedHashSet<String> result = new LinkedHashSet<>();

            for (int i = from; i < to; i++) {
                addIndexToResult(records.get(i).dataIndex, result);
            }

            return new ArrayList<>(result);
        }

        /**
         * 查询一段 link 数组里命中的属性索引。
         * <p>
         * 这个方法会在 queryLinks 的每一个位置尝试匹配：
         * <p>
         * queryLinks = A-B-D
         * <p>
         * start=0, link=A:
         * 候选：A-B-C, A-B-D
         * 比较后只有 A-B-D 命中
         */
        public List<String> match(String[] queryLinks) {
            if (queryLinks == null || queryLinks.length == 0 || records.isEmpty()) {
                return Collections.emptyList();
            }

            ensureBuilt();

            LinkedHashSet<String> result = new LinkedHashSet<>();

            for (int start = 0; start < queryLinks.length; start++) {
                String startLinkId = queryLinks[start];

                int from = lowerBound(startLinkId);
                int to = upperBound(startLinkId);

                if (from >= to) {
                    continue;
                }

                for (int i = from; i < to; i++) {
                    PathRecord record = records.get(i);

                    if (matchesAt(record.linkIds, queryLinks, start)) {
                        addIndexToResult(record.dataIndex, result);
                    }
                }
            }

            return new ArrayList<>(result);
        }

        private boolean matchesAt(String[] patternLinks, String[] queryLinks, int start) {
            if (start + patternLinks.length > queryLinks.length) {
                return false;
            }

            for (int i = 0; i < patternLinks.length; i++) {
                if (!Objects.equals(patternLinks[i], queryLinks[start + i])) {
                    return false;
                }
            }

            return true;
        }

        private void ensureBuilt() {
            if (!built) {
                build();
            }
        }

        /**
         * 找到第一个 startLink >= target 的位置。
         */
        private int lowerBound(String target) {
            int left = 0;
            int right = records.size();

            while (left < right) {
                int mid = (left + right) >>> 1;

                String midValue = records.get(mid).startLink;

                if (midValue.compareTo(target) < 0) {
                    left = mid + 1;
                } else {
                    right = mid;
                }
            }

            return left;
        }

        /**
         * 找到第一个 startLink > target 的位置。
         */
        private int upperBound(String target) {
            int left = 0;
            int right = records.size();

            while (left < right) {
                int mid = (left + right) >>> 1;

                String midValue = records.get(mid).startLink;

                if (midValue.compareTo(target) <= 0) {
                    left = mid + 1;
                } else {
                    right = mid;
                }
            }

            return left;
        }

        private void addIndexToResult(String index, Set<String> result) {
            if (index == null || index.isEmpty()) {
                return;
            }

            String[] items = index.split(",");

            for (String item : items) {
                if (item != null && !item.isEmpty()) {
                    result.add(item);
                }
            }
        }
    }

    public static class PathRecord {

        /**
         * 完整 link 序列。
         * 例如 [A, B, C]
         */
        private final String[] linkIds;

        /**
         * 起点 link。
         * 例如 [A, B, C] 的 startLink 是 A。
         */
        private final String startLink;

        /**
         * 属性索引。
         */
        private final String dataIndex;

        public PathRecord(String[] linkIds, String dataIndex) {
            this.linkIds = linkIds;
            this.startLink = linkIds[0];
            this.dataIndex = dataIndex;
        }
    }
}
