/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 *   Additions: Hedvig Kjellström, 2012-14
 */  


//package ir;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;


/**
 *   Implements an inverted index as a HashTable from words to PostingsLists.
 */
public class HashedIndex implements Index {

	/** The index as a HashTable. */
	private HashMap<String, PostingsList> index = new HashMap<String,PostingsList>();

	/** The pagerank scores for each document*/
	private HashMap<String, Double> pageranks = new HashMap<String, Double>();

	/**
	 * Loads the pre-computed pageranks and stores them in
	 * pageranks.
	 */
	public void loadPageRanks(){
		String prFilename = "pagerank_exact_results.txt";
		String articleTitlesFilename = "pagerank\\articleTitles.txt";
		readPageRanks(prFilename, articleTitlesFilename);
	}

	/**
	 *  Inserts this token in the index.
	 */
	public void insert( String token, int docID, int offset ) {
		PostingsList pl = index.get(token);
		//Check if token already is in index or not.
		if (pl != null){
			pl.add(docID, offset);
		} else {
			try {
				pl = new PostingsList();
			} catch (Exception e) {
				e.toString();
				e.printStackTrace();
			}
			pl.add(docID, offset);
			index.put(token, pl);
		}
	}


	/**
	 *  Returns all the words in the index.
	 */
	public Iterator<String> getDictionary() {
		return index.keySet().iterator();
	}


	/**
	 *  Returns the postings for a specific term, or null
	 *  if the term is not in the index.
	 */
	public PostingsList getPostings( String token ) {
		return index.get(token);
	}


	/**
	 *  Searches the index for postings matching the query.
	 */
	public PostingsList search( Query query, int queryType, int rankingType, int structureType ) {
		PostingsList ret = null;

		if(queryType == INTERSECTION_QUERY){ //query using the intersection algorithm
			Iterator<String> it = query.terms.iterator();
			PostingsList temp1 = index.get(it.next()), temp2 = null;
			while(it.hasNext()){
				temp2 = index.get(it.next());
				temp1 = intersect(temp1, temp2);
			}
			ret = temp1;
		} else if(queryType == PHRASE_QUERY){
			Iterator<String> it = query.terms.iterator();
			PostingsList temp1 = index.get(it.next()), temp2 = null;
			while(it.hasNext()){
				temp2 = index.get(it.next());
				temp1 = phrase(temp1, temp2);
			}
			ret = temp1;
		} else if(queryType == RANKED_QUERY){
			if(rankingType == TF_IDF){
				ret = cosineScore(query, false);
			} else if(rankingType == PAGERANK){
				ret = pageRanking(query);
			} else if(rankingType == COMBINATION){
				ret = cosineScore(query, true);
			}
		}

		return ret;
	}

	/**
	 * Intersects the two parameterized PostingsLists and returns the
	 * intersection.
	 * 
	 * @return The intersection of p1 and p2.
	 */
	private PostingsList intersect(PostingsList p1, PostingsList p2){
		//Some sanity-checks
		if(p1 == null && p2 == null) return new PostingsList();
		if(p1 == null || p1.size() == 0) return p2;
		if(p2 == null || p2.size() == 0) return p1;

		//The intersect-algorithm from page 11 in the course book.
		PostingsList ret = new PostingsList();

		int index1 = 0, index2 = 0;
		while(index1 < p1.size() && index2 < p2.size()){
			if(p1.get(index1).docID == p2.get(index2).docID){
				ret.add(p1.get(index1).clone());
				index1++;
				index2++;
			} else if(p1.get(index1).docID < p2.get(index2).docID){
				index1++;
			} else index2++;
		}

		return ret;
	}

