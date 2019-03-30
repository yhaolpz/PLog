package com.wyh.plog.util;

import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by wyh on 2019/3/13.
 */
public class DeviceUtil {
    //todo 不需要这些信息
    public static String getDeviceModel() {
        return Build.MODEL;
    }

    public static final int getSDKInt() {
        return Build.VERSION.SDK_INT;
    }

    public static final String getDeviceInfo() {
        return getDeviceModel() + "," + getSDKInt();
    }

    /**
     * To get whether the device is root or not
     */
    public static boolean isRoot() {
        String binPath = "/system/bin/su";
        String xBinPath = "/system/xbin/su";
        return (new File(binPath)).exists() && isExecutable(binPath) || (new File(xBinPath)).exists() && isExecutable(xBinPath);
    }

    private static boolean isExecutable(String filePath) {
        Process p = null;
        BufferedReader in = null;

        try {
            p = Runtime.getRuntime().exec("ls -l " + filePath);
            in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String e = in.readLine();
            if (e == null || e.length() < 4) {
                return false;
            }
            char flag = e.charAt(3);
            if (flag != 115 && flag != 120) {
                return false;
            }
            return true;
        } catch (IOException var16) {
            var16.printStackTrace();
            return false;
        } finally {
            if (p != null) {
                p.destroy();
            }
            IOUtil.closeQuietly(in);
        }
    }
}
