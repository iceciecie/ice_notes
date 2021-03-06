### 数据库锁
- 锁的分类：根据加锁的范围，可以分为全局锁、表级锁、行锁

### 全局锁
- 什么是全局锁？
  - 全局锁就是对整个数据库实例加锁，加锁后整个数据库处于只读状态。此时insert、update、delete、ddl操作、更新事务的提交操作都会被阻塞
- 全局锁的使用场景？
  - 典型使用场景是给数据库做全库的逻辑备份。也就是在某个固定时间点将整个库的所有表数据都select出来保存
- 全局锁会使全库只读，这样做有什么危险？
  1. 如果是在主库上备份，此时不仅进行写操作，业务基本停摆
  2. 如果是在读库上备份，那么备份期间从库不能执行主库同步过来的binlog，会导致主从延迟
- 全局锁的作用是使全库只读，除了使用FTWRL命令外还有什么其他办法？
  - 全局锁是通过保证固定时间点所有表读取的数据不会发生变化来实现逻辑备份的，在可重复度隔离情况下的事务也满足这一要求。可重复读是可以满足逻辑备份的要求的，但是要求所有的表都是支持事务的，如果用了不支持事务的引擎，则只能使用FTWRL
- 全局锁使得全库只读，为什么不适用set global readonly=true来实现呢？
  1. 修改global变量影响大，因为global变量可能用于其他逻辑，比如有的系统会用readonly值来判断是主库还是从库
  2. FTWRL和readonly在异常处理机制上有差异。FRWRL在客户短异常断开时，会自动释放锁；但是readonly不会释放锁，而是一直保持readonly状态，这样会导致整个库长时间处于不可写的状态，比较危险

### 表级锁
- mysql中表级锁有两种，一是表锁，而是元数据锁
##### 表锁
- 表锁分为读锁和写锁。读锁和读锁不互斥，读锁和写锁互斥、写锁和写锁，读锁均互斥。
- 使用lock tables ... read/write来加锁/解锁。在客户端断开时，锁也会主动释放
- lock tables 除了会限制别的线程读写外，也会限定本线程接下来的操作对象
- 在没有更细粒度的锁的支持下，表锁是最常用的并发处理方式。如果有类似行锁这样的机制，基本不会使用表锁了
##### MDL锁（metadata lock）
- MDL锁不需要显式的使用，mysql会自动添加。当对一个表进行增删改查时会添加MDL读锁；当对表做结构变更时，加MDL写锁。
  - 读锁和读锁不互斥，因此可以多个线程同时对一张表增删改查
  - 读锁和读锁、读锁和写锁均互斥，用来保证变更表结构时的安全性。因此不同的DDL是互斥的
- 为什么给一个小表加字段，整个库挂了？
  1. MDL锁在语句开始执行时申请，但是语句结束后并不会马上释放，二是等到整个事务提交后再释放
  2. 数据库内等待锁的线程是通过队列的方式排队执行的
  3. 由于存在未提交的长事务，在执行添加字段操作时拿不到MDL读锁，所以DDL操作会堵塞。此时后续的增删改查操作又会源源不断的进到数据库中，导致整个数据库的线程会爆满
- 如何安全的给小表加字段？
  1. 在给小表加字段过程中，如果发现由于长事务导致一直拿不到MDL写锁，可以将长事务直接kill掉。但是若数据库操作比较频繁，这中方法也会麻烦
  2. 在执行添加字段操作时，在alert语句里设置等待时间，如果等待时间内能拿到MDL锁，则执行操作，如果拿不到则先放弃，不要阻塞后面的语句，等后续在重新尝试这个命令

### InnoDB行锁
- 两阶段锁协议：事务开启后，行锁在需要的时候才加上的，但并不是不需要了就立即释放，二是直到事务提交才会释放锁
- 在事务需要多个行锁时，尽量把最可能造成锁冲突、最可能影响并发度的锁往后方，以减少等待锁、占有锁的时间
- 两个事务相互依赖对方的行锁，则会造成死锁，解决死锁的策略有两种，正常情况下多使用第二种方法
  1. 一直等待，直到超时
  2. 发起死锁检测，发现死锁后，主动回滚死锁链条中的某一个事务，让其他事务得以继续执行
