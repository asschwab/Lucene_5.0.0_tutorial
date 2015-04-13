package de.aptsolutions.lucene_500_test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.GregorianCalendar;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class LuceneClient {

	private static final String PATH_TO_INDEX = "lucene_index_500";

	public static void main(String[] args) throws IOException, ParseException {
		System.out.println("Indexing...");
		index();
		System.out.println("Searching for the issue LUCENE-5945");
		search("versioninfo", "LUCENE-5945");
	}

	/**
	 * build the index
	 * 
	 * @see org.apache.lucene.analysis.standard.StandardAnalyzer
	 * @see org.apache.lucene.index.IndexWriterConfig.IndexWriterConfig
	 * @link 
	 *       http://lucene.apache.org/core/5_0_0/analyzers-common/org/apache/lucene
	 *       /analysis/standard/StandardAnalyzer.html
	 * @link http://lucene.apache.org/core/5_0_0/core/org/apache/lucene/index/
	 *       IndexWriterConfig.html
	 */
	private static void index() throws IOException {
		Directory dir = null;
		IndexWriter writer = null;
		try {
			/*
			 * 1. where to store our index files
			 */
			dir = FSDirectory.open(Paths.get(PATH_TO_INDEX));
			/* use the following line instead, if you only want a ram index */
			// Directory index = new RAMDirectory();

			/*
			 * 2. get an instance of an analyzer, here we use the
			 * StandardAnalyer in earlier versions we had to add a version
			 * parameter, now this is no longer required nor possible
			 */
			Analyzer analyzer = new StandardAnalyzer();

			/*
			 * 3. Create an index writer and its configuration object
			 */
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			// if we want to override an existent index we should use the
			// following line instead
			iwc.setOpenMode(OpenMode.CREATE);
			writer = new IndexWriter(dir, iwc);

			/*
			 * 4. add a sample document to the index
			 */
			Document doc = new Document();
			
			
			// We add  an id field that is searchable, but doesn't trigger tokenization of the content
			Field idField = new StringField("id", "Apache Lucene 5.0.0", Field.Store.YES);
			doc.add(idField);

			// Add the last big lucene version birthday which we don't want to store but to be indexed nevertheless to be filterable
			doc.add(new LongField("lastVersionBirthday", new GregorianCalendar(2015,1,20).getTimeInMillis(), Field.Store.NO));

			// The version info content should be searchable also be tokens, this is why we use a TextField; as we use a reader, the content is not stored!
			doc.add(new TextField("versioninfo", new BufferedReader(
					new InputStreamReader(new FileInputStream(new File("changelog_rel_5.0.0.txt")), StandardCharsets.UTF_8))));

			if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
				// New index
				System.out.println("adding current lucene version with changelog info");
				writer.addDocument(doc);
			} else {
				// Existing index 
				System.out.println("updating index with current lucene version with changelog info");
				writer.updateDocument(new Term("id", "Apache Lucene 5.0.0"), doc);
			}
		} finally {
			if (writer != null)
				writer.close();
			if (dir != null)
				dir.close();
		}

	}

	/**
	 * search the index 
	 */
	private static void search(String field, String value) throws IOException, ParseException {
		/*
		 * 1. build an index reader and index searcher
		 */
		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(PATH_TO_INDEX)));
	    IndexSearcher searcher = new IndexSearcher(reader);
	    
	    /* 
	     * 2. build an analyzer again - use the same as in the indexing process
	     */
	    Analyzer analyzer = new StandardAnalyzer();
	    
	    /*
	     * 3. Build a query parser who will parse our query, written in Lucene Query Language 
	     */
	    QueryParser parser = new QueryParser(field, analyzer);
	    
	    /*
	     * 4. we search the value in a given field, e.g. "versioninfo:LUCENE-5945"
	     */
	    Query query = parser.parse(field + ":" + value);
	    
	    /*
	     * 5. we trigger the search, interested in the 5 first matches
	     */
	    TopDocs results = searcher.search(query, 5);
	    
	    /*
	     * 6. We get the hit information via the scoreDocs attribute in the TopDocs object
	     */
	    ScoreDoc[] hits = results.scoreDocs;	    
	    int numTotalHits = results.totalHits;
	    System.out.println(numTotalHits + " total matching documents");
	    
	    if (hits.length > 0) {
	    	/*
	    	 * Matching score for the first document
	    	 */
	    	System.out.println("Matching score for first document: " + hits[0].score);
	    	
	    	/*
	    	 * We load the document via the doc id to be found in the ScoreDoc.doc attribute
	    	 */
	    	Document doc = searcher.doc(hits[0].doc);
	    	System.out.println("Id of the document: " + doc.get("id"));
	    }
	    
	}
}
