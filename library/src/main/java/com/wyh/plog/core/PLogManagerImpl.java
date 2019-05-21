package com.wyh.plog.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.wyh.plog.helper.FileHelper;
import com.wyh.plog.internal.PLogExceptionHandler;
import com.wyh.plog.internal.PLogLifecycle;
import com.wyh.plog.internal.PLogReceiver;
import com.wyh.plog.record.LogRecorder;
import com.wyh.plog.record.impl.LogRecorderImpl;
import com.wyh.plog.upload.PrepareUploadListener;
import com.wyh.plog.upload.UploadListener;
import com.wyh.plog.util.AppUtil;
import com.wyh.plog.util.LogUtil;
import com.wyh.plog.util.NetUtil;

import java.io.File;
import java.util.List;

/**
 * Created by wyh on 2019/3/13.
 */
enum PLogManagerImpl implements PLogManager {
    @SuppressLint("StaticFieldLeak") INSTANCE;

    private PLog.Config mConfig;
    private LogRecorder mLogRecorder;
    private PLogReceiver mPLogReceiver;

    public static PLogManagerImpl getInstance() {
        return INSTANCE;
    }

    private boolean initialized() {
        return mConfig != null;
    }

    void init(PLog.Config config) {
        if (initialized()) {
            throw new IllegalArgumentException("PLogManagerImpl Already Init");
        }
        if (config == null) {
            throw new IllegalArgumentException("PLogManagerImpl config is null");
        }
        mConfig = config;
        PLogPrint.i(PLogTag.INTERNAL_TAG, "PLogManagerImpl-->init " + config.toString());
        if (!initLogDir()) {
            PLogPrint.e(PLogTag.INTERNAL_TAG, "PLogManagerImpl-->initLogDir fail ");
            return;
        }
        PLogPrint.i(PLogTag.INTERNAL_TAG, "PLogManagerImpl-->init LogDir=" + mConfig.getLogDir());
        PLogExecutor.executeDisk(new Runnable() {
            @Override
            public void run() {
                FileHelper.cleanOverdueLog(mConfig.application, mConfig.getLogDir(), mConfig.overdueDayMs);
            }
        });
        PLogPrint.setDebugLevel(config.logcatDebugLevel);
        PLogLifecycle.init(mConfig.application);
        destroyReceiver();
        registerReceiver(mConfig.application);
        mLogRecorder = new LogRecorderImpl(mConfig);
        new PLogExceptionHandler();
    }

    private boolean initLogDir() {
        if (TextUtils.isEmpty(mConfig.getLogDir())) {
            File file = FileHelper.getDefaultLogDir(mConfig.application);
            if (FileHelper.isDirExist(file)) {
                mConfig.setLogDir(file.getAbsolutePath());
            } else {
                /* no-op */
            }
        } else {
            mConfig.setLogDir(mConfig.getLogDir() + File.separator
                    + AppUtil.getCurProcessName(mConfig.application));
            File dirFile = new File(mConfig.getLogDir());
            if (FileHelper.isDirExist(dirFile)) {
                /* no-op */
            } else {
                dirFile.mkdirs();
            }
        }
        return FileHelper.isDirExist(mConfig.getLogDir());
    }

    @Override
    public void v(@NonNull String tag, @NonNull String msg, @Nullable Object... args) {
        msg = LogUtil.formatMsg(msg, args);
        PLogPrint.v(tag, msg);
        record(PLog.DebugLevel.VERBOSE, tag, msg);
    }

    @Override
    public void d(@NonNull String tag, @NonNull String msg, @Nullable Object... args) {
        msg = LogUtil.formatMsg(msg, args);
        PLogPrint.d(tag, msg);
        record(PLog.DebugLevel.DEBUG, tag, msg);
    }

    @Override
    public void d(@NonNull String tag, @NonNull Object obj) {
        String msg = LogUtil.toString(obj);
        PLogPrint.d(tag, msg);
        record(PLog.DebugLevel.DEBUG, tag, msg);
    }

    @Override
    public void i(@NonNull String tag, @NonNull String msg, @Nullable Object... args) {
        msg = LogUtil.formatMsg(msg, args);
        PLogPrint.i(tag, msg);
        record(PLog.DebugLevel.INFO, tag, msg);
    }

    @Override
    public void w(@NonNull String tag, @NonNull String msg, @Nullable Object... args) {
        msg = LogUtil.formatMsg(msg, args);
        PLogPrint.w(tag, msg);
        record(PLog.DebugLevel.WARNING, tag, msg);
    }

