package com.wyh.plog.helper;

import android.content.Context;
import android.os.Debug;

import com.wyh.plog.core.PLog;
import com.wyh.plog.core.PLogConstant;
import com.wyh.plog.core.PLogTag;
import com.wyh.plog.util.LogUtil;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by wyh on 2019/3/12.
 */
public class PerformanceHelper {

    /**
     * 记录线程使用情况
     */
    public static void recordThread() {
        Map<String, Integer> threadNumMap = new LinkedHashMap<>();
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread thread : threadSet) {
            String threadName = thread.getName().replaceAll("#?_?-?\\d+", "");
            if (threadNumMap.containsKey(threadName)) {
                threadNumMap.put(threadName, threadNumMap.get(threadName) + 1);
            } else {
                threadNumMap.put(threadName, 1);
            }
        }
        List<String> msgList = new LinkedList<>();
        msgList.add(String.valueOf(threadSet.size()));
        for (Map.Entry<String, Integer> entry : threadNumMap.entrySet()) {
            if (entry.getValue() >= PLogConstant.MIN_THREAD_NUM) {
                msgList.add(entry.getKey() + ":" + entry.getValue());
            }
        }
        PLog.d(PLogTag.THREAD_TAG, msgList);
    }

    /**
     * 记录内存使用情况
     */
    public static void recordMemory(Context context) {
        List<String> msgList = new LinkedList<>();
        msgList.add("maxMemory:" + LogUtil.b2Mb(Runtime.getRuntime().maxMemory()));
        msgList.add("totalMemory:" + LogUtil.b2Mb(Runtime.getRuntime().totalMemory()));
        msgList.add("freeMemory:" + LogUtil.b2Mb(Runtime.getRuntime().freeMemory()));
        msgList.add("nativeMemory:" + LogUtil.b2Mb(Debug.getNativeHeapAllocatedSize()));
        PLog.d(PLogTag.MEMORY_TAG, msgList);
    }

}
