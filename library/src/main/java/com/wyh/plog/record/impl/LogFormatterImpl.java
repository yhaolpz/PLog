package com.wyh.plog.record.impl;

import com.wyh.plog.core.PLog;
import com.wyh.plog.core.PLogConstant;
import com.wyh.plog.record.LogFormatter;
import com.wyh.plog.util.DateUtil;


/**
 * Created by wyh on 2019/3/13.
 */

public class LogFormatterImpl implements LogFormatter {

    private final StringBuilder content = new StringBuilder();

    @Override
    public String format(@PLog.DebugLevel.Level int debugLevel, String tag, String msg) {
        if (content.length() > 0) {
            content.delete(0, content.length());
        }
        content.append(DateUtil.getHour());
        content.append(PLogConstant.FIELD_SEPERATOR);
        content.append(getLevelStr(debugLevel));
        content.append(tag);
        content.append(": ");
        content.append(msg);
        content.append('\n');
        return content.toString();
    }


    private String getLevelStr(@PLog.DebugLevel.Level int debugLevel) {
        switch (debugLevel) {
            case PLog.DebugLevel.VERBOSE:
                return "V/";
            case PLog.DebugLevel.DEBUG:
                return "D/";
            case PLog.DebugLevel.INFO:
                return "I/";
            case PLog.DebugLevel.WARNING:
                return "W/";
            case PLog.DebugLevel.ERROR:
                return "E/";
            case PLog.DebugLevel.ALL:
            case PLog.DebugLevel.NONE:
                return "";
        }
        return "";
    }


}