- 死锁检测是一个消耗CPU的操作，当同时存在大量的死锁检测时，数据的CPU压力会很大，解决的办法有两个
  1. 临时放弃死锁检查。但是这样需要承担业务超时的风险
  2. 控制并发度，使得一段时间内只有一定数量的死锁检测。可以通过中间件实现，也可以修改源码。从设计角度看可以将一行改成多行来减少锁冲突，比如金额本来是一行，改成10行，这样冲突的记录就是原来的1/10，但是要注意处理边界问题

### 事务隔离
- 隔离级别：隔离级别越高，事务执行的效率也就越低
  1. 读未提交：一个事务还没提交，它做的变更就能被其他事务读到
  2. 读提交：一个事务只有提交后，它做的变更才能被其他事务读到
  3. 可重复读：一个事务执行过程中看到的数据，总是和这个事务启动时看到的数据是一致的，且该事务未提交前做的变更对其他事务也是不可见的
  4. 串行化：对于同一行记录，写会加写锁，读会加读锁。当出现读写锁冲突时，后访问的事务必须等前一个事务执行完成，才能继续执行
- 隔离通过视图的方式实现，在不同的隔离级别下，创建的视图也是不一样的
  1. 读未提交：直接返回的是记录上的最新值，此时不创建视图
  2. 读提交：在每一个sql语句开始执行时创建
  3. 可重复度：在事务启动时创建，整个事务存在期都用这个视图
  4. 串行化则是直接通过加锁的方式来避免并行访问
- 事务隔离的实现：mysql会在每一条记录更新时保留对于的回滚操作，记录上的最新值都可以通过回滚操作恢复到前一个状态的值。事务启动时，会根据当前最新的记录值，使用事务启动时间点到最新记录值时间点之间的回滚操作将数据回滚到事务启动时的状态。
  - 注意事项：对于一条回滚日志来说，当系统中没有比这个回滚日志更早的视图时，就会删除此回滚日志。不要使用长事务，因为长事务会使得数据库中会存在时间很老的视图，与之对应的回滚日志就必须保留，这样会占用大量的存储空间
- 事务的启动方式
  1. begin/start transation：启动事务，但是事务真正开始启动是执行此命令后，第一个执行操作InnoDB表的语句的时候
  2. set autocommit=1：操作表的语句本身就是一个事务，再语句执行结束后会自动提交
  3. start transation with consistent snapshot：马上启动一个事务并创建一个持续整个事务的一致性快照，这个只在可重复度的隔离登记下有作用，其他的隔离登记下等同于begin/start transation
  4. commit work and chain：提交事务并自动启动下一个事务
- mysql中的两个视图
  1. view。是一个用查询语句定义出来的虚拟表，在调用的时候执行查询语句并生成结果。创建视图的语法是create view...，view的查询方法与表一致
  2. InnoDB在实现MVCC时用到的一致性视图，即consistent read view，用于支持RC(Read Committed，读提交)和RR(Repeatable Read，可重复读)隔离级别的实现，其没有物理结构，作用是事务执行期间用来定义“我能看到什么数据”
- mysql是如何实现MVCC一致性视图的 **<font color="red">需要进一步理解！！！</font>**
  1. InnoDB中的每一个事务都有一个事务ID，叫做transaction id，它是在事务开始的时候向InnoDB事务系统申请的，是按撒申请顺序严格递增的，当每次事务更新数据时，都会生成一个新的数据斑斑，并且把transation id赋值给这个数据版本的事务ID，同时旧的事务版本也会保留。当处于可重复度隔离级别下，其作用在于对不同时间点启动的事务可以根据事务版本来确定数据的可见性。
  2. 事务启动的瞬间，InnoDB会为每个事务构造一个数组，用来保存这个事务启动瞬间，当前正在“活跃”的所有事务ID。“活跃”指的就是，启动了但还没提交。
  3. MYSQL更新数据时都是先度后写，而且只能读当前的值，称为“当前读”。同时加了锁的select语句，也是当前读
- 一个数据版本，对于一个事务视图来说，除了自己的更新总是可见以外，有三种情况
  1. 版本未提交，不可见
  2. 版本已提交，但是是在视图创建后提交的，不可见
  3. 版本已提交，而且是在事务创建前提交的，可见
- 读提交和可重复读的区别是
  1. 可重复读隔离级别下，只需要在事务开始的时候创建一致性视图，之后事务里的其他查询都共用这个一致性视图。也就是说对于一个数据只需要保存一次事务版本的数组信息
  2. 在读提交隔离级别下，每一个语句都会重新算出一个新的视图。也就是说，每一个语句都需要保存一次事务版本的数组信息

