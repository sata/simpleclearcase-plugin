package jenkins.plugins.simpleclearcase.util;

/**
 * @author etavsam
 * 
 */
public class OsUtil {
	public static final String WIN_NEWLINE  	   = "\r\n";

	public static String getMailFromAuthor(String author, boolean isUnix) {
		
		if (isUnix == false) //currently no support for !unix systems
			return null;
		
		//must retreive mail from shell, how?
		return null;
	}
}
