import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.NoMergePolicy;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;

public class BaseIndexer implements Runnable {
  private static Logger Log = LoggerFactory.getLogger(BaseIndexer.class);
  
  class WikiLineDataRec {
    String Title;
    String Date;
    String Body;
  };

  static public class InputFileRec {
	  public File FileHandle;
	  public long FileSize;
	
	  public InputFileRec(File f, long s) {
	    FileHandle = f;
	    FileSize = s;
	  }
	}

  ArrayList<InputFileRec> ListOfFilesToIndex;
  BufferedReader WikiReader;
  WikiLineDataRec TempDataRec = new WikiLineDataRec();

  IndexWriter MainIndexWriter;
  
  BaseIndexer(IndexWriter mainIndexWriter, BufferedReader wikiReader, ArrayList<InputFileRec> listOfFilesToIndex) throws IOException {
    MainIndexWriter = mainIndexWriter;
    ListOfFilesToIndex = listOfFilesToIndex;
    WikiReader = wikiReader;
    CreateIndexableFieldType();
  }
  
  private FieldType IndexableFieldType;
  
  void CreateIndexableFieldType() {
  	IndexableFieldType = new FieldType();
    
  	IndexableFieldType.setStoreTermVectors(true);
  	IndexableFieldType.setStoreTermVectorPositions(true);
  	IndexableFieldType.setStoreTermVectorPayloads(true);
    
  	IndexableFieldType.setIndexed(true);
  	IndexableFieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
    
  	IndexableFieldType.setStored(true);    
  }

  protected static IndexWriterConfig MakeIndexWriterConfig() throws IOException {
    IndexWriterConfig indexWriterConfig = new IndexWriterConfig(IndexConfig.LUCENE_VERSION, new StandardAnalyzer(IndexConfig.LUCENE_VERSION));
    if (IndexConfig.Parsed.RAM_BUFFER_SIZE_MB != 0)
      indexWriterConfig.setRAMBufferSizeMB(IndexConfig.Parsed.RAM_BUFFER_SIZE_MB);
    if (IndexConfig.Parsed.NO_MERGE_POLICY)
      indexWriterConfig.setMergePolicy(NoMergePolicy.NO_COMPOUND_FILES);
    return indexWriterConfig;
  }

  protected static IndexWriter MakeIndexWriter(Directory outputDir) throws IOException {
    return new IndexWriter(outputDir, MakeIndexWriterConfig());
  }

  public void Close() throws CorruptIndexException, IOException {}

  public void OpenInterimWriter() throws CorruptIndexException, LockObtainFailedException, IOException {}

  void AddToMainIndex(Directory indexDir) throws CorruptIndexException, IOException {
    MainIndexWriter.addIndexes(indexDir);
  }

  public void AddDocument(Document doc) throws CorruptIndexException, IOException {
    MainIndexWriter.addDocument(doc);
  }

  private String GetNextLine() throws IOException {
    synchronized (WikiReader) {
      return WikiReader.readLine();
    }
  }

  private InputFileRec GetNextFileRec() {
    synchronized (ListOfFilesToIndex) {
      Iterator<InputFileRec> itr = ListOfFilesToIndex.iterator();
      if (itr.hasNext()) {
        InputFileRec fr = itr.next();
        itr.remove();
        return fr;
      }
      else
        return null;
    }
  }

  private boolean GetNextRec(WikiLineDataRec dataRec) throws IOException {
    String line;
    while ((line = GetNextLine()) != null) {
      int k1 = 0;
      int k2 = line.indexOf(IndexConfig.SEP, k1);
      if (k2 < 0)
        continue;
      dataRec.Title = line.substring(k1, k2);
      k1 = k2 + 1;
      k2 = line.indexOf(IndexConfig.SEP, k1);
      if (k2 < 0)
        continue;
      dataRec.Date = line.substring(k1, k2);
      k1 = k2 + 1;
      k2 = line.indexOf(IndexConfig.SEP, k1);
      if (k2 >= 0)
        continue;
      dataRec.Body = line.substring(k1);
      return true;
    }
    return false;
  }
  
