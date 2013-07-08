import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexMain {
  private static Logger Log = LoggerFactory.getLogger(IndexMain.class);
  
  public static void Usage() {
    Log.info("Usage: ");
    Log.info("\tIndexTest {index}|{query querystring}|{metrics}");
    Log.info("e.g.");
    Log.info(" IndexTest index -- to index the configured folder");
    Log.info(" IndexTest query querystring -- to query the configured index folder");
    Log.info(" IndexTest metrics -- to dump metrics for this index");
  }
  
  enum IndexCommand {
    Invalid, Index, Query, Metrics
  };
  
  private static IndexCommand GetIndexCommand(String[] args) throws IllegalArgumentException {
    if (args.length == 1 && args[0].equalsIgnoreCase("index"))
      return IndexCommand.Index;
    else if (args.length == 2 && args[0].equalsIgnoreCase("query"))
      return IndexCommand.Query;
    else if (args.length == 1 && args[0].equalsIgnoreCase("metrics"))
      return IndexCommand.Metrics;
    else
      throw new IllegalArgumentException("Invalid Arguments Passed.");
  }
  
  public static void main(String[] args) {
    Logging.Configure();
    try {
      IndexCommand indexCommand = GetIndexCommand(args);
      switch (indexCommand) {
        case Index:
          Index();
          break;
        case Query:
          Search(args[1]);
          break;
        case Metrics:
          Metrics();
          break;
        default:
          throw new Exception("Invalid Command");
      }
    } catch (IllegalArgumentException e) {
      Usage();
    } catch (Exception e) {
      Log.error("Exception", e);
    }
    return;
  }
  
  public static void Index() throws Exception {
    IndexConfig.SetupForIndex();
    
    IndexMain indexer = new IndexMain();
    indexer.BuildIndex();
  }
  
  private static void Search(String queryString) throws Exception {
    IndexConfig.SetupForSearch();
    
    Searcher searcher = new Searcher();
    searcher.SearchIndex(IndexConfig.Parsed.OUTPUT_DIR_FINAL, queryString);
  }
  
  private static void Metrics() throws Exception {
    IndexConfig.SetupForSearch();
    new Metrics().Dump(IndexConfig.Parsed.OUTPUT_DIR_FINAL);
  }
  
  private void BuildIndex() throws Exception {
    ArrayList<BaseIndexer.InputFileRec> listOfFilesToIndex = null;
    BufferedReader wikiReader = null;
    
    if (IndexConfig.UsingWikiLineFile())
      wikiReader = new BufferedReader(new FileReader(IndexConfig.WIKI_LINE_FILE));
    else {
      listOfFilesToIndex = new ArrayList<BaseIndexer.InputFileRec>();
      for (File file : IndexConfig.FILES_TO_INDEX)
        listOfFilesToIndex.add(new BaseIndexer.InputFileRec(file, file.length()));
    }
    
    Metrics.Watch.Start();
    
    IndexWriter mainIndexWriter = BaseIndexer.MakeIndexWriter(FSDirectory.open(new File(IndexConfig.Parsed.OUTPUT_DIR_FINAL)));
    List<Thread> interimIndexerThreads = new ArrayList<Thread>();
    for (int i = 0; i < IndexConfig.MAX_THREADS; i++) {
      Thread t = null;
      if (IndexConfig.Parsed.Scheme == IndexConfig.IndexingScheme.Base)
        t = new Thread(new BaseIndexer(mainIndexWriter, wikiReader, listOfFilesToIndex));
      else if (IndexConfig.Parsed.Scheme == IndexConfig.IndexingScheme.Multi)
        t = new Thread(new MultiIndexer(mainIndexWriter, wikiReader, listOfFilesToIndex));
      else if (IndexConfig.Parsed.Scheme == IndexConfig.IndexingScheme.Indep)
        t = new Thread(new IndepIndexer(mainIndexWriter, wikiReader, listOfFilesToIndex));
      else
        throw new Exception("Invalid Indexing Scheme");
      t.start();
      interimIndexerThreads.add(t);
    }
    
    for (Thread indexerThread : interimIndexerThreads) {
      try {
        indexerThread.join();
      } catch (InterruptedException e) {
        Log.error("[" + indexerThread.getId() + "]  threw exception ");
        e.printStackTrace();
      }
    }
    
    Log.info("************************");
    Log.info("Until closing took: " + ThreadSafeFormatter.Format(Metrics.Watch.GetElapsed()) + " millisecs");
    Log.info(ThreadSafeFormatter.Format(mainIndexWriter.numDocs()) + " documents were added.");
    
    long startClosingTime = Metrics.Watch.GetElapsed();
    mainIndexWriter.close();
    long finalClosingTime = Metrics.Watch.GetElapsed() - startClosingTime;
    Log.info("Final closing took: " + ThreadSafeFormatter.Format(finalClosingTime) + " millisecs");
    
    Metrics.Watch.Stop();
    
    long totalBytesIndexed = Metrics.TotalBytesIndexed.get();
    long totalIndexingTime = Metrics.Watch.GetElapsed();
    Log.info("Total indexing took: " + ThreadSafeFormatter.Format(totalIndexingTime) + " millisecs " 
        + "GigsPerHour OverAll Rate: " + Metrics.GigsPerHour(totalBytesIndexed, totalIndexingTime));
  }
}