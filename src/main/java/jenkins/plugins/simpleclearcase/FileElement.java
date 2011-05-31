package jenkins.plugins.simpleclearcase;

import java.io.File;

import hudson.FilePath;

/**
 * @author etavsam
 *
 * This class represents a file element inside the repository. It's a simple container 
 */
public class FileElement {
	public static final String INIT_VERSION = "0"; 
	
	private FilePath filePath;
	private String version;
	
	public FileElement() {
		
	}
	
	public FileElement(String path, String version) {
		setFilePath(path);
		this.version  = version;
	}

	public FileElement(String path) {
		this(path, INIT_VERSION);
	}
	
	public FilePath getFilePath() {
		return filePath;
	}
	
	public void setFilePath(String path) {
		this.filePath = new FilePath(new File(path));
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
}
