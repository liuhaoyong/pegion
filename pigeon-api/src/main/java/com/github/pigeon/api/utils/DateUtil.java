package com.github.pigeon.api.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

/**
 * 时间处理工具类
 * @author liuhaoyong
 * time : 2016年3月14日 下午2:36:41
 */
public final class DateUtil {

    public final static long    ONE_DAY_SECONDS      = 86400;
    public final static String  shortFormat          = "yyyyMMdd";
    public final static String  longFormat           = "yyyyMMddHHmmss";
    public final static String  webFormat            = "yyyy-MM-dd";
    public final static String  timeFormat           = "HHmmss";
    public final static String  monthFormat          = "yyyyMM";
    public final static String  chineseDtFormat      = "yyyy年MM月dd日";
    public final static String  newFormat            = "yyyy-MM-dd HH:mm:ss";
    public final static String  noSecondFormat       = "yyyy-MM-dd HH:mm";
    public final static long    ONE_DAY_MILL_SECONDS = 86400000;

    /**
     *
     * @param date
     * @param format
     * @return
     */
    public static String formatDate(Date date, String format) {
        return DateFormatUtils.format(date,format);
    }


    /**
     *  转换成 yyyy-MM-dd HH:mm:ss
     * @param date
     * @return
     */
    public static String formatDateIOS_DATE_TIME(Date date){
        return DateFormatUtils.format(date,newFormat);
    }

    /**
     * 转化成yyyy-MM-dd
     * @param date
     * @return
     */
    public static String formatDateIOS_DATE(Date date){
        return DateFormatUtils.format(date,webFormat);
    }


    /**
     * 解析Date
     * @param dateStr
     * @return
     */
    public static Date parseDate(String dateStr,String pattern) throws ParseException{
        return DateUtils.parseDate(dateStr, new String[]{pattern});
    }

    /**
     * 根据yyyy-MM-dd HH:mm:ss解析成Date
     * @param dateStr
     * @return
     * @throws ParseException
     */
    public static Date parseDateIOS_DATE_TIME(String dateStr) throws ParseException{
        return parseDate(dateStr,newFormat);
    }

    /**
     * 根据yyyy-MM-dd解析成Date
     * @param dateStr
     * @return
     * @throws ParseException
     */
    public static Date parseDateIOS_DATE(String dateStr)  throws ParseException{
        return parseDate(dateStr,webFormat);
    }

    /**
     * 加小时
     *
     * @param date
     * @param hours
     *
     * @return
     */
    public static Date addHours(Date date, int hours) {
        return DateUtils.addHours(date, hours * 60);
    }

    /**
     * 叫分钟
     *
     * @param date
     * @param minutes
     *
     * @return
     */
    public static Date addMinutes(Date date, int minutes) {
        return DateUtils.addMinutes(date, minutes * 60);
    }

    /**、
     * 加秒
     * @param date1
     * @param secs
     *
     * @return
     */

    public static Date addSeconds(Date date, int secs) {
        return DateUtils.addSeconds(date, secs);
    }

    /**、
     * 加毫秒
     * @param date1
     * @param secs
     *
     * @return
     */
    public static Date addMilliseconds(Date date, int ms) {
        return DateUtils.addMilliseconds(date, ms);
    }

    /**
     * 叫天
     * @param date
     * @param secs
     * @return
     */
    public static Date addDays(Date date, int day) {
        return DateUtils.addDays(date, day);
    }

    /**
     * 取明天
     * @return
     */
    public static Date getTomorrow(){
        return addDays(new Date(),1);
    }


    /**
     * 取昨天
     * @return
     */
    public static Date getYesterday(){
        return addDays(new Date(),-1);
    }


    /**
     * 判断输入的字符串是否为合法的小时
     *
     * @param hourStr
     *
     * @return true/false
     */
    public static boolean isValidHour(String hourStr) {
        if (!StringUtils.isEmpty(hourStr) && StringUtils.isNumeric(hourStr)) {
            int hour = new Integer(hourStr).intValue();

            if ((hour >= 0) && (hour <= 23)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断输入的字符串是否为合法的分或秒
     *
     * @param minuteStr
     *
     * @return true/false
     */
    public static boolean isValidMinuteOrSecond(String str) {
        if (!StringUtils.isEmpty(str) && StringUtils.isNumeric(str)) {
            int hour = new Integer(str).intValue();

            if ((hour >= 0) && (hour <= 59)) {
                return true;
            }
        }

        return false;
    }


    /**
     * 取得两个日期间隔秒数（日期1-日期2）
     *
     * @param one 日期1
     * @param two 日期2
     *
     * @return 间隔秒数
     */
    public static long getDiffSeconds(Date one, Date two) {
        Calendar sysDate = new GregorianCalendar();

        sysDate.setTime(one);

        Calendar failDate = new GregorianCalendar();

        failDate.setTime(two);
        return (sysDate.getTimeInMillis() - failDate.getTimeInMillis()) / 1000;
    }

    /**
     * 间隔分钟数
     * @param one
     * @param two
     * @return
     */
    public static long getDiffMinutes(Date one, Date two) {
        Calendar sysDate = new GregorianCalendar();

        sysDate.setTime(one);

        Calendar failDate = new GregorianCalendar();

        failDate.setTime(two);
        return (sysDate.getTimeInMillis() - failDate.getTimeInMillis()) / (60 * 1000);
    }

    /**
     * 取得两个日期的间隔天数
     *
     * @param one
     * @param two
     *
     * @return 间隔天数
     */
    public static long getDiffDays(Date date1, Date date2) {
        return (date1.getTime() - date2.getTime()) / (1000 * 3600 * 24);
    }


    /**
     * 是否在现在时间之前
     * @param date
     * @return
     */
    public static boolean isBeforeNow(Date date) {
        if (date == null)
            return false;
        return date.compareTo(new Date()) < 0;
    }


    /**
     * 判断两个日期是否是同一天
     * @param date1
     * @param date2
     * @return
     */
    public static boolean isSameDay(Date date1, Date date2) {
       return DateUtils.isSameDay(date1, date2);
    }

    /**
     *
     * @param date
     * @param dateFormat
     * @return
     */
    public static String getDateString(Date date, DateFormat dateFormat) {
        if (date == null || dateFormat == null) {
            return null;
        }

        return dateFormat.format(date);
    }

    /**
     *
     * @param pattern
     * @return
     */
    public static DateFormat getNewDateFormat(String pattern) {
        DateFormat df = new SimpleDateFormat(pattern);

        df.setLenient(false);
        return df;
    }

    public static String format(Date date, String format) {
        if (date == null) {
            return null;
        }

        return new SimpleDateFormat(format).format(date);
    }


    public static long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }


}
