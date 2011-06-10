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
	private String eventDescription;
	private String operation;
	private String comment;
	private SimpleClearCaseChangeLogSet parent;
	
	public SimpleClearCaseChangeLogEntry() {
		//Default constructor is needed for Digester XML-parser
	}
	
	public SimpleClearCaseChangeLogEntry(Date date, String user, String version, String eventDescription, 
																		String operation, String comment) {
		this.date   		  = date;
		this.user   		  = user;
		this.version		  = version;
		this.eventDescription = eventDescription;
		this.operation  	  = operation;
		this.comment		  = comment;
	}
	
	public SimpleClearCaseChangeLogEntry(Date date, String user, String path, String version, 
									String eventDescription, String operation, String comment) {
		this(date, user, version, eventDescription, operation, comment);
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
	
	public String getOperation() {
		return operation;
	}
	
	public void setOperation(String operation) {
		this.operation = operation;
	}
	
	public String getEventDescription() {
		return eventDescription;
	}
	
	public void setEventDescription(String eventDescription) {
		this.eventDescription = eventDescription;
	}
	
	public String getComment() {
		return comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
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
