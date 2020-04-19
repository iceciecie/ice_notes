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