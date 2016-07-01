package main;

/** Class: WebDownloader
 *  Description: Handles loading and downloading webpages/files and following links
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WebDownloader {
	private Set<String> linksFollowed;
	private Set<String> downloadedFiles;
	private String homeURL;
	private static String staticHomeURL;
	private String workingDir;
	private String contentType;
	private MongoConnect connect;
	private Queue linksToFollow;

	/** main */
	public static void main(String[] args) {
		try {
			WebDownloader downloader = new WebDownloader();
			//Load Rules
			Rules.init();

			//Run
			downloader.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** run */
	public void run() {
		//Init Sets and Queue
		linksFollowed = new HashSet<String>();
		downloadedFiles = new HashSet<String>();
		linksToFollow = new Queue(1024);  

		//Get Working Direction and Home URL
		homeURL = Rules.getHomeURL();
		workingDir = Rules.getWorkingDir() + Rules.getSubdir();

		//If WorkingDir ends with a slash remove it
		if ((workingDir.length() > 0) && (workingDir.charAt(workingDir.length()-1) == '/')) {
			workingDir = workingDir.substring(0, workingDir.length()-1);
		}

		//Init Mongo Database
		connect = new MongoConnect();

		//Add Existing Files To Already Followed
		if (!Rules.redownloadExistingFiles() && !Rules.isLocalFile()) {
			redownloadExistingFiles();
		}

		if (Rules.getSaveType().equalsIgnoreCase("fullSite")) {
			//Enqueue the homepage and loop till finished
			linksToFollow.enqueue(homeURL);

			//For local files change homeURL to exclude end
			homeURL = homeURL.substring(0, homeURL.lastIndexOf('/')+1);

			while (!linksToFollow.isEmpty()) {
				downloadWebpages(linksToFollow.dequeue());
			}
		} else if (Rules.getSaveType().equalsIgnoreCase("content")) {
			//Enqueue the homepage and loop till finished
			linksToFollow.enqueue(homeURL);
			while (!linksToFollow.isEmpty()) {
				downloadContentOnly(linksToFollow.dequeue());
			}
		}

		//Close Connection To Database
		connect.onExit();
	}

	/** downloadLinks
	 *  Downloads page and follows all relevant links
	 * @param homepageURL
	 */
	public void downloadWebpages(String homepageURL) {
		//Null Url
		if (homepageURL == null) {
			return;
		}

		//Already Visited Page
		if (linksFollowed.contains(homepageURL)) {
			return;
		} 

		if (Rules.isLocalFile() && homepageURL.contains("http")) {
			homepageURL = getFileLocation(homepageURL);
		}

		//Download Homepage
		Document homepage = downloadWebpage(homepageURL, 0);

		//If The Page Returned is Null Return
		if (homepage == null) {
			return;
		}

		//Download Root
		int length = saveWebpage(homepage, homepageURL);

		//Follow All Linkes 
		followLinks(homepage, "a", "href");

		//Download All Images
		downloadFiles(homepage, "img", "src");

		//Download Style Sheets
		downloadFiles(homepage, "link", "href");

		//Download Javascript
		downloadFiles(homepage, "script", "src");

		//Index If Meets Rules
		if (Rules.shouldIndex(homepage, homepageURL)) {
			connect.insert(Rules.buildBSON(homepageURL, withoutHomeURL(homepageURL), length, homepage));
		}
	}

	/** downloadContentOnly
	 *  Downloads page and follows all relevant links
	 * @param homepageURL
	 */
	public void downloadContentOnly(String homepageURL) {
		//Null Url
		if (homepageURL == null) {
			return;
		}

		//Already Visited Page
		if (linksFollowed.contains(homepageURL)) {
			return;
		} 

		//Download Homepage
		Document homepage = downloadWebpage(homepageURL, 0);

		//If The Page Returned is Null Return
		if (homepage == null) {
			return;
		}

		//Follow All Linkes 
		followLinks(homepage, "a", "href");

		//Get Info On Content
		Element content = homepage.select(Rules.getContentType()).first();
		String contentSrc = content.absUrl(Rules.getContentLoc());

		//Download and Index If Meets Rules
		if (Rules.shouldIndex(homepage, contentSrc)) {
			int length = downloadFile(content, Rules.getContentLoc());
			connect.insert(Rules.buildBSON(contentSrc, withoutHomeURL(contentSrc), length, homepage));
		}
	}

	/** followLinks 
	 * Gets all elements with a given tag and follows linkType
	 */
	public void followLinks(Document page, String tag, String linkType) {
		//Get All Elements
		Elements links = page.select(tag);

		//Loops Through All Elements
		for (int i = 0; i < links.size(); i+=1) {
			Element currElem = links.get(i);

			//Get absolute URL Path and Get Filetype
			String src = currElem.absUrl(linkType);
			src = cleanAbsoluteUrl(src);
			String fileType = src.substring(src.lastIndexOf('.')+1, src.length());

			//If you haven't followed link yet and should follow the link type
			if (Rules.shouldFollow(src, fileType) && !linksFollowed.contains(src) && src.length() > 0) {
				linksToFollow.enqueue(src);
			}

			if (src.contains("1342-h.htm")) {
				System.out.println("hello");
			}
			//Save Map Of Links To Name
			Rules.putURLByName(withoutHomeURL(src), currElem.text());
		}
	}

	/** cleanAbsoluteUrl
	 * Removes query and php text from url
	 * @param url
	 * @return
	 */
	private String cleanAbsoluteUrl(String url) {
		int queryIndex = url.indexOf('?');
		int phpIndex = url.indexOf('#');

		if (queryIndex != -1 && phpIndex != -1) {
			return url.substring(0, Math.min(queryIndex, phpIndex));
		} else if (queryIndex != -1) {
			return url.substring(0, queryIndex);
		} else if (phpIndex != -1) {
			return url.substring(0, phpIndex);
		}  else {
			return url;
		}
	}

	/** downloadFiles
	 * Download all of element types on a certain page
	 * @param page
	 * @param tag
	 * @param linkType
	 */
	public void downloadFiles(Document page, String tag, String linkSrc) {
		//Get all elements with given tag
		Elements elements = page.select(tag);
		for (int i = 0; i < elements.size(); i+=1) {
			downloadFile(elements.get(i), linkSrc);
		}
	}

	/** downloadFile
	 * Downloads a file with a given link src and element
	 * @param elem
	 * @param linkSrc
	 * @return length
	 */
	public int downloadFile(Element elem, String linkSrc) {
		//Get Source to download and filetype
		String src = elem.absUrl(linkSrc);
		String fileType = src.substring(src.lastIndexOf('.')+1, src.length());
		int length = 0;
		//If you haven't downloaded and should follow link
		if (Rules.shouldFollow(src) && Rules.shouldDownload(fileType) && !downloadedFiles.contains(src)) {
			length = saveFile(src, 0);
			downloadedFiles.add(src);
		}
		return length;
	}

	/** downloadWebpage
	 * Connects to a webpage
	 * @param url
	 * @param level
	 * @return
	 */
	public Document downloadWebpage(String url, int depth) {
		//If you haven't followed 
		if (!linksFollowed.contains(url) && url.startsWith(homeURL)) {
			try {
				System.out.println("Connecting To " + url);
				linksFollowed.add(url);
				if (Rules.isLocalFile()) {
					File file = new File(url);
					return Jsoup.parse(file, "UTF-8", Rules.getWebsiteURL());
				} else {
					return Jsoup.connect(url).get();
				}
			} catch(SocketTimeoutException e) {
				//Try up to five times to download before failing
				if (depth > 5) {
					return downloadWebpage(url, depth+1);
				}
				return null;
			} catch(UnknownHostException e) {
				//Check that internet hasn't gone out
				try {
					URL google = new URL("http:www.google.com");
					google.openConnection();
				} catch (UnknownHostException f) {
					System.out.println("Likely That Computer Lost Connection! Press Enter To Continue.");
					Scanner scanner = new Scanner(System.in);
					scanner.nextLine();
					scanner.close();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			} catch(IllegalArgumentException e)  {
				e.printStackTrace();
				return null;
			} catch (FileNotFoundException e) {
				System.out.println("File At " + url + " doesn't exist... Skipping File");
				return null;
			} catch(Exception e) {
				e.printStackTrace();
				return null;
			} 
		}
		return null;
	}

	/** saveFile
	 * Downloads a file at a given url path and saves to disk
	 * @param url
	 * @return
	 */
	public int saveFile(String url, int depth) {
		//Download URL
		ByteBuffer toRead = new ByteBuffer();
		try {
			if (!Rules.isLocalFile()) {
				String urlToRead = homeURL + withoutHomeURL(url);
				toRead.readURL(new URL(urlToRead));
				System.out.println("Downloaded File At " + urlToRead);
			}
		} catch(SocketTimeoutException e) {
			if (depth < 5) {
				saveFile(url, depth+=1);
			}
			return 0;
		} catch(UnknownHostException e) {
			System.out.println("Unknown Host "+ url);
			return 0;
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}

		//Save File
		return saveByteBuffer(toRead, url);

	}

	/** saveFile
	 * Saves an already downloaded document to disk
	 * @param doc
	 * @param url
	 * @return
	 */
	public int saveWebpage(Document doc, String url) {
		//Get As String
		ByteBuffer b = new ByteBuffer(doc.toString());

		//Write
		return saveByteBuffer(b, url);
	}

	/** saveByteBuffer
	 * Save a byte buffer to disk
	 * @param b
	 * @param url
	 * @return
	 */
	public int saveByteBuffer(ByteBuffer b, String url) {
		try {
			if (!Rules.isLocalFile()) {
				//Create File and Write
				String fileToWrite = workingDir + withoutHomeURL(url);

				//Catch if homepage
				int lenDifference = fileToWrite.length()-workingDir.length();
				if (lenDifference==1) {
					fileToWrite += "index.html";
				} else if (lenDifference==0) {
					fileToWrite += "/index.html";
				}

				//Save to format
				File file = new File(fileToWrite);
				b.write(file);
				System.out.println("Wrote File At " + fileToWrite);
				return b.length();
			}
			return b.length();
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	/** redownloadExistingFiles
	 * Auxillary Redownload Exisiting Files
	 */
	public void redownloadExistingFiles() {
		File rootFolder = new File(workingDir);
		File[] listOfFiles = rootFolder.listFiles();
		redownloadExistingFiles(listOfFiles);
	}

	/**
	 * 
	 * @param folder
	 */
	private void redownloadExistingFiles(File[] folder) {
		//If the folder is empty return
		if (folder == null || folder.length == 0) {
			return;
		} 
		//Loop Through all files
		for (int i = 0; i < folder.length; i+=1) {
			File curr = folder[i];
			//If its a file
			if (curr.isFile()) {
				//Get Filepath
				String filepath = curr.getAbsolutePath();
				String subdir = Rules.getSubdir();

				//Build Filepath
				filepath = filepath.substring(filepath.indexOf(subdir) + subdir.length(), filepath.length());
				String visitedURL = homeURL + filepath;

				//Get if webpage or other file
				String fileType = visitedURL.substring(visitedURL.lastIndexOf('.')+1,visitedURL.length());
				if (Rules.shouldDownload(fileType)) {
					downloadedFiles.add(visitedURL);
				} else if (Rules.shouldFollow(visitedURL, fileType)) {
					linksFollowed.add(visitedURL);
				}
			} else if (curr.isDirectory()) {
				redownloadExistingFiles(curr.listFiles());
			}
		}
	}

	public String getFileLocation(String url) {
		String path = withoutHomeURL(url);
		return Rules.getWorkingDir() + Rules.getSubdir() + path;
	}

	/** withoutHomeURL
	 * Gets just the URL path
	 * @param url
	 * @return
	 */
	public String withoutHomeURL(String url) {
		//Without url 
		if (url.contains("http")) {
			int index = url.indexOf(Rules.getWebsiteURL());
			if (index == -1) {
				return url;
			} else if(url.length() == Rules.getWebsiteURL().length()) {
				return "index.html";
			} else {
				return url.substring(index + Rules.getWebsiteURL().length(), url.length());
			}
		//Without Base File Directory
		} else {
			int index = url.indexOf(homeURL);
			if (index == -1) {
				return url;
			} else if(url.length() == homeURL.length()) {
				return "index.html";
			} else {
				return url.substring(index + homeURL.length()-1, url.length());
			}
		}		
	}
}

