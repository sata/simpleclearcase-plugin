package jenkins.plugins.simpleclearcase.util;

import java.util.ArrayList;
import java.util.List;

import jenkins.plugins.simpleclearcase.LoadRuleDateMap;
import jenkins.plugins.simpleclearcase.SimpleClearCaseChangeLogEntry;

public class ListUtil {

    
    /**
     * @param entries
     * @param loadRuleMap
     * @param loadRules 
     * @return true if method was successful in removing the  entries where 
     * a loadrule is prefixing any path of the entry and also where the date is matching the entries date.
     */
    public static boolean removeEntries(List<SimpleClearCaseChangeLogEntry> entries, 
                                                            LoadRuleDateMap loadRuleMap, List<String> loadRules) {
        List<SimpleClearCaseChangeLogEntry> entriesToRemove = new ArrayList<SimpleClearCaseChangeLogEntry>();

        for (SimpleClearCaseChangeLogEntry entry : entries) {

            //for each entry we have to compare against each load rule
            for (String loadRule : loadRules) {
                if (entry.containsPathWithPrefix(loadRule) && 
                        entry.getDate().compareTo(loadRuleMap.getBuiltTime(loadRule)) == 0) {
                    entriesToRemove.add(entry); //we add the entry we want to remove later on
                    break; //found a match for entry, then we skip comparing against other load rules
                }
            }
        }
        return entries.removeAll(entriesToRemove);
    }
}
