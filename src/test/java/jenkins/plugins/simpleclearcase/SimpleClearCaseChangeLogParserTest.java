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

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;

import jenkins.plugins.simpleclearcase.util.PropertiesUtil;

import org.junit.Test;
import org.xml.sax.SAXException;

import junit.framework.Assert;

public class SimpleClearCaseChangeLogParserTest {
	private static final String CHANGELOG_SMALL = "changelog-small.xml";
	
	private SimpleClearCaseChangeLogSet readSet(String name) throws IOException, ParserConfigurationException, SAXException {
		InputStream is = SimpleClearCaseChangeLogParserTest.class.getResourceAsStream(name);
		List<SimpleClearCaseChangeLogEntry> entries = SimpleClearCaseChangeLogParser.readChangeLog(is);
		SimpleClearCaseChangeLogSet set = new SimpleClearCaseChangeLogSet(null, entries);
		return set;
	}
	
	@Test
	public void testParserNonEmpty() throws IOException, ParserConfigurationException, SAXException {
		Assert.assertFalse("SimpleClearCaseChangeLogSet shouldn't be empty", readSet(CHANGELOG_SMALL).isEmptySet());
	}
	
	//TODO move test cases to SimpleClearCaseChangeLogEntry
	@Test
	public void testParserFetchDate() throws IOException, ParserConfigurationException, SAXException {
		SimpleClearCaseChangeLogSet set = readSet(CHANGELOG_SMALL);
		
		Calendar cal = new GregorianCalendar(2011, Calendar.JUNE, 20, 13, 49, 53);
		cal.setTimeZone(TimeZone.getTimeZone(PropertiesUtil.getTimeZone()));
		
		
		//changelog-small date, is defined as 15:49:53+0200, we set the timezone and specify 13;49 which
		//will be transformed into 15:49 CEST
		

		Date entryDate = set.getEntries().get(0).getDate();

		Assert.assertTrue("Fetched date doesn't match the created one", entryDate.equals(cal.getTime()));
		}
	
	//TODO move test cases to SimpleClearCaseChangeLogEntry
	@Test
	public void testParserFetchUser() throws IOException, ParserConfigurationException, SAXException {
		SimpleClearCaseChangeLogSet set = readSet(CHANGELOG_SMALL);
		
		Assert.assertEquals("Fetched username doesn't match", "etavsam", set.getEntries().get(0).getUser());
	}
}
