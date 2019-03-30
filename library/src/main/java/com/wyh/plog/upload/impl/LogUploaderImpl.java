package com.wyh.plog.upload.impl;

import android.support.annotation.NonNull;

import com.wyh.plog.core.PLog;
import com.wyh.plog.core.PLogPrint;
import com.wyh.plog.core.PLogTag;
import com.wyh.plog.upload.UploadListener;

import java.io.File;
import java.util.List;

/**
 * Created by wyh on 2019/3/13.
 */
public class LogUploaderImpl implements UploadListener {

    private PLog.Config mConfig;


    public LogUploaderImpl(PLog.Config config) {
        mConfig = config;
    }


    @Override
    public void upload(@NonNull final List<File> files) {
        for (File file : files) {
            PLogPrint.d(PLogTag.INTERNAL_TAG, "LogUploaderImpl-->upload file:" + file.getAbsolutePath());
        }
    }
}