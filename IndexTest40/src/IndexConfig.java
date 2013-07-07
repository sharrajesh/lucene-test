import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;

public class IndexConfig {
  private static Logger Log = LoggerFactory.getLogger(IndexConfig.class);
  
  public static final Version LUCENE_VERSION = Version.LUCENE_43;
  
  public static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();
  public static final long TOTAL_MEMORY = Runtime.getRuntime().totalMemory();
  public static final long MAX_MEMORY = Runtime.getRuntime().maxMemory();
  public static final long FREE_MEMORY = Runtime.getRuntime().freeMemory();
  
  public static final long KILO = 1024;
  public static final long MEGA = KILO * KILO;
  public static final long GIGA = MEGA * KILO;
  public static final double GIGA_PER_HOUR_FACTOR = (3600.0 * 1000.0) / GIGA;
  
  public final static char SEP = '\t';
  
  public static final String[] EXTENSIONS_TO_INDEX = null; // = {"cpp", "h",
                                                           // "xml", "c", "txt",
                                                           // "html"};
  
  public enum IndexingScheme {
    Unknown, Base, Multi, Indep
  }
  
  public static class Parsed {
    public static String INPUT_FILES_DIR = null;
    private static String WIKI_LINE_FILE_NAME = null;
    
    public static String OUTPUT_DIR;
    public static String OUTPUT_DIR_FINAL;
    public static String OUTPUT_DIR_TEMP;
    
    private static String INDEXING_SCHEME;
    public static IndexConfig.IndexingScheme Scheme;
    
    public static long RAM_BUFFER_SIZE_MB;
    public static boolean NO_MERGE_POLICY;
    public static boolean USE_RAM_DIR = true;
    public static long INDEX_CLOSE_THRESHHOLD = 0;// 150*1024*1024;
  }
  
  public static long STATUS_UPDATE_THRESHHOLD_DOCS = 0;// 2000;
  
  public static long INPUT_DATA_SIZE = 0;
  public static long TOTAL_DOCS_TO_INDEX = 0;
  
  public static Collection<File> FILES_TO_INDEX;
  public static File WIKI_LINE_FILE;
  
  public static boolean FileExists(String filePath) {
    return new File(filePath).exists();
  }
  
  public static void SetupForIndex() throws Exception {
    LoadConfigXml();
    
    CleanOutputDirs();
    
    FindFilesToIndex();
    SetThreshholds();
    PrintConfig();
  }
  
  public static void SetupForSearch() throws Exception {
    LoadConfigXml();
  }
  
  private static void LoadConfigXml() throws Exception {
    String configFile = FindConfigXml();
    
    ParseConfigXml(configFile);
    
    ValidateParsedConfig();
  }
  
  private static String FindConfigXml() throws Exception {
    String configs[] = { "config.xml", "conf\\config.xml" };
    for (String config : configs) {
      if (IndexConfig.FileExists(config))
        return config;
    }
    Log.error("config.xml not found");
    throw new Exception("config.xml not found");
  }
  
  private static void ParseConfigXml(String configFile) throws Exception {
    XMLConfiguration config;
    try {
      config = new XMLConfiguration(configFile);
      Parsed.INPUT_FILES_DIR = config.getString("params.input_dir").toLowerCase();
      Parsed.WIKI_LINE_FILE_NAME = config.getString("params.input_file").toLowerCase();
      
      Parsed.OUTPUT_DIR = config.getString("params.output_dir").toLowerCase();
      Parsed.OUTPUT_DIR_FINAL = Parsed.OUTPUT_DIR + "\\final";
      Parsed.OUTPUT_DIR_TEMP = Parsed.OUTPUT_DIR + "\\temp";
      
      Parsed.USE_RAM_DIR = config.getInt("params.use_ramdir") == 1;
      Parsed.INDEX_CLOSE_THRESHHOLD = config.getInt("params.index_close_threshhold");
      Parsed.RAM_BUFFER_SIZE_MB = config.getInt("params.ram_buffer_size_mb");
      Parsed.NO_MERGE_POLICY = config.getInt("params.no_merge_policy") == 1;
      
      Parsed.INDEXING_SCHEME = config.getString("params.index_scheme").toLowerCase();
    } catch (ConfigurationException e) {
      Log.error("Exception", e);
      throw new Exception("Error parsing config.xml");
    }
  }
  
  private static void ValidateParsedConfig() throws Exception {
    if (Parsed.INDEXING_SCHEME.equals("base"))
      Parsed.Scheme = IndexingScheme.Base;
    else if (Parsed.INDEXING_SCHEME.equals("multi"))
      Parsed.Scheme = IndexingScheme.Multi;
    else if (Parsed.INDEXING_SCHEME.equals("indep"))
      Parsed.Scheme = IndexingScheme.Indep;
    else
      throw new Exception("INVALID: IndexingScheme");
    
    if (Parsed.WIKI_LINE_FILE_NAME != null && !FileExists(Parsed.WIKI_LINE_FILE_NAME))
      Parsed.WIKI_LINE_FILE_NAME = null;
  }
  
