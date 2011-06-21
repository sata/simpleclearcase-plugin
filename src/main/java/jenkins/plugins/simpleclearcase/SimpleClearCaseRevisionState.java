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

import java.util.Date;

import hudson.scm.SCMRevisionState;


/**
 * @author Sam Tavakoli
 *
 * We represent a revision in a primitive manner. What we keep track of is the latest commit 
 * date on a specific load rule path. Meaning that if a build has multiple load rules to check
 *  we have to compare the latest commit date for each load rule individually. Hence the Map.
 */
public class SimpleClearCaseRevisionState extends SCMRevisionState {

	private LoadRuleDateMap map;

	public SimpleClearCaseRevisionState(LoadRuleDateMap map) { 
		this.map = map;
	}
	
	public SimpleClearCaseRevisionState() {
		map = new LoadRuleDateMap();
	}
	
	public Date getBuiltTime(String loadRule) {
		return map.getBuiltTime(loadRule);
	}
	
	public void setBuiltTime(String loadRule, Date date) {
		map.setBuildTime(loadRule, date);
	}
	
	public LoadRuleDateMap getLoadRuleDateMap() {
		return map;
	}
}
