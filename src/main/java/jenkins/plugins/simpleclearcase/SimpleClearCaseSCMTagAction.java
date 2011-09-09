package jenkins.plugins.simpleclearcase;

import hudson.model.AbstractBuild;
import hudson.scm.AbstractScmTagAction;

public class SimpleClearCaseSCMTagAction extends AbstractScmTagAction {
    private SimpleClearCaseRevisionState state;
    
    protected SimpleClearCaseSCMTagAction(AbstractBuild build, SimpleClearCaseRevisionState state) {
        super(build);
        this.state = state;
    }

    public String getDisplayName() {
        return null;
    }

    public String getIconFileName() {
        return null;
    }

    @Override
    public boolean isTagged() {

        if (state == null) {
            return false;
        }
        
        if (state.getLoadRuleDateMap().isDatesEmpty() == false) {
            return false;
        } 
        
        return true;
    }
}
