package com.wyh.plog.upload;

/**
 * Created by wyh on 2019/3/13.
 * 监听上传前文件是否已准备好
 */
public interface PrepareUploadListener {
    /**
     * 文件已准备好，可以上传
     */
    void readyToUpload();

    /**
     * 文件准备出错，不用上传了
     */
    void failToReady();
}
