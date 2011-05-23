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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import jenkins.plugins.simpleclearcase.util.DateUtil;
import jenkins.plugins.simpleclearcase.util.DebugHelper;

import org.jvnet.localizer.ResourceBundleHolder;

import hudson.FilePath;
import hudson.Launcher;
import hudson.util.ArgumentListBuilder;

public class ClearTool {
	
	private static final String ADDED_FILE_ELEMENT_QUOTATION = "\"";
	private static final String ADDED_FILE_ELEMENT  		 = "Added file element";
	
	private static final String LSVIEW    = "lsview"; 
	private static final String LSHISTORY = "lshistory";
	
	private static final String PARAM_ALL   = "-all";
	private static final String PARAM_TAG   = "-tag";
	private static final String PARAM_SINCE = "-since";
	private static final String PARAM_FMT   = "-fmt";
	private static final String PARAM_NCO   = "-nco";
	private static final String PARAM_LAST 	= "-last";
	
	private static final String LSHISTORY_SPLIT_SEQUENCE	= "\" \"";
	private static final String LSHISTORY_ENTRY_DATE_FORMAT = "yyyyMMdd.HHmmss";
	private static final String SINCE_DATE_FORMAT   		= "d-MMM-yy.HH:mm:ss'UTC'Z";

	private Launcher launcher;
	private FilePath workspace;
	
	public ClearTool(Launcher launcher, FilePath workspace) {
		this.launcher  = launcher;
		this.workspace = workspace;
	}
	
	public boolean doesViewExist(String viewTag) throws InterruptedException {
		ArgumentListBuilder cmd = new ArgumentListBuilder();
		
		cmd.add(LSVIEW);
		cmd.add(PARAM_TAG, viewTag);

		try {
			execute(cmd);
		} catch (IOException e) {
			return false; //if there isn't such view 
		}
		return true;
	}
	
	/**
	 * @param loadRules the paths where to fetch commit dates from
	 * @return the latest commit date on any of the load rules
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public Date getLatestCommitDate(List<String> loadRules) throws InterruptedException, IOException {
		ArrayList<SimpleClearCaseChangeLogEntry> entries = new ArrayList<SimpleClearCaseChangeLogEntry>();

		for (String lr : loadRules) {
			SimpleClearCaseChangeLogEntry c = lastLshistory(lr);
			
			if (c != null) {
				entries.add(c);
			}
		}
		return DateUtil.getLatestDate(entries);
	}
	
	
	/**
	 * @param loadRules All the file paths which we want to fetch commit changes from SCM
	 * @param since specifies from when, we don't want to fetch information which has been collected for previous builds
	 * @return a list of all changelog entries, in order of the the date stamps
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public List<SimpleClearCaseChangeLogEntry> lshistory(List<String> loadRules, Date since) throws InterruptedException, IOException {
			List<SimpleClearCaseChangeLogEntry> entries = new ArrayList<SimpleClearCaseChangeLogEntry>();
			
			for (String lr : loadRules) {
				List<SimpleClearCaseChangeLogEntry> l  = lshistory(lr, since, false);
				
				if (l != null) {
					entries.addAll(l);
				}
			}
			//sort the entries according to their date
			Collections.sort(entries, new SimpleClearCaseChangeLogEntryDateComparator());
			
			return entries;
	}
	
	/**
	 * @param filePath
	 * @param since
	 * @param onlyLast take only the last entry from the filepath
	 * @return a list of ChangeLog entries, parsed from the rawLshistory
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private List<SimpleClearCaseChangeLogEntry> lshistory(String filePath, Date since, boolean onlyLast) throws InterruptedException, IOException {
		BufferedReader in = new BufferedReader(rawLshistory(filePath, since, onlyLast));
		List<SimpleClearCaseChangeLogEntry> ret = new ArrayList<SimpleClearCaseChangeLogEntry>();
		
		String readline = in.readLine();

		SimpleClearCaseChangeLogEntry currentEntry  = null;
		
		while (readline != null) {
			
			//a commit entry could be split over several lines hence we need to check for additional info
			if (readline.startsWith(ADDED_FILE_ELEMENT)) {
				if (currentEntry == null) {
					DebugHelper.error(launcher, "CurrentEntry is null when a ADDED_FILE_ELEMENT popped up, " + 
																							"shouldn't happen.");
				}
				//with the formatting we have the ADDED_FILE_ELEMENT row wraps the filepath with quote.
				int startIndex = readline.indexOf(ADDED_FILE_ELEMENT_QUOTATION) + 1; 
				int endIndex   = readline.indexOf(ADDED_FILE_ELEMENT_QUOTATION, startIndex);
				currentEntry.addPath(readline.substring(startIndex, endIndex));
				
				continue;
				} 
			
			currentEntry = parseEntry(readline);
			
			if (currentEntry != null) {
				ret.add(currentEntry);
			} else {
				DebugHelper.error(launcher, "Could not parse entry from lshistory: %s", readline);
			}
			readline = in.readLine();
		}
		return ret;
	}
	
	private SimpleClearCaseChangeLogEntry lastLshistory(String loadRule) throws InterruptedException, IOException {
		List<SimpleClearCaseChangeLogEntry> entries = lshistory(loadRule, null, true);
		SimpleClearCaseChangeLogEntry ret = null;
		
		if (entries.size() > 1) {
			DebugHelper.error(launcher, "LastLshistory call gave more entries than it should, loadrule: %s",
																									loadRule);
		} else if (entries.size() == 1) {
			ret = entries.get(0);
		}
		return ret;
	}
	
	/**
	 * @param filePath to the element in repository
	 * @param since from when we want to fetch history entries from
	 * @param onlyLast only fetch the last entry for the filePath
	 * @return reader to the byte array input stream with raw entries
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private Reader rawLshistory(String filePath, Date since, boolean onlyLast) throws InterruptedException, IOException {
		ArgumentListBuilder cmd = new ArgumentListBuilder();

		//fetching locale and time zone settings from properties file
		SimpleDateFormat fmt = new SimpleDateFormat(SINCE_DATE_FORMAT, 
					new Locale(ResourceBundleHolder.get(SimpleClearCaseSCM.class).format("Locale")));
		fmt.setTimeZone(TimeZone.getTimeZone(
					ResourceBundleHolder.get(SimpleClearCaseSCM.class).format("TimeZone")));

		cmd.add(LSHISTORY);
		/*cmd.add(PARAM_ALL); should we really have ALL? */

