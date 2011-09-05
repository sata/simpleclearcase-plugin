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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jenkins.plugins.simpleclearcase.SimpleClearCaseChangeLogSet;
import jenkins.plugins.simpleclearcase.util.DateUtil;
import jenkins.plugins.simpleclearcase.util.DebugHelper;
import jenkins.plugins.simpleclearcase.util.OsUtil;
import jenkins.plugins.simpleclearcase.util.PropUtils;

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

    //due to syncronization problem with Calendar we need to have an private instance to DateUtil
    private DateUtil dateUtil;
    private SimpleClearCaseChangeLogParser changeLogParser;
    
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @DataBoundConstructor
    public SimpleClearCaseSCM(String loadRules, String viewname) {
        this.loadRules = loadRules;
        this.viewname = viewname;
    
        dateUtil = new DateUtil();
        changeLogParser = new SimpleClearCaseChangeLogParser();
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build, Launcher launcher, 
                                         TaskListener listener) throws IOException, InterruptedException {

        if (build == null) {
            DebugHelper.info(listener, "%s: Build is null, returning null", LOG_CALC_REVISIONS_FROM_BUILD);
            return null;
        }

        DebugHelper.info(listener, "%s: Build Number is: %s", LOG_CALC_REVISIONS_FROM_BUILD, build.getNumber());    

        if (build.getChangeSet().isEmptySet() == true) {
            // if the changeset is empty then we cant give any revision state
            DebugHelper.info(listener, "%s: Build lacks a changeSet, returning null, buildNumber is: %d", LOG_CALC_REVISIONS_FROM_BUILD, build.getNumber());
            return null;
        } 


        // fetch the latest commit dates from the last build for comparison
        SimpleClearCaseChangeLogSet set = (SimpleClearCaseChangeLogSet) build.getChangeSet();
        LoadRuleDateMap changeSetCommits = set.getLatestCommitDates(getLoadRulesAsList());

        // info purposes
        DebugHelper.info(listener, "%s: Latest commit from builds changeSet: %s", LOG_CALC_REVISIONS_FROM_BUILD, changeSetCommits);

        return new SimpleClearCaseRevisionState(changeSetCommits);
    }


    @Override
    protected PollingResult compareRemoteRevisionWith(AbstractProject<?, ?> project, Launcher launcher,
                                  FilePath workspace, TaskListener listener, SCMRevisionState baseline)
                                                                throws IOException, InterruptedException {

        // if there is no baseline it means we haven't built before, hence build
        final AbstractBuild<?, ?> lastBuild = project.getLastBuild();
        
        if (lastBuild == null) {
            DebugHelper.info(listener, "%s: This is the first build as there isn't any previous build, we return BUILD_NOW", LOG_COMPARE_REMOTE_REVISION_WITH);
            return PollingResult.BUILD_NOW;
        } 
        
        DebugHelper.info(listener, "%s: Last build: #%s", LOG_COMPARE_REMOTE_REVISION_WITH, lastBuild.getNumber());

        ClearTool ct = new ClearTool(launcher, listener, workspace, viewname);
        LoadRuleDateMap baselineCommits = ((SimpleClearCaseRevisionState) baseline).getLoadRuleDateMap();

        DebugHelper.info(listener, "%s: Baseline commits from RevisionState is: %s",
                                                       LOG_COMPARE_REMOTE_REVISION_WITH, baselineCommits);

        // we send baselines LoadRuleDateMap to cleartool to limit the size of
        // the data fetched from lshistory
        LoadRuleDateMap remoteRevCommits = ct.getLatestCommitDates(getLoadRulesAsList(), 
                                                                                         baselineCommits);
        // meaning that there are no more entries from the time of last build,
        // hence we don't build
        if (remoteRevCommits.isDatesEmpty() == true) {
            DebugHelper.info(listener, "%s: There is no later commits, remoteRevisionCommits dates are null returning NO_CHANGES",
                                                                                                LOG_COMPARE_REMOTE_REVISION_WITH);
            return PollingResult.NO_CHANGES;
        }

        DebugHelper.info(listener, "%s: Remote revision date time is:%s",
                                                 LOG_COMPARE_REMOTE_REVISION_WITH, remoteRevCommits);

        /*
           we need a quiet period to be sure that someone isn't in the middle of commit session.
           quiet time works as we compare remoteRevisionDate added with quiet period against current time
           if it's not before, it means that quiet period has not passed yet, 
           which means we signal no changes
         */
        if (dateUtil.anyDateBefore(remoteRevCommits, new Date(), PropUtils.getQuietPeriod()) == false) {
            DebugHelper.info(listener, "%s: Still in quiet period, returning NO_CHANGES", 
                                                                        LOG_COMPARE_REMOTE_REVISION_WITH);
            return PollingResult.NO_CHANGES;
        }

        // if baseline has a load rule which its date is before the date of the
        // remote revision then it means there are changes
        if (baselineCommits.isBefore(remoteRevCommits) == true) {
            DebugHelper.info(listener, "%s: There are new commits, baseline commit dates for load rule " +
            		           "is before remote, returning BUILD_NOW", LOG_COMPARE_REMOTE_REVISION_WITH);
            return PollingResult.BUILD_NOW;
        } else {
            DebugHelper.info(listener, "%s: Baseline build dates are equal or newer than repo, " +
            		                           " returning NO_CHANGES", LOG_COMPARE_REMOTE_REVISION_WITH);
            return PollingResult.NO_CHANGES;
        }
    }

    @Override
    public boolean checkout(AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, 
                    BuildListener listener, File changelogFile) throws IOException, InterruptedException {

        DebugHelper.info(listener, "%s: Starting to 'checkout'", LOG_CHECKOUT);
        ClearTool ct = new ClearTool(launcher, listener, workspace, viewname);

        LoadRuleDateMap changelogSetCommits = null;
        // we don't have a latest commit date as we haven't tracked the
        // changelog due to the lack of previous builds.
        if (build.getPreviousBuild() != null 
                                       && build.getPreviousBuild().getChangeSet().isEmptySet() == false) {

            // From the previous ChangeLogSet we will fetch the date, such that
            // the lshistory output in ClearTool doesn't present information already reviewed
            SimpleClearCaseChangeLogSet previousChangeLogSet = (SimpleClearCaseChangeLogSet) build
                                                                       .getPreviousBuild().getChangeSet();
            changelogSetCommits = previousChangeLogSet.getLatestCommitDates(getLoadRulesAsList());

            DebugHelper.info(listener,"%s: Fetched Dates from previous builds changelog: %s",
                                                                       LOG_CHECKOUT, changelogSetCommits);
        } else {
            DebugHelper.info(listener, "%s: There is no Previous build or its empty, we invoke lshistory with null date",
                                                                                                            LOG_CHECKOUT);
        }

        List<SimpleClearCaseChangeLogEntry> entries = ct.lshistory(getLoadRulesAsList(), 
                                                                                     changelogSetCommits);
        // sort the entries according to 'setting'
        Collections.sort(entries, new SimpleClearCaseChangeLogEntryDateComparator(
                                                                  SimpleClearCaseSCM.CHANGELOGSET_ORDER));
        // create the set with entries
        SimpleClearCaseChangeLogSet set = new SimpleClearCaseChangeLogSet(build, entries);
        return changeLogParser.writeChangeLog(changelogFile, set, listener);
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
