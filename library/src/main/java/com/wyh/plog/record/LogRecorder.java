package com.wyh.plog.record;

import com.wyh.plog.core.PLog;
import com.wyh.plog.upload.PrepareUploadListener;

public interface LogRecorder {

    /**
     * 准备日志文件，即将上传
     *
     * @param listener 准备结果
     */
    void prepareUploadAsync(PrepareUploadListener listener);

    /**
     * 写入文件
     */
    void record(@PLog.DebugLevel.Level int debugLevel, String tag, String msg);
}