#### 索引
###### 索引的出现是为了提高数据查询的效率，类似书的目录

###### 索引的常见数据类型
  1. 哈希表：是一种以k-v形式存储数据的结构，先将k值hash后确定一个具体的位置，再将value值放入此位置。当hash冲突时，使用链表的方式来往后延展。由于hash后位置不是连续的，且链表也不是有序链表，所以hash表的方式只适合等值查询的场景
  2. 有序数组：有序数组是通过数组的方式来保存数据，且数据是有序的。由于有序数组是连续的，所以有序数组的等值查询和区间查询都是很快的，但是当数据需要频繁更新时，效率就比较低，因为需要移动大量的数据来保持有序性。有序数组值适合静态存储引擎
  3. 搜索树：搜索树的查询和更新操作都是比较好的选择，但是由于数据库的数据量很大，如果是而叉搜索树的话，树的层次（树高）会比较大，对于固定数量的数据来说，树高越大，当需要读取数据时，磁盘I/O的次数就越多，所以实际中多使用N叉搜索树

###### 数据库的数据类型对与数据库来说非常重要，理解数据库的各种数据类型有利于更好的使用数据库

###### InnoDB的索引模型
  1. 表的数据是根据主键顺序以索引的形式存放的，这种存储的方式称为索引组织表。索引使用的是B+树索引模型
  2. 主键索引的叶子节点保存的是主键对应的整行记录，非主键索引保存的是主键的内容。主键索引称为聚族索引，非主键索引称为二级索引

###### 为什么查询时要尽量基于主键查询？
  - 基于主键查询只需要扫描主键对于的B+树，基于非主键索引，需要先扫描非主键索引的B+树，拿到主键值后再去扫描主键索引的B+树

###### 索引维护
  1. 若新增的数据是有序的，则只需要将新的数据在B+树后新增即可，若是无序的，则需要挪动数据的位置
  2. 如果挪动数据位置的页是满的，则需要新申请一个数据页并做对应的数据迁移，这称为页分裂。页分裂不仅会影响性能，还会造成数据页的利用率下降
  3. 由于非主键索引都需要保存主键信息，所以如果主键占有空间小的话，那整个表的索引占有空间都会小

###### 使用自增主键的好处
  - 每次新增的主键都是递增的，在B+树中做的都是追加操作，不会触发数据移动，也不会触发叶子节点的分裂。递增主键通常为数字型，占用空间小，非主键索引占有的空间也会小，可以节省磁盘空间

###### 回表
- 主键索引保存了全部的数据信息，非主键索引保存的是索引列加上主键列值。在数据查询时，如果存在回到主键索引树上搜索的情况，就称为回表

###### 覆盖索引
- 非主键索引保存的是索引列信息和主键信息，查询时如果查询的列都是非主键索引列，则不需要回表去获取数据，这总情况称为覆盖索引

###### 最左前缀原则
- 创建索引时，根据列的最左N位创建索引，也可以起到索引的效果。

###### 创建联合主键时字段的顺序安排
  1. 联合索引只对索引首列的查询起到索引作用
  2. 联合索引对非索引首列有覆盖索引的作用
  3. 联合索引对索引首列的搜索其加速作用，其保存了索引全列的数据，所以在查询非索引首列时不需要回表，提高了查询效率
  4. 由于联合索引对非索引首列不起搜索加速作用，非索引首列也有频繁的查询操作，则需要对其单独创建索引。
  5. 当即需要创建联合索引，又需要创建单独索引时，处于索引存储空间最小化的考虑，可以为列较小的字段单独创建索引并将列较大的放在联合索引的最左边

###### 索引下推
- 在索引遍历过程中，先对索引中包含的字段先做判断，直接过滤掉不满足条件的记录，减少回表次数

###### 查询时普通索引和唯一索引查询数据时的区别？
  1. 对于普通索引来说，索引只保证了有序性，但是没有保证唯一性。查找到满足条件的第一条记录后，还需要去查询下一条记录，直到碰到第一个不满足条件的记录才停止，并将所有满足条件的记录都返回回去
  2. 对于唯一索引来说，由于唯一索引保证了唯一性。查找到满足条件的记录后就会停止。并将满足条件的记录返回回去
  3. 由于mysql中是以数据页为单位进行读写的，在普通索引查询时，若需要去读下一条记录时，下一条记录有很大可能性已经在内存中了，此时读取的消耗几乎可以忽略不计。所以普通索引和唯一索引的差距在于普通索引会多进行一次指针寻找和一次判断，而这些的速度很快，所以对于查询而言，普通索引和唯一索引的差距微乎其微

