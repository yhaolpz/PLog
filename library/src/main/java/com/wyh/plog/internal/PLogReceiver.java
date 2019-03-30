package com.wyh.plog.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.BatteryManager;

import com.wyh.plog.core.PLog;
import com.wyh.plog.core.PLogExecutor;
import com.wyh.plog.core.PLogTag;
import com.wyh.plog.helper.PerformanceHelper;
import com.wyh.plog.util.NetUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by wyh on 2019/3/12.
 */

public class PLogReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        final String action = intent.getAction();
        if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
            showBatteryState(intent);
        } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            showNetworkType(context);
        } else if (Intent.ACTION_TIME_TICK.equals(action)) {
            PLogExecutor.executeDisk(new Runnable() {
                @Override
                public void run() {
                    //每分钟记录一次
                    showAppState(context);
                }
            });
        }
    }


    /**
     * 记录内存、线程等使用状态
     */
    private void showAppState(Context context) {
        PerformanceHelper.recordMemory(context);
        PerformanceHelper.recordThread();
    }


    /**
     * 记录网络状态变化
     */
    private void showNetworkType(Context context) {
        String networkType = NetUtil.getNetworkType(context.getApplicationContext());
        PLog.d(PLogTag.NETWORK_TAG, "net-->" + networkType);
    }

    /**
     * 记录电池电量
     */
    private void showBatteryState(Intent intent) {
        int status = intent.getIntExtra("status", 0);
        int level = intent.getIntExtra("level", 0);
        String statusResult = "discharging";
        switch (status) {
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
                statusResult = "discharging";
                break;
            case BatteryManager.BATTERY_STATUS_CHARGING:
                statusResult = "charging";
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                statusResult = "discharging";
                break;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                statusResult = "discharging";
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                statusResult = "charging";
                break;
        }
        List<String> msgList = new LinkedList<>();
        msgList.add(String.valueOf((level * 1.00 / 100)));
        msgList.add(statusResult);
//        PLog.d(PLogTag.BATTERY_TAG, msgList);
    }

}
