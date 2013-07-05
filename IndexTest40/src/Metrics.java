import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Metrics {
  private static Logger Log = LoggerFactory.getLogger(Metrics.class);
  
	public static StopWatch Watch = new StopWatch();
	public static AtomicLong TotalDocsIndexed = new AtomicLong(0);
	public static AtomicLong TotalBytesIndexed = new AtomicLong(0);

 public int Dump(String indexDir) {
    int ret = -1;
    Watch.Reset();
    Watch.Start();
    try {
      FSDirectory indexDirFs;
      indexDirFs = FSDirectory.open(new File(indexDir));
      
      IndexReader reader = DirectoryReader.open(indexDirFs);
      
      Log.info("Opening took: " + Metrics.Watch.GetElapsed() + " millisecs");
      
      Watch.Reset();
      Watch.Start();
      
      long uniqueWords = 0;
      
//      Fields fields = reader.getTermVectors(0);
      uniqueWords = reader.getSumTotalTermFreq("body");
//      for (int docId = 0; docId < reader.numDocs(); docId++) {
//      	Fields fields = reader.getTermVectors(docId);
//      	uniqueWords += fields.getUniqueTermCount();
//      }
      
      Log.info("Total number of documents: " + reader.numDocs());
      Log.info("Total number of unique words: " + uniqueWords);
      Log.info("Total calculation took: " + Metrics.Watch.GetElapsed() + " millisecs");
      
      reader.close();
      ret = 0;
    }
    catch (IOException e) {
      Log.error("Exception ", e);
    }
    return ret;
  }

	static String GigsPerHour(long bytesIndexed, long miliSecsElapsed) {
	  double rateInst = (bytesIndexed * IndexConfig.GIGA_PER_HOUR_FACTOR)/miliSecsElapsed;
	  return new DecimalFormat("####.####").format(rateInst);
	}
}
