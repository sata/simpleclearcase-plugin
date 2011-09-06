package jenkins.plugins.simpleclearcase;

import hudson.model.AbstractBuild;
import hudson.scm.AbstractScmTagAction;

public class SimpleClearCaseSCMTagAction extends AbstractScmTagAction {
    private LoadRuleDateMap map;
    
    protected SimpleClearCaseSCMTagAction(AbstractBuild build, LoadRuleDateMap map) {
        super(build);
        this.map = map;
    }

    public String getDisplayName() {
        return null;
    }

    public String getIconFileName() {
        return null;
    }

    @Override
    public boolean isTagged() {

        if (map == null) {
            return false;
        }
        
        if (map.isDatesEmpty() == false) {
            return false;
        } 
        
        return true;
    }
    
    public LoadRuleDateMap getLoadRuleDateMap() {
        return map;
    }
}
