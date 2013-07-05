import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Random;

public class MultiIndexer extends BaseIndexer {
  private static Logger Log = LoggerFactory.getLogger(MultiIndexer.class);
  
  IndexWriter InterimWriter = null;
  long InterimCloseThreshhold = 0;
  Random Rand = new Random();

  MultiIndexer(IndexWriter mainIndexWriter) throws IOException {
    super(mainIndexWriter);
  }
  
  long GetNewCloseThreshhold() {
    int minVal = (int) IndexConfig.Parsed.INDEX_CLOSE_THRESHHOLD / 2;
    long newThreshhold = Rand.nextInt(minVal) + minVal; 
    Log.info("[" + Thread.currentThread().getId() + "]  New close threshhold: " + newThreshhold);
    return newThreshhold;
  }

  int TempIndexCounter = 700;
  protected IndexWriter MakeFileSystemBasedIndexWriter() throws CorruptIndexException, LockObtainFailedException, IOException {
    String tempOutDir = IndexConfig.Parsed.OUTPUT_DIR_TEMP + "\\index-" + Thread.currentThread().getId() + "-" + TempIndexCounter++;
    return MakeIndexWriter(FSDirectory.open(new File(tempOutDir)));
  }
  
  private IndexWriter MakeInterimWriter() throws IOException {
    if (IndexConfig.Parsed.USE_RAM_DIR)
      return MakeIndexWriter(new RAMDirectory());
    else
      return MakeFileSystemBasedIndexWriter();
  }
  
  public void OpenInterimWriter() throws CorruptIndexException, LockObtainFailedException, IOException {
    if (InterimWriter == null) {
      InterimWriter = MakeInterimWriter();
      InterimCloseThreshhold = GetNewCloseThreshhold();
    }
    else {
      if (InterimBytesIndexed > InterimCloseThreshhold) {
        InterimCloseThreshhold = GetNewCloseThreshhold();
        InterimBytesIndexed = 0;
        CloseInterimWriter();
        InterimWriter = MakeInterimWriter();
      }
    }
  }
  
  public void AddDocument(Document doc) throws CorruptIndexException, IOException {
    InterimWriter.addDocument(doc);    
  }
  
  protected void CloseInterimWriter() throws CorruptIndexException, IOException {
    if (InterimWriter != null) {
      Directory idxDir = InterimWriter.getDirectory();
      InterimWriter.close();
      InterimWriter = null;
      AddToMainIndex(idxDir);
      idxDir.close();
    }
  }
  
  public void Close() throws CorruptIndexException, IOException {
    CloseInterimWriter();
    super.Close();
  }
}