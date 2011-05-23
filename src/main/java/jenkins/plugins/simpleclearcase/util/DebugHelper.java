package jenkins.plugins.simpleclearcase.util;

import hudson.Launcher;

public class DebugHelper {
	
	/**
	 * @param launcher
	 * @param format the formatted string specifying the message to be presented as error message
	 *				 the method also appends appropiate newline seqence depending on OS.
	 * @param args is an object array of arguments which has it's place in formatted string
	 */
	public static void error(Launcher launcher, String format, Object... args) {
		launcher.getListener().error(format + OsUtil.getNewline(launcher.isUnix()), args);
	}
	
	/**
	 * @param launcher
	 * @param msg
	 */
	public static void error(Launcher launcher, String msg) {
		launcher.getListener().error(msg);
	}
	
	/**
	 * @param launcher
	 * @param format
	 * @param args
	 */
	public static void info(Launcher launcher, String format, Object... args) {
		launcher.getListener().getLogger().printf(format + OsUtil.getNewline(launcher.isUnix()), args);
	}
	
	/**
	 * @param launcher
	 * @param msg
	 */
	public static void info(Launcher launcher, String msg) {
		launcher.getListener().getLogger().println(msg);
	}
}
