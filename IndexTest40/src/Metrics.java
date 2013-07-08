import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
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
      
      long totalTermsWithDuplicates = 0;
      long totalTermsWithoutDuplicates = 0;
      
      Fields fields = SlowCompositeReaderWrapper.wrap(reader).fields();
      
      for (String field : fields) {
        Log.info("\nTotal terms with duplicates for '" + field + "': " + reader.getSumTotalTermFreq(field));
        totalTermsWithDuplicates += reader.getSumTotalTermFreq(field);
        
        Terms terms = SlowCompositeReaderWrapper.wrap(reader).terms(field);
        long totalTermsWithoutDuplicatesForField = 0;
        TermsEnum termsEnum = terms.iterator(null);
        while (termsEnum.next() != null)
          totalTermsWithoutDuplicatesForField++;
        
        Log.info("Total terms without duplicates for '" + field + "': " + totalTermsWithoutDuplicatesForField);
        totalTermsWithoutDuplicates += totalTermsWithoutDuplicatesForField;
      }
      
      Log.info("\nTotal number of documents: " + ThreadSafeFormatter.Format(reader.numDocs()));
      Log.info("Total terms with duplicates: " + ThreadSafeFormatter.Format(totalTermsWithDuplicates));
      Log.info("Total terms without duplicates: " + ThreadSafeFormatter.Format(totalTermsWithoutDuplicates));
      Log.info("Total calculation took: " + ThreadSafeFormatter.Format(Metrics.Watch.GetElapsed()) + " millisecs");
      
      reader.close();
      ret = 0;
    } catch (IOException e) {
      Log.error("Exception ", e);
    }
    return ret;
  }
  
  static String GigsPerHour(long bytesIndexed, long miliSecsElapsed) {
    double rateInst = (bytesIndexed * IndexConfig.GIGA_PER_HOUR_FACTOR) / miliSecsElapsed;
    return ThreadSafeFormatter.Format(rateInst);
  }
}
