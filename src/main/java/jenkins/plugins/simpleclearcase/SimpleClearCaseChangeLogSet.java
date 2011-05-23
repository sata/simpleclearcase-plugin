package jenkins.plugins.simpleclearcase;

import hudson.model.AbstractBuild;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import jenkins.plugins.simpleclearcase.util.DateUtil;

public class SimpleClearCaseChangeLogSet extends hudson.scm.ChangeLogSet<SimpleClearCaseChangeLogEntry> {
	private List<SimpleClearCaseChangeLogEntry> entries;

	protected SimpleClearCaseChangeLogSet(AbstractBuild<?, ?> build, List<SimpleClearCaseChangeLogEntry> entries) {
		super(build);
		this.entries = entries;
	}

	/**
	 * @return the latest commit date from all of the entries
	 */
	public Date getLatestCommitDate() {
		return DateUtil.getLatestDate(entries);
	}

	public Iterator<SimpleClearCaseChangeLogEntry> iterator() {
		return entries.iterator();
	}

	@Override
	public boolean isEmptySet() {
		return entries.isEmpty();
	}
}
