package com.wyh.plog.record.impl;

import android.text.TextUtils;

import com.wyh.plog.core.PLog;
import com.wyh.plog.core.PLogConstant;
import com.wyh.plog.core.PLogExecutor;
import com.wyh.plog.core.PLogPrint;
import com.wyh.plog.core.PLogTag;
import com.wyh.plog.helper.FileHelper;
import com.wyh.plog.helper.PermissionHelper;
import com.wyh.plog.record.LogFormatter;
import com.wyh.plog.record.LogRecorder;
import com.wyh.plog.record.LogWriter;
import com.wyh.plog.upload.PrepareUploadListener;
import com.wyh.plog.util.AppUtil;
import com.wyh.plog.util.DateUtil;
import com.wyh.plog.util.DeviceUtil;


/**
 * Created by wyh on 2019/3/13.
 */
public class LogRecorderImpl implements LogRecorder {


    private LogWriter logWriter;
    private LogFormatter logFormatter;
    private PLog.Config config;
    @PLog.DebugLevel.Level
    private int mDebugLevel;
    private long mPartFileSizeLimit;

    public LogRecorderImpl(final PLog.Config config) {
        this.mDebugLevel = config.recordDebugLevel;
        this.logFormatter = new LogFormatterImpl();
        this.config = config;
        this.mPartFileSizeLimit = config.fileSizeLimitDayByte / 3;
        PLogExecutor.executeDisk(new Runnable() {
            @Override
            public void run() {
                if (!PermissionHelper.hasWriteAndReadStoragePermission(config.application)) {
                    PLogPrint.e(PLogTag.INTERNAL_TAG, "LogRecorderImpl-->!hasWriteAndReadStoragePermission");
                    return;
                }
                tryInitLogWriter();
            }
        });
    }


    private synchronized void tryInitLogWriter() {
        if (null != logWriter) {
            return;
        }
        PLogPrint.d(PLogTag.INTERNAL_TAG, "tryInitLogWriter-->dirPath=" + config.getLogDir());
        try {
            MmapLogWriter mmapLogWriter = new MmapLogWriter();
            String basicInfo = logFormatter.format(PLog.DebugLevel.DEBUG, PLogTag.INTERNAL_TAG, getBasicInfo(config));
            PLogPrint.d(PLogTag.INTERNAL_TAG, "tryInitLogWriter-->basicInfo=" + basicInfo);
            mmapLogWriter.init(config.application, basicInfo, config.getLogDir(), null);
            logWriter = mmapLogWriter;
        } catch (Throwable ex) {
            ex.printStackTrace();
            PLogPrint.e(PLogTag.INTERNAL_TAG, "tryInitLogWriter-->init MmapLogWriter error:" + ex.toString());
        }
    }

    @Override
    public void record(final int debugLevel, final String tag, final String msg) {
        if (debugLevel < mDebugLevel || TextUtils.isEmpty(tag) || TextUtils.isEmpty(msg)) {
            return;
        }
        if (!PermissionHelper.hasWriteAndReadStoragePermission(config.application)) {
            PLogPrint.e(PLogTag.INTERNAL_TAG, "log-->!hasWriteAndReadStoragePermission");
            return;
        }
        PLogExecutor.executeDisk(new Runnable() {
            @Override
            public void run() {
                checkInitAndRecordSync(logFormatter.format(debugLevel, tag, msg));
            }
        });
    }


    /**
     * check whether logWriter is initialized or not firstly.
     * then write the content to log file.
     *
     * @param msgContent
     */
    private void checkInitAndRecordSync(String msgContent) {
        if (TextUtils.isEmpty(msgContent)) {
            return;
        }
        tryInitLogWriter();
        try {
            logWriter.write(msgContent, mPartFileSizeLimit);
        } catch (Throwable ex) {
            PLogPrint.e(PLogTag.INTERNAL_TAG, "write " + ex.toString());
            ex.printStackTrace();
            tryWriteLog(msgContent);
        }
    }


    private void tryWriteLog(String content) {
        try {
            logWriter.write(content, mPartFileSizeLimit);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private String getBasicInfo(PLog.Config config) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Android")
                .append(PLogConstant.FIELD_SEPERATOR)
                .append(AppUtil.getCurProcessName(config.application))
                .append(PLogConstant.FIELD_SEPERATOR)
                .append(AppUtil.getVersionName(config.application))
                .append(PLogConstant.FIELD_SEPERATOR)
                .append("~")
                .append(PLogConstant.FIELD_SEPERATOR)
                .append(DeviceUtil.getDeviceInfo())
                .append(PLogConstant.FIELD_SEPERATOR)
                .append(DeviceUtil.isRoot() ? 1 : 0);
        return stringBuilder.toString();
    }


    @Override
    public void prepareUploadAsync(final PrepareUploadListener listener) {
        if (listener == null) {
            return;
        }
        if (!PermissionHelper.hasWriteAndReadStoragePermission(config.application)) {
            PLogPrint.e(PLogTag.INTERNAL_TAG, "prepareUploadAsync-->!hasWriteAndReadStoragePermission");
            PLogExecutor.executeMain(new Runnable() {
                @Override
                public void run() {
                    listener.failToReady();
                }
            });
            return;
        }

        PLogExecutor.executeDisk(new Runnable() {
            @Override
            public void run() {

                tryInitLogWriter();
                logWriter.closeAndRenew(true);

                final String writeFileName = DateUtil.getDate() + PLogConstant.MMAP;
                // avoid to block write operation, we just rename except the writing log file, have not compress log file
                FileHelper.renameToUpAllIfNeed(writeFileName, config.getLogDir(), config.overdueDayMs);

                PLogExecutor.executeMain(new Runnable() {
                    @Override
                    public void run() {
                        listener.readyToUpload();
                    }
                });
            }
        });
    }

}
