package kangdb.backend.utils;

/**
 * @author ykangli
 * @version 1.0
 * @date 2022/1/27 21:30
 * 通过 panic 方法，强制停机。（终止当前正在运行的Java虚拟机）
 */
public class Panic {
    public static void panic(Exception err) {
        err.printStackTrace();
        // status 为 0：表示正常退出程序，也就是结束当前正在运行中的java虚拟机。
        // status 为 1 或 -1 或 任何其他非零值 ：表示非正常退出当前程序。
        System.exit(1);
    }
}