		//since parameter isn't interesting if we only fetch the last entry
		if (onlyLast == true) {
			cmd.add(PARAM_LAST);
		} else {
			cmd.add(PARAM_SINCE, fmt.format(since).toLowerCase());
		}
		
		cmd.add(PARAM_FMT, SimpleClearCaseChangeLogEntry.LSHISTORY_FORMATTING);
		cmd.add(PARAM_NCO);
		cmd.add(filePath);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		execute(cmd, baos);
		
		Reader ret = new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()));
		baos.close();
		
		return ret;
	}
	
	private boolean execute(ArgumentListBuilder cmd) throws IOException, InterruptedException {
		return execute(cmd, null, null);
	}
	
	private boolean execute(ArgumentListBuilder cmd, OutputStream out) throws IOException, InterruptedException {
		return execute(cmd, null, out);
	}
	
	private boolean execute(ArgumentListBuilder cmd, FilePath workDir, OutputStream out) throws IOException, InterruptedException {
		
		if (workDir == null) {
			workDir = workspace;
		}
		
		Launcher.ProcStarter procStarter = this.launcher.launch();
		//setting ProcStarter properties
		procStarter = procStarter.cmds(cmd).pwd(workDir).stdout(out);
		
		int ret = procStarter.join();	
		
		if (ret != 0) {
			String errMsg = String.format("ClearTool: Exit code wasn't ok, " + 
											"code: %d. Tried to execute: %s", ret, cmd.toString());
			DebugHelper.error(launcher, errMsg);
			
			throw new IOException(errMsg);
		}
		return true;
	}
	
	private SimpleClearCaseChangeLogEntry parseEntry(String readline) {
		//here we must parse out each parameter 
		String[] splitted = readline.split(LSHISTORY_SPLIT_SEQUENCE);
		//see the ChangeLogEntry.LSHISTORY_FORMATTING for details regarding parameters in one entry
		
		//ClearCase returns with a specific formatting on date
		SimpleDateFormat fmt = new SimpleDateFormat(LSHISTORY_ENTRY_DATE_FORMAT, 
					new Locale(ResourceBundleHolder.get(SimpleClearCaseSCM.class).format("Locale")));
		
		Date entryDate = null;
		
		try {
			entryDate = fmt.parse(splitted[0]);
		} catch (ParseException e) {
			DebugHelper.error(launcher, "Cannot parse Date recieved from raw " + 
								"lshistory, date string: %s", splitted[0]);
		}
		
		if (entryDate == null) {
			return null; //if we cannot parse the date then the whole entry will be irrelevant 
		}
		
		//the constructor of ChangeLogEntry follows LSHISTORY_FORMATTING parameter order
		return new SimpleClearCaseChangeLogEntry(entryDate, splitted[1], splitted[2], splitted[3], 
								splitted[4], splitted[5], launcher.isUnix());
	}
}
