package com.wyh.example;

import android.app.Application;

import com.wyh.plog.core.PLog;

/**
 * Created by wyh on 2019/3/30.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initPLog();
    }

    private void initPLog() {
        PLog.Config config = new PLog.Config.Builder(this)
                .logcatDebugLevel(PLog.DebugLevel.DEBUG)
                .recordDebugLevel(PLog.DebugLevel.DEBUG)
                .fileSizeLimitDay(15)
                .overdueDay(3)
                .cipherKey("123456")
                .build();
        PLog.init(config);
    }
}
