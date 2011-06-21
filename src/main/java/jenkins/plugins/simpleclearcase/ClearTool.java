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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import jenkins.plugins.simpleclearcase.util.DateUtil;
import jenkins.plugins.simpleclearcase.util.DebugHelper;
import jenkins.plugins.simpleclearcase.util.PropertiesUtil;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;

/**
 * @author Sam Tavakoli
 *
 */
public class ClearTool {
	private static final String LOG_LSHISTORY_PRIVATE = "lshistory(String filePath, Date since)";
	
	private static final String ADDED_ELEMENT_QUOTATION = "\"";
	private static final String ADDED_FILE_ELEMENT  	= "Added file element";
	private static final String ADDED_DIRECTORY_ELEMENT = "Added directory element";
	
	private static final String CLEARTOOL = "cleartool";
	private static final String LSVIEW    = "lsview"; 
	private static final String LSHISTORY = "lshistory";
	private static final String SETVIEW   = "setview";
	
	private static final String PARAM_SINCE   = "-since";
	private static final String PARAM_FMT     = "-fmt";
	private static final String PARAM_NCO     = "-nco";
	private static final String PARAM_RECURSE = "-recurse";
	private static final String PARAM_LAST    = "-last";

	private static final String PARAM_EXEC    = "-exec";
	
	private static final String LSHISTORY_ENTRY_DATE_FORMAT = "yyyyMMdd.HHmmss";
	private static final String SINCE_DATE_FORMAT   		= "d-MMM-yy.HH:mm:ss'UTC'Z";

	private Launcher launcher;
	private TaskListener listener;
	private FilePath workspace;
	private String viewname;
	
	public ClearTool(Launcher launcher, TaskListener listener, FilePath workspace, String viewname) throws InterruptedException, IOException {
		this.launcher  = launcher;
		this.listener  = listener;
		this.workspace = workspace;
		this.viewname  = viewname.trim();
	}
	
	public boolean doesViewExist(String viewTag) throws InterruptedException {
		ArgumentListBuilder cmd = new ArgumentListBuilder();
		
		cmd.add(LSVIEW);
		cmd.add(viewTag);

		try {
			execute(cmd);
		} catch (IOException e) {
			return false; //if there isn't such view 
		}
		return true;
	}
	
