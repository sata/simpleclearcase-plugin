package jenkins.plugins.simpleclearcase.util;

import java.util.ArrayList;
import java.util.Date;
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

    /**
     * @param loadRules
     * @return a LoadRuleDateMap which maps a load rule against the latest commit date of that 
     *         specific load rule
     */
    public static LoadRuleDateMap getLatestCommitDates(List<SimpleClearCaseChangeLogEntry> entries, List<String> loadRules) {
        LoadRuleDateMap ret = new LoadRuleDateMap();

        for (String lr : loadRules) {
            ret.setBuildTime(lr, getLatestCommitDate(entries, lr));
        }
        return ret;
    }

    /**
     * @param loadRule
     * @return the latest commit date from all of the entries who are fetched from load rule 'loadrule'
     */
    private static Date getLatestCommitDate(List<SimpleClearCaseChangeLogEntry> entries, String loadRule) {
        List<SimpleClearCaseChangeLogEntry> prefixedEntries = 
                new ArrayList<SimpleClearCaseChangeLogEntry>();

        for (SimpleClearCaseChangeLogEntry entry : entries) {
            if (entry.containsPathWithPrefix(loadRule) == true) {
                prefixedEntries.add(entry);
            }
        }
        return DateUtil.getLatestDate(prefixedEntries);
    }


}