    @Override
    public void e(@NonNull String tag, @NonNull String msg, @Nullable Object... args) {
        msg = LogUtil.formatMsg(msg, args);
        PLogPrint.e(tag, msg);
        record(PLog.DebugLevel.ERROR, tag, msg);
    }

    @Override
    public void e(@Nullable Throwable tr, @NonNull String tag, @Nullable String msg, @Nullable Object... args) {
        if (TextUtils.isEmpty(msg)) {
            msg = "Empty/NULL log message";
        } else {
            msg = LogUtil.formatMsg(msg, args);
        }
        if (tr != null) {
            msg += " : " + LogUtil.getStackTraceString(tr);
        }
        PLogPrint.e(tag, msg);
        record(PLog.DebugLevel.ERROR, tag, msg);
    }

    @Override
    public void record(@PLog.DebugLevel.Level int level, @NonNull String tag, @NonNull String msg) {
        mLogRecorder.record(level, tag, msg);
    }

    @Override
    public void print(@NonNull String tag, @NonNull String msg, @Nullable Object... args) {
        msg = LogUtil.formatMsg(msg, args);
        PLogPrint.d(tag, msg);
    }

    @Override
    public void print(@NonNull String tag, @NonNull Object obj) {
        String msg = LogUtil.toString(obj);
        PLogPrint.d(tag, msg);
    }

    @Override
    public void print(@PLog.DebugLevel.Level int level, @NonNull String tag, @NonNull String msg) {
        PLogPrint.print(level, tag, msg);
    }


    @Override
    public void upload(@Nullable final UploadListener listener) {
        if (!NetUtil.isNetworkAvailable(mConfig.application)) {
            PLogPrint.e(PLogTag.INTERNAL_TAG, "upload--> Network not Available !");
            return;
        }
        if (mConfig.recordDebugLevel == PLog.DebugLevel.NONE) {
            PLogPrint.e(PLogTag.INTERNAL_TAG, "upload--> getRecordDebugLevel() == PLog.DebugLevel.NONE !");
            return;
        }
        mLogRecorder.prepareUploadAsync(new PrepareUploadListener() {
            @Override
            public void readyToUpload() {
                PLogPrint.d(PLogTag.INTERNAL_TAG, "upload-->prepareUploadAsync readyToUpload");
                List<File> zipFiles = FileHelper.filterExistsZipFiles(mConfig.getLogDir(), mConfig.overdueDayMs);
                PLogPrint.d(PLogTag.INTERNAL_TAG, "upload-->prepareUploadAsync existsZipFiles size=" + zipFiles.size());
                final File newZipFile = FileHelper.zipAllUpLogFile(mConfig.application, mConfig.getLogDir(), mConfig.cipherKey, mConfig.overdueDayMs);
                if (newZipFile == null) {
                    PLogPrint.e(PLogTag.INTERNAL_TAG, "upload-->prepareUploadAsync zipAllUpLogFile newZipFile is null");
                }
                zipFiles.add(newZipFile);
                if (zipFiles.isEmpty()) {
                    PLogPrint.e(PLogTag.INTERNAL_TAG, "upload-->prepareUploadAsync zipFiles isEmpty, Cancel upload !");
                    return;
                }
                for (File file : zipFiles) {
                    PLogPrint.d(PLogTag.INTERNAL_TAG, "upload file:" + file.getAbsolutePath());
                }
                if (listener != null) {
                    listener.upload(zipFiles);
                } else {
                    PLogPrint.e(PLogTag.INTERNAL_TAG, "upload-->You should set up a listener and delete the file immediately after the upload is successful.");
                }
            }

            @Override
            public void failToReady() {
                PLogPrint.e(PLogTag.INTERNAL_TAG, "upload-->prepareUploadAsync failToReady");
            }
        });
    }

    void destroyReceiver() {
        if (mConfig.application != null && mPLogReceiver != null) {
            mConfig.application.unregisterReceiver(mPLogReceiver);
            mPLogReceiver = null;
            PLogPrint.i(PLogTag.INTERNAL_TAG, "PLogManagerImpl-->unregisterReceiver");
        }
    }

    private void registerReceiver(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("registerReceiver-->context == null");
        }
        if (mPLogReceiver != null) {
            return;
        }
        PLogPrint.i(PLogTag.INTERNAL_TAG, "PLogManagerImpl-->new PLogReceiver & registerReceiver");
        mPLogReceiver = new PLogReceiver();
        IntentFilter filter = new IntentFilter();
        //电量变化信息没什么用
//        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_TIME_TICK);
        mConfig.application.registerReceiver(mPLogReceiver, filter);
    }


}