	/**
	 * @param loadRules the paths where to fetch commit dates from
	 * @return the latest commit date for each load rule
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public LoadRuleDateMap getLatestCommitDates(List<String> loadRules, LoadRuleDateMap previousCommits) throws InterruptedException, IOException {
		LoadRuleDateMap ret = new LoadRuleDateMap();
		for (String lr : loadRules) {
			//we fetch the latest date for load rule lr and limit the set of entries from 
			//lshistory by giving the previous commit date for the load rule
			ret.setBuildTime(lr, DateUtil.getLatestDate(lshistory(lr, 
											previousCommits.getBuiltTime(lr))));
		}
		return ret;
	}
	
	/**
	 * @param loadRules loadRules All the file paths which we want to fetch commit changes from SCM
	 * @param previousCommit specifies from when, we don't want to fetch information which has been collected for previous builds, 
	 * 					     this is load rule specific, not all load rule have the same commit-dates.
	 * @return
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public List<SimpleClearCaseChangeLogEntry> lshistory(List<String> loadRules, LoadRuleDateMap previousCommit) throws InterruptedException, IOException {
		List<SimpleClearCaseChangeLogEntry> entries = new ArrayList<SimpleClearCaseChangeLogEntry>();

		for (String lr : loadRules) {
			List<SimpleClearCaseChangeLogEntry> l;
			
			if (previousCommit == null) {
				l = lshistory(lr, null);
			} else {
				l  = lshistory(lr, previousCommit.getBuiltTime(lr));
			}

			if (l != null) {
				entries.addAll(l);
			}
		}					
		return entries;
	}
	/**
	 * @param filePath a specific file path	 which we want to fetch commit changes from SCM.
	 * @param since
	 * @return a list of ChangeLog entries, parsed from the rawLshistory
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private List<SimpleClearCaseChangeLogEntry> lshistory(String filePath, Date since) throws InterruptedException, IOException {
		BufferedReader in = new BufferedReader(rawLshistory(filePath, since));
		List<SimpleClearCaseChangeLogEntry> ret = new ArrayList<SimpleClearCaseChangeLogEntry>();
		
		String readline = in.readLine();
		SimpleClearCaseChangeLogEntry currentEntry  = null;
		
		while (readline != null) {
			//empty lines cannot be parsed
			if (readline.trim().isEmpty()) {
				readline = in.readLine();
				continue;
			}
			
			//a commit entry could be split over several lines hence we need to check for additional info
			if (readline.startsWith(ADDED_FILE_ELEMENT) || readline.startsWith(ADDED_DIRECTORY_ELEMENT)) {
				if (currentEntry == null) {
					DebugHelper.error(listener, "%s: CurrentEntry is null when a ADDED_FILE_ELEMENT or " +
													"ADDED_DIRECTORY_ELEMENT popped up, shouldn't happen. row is: %s", 
													 LOG_LSHISTORY_PRIVATE, readline);
				}
				
				//with the formatting we have the ELEMENT row wraps the filepath with quote.
				int startIndex = readline.indexOf(ADDED_ELEMENT_QUOTATION) + 1; 
				int endIndex   = readline.indexOf(ADDED_ELEMENT_QUOTATION, startIndex);
				currentEntry.addPath(readline.substring(startIndex, endIndex));
 			} else { 
 				//here we actually parse the entry and build an entry from it
 				SimpleClearCaseChangeLogEntry tmpEntry = parseRawLsHistoryEntry(readline);

 				// if its a valid entry we add it to collection and update current entry such that
 				// new paths like "added file directory" are also added to the entry
				if (tmpEntry != null) {
					currentEntry = tmpEntry;
					ret.add(currentEntry);
					DebugHelper.info(listener, "%s: added newly created entry: %s",
												LOG_LSHISTORY_PRIVATE, DateUtil.formatDate(currentEntry.getDate()));					
				} else {
					DebugHelper.error(listener, "%s: Wasn't able to parse row, hence we skip it, line: %s", 
												LOG_LSHISTORY_PRIVATE, readline);
				}
			}
			readline = in.readLine();
		}
		in.close();
		return ret;
	}
	
	/**
	 * @param filePath to the element in repository
	 * @param since from when we want to fetch history entries from
	 *
	 * @return reader to the byte array input stream with raw entries
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private Reader rawLshistory(String filePath, Date since) throws InterruptedException, IOException {
		ArgumentListBuilder cmd = new ArgumentListBuilder();

		//fetching locale and time zone settings from properties file
		SimpleDateFormat fmt = new SimpleDateFormat(SINCE_DATE_FORMAT, 
					new Locale(PropertiesUtil.getLocale()));
		fmt.setTimeZone(TimeZone.getTimeZone(PropertiesUtil.getTimeZone()));

		cmd.add(LSHISTORY);
		
		if (since != null){
			// if the date is null, there is no time bound on lshistory
			cmd.add(PARAM_RECURSE); //we only want to recurse when we have a baseline time
			cmd.add(PARAM_SINCE, fmt.format(since).toLowerCase());
		} else {
			// if it's the first build there isn't any previous date to take as starting point
			//but we don't want a gigantic set, so we also add LAST parameter, to limit down the resultset
			cmd.add(PARAM_LAST);
			cmd.add(PropertiesUtil.getLshistoryLastNumEventsValue());
		}
		
		cmd.add(PARAM_FMT);
		cmd.add(SimpleClearCaseChangeLogEntry.LSHISTORY_FORMATTING);
		cmd.add(PARAM_NCO);
		cmd.add(filePath);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		executeWithView(cmd, baos);
		
		byte[] result = baos.toByteArray();
		baos.close();
		
		Reader ret = new InputStreamReader(new ByteArrayInputStream(result));
		
		return ret;
	}

	/**
	 * @param cmd the command set which needs to be padded according to needsView and executing program
	 * @param needsView
	 * @return
	 */
	private ArgumentListBuilder appendOptions(ArgumentListBuilder cmd, boolean needsView) {
		//if we get null or empty command set we return null as well
		if (cmd == null || cmd.toList().size() < 1) {
			return null;
		}
		ArgumentListBuilder ret = null;
		
		//we need to add setview with exec call as prefix if a view is needed
		if (needsView == true) {
			ret = new ArgumentListBuilder();

			ret.add(CLEARTOOL);
			ret.add(SETVIEW);
			ret.add(PARAM_EXEC);
			ret.add(cmd.prepend(CLEARTOOL).toStringWithQuote());
			ret.add(this.viewname);
		} else {
			//if we don't need a view to execute we just prepend with cleartool
			ret = cmd.prepend(CLEARTOOL);
		}
		return ret;
	}
	
