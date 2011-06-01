package jenkins.plugins.simpleclearcase;

import hudson.Util;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jenkins.plugins.simpleclearcase.util.DateUtil;
import jenkins.plugins.simpleclearcase.util.DebugHelper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

public class SimpleClearCaseChangeLogParser extends ChangeLogParser {
	private static final String XML_VERSION 			 = "1.0";
	private static final String XML_ENCODING			 = "UTF-8";
	private static final String XML_INDENT  			 = "yes";
	private static final String XML_INDENT_SPACE		 = "2";
	private static final String XML_INDENT_SPACE_SETTING = "{http://xml.apache.org/xslt}indent-amount";
	
	private static final String CHANGELOG = "changelog";
	private static final String VERSION   = "version";
	private static final String DATE	  = "date";
	private static final String ENTRY     = "entry";
	private static final String USER	  = "user";
	private static final String COMMENT   = "comment";
	private static final String ITEMS     = "items";
	private static final String ITEM	  = "item";
	private static final String OPERATION = "operation";
	
	public static boolean writeChangeLog(File file, SimpleClearCaseChangeLogSet set, TaskListener listener) throws IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document doc;
		
		try {
			doc = factory.newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException ex) {
			DebugHelper.error(listener, "Couldn't create a new DocumentBuilder, ExceptionMsg: e: %s", ex.getMessage());
			return false;
		}
		
		doc.setXmlVersion(XML_VERSION);
		
		Element changelog = doc.createElement(CHANGELOG);

		for (SimpleClearCaseChangeLogEntry e : set.getEntries()) {
			Element entry = doc.createElement(ENTRY);
		
			entry.setAttribute(VERSION, e.getVersion());
			entry.setAttribute(OPERATION, e.getOperation());

			Element date = doc.createElement(DATE);
			date.setTextContent(DateUtil.formatDate(e.getDate()));
						
			Element user = doc.createElement(USER);
			user.setTextContent(e.getUser());
		
			Element comment = doc.createElement(COMMENT);
			comment.setTextContent(e.getComment());
		

			Element items = doc.createElement(ITEMS);
			// FIXME when changing Entry such that file elements contain versions then this must be also changed		
			for (String filePath : e.getAffectedPaths()) {
				Element item = doc.createElement(ITEM);
				item.setTextContent(filePath);
				items.appendChild(item);
			}
			
			entry.appendChild(date);
			entry.appendChild(user);
			entry.appendChild(comment);
			entry.appendChild(items);
			
			changelog.appendChild(entry);
		}
		
		doc.appendChild(changelog);
	
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t;
		try {
			t = tf.newTransformer();
		
			t.setOutputProperty(OutputKeys.ENCODING, XML_ENCODING);
			t.setOutputProperty(OutputKeys.INDENT, XML_INDENT);
			t.setOutputProperty(XML_INDENT_SPACE_SETTING, XML_INDENT_SPACE);
		
			DOMSource src = new DOMSource(doc);
		
			StreamResult res = new StreamResult(new PrintWriter(new FileWriter(file)));

			t.transform(src, res);
		} catch (TransformerException ex) {
			DebugHelper.error(listener, "Couldn't create transform and write to resultStream, ExceptionMsg: e: %s", ex.getMessage());
			return false;
		}
		
		return true;
	}
	
	public static List<SimpleClearCaseChangeLogEntry> readChangeLog(File file) throws IOException, ParserConfigurationException, SAXException {
		List<SimpleClearCaseChangeLogEntry> ret = new ArrayList<SimpleClearCaseChangeLogEntry>();
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(file);
		
		//proper change log should only contain a CHANGE LOG element
		Element changelog = (Element) doc.getElementsByTagName(CHANGELOG).item(0);
		
		//fetch all the entries from changelog
		NodeList entries = changelog.getElementsByTagName(ENTRY);
		
		//as NodeList doesn't implement iterable...
		for (int i = 0; i < entries.getLength(); i++) {
			Element elemEntry = (Element) entries.item(i);
			
			String date 	 = elemEntry.getElementsByTagName(DATE).item(0).getTextContent();
			String user 	 = elemEntry.getElementsByTagName(USER).item(0).getTextContent();
			String comment   = elemEntry.getElementsByTagName(COMMENT).item(0).getTextContent();
			String operation = elemEntry.getAttribute(OPERATION);
			String version   = elemEntry.getAttribute(VERSION);
			
			//we create the entry without any file path reference
			SimpleClearCaseChangeLogEntry entry = new SimpleClearCaseChangeLogEntry(DateUtil.parseDate(date), 
																		user, version, comment, operation);
			//adding all available file paths to entry
			addFilePathsToEntry(elemEntry.getElementsByTagName(ITEMS), entry);
			
			ret.add(entry);
		}
		return ret;
	}
	
	private static void addFilePathsToEntry(NodeList items, SimpleClearCaseChangeLogEntry entry) {
		for (int i = 0; i < items.getLength(); i++) {
			Element elemItem = (Element) items.item(i);
			entry.addPath(elemItem.getTextContent());
		}
	}
	
	@Override
	public ChangeLogSet<? extends Entry> parse(AbstractBuild build,
			File changelogFile) throws IOException, SAXException {

		List<SimpleClearCaseChangeLogEntry> entries;
		try {
			entries = readChangeLog(changelogFile);
		} catch (ParserConfigurationException e) {
			return null;
		}
		
		return new SimpleClearCaseChangeLogSet(build, entries);
	}
}