###### change buffer
  - change buffer是一种用来减少随机读磁盘以减少IO消耗的策略，在建有普通索引的列上进行数据更新时会用到这一个机制
  - change buffer具体指的是当需要更新一个数据页时，如果数据页在内存当中的话就直接更新，如果数据页不再内存当中，就先将数据保存到change buffer中，此时数据更新就算完成了。当下次查询需要访问这个数据页的时候，先将数据页读入内存中，再执行和此数据页相关的change buffer操作，这样就得到了完整的数据页信息。
  - change buffer除了在内存中以外，也会被写入到磁盘中
  - 将change buffer中的操作应用到原数据页，得到最新结果的过程称为merge，发声merge的时间点有两个
    1. 访问这个数据页的时候
    2. 后台会定期执行merge
    3. 数据库正常关闭的过程中
  - merge的执行流程
    1. 从磁盘读入数据页到内存
    2. 从change buffer里找出这个数据页的change buffer记录，依次应用，得到新版的数据页
    3. 写redo log。这个redo log包含了数据的变更和change buffer的变更
  - change buffer的使用场景
    1. 首先change buffer的使用目的是为了将数据变更的动作缓存起来，减少磁盘IO的操作。为了达到这个目的需要在merge之前尽量多的保存change buffer，这就需要不能频繁的读取数据页，所以对于写多读少的业务来说，change buffer的收益较大

###### 更新时普通索引和唯一索引的区别?
  - 唯一索引更新时，需要去读取下一条的记录来判断是否符合唯一性约束，如果此时数据页在内存中，则直接判断就可以了；但是如果数据不再内存中，则需要从磁盘中读取。对于普通索引来说，不需要满足唯一性约束，数据页在内存中，直接判断，不再内存中，是哟change buffer，都不需要去读磁盘，所以普通索引比唯一索引的效率要高

###### change buffer 和 redo log的区别
- redo log主要节省的是随机写磁盘的IO消耗，而change buffer主要节省的则是随机读磁盘的IO消耗

###### 优化器选择索引的判断逻辑
- 优化器选择索引需要考虑的因素有很多，列举几个如下
  1. 扫描的行数
  2. 是否使用临时表
  3. 是否需要排序
  4. 是否需要回表
- 选择索引时，扫描的行数如何判断？
  - mysql会维护一个索引的“基数”，“基数”的产生是通过采用统计的方式计算出来的。mysql会采样N个数据页，得到每个数据页上的不同值，再算出平均数，最后乘以这个索引的页面数，就得到了这个索引的基数。在判断扫描行树时，根据条件结合基数来估计需要扫描多少行。
  - mysql的索引统计信息是动态更新的，当变更的数据行超过一定比例时，会自动触发重做一次索引统计

###### 索引选择异常时的处理
1. 如果确定是索引统计信息不对，可以使用analyze table t来重新统计索引信息
2. 采用force index强行选择一个索引
3. 修改sql语句，引导mysql使用我们期望的索引
4. 新建一个更合适的索引，来提供给优化器选择，或删除误用的索引

###### 怎么给字符串字段加索引
1. 直接创建完整索引，这样比较占用空间
2. 创建前缀索引，节省空间，创建前可以通过查询区分度的方法确定前缀的长度。但是前缀索引会增加查询扫描次数，并且不能使用覆盖索引
3. 倒序存储，再创建前缀索引，用于绕过字符串本身前缀的区分度不够的问题
4. 创建hash字段索引，查询性能稳定，有额外的存储和计算消耗，和前面的三种方式一样，都不支持范围扫描