	/**
	 * @param cmd 
	 * @return true if command was successfully executed
	 * @throws IOException thrown if return code is above 0
	 * @throws InterruptedException
	 */
	private boolean execute(ArgumentListBuilder cmd) throws IOException, InterruptedException {
		return execute(cmd, null, null, false);
	}
	
	/**
	 * @param cmd
	 * @param out output from executed command is pushed to this stream
	 * @return true if command was successfully executed
	 * @throws IOException if return code of process is above 0
	 * @throws InterruptedException
	 */
	private boolean executeWithView(ArgumentListBuilder cmd, OutputStream out) throws IOException, InterruptedException {
		return execute(cmd, null, out, true);
	}
	
	/**
	 * @param cmd
	 * @param workDir where we execute the command from
	 * @param out output from executed command is pushed to this steram
	 * @param setView we set the given view if this is true
	 * @return true if command was successfully executed
	 * @throws IOException if return code of process is above 0
	 * @throws InterruptedException 
	 */
	private boolean execute(ArgumentListBuilder cmd, FilePath workDir, OutputStream out, boolean needsView) throws IOException, InterruptedException {
		if (workDir == null) {
			workDir = workspace;
		}
		//append neccessary flags and command for execution
		cmd = appendOptions(cmd, needsView);

		//setting ProcStarter properties
		Launcher.ProcStarter procStarter = this.launcher.launch().cmds(cmd).pwd(workDir);
		
		if (out != null) {
			procStarter = procStarter.stdout(out);
		}
		int ret = procStarter.join();
		
		if (ret != 0) {
			String errMsg = String.format("ClearTool: Exit code wasn't ok, " + 
										"code: %d. Tried to execute: %s", ret, cmd.toStringWithQuote());
			if (listener != null) { 
				DebugHelper.error(listener, errMsg);
			}
			
			if (out != null) {
				out.flush();
				out.close();
			}
			throw new IOException(errMsg);
		}
		return true;
	}
	
	private SimpleClearCaseChangeLogEntry parseRawLsHistoryEntry(String readline) {
		//ClearCase returns with a specific formatting on date
		SimpleDateFormat fmt = new SimpleDateFormat(LSHISTORY_ENTRY_DATE_FORMAT, 
											new Locale(PropertiesUtil.getLocale()));

		//see the ChangeLogEntry.LSHISTORY_FORMATTING for details regarding parameters in one entry		
		String[] splitted = readline.split(SimpleClearCaseChangeLogEntry.LSHISTORY_SPLIT_SEQUENCE);
		
		Date entryDate = null;
		
		try {
			entryDate = fmt.parse(splitted[0]);
		} catch (ParseException e) {
			//if we cannot parse the date then the whole entry will be irrelevant and we just return null
			//calling method will log the line
		}

		if (entryDate == null) {
			return null; 
		}
		
		String user 			= splitted[1];
		String path 			= splitted[2];
		String version  		= splitted[3];
		String eventDescription = splitted[4];
		String operation		= splitted[5];
		String comment  		= (splitted.length > 6) ? splitted[6] : ""; //if there isn't a comment we have an element less
		


		//the constructor of ChangeLogEntry follows LSHISTORY_FORMATTING parameter order
		return new SimpleClearCaseChangeLogEntry(entryDate, user, path, version, eventDescription,
																				operation, comment);
	}
}