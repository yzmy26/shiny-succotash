package com.yzmy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Solution2 {
    /**
     * 2. 两数相加
     */
    public ListNode addTwoNumbers(ListNode l1, ListNode l2) {
        if (l1 == null) {
            return l2;
        }
        if (l2 == null) {
            return l1;
        }
        ListNode res = new ListNode();
        ListNode head = res;
        int temp = 0;
        while (l1 != null) {
            temp += l1.val;
            l1 = l1.next;
          if (l2 != null) {
              temp += l2.val;
              l2 = l2.next;
              // 这里保证l1不会为null
              if (l1 == null) {
                  l1 = l2;
                  l2 = null;
              }
          }
          if (temp >= 10) {
              res.next = new ListNode(temp % 10);
              temp = 1;
          } else {
              res.next = new ListNode(temp);
              temp = 0;
          }
          res = res.next;
        }
        if (temp > 0) {
            res.next = new ListNode(temp);
        }
        return head.next;
    }

    public class ListNode {
        int val;
        ListNode next;
        ListNode() {}
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    }
}
