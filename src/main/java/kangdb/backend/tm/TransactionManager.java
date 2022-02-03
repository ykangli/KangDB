package kangdb.backend.tm;

import kangdb.backend.utils.Panic;
import kangdb.common.Error;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author ykangli
 * @version 1.0
 * @date 2022/1/27 20:04
 * TransactionManager 对事务进行管理, 能够让其他模块查询事务的状态。
 */
public interface TransactionManager {

    /**
     * 开始一个事务，并返回XID
     * @return xid  (每一个事务都有一个 XID，这个 ID 唯一标识了这个事务)
     */
    long begin();

    /**
     * 提交xid事务
     * @param xid 事务的xid
     */
    void commit(long xid);

    /**
     * 回滚xid事务
     * @param xid 事务的xid
     */
    void abort(long xid);

    /**
     * 查询一个事务的状态是否是正在进行的状态
     * @param xid 事务的xid
     * @return java.lang.boolean
     */
    boolean isActive(long xid);

    /**
     *  查询一个事务的状态是否是已提交
     * @param xid 事务的xid
     * @return java.lang.boolean
     */
    boolean isCommitted(long xid);

    /**
     * 查询一个事务的状态是否是已取消
     * @param xid 事务的xid
     * @return java.lang.boolean
     */
    boolean isAborted(long xid);

    /**
     * 关闭TM
     */
    void close();

    /**
     * 创建一个 xid 文件并创建 Transaction Manager(TM)
     * @param path xid文件路径
     * @return TransactionManagerImpl对象
     */
    public static TransactionManagerImpl create(String path) {
        File file = new File(path + TransactionManagerImpl.XID_SUFFIX);
        try {
            //创建文件失败
            if (!file.createNewFile()) {
                Panic.panic(Error.FileExistsException);
            }
        } catch (Exception e) {
            Panic.panic(e);
        }
        //该函数确定程序是否可以读取或写入由抽象路径名表示的文件。
        if(!file.canRead() || !file.canWrite()) {
            Panic.panic(Error.FileCannotRWException);
        }

        FileChannel fc = null;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "rw");
            fc = raf.getChannel();
        } catch (FileNotFoundException e) {
            Panic.panic(e);
        }

        //写只有前8字节Header的空文件，即设置 xidCounter 为 0
        ByteBuffer buffer = ByteBuffer.wrap(new byte[TransactionManagerImpl.LEN_XID_HEADER_LENGTH]);
        try {
            fc.position(0);
            fc.write(buffer);
        } catch (IOException e) {
            Panic.panic(e);
        }
        return new TransactionManagerImpl(raf, fc);
    }

    /**
     * 从一个已有的 xid 文件中 Transaction Manager(TM)  （不需要再写 8 字节的文件头）
     * @param path xid文件路径
     * @return TransactionManagerImpl对象
     */
    public static TransactionManagerImpl open(String path) {
        File file = new File(path + TransactionManagerImpl.XID_SUFFIX);
        try {
            //创建文件失败
            if (!file.createNewFile()) {
                Panic.panic(Error.FileExistsException);
            }
        } catch (Exception e) {
            Panic.panic(e);
        }
        //该函数确定程序是否可以读取或写入由抽象路径名表示的文件。
        if(!file.canRead() || !file.canWrite()) {
            Panic.panic(Error.FileCannotRWException);
        }

        FileChannel fc = null;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "rw");
            fc = raf.getChannel();
        } catch (FileNotFoundException e) {
            Panic.panic(e);
        }
        return new TransactionManagerImpl(raf, fc);
    }
}
