/**
 * The MIT License
 * 
 * Copyright (c) 2011, Sun Microsystems, Inc., Sam Tavakoli
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package jenkins.plugins.simpleclearcase.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jenkins.plugins.simpleclearcase.LoadRuleDateMap;
import jenkins.plugins.simpleclearcase.SimpleClearCaseChangeLogEntry;

public class DateUtil {
    private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static SimpleDateFormat DATETIME_FORMATTER = new SimpleDateFormat(
                                                      DATETIME_FORMAT, new Locale(PropUtils.getLocale()));

    /**
     * @param entries a list of ChangeLogEntry
     * @return the latest commit date from a entry in entries, if entries is empty 
     * 		   then null is returned.
     */
    public static Date getLatestDate(List<SimpleClearCaseChangeLogEntry> entries) {
        Date latest = null;
        // Iterate over all entries, return the latest date

        for (SimpleClearCaseChangeLogEntry e : entries) {
            if (latest == null) {
                latest = e.getDate();
                continue;
            }

            if (latest.before(e.getDate())) {
                latest = e.getDate();
            }
        }
        return latest;
    }

    public static String formatDate(Date date) {
        return DATETIME_FORMATTER.format(date);
    }

    public static Date parseDate(String date) {
        Date ret;

        try {
            ret = DATETIME_FORMATTER.parse(date);
        } catch (ParseException e) {
            ret = null;
        }
        return ret;
    }

    /**
     * @param commits
     * @param dateCompare
     * @param minToAdd
     * @return false if any date is equal or after compared date, true otherwise
     */
    public static boolean anyDateBefore(LoadRuleDateMap commits,
            Date dateCompare, int minToAdd) {
        for (Date date : commits.getDates()) {
            if (before(date, dateCompare, minToAdd) == false) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param date
     * @param minToAdd
     * @return true if date1 added with minToAdd minutes is before date2, otherwise false 
     */
    public static boolean before(Date date1, Date date2, int minToAdd) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date1);
        cal.add(Calendar.MINUTE, minToAdd);

        // now date1 is added with the additional minutes
        date1 = cal.getTime();

        // return the comparison
        return date1.before(date2);
    }
}
