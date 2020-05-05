## week_one

### 数组
- 数组是一种线性的数据结构, 数组是内存中一组联系的内存单元, 用来存储类型相同的数据
- 数组可以通过下表直接访问元素, 而进行新增, 删除操作时为了保证位置的联系性, 需要对影响到的元素继续搬移
- 时间复杂度
    1. 根据下标查找: O(1)
    2. 根据值查找: O(n)
    3. 新增, 删除: O(n)

### 链表
- 链表是一种线性的数据结构, 其每个节点不仅保存了当前元素的值, 还保存了指向相邻节点的指针, 根据指针的数量和方向可以将链表分为几种. 单向链表, 双向链表, 单向循环链表, 双向循环链表
- 在已知某一个节点的情况下, 链表可以快速定位到相邻的节点
- 时间复杂度
    1. 已知节点新增, 删除: O(1)
    2. 查找: O(n) 

### 跳表
- 跳表在进行插入, 删除时的效率是很高的, 但是在进行插入, 删除操作前定位到对应的节点的效率不高, 而跳表可以解决这个问题
- 链表+多级索引就是跳表, 其通过添加多级索引的方式来快递查找底层链表的元素 

### 栈
- 先进后出; 新增, 删除的时间复杂度都是O(1), 查询的时间复杂度是O(n)的

### 队列
- 先进先出; 新增, 删除的时间复杂度都是O(1), 查询的时间复杂度是O(n)的

### 双端队列
- 实际工程中直接使用栈和队列的情况比较少, 通常情况下使用的都是双端队列, 双端队列可以理解为栈和队列的结合体, 两端都可以插入和删除
- 双端队列新增, 删除的时间复杂度都是O(1), 查询的时间复杂度是O(n)的

### 优先队列
- 插入操作, O(1)
- 查找操作是O(logN), 按照元素的优先级取出
- 底层具体实现的数据结构较为多样和复杂; 可以是heap, bst, treap

### 哈希表
- 哈希表, 也叫散列表, 是根据关键码值而直接进行访问的数据结构. 它通过把关键码值映射到表中的一个位置来访问记录, 以加快查找的速度; 这个映射的函数叫做散列函数, 存放记录的数据叫做哈希表
- 查询, 新增, 删除的时间复杂度是O(1)的
- 实际工程中, 哈希表的应用主要有两个, Set和Map

### 树
- 树就是特殊化的图, 因为树没有环; 链表就是特殊化的树, 链表在同一个方向上只和一个节点关联, 只有一个分叉
- 树有且仅有一个根节点, 可以有一个或多个子树
- 树的遍历: 前,中, 后序遍历中, 左右节点的数学是不变的, 都是先左后右, 区别在于根节点所处的位置, 前序:根节点在最前面; 中序, 根节点在中间; 后序:根节点在最后
    1. Pre-oeder: root-left-right
    2. In-order: left-root-right
    3. Post-order: left-right-root

### 二叉树
- 根节点及其子节点都只有两个分叉

### 二叉搜索树
- 二叉搜索树, 也叫二叉排序树, 有序二叉树, 排序二叉树, 是指一颗空树, 或者具有下列性质的二叉树
    1. 左子树上的所有节点的值均小于它根节点的值
    2. 右子树上所有节点的值均大于它的根节点的值
    3. 以此类推: 左,右子树也分别是二叉搜索树
- 中序遍历的结果是升序排列
- 查询, 新增, 删除操作的时间复杂度都是O(logN)的

### 递归
- 递归的思维要点
    1. 不要人肉进行递归
    2. 找到最近最简方法, 将其拆解成可重复解决的问题(重复子问题)
    3. 数学归纳法思维
- 递归的代码模板
```java
public void recur(int level, int param) {
    //set terminator condition
    if (level > MAX_LEVEL) {
        return;
    }
    //process currentlogic
    process(level, param);
    //drill down
   recur(level:level+1, newParam);
    //restore current status
} 
```
- 前序递归的java代码模板
```java
public static void
```

- 中序递归的java代码模板
```java
pubilc static void
```

- 后续递归的java代码模板
```java
public static void
```

### 分治和回溯
- 分治和回溯本质是递归, 是递归的一种特殊形式
##### 分治
- 代码模板
```java
pubic void divide_conquer(problem, param1, param2, ...) {
    // recursion terminator
    if (problem is none) {
        print_result;
        return;
    }

    //prepare data
    data = prepare_data(problem);
    List<Problem> subProblems = split_problem(problem, data);

    //conquer subProblems
    subResult1 = drive_conquer(subProblems[0], param1, param2, ...);
    subResult2 = drive_conquer(subProblems[2], param1, param2, ...);
    subResult3 = drive_conquer(subProblems[3], param1, param2, ...);
    ...

    //process adn generate the final result
    result = process_result(subResult1, subResult2, subResult3....);

    //revert the current level states
}
```

##### 回溯
- 在递归的每一层不断的尝试, 知道得到需要的结构

### 深度优先搜索和广度优先搜索
##### 深度优先搜索(Depth-First-Search)
- 二叉树深度优先搜索代码模板
```java
Set visited  = new Set();
public void dfs(TreeNode node) {
    if (node == null || visited contains node) {
        return;
    }
    visited.add(node);
    dfs(node.leftNode);
    dfs(node.rightNode);
}
```
- 树的深度优先搜索代码模板
```java
Set visited = new Set();
public void dfs (TreeNode node) {
    if (node == null || visited contaions node) {
        return;
    }
    List<TreeNode> nodeList = node.children;
    for (TreeNode child : nodeList) {
        dfs(child);
    }
}
```
- 非递归的深度优先搜索代码模板, 使用栈来实现
```java
public void DFS(TreeNode root) {
    if (root == null) {
        return;
    }
    Stack<TreeNode> stack = new Stack();
    stack.push(root);
    Set<TreeNode> visited = new Set();
    while (stack.isNotEmpty()) {
        TreeNode node = stack.pop();
        visited.add(node);
        List<TreeNode> children = generate_related_nodes(node);
        if (CollectionUtils.isNotEmpty(children)) {
            stack.push(children);
        }
    }
}
```
##### 广度优先搜索(Breadth-First-Search)
- 广度优先搜索, 使用队列实现
- 代码模板
```java
public void BFS(TreeNode root) {
    if (root == null) {
        return;
    }
    Queue<TreeNode> queue = new Queue();
    queue.add(root);
    Set<TreeNode> visited = new Set();
    while (queue.isNotEmpty) {
        TreeNode node = queue.pop();
        process(node);
        visited.add(node); 
        List<TreeNode> children = generate_related_nodes(node);
        if (CollectionUtils.isNotEmpty()) {
            queue.push(children);
        }
    }
}
```
##### 使用非递归的形式实现深度优先搜索和广度优先搜索, 代码逻辑大致相同, 实现时利用了栈和队列的特性.
- 栈先进后出, 由于父节点比子节点先入栈, 所以父节点的处理时间晚于子节点, 所以可以实现深度优先
- 队列先进先出, 父节点先入队, 所以父节点会先被处理; 在处理同一级节点时, 子节点是要先经过父节点才能入队的, 所以在每一个子节点被处理时, 其父节点和父节点的兄弟节点肯定已经处理过了, 所以可以实现广度优先搜索




























