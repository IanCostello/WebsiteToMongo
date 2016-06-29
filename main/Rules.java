package main;

/** Class: Rules
 *  Description: Loads Rules From Files and handles directing web crawler what to follow 
 *  as well as building documents to put in database
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Rules {
	//Rules For Following and Downloading
	private static Set<String> filesToDownload;
	private static Set<String> linksToFollow;
	private static Set<String> pathsToIndex;
	private static Set<String> pathsToNotIndex;
	private static Set<String> linksToDisclude;
	
	//Rules For Indexing
	private static Set<String> selectorsToInclude;
	private static Set<String> selectorsToDisclude;
	
	//To Index
	private static ArrayList<Index> toIndex;
	
	//Other Rules
	private static boolean followExternalLinks;
	private static boolean respectRobots;
	private static boolean redownloadExistingFiles;
	
	//Config
	private static String homepage;
	private static boolean localFile;
	private static String workingDir;
	private static String subdir;
	private static String database;
	private static String collection;
	private static String saveType;
	private static String contentType;
	private static String contentLoc;
	private static String websiteURL;

	//Break Chars
	private static final ByteBuffer COMMA = new ByteBuffer(",");
	private static final ByteBuffer NEXT = new ByteBuffer("~");
	private static final ByteBuffer COMMAND = new ByteBuffer("+");
	private static final ByteBuffer START_COMMAND = new ByteBuffer("(");
	private static final ByteBuffer END_COMMAND = new ByteBuffer(")");

	/** init
	 * Inits The Rule Set
	 * @throws IOException
	 */
	public static void init() throws IOException {
		//Init All Arrays
		filesToDownload = new HashSet<String>();
		linksToFollow = new HashSet<String>();
		pathsToIndex = new HashSet<String>();
		pathsToNotIndex = new HashSet<String>();
		selectorsToInclude = new HashSet<String>();
		selectorsToDisclude = new HashSet<String>();
		linksToDisclude = new HashSet<String>();
		toIndex = new ArrayList<Index>();

		//Load Properties Files
		String workingDir = System.getProperty("user.dir")+'/';
		File file = new File((workingDir + "indexRules.settings"));
		ByteBuffer buf = new ByteBuffer();
		buf.read(file);	
		buf.moveStartPast(NEXT);

		do {
			//Get Groupname
			ByteBuffer groupName = new ByteBuffer();
			buf.getToken(groupName, NEXT);
			
			//Special Case For Indexing Rules
			boolean indexElem = groupName.equals("To Index");

			//Get Text In Group Element
			ByteBuffer group = new ByteBuffer();
			buf.getToken(group, NEXT);

			//Loop Through Each Element
			while (group.start() != group.end()) {
				//Grab Attribute
				ByteBuffer attr = new ByteBuffer();
				group.getToken(attr, ByteBuffer.COLON_SEPARATOR);
				
				//Grab Value
				ByteBuffer val = new ByteBuffer();
				group.getToken(val, ByteBuffer.CRLF);
				
				//Ensure valid entry
				if (attr.length() != 0 && val.length() != 0) {
					//Special Case For Index
					if (indexElem)
						parseIndex(attr.toString(), val.toString());
					//Otherwise Parse Rule name
					else
						parseRuleName(attr.toString(), val.toString());
				}
			}
		} while (buf.start() != buf.end());
		
		//Notify finished parsing
		System.out.println("Loading Rules");
	}

	/** parseIndex
	 * Add attribute:property pair to list to be parsed later
	 * @param attribute
	 * @param property
	 */
	private static void parseIndex(String attribute, String property) {
		toIndex.add(new Index(attribute, property));
	}

	/** parseRuleName
	 * Parses a rule
	 * @param attribute
	 * @param property
	 */
	private static void parseRuleName(String attribute, String property) {
		switch (attribute) {
			case "followExternalLinks":
				followExternalLinks = property.equals("true");
				break;
			case "respectRobots":
				respectRobots = property.equals("true");
				break;
			case "redownloadExistingFiles":
				redownloadExistingFiles = property.equals("true");
				break;
			case "homepageURL":
				homepage = property;
				localFile = !homepage.contains("http");
				break;
			case "saveType":
				saveType = property;
				break;
			case "websiteURL":
				saveType = property;
				break;
			case "contentType":
				contentType = property;
				break;
			case "contentLoc":
				contentLoc = property;
				break;
			case "workingDir":
				workingDir = property;
				break;
			case "subdir":
				subdir = property;
				break;
			case "downloadFiles":
				addProperties(filesToDownload, property);
				break;
			case "linksToFollow":
				addProperties(linksToFollow, property);
				break;
			case "disallow":
				linksToDisclude.add(property);
				break;
			case "includes":
				selectorsToInclude.add(property);
				break;
			case "!includes":
				selectorsToDisclude.add(property);
				break;
			case "url-pattern":
				pathsToIndex.add(property);
				break;
			case "!url-pattern":
				pathsToNotIndex.add(property);
				break;
			case "database":
				database = property;
				break;
			case "collection":
				collection = property;
				break;
		default:
			System.out.println("Unknown Attribute " + attribute + " with property " + property);
		}
	}

	/** addProperties
	 * Parses a list of properties and puts in set
	 * @param set
	 * @param property
	 */
	private static void addProperties(Set<String> set, String property) {
		ByteBuffer buf = new ByteBuffer(property);
		ByteBuffer token = new ByteBuffer();
		ByteBuffer last = new ByteBuffer();
		do {
			//Update Last
			last = new ByteBuffer(token);
			//Get Next and Add
			buf.getToken(token, COMMA);
			set.add(token.toString());
			
		//Break if cycled without changing
		} while (!last.equals(token));
	}

	/** shouldDownload
	 * If the filetype is on the download list return true
	 * @param fileType
	 * @return
	 */
	public static boolean shouldDownload(String fileType) {
		String lowerCase = fileType.toLowerCase();
		return filesToDownload.contains(lowerCase);
	}

	/** shouldFollow
	 * If the absURl is not on the ignore path follow and fileType is on follow list
	 */
	public static boolean shouldFollow(String absURL, String fileType) {
		String lowerCase = fileType.toLowerCase();
		//If Should Follow External Links 
		if (followExternalLinks) {
			return ((setNotContainsAbsURL(linksToDisclude, absURL)) && linksToFollow.contains(lowerCase));
		//Otherwise ensure it is on local site 
		} else {
			return ((setNotContainsAbsURL(linksToDisclude, absURL)
					&& absURL.contains(homepage) && linksToFollow.contains(lowerCase)));
		}
	}
	
	/** shouldFollow
	 * If the absURl is not on the ignore path follow
	 */
	public static boolean shouldFollow(String absURL) {
		//If Should Follow External Links 
		if (followExternalLinks) {
			return (setNotContainsAbsURL(linksToDisclude, absURL));
		//Otherwise ensure it is on local site 
		} else {
			return ((setNotContainsAbsURL(linksToDisclude, absURL)
					&& absURL.contains(homepage)));
		}
	}

	/** shouldIndex
	 * Returns whether a webpage should be indexed in the database
	 * @param homepage
	 * @param absURL
	 * @return
	 */
	public static boolean shouldIndex(Document homepage, String absURL) {
		//Return true if doc has include selectors and doesn't have exclude selectors
		//and isn't on list of paths to not index
		return ((docHasSelector(selectorsToInclude, homepage)) &&
				(docNotHasSelector(selectorsToDisclude, homepage)) &&
				(setNotContainsAbsURL(pathsToNotIndex, absURL)));
	}
	
	/** setContainsAbsURL
	 * Returns if an absolute url path contains a path in a set
	 * @param set
	 * @param absURL
	 * @return
	 */
	private static boolean setContainsAbsURL(Set<String> set, String absURL) {
		Iterator<String> selectors = set.iterator();
		while (selectors.hasNext()) {
			String path = selectors.next();
			if (absURL.contains(path)) {
				return true;
			}
		}
		return false;
	}
	
	/** setNotContainsAbsURL
	 * Returns if an absolute url path contains a path in a set
	 * @param set
	 * @param absURL
	 * @return
	 */
	private static boolean setNotContainsAbsURL(Set<String> set, String absURL) {
		Iterator<String> selectors = set.iterator();
		while (selectors.hasNext()) {
			String path = selectors.next();
			if (absURL.contains(path)) {
				return false;
			}
		}
		return true;
	}

	/** buildBson
	 * Builds a database entry from a webpage
	 * @param src
	 * @param savLoc
	 * @param length
	 * @param page
	 * @return
	 */
	public static org.bson.Document buildBSON(String src, String savLoc, int length, Document page) {
		//Build $Defaults 
		String filePath = subdir + savLoc;
		int lastIndex = src.lastIndexOf('/')+1;
		String fileName = src.substring(lastIndex, src.length());
		double fileSize = (double)length/1000;
		String fileType = src.substring(src.lastIndexOf('.')+1, src.length());

		//Create blank document and loop through indexing rules
		org.bson.Document doc = new org.bson.Document();
		for (int i = 0; i < toIndex.size(); i+=1) {
			Index index = toIndex.get(i);

			//Parse Value
			String val = index.getVal();
			String valToInput = val;

			//Default Case add relevant information
			if (val.startsWith("$")) {
				switch (val) {
				case "$fileType":
				valToInput = fileType;
					break;
				case "$size":
					valToInput = Double.toString(fileSize);
					break;
				case "$fileName":
					valToInput = fileName;
					break;
				case "$filePath":
					valToInput = filePath;
					break;
				}
			//Otherwise if text is a literal statement remove comma's
			} else if (val.startsWith("'")|val.startsWith("\"")) {
				valToInput = val.substring(1, valToInput.length()-1);
				
			//Otherwise is a code statement and parse
			} else {
				valToInput = parseCode(val, page);
				System.out.println();
			}
			
			//Append to BSON document
			doc.append(index.getAttr(), valToInput);
		}
		
		return doc;
	}

	/** parseCode
	 * Auxillary Method For Recurvive parseCode
	 * @param val
	 * @param page
	 * @return
	 */
	private static String parseCode(String val, Document page) {
		ByteBuffer buf = new ByteBuffer(val);
		return parseCode(buf, page.getAllElements()).ownText();
	}
	
	/** parseCode
	 * Parses Code Into 
	 * @param buf
	 * @param elements
	 * @return
	 */
	private static Element parseCode(ByteBuffer buf, Elements elements) {
		int size = elements.size();
		
		//If Only One Child Remains or no commands remain return first element
		if (size == 1 && elements.first().children().size() == 0 || buf.length() == 0) {
			Element curr = elements.get(0);
			Elements children = elements.get(0).children();
			while (children.size() > 0) {
				curr = children.first();
				children = curr.children();
			}
			return curr;
			
		//If no elements return null
		} else if (size == 0) {
			return null;
			
		//Otherwise return
		} else {
			//Get Command
			ByteBuffer command = new ByteBuffer();
			buf.getToken(command, COMMAND);
			
			//If it is a command
			if (command.indexOf('(') != -1) {
				//Get command text
				ByteBuffer commandText = new ByteBuffer();
				command.getToken(commandText, START_COMMAND);
				
				//Get argument
				ByteBuffer commandProperties = new ByteBuffer();
				command.getToken(commandProperties, END_COMMAND);
				
				//Handled Commands
				if (commandText.equals("child")) {
					return parseCode(buf, elements.get(commandProperties.intValue()).children());
				} else {
					System.out.println("Unknown Command " + commandText + " return first element.");
					return elements.get(0);
				}
				
			//Otherwise it is a selector
			} else {
				Elements nextElems = elements.select(command.toString());
				//If there is more commands
				return parseCode(buf, nextElems);
			}
		}
	}

	/** docHasSelector
	 * Return if a jsoup document has a selector
	 * @param set
	 * @param homepage
	 * @return
	 */
	private static boolean docHasSelector(Set<String> set, Document homepage) {
		Iterator<String> selectors = set.iterator();
		while (selectors.hasNext()) {
			String selector = selectors.next();
			if (homepage.select(selector).size() == 0) {
				return false;
			}
		}
		return true;
	}

	/** docNotHasSelector
	 * Returns if a jsoup document doesn't have selector
	 * @param set
	 * @param homepage
	 * @return
	 */
	private static boolean docNotHasSelector(Set<String> set, Document homepage) {
		Iterator<String> selectors = set.iterator();
		while (selectors.hasNext()) {
			String selector = selectors.next();
			if (homepage.select(selector).size() > 0) {
				return false;
			}
		}
		return true;
	}

	/** withoutHomeURL
	 * Gets just the URL path after home url eg www.google.com/w/hello.html returns w/hello.html
	 * @param url
	 * @return
	 */
	public static String withoutHomeURL(String url) {
		int index = url.indexOf(homepage);
		if (index == -1) {
			return url;
		} else {
			return url.substring(index + homepage.length(), url.length());
		}
	}

	/** Getters */
	public static String getHomeURL() {
		return homepage;
	}

	public static String getWorkingDir() {
		return workingDir;
	}
	
	public static String getSaveType() {
		return saveType;
	}
	
	public static String getContentType() {
		return contentType;
	}
	
	public static String getContentLoc() {
		return contentLoc;
	}
	
	public static String getSubdir() {
		return subdir;
	}

	public static boolean isLocalFile() {
		return localFile;
	}
	
	public static String getWebsiteURL() {
		return websiteURL;
	}
	
	public static boolean respectRobots() {
		return respectRobots;
	}
	
	public static boolean redownloadExistingFiles() {
		return redownloadExistingFiles;
	}
	
	public static String getDatabase() {
		return database;
	}
	public static String getCollection() {
		return collection;
	}

	/** Index */
	private static class Index {
		private String attr;
		private String val;

		public Index(String attr, String val) {
			this.attr = attr;
			this.val = val;
		}

		public String getAttr() {
			return attr;
		}

		public String getVal() {
			return val;
		}
	}
}
