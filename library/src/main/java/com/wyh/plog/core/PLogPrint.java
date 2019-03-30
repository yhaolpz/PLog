package com.wyh.plog.core;

import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Created by wyh on 2019/3/20.
 */
public final class PLogPrint {

    private PLogPrint() {
    }

    @PLog.DebugLevel.Level
    private static int sDebugLevel = PLog.DebugLevel.DEBUG;

    public static void setDebugLevel(int debugLevel) {
        PLogPrint.sDebugLevel = debugLevel;
    }

    public static void v(@NonNull String tag, @NonNull String msg) {
        print(PLog.DebugLevel.VERBOSE, tag, msg);
    }

    public static void d(@NonNull String tag, @NonNull String msg) {
        print(PLog.DebugLevel.DEBUG, tag, msg);
    }

    public static void i(@NonNull String tag, @NonNull String msg) {
        print(PLog.DebugLevel.INFO, tag, msg);
    }

    public static void w(@NonNull String tag, @NonNull String msg) {
        print(PLog.DebugLevel.WARNING, tag, msg);
    }

    public static void e(@NonNull String tag, @NonNull String msg) {
        print(PLog.DebugLevel.ERROR, tag, msg);
    }

    public static void print(@PLog.DebugLevel.Level int level, @NonNull String tag, @NonNull String msg) {
        if (level >= sDebugLevel) {
            switch (level) {
                case PLog.DebugLevel.VERBOSE:
                    Log.v(tag, msg);
                    break;
                case PLog.DebugLevel.DEBUG:
                    Log.d(tag, msg);
                    break;
                case PLog.DebugLevel.INFO:
                    Log.i(tag, msg);
                    break;
                case PLog.DebugLevel.WARNING:
                    Log.w(tag, msg);
                    break;
                case PLog.DebugLevel.ERROR:
                    Log.e(tag, msg);
                    break;
                case PLog.DebugLevel.ALL:
                case PLog.DebugLevel.NONE:
                    break;
            }
        }
    }

}
