package jenkins.plugins.simpleclearcase.util;

/**
 * @author etavsam
 * 
 * Currently the support or Windows is lacking as I have not intention on running Jenkins 
 * in a Windows environment, though I took (some( consideration of how to do add windows 
 * support in an fairly easy fashion. 
 *
 */
public class OsUtil {
	public static final String WIN_NEWLINE  	   = "\r\n";
	public static final String UNIX_NEWLINE 	   = "\n";
	public static final String WIN_PATH_SEPARATOR  = "\\";
	public static final String UNIX_PATH_SEPARATOR = "/";
	
	public static String getPathSeparator(boolean isUnix) {
		if (isUnix == true) 
			return UNIX_PATH_SEPARATOR;
		else
			return WIN_PATH_SEPARATOR;
	}
	
//	this method isn't used, as we use String.format with %n to let line.separator handle the win/unix new line charachter
//	public static String getNewline(boolean isUnix) {
//		if (isUnix == true)
//			return UNIX_NEWLINE;
//		else
//			return WIN_NEWLINE;
//	}

	/**
	 * @param path
	 * @param isUnix
	 * @return transformed path depending on the OS being Unix or Windows
	 */
	public static String transformPath(String path, boolean isUnix) {
		if (path == null) 
			return null;
		
		String retVal = path;
	
		if (isUnix == true) {
			//strip all windows newline char
			retVal = retVal.replace(WIN_NEWLINE, UNIX_NEWLINE);
			//convert all path separators with UNIX separator
			retVal = retVal.replace(WIN_PATH_SEPARATOR, UNIX_PATH_SEPARATOR);
		} else {
			retVal = retVal.replace(UNIX_NEWLINE, WIN_NEWLINE);
			retVal = retVal.replace(UNIX_PATH_SEPARATOR, WIN_PATH_SEPARATOR);
		}
		return retVal;
	}
	
	public static String getMailFromAuthor(String author, boolean isUnix) {
		
		if (isUnix == false) //currently no support for !unix systems
			return null;
		
		//must retreive mail from shell, how?
		return null;
	}
		/**
	 * @param filePath
	 * @param isUnix
	 * @return the filename from path with the convention of unix
	 */
	public static String getName(String filePath, boolean isUnix) {
		int index = filePath.lastIndexOf(getPathSeparator(isUnix));
		return filePath.substring(index + 1);
	}

	/**
	 * @param filePath
	 * @param isUnix
	 * @return the path without the filename without trailing slash
	 */
	public static String getPath(String filePath, boolean isUnix) {
		int index = filePath.lastIndexOf(getPathSeparator(isUnix));
		return filePath.substring(0, index);
	}
}
