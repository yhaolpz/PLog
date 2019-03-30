package com.wyh.plog.record.impl;

import android.content.Context;

import com.wyh.plog.core.PLogConstant;
import com.wyh.plog.core.PLogPrint;
import com.wyh.plog.core.PLogTag;
import com.wyh.plog.record.LogWriter;
import com.wyh.plog.util.DateUtil;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wyh on 2019/3/10.
 * mmap方式写入
 */
public class MmapLogWriter implements LogWriter {

    static {
        try {
            System.loadLibrary("plog-lib");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private native long nativeInit(String basicInfo, String dir);

    private native long nativeWrite(long logWriterObject, String msgContent);

    private native long nativeGetFileSize(long logWriterObject);

    private native void nativeCloseAndRenew(long logWriterObject, boolean uploadAction);

    private final AtomicBoolean initFlag = new AtomicBoolean(false);

    private final AtomicInteger writeNum = new AtomicInteger(0);

    //C++ LogWriter对象的句柄
    private long nativeLogWriter;

    private String logFileDir;
    private String buildDate;
    private File logFile;

    @Override
    public void init(Context context, final String basicInfoContent, final String dir, String key) throws Throwable {
        logFileDir = dir;
        buildDate = DateUtil.getDate();
        nativeLogWriter = nativeInit(basicInfoContent, dir);
        initFlag.set(true);
        logFile = new File(logFileDir + File.separator + buildDate + PLogConstant.MMAP);
    }

    @Override
    public void write(String content) throws Exception {
        if (nativeLogWriter <= 0) {
            return;
        }
        if (!initFlag.get()) {
            return;
        }
        // 判断写入的时候日期是否是当天，判断日志文件是否存在
        if (!DateUtil.getDate().equals(buildDate) || !isLogFileExist()) {
            // 确保文件目录存在，以防被手动删除
            File dir = new File(logFileDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            buildDate = DateUtil.getDate();
            closeAndRenew(false);
            logFile = new File(logFileDir + File.separator + buildDate + PLogConstant.MMAP);
        }

        // 定期检查是否超过文件大小上限，超过的话就 closeAndRenew
        if (writeNum.getAndIncrement() > PLogConstant.LOG_INTERVAL_LOG_NUM) {
            writeNum.set(0);
            long fileSize = nativeGetFileSize(nativeLogWriter);
            PLogPrint.d(PLogTag.INTERNAL_TAG, "write--> writeNum>LOG_INTERVAL_LOG_NUM, nativeGetFileSize=" + fileSize);
            if (fileSize > PLogConstant.LOG_PART_FILE_SIZE_LIMIT) {
                PLogPrint.d(PLogTag.INTERNAL_TAG, "write-->file size beyond LOG_PART_FILE_SIZE_LIMIT, closeAndRenew");
                closeAndRenew(false);
                logFile = new File(logFileDir + File.separator + buildDate + PLogConstant.MMAP);
            }
        }

        nativeWrite(nativeLogWriter, content);
    }


    /**
     * 这个其实是有两个用处，第一个用处当然是上传时;
     * 第二个用处是如果发现当前日期和现在日期不一样，也要进行这样的操作。
     */
    @Override
    public void closeAndRenew(boolean uploadAction) {
        if (nativeLogWriter <= 0) {
            return;
        }
        nativeCloseAndRenew(nativeLogWriter, uploadAction);
    }

    @Override
    public boolean isLogFileExist() {
        return logFile != null && logFile.exists();
    }

}
