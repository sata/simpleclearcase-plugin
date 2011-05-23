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
	private boolean isUnix;
	
	public FileElement(String path, String version, boolean isUnix) {
		this.filePath = new FilePath(new File(path));
		this.version  = version;
		this.isUnix   = isUnix;
	}

	public FileElement(String path, boolean isUnix) {
		this(path, INIT_VERSION, isUnix);
	}
	
	
	public FilePath getFilePath() {
		return filePath;
	}

	public String getVersion() {
		return version;
	}
	
	public boolean isUnix() {
		return isUnix;
	}
}