  private Document GetNextDocumentFromWikiReader() throws IOException {
    Document doc = null;
    if (GetNextRec(TempDataRec)) {
      doc = new Document();
      
      doc.add(new TextField("title", TempDataRec.Title, Field.Store.YES));
      doc.add(new TextField("date", TempDataRec.Date, Field.Store.YES));
      doc.add(new Field("body", TempDataRec.Body, IndexableFieldType));

      UpdateStats(TempDataRec.Body.length());
    }
    return doc;
  }
  
  private Document GetNextDocumentFromMainList() throws IOException {
    InputFileRec fileRec = GetNextFileRec();
    if (fileRec == null)
      return null;
    
    File file = fileRec.FileHandle;
    
    Document doc = new Document();
    
    doc.add(new TextField("title", file.getPath(), Field.Store.YES));
    
    String body = FileUtils.readFileToString(file);
    doc.add(new Field("body", body, IndexableFieldType));
    //doc.add(new Field("body", new FileReader(file), IndexableFieldType));
    
    BasicFileAttributes attributes = Files.readAttributes(Paths.get(file.getPath()), BasicFileAttributes.class);
    doc.add(new LongField("date", attributes.creationTime().toMillis(), Field.Store.YES));
    
    UpdateStats(fileRec.FileSize);
    
    return doc;
  }
  
  private Document GetNextDocument() throws IOException {
    if (WikiReader == null)
      return GetNextDocumentFromMainList();
    else
      return GetNextDocumentFromWikiReader();
  }
  
  StopWatch AddDocsWatch = new StopWatch();
  
  long LocalDocsIndexed = 0;
  long LocalBytesIndexed = 0;
  long InterimBytesIndexed = 0;

  private void BuildIndex() throws IOException {
    Document doc;
    while ((doc = GetNextDocument()) != null) {
      OpenInterimWriter();

      AddDocsWatch.Start();
      AddDocument(doc);
      AddDocsWatch.Stop();

      PrintStatusUpdate();
    }
    Close();
    PrintExitStatus();
  }

  private void UpdateStats(long docSize) {
    LocalBytesIndexed += docSize;
    InterimBytesIndexed += docSize;
    Metrics.TotalBytesIndexed.addAndGet(docSize);
    
    LocalDocsIndexed++;
    Metrics.TotalDocsIndexed.incrementAndGet();
  }
  
  private void PrintStatusUpdate() {
    if (LocalDocsIndexed % IndexConfig.STATUS_UPDATE_THRESHHOLD_DOCS == 0) {
      Runtime.getRuntime().gc();
      long totalBytesIndexed = Metrics.TotalBytesIndexed.get();
      long totalTimeElapsed = Metrics.Watch.GetElapsed();
      Log.info("[" + Thread.currentThread().getId() + "]  Adding TotalDocs: " + Metrics.TotalDocsIndexed.get() + 
          " LocalDocs: " + LocalDocsIndexed + " TotalBytes: " + totalBytesIndexed + " LocalBytes: " + LocalBytesIndexed + " TotalTimeElapsed: " + totalTimeElapsed);
      Log.info("[" + Thread.currentThread().getId() + "]  GigsPerHour Thread Rate: " + 
          Metrics.GigsPerHour(LocalBytesIndexed, AddDocsWatch.GetElapsed()) + " OverAll Rate: " + Metrics.GigsPerHour(totalBytesIndexed, totalTimeElapsed));
    }
  }

  void PrintExitStatus() {
    long totalBytesIndexed = Metrics.TotalBytesIndexed.get();
    long totalTimeElapsed = Metrics.Watch.GetElapsed();
    Log.info("[" + Thread.currentThread().getId() + "] TotalDocs: " + Metrics.TotalDocsIndexed.get() + 
        " LocalDocs: " + LocalDocsIndexed + " TotalBytes: " + totalBytesIndexed + " LocalBytes: " + LocalBytesIndexed + " ExitingAfter: " + totalTimeElapsed + " millisecs");
    Log.info("[" + Thread.currentThread().getId() + "] GigsPerHour OverAll Rate: " + Metrics.GigsPerHour(totalBytesIndexed, totalTimeElapsed));
  }

  @Override
  public void run() {
    try {
      BuildIndex();
    }
    catch (IOException e) {
      Log.error("[" + Thread.currentThread().getId() + "]  exception happend after : " + Metrics.Watch.GetElapsed() + " millisecs");
      Log.error("Exception", e);
    }
  }
}