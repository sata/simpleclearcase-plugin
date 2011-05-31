package jenkins.plugins.simpleclearcase.util;

import hudson.model.TaskListener;

public class DebugHelper {
	
	/**
	 * @param listener
	 * @param format the formatted string specifying the message to be presented as error message
	 *				 the method also appends appropiate newline seqence depending on OS.
	 * @param args is an object array of arguments which has it's place in formatted string
	 */
	public static void error(TaskListener listener, String format, Object... args) {
		listener.error(format + "%n", args);
	}
	
	/**
	 * @param launcher
	 * @param msg
	 */
	public static void error(TaskListener listener, String msg) {
		listener.error(msg);
	}
	
	/**
	 * @param launcher
	 * @param format
	 * @param args
	 */
	public static void info(TaskListener listener, String format, Object... args) {
		listener.getLogger().printf(format + "%n", args);
	}
	
	/**
	 * @param launcher
	 * @param msg
	 */
	public static void info(TaskListener listener, String msg) {
		listener.getLogger().println(msg);
	}
}
