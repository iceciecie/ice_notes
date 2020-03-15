package follow.ice.phenix.leecode.tree;

import java.util.ArrayList;
import java.util.List;

class TreeNode {
  Integer val;
  TreeNode left;
  TreeNode right;

  public TreeNode(int val, TreeNode left, TreeNode right) {
    this.val = val;
    this.left = left;
    this.right = right;
  }
}

/** 遍历平衡二叉树的例子 前序遍历：根节点、左节点、右节点 中序遍历：左节点、根节点、右节点 后序遍历：左节点、右节点、根节点 */
public class OrderTreeDemo {

  private static List<Integer> treeValList = new ArrayList<>();

  public static void main(String[] args) {}

  private void preOrder(TreeNode self, TreeNode root) {
    if (self == root || self == null) {
      return;
    }
    treeValList.add(self.val);
    preOrder(self.left, root);
    preOrder(self.right, root);
  }

  private void inOrder(TreeNode self, TreeNode root) {
    if (self == root || self == null) {
      return;
    }
    inOrder(self.left, root);
    treeValList.add(self.val);
    inOrder(self.right, root);
  }

  private void postOrder(TreeNode self, TreeNode root) {
    if (self == root || self == null) {
      return;
    }
    postOrder(self.left, root);
    postOrder(self.right, root);
    treeValList.add(self.val);
  }
}
