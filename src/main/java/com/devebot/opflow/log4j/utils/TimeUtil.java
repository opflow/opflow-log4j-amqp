package com.devebot.opflow.log4j.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author drupalex
 */
public class TimeUtil {
    
    private static final String ISO8601_TEMPLATE = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat(ISO8601_TEMPLATE);
    
    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    public static String toISO8601UTC(Date date) {
        return DATE_FORMAT.format(date);
    }
    
    public static String toISO8601UTC(long time) {
        return DATE_FORMAT.format(new Date(time));
    }
    
    public static Date fromISO8601UTC(String dateStr) {
        try {
            return DATE_FORMAT.parse(dateStr);
        } catch (ParseException e) {}
        return null;
    }
    
    public static long getCurrentTime() {
        return (new Date()).getTime();
    }
    
    public static String getCurrentTimeString() {
        return toISO8601UTC(new Date());
    }
}
