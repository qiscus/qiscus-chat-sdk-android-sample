package com.qiscus.mychatui.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created on : January 31, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public final class DateUtil {
    private static DateFormat fullDateFormat;

    static {
        fullDateFormat = new SimpleDateFormat("dd MMM yyyy 'at' hh:mm a");
    }

    public static String toFullDate(Date date) {
        return fullDateFormat.format(date);
    }

    public static String getLastMessageTimestamp(Date utcDate) {
        if (utcDate != null) {
            Calendar todayCalendar = Calendar.getInstance();
            Calendar localCalendar = Calendar.getInstance();
            localCalendar.setTime(utcDate);

            if (getDateStringFromDate(todayCalendar.getTime())
                    .equals(getDateStringFromDate(localCalendar.getTime()))) {

                return getTimeStringFromDate(utcDate);

            } else if ((todayCalendar.get(Calendar.DATE) - localCalendar.get(Calendar.DATE)) == 1) {
                return "Yesterday";
            } else {
                return getDateStringFromDate(utcDate);
            }
        } else {
            return null;
        }
    }

    public static String getTimeStringFromDate(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.US);
        return dateFormat.format(date);
    }

    public static String getDateStringFromDate(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        return dateFormat.format(date);
    }

    public static String getDateStringFromDateTimeline(Date date) {
        DateFormat day = new SimpleDateFormat("dd", Locale.US);
        DateFormat month1 = new SimpleDateFormat("MM", Locale.US);
        DateFormat years = new SimpleDateFormat("yyyy", Locale.US);
        String dayText = day.format(date);
        String month = month1.format(date);
        String monthText = "";
        if (month.equals("01")) {
            monthText = "Januari";
        } else if (month.equals("02")) {
            monthText = "Febuari";
        } else if (month.equals("03")) {
            monthText = "Maret";
        } else if (month.equals("04")) {
            monthText = "April";
        } else if (month.equals("05")) {
            monthText = "Mei";
        } else if (month.equals("06")) {
            monthText = "Juni";
        } else if (month.equals("07")) {
            monthText = "July";
        } else if (month.equals("08")) {
            monthText = "Agustus";
        } else if (month.equals("09")) {
            monthText = "September";
        } else if (month.equals("10")) {
            monthText = "Oktober";
        } else if (month.equals("11")) {
            monthText = "November";
        } else if (month.equals("12")) {
            monthText = "Desember";
        }
        String yearsText = years.format(date);
        String time = getTimeStringFromDate(date);
        String all = dayText + " " + monthText + " " + yearsText + " " + time;
        return all;
    }

}