###### 为什么mysql的sql执行速度会变慢？
- InnoDB通过redo log加内存数据来保证数据的一致性的，但是这种策略会导致内存中存在数据页“脏页”。当由于某些原因导致redo log需要往磁盘中写数据时，就会导致mysql的性能下降
- “脏页”和“干净页”：当内存数据页和磁盘数据页内容不一致的时候，我们称这个内存页为“脏页”。内存数据写入磁盘后，内存和磁盘上的数据页的内容就一致了，称为“干净页”
- 什么情况下会触发flush(刷脏页)
  1. redo log写满了，系统会停止所有更新操作，将部分或全部redo log按照先后顺序依次写入磁盘中以留出空间给后续的操作写redo log
  2. 系统内存不足，当需要新的内存页而内存不够用的时候，就要淘汰一些数据页，空出内存给其他数据页使用，如果淘汰的是脏页，就需要先将数据写入磁盘中
  3. 系统空闲时，系统将会将redo log写入磁盘中
  4. mysql正常关闭时
- flush可能造成严重性能影响的情况
  1. redo log写满，需要先写redo log，此时系统会停止更新
  2. 内存不够用了，需要flush腾出内存空间给新的内存页使用
- InnoDB刷脏页的策略
  1. 需要直到InnoDB所在主机的IO能力
  2. 需要直到最大的脏页比例
  3. 需要直到最新的redo log日志号和checkpoint之间的序列号差值
- 最大脏页比例默认值为75%，实际使用时尽量不要超过这个值
- mysql 刷脏页时的连带模式
  - 在刷脏页时，如果数据页旁边的数据页也是脏页的话，会将旁边的数据页一并刷掉，甚至出现连锁反应。
  - 可以通过innodb_flush_neighbors参数来控制这个行为，对于IO性能好的主机，建议关闭此功能

###### InnoDB的表结构
- InnoDB的表包含两个部分，表结构定义和表数据
  1. 表结构定义：8.0以前，保存在以.frm为后缀的文件里；8.0以后保存在系统数据库表里
  2. 表数据：innodb_file_per_table设置为OFF，表数据放在系统共享表空间；设置为ON，每个表数据都会单独存储在一个以.ibd为后缀的文件中。从5.5.5开始，默认指为ON

###### 数据空洞
- 删除数据，删除数据时，InnoDB引擎只是将对应的记录标记为删除，但是不会释放表空间
- 插入数据，插入数据时如果触发了页分裂，可能使得数据页的利用率，也会导致数据空洞
- 出现数据空洞时，就会出现删除了数据，但是表空间却没有减小的情况，可以通过重建表来排除数据空洞

###### count(\*)的实现方式
1. MyISAM引擎是将一个表的总行数存在磁盘上，因此执行count(\*)的时候会直接返回这个值，效率很高
2. InnoDB在执行count(\*)时，需要将数据一行一行的从引擎中读出来，然后累积计数
- 为什么InnoDB不把总行数存在磁盘上？
    1. 因为mysql的MVCC机制，由于事务的原因，在不同隔离级别下，每一条记录都需要判断自己对于当前的查询对话是否可见，所以需要一行一行的读出来进行判断
- InnoDB在count(\*)时做的优化：一个表会有多个索引，引擎会去选择最小的那个索引进行计算
- show table status会显示表的总行数，但是这个总行数是通过采样统计估算出来的，不能作为精确数值使用
- 直接使用count(\*)会遍历全表，那如何实现获取记录的总数？
  - 使用缓存，保存表的总行数。新增一条记录行数加1，删除一条记录行数减1。但是有两个问题
    1. 如果缓存系统出现问题，比如异常重启，就会出现保存了的数据丢失的情况。此时可以通过重新查询一次count(\*)再放入缓存中
    2. 如果数据库和缓存系统组合在一起，不能实现分布式事务的话，会出现逻辑上的不一致。假设数据库已经记录了数据，但是缓存中的行数还未加1，此时由于没有事务，数据库中的真实行数会比缓存中的多一行，造成数据不一致
  - 使用数据库来保存计数
    1. 新建一张计数表来统计表的总行数，由于数据库本身可以异常恢复且有事务支持，所以可以实现精确统计
- count(\*)、count(主键ID)、count(1)、count(字段)的性能差异
  1. count(\*)：按行累加
  2. count(主键ID)：先遍历表获取主键返回给Server层，Server层拿到id后，判断不可能为空的，就按行累加
  3. count(1)：遍历整张表，但是不取值，Server层对拿到的每一行数据，放一个数字“1”进去，判断不可能为空的，按行累加
  4. count(字段)：遍历表拿到字段的值，再判断这个值是否为null，不是null才累加
  5. 总体性能：count(\*)≈count(1)>count(主键Id)>count(字段)

### order by 是如何工作的？
- 几个关键概念
  1. mysql会为每个线程都分配一块内存用于排序，称为sort_buffer

