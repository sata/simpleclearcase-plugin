package jenkins.plugins.simpleclearcase;

import java.util.Comparator;

	/*
	 * This is used primarily to order ChangeLog entries by date than the order of load rule
	 * The order of the date is decided through the increasing parameter when creating the comparator
	 */

public class SimpleClearCaseChangeLogEntryDateComparator implements Comparator<SimpleClearCaseChangeLogEntry> {
	
	public static final int DECREASING = -1;
	public static final int INCREASING = 1;
	
	private final int order;
	
	/**
	 * @param increasing
	 */
	public SimpleClearCaseChangeLogEntryDateComparator(int order) {
		this.order = order;
	}
	public int compare(SimpleClearCaseChangeLogEntry e1, SimpleClearCaseChangeLogEntry e2) {
		
		//depending on the order value  we either have decreasing or increasing order
		return (e1.getDate().compareTo(e2.getDate()) * this.order); 
		}
}
