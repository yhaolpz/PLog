package com.wyh.plog.core;

/**
 * Created by wyh on 2019/3/12.
 */
public final class PLogConstant {
    public static final long FORMAT_DAY_MS = 24 * 3600 * 1000L;
    // 日志存放目录
    public static final String LOG_DIR = "plog_log";
    // 间隔的多少条日志条数判断一次是否超过文件上限
    public static final int LOG_INTERVAL_LOG_NUM = 1000;
    //可存放到sd卡目录的sd卡剩余空间下限，小于此值时采用cache目录
    public static final long MIN_SDCARD_FREE_SPACE_MB = 50;
    // mmap日志文件后缀
    public static final String MMAP = "-mmap";
    // 准备好上传的日志文件后缀
    public static final String UP = "-up";
    // 压缩后的日志文件后缀
    public static final String ZIP = ".zip";
    // 打印内存单位
    public static final String MB = "MB";
    public static final int FORMAT_MB = 1024 * 1024;
    // 日志分隔符号
    public static final String FIELD_SEPERATOR = " ";

    public static final int MIN_THREAD_NUM = 10;

}