	/**
	 * Returns a PostingsList containing intersections of the offset-lists
	 * in the parameterized PostingsLists PostingsEntries where the word
	 * represented by p1 is directly followed by the word represented by p2.
	 */
	private PostingsList phrase(PostingsList p1, PostingsList p2){
		PostingsList ret = new PostingsList();
		//Some sanity-checks
		if(p1 == null && p2 == null) return new PostingsList();
		if(p1 == null || p1.size() == 0) return ret;
		if(p2 == null || p2.size() == 0) return ret;

		int PLindex1 = 0, PLindex2 = 0;
		while (PLindex1 < p1.size() && PLindex2 < p2.size()){
			//First check for equal docID:s
			PostingsEntry pe1 = p1.get(PLindex1), pe2 = p2.get(PLindex2); 
			if(pe1.docID == pe2.docID){


				//-------------Intersect the offset-lists-------------
				int offsetIndex1 = 0, offsetIndex2 = 0;

				while(offsetIndex1 < pe1.offsets.size() && 
						offsetIndex2 < pe2.offsets.size()){
					if(pe1.offsets.get(offsetIndex1) + 1 == pe2.offsets.get(offsetIndex2)){
						ret.add(pe2.docID, pe2.offsets.get(offsetIndex2));
						offsetIndex1++; offsetIndex2++;
					} else if(pe1.offsets.get(offsetIndex1) + 1 < pe2.offsets.get(offsetIndex2)){
						offsetIndex1++;
					} else offsetIndex2++;
				}
				//-------------end of intersection of offset-lists-------------


				PLindex1++; PLindex2++;
			} else if(pe1.docID < pe2.docID){
				PLindex1++;
			} else PLindex2++;
		}
		return ret;
	}

	/**
	 * @param query The query to be used when searching
	 * @param includePageRank Tell whether pagerank scores should be weighted in or not.
	 * @return A ranked PostingsList.
	 * @throws Exception 
	 */
	private PostingsList cosineScore(Query query, boolean includePageRank){
		PostingsList ret = null;

		try {
			ret = new PostingsList("ArrayList");
		} catch (Exception e) {
			e.toString();
			System.exit(0);
		}

		//Make sure that all words in query appear only once.
		HashSet<String> words = new HashSet<String>();
		Iterator<String> iterator = query.terms.iterator();
		int pos = 0;
		String word = "";
		while(iterator.hasNext()){
			word = iterator.next();
			if(words.contains(word)){
				//Remove duplicate word and its corresponding weight.
				iterator.remove();
				query.weights.remove(pos);
				pos--; //adjust position
			} else {
				words.add(word);
			}
			pos++;
		}
		System.err.println("\n\nQuery length: " + query.size() + " ");
		Iterator<String> itTerms = query.terms.iterator();
		Iterator<Double> itWeights = query.weights.iterator();
		while(itTerms.hasNext() && itWeights.hasNext()){ //For each query term...
			String queryTerm = itTerms.next();
			double queryTermWeight = itWeights.next();
			//System.err.print("(" + queryTerm + ", " + queryTermWeight + ") ");
			// Calculate the weight of the current dimension in the query vector.
			double w_tq = 1.0 * inverseDocumentFrequency(queryTerm);
			PostingsList queryTermPL = index.get(queryTerm);
			for(int i = 0; i < queryTermPL.size(); i++){ //For each document with query term
				//Calculate the weight of the current dimension in the document vector.
				double tf_d = 1 + Math.log10(queryTermPL.get(i).offsets.size());
				double w_td = tf_d * inverseDocumentFrequency(queryTerm);
				//Check if this document is already in ret.
				//If it isn't, add it to ret and set the score. Otherwise simply update the score.
				//This is the dot product being calculated. 
				int currentDocID = queryTermPL.get(i).docID;
				if(ret.findPostingsEntry(currentDocID) == -1){
					PostingsEntry tempPE = new PostingsEntry(currentDocID);
					tempPE.score = w_tq * w_td * queryTermWeight;
					ret.addInSortedPosition(tempPE);
				} else {
					ret.get(ret.findPostingsEntry(currentDocID)).score += w_tq * w_td * queryTermWeight;
				}
			}
		}
		//normalize the scores
		PostingsEntry temp = null;
		for (int i = 0; i < ret.size(); i++){
			temp = ret.get(i) ;
			temp.score /= Math.sqrt(docLengths.get("" + temp.docID)) * query.size();
		}

		//If wanted, include the pagerank scores.
		double prWeight = 0.5;
		if(includePageRank){
			for (int i = 0; i < ret.size(); i++){
				temp = ret.get(i);
				temp.score = prWeight * (pageranks.get(docIDs.get(temp.docID + ""))) + (1-prWeight) * temp.score;
			}
		}

		//sort and return the result
		ret.sortList();
		return ret;
	}


