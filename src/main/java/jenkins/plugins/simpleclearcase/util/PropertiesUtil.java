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
 * THE SOFTWARE
 */

package jenkins.plugins.simpleclearcase.util;

import jenkins.plugins.simpleclearcase.ClearTool;
import jenkins.plugins.simpleclearcase.SimpleClearCaseSCM;

import org.jvnet.localizer.ResourceBundleHolder;

public class PropertiesUtil {
	private static final String LOCALE  						= "Locale";
	private static final String TIMEZONE						= "TimeZone";
	private static final String QUIET_PERIOD					= "QuietPeriod";
	private static final String LSHISTORY_LAST_NUM_EVENTS_VALUE = "LshistoryLastNumEventsValue";
	
	public static String getLocale() {
		return ResourceBundleHolder.get(SimpleClearCaseSCM.class).format(LOCALE);
	}
	
	public static String getTimeZone() {
		return ResourceBundleHolder.get(SimpleClearCaseSCM.class).format(TIMEZONE);
	}
	
	public static int getQuietPeriod() {
		return Integer.parseInt(ResourceBundleHolder.get(SimpleClearCaseSCM.class).format(QUIET_PERIOD));
	}
	
	public static String getLshistoryLastNumEventsValue() {
		return ResourceBundleHolder.get(ClearTool.class).format(LSHISTORY_LAST_NUM_EVENTS_VALUE);
	}
}
