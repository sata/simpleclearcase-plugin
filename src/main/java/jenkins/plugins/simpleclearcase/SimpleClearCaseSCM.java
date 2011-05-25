package jenkins.plugins.simpleclearcase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jenkins.plugins.simpleclearcase.SimpleClearCaseChangeLogSet;
import jenkins.plugins.simpleclearcase.util.DebugHelper;
import jenkins.plugins.simpleclearcase.util.OsUtil;

import org.jvnet.localizer.ResourceBundleHolder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.ModelObject;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.scm.SCM;
import hudson.util.FormValidation;

public class SimpleClearCaseSCM extends SCM {
	
	private List<String> loadRules;
	private String workspace;
	private String viewname;
	
	@Extension
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	@DataBoundConstructor
	public SimpleClearCaseSCM(String loadRules, String viewname) {
		this.loadRules  = splitLoadRules(loadRules);
		this.viewname   = viewname;
	}
	
	@Override
	public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build,
			Launcher launcher, TaskListener listener) throws IOException,
			InterruptedException {

		if (build == null) {
			return null;
		} else {
			DebugHelper.info(launcher, "calcRevisionFromBuild - the build time is: %s", 
																				build.getTime());
			//fetch the latest commit date from the last build for comparison 
			Date latestCommit = ((SimpleClearCaseChangeLogSet) build.getChangeSet()).getLatestCommitDate();
			
			return new SimpleClearCaseRevisionState(latestCommit);
		}
	}

	@Override
	protected PollingResult compareRemoteRevisionWith(
			AbstractProject<?, ?> project, Launcher launcher,
			FilePath workspace, TaskListener listener, SCMRevisionState baseline)
			throws IOException, InterruptedException {
		
		DebugHelper.info(launcher, "compareRemoteRevisionWith - testing");
		
		//if there is no baseline it means we haven't built before, hence build
		if (baseline == null) {
			return PollingResult.BUILD_NOW;
		}
		ClearTool ct = new ClearTool(launcher, workspace);
		
		Date remoteRevisionDate = ct.getLatestCommitDate(getLoadRules()); 
		
		if (((SimpleClearCaseRevisionState) baseline).getBuiltTime().before(remoteRevisionDate)) {
			DebugHelper.info(launcher, "compareRemoteRevisionWith - build now");
			return PollingResult.BUILD_NOW;
		} else { 
			DebugHelper.info(launcher, "compareRemoteRevisionWith - no change");
			return PollingResult.NO_CHANGES;
		}
	}

	@Override
	public boolean checkout(AbstractBuild<?, ?> build, Launcher launcher,
			FilePath workspace, BuildListener listener, File changelogFile)
			throws IOException, InterruptedException {
		
		DebugHelper.info(launcher, "Checkout - start");		
		ClearTool ct = new ClearTool(launcher, workspace);
		
		SimpleClearCaseChangeLogSet previousChangeLogSet = (SimpleClearCaseChangeLogSet) build.getPreviousBuild().getChangeSet();

		//From the previous ChangeLogSet we will fetch the date, such that the lshistory output in ClearTool
		//dosn't present information already reviewed
		List<SimpleClearCaseChangeLogEntry> entries = ct.lshistory(getLoadRules(), previousChangeLogSet.getLatestCommitDate());
		
		//create the set with entries
		SimpleClearCaseChangeLogSet set = new SimpleClearCaseChangeLogSet(build, entries);
		
		//write down ChangeLogSet to file
		return SimpleClearCaseChangeLogParser.writeChangeLog(changelogFile, set);
	}
	
	@Override
	public boolean requiresWorkspaceForPolling() {
		return false;
	}
	
	@Override
	public ChangeLogParser createChangeLogParser() {
		return new SimpleClearCaseChangeLogParser();
	}
	
	/**
	 * @return
	 */
	public String getViewname() {
		return viewname;
	}

	public List<String> getLoadRules() {
		return loadRules;
	}

	/**
	 * @param lr
	 * @return
	 */
	private List<String> splitLoadRules(String lr) {
		//character class matches against both \r and \n, as WIN_NEWLINE defines both \r\n, we don't need
		//to refer to UNIX_NEWLINE, as it's only the \n character. [] is an character regex 
		String[] split = lr.split(String.format("[%s]+", OsUtil.WIN_NEWLINE));
		List<String> ret = new ArrayList<String>();
		
		for (String s : split) {
			if (s.length() > 0) {
				s.trim();
				ret.add(s);
			}
		}
		return ret;
	}
	
	/**
	 * @author etavsam
	 *
	 */
	public static class DescriptorImpl extends SCMDescriptor<SCM> implements ModelObject {

		protected DescriptorImpl() {
			super(null);
		}
		
		/* (non-Javadoc)
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return Messages.simpleclearcase_DisplayName();
		}
		
		public FormValidation doCheckViewname(@QueryParameter String value) {
			
			if (value == null || value.trim().isEmpty() == true) {
				return FormValidation.error(Messages.simpleclearcase_viewname_empty());
			}
			
			if (value.contains(" ") == true) {
				return FormValidation.error(Messages.simpleclearcase_viewname_whitespace());
			}
			
			/* validation for windows is not implemented */
			
			return FormValidation.ok();
		}
		
		public FormValidation doCheckLoadRules(@QueryParameter String value) {
			if (value == null || value.trim().isEmpty() == true) {
				return FormValidation.error(Messages.simpleclearcase_loadRules_empty());
			}
			
			if (value.contains(" ") == true) {
				return FormValidation.error(Messages.simpleclearcase_viewname_whitespace());
			}
			
			/* validation for windows is not implemented */
			
			return FormValidation.ok();
		}
	}
}