###### 全字段排序
- 将待排序的数据字段全部从磁盘中取出，都放入到sort_buffer中，在排序时，若内存空间不够，则需要用到磁盘中的临时文件来辅助排序。Mysql会将需要排序的数据分成N份，每一份单独排序后保存在临时文件中，然后把这12个有序文件再合并成一个有序的大文件
- 执行顺序
  1. 在查询条件对应的字段索引中搜索满足条件的记录，拿到对应的主键ID
  1. 初始化sort_buffer，确定需要放入sort_buffer的字段
  2. 拿着主键ID去主键索引树中获取查询字段的值，将其放入sort_buffer中
  3. 继续搜索查询条件字段的索引，重复2过程，直到不满足查询条件为止
  4. 在sert_buffer中排序完成后将数据返回

###### rowId排序
- 当查询的字段很多时，sort_buffer一次能装下的数据就会很少，需要的临时文件也就越多，效率也就会越低。当max_length_for_sort_data小于查询字段的总长度时，就会使用rowId排序的办法。和全字段排序不同，rowId排序放入sort_buffer的是待排序字段加上该行对应的主键ID，在排序完成后，再用主键ID去主键索引树中获取一次查询的数据即可
- 执行顺序
  1. 初始化sort_buffer，确定需要放入sort_buffer的字段
  2. 搜索查询条件字段索引书，查找满足条件的记录，得到对应的主键ID
  3. 拿着主键ID去主键索引树中获取待排序字段的值，并将待排序字段值和主键ID一起放入sort_buffer中
  4. 重复2、3过程，将满足条件的数据全部放入sort_buffer中
  5. 在sort_buffer中排序，根据排序后的主键ID去主键索引树中搜索对应数据组装并返回

###### 全字段排序和rowId排序的对比
1. 全数据字段只需要搜索一次主键索引树，但是由于放入sort_buffer的数据较多，可能会使用较多的临时文件，访问临时文件需要读写磁盘，消耗较大。
2. rowId排序需要搜索两次主键索引树，但是放入sort_buffer的数据较少，产生的临时文件也较少，对应的磁盘IO也会变少
3. 二者实际上是在对临时文件的IO操作和对主键搜索的IO操作之间做一个平衡。可以通过设置max_length_for_sort_data来改变mysql的排序算法选择

###### 优化排序的方法
1. 建立查询条件字段和待排序字段的联合索引。在索引内待排序字段本身就是有序的，就不需要再到sort_buffer中去排序了。只需要拿着主键ID去主键索引树上搜索一次对应的数据即可
2. 在1的基础上，使用覆盖索引，可以节省去主键ID搜索的操作，再一步提高效率

###### order by rand()的执行顺序
1. 创建一个临时表，表中包含需要查询的字段和一个用来存储double类型数据的字段
2. 从原始表中获取需要查询的字段，每一行记录都调用一次rand()方法生成一个随机数，一起放置到内存表中
3. 初始化sort_buffer，从内存表中获取double类型的字段和每一行的位置信息放入sort_buffer中
4. 在sort_buffer中针对double类型字段排序
5. 从排序后的结果中获取指定数量的记录，根据位置信息去内存表中获取需要查询的字段并返回

###### 磁盘临时表
- temp_table_size这个配置是限制内存临时表大小的。如果临时表的大小超过了temp_table_size，那么内存临时表就会转成磁盘临时表

###### 优先队列算法
- mysql 5.6版本引入的新的排序算法，其维护一个队列，队列中保存需要排序的全部数据。首先将队列头的N个数据组成一个堆，这个堆是有序的，后面再依次拿队列中的数据来进行比对，以获取需要的数据

###### 不使用order by rand()的替换方法
- order by rand()方法需要扫描大量的行数，资源消耗比较大，而且排序是基于业务来做的，业务的逻辑应该在业务代码里，数据库最好只做纯粹的数据保存，获取。
- 替代 order by rand()的方法
  1. 根据主键ID来排序。获取主键ID最大值M和最小值的差N，计算出数X=(M-N)\*rand() + N，再去取不小于X的第一个ID的行。但是由于主键ID可能不连续，这样的话每个主键ID被随机到的几率是不一致的
  2. 根据行数来排序。获取表的总行数C，计算出随机数Y=floor(C * rand())，获取一个整数，再用limit Y,1 来获取数据
