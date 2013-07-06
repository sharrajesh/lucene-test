# Create a java-downloads folder
* I created under E:\depot\work\java-downloads
* I have copied all of its contents to \\devshare\devshare\rsharma\java-downloads

# Download lucene
* To E:\depot\work\java-downloads\lucene-4.3.1-svn either
  * Directly from https://svn.apache.org/repos/asf/lucene/dev/tags/lucene_solr_4_3_1/lucene
  * Or just use my java-downloads folder

# Download and install 64 bit version jdk 7
* Download jdk-7u25-windows-x64.exe either
  * From http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html
  * Or just use my java-downloads folder
* set JAVA_HOME=C:\Program Files\Java\jdk1.7.0_25
* see if we are running 64 bit version of java
  * run cmd.exe
  * java -version
  * where java.exe -- to where is it running from

# Installing ant
* Download ant first either 
  * From http://ant.apache.org/bindownload.cgi
  * Or use my java-downloads folder
* Unzip to C:\apache-ant-1.9.1
* Set user environment variable ANT_HOME to C:\apache-ant-1.9.1
* Change your user environment variable PATH to include %ANT_HOME%\bin
* See if ant is properly installed
  * run ant -version 
  * if it complains of tools.jar then it means you have old version of java
  
# Building lucene  
* You might have first have to do
  * ant ivy-bootstrap
* To build core
  * cd E:\depot\work\java-downloads\lucene-4.3.1-svn
  * See E:\depot\work\java-downloads\lucene-4.3.1-svn\build.txt
  * ant
* To build queryparser  
  * cd E:\depot\work\java-downloads\lucene-4.3.1-svn\queryparser
  * ant
* To analyzers
  * cd E:\depot\work\java-downloads\lucene-4.3.1-svn\analysis
  * ant
* Stuff will get built at E:\depot\work\java-downloads\lucene-4.3.1-svn\build
  * E:\depot\work\java-downloads\lucene-4.3.1-svn\build\core
  * E:\depot\work\java-downloads\lucene-4.3.1-svn\build\analysis\common
  * E:\depot\work\java-downloads\lucene-4.3.1-svn\build\queryparser

# Download lucene test IndexTest40 dependencies either
  * By yourself reading from E:\depot\cnorris\LuceneTest\IndexTest40\build.xml
  * Or just use my java-downloads folder

# Installing ivy
* To install ant you need ivy
* you can also install it from Lucene directory
  * cd E:\depot\work\java-downloads\lucene-4.3.1-svn
  * ant ivy-bootstrap
  
#  Installing 64 bit version of eclipse
* Download and install x64 eclipse from http://www.eclipse.org/downloads/
* Run eclipse
* Menu->Windows->Preferences->Java->Installed JREs->Add JRE->"C:\Program Files\Java\jdk1.7.0_25"

# Import IndexTest40 that tests lucene
* Run eclipse
* Menu->File->New Project->Java Project from Existing Ant Buildfile
* Select E:\depot\cnorris\LuceneTest\IndexTest40\build.xml
* Right Mouse Click (RMC) Project IndexTest40->Properties->Java Build Path->Libraries
  * Make sure every thing looks OK
  * You might have to update JAVA_DOWNLOADS variable by "Add Variable" and "Extending" it
  
# Create eclipse "Launch Configuration" 
* RMC IndexTest40 Project->Properties->Run/Debug Settings->New->Java Application->
* Main tab
  * Project->IndexTest40
  * Main class->Search->IndexMain --- where holds static main
* Argument tab
  * Program Argument->Put either index, query, metrics
  * Working directory->Other->E:\depot\cnorris\LuceneTest\IndexTest40
    * So that config.xml, logback.xml can be found
* Name your "Launch Configuration" IndexTest40 or whatever

# Debug IndexTest40 from Eclipse
* RMC Project->Debug As->Debug Configurations->Search for->IndexTest40 that you created above

# Creating Runnable jar file
* RMC Project->Export->Java->Runnable JAR file
  * Launch Configuration->IndexTest40
  * Export destination->E:\depot\cnorris\LuceneTest\IndexTest40\IndexTest40.jar
  * Library handling->Package required libraries into generated JAR
  
# Configuring IndexTest40.jar with config.xml
* Configure input location
  * By using wiki line file
    * Note that if a line file is found then "indexing all files inside a folder" will not happend
    * For 1 GB files
      * change "input_file" to C:\indexdata\input\1gb\small-aa
    * For 200 MB files
      * change "input_file" to C:\indexdata\input\200mb\small-aa
    * For original 30 GB file
      * change "input_file" to C:\indexdata\input\orig\enwiki.txt
  * By using indexing all files inside a folder
    * Make sure "input_file" is to something non existant
    * For each file is 512 KB 
      * Change "input_dir" to C:\indexdata\input\512kb
* Configure output location
  * It is better to create output on a separate drive
  * Change "output_dir" to c:\indexdata\output
  
# Running it outside of eclipse
## Goto your jar location
* cd E:\depot\cnorris\LuceneTest\IndexTest40 -- or where ever you exported your runnable jar file
* make sure there is config.xml and logback.xml in the local folder
* java -version -- to make sure you have 64 bit java
* use index.bat or java -Xmx8g -Xms8g -server -jar IndexTest40.jar %*
* Note that index, query and metric arguments will not work under config.xml is set correctly
## To Index
* make sure config.xml is configure 
  * for location of input_file or input_dir
  * for location of output_dir
* index.bat index
## To query
* make sure config.xml is properly configured
* index.bat query querystring
## To find metrics
* make sure config.xml is properly configured
* index.bat metrics

# Extract wiki line text by either
* Using the data parsed out by me from c:\indexdata\input
* Or do it yourself
  * Some documentation at C:\ad\work\java-downloads\lucene-4.3.1-svn\benchmark\README.enwiki
  * First goto http://dumps.wikimedia.org/enwiki/20120601/
  * Download 8 GB or so file enwiki-20120601-pages-articles-multistream.xml.bz2 to c:\indexdata\input\orig
    * unzip to get .xml file
  * cd C:\ad\work\java-downloads\lucene-4.3.1-svn\benchmark
  * make folder ./temp and ./work
  * move the ~30GB .xml file and copy to ./temp
  * change conf/extractWikipedia.alg
    * Where to get documents from:
      * docs.file=C:/indexdata/input/orig/enwiki-20120601-pages-articles-multistream.xml
    * Where to write the line file output:
      * line.file.out=line.file.out=C:/indexdata/input/orig/enwiki.txt
  * ant run-task -Dtask.alg=conf/extractWikipedia.alg
  * move it back to c:/indexdata/input/orig/enwiki.txt
  * also move back your orignal xml file back to c:/indexdata/input/orig/

# To work with very large files
## To see a few lines of a very large file
* run cygwin
* head -5 large.txt
* tail -5 large.txt
## number of lines in text
* wc -l large.txt
## to split a large file into 200 mega byte size chunks
* split -b 200m orig/enwiki.txt small- -a 20 
  * each file will start will prefix "small-" 
  * "-a 20" default is 2; it is 20 so that we dont run out of them
* split -b 1024m orig/enwiki.txt small- -a 20

# Some eclipse functionality
## open existing project 
* Menu->File->Import->General->Existing Projects->Lucenettest Directory->IndexTest40
## export an existing eclipse project to ant build.xml 
* RMC project->Export->General->Ant files
## create a java project from build.xml 
* Menu->File->New->Project->Java Project->From Existing Ant Build File
