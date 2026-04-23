package com.yzmy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Solution215 {
    /**
     * 215. 数组中的第K个最大元素
     * 大顶堆解法。实际应用小顶堆更好？和桶排序
     */
    public int findKthLargest(int[] nums, int k) {
        // 构建大顶堆，每次找出最大元素
        makeBigHeap(nums);
        for (int i = 1; i < k; i++) {
            nums[0] = nums[nums.length - i];
            // 将这个值下移到合适位置
            makeBigNode(nums, 0, nums.length - i - 1);
        }
        return nums[0];
    }

    /**
     * 将给定的整数数组转换为大顶堆。
     * 大顶堆是一种完全二叉树，其中每个父节点的值都大于或等于其子节点的值。
     *  0
     * 1  2
     * 34 56
     * @param nums 需要被转换为大顶堆的整数数组
     */
    private void makeBigHeap(int[] nums) {
        int length = nums.length;
        if (length < 2) {
            return;
        }
        for (int i = length /2 - 1; i >= 0; i--) {
            makeBigNode(nums, i, length - 1);
        }
    }

    private void makeBigNode(int[] nums, int i, int end) {
        if (i * 2 + 1 > end) {
            return;
        }
        // 获取左中右最大值
        int max;
        if (i * 2 + 2 > end) {
            max = i * 2 + 1;
        } else {
            max = nums[i * 2 + 2] > nums[i * 2 + 1] ? i * 2 + 2 : i * 2 + 1;
        }
        if (nums[i] < nums[max]) {
            int tmp = nums[i];
            nums[i] = nums[max];
            nums[max] = tmp;
            // 继续往下推导
            makeBigNode(nums, max, end);
        }
    }

    /**
     * 快排，使用从小到大排序，应先找right再找left，否则会多很多没必要的判断
     * 若要找第k大元素，可以根据基准数字的位置来寻找单向区间，最终即可找到第k大
     * @param nums
     * @param start
     * @param end
     * @return
     */
    public int[] quickSort(int[] nums, int start, int end) {
        int left = start + 1, right = end;
        if (start >= end) {
            return nums;
        }
        int base = nums[start];
        // 选择基准数，小的放左边，大的放右边。双指针swap
        while (left < right) {
            if (nums[left] < base) {
                left++;
                continue;
            }
            while (nums[right] >= base) {
                right--;
                if (right<=left) {
                    break;
                }
            }
            if (right<=left) {
                break;
            }
            int tmp = nums[left];
            nums[left] = nums[right];
            nums[right] = tmp;
            left++;
            right--;
        }
        while (right > start && nums[right] >= base) {
            right--;
        }
        nums[start] = nums[right];
        nums[right] = base;
        quickSort(nums, start, right-1);
        quickSort(nums, right + 1, end);
        return nums;
    }

    /**
     *
     * @param nums
     * @param k
     * @return
     */
    public int findKthLargestByBucket(int[] nums, int k) {
        // 寻找最大值确定桶长度
        int min =Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int num : nums) {
            max = Math.max(num, max);
            min = Math.min(num, min);
        }
        int[] buckets = new int[max - min + 1];
        for (int num : nums) {
            buckets[num - min]++;
        }
        // 寻找第k大元素
        int count = 0;
        for (int i = buckets.length -1; i >= 0; i--) {
            count += buckets[i];
            if (count >= k) {
                return i + min;
            }
        }
        return -1;
    }
}
