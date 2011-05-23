package jenkins.plugins.simpleclearcase;

import java.util.Date;

import hudson.scm.SCMRevisionState;


public class SimpleClearCaseRevisionState extends SCMRevisionState {

	private Date buildTime;
	
	public SimpleClearCaseRevisionState(Date buildTime) {
		this.buildTime = buildTime;
	}
	
	public Date getBuiltTime() {
		return buildTime;
	}
}
