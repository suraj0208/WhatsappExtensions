package com.suraj.waext.data;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by suraj on 23/2/17.
 */
public class ContributionCalendarDate extends Date {

    public ContributionCalendarDate(long date) {
        super(date);
    }

    @Override
    public int hashCode() {
        Calendar thisCalendar = Calendar.getInstance(Locale.getDefault());
        thisCalendar.setTime(this);

        Integer date = thisCalendar.get(Calendar.DATE);
        Integer month = thisCalendar.get(Calendar.MONTH);
        Integer year = thisCalendar.get(Calendar.YEAR);

        String format = date + "/" + month + "/" + year;
        return format.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Date))
            return super.equals(obj);

        Calendar thisCalendar = Calendar.getInstance(Locale.getDefault());
        thisCalendar.setTime(this);

        Integer date = thisCalendar.get(Calendar.DATE);
        Integer month = thisCalendar.get(Calendar.MONTH);
        Integer year = thisCalendar.get(Calendar.YEAR);

        Calendar otherCalendar = Calendar.getInstance(Locale.getDefault());
        otherCalendar.setTime((Date) obj);

        Integer otherDate = otherCalendar.get(Calendar.DATE);
        Integer otherMonth = otherCalendar.get(Calendar.MONTH);
        Integer otherYear = otherCalendar.get(Calendar.YEAR);

        return date.equals(otherDate) && month.equals(otherMonth) && year.equals(otherYear);

    }

    @Override
    public int compareTo(Date anotherDate) {
        Calendar thisCalendar = Calendar.getInstance(Locale.getDefault());
        thisCalendar.setTime(this);

        Integer date = thisCalendar.get(Calendar.DATE);
        Integer month = thisCalendar.get(Calendar.MONTH);
        Integer year = thisCalendar.get(Calendar.YEAR);

        Calendar otherCalendar = Calendar.getInstance(Locale.getDefault());
        otherCalendar.setTime(anotherDate);

        Integer otherDate = otherCalendar.get(Calendar.DATE);
        Integer otherMonth = otherCalendar.get(Calendar.MONTH);
        Integer otherYear = otherCalendar.get(Calendar.YEAR);

        String format = year + "/" + (Integer.toString(month).length() < 2 ? "0" + month : month) + "/" + (Integer.toString(date).length() < 2 ? "0" + date : date);
        String otherFormat = otherYear + "/" + (Integer.toString(otherMonth).length() < 2 ? "0" + otherMonth : otherMonth) + "/" + (Integer.toString(otherDate).length() < 2 ? "0" + otherDate : otherDate);

        return format.compareTo(otherFormat);

    }
}
