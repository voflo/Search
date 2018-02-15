/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Hedvig Kjellström, 2012
 */  

//package ir;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;

public class Query {

	public LinkedList<String> terms = new LinkedList<String>();
	public LinkedList<Double> weights = new LinkedList<Double>();

	/**
	 *  Creates a new empty Query 
	 */
	public Query() {
	}

	/**
	 *  Creates a new Query from a string of words
	 */
	public Query( String queryString  ) {
		StringTokenizer tok = new StringTokenizer( queryString );
		while ( tok.hasMoreTokens() ) {
			terms.add( tok.nextToken() );
			weights.add( new Double(1) );
		}    
	}

	/**
	 *  Returns the number of terms
	 */
	public int size() {
		return terms.size();
	}

	/**
	 *  Returns a shallow copy of the Query
	 */
	public Query copy() {
		Query queryCopy = new Query();
		queryCopy.terms = (LinkedList<String>) terms.clone();
		queryCopy.weights = (LinkedList<Double>) weights.clone();
		return queryCopy;
	}

	/**
	 *  Expands the Query using Relevance Feedback.
	 *  
	 *  @param results Contains the ranked list from the current search
	 *  @param docIsRelevant Contains the users feedback on which of the 10 first hits are relevant
	 */
	public void relevanceFeedback( PostingsList results, boolean[] docIsRelevant, Indexer indexer ) {
		double alpha = 1, beta = 0.75;
		System.err.println("alpha = " + alpha + " beta = " + beta);
		
		//Multiply weights of original query terms with alpha
		ListIterator<Double> it = weights.listIterator();
		while(it.hasNext()){
			double w = it.next();
			it.set(w*alpha);
		}

		//Add query terms from relevant docs, set/update weights of these to beta*<weight of term in doc>		
		for(int i = 0; i < docIsRelevant.length; i++){
			if(!docIsRelevant[i]) continue; //if the document was not marked as relevant, continue to the next one.

			//Fetch the words from the document
			int docID = results.get(i).docID;
			List<String> words = getWordsFromDoc(docID, indexer.index);

			//Add word to query, set its weight.
			//Or find the word in the query and update its weight
			int pos = -2;
			for(String word : words){
				pos = getWordPosInQuery(word);
				double newWeight = beta * indexer.index.getScore(word, docID);

				if(pos == -1){ //The case when we need to add the word to the query
					terms.add(word);
					weights.add(newWeight);
				} else { //The case when the word is already in the query
					weights.set(pos, weights.get(pos) + newWeight);
				}
			}
		}
	}

	/**
	 * Returns the position of the parameterized word in the query.
	 * @param word The word to look for in the query.
	 * @return The position of the word in the query, or -1 if it wasn't found.
	 */
	private int getWordPosInQuery(String word){
		for (int i = 0; i < terms.size(); i++)
			if(word.equals(terms.get(i))) return i;

		return -1;
	}

	/**
	 * Builds a list of the words within a document.
	 * @param docID The docID for the document to read through.
	 * @param index The index for the current document collection.
	 * @return A list of the words within the document docID.
	 */
	private List<String> getWordsFromDoc(int docID, Index index){
		List<String> words = new LinkedList<String>();

		Iterator<String> it = index.getDictionary();
		String word = "";
		PostingsList pl = null;
		while(it.hasNext()){
			word = it.next();
			pl = index.getPostings(word);
			if(pl.findPostingsEntry(docID) != -1){
				words.add(word);
			}
		}

		return words;
	}
}