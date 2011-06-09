package jenkins.plugins.simpleclearcase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import hudson.model.User;
import hudson.scm.ChangeLogSet;

/**
 * @author etavsam
 *
 * This class represents a entry in the formatted lshistory output.
 * the formatting is according to LSHISTORY_FORMATTING
 * 
 */
public class SimpleClearCaseChangeLogEntry extends ChangeLogSet.Entry {

	/* 
	 * review fmt_ccase in IBM clearcase documentation for more details
	 * 
	 *  %Nd - Numeric Date
	 *  %u  - Username of comitter
	 *  %En - Element path with name
	 *  %Vn - Version Id
	 *  %e  - Event description
	 *  %o  - Operation
	 *  %c  - Comment
	 *    
	 */
	// we would add an extra single quotes around the string, but as the formatting contains white space
	// the ArgumentListBuilder will automatically quote this for us, double quoting would cause it to break down.
//	public static final String LSHISTORY_FORMATTING = "%Nd\" \"%u\" \"%En\" \"%Vn\" \"%e\" \"%o\" \"%Nc\n";
//	public static final String LSHISTORY_SPLIT_SEQUENCE	= "\" \"";
	
	// for the purpose of calling -fmt through a setview -exec we need to escape the newline character
	// otherwise it will break after invoking -exec through the spawned shell from cleartool. 
	public static final String LSHISTORY_FORMATTING = "%Nd| |%u| |%En| |%Vn| |%e| |%o| |%Nc\\n";
	public static final String LSHISTORY_SPLIT_SEQUENCE	= "\\| \\|";
	
	private Date date;
	private String user;
	private List<FileElement> elements = new ArrayList<FileElement>();
	private String version;
	private String comment;
	private String operation;

	private SimpleClearCaseChangeLogSet parent;
	
	public SimpleClearCaseChangeLogEntry() {
		//Default constructor is needed for Digester XML-parser
	}
	
	public SimpleClearCaseChangeLogEntry(Date date, String user, String version, String comment, 
										String operation) {
		this.date      = date;
		this.user      = user;
		this.version   = version;
		this.comment   = comment;
		this.operation = operation;
	}
	
	public SimpleClearCaseChangeLogEntry(Date date, String user, String path, String version, 
									String comment, String operation) {
		this (date, user, version, comment, operation);
		this.addPath(path);
	}

	public Date getDate() {
		return date;
	}
	
	public void setDate(Date date) {
		this.date = date;
	}
	
	public void addPath(String path) {
		elements.add(new FileElement(path));
	}
	public String getUser() {
		return user;
	}
		
	public void setUser(String user) {
		this.user = user;
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	public String getComment() {
		return comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	public String getOperation() {
		return operation;
	}
	
	public void setOperation(String operation) {
		this.operation = operation;
	}
	
	@Override
	public String getMsg() {
		return comment;
	}

	@Override
	public User getAuthor() {
		return User.get(user);
	}

	public SimpleClearCaseChangeLogSet getParent() {
		return parent;
	}
	
	public void setParent(SimpleClearCaseChangeLogSet set) {
		this.parent = set;
	}
	
	@Override
	public Collection<String> getAffectedPaths() {
		Collection<String> ret = new ArrayList<String>();
		for (FileElement e : elements) {
			ret.add(e.getFilePath().getRemote());
		}
		return ret;
	}
}
