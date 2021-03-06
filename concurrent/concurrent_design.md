## 避免共享的设计模式
#### Immutability模式（不可变模式）
- 多个线程同时读写共享变量时存在并发问题，只读不写的话就可以避免并发问题，所以Immutability模式就是提供了只读不写的变量来保证并发安全
- 由于变量不可修改，当创建大量变量时占用资源很多，所以可以通过享元模式来复用对象
- java提供的不可变对象及创建不可变对象的方法
    1. String、Long、Integer、Double等基本数据类型的包装类都是不可变对象，且都使用了享元模式
    2. 使用final修饰类，属性，不提供set方法可以做到变量不可变，但是要注意final修饰的边界
- 基本数据类型的包装类不适合用来做锁，因为使用了享元模式，看似不同的对象在jvm中可能是同一个对象

#### Copy-On-Write模式
- 写时复制，适合读多写少的情况，在有写操作时，通过copy一份数据出来进行新增操作来实现。

#### 线程本地存储模式
- 多个线程同时读写**共享**变量时存在并发问题，可以通过消除共享的方式来解决并发问题
- java提供了ThreadLocal来避免变量共享
- ThreadLocal
    1. ThreadLocal的使用方法
        - 创建ThreadLocal对象，调用set()方法保存对象，调用get()方法获取变量
    2. ThreadLocal的工作原理
        - Thread内部维护了一个ThreadLocalMap，其内部保存数据的是一个Entry数组，当set值时，先获取当前线程的ThreadLocalMap，再将ThreadLocal对象hash之后确定其在Entry数组中的位置，再将ThreadLocal对象和保存的变量存入到Entry数组中
    3. ThreadLocal与内存泄露
        - ThreadLocal对象的key是若引用的，其保存数据的Entry.value确是强引用的。当线程被复用的时候，比如使用了线程池，则其value值由于其关联的线程一直是存活的，所以不会释放value，导致内存泄露
        - 解决办法就是，一定要手动remove设置的变量

## 多线程版本的if设计模式
#### Guarded Suspension模式：等待唤醒机制的规范实现
- 引入第三方来实现两个线程之间的协作
- 当协作的线程有数据交换或关联关系时，第三方可以维护此关系

## 三种简单的分工模式
#### Thread-Pre-Message模式
- 为每个任务创建一个独立的线程
- java中不太适合使用这种模式，因为java中创建线程的消耗太大，不适合频繁创建
- java也提供Fiber来实现轻量级的线程，但是不是很成熟

#### Worker Thread模式
- 创建一定数量的线程，当有任务要执行时，线程获取任务并执行，当没有任务要执行时，线程空闲
- 对应到java中就是java的线程池
- 使用线程池的注意事项
    1. 创建线程时要使用有界队列来接受任务，并根据业务需求指定拒绝策略
    2. 当使用同一个线程池时，提交的任务要相互独立，不能有依赖关系，否决容易造成死锁；对于有依赖关系的任务，应该创建不同的线程池来执行

#### 生产者-消费者模式
- 生产者-消费者的核心在于任务队列，任务队列大了可能导致OOM，小了会导致阻塞无法提升效率
- 优点
    1. 可以解耦，生产者、消费者之间没有依赖关系，但是可以通过队列来进行通信，所以可以解耦
    2. 可以平衡生产者和消费者的速度差异。同步情况下不管生产者、消费者的速度差异如何，双方都必须创建同样多的线程来处理任务；使用生产者-消费者模式后，由于阻塞队列的存在，速度块的可以少常见线程以节省资源
    3. 支持批量执行以提升性能。比如1000条数据一条一条插入的性能弱于一次插入1000条数据，队列可以保存1000条数据，再由消费者一次获取执行，就可以提升性能
    4. 支持分阶段提交以提升性能，本质上还是将数据缓存起来，在合适的时候可以一次批量执行
- MQ中的点对点模式是一个消息只会被一个消费者消费；发布订阅模式则是一个消息可以被多个消费者消费 

#### 如何优雅的终止线程和线程池
- 优雅的终止：在T2线程中终止T1线程，且T1线程不是立即终止，而是处理完任务停止前要做的事情才线程终止，也就是线程T1有机会处理“后事”
- 两阶段终止模式
    1. 发送终止指令
    2. 响应终止指令
- 终止线程
    1. 不能使用stop()方法
    2. 要优雅的终止线程，线程要处理一定的逻辑，所及线程必须是runnable状态，则首先要让线程恢复到runnable状态，再设置线条终止的标志，线程执行到标志位自动终止线程
    3. 首先调用interrupt()方法讲待终止的线程唤醒到runnable状态，再设置待终止线程的标志位；待终止线程执行到标志位后，检测标志状态，自动终止线程
- 终止线程池
    1. shutdown()方法：线程池拒绝接收新任务，但是会等待正在执行的任务和进入了阻塞队列的任务全部执行完才会最终关闭线程池
    2. shutdownNow()方法：线程拒绝接收新任务，且会中断正在执行的任务，且阻塞队列中的任务也会失去执行的机会，但是会将失去执行机会的任务作为shutdownNow()方法的返回值返回
    3. 如果执行的任务允许取消，则不能使用shutdownNow()方法，只能使用shutdown()方法，如果任务允许重复执行，或者被取消的任务可以后续补偿执行，则可以使用shutdownNow()方法，前提是处理好方法的返回值
    4. 设置标志位状态时，要考虑到状态是否可见

## 高并发框架分析
#### 高性能限流器Guava RateLimiter


#### 高性能网络应用框架Netty


#### 高性能队列Disruptor


#### 高性能数据库连接池HiKariCP
































