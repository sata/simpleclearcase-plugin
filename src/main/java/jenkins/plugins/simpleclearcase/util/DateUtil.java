package jenkins.plugins.simpleclearcase.util;

import hudson.Util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.jvnet.localizer.ResourceBundleHolder;

import jenkins.plugins.simpleclearcase.SimpleClearCaseChangeLogEntry;
import jenkins.plugins.simpleclearcase.SimpleClearCaseSCM;

public class DateUtil {

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
			
			if (latest.before(e.getDate()) ) {
				latest = e.getDate();
			}
		}
		return latest;
	}
	
	public static Date parseDate(String date) {
		Date ret;
		SimpleDateFormat fmt = new SimpleDateFormat(Util.XS_DATETIME_FORMATTER.getPattern(), 
				new Locale(ResourceBundleHolder.get(SimpleClearCaseSCM.class).format("Locale")));

		try {
			ret = fmt.parse(date);
		} catch (ParseException e) {
			ret = null;
		}
		return ret;
	}
}
