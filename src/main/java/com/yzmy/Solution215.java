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
}
