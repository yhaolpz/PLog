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
import com.wyh.plog.internal.PLogLifecycle;
import com.wyh.plog.internal.PLogReceiver;
import com.wyh.plog.record.LogRecorder;
import com.wyh.plog.record.impl.LogRecorderImpl;
import com.wyh.plog.upload.UploadListener;
import com.wyh.plog.upload.PrepareUploadListener;
import com.wyh.plog.upload.impl.LogUploaderImpl;
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

    public static PLogManagerImpl getInstance() {
        return INSTANCE;
    }

    private Context mAppContext;
    private PLog.Config mConfig;
    private LogRecorder mLogRecorder;
    private PLogReceiver mPLogReceiver;

    void init(@NonNull PLog.Config config) {
        PLogPrint.setDebugLevel(config.getLogcatDebugLevel());
        PLogPrint.i(PLogTag.INTERNAL_TAG, "PLogManagerImpl-->init " + config.toString());
        mConfig = config;
        mAppContext = mConfig.getContext();
        if (TextUtils.isEmpty(mConfig.getLogDir())) {
            mConfig.setLogDir(FileHelper.getDefaultLogDir(mAppContext).getAbsolutePath());
            PLogPrint.i(PLogTag.INTERNAL_TAG, "PLogManagerImpl-->init FileHelper.getDefaultLogDir=" + mConfig.getLogDir());
        } else {
            mConfig.setLogDir(mConfig.getLogDir() + File.separator + AppUtil.getCurProcessName(mAppContext));
            File dirFile = new File(mConfig.getLogDir());
            if (!dirFile.exists() || !dirFile.isDirectory()) {
                dirFile.mkdirs();
            }
            PLogPrint.i(PLogTag.INTERNAL_TAG, "PLogManagerImpl-->init LogDir=" + mConfig.getLogDir());
        }
        PLogExecutor.executeDisk(new Runnable() {
            @Override
            public void run() {
                FileHelper.cleanOverdueLog(mAppContext, mConfig.getLogDir());
            }
        });
        PLogLifecycle.init(mAppContext);
        destroyReceiver();
        registerReceiver(mConfig.getContext());
        mLogRecorder = new LogRecorderImpl(mConfig);
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
        if (!NetUtil.isNetworkAvailable(mConfig.getContext())) {
            PLogPrint.e(PLogTag.INTERNAL_TAG, "upload--> Network not Available !");
            return;
        }
        if (mConfig.getRecordDebugLevel() == PLog.DebugLevel.NONE) {
            PLogPrint.e(PLogTag.INTERNAL_TAG, "upload--> getRecordDebugLevel() == PLog.DebugLevel.NONE !");
            return;
        }
        mLogRecorder.prepareUploadAsync(new PrepareUploadListener() {
            @Override
            public void readyToUpload() {
                PLogPrint.d(PLogTag.INTERNAL_TAG, "upload-->prepareUploadAsync readyToUpload");
                List<File> zipFiles = FileHelper.filterExistsZipFiles(mConfig.getLogDir());
                PLogPrint.d(PLogTag.INTERNAL_TAG, "upload-->prepareUploadAsync existsZipFiles size=" + zipFiles.size());
                final File newZipFile = FileHelper.zipAllUpLogFile(mConfig.getContext(), mConfig.getLogDir(), mConfig.getCipherKey());
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
        if (mAppContext != null && mPLogReceiver != null) {
            mAppContext.unregisterReceiver(mPLogReceiver);
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
        mAppContext.registerReceiver(mPLogReceiver, filter);
    }


}