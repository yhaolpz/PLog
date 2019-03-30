package com.wyh.plog.core;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.wyh.plog.upload.UploadListener;

/**
 * Created by wyh on 2019/3/20.
 * 对外接口形式
 */
interface PLogManager {

    void v(@NonNull String tag, @NonNull String msg, @Nullable Object... args);

    void d(@NonNull String tag, @NonNull String msg, @Nullable Object... args);

    void d(@NonNull String tag, @NonNull Object obj);

    void i(@NonNull String tag, @NonNull String msg, @Nullable Object... args);

    void w(@NonNull String tag, @NonNull String msg, @Nullable Object... args);

    void e(@NonNull String tag, @NonNull String msg, @Nullable Object... args);

    void e(@Nullable Throwable tr, @NonNull String tag, @Nullable String msg, @Nullable Object... args);

    void record(@PLog.DebugLevel.Level int level, @NonNull String tag, @NonNull String msg);

    void print(@NonNull String tag, @NonNull String msg, @Nullable Object... args);

    void print(@NonNull String tag, @NonNull Object obj);

    void print(@PLog.DebugLevel.Level int level, @NonNull String tag, @NonNull String msg);

    void upload(@Nullable UploadListener listener);
}