	/**
	 * @return A PostingsList containing documents ordered according to their pagerank score.
	 */
	private PostingsList pageRanking(Query query){
		PostingsList ret = null;

		try {
			ret = new PostingsList("ArrayList");
		} catch (Exception e) {
			e.toString();
			System.exit(0);
		}

		Iterator<String> it = query.terms.iterator();
		String queryTerm;
		PostingsList queryTermPL;
		PostingsEntry pe;
		while(it.hasNext()){
			queryTerm = it.next();
			queryTermPL = index.get(queryTerm);
			for(int i = 0; i < queryTermPL.size(); i++){ //For each document with query term
				pe = queryTermPL.get(i);
				if(ret.findPostingsEntry(pe.docID) == -1){//a new document
					PostingsEntry newpe = new PostingsEntry(pe.docID);
					newpe.score = pageranks.get(docIDs.get(pe.docID + ""));
					ret.addInSortedPosition(newpe);
				}
			}
		}

		//sort and return the result
		ret.sortList();
		return ret;
	}

	/**
	 * Reads pagerank scores from a file and stores them.
	 * @param prFilename File containing the pageranks
	 * @param articleTitlesFilename File containing translation from linkIDs to Article Titles.
	 */
	private void readPageRanks(String prFilename, String articleTitlesFilename){
		try {
			//First read the pageranks from the file.
			//These are stored using the links IDs.
			HashMap<Integer, Double> prLinksID = new HashMap<Integer, Double>();
			BufferedReader prIn = new BufferedReader( new FileReader( prFilename ));
			String line;
			while ((line = prIn.readLine()) != null) {
				//Fetch the document name and its score.
				int index = line.indexOf( ":" );
				Integer linkID = new Integer(line.substring( 0, index ));
				Double rank = new Double(line.substring(index+1, line.length()));

				//Store the score
				prLinksID.put(linkID, rank);
			}
			prIn.close();

			//Then read the translation from linkIDs to article titles
			HashMap<Integer, String> articleTitles = new HashMap<Integer, String>();
			BufferedReader atIn = new BufferedReader(new FileReader(articleTitlesFilename));
			while((line = atIn.readLine()) != null){
				//Fetch the document link id and title
				int index = line.indexOf(";");
				Integer linkID = new Integer(line.substring(0, index));
				String title = line.substring(index + 1);
				articleTitles.put(linkID, title);
			}
			atIn.close();

			//Now use the pageranks and the linkid-to-article title translation
			//to fill our pagerank HashMap<docname, pagerank score>
			Iterator<Integer> linkIDs  = prLinksID.keySet().iterator();
			Integer linkid;
			String docName;
			while(linkIDs.hasNext()){
				linkid = linkIDs.next();
				docName = articleTitles.get(linkid);
				if(docName == null) continue;
				pageranks.put(docName, prLinksID.get(linkid));
			}
		}
		catch ( FileNotFoundException e ) {
			e.printStackTrace();
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the score for the term in docID.
	 */
	public double getScore(String term, int docID){
		double ret = 0;
		PostingsList pl = index.get(term);
		double tf = 1 + Math.log10(pl.get(pl.findPostingsEntry(docID)).offsets.size());
		ret = inverseDocumentFrequency(term) * tf;
		return ret;
	}

	/**
	 * @param term The term which inverse document frequency is to be returned
	 * @return	The inverse document frequency for term 
	 */
	private double inverseDocumentFrequency(String term){
		return Math.log10(docIDs.size() / documentFrequency(term));
	}

	/**
	 * @param term The term which doc. frequency is to be returned
	 * @return	The document frequency for term
	 */
	private double documentFrequency(String term){
		PostingsList pl = index.get(term);
		if(pl == null){
			return 0;
		} else{
			return index.get(term).size();
		}
	}

	/**
	 *  No need for cleanup in a HashedIndex.
	 */
	public void cleanup() {}
}
