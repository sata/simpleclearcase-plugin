/*
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
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
	 * @param listener
	 * @param msg
	 */
	public static void fatalError(TaskListener listener, String msg) {
		listener.fatalError(msg);
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
		String formatted = String.format(format + "%n", args);
		info(listener, formatted);
	}
	
	/**
	 * @param launcher
	 * @param msg
	 */
	public static void info(TaskListener listener, String msg) {
		listener.getLogger().println(msg);
	}
}
