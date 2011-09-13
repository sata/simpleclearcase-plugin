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
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package jenkins.plugins.simpleclearcase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jenkins.plugins.simpleclearcase.SimpleClearCaseChangeLogSet;
import jenkins.plugins.simpleclearcase.util.DebugHelper;
import jenkins.plugins.simpleclearcase.util.OsUtil;

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
import hudson.model.Hudson;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.scm.SCM;
import hudson.util.FormValidation;

public class SimpleClearCaseSCM extends SCM {

    public final static String LOG_COMPARE_REMOTE_REVISION_WITH = "compareRemoteRevisionWith";
    public final static String LOG_CHECKOUT                     = "checkout";
    public final static String LOG_CALC_REVISIONS_FROM_BUILD    = "calcRevisionsFromBuild";

    public final static int CHANGELOGSET_ORDER = SimpleClearCaseChangeLogEntryDateComparator.DECREASING;

    private String loadRules;
    private String viewname;
        
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @DataBoundConstructor
    public SimpleClearCaseSCM(String loadRules, String viewname) {
        this.loadRules = loadRules;
        this.viewname = viewname;
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build, Launcher launcher, 
                                         TaskListener listener) throws IOException, InterruptedException {
        DebugHelper.info(listener, "%s: Since we add RevisisonState as an action in checkout this method " + 
                                                        "should not be invoked", LOG_CALC_REVISIONS_FROM_BUILD);
        return null;
    }


    @Override
    protected PollingResult compareRemoteRevisionWith(AbstractProject<?, ?> project, Launcher launcher,
                                  FilePath workspace, TaskListener listener, SCMRevisionState scmRevisionState)
                                                                throws IOException, InterruptedException {
        ClearTool ct = new ClearTool(launcher, listener, workspace, viewname);
        final AbstractBuild<?, ?> lastBuild = project.getLastBuild();
        
        SimpleClearCaseRevisionState baseline = (SimpleClearCaseRevisionState) scmRevisionState;
        LoadRuleDateMap baselineLRMap = baseline.getLoadRuleDateMap();
        LoadRuleDateMap remoteLRMap;
        SimpleClearCaseRevisionState remote;
        PollingResult.Change change;
        
        DebugHelper.info(listener, "%s: Last build: #%s", LOG_COMPARE_REMOTE_REVISION_WITH, lastBuild.getNumber());
        DebugHelper.info(listener, "%s: Baseline LR-mapping from RevisionState is: %s",
                                                       LOG_COMPARE_REMOTE_REVISION_WITH, baselineLRMap);

        // we send baselines LoadRuleDateMap to cleartool to limit the size of
        // the data fetched from lshistory. To speed up the polling.
        remoteLRMap = ct.getLatestCommitDates(getLoadRulesAsList(), baselineLRMap);
        
        DebugHelper.info(listener, "%s: remoteLRMap is: %s", LOG_COMPARE_REMOTE_REVISION_WITH, remoteLRMap);
        
        // if baseline has a load rule which its date is before the date of the
        // remote revision then it means there are changes        
        if (baselineLRMap.isBefore(remoteLRMap) == true) {
            DebugHelper.info(listener, "%s: There are new commits, baseline dates for load rule " +
            		           "is before remote, returning BUILD_NOW with a new remoteLRMap", LOG_COMPARE_REMOTE_REVISION_WITH);
            change = PollingResult.Change.SIGNIFICANT;
            remote = new SimpleClearCaseRevisionState(remoteLRMap, baseline.getBuildNumber());
        } else {
            DebugHelper.info(listener, "%s: Baseline build dates are equal or newer than repo, " +
            		                           " returning NO_CHANGES with baseline = remote", LOG_COMPARE_REMOTE_REVISION_WITH);
            change = PollingResult.Change.NONE;
            remote = baseline;
        }
        
        DebugHelper.info(listener, "%s: Returning PollingResult with these parameters: baseline: %s, remote: %s, Change: %s", 
                     LOG_COMPARE_REMOTE_REVISION_WITH, baseline.getLoadRuleDateMap(), remote.getLoadRuleDateMap(), change);
        
        return new PollingResult(baseline, remote, change);
    }

