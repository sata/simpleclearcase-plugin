package jenkins.plugins.simpleclearcase;

import java.util.Comparator;

public class SimpleClearCaseChangeLogEntryDateComparator implements Comparator<SimpleClearCaseChangeLogEntry> {
	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 * 
	 * This is used primarily to order ChangeLog entries by date than the order of load rule
	 */
	public int compare(SimpleClearCaseChangeLogEntry e1, SimpleClearCaseChangeLogEntry e2) {
		return e1.getDate().compareTo(e2.getDate());
	}
}
