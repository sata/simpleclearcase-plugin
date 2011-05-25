package jenkins.plugins.simpleclearcase;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.SAXException;

import com.thoughtworks.xstream.XStream;

public class SimpleClearCaseChangeLogParser extends ChangeLogParser {
	private static final String CHANGELOG   	  = "changelog";
	private static final String CHANGELOG_ENTRY   = "entry";
	private static final String CHANGELOG_ENTRIES = "entries";
	private static final String FILE_ELEMENT	  = "file";
	
	private static final String IGNORE_FIELD_BUILD  = "build";
	private static final String IGNORE_FIELD_UNIX   = "isUnix";
	private static final String IGNORE_FIELD_PARENT = "parent"; 
	
	private static XStream initXStream() {
		XStream xs = new XStream();
		xs.setMode(XStream.NO_REFERENCES);
		xs.alias(CHANGELOG, SimpleClearCaseChangeLogSet.class);
		xs.alias(CHANGELOG_ENTRY, SimpleClearCaseChangeLogEntry.class);
		xs.alias(FILE_ELEMENT, FileElement.class);
		xs.addImplicitCollection(SimpleClearCaseChangeLogSet.class, CHANGELOG_ENTRIES,
								 				SimpleClearCaseChangeLogEntry.class);
		xs.omitField(SimpleClearCaseChangeLogSet.class, IGNORE_FIELD_BUILD);
		xs.omitField(SimpleClearCaseChangeLogEntry.class, IGNORE_FIELD_UNIX);
		xs.omitField(SimpleClearCaseChangeLogEntry.class, IGNORE_FIELD_PARENT);
		
		return xs;
	}
	
	public static boolean writeChangeLog(File file, SimpleClearCaseChangeLogSet set) throws IOException {
		FileWriter fw = new FileWriter(file);
		
		XStream xs = initXStream();
		xs.toXML(set, fw);
		fw.close();
		
		return true;
	}
	
	public static SimpleClearCaseChangeLogSet readChangeLog(File file) throws FileNotFoundException {
		FileReader fr = new FileReader(file);
		XStream xs = initXStream();
		
		return (SimpleClearCaseChangeLogSet) xs.fromXML(fr);
	}
	
	@Override
	public ChangeLogSet<? extends Entry> parse(AbstractBuild build,
			File changelogFile) throws IOException, SAXException {
		
		/* as we are using XStream as serializer and deserializer
		 * we cannot directly call to constructor of SimpleClearCaseChangeLog
		 * As it takes the build as a parameter, hence we need to pull
		 * the entries of the XStream created set and send it with the build
		 * to the constructor. 
		 * */
		SimpleClearCaseChangeLogSet set = readChangeLog(changelogFile);
		
		List<SimpleClearCaseChangeLogEntry> entries = new ArrayList<SimpleClearCaseChangeLogEntry>();
		
		for (SimpleClearCaseChangeLogEntry entry : set) {
			entries.add(entry);
		}
		
		return new SimpleClearCaseChangeLogSet(build, entries);
	}
}
