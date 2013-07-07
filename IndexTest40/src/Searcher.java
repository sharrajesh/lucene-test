import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.queryparser.classic.ParseException;

//import org.apache.lucene.index.DocsAndPositionsEnum;
//import org.apache.lucene.index.Terms;
//import org.apache.lucene.index.TermsEnum;
//import org.apache.lucene.util.BytesRef;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class Searcher {
  private static Logger Log = LoggerFactory.getLogger(Searcher.class);
  
  public void SearchIndex(String indexDirToSearch, String queryString) {
    Metrics.Watch.Start();
    
    try {
      IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexDirToSearch)));
      Log.info("Opening of the index took: " + Metrics.Watch.GetElapsed() + " millisecs");
      
      StandardAnalyzer analyzer = new StandardAnalyzer(IndexConfig.LUCENE_VERSION);
      
      QueryParser parser = new QueryParser(IndexConfig.LUCENE_VERSION, "body", analyzer);
      Query query = parser.parse(queryString);
      query.rewrite(reader);
      Log.info("\nQuery Terms: " + query.toString() + " \n");
      Set<Term> queryTerms = new HashSet<Term>();
      query.extractTerms(queryTerms);
      
      IndexSearcher searcher = new IndexSearcher(reader);
      int hitsPerPage = 10;
      TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
      searcher.search(query, collector);
      
      int totalHitsFound = collector.getTotalHits();
      
      int totalIndex = 0;
      ScoreDoc[] hits = collector.topDocs().scoreDocs;
      
      BytesRef queryTermString = new BytesRef(queryString);
      
      while (hits.length > 0) {
        for (int i = 0; i < hits.length; ++i) {
          int docId = hits[i].doc;
          
          Document d = searcher.doc(docId);
          Log.info(totalIndex++ + " title: " + d.get("title") + " date: " + d.get("date"));
          
          // Terms documentTerms = reader.getTermVector(docId, "body");
          // TermsEnum documentTermsEnum = documentTerms.iterator(null);
          //
          // while (documentTermsEnum.next() != null) {
          // if (documentTermsEnum.term().bytesEquals(queryTermString))
          // Log.info(documentTermsEnum.ord() + " - " +
          // documentTermsEnum.term().utf8ToString() + "\n");
          // }
          //
          // DocsAndPositionsEnum docsAndPositionsEnum =
          // documentTermsEnum.docsAndPositions(null, null);
          // while (docsAndPositionsEnum.nextDoc() !=
          // DocIdSetIterator.NO_MORE_DOCS) {
          // for (int f = 0; f < docsAndPositionsEnum.freq(); f++)
          // Log.info(docsAndPositionsEnum.nextPosition() + " " +
          // docsAndPositionsEnum.startOffset() + " " +
          // docsAndPositionsEnum.endOffset() + " " +
          // docsAndPositionsEnum.getPayload() + "\n");
          // }
        }
        hits = searcher.searchAfter(hits[hits.length - 1], query, hitsPerPage).scoreDocs;
      }
      
      reader.close();
      Metrics.Watch.Stop();
      
      Log.info("Index searched : " + indexDirToSearch + ", for query: " + queryString);
      Log.info("Total hits found " + totalHitsFound + " hits");
      Log.info("Total documents in the index:" + reader.numDocs());
      Log.info("Total searching took: " + Metrics.Watch.GetElapsed() + " millisecs");
    } catch (CorruptIndexException e) {
      Log.error("Exception: ", e);
    } catch (IOException e) {
      Log.error("Exception: ", e);
    } catch (ParseException e) {
      Log.error("Exception: ", e);
    }
  }
}