  private static void CleanOutputDirs() throws Exception {
    try {
      FileUtils.deleteDirectory(new File(Parsed.OUTPUT_DIR_FINAL));
      FileUtils.deleteDirectory(new File(Parsed.OUTPUT_DIR_TEMP));
    } catch (IOException e) {
      Log.error("Exception", e);
      throw new Exception("Error Cleaning output dirs");
    }
  }
  
  private static void PrintConfig() {
    Log.info("INDEXING_SCHEME: " + Parsed.INDEXING_SCHEME);
    
    Log.info("USE_RAMDIR: " + Parsed.USE_RAM_DIR);
    Log.info("NO_MERGE_POLICY: " + Parsed.NO_MERGE_POLICY);
    Log.info("RAM_BUFFER_SIZE_MB: " + Parsed.RAM_BUFFER_SIZE_MB);
    Log.info("WIKI_LINE_FILE: " + (Parsed.WIKI_LINE_FILE_NAME == null ? "Not in use" : Parsed.WIKI_LINE_FILE_NAME));
    Log.info("INPUT_DIR: " + Parsed.INPUT_FILES_DIR);
    Log.info("OUTPUT_DIR: " + Parsed.OUTPUT_DIR + " OUTPUT_DIR_FINAL: " + Parsed.OUTPUT_DIR_FINAL + " OUTPUT_DIR_TEMP: " + Parsed.OUTPUT_DIR_TEMP);
    
    Log.info("MAX_MEMORY: " + MAX_MEMORY + " TOTAL_MEMORY: " + TOTAL_MEMORY + " FREE_MEMORY: " + FREE_MEMORY);
    Log.info("MAX_THREADS: " + MAX_THREADS);
    
    Log.info("INDEX_CLOSE_THRESHHOLD: " + Parsed.INDEX_CLOSE_THRESHHOLD);
    Log.info("STATUS_UPDATE_THRESHHOLD_DOCS: " + STATUS_UPDATE_THRESHHOLD_DOCS);
    
    final long dataPerWorker = (INPUT_DATA_SIZE + MAX_THREADS) / MAX_THREADS;
    Log.info("TOTAL_DOCS_TO_INDEX: " + TOTAL_DOCS_TO_INDEX + " INPUT_DATA_SIZE: " + INPUT_DATA_SIZE + " DATA_PER_WORKER: " + dataPerWorker);
  }
  
  private static void FindFilesToIndex() {
    if (UsingWikiLineFile()) {
      WIKI_LINE_FILE = new File(Parsed.WIKI_LINE_FILE_NAME);
      INPUT_DATA_SIZE = FileUtils.sizeOf(WIKI_LINE_FILE);
    } else {
      File inputDir = new File(Parsed.INPUT_FILES_DIR);
      FILES_TO_INDEX = FileUtils.listFiles(inputDir, IndexConfig.EXTENSIONS_TO_INDEX, true);
      TOTAL_DOCS_TO_INDEX = FILES_TO_INDEX.size();
      for (File file : FILES_TO_INDEX)
        INPUT_DATA_SIZE += file.length();
    }
  }
  
  public static boolean UsingWikiLineFile() {
    return Parsed.WIKI_LINE_FILE_NAME != null; // assumes file exists if not
                                               // null
  }
  
  private static void SetThreshholds() throws IOException {
    final long dataPerWorker = (INPUT_DATA_SIZE + MAX_THREADS) / MAX_THREADS;
    if (Parsed.INDEX_CLOSE_THRESHHOLD == 0 || Parsed.INDEX_CLOSE_THRESHHOLD == -1) {
      Parsed.INDEX_CLOSE_THRESHHOLD = (MAX_MEMORY + 3 * MAX_THREADS) / (3 * MAX_THREADS);
      if (Parsed.INDEX_CLOSE_THRESHHOLD > dataPerWorker)
        Parsed.INDEX_CLOSE_THRESHHOLD = dataPerWorker;
    }
    
    if (STATUS_UPDATE_THRESHHOLD_DOCS == 0) {
      if (TOTAL_DOCS_TO_INDEX == 0)
        STATUS_UPDATE_THRESHHOLD_DOCS = 5000;
      else if (TOTAL_DOCS_TO_INDEX < 100)
        STATUS_UPDATE_THRESHHOLD_DOCS = 10;
      else if (TOTAL_DOCS_TO_INDEX < 1000)
        STATUS_UPDATE_THRESHHOLD_DOCS = 50;
      else
        STATUS_UPDATE_THRESHHOLD_DOCS = 200;
    }
  }
}