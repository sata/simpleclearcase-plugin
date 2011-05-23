package jenkins.plugins.simpleclearcase.util;

import java.util.Date;
import java.util.List;

import jenkins.plugins.simpleclearcase.SimpleClearCaseChangeLogEntry;

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
}
