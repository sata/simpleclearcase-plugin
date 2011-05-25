package jenkins.plugins.simpleclearcase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import hudson.model.User;
import hudson.scm.ChangeLogSet;
import jenkins.plugins.simpleclearcase.util.OsUtil;

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
	public static final String LSHISTORY_FORMATTING = "'\"%Nd\" \"%u\" \"%En\" \"%Vn\" \"%e\" \"%o\" \n%c\n'";
	
	private Date date;
	private String user;
	private List<FileElement> elements = new ArrayList<FileElement>();
	private String version;
	private String comment;
	private String operation;
	private boolean isUnix;
	private SimpleClearCaseChangeLogSet parent;
	
	public SimpleClearCaseChangeLogEntry(Date date, String user, String path, String version, 
									String comment, String operation, boolean isUnix) {
		this.date      = date;
		this.user      = user;
		this.version   = version;
		this.comment   = comment;
		this.operation = operation;
		this.isUnix    = isUnix;
		
		this.addPath(path);
	}

	public Date getDate() {
		return date;
	}
	
	public void addPath(String path) {
		elements.add(new FileElement(path, isUnix));
	}
	public String getUser() {
		return user;
	}
		
	public String getVersion() {
		return version;
	}
	
	public String getComment() {
		return comment;
	}
	
	public String getOperation() {
		return operation;
	}
	
	public boolean isUnix() {
		return isUnix;
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
	
	/**
	 * @param filePath
	 * @param isUnix
	 * @return the filename from path with the convention of unix
	 */
	public static String getName(String filePath, boolean isUnix) {
		int index = filePath.lastIndexOf(OsUtil.getPathSeparator(isUnix));
		return filePath.substring(index + 1);
	}

	/**
	 * @param filePath
	 * @param isUnix
	 * @return the path without the filename without trailing slash
	 */
	public static String getPath(String filePath, boolean isUnix) {
		int index = filePath.lastIndexOf(OsUtil.getPathSeparator(isUnix));
		return filePath.substring(0, index);
	}
}
