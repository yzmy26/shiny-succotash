package com.yzmy.qzs;

import lombok.Data;

import java.util.*;

public class Work {


    @Data
    class GuidePath {
        private ZoneCamera[] zoneCameras;
        private TrafficSign[] trafficSigns;
    }

    @Data
    class ZoneCamera {
        private List<String> links;
        private int speed;
    }
    @Data
    class TrafficSign {
        private List<String> links;
        private int signType;
    }


    static class FacilityMatch {
        String type;  // "ZoneCamera" or "TrafficSign"
        int index;    // 在对应数组中的下标

        FacilityMatch(String type, int index) {
            this.type = type;
            this.index = index;
        }

        @Override
        public String toString() {
            return type + "[" + index + "]";
        }
    }

    static class AcNode {
        Map<String, AcNode> children = new HashMap<>();
        AcNode fail;
        // 终止节点：在此结束匹配的所有设施
        List<FacilityMatch> facilities = new ArrayList<>();
    }

    @Data
    public static class RouteFacilityMatcher {

        private final AcNode root = new AcNode();

        // ---- 构建 AC 自动机 ----

        public void addPattern(int index, List<String> links, String type) {
            AcNode node = root;
            for (String link : links) {
                node = node.children.computeIfAbsent(link, k -> new AcNode());
            }
            node.facilities.add(new FacilityMatch(type, index));
        }

        public void buildFail() {
            Queue<AcNode> queue = new ArrayDeque<>();
            for (AcNode child : root.children.values()) {
                child.fail = root;
                queue.add(child);
            }
            while (!queue.isEmpty()) {
                AcNode parent = queue.poll();
                for (var e : parent.children.entrySet()) {
                    String link = e.getKey();
                    AcNode child = e.getValue();

                    AcNode f = parent.fail;
                    while (f != root && !f.children.containsKey(link)) {
                        f = f.fail;
                    }
                    child.fail = f.children.getOrDefault(link, root);

                    queue.add(child);
                }
            }
        }

        // ---- 初始化：从 PathInfo 批量注册 ----

        public static RouteFacilityMatcher fromPathInfo(GuidePath pathInfo) {
            RouteFacilityMatcher matcher = new RouteFacilityMatcher();

            ZoneCamera[] cameras = pathInfo.getZoneCameras();
            if (cameras != null) {
                for (int i = 0; i < cameras.length; i++) {
                    matcher.addPattern(i, cameras[i].getLinks(), "ZoneCamera");
                }
            }

            TrafficSign[] signs = pathInfo.getTrafficSigns();
            if (signs != null) {
                for (int i = 0; i < signs.length; i++) {
                    matcher.addPattern(i, signs[i].getLinks(), "TrafficSign");
                }
            }

            matcher.buildFail();
            return matcher;
        }

        // ---- 匹配 ----

        // 沿 route 扫描，返回所有连续匹配上的设施 {type, index}
        public List<FacilityMatch> match(List<String> routeLinks) {
            List<FacilityMatch> result = new ArrayList<>();
            AcNode node = root;

            for (String link : routeLinks) {
                while (node != root && !node.children.containsKey(link)) {
                    node = node.fail;
                }
                node = node.children.getOrDefault(link, root);

                // 收集当前节点 + 沿 fail 链上所有匹配
                AcNode tmp = node;
                while (tmp != root) {
                    if (!tmp.facilities.isEmpty()) {
                        result.addAll(tmp.facilities);
                    }
                    tmp = tmp.fail;
                }
            }

            return result;
        }
    }
}

