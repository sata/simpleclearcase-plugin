package jenkins.plugins.simpleclearcase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class SimpleClearCaseChangeLogParser {
	
	public static boolean WriteChangeLog(File file, SimpleClearCaseChangeLogSet set) throws IOException {
		PrintWriter pw = new PrintWriter(new FileWriter(file));
		
		return false;
	
	}
	
//	public static SimpleClearCaseChangeLogSet ReadChangeLog(File file) {
//		return null;
//	}
//	
//	public static SimpleClearCaseChangeLogSet ParseChangeLog() {
//		return null;
//	}
}
