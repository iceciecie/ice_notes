package follow.ice.phenix.leecode.recursion;

/**
 * 递归调用的模板
 */
public class RecursionDemo {
    private static final int MAX_LEVEL = 100;

    private void recursion(int level, int param) {
        //递归终止条件
        if (level > MAX_LEVEL) {
            return;
        }
        //处理当前逻辑
        process(level, param);
        //下探到下一层
        recursion(level + 1, param);
        //清理当前层数据
        clearSomeThing();
    }

    private void clearSomeThing() {

    }

    private void process(int level, int param) {

    }
}
