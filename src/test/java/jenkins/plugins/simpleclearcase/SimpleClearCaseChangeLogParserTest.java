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
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.xml.sax.SAXException;

import junit.framework.Assert;

public class SimpleClearCaseChangeLogParserTest {
	public static final String CHANGELOG_SMALL = "changelog-small.xml";
	
	private SimpleClearCaseChangeLogParser parser = new SimpleClearCaseChangeLogParser();
	
	public SimpleClearCaseChangeLogSet readSet(String name) throws IOException, 
	                                                          ParserConfigurationException, SAXException {
		InputStream is = SimpleClearCaseChangeLogParserTest.class.getResourceAsStream(name);
		List<SimpleClearCaseChangeLogEntry> entries = parser.readChangeLog(is);
		SimpleClearCaseChangeLogSet set = new SimpleClearCaseChangeLogSet(null, entries);
		return set;
	}
	
	@Test
	public void testParserNonEmpty() throws IOException, ParserConfigurationException, SAXException {
		Assert.assertFalse("SimpleClearCaseChangeLogSet shouldn't be empty", 
		                                                           readSet(CHANGELOG_SMALL).isEmptySet());
	}
}
