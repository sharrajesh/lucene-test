import java.io.*;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;

public class IndepIndexer extends MultiIndexer {
	IndexWriter FileSystemBasedWriter = null;

	IndepIndexer(IndexWriter mainIndexWriter) throws IOException {
		super(mainIndexWriter);
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