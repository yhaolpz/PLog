package com.wyh.plog.util;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by wyh on 2019/3/10.
 */
public class DateUtil {

    public static final String FORMAT_DATE_EN = "yyyy-MM-dd";
    public static final String FORMAT_TIME_EN = "yyyy-MM-dd HH:mm:ss";
    public static final String FORMAT_HOUR_EN = "HH:mm:ss";


    @StringDef({FORMAT_DATE_EN, FORMAT_TIME_EN, FORMAT_HOUR_EN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FORMAT {
    }

    private static final SimpleDateFormat SDF = new SimpleDateFormat(FORMAT_DATE_EN, Locale.CHINA);


    public static String getDate() {
        return format(FORMAT_DATE_EN, new Date());
    }

    public static String getHour() {
        return format(FORMAT_HOUR_EN, new Date());
    }

    public static String getTime() {
        return format(FORMAT_TIME_EN, new Date());
    }

    public static String format(@FORMAT String timeFormat, Date date) {
        SDF.setTimeZone(TimeZone.getDefault());
        SDF.applyPattern(timeFormat);
        return SDF.format(date);
    }
}
