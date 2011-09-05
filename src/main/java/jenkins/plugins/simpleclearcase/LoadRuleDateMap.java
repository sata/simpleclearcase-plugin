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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jenkins.plugins.simpleclearcase.util.DateUtil;

public class LoadRuleDateMap {
	private static final String OUTPUT_FORMAT = "[%s, %s]";
	//due to syncronization problem with Calendar we need to have an private instance to DateUtil
	private DateUtil dateUtil;
	private Map<String, Date> map;
	
	public LoadRuleDateMap() {
		map = new HashMap<String, Date>();
		dateUtil = new DateUtil();
	}
	
	public Date getBuiltTime(String loadRule) {
		return map.get(loadRule);
	}
	
	public void setBuildTime(String loadRule, Date date) {
		map.put(loadRule, date);
	}
	
	public Collection<Date> getDates() {
		return map.values();
	}
	
	/**
	 * @return true if any load rule is missing a date value. otherwise false 
	 */
	public boolean isDatesEmpty() {
		for (Date date : map.values()) {
			if (date == null) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param compare the comparison LoadRuleDateMap
	 * @return true if this LoadRuleDateMap has a date before compare date, for a specific load rule  
	 */
	public boolean isBefore(LoadRuleDateMap compare) {
		
		for (Map.Entry<String, Date> entry : map.entrySet()) {
			//fetch the date for the specific load rule
			Date compareDate = compare.getBuiltTime(entry.getKey());

			//compare the entries date for the load rule against the comparisons date
			if (entry.getValue().before(compareDate)) {
				return true;
			}
		}
		return false;
	}

	private List<String> getAsList() {
		List<String> ret = new ArrayList<String>();

		for (Map.Entry<String, Date> entry : map.entrySet()) {
			//we create a string with format
			ret.add(String.format(OUTPUT_FORMAT, entry.getKey(),  
					         (entry.getValue() != null) ? dateUtil.formatDate(entry.getValue()) : null));
		}
		return ret;
	}

	public String toString() {
		return Arrays.toString(getAsList().toArray());
	}
}
