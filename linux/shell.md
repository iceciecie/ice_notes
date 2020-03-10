- 给文件赋权：chmod u+rx fileName
- 执行方式
  1. bash filename.sh 异步线程执行
  2. ./filename.sh 异步线程执行
  3. source filename.sh 同步线程执行
  4. . filename.sh 同步线程执行
- 管道：将前一个命令执行的结果传递给后面的命令，管道浮"|"
- 重定向符：可以自由组合使用
  1. \>  输入文件，会覆盖原来的信息
  2. \>> 追加输入
  3. 2  错误信息输入
  4. &  不管错误不错误，全部输入
