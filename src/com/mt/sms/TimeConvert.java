package com.mt.sms;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeConvert {
    /**
     * format pattern yyyyMMdd HH:mm:ss
     */
    public static final String DATE_PATTERN_A = "yyyyMMdd HH:mm:ss";
    /**
     * format pattern yyyy-MM-dd HH:mm:ss
     */
    public static final String DATE_PATTERN_B = "yyyy-MM-dd HH:mm:ss";
    /**
     * format pattern yyyyMMddHHmmss
     */
    public static final String DATE_PATTERN_C = "yyyyMMddHHmmss";
    
    
    /**
     * 将字符串转为时间戳
     * @param timeStr
     * @return time, or -1
     */
    public static long str2Time(String timeStr, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        try {
            Date d = format.parse(timeStr);
            return d.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        return -1;
    }

    /**
     * 将时间戳转为字符串
     * @param timeStamp
     * @return formated date
     */
    public static String time2Str(String timeStamp, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        
        // 例如：cc_time=1291778220
        long lcc_time = Long.valueOf(timeStamp) * 1000L;
        return format.format(new Date(lcc_time));
    }
    
    /**
     * 将时间戳转为字符串
     * @param time
     * @return formated date
     */
    public static String time2Str(long time, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        
        return format.format(new Date(time));
    }
    
    /**
     * 返回当前时间，完整格式，yyyy-MM-dd HH:mm:ss
     * @return
     */
    public static String getTimestamp(){
    	 return time2Str(new Date().getTime(), DATE_PATTERN_B);
    }
    /**
     * 返回与当前时间差几分钟的字符串
     * @param minutes 分钟数；正数，往后推；负数，向前推
     * @return
     */
    public static String getTimeDiff(int minutes){
    	long cur = new Date().getTime();
    	cur =+ minutes*60 *1000;
    	return time2Str(cur, DATE_PATTERN_B);
    	
    }
    /**
     * 
     * @return 当前时间，HH:mm:ss
     */
    public static String getCurrTime(){
    	return time2Str(new Date().getTime(), "HH:mm:ss");
    }
}
