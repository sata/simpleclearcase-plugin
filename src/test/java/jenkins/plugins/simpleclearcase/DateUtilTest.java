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
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.plugins.simpleclearcase;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import jenkins.plugins.simpleclearcase.util.DateUtil;

import junit.framework.Assert;

import org.junit.Test;

public class DateUtilTest {

	private Calendar initCalendar() {
		Calendar cal = new GregorianCalendar(2011, Calendar.JUNE, 20, 13, 49, 53);
		return cal;
	}
	
	@Test
	public void testBefore() {
		Calendar cal = initCalendar()
		;
		Date a = cal.getTime();
		Date b = cal.getTime();
		
		//min to add is 0
		Assert.assertFalse("Dates are the same, no one is before", DateUtil.before(a, b, 0));
		//min to add is 10
		Assert.assertFalse("Dates are the same, a has minToAdd 10", DateUtil.before(a, b, 10));
		
		cal.add(Calendar.MINUTE, 20);
		//b is now 20 minutes after a
		b = cal.getTime();
		
		//min to add is 0
		Assert.assertTrue("a is before b", DateUtil.before(a, b, 0));
		//min to add is 19
		Assert.assertTrue("a is before b", DateUtil.before(a, b, 19));
		//min to add is 20
		Assert.assertFalse("a is before b, though minToAdd 20", DateUtil.before(a, b, 20));
	}
	
	@Test
	public void testAnyDateBefore() {
		Calendar cal = initCalendar();
		Date oldest = cal.getTime();
		
		cal.add(Calendar.SECOND, 1);
		
		LoadRuleDateMap map = new LoadRuleDateMap();
		map.setBuildTime("/fake/1", cal.getTime());
		
		cal.add(Calendar.HOUR, 1);
		map.setBuildTime("/fake/2/", cal.getTime());

		cal.add(Calendar.MINUTE, 30);
		map.setBuildTime("/fake/3", cal.getTime());
		
		//last date
		cal.add(Calendar.MINUTE, 1);
		Date last = cal.getTime();
		
		Assert.assertFalse("no dates in map is before oldest date", 
		                                                          DateUtil.anyDateBefore(map, oldest, 0));
		Assert.assertTrue("all dates are before", DateUtil.anyDateBefore(map, last, 0));
		Assert.assertFalse("All dates are before, min to add is 1 making it false", 
		                                                            DateUtil.anyDateBefore(map, last, 1));
	}
	
	@Test
	public void testParseDateEmpty() {
		Date date = DateUtil.parseDate("");
		Assert.assertNull("Empty string should return null", date);
		
		date = DateUtil.parseDate(" ");
		Assert.assertNull("white space string should return null", date);
		
		date = DateUtil.parseDate(".2001111E4.2001111E4");
		Assert.assertNull("Invalid string should return null", date);
	}
}
