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
  	Invalid,
  	Index,
  	Query,
  	Metrics
  };
  
  private static IndexCommand GetIndexCommand(String[] args) throws IllegalArgumentException {
    if (args.length == 1 && args[0].equalsIgnoreCase("index"))
    	return IndexCommand.Index;
    else if(args.length == 2 && args[0].equalsIgnoreCase("query"))
    	return IndexCommand.Query;
    else if(args.length == 1 && args[0].equalsIgnoreCase("metrics"))
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
		} 
		catch (IllegalArgumentException e) {
			Usage();
		}
		catch (Exception e) {
			Log.error("Exception", e);
		}
		return;
  }

  public static void Index() throws Exception{
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
    Metrics.Watch.Start();
    
    IndexWriter mainIndexWriter = BaseIndexer.MakeIndexWriter(FSDirectory.open(new File(IndexConfig.Parsed.OUTPUT_DIR_FINAL)));
    List<Thread> interimIndexerThreads = new ArrayList<Thread>();
    for (int i = 0; i < IndexConfig.MAX_THREADS; i++) {
      Thread t = null;
      if (IndexConfig.Parsed.Scheme == IndexConfig.IndexingScheme.Base)
        t = new Thread(new BaseIndexer(mainIndexWriter));
      else if (IndexConfig.Parsed.Scheme == IndexConfig.IndexingScheme.Multi)
        t = new Thread(new MultiIndexer(mainIndexWriter));
      else if (IndexConfig.Parsed.Scheme == IndexConfig.IndexingScheme.Indep)
        t = new Thread(new IndepIndexer(mainIndexWriter));
      else
      	throw new Exception("Invalid Indexing Scheme");
      t.start();
      interimIndexerThreads.add(t);
    }
    
    for (Thread indexerThread : interimIndexerThreads) {
      try {
        indexerThread.join();
      }
      catch (InterruptedException e) {
        Log.error("[" + indexerThread.getId() + "]  threw exception ");
        e.printStackTrace();
      }
    }

    Log.info("************************");
    Log.info("Until closing took: " + Metrics.Watch.GetElapsed() + " millisecs");
    Log.info(mainIndexWriter.numDocs() + " documents were added.");

    long startClosingTime = Metrics.Watch.GetElapsed();
    mainIndexWriter.close();
    Log.info("Final closing took: " + (Metrics.Watch.GetElapsed() - startClosingTime) + " millisecs");
    
    Metrics.Watch.Stop();
    
    long totalTime = Metrics.Watch.GetElapsed();
    Log.info("Total indexing took: " + totalTime + " millisecs" + " GigsPerHour OverAll Rate: " + Metrics.GigsPerHour(Metrics.TotalBytesIndexed.get(), totalTime));
  }
}