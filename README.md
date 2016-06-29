## What Does It Do?
WebsiteToMongo is a configurable web crawler that downloads websites locally and then indexes the results in a mongo db dataase

##How To Configure 
Configuration is relatively easy and mostly requires inputting values

## Config Explained
###~Defaults~
#####followExternalLinks:(true/false) 
Whether to follow external links 
#####respectRobots:(true/false) 
Whether to respect robots.txt, but currently not implemented

###~Basic Config~
#####homepageURL: http://www.google.com/ 
The URL or local file of where the webcrawler should start
#####websiteURL: http://www.google.com/ 
URL of website, which is only required for downloaded local websites
#####workingDir: /Users/sampleUser/Documents/testWebsites/
Source Folder
#####subdir: google 
Where to save in source folder
#####saveType: (fullSite/content)
fullSite downloads the entire site for offline viewing, while content just downloads certain content

###~Content Config~
Only Required For saveType:content
#####contentType:video 
The selector for content type to download
#####contentLoc:src 
The tag to get the content source eg <video src="video.mp4">

###~Database Config~
#####database: wikiForSchoolsTest 
Mongo Database Name
#####collection: activities 
Mongo Collection Name

###~Follow Rules~
Rules about what links to follow and files to download
#####downloadFiles:jpg, png, gif, jpeg, tif, css, js 
File types to download
#####linksToFollow:html, htm, php, asp 
Href links to follow
#####disallow:.png.htm 
Example Exclude Paths
#####disallow:/images/ 
Example Exclude Path

###~Index Rules~ 
Rules about what files to Index 
#####includes: video 
Index only if has selector type eg #id .class or element eg button
#####!include: .audio 
Don't index if has selector
#####url-pattern: relevantFiles/ 
Index only if has url pattern
#####!url-pattern uselessFiles/ 
Don't index if has urlPath

##~To Index~
How Indexed Results Are loaded into the database
####### field:value
###Fields
No Rules About Names
###Value
####Defaults
Always Starts with $ and following default value
##### $fileType
The filetype eg html 
##### $size 
The size of a file in MB
##### $fileName
The name of the file eg index.html
##### $filePath
Full filepath from working directory google/index.html
####Literals
Denotes by text inside of ' or " it is always read as such 
####Commands
Grab Certain Elements inside of doc. Commands are pretty limited as of now, but you can grab any selector and any of its children.
#####selector
Grabs the text of a selector
#####selector+child(0)
Grabs the text of the first child of a selector
