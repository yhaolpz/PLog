package com.wyh.plog.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

/**
 * Created by wyh on 2019/3/10.
 */

public class AppUtil {

    private static String sVersionName;
    private static String sCurProcessName;

    @NonNull
    public static String getVersionName(@NonNull Context context) {
        if (sVersionName != null) {
            return sVersionName;
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            return sVersionName = packageInfo.versionName;
        } catch (Exception ex) {
            ex.printStackTrace();
            return sVersionName = "UNKNOWN";
        }
    }

    @NonNull
    public static String getCurProcessName(@NonNull Context context) {
        if (sCurProcessName != null) {
            return sCurProcessName;
        }
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            final int pid = android.os.Process.myPid();
            for (ActivityManager.RunningAppProcessInfo appProcess : activityManager
                    .getRunningAppProcesses()) {
                if (appProcess.pid == pid) {
                    return sCurProcessName = appProcess.processName;
                }
            }
        }
        return sCurProcessName = "UNKNOWN";
    }

}