    @Override
    public boolean checkout(AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, 
                    BuildListener listener, File changelogFile) throws IOException, InterruptedException {

        DebugHelper.info(listener, "%s: Starting to 'checkout'", LOG_CHECKOUT);
        ClearTool ct = new ClearTool(launcher, listener, workspace, viewname);

        List<SimpleClearCaseChangeLogEntry> entries;
                
        // we don't have a latest commit date as we haven't tracked the
        // changelog due to the lack of previous builds.
        if (build.getPreviousBuild() != null && build.getPreviousBuild().getAction(SimpleClearCaseRevisionState.class) != null) {
            LoadRuleDateMap previousBuildLRMap  = build.getPreviousBuild().getAction(SimpleClearCaseRevisionState.class).getLoadRuleDateMap();

            DebugHelper.info(listener,"%s: Fetched Dates from previous builds RevisionState LRMap: %s",
                                                                LOG_CHECKOUT, previousBuildLRMap);
            entries = ct.lshistory(getLoadRulesAsList(), previousBuildLRMap);
        } else {
            DebugHelper.info(listener, "%s: There is no Previous build or there isn't any RevisionState added, " + 
                                                                " we invoke lshistory with null date", LOG_CHECKOUT);
            //if we don't have any RevisionState from previous build.
            entries = ct.lshistory(getLoadRulesAsList(), null);
        }

        // sort the entries according to 'setting'
        Collections.sort(entries, new SimpleClearCaseChangeLogEntryDateComparator(
                                                                  SimpleClearCaseSCM.CHANGELOGSET_ORDER));
        // create the set with entries
        SimpleClearCaseChangeLogSet set = new SimpleClearCaseChangeLogSet(build, entries);
        
        // from the entries we just fetched, we build a LR-map for the new revisionState
        LoadRuleDateMap buildLRMap = set.getLatestCommitDates(getLoadRulesAsList());
        
        SimpleClearCaseRevisionState buildRevisionState = new SimpleClearCaseRevisionState(buildLRMap, build.getNumber());
        build.addAction(buildRevisionState);
        
        DebugHelper.info(listener, "%s: the add Action buildRevState number is: %d, LRMap is: %s", LOG_CHECKOUT, 
                                                    buildRevisionState.getBuildNumber(), buildRevisionState.getLoadRuleDateMap());
        DebugHelper.info(listener, "%s: Added RevisionState in checkout for build", LOG_CHECKOUT);
        
        return ((SimpleClearCaseChangeLogParser) createChangeLogParser()).writeChangeLog(changelogFile, set, listener);
    }

    @Override
    public boolean requiresWorkspaceForPolling() {
        return true;
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

    public String getLoadRules() {
        return loadRules;
    }

    public List<String> getLoadRulesAsList() {
        return splitLoadRules(loadRules);
    }

    /**
     * @param lr
     * @return
     */
    private static List<String> splitLoadRules(String lr) {
        // character class matches against both \r and \n, as WIN_NEWLINE defines both \r\n, we don't need
        // to refer to UNIX_NEWLINE, as it's only the \n character. [] is an character regex
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

        public FormValidation doCheckViewname(@QueryParameter String value)
                                                                throws InterruptedException, IOException {
            if (value == null || value.trim().isEmpty() == true) {
                return FormValidation.error(Messages.simpleclearcase_viewname_empty());
            }

            if (value.contains(" ") == true) {
                return FormValidation.error(Messages.simpleclearcase_viewname_whitespace());
            }

            Launcher launcher = Hudson.getInstance().createLauncher(TaskListener.NULL);
            ClearTool ct = new ClearTool(launcher, null, null, value);

            if (ct.doesViewExist(value) == false) {
                return FormValidation.error(Messages.simpleclearcase_viewname_doesntexist());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckLoadRules(@QueryParameter String value,
                                               @QueryParameter("viewname") String viewname)
                                                                throws InterruptedException, IOException {
            if (value == null || value.trim().isEmpty() == true) {
                return FormValidation.error(Messages.simpleclearcase_loadRules_empty());
            }

            if (value.contains(" ") == true) {
                return FormValidation.error(Messages.simpleclearcase_loadRules_whitespace());
            }
            // remove duplications and check if sizes differ
            List<String> splittedRules = splitLoadRules(value);
            Set<String> uniqueSet = new HashSet<String>(splittedRules);

            if (uniqueSet.size() < splittedRules.size()) {
                return FormValidation.error(Messages.simpleclearcase_loadRules_duplicated_loadrule());
            }

            // checking for trailing slashes
            for (String lr : splittedRules) {
                if (lr.endsWith("/") || lr.endsWith("\\")) {
                    return FormValidation.error(Messages.simpleclearcase_loadRules_trailingslash());
                }
            }
            
            // checking if any load rule is a prefix of any other load rule
            for (String s : splittedRules) {
                for (String q : splittedRules) {
                    if (s == q) {
                        // as duplications are removed, the only time they can be equal
                        // is if the comparison is over the same object, which we don't want
                        continue;
                    }
                    if (s.startsWith(q) == true) {
                        return FormValidation.error(Messages.simpleclearcase_loadRules_loadruleprefixed()+ q);
                    }
                }
            }
            
            // check if paths actually exists, as its the heaviest task its last
            Launcher launcher = Hudson.getInstance().createLauncher(TaskListener.NULL);
            ClearTool ct = new ClearTool(launcher, null, null, viewname);

            for (String lr : splittedRules) {
                if (ct.doesClearCasePathExist(lr) == false) {
                    return FormValidation.error(Messages.simpleclearcase_loadRules_pathdoesnotexist() + lr);
                }
            }
            return FormValidation.ok();
        }
    }
}
