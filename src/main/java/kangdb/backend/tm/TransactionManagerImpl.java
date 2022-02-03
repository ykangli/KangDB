package kangdb.backend.tm;

import kangdb.backend.utils.Panic;
import kangdb.backend.utils.Parser;
import kangdb.common.Error;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author ykangli
 * @version 1.0
 * @date 2022/1/27 20:47
 */
public class TransactionManagerImpl implements TransactionManager {

    /**
     * XID文件头长度，记录了这个 XID 文件管理的事务的个数
     */
    static final int LEN_XID_HEADER_LENGTH = 8;

    /**
     * 每个事务的占用长度
     */
    private static final int XID_FIELD_SIZE = 1;

    /**
     * 事务正在进行，尚未结束
     */
    private static final byte FIELD_TRAN_ACTIVE = 0;

    /**
     * 事务已提交
     */
    private static final byte FIELD_TRAN_COMMITTED = 1;

    /**
     * 事务回滚
     */
    private static final byte FIELD_TRAN_ABORTED = 2;

    /**
     * 超级事务，永远为 commited 状态
     */
    public static final long SUPER_XID = 0;

    /**
     * XID 文件后缀
     */
    static final String XID_SUFFIX = ".xid";

    private RandomAccessFile file;

    private FileChannel fc;

    /**
     * XID 文件管理的事务的个数
     */
    private long xidCounter;

    private Lock counterLock;

    public TransactionManagerImpl(RandomAccessFile file, FileChannel fc) {
        this.file = file;
        this.fc = fc;
        this.counterLock = new ReentrantLock();
        checkXIDCounter();
    }

    /**
     * 检查XID文件是否合法
     * 读取XID_FILE_HEADER中的xidcounter，根据它计算文件的理论长度，对比实际长度
     */
    private void checkXIDCounter() {
        long fileLen = 0;
        try {
            //文件的实际长度
            fileLen = file.length();
        } catch (IOException e1) {
            Panic.panic(Error.BadXIDFileException);
        }
        if (fileLen < LEN_XID_HEADER_LENGTH) {
            Panic.panic(Error.BadXIDFileException);
        }
        //从堆空间中分配一个容量大小为 LEN_XID_HEADER_LENGTH 的byte数组作为缓冲区的byte数据存储器
        ByteBuffer buf = ByteBuffer.allocate(LEN_XID_HEADER_LENGTH);
        try {
            fc.position(0);
            fc.read(buf);
        } catch (IOException e) {
            Panic.panic(e);
        }
        //初始时 xidCounter应该为0，刚开始没有事务开启
        this.xidCounter = Parser.parseLong(buf.array());
        //理论上文件的长度
        long end = getXidPosition(this.xidCounter + 1);
        if (end != fileLen) {
            Panic.panic(Error.BadXIDFileException);
        }
    }

    /**
     * 根据事务xid取得其在xid文件中对应的位置
     */
    private long getXidPosition(long xid) {
        return LEN_XID_HEADER_LENGTH + (xid - 1) * XID_FIELD_SIZE;
    }

    /**
     * 更新xid事务的状态为status
     */
    private void updateXID(long xid, byte status) {
        long offset = getXidPosition(xid);
        byte[] bytes = new byte[XID_FIELD_SIZE];
        bytes[0] = status;
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        try {
            fc.position(offset);
            fc.write(buffer);
        } catch (IOException e) {
            Panic.panic(e);
        }
        try {
            //nio中方法，强制同步缓存内容到文件中
            fc.force(false);
        } catch (IOException e) {
            Panic.panic(e);
        }
    }

    /**
     * 将XID加一，并更新XID Header
     */
    private void incrXIDCounter() {
        xidCounter++;
        ByteBuffer buffer = ByteBuffer.wrap(Parser.long2Byte(xidCounter));
        try {
            fc.position(0);
            fc.write(buffer);
        } catch (IOException e) {
            Panic.panic(e);
        }
        try {
            fc.force(false);
        } catch (IOException e) {
            Panic.panic(e);
        }
    }

    /**
     * 检测XID事务是否处于status状态
     */
    private boolean checkXID(long xid, byte status) {
        //xid文件中存储该事务状态的位置
        long xidPosition = getXidPosition(xid);
        ByteBuffer buffer = ByteBuffer.wrap(new byte[XID_FIELD_SIZE]);
        try {
            fc.position(0);
            //读取该位置的这个字节（该事务的实际状态）
            fc.read(buffer);
        } catch (IOException e) {
            Panic.panic(e);
        }
        return buffer.array()[0] == status;
    }

    /**
     * 开启事务
     *
     * @return 事务的xid
     */
    @Override
    public long begin() {
        //获取锁
        counterLock.lock();
        //事务的 XID 从 1 开始标号，并自增，不可重复。
        try {
            long xid = xidCounter + 1;
            //设置当前事务状态为 0
            updateXID(xid, FIELD_TRAN_ACTIVE);
            //改变xid文件Header
            incrXIDCounter();
            return xid;
        } finally {
            //释放锁
            counterLock.unlock();
        }
    }

    /**
     * 提交xid事务
     *
     * @param xid 事务的xid
     */
    @Override
    public void commit(long xid) {
        updateXID(xid, FIELD_TRAN_COMMITTED);
    }

    @Override
    public void abort(long xid) {
        updateXID(xid, FIELD_TRAN_ABORTED);
    }

    @Override
    public boolean isActive(long xid) {
        //xid == 0 为超级事务
        if(xid == SUPER_XID) {
            return false;
        }
        return checkXID(xid, FIELD_TRAN_ACTIVE);
    }

    @Override
    public boolean isCommitted(long xid) {
        //xid == 0 为超级事务
        if(xid == SUPER_XID) {
            return false;
        }
        return checkXID(xid, FIELD_TRAN_COMMITTED);
    }

    @Override
    public boolean isAborted(long xid) {
        //xid == 0 为超级事务
        if(xid == SUPER_XID) {
            return false;
        }
        return checkXID(xid, FIELD_TRAN_ABORTED);
    }

    @Override
    public void close() {
        try {
            fc.close();
            file.close();
        } catch (IOException e) {
            Panic.panic(e);
        }
    }
}
