package com.wyh.plog.internal;

import com.wyh.plog.core.PLog;
import com.wyh.plog.core.PLogTag;

/**
 * Created by wyh on 2019/3/12.
 */

public class PLogExceptionHandler {

    public static void logUncaughtException(Thread thread, Throwable throwable) {
        PLog.e(PLogTag.EXCEPTION_TAG, "Exception occurred in Thread " + thread.getName());
        PLog.e(PLogTag.EXCEPTION_TAG, "Exception type: " + throwable.getClass().getName());
        PLog.e(PLogTag.EXCEPTION_TAG, "Exception message: " + throwable.getMessage());
        StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            String stackBuilder = " " +
                    stackTraceElement.getClassName() +
                    " " +
                    stackTraceElement.getMethodName() +
                    "(" +
                    stackTraceElement.getFileName() +
                    ":" +
                    stackTraceElement.getLineNumber() +
                    ")";
            PLog.e(PLogTag.EXCEPTION_TAG, stackBuilder);
        }
    }
}
