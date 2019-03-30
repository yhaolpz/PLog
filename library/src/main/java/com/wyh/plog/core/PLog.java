package com.wyh.plog.core;

import android.app.Application;
import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import com.wyh.plog.upload.UploadListener;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by wyh on 2019/3/10.
 */
public final class PLog {

    //未初始化
    private static boolean sNotInit = true;

    private PLog() {

    }

    /**
     * application create 时初始化
     *
     * @param config 配置
     */
    public static void init(PLog.Config config) {
        if (config == null) {
            throw new IllegalArgumentException("PLogManagerImpl config is null");
        }
        if (!sNotInit) {
            throw new IllegalArgumentException("PLogManagerImpl Already Init");
        }
        sNotInit = false;
        PLogManagerImpl.getInstance().init(config);
    }

    public static void v(@NonNull String tag, @NonNull String msg, @Nullable Object... args) {
        if (sNotInit) {
            return;
        }
        PLogManagerImpl.getInstance().v(tag, msg, args);
    }

    public static void d(@NonNull String tag, @NonNull String msg, @Nullable Object... args) {
        if (sNotInit) {
            return;
        }
        PLogManagerImpl.getInstance().d(tag, msg, args);
    }

    public static void d(@NonNull String tag, @NonNull Object obj) {
        if (sNotInit) {
            return;
        }
        PLogManagerImpl.getInstance().d(tag, obj);
    }

    public static void i(@NonNull String tag, @NonNull String msg, @Nullable Object... args) {
        if (sNotInit) {
            return;
        }
        PLogManagerImpl.getInstance().i(tag, msg, args);
    }

    public static void w(@NonNull String tag, @NonNull String msg, @Nullable Object... args) {
        if (sNotInit) {
            return;
        }
        PLogManagerImpl.getInstance().w(tag, msg, args);
    }

    public static void e(@NonNull String tag, @NonNull String msg, @Nullable Object... args) {
        if (sNotInit) {
            return;
        }
        PLogManagerImpl.getInstance().e(tag, msg, args);
    }

    public static void e(@Nullable Throwable tr, @NonNull String tag, @Nullable String msg, @Nullable Object... args) {
        if (sNotInit) {
            return;
        }
        PLogManagerImpl.getInstance().e(tr, tag, msg, args);
    }

    /**
     * 只写入到日志文件
     */
    public static void record(@NonNull String tag, @NonNull String msg) {
        record(DebugLevel.DEBUG, tag, msg);
    }

    /**
     * 只写入到日志文件
     */
    public static void record(@DebugLevel.Level int level, @NonNull String tag, @NonNull String msg) {
        if (sNotInit) {
            return;
        }
        PLogManagerImpl.getInstance().record(level, tag, msg);
    }

    /**
     * 只输出到logcat
     */
    public static void print(@NonNull String tag, @NonNull String msg, @Nullable Object... args) {
        if (sNotInit) {
            return;
        }
        PLogManagerImpl.getInstance().print(tag, msg, args);
    }

    /**
     * 只输出到logcat
     */
    public static void print(@NonNull String tag, @NonNull Object obj) {
        if (sNotInit) {
            return;
        }
        PLogManagerImpl.getInstance().print(tag, obj);
    }

    /**
     * 只输出到logcat
     */
    public static void print(@PLog.DebugLevel.Level int level, @NonNull String tag, @NonNull String msg) {
        if (sNotInit) {
            return;
        }
        PLogManagerImpl.getInstance().print(level, tag, msg);
    }

    /**
     * 上传
     * 注意：建议设置上传监听并在上传成功后及时删除日志文件
     */
    public static void upload(@Nullable UploadListener listener) {
        if (sNotInit) {
            return;
        }
        PLogManagerImpl.getInstance().upload(listener);
    }


    public static final class Config {
        private final Context application;
        private final String gid;
        @DebugLevel.Level
        private final int logcatDebugLevel;
        @DebugLevel.Level
        private final int recordDebugLevel;
        private final String cipherKey;
        private String logDir;

        @Override
        public String toString() {
            return "Config{" +
                    "application=" + application +
                    ", gid='" + gid + '\'' +
                    ", logcatDebugLevel=" + logcatDebugLevel +
                    ", recordDebugLevel=" + recordDebugLevel +
                    ", cipherKey='" + cipherKey + '\'' +
                    ", logDir='" + logDir + '\'' +
                    '}';
        }

        private Config(final Builder builder) {
            this.application = builder.application;
            this.gid = builder.gid;
            this.logDir = builder.logDir;
            this.logcatDebugLevel = builder.logcatDebugLevel;
            this.recordDebugLevel = builder.recordDebugLevel;
            this.cipherKey = builder.cipherKey;
        }

        public String getGid() {
            return gid;
        }

        public String getLogDir() {
            return logDir;
        }

        public Context getContext() {
            return application;
        }

        public int getLogcatDebugLevel() {
            return logcatDebugLevel;
        }

        public int getRecordDebugLevel() {
            return recordDebugLevel;
        }

        public String getCipherKey() {
            return cipherKey;
        }

        void setLogDir(String logDir) {
            this.logDir = logDir;
        }

        public static final class Builder {
            private Application application;
            private String gid;
            private String logDir;
            private String cipherKey;
            @DebugLevel.Level
            private int logcatDebugLevel;
            @DebugLevel.Level
            private int recordDebugLevel;

            public Builder(Application application) {
                this.application = application;
            }

            public Builder gid(String gid) {
                this.gid = gid;
                return this;
            }

            /**
             * 日志存储路径，默认会先选择sd卡，失败则选择cache目录
             */
            public Builder logDir(String logDir) {
                this.logDir = logDir;
                return this;
            }

            /**
             * 允许输出到logcat的最低debug级别
             */
            public Builder logcatDebugLevel(@DebugLevel.Level int level) {
                this.logcatDebugLevel = level;
                return this;
            }

            /**
             * 允许记录到文件的最低debug级别, 对于record来说NONE不只会禁用记录，也会禁用上传操作
             */
            public Builder recordDebugLevel(@DebugLevel.Level int level) {
                this.recordDebugLevel = level;
                return this;
            }

            /**
             * 日志密钥
             */
            public Builder cipherKey(String cipherKey) {
                this.cipherKey = cipherKey;
                return this;
            }

            public Config build() {
                if (application == null) {
                    throw new IllegalArgumentException("application == null");
                }
                return new Config(this);
            }

        }
    }


    public static final class DebugLevel {
        public static final int ALL = -1;
        public static final int VERBOSE = 0;
        public static final int DEBUG = 1;
        public static final int INFO = 2;
        public static final int WARNING = 3;
        public static final int ERROR = 4;
        //对于record来说NONE不只会禁用记录，也会禁用上传操作
        public static final int NONE = 5;

        @IntDef({ALL, VERBOSE, DEBUG, INFO, WARNING, ERROR, NONE})
        @Retention(RetentionPolicy.SOURCE)
        public @interface Level {
        }
    }

}
