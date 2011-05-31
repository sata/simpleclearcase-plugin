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

import org.jvnet.localizer.ResourceBundleHolder;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;

public class ClearTool {
	
	private static final String ADDED_FILE_ELEMENT_QUOTATION = "\"";
	private static final String ADDED_FILE_ELEMENT  		 = "Added file element";
	
	private static final String CLEARTOOL = "cleartool";
	private static final String LSVIEW    = "lsview"; 
	private static final String LSHISTORY = "lshistory";
	
	private static final String PARAM_TAG     = "-tag";
	private static final String PARAM_SINCE   = "-since";
	private static final String PARAM_FMT     = "-fmt";
	private static final String PARAM_NCO     = "-nco";
	private static final String PARAM_RECURSE = "-recurse";
	
	private static final String LSHISTORY_SPLIT_SEQUENCE	= "\" \"";
	private static final String LSHISTORY_ENTRY_DATE_FORMAT = "yyyyMMdd.HHmmss";
	private static final String SINCE_DATE_FORMAT   		= "d-MMM-yy.HH:mm:ss'UTC'Z";

	private Launcher launcher;
	private TaskListener listener;
	private FilePath workspace;
	private String viewname;
	
	public ClearTool(Launcher launcher, TaskListener listener, FilePath workspace, String viewname) {
		this.launcher  = launcher;
		this.listener  = listener;
		this.workspace = workspace;
		this.viewname  = viewname;
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
	public Date getLatestCommitDate(List<String> loadRules, Date since) throws InterruptedException, IOException {
		return DateUtil.getLatestDate(lshistory(loadRules, since));
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
				List<SimpleClearCaseChangeLogEntry> l  = lshistory(lr, since);
				
				if (l != null) {
					entries.addAll(l);
				}
			}					
			return entries;
	}
	
	/**
	 * @param filePath
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
			
			//a commit entry could be split over several lines hence we need to check for additional info
			if (readline.startsWith(ADDED_FILE_ELEMENT)) {
				if (currentEntry == null) {
					DebugHelper.error(listener, "CurrentEntry is null when a ADDED_FILE_ELEMENT popped up, " + 
																							"shouldn't happen.");
				}
				//with the formatting we have the ADDED_FILE_ELEMENT row wraps the filepath with quote.
				int startIndex = readline.indexOf(ADDED_FILE_ELEMENT_QUOTATION) + 1; 
				int endIndex   = readline.indexOf(ADDED_FILE_ELEMENT_QUOTATION, startIndex);
				currentEntry.addPath(readline.substring(startIndex, endIndex));
				
				continue;
				} 
			
			currentEntry = parseRawLsHistoryEntry(readline);
			
			if (currentEntry != null) {
				ret.add(currentEntry);
			} else {
				DebugHelper.error(listener, "Could not parse entry from lshistory: %s", readline);
			}
			readline = in.readLine();
		}
		return ret;
	}
	
	/**
	 * @param filePath to the element in repository
	 * @param since from when we want to fetch history entries from
	 * @return reader to the byte array input stream with raw entries
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private Reader rawLshistory(String filePath, Date since) throws InterruptedException, IOException {
		ArgumentListBuilder cmd = new ArgumentListBuilder();

		//fetching locale and time zone settings from properties file
		SimpleDateFormat fmt = new SimpleDateFormat(SINCE_DATE_FORMAT, 
					new Locale(ResourceBundleHolder.get(SimpleClearCaseSCM.class).format("Locale")));
		fmt.setTimeZone(TimeZone.getTimeZone(
					ResourceBundleHolder.get(SimpleClearCaseSCM.class).format("TimeZone")));

		cmd.add(CLEARTOOL);
		cmd.add(LSHISTORY);
		cmd.add(PARAM_RECURSE);

		if (since != null){
			// if the date is null, there is no time bound on lshistory
			// if it's the first build there isn't any previous date to take as starting point
			cmd.add(PARAM_SINCE, fmt.format(since).toLowerCase());
		}
		
		cmd.add(PARAM_FMT);
		cmd.add(SimpleClearCaseChangeLogEntry.LSHISTORY_FORMATTING);
		cmd.add(PARAM_NCO);
		cmd.add(filePath);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		execute(cmd, baos);
		
		Reader ret = new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()));
		baos.close();
		
		return ret;
	}

	/**
	 * 
	 * @param cmd the commands we want to run against ClearCase
	 * @return a wrapped version of the ArgumentListBuilder which executes the given cmd against 
	 * ClearCase with the viewname recieved from the SCM plugin.
	 * 
	 *  Example on the 'wrapping': cmd lshistory /some/vob/somefile,
	 */
	private static ArgumentListBuilder wrapCommandsWithSetView(ArgumentListBuilder cmd) {
		return null;
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
											"code: %d. Tried to execute: %s", ret, cmd.toStringWithQuote());
			DebugHelper.error(listener, errMsg);
			
			if (out != null) {
				out.close();
			}
			
			throw new IOException(errMsg);
		}
		return true;
	}
	
	private SimpleClearCaseChangeLogEntry parseRawLsHistoryEntry(String readline) {
		//ClearCase returns with a specific formatting on date
		SimpleDateFormat fmt = new SimpleDateFormat(LSHISTORY_ENTRY_DATE_FORMAT, 
					new Locale(ResourceBundleHolder.get(SimpleClearCaseSCM.class).format("Locale")));

		//see the ChangeLogEntry.LSHISTORY_FORMATTING for details regarding parameters in one entry		
		String[] splitted = readline.split(LSHISTORY_SPLIT_SEQUENCE);

		Date entryDate = null;
		
		try {
			entryDate = fmt.parse(splitted[0]);
		} catch (ParseException e) {
			DebugHelper.error(listener, "Cannot parse Date recieved from raw " + 
								"lshistory, date string: %s", splitted[0]);
		}

		if (entryDate == null) {
			return null; //if we cannot parse the date then the whole entry will be irrelevant 
		}
		
		//the constructor of ChangeLogEntry follows LSHISTORY_FORMATTING parameter order
		return new SimpleClearCaseChangeLogEntry(entryDate, splitted[1], splitted[2], splitted[3], 
								splitted[4], splitted[5]);
	}
}
