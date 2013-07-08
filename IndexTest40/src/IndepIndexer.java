import java.io.*;
import java.util.ArrayList;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;

public class IndepIndexer extends MultiIndexer {
	IndexWriter FileSystemBasedWriter = null;

	IndepIndexer(IndexWriter mainIndexWriter, BufferedReader wikiReader,
	    ArrayList<InputFileRec> listOfFilesToIndex) throws IOException {
		super(mainIndexWriter, wikiReader, listOfFilesToIndex);
	}

	public void OpenInterimWriter() throws CorruptIndexException,
	    LockObtainFailedException, IOException {
		if (FileSystemBasedWriter == null)
			FileSystemBasedWriter = MakeFileSystemBasedIndexWriter();
		super.OpenInterimWriter();
	}

	void AddToMainIndex(Directory indexDir) throws CorruptIndexException,
	    IOException {
		FileSystemBasedWriter.addIndexes(indexDir);
	}

	public void Close() throws CorruptIndexException, IOException {
		super.Close();
		if (FileSystemBasedWriter != null)
			FileSystemBasedWriter.close();
	}
}