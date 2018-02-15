/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2012
 */  

import java.util.*;

import jdk.nashorn.internal.ir.ContinueNode;

import java.io.*;

public class PageRank{

	/**  
	 *   Maximal number of documents. We're assuming here that we
	 *   don't have more docs than we can keep in main memory.
	 */
	final static int MAX_NUMBER_OF_DOCS = 2000000;

	/**
	 *   Mapping from document names to document numbers.
	 */
	Hashtable<String,Integer> docNumber = new Hashtable<String,Integer>();

	/**
	 *   Mapping from document numbers to document names
	 */
	String[] docName = new String[MAX_NUMBER_OF_DOCS];

	/**  
	 *   A memory-efficient representation of the transition matrix.
	 *   The outlinks are represented as a Hashtable, whose keys are 
	 *   the numbers of the documents linked from.<p>
	 *
	 *   The value corresponding to key i is a Hashtable whose keys are 
	 *   all the numbers of documents j that i links to.<p>
	 *
	 *   If there are no outlinks from i, then the value corresponding 
	 *   key i is null.
	 */
	HashMap<Integer,HashMap<Integer,Boolean>> link = new HashMap<Integer,HashMap<Integer,Boolean>>();

	/**
	 *   The number of outlinks from each node.
	 */
	int[] out = new int[MAX_NUMBER_OF_DOCS];

	/**
	 *   The number of documents with no outlinks.
	 */
	int numberOfSinks = 0;

	/**
	 *   The probability that the surfer will be bored, stop
	 *   following links, and take a random jump somewhere.
	 */
	final static double BORED = 0.15;//Real value is 0.15

	/**
	 *   Convergence criterion: Transition probabilities do not 
	 *   change more that EPSILON from one iteration to another.
	 */
	final static double EPSILON = 0.0001;

	/**
	 *   Never do more than this number of iterations regardless
	 *   of whether the transition probabilities converge or not.
	 */
	final static int MAX_NUMBER_OF_ITERATIONS = 1000;

	/**
	 * Variable holding the values for the P matrix for the case
	 * when a document is a sink.
	 */
	private final double sink_value;

	/**
	 * Variable holding the value for the P matrix for the case
	 * when the link looked for does not exist.
	 */
	private final double no_link_value;

	/**
	 * Number of documents in the document collection.
	 */
	private final int numberOfDocs;

	private HashMap<Integer, HashMap<Integer, Double>> P;
	
	/**
	 * HashMap containing the exact pagerank scores. Maps from
	 * docname (the same numbers found in the links file) to score.
	 */
	private HashMap<Integer, Double> exactPageRanks;
	
	/**
	 * The docnames (the same numbers found in the links file) of
	 * the 30 best exactly scored documents.
	 */
	private Set<Integer> bestDocs = new HashSet<Integer>();
	
	/**
	 * The docnames (the same numbers found in the links file) of
	 * the 30 worst exactly scored documents.
	 */
	private Set<Integer> worstDocs = new HashSet<Integer>();

	/**
	 * Inner class to represent a document and its score.
	 */
	class RankedDoc implements Comparable<RankedDoc>{
		String docName;
		double pageRankScore;

		RankedDoc(String docName, double pageRankScore){
			this.docName = docName;
			this.pageRankScore = pageRankScore;
		}

		@Override
		public int compareTo(RankedDoc rd) {
			return Double.compare(rd.pageRankScore, pageRankScore);
		}

		@Override
		public String toString(){
			return docName + ": " + pageRankScore;
		}
	}


	public PageRank( String filename ) {
		//Set some of constants
		numberOfDocs = readDocs( filename );
		readExactPageRanks("pagerank_exact_results.txt"); //Needed for goodness measures...
		sink_value = 1/(double)numberOfDocs;
		no_link_value = BORED/(double)numberOfDocs;
		P = createPMatrix();
		
		//Compute the page rank
		double[] score = powerIteration();
		//powerIteration();
		//mc1();
		//mc2();
		//mc3();
		//mc4();
		//mc5();
		
		goodnessMeasure1(score);
		goodnessMeasure2(score);

	}

	/**
	 * Computes the exact pagerank scores using power iteration.
	 * @param noOfDocs
	 */
	private double[] powerIteration(){
		long time = System.currentTimeMillis();
		//Compute the pagerank score with power iteration
		double[] pageRanking = computePagerank();

		time = (System.currentTimeMillis() - time)/1000;

		printRanking(pageRanking, 30);

		System.err.println("PageRanking time: " + (time/60) + " minutes and " + (time%60) + " seconds");
		return pageRanking;
	}

	/**
	 * MonteCarlo simulation algorithm 1.
	 * MC end-point with random start.
	 * 
	 * Start N random walks at random documents. Scores are
	 * incremented when a walk ends on a document.
	 */
	private double[] mc1(){
		double[] score = new double[numberOfDocs];
		Random rand = new Random(System.currentTimeMillis());

		double probToStop = BORED;
		int N = numberOfDocs * 1000;
		
		//Compute the scores
		for (int i = 0; i < N; i++){
			int currentDoc = rand.nextInt(numberOfDocs); //Start at a random document

			//Simulate the walk, stop with a certain probability
			while(rand.nextDouble() > probToStop){
				currentDoc = nextDocInWalk(currentDoc, rand);
			}
			//The walk has ended, increment the score for the last document
			score[currentDoc]++;
		}

		//Normalize the scores.
		for(int i = 0; i < numberOfDocs; i++){
			score[i] /= (double)N;
		}
		System.out.println("Finished ranking using N = " + N);
		printRanking(score, 30);
		
		return score;
	}

	/**
	 * MonteCarlo simulation algorithm 2.
	 * MC end-point with cyclic start.
	 * 
	 * Start walks from every document m times. Scores are
	 * incremented when a walk ends on a document.
	 */
	private double[] mc2(){
		double[] score = new double[numberOfDocs];
		Random rand = new Random(System.currentTimeMillis());

		double probToStop = BORED;
		
		int m = 1000; //The number of times a walk should be started from each document.
		int N = m * numberOfDocs;
		//Compute the scores
		for (int i = 0; i < numberOfDocs; i++){
			for (int j = 0; j < m; j++){
				int currentDoc = i; //Start at each document m times

				//Simulate the walk, stop with a certain probability
				while(rand.nextDouble() > probToStop){
					currentDoc = nextDocInWalk(currentDoc, rand);
				}
				//The walk has ended, increment the score for the last document
				score[currentDoc]++;
			}
		}

		//Normalize the scores.
		for(int i = 0; i < numberOfDocs; i++){
			score[i] /= (double)numberOfDocs * m; //divide by number of walks
		}
		
		System.out.println("Finished ranking using N = " + N + " m = " + m);
		printRanking(score, 30);
		return score;
	}

	/**
	 * MonteCarlo simulation algorithm 3.
	 * MC complete path with cyclic start.
	 * 
	 * Start walks from every document m times. Scores are
	 * incremented for every visit to a document during the
	 * walk.
	 */
	private double[] mc3(){
		double[] score = new double[numberOfDocs];
		Random rand = new Random(System.currentTimeMillis());

		double probToStop = BORED;
		int m = 1000; //The number of times a walk should be started from each document.
		int N = m * numberOfDocs;
		int numPoints = 0;

		//Compute the scores
		for (int i = 0; i < numberOfDocs; i++){
			for (int j = 0; j < m; j++){
				int currentDoc = i; //Start at each document m times

				//Simulate the walk, stop with a certain probability
				while(rand.nextDouble() > probToStop){
					currentDoc = nextDocInWalk(currentDoc, rand);
					score[currentDoc]++; //This document has been visited, increase the score.
					numPoints++; //Keep track of the number of points that have been given.
				}
			}
		}

		//Normalize the scores.
		for(int i = 0; i < numberOfDocs; i++){
			score[i] /= (double)numPoints; //divide by number of points given
		}
		
		System.out.println("Finished ranking using N = " + N + " m = " + m);
		printRanking(score, 30);
		return score;
	}

	/**
	 * MonteCarlo simulation algorithm 4.
	 * MC complete path stopping at dangling nodes.
	 * 
	 * Start walks from every document m times. Scores are
	 * incremented for every visit to a document during the
	 * walk. However, the walk always stops at sinks.
	 */
	private double[] mc4(){
		double[] score = new double[numberOfDocs];
		Random rand = new Random(System.currentTimeMillis());

		double probToStop = BORED;
		int m = 1000; //The number of times a walk should be started from each document.
		int N = numberOfDocs * m;
		
		int numPoints = 0;
		//Compute the scores
		for (int i = 0; i < numberOfDocs; i++){
			for (int j = 0; j < m; j++){
				int currentDoc = i; //Start at each document m times

				//Simulate the walk, stop with a certain probability
				while(rand.nextDouble() > probToStop){
					currentDoc = nextDocInWalk(currentDoc, rand);
					score[currentDoc]++; //This document has been visited, increase the score.
					numPoints++; //Keep track of the number of points that have been given.
					if(link.get(currentDoc) == null) break; //If this is a sink, stop the walk.
				}
			}
		}

		//Normalize the scores.
		for(int i = 0; i < numberOfDocs; i++){
			score[i] /= (double)numPoints; //divide by number of points given
		}
		
		System.out.println("Finished ranking using N = " + N + " m = " + m);
		printRanking(score, 30);
		return score;
	}

	/**
	 * MonteCarlo simulation algorithm 5.
	 * MC complete path with random start.
	 * 
	 * Start N walks at random documents. Scores are incremented
	 * when a document is visited during the walk, however the walk
	 * stops when a sink is reached.
	 */
	private double[] mc5(){
		double[] score = new double[numberOfDocs];
		Random rand = new Random(System.currentTimeMillis());

		double probToStop = BORED;
		int N = numberOfDocs * 1000;
		int numPoints = 0;

		//Compute the scores
		for (int i = 0; i < N; i++){
			int currentDoc = rand.nextInt(numberOfDocs); //Start at a random document

			//Simulate the walk, stop with a certain probability
			while(rand.nextDouble() > probToStop){
				currentDoc = nextDocInWalk(currentDoc, rand);
				score[currentDoc]++; //This document has been visited, increase the score.
				numPoints++; //Keep track of the number of points that have been given.
				if(link.get(currentDoc) == null) break; //If this is a sink, stop the walk.
			}
		}

		//Normalize the scores.
		for(int i = 0; i < numberOfDocs; i++){
			score[i] /= (double)numPoints; //divide by number of points given
		}
		System.out.println("Finished ranking using N = " + N);
		printRanking(score, 30);
		return score;
	}
	
	/**
	 * Prints the sum of squared differences between the exact pageranks
	 * (found in Task 2.4) and the MC-estimated pageranks for the 30
	 * documents with highest exact pagerank in linksDavis.txt.
	 */
	private void goodnessMeasure1(double[] score){
		double diffSum = 0;
		Iterator<Integer> it = bestDocs.iterator();
		int doc = -1;
		while(it.hasNext()){
			doc = it.next();
			diffSum += Math.abs(score[doc]-exactPageRanks.get(doc));
		}
		System.out.println("Goodness measure1: " + diffSum);
	}
	
	/**
	 * Prints the sum of squared differences between the exact pageranks
	 * (found in Task 2.4) and the MC-estimated pageranks for the 30
	 * documents with lowest exact pagerank in linksDavis.txt.
	 */
	private void goodnessMeasure2(double[] score){
		double diffSum = 0;
		Iterator<Integer> it = worstDocs.iterator();
		int doc = -1;
		while(it.hasNext()){
			doc = it.next();
			diffSum += Math.abs(score[doc]-exactPageRanks.get(doc));
		}
		System.out.println("Goodness measure2: " + diffSum);
	}

	/**
	 * Computes the next document in a random walk. With probability 1-BORED
	 * the walks next document is a uniformly random out-link from the current
	 * document. With a probability BORED the next document is a uniformly 
	 * random document from the whole document collection.
	 * 
	 * @param currentDoc The document to move from.
	 * @param rand Random generator.
	 * @return The next document in a random walk.
	 */
	private int nextDocInWalk(int currentDoc, Random rand){
		int nextDoc;

		if(rand.nextDouble() > BORED){
			HashMap<Integer, Boolean> currentLinks = link.get(currentDoc);
			//If there are no out-links pick a random document.
			if(currentLinks == null){
				nextDoc = rand.nextInt(numberOfDocs);
			} else {//If there are out-links, pick one of them with uniform probability.
				Object[] toLinks = currentLinks.keySet().toArray();
				nextDoc = (Integer)toLinks[rand.nextInt(toLinks.length)];
			}
		} else {
			nextDoc = rand.nextInt(numberOfDocs);
		}

		return nextDoc;
	}

	/**
	 * Prints the top scores of the parameterized pageRanking.
	 * 
	 * @param pageRanking 		An array containing the page rank for the documents using
	 * 					  		the internal indices.
	 * @param noOfDocsToPrint	The number of prints wanted.
	 */
	private void printRanking(double[] pageRanking, int noOfDocsToPrint){
		PriorityQueue<RankedDoc> pq = new PriorityQueue<RankedDoc>();
		for (int i = pageRanking.length-1; i >= 0; i--){
			pq.add(new RankedDoc("" + docName[i], pageRanking[i]));
		}

		for(int i = 0; i < pageRanking.length && i < noOfDocsToPrint; i++){
			System.out.println(pq.poll());
		}
	}

	/**
	 *   Reads the documents and creates the docs table. When this method 
	 *   finishes executing then the @code{out} vector of outlinks is 
	 *   initialised for each doc, and the @code{p} matrix is filled with
	 *   zeroes (that indicate direct links) and NO_LINK (if there is no
	 *   direct link. <p>
	 *
	 *   @return the number of documents read.
	 */
	private int readDocs( String filename ) {
		int fileIndex = 0;
		try {
			System.err.print( "Reading file... " );
			BufferedReader in = new BufferedReader( new FileReader( filename ));
			String line;
			while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
				int index = line.indexOf( ";" );
				String title = line.substring( 0, index );
				Integer fromdoc = docNumber.get( title );
				//  Have we seen this document before?
				if ( fromdoc == null ) {	
					// This is a previously unseen doc, so add it to the table.
					fromdoc = fileIndex++;
					docNumber.put( title, fromdoc );
					docName[fromdoc] = title;
				}
				// Check all outlinks.
				StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
				while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
					String otherTitle = tok.nextToken();
					Integer otherDoc = docNumber.get( otherTitle );
					if ( otherDoc == null ) {
						// This is a previousy unseen doc, so add it to the table.
						otherDoc = fileIndex++;
						docNumber.put( otherTitle, otherDoc );
						docName[otherDoc] = otherTitle;
					}
					// Set the probability to 0 for now, to indicate that there is
					// a link from fromdoc to otherDoc.
					if ( link.get(fromdoc) == null ) {
						link.put(fromdoc, new HashMap<Integer,Boolean>());
					}
					if ( link.get(fromdoc).get(otherDoc) == null ) {
						link.get(fromdoc).put( otherDoc, true );
						out[fromdoc]++;
					}
				}
			}
			if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
				System.err.print( "stopped reading since documents table is full. " );
			}
			else {
				System.err.print( "done. " );
			}
			// Compute the number of sinks.
			for ( int i=0; i<fileIndex; i++ ) {
				if ( out[i] == 0 )
					numberOfSinks++;
			}
			in.close();
		}
		catch ( FileNotFoundException e ) {
			System.err.println( "File " + filename + " not found!" );
		}
		catch ( IOException e ) {
			System.err.println( "Error reading file " + filename );
		}
		System.err.println( "Read " + fileIndex + " number of documents" );
		return fileIndex;
	}
	
	/**
	 * Reads the exactly pre-computed pageranks from a file.
	 * The results are stored in the HashMap exactPageRanks and the
	 * 30 best and worst scored documents are stored in bestDocs and
	 * worstDocs respectively.
	 * @param fileName The file containing the exact pageranks.
	 */
	private void readExactPageRanks(String filename){
		exactPageRanks = new HashMap<Integer, Double>();
		try {
			System.err.print( "Reading pre-computed pageranks... " );
			BufferedReader in = new BufferedReader( new FileReader( filename ));
			String line;
			while ((line = in.readLine()) != null) {
				//Fetch the document name and its score.
				int index = line.indexOf( ":" );
				Integer title = new Integer(line.substring( 0, index ));
				Double rank = new Double(line.substring(index+1, line.length()));
				
				//Store the score
				exactPageRanks.put(title, rank);
				
				//Update the best and worst score sets
				addBestDoc(title);
				addWorstDoc(title);
			}
			System.err.println("done!");
			in.close();
		}
		catch ( FileNotFoundException e ) {
			System.err.println( "File " + filename + " not found!" );
		}
		catch ( IOException e ) {
			System.err.println( "Error reading file " + filename );
		}
	}
	
	/**
	 * If docName has a better score than the worst document in the 
	 * bestDocs set, remove the worst doc and insert docName.
	 * @param docName
	 */
	private void addBestDoc(int docName){
		Double score = exactPageRanks.get(docName);
		if(score == null){
			System.out.println("ERROR WHEN CHECKING BEST SCORE, SCORE==NULL");
		}
		if(bestDocs.size() < 30){
			bestDocs.add(docName);
		} else {
			Iterator<Integer> it = bestDocs.iterator();
			double minScore = Double.POSITIVE_INFINITY, tempScore;
			int minScoreDoc = -1, tempDoc;
			while(it.hasNext()){
				tempDoc = it.next();
				tempScore = exactPageRanks.get(tempDoc); 
				if(tempScore < minScore){
					minScore = tempScore;
					minScoreDoc = tempDoc;
				}
			}
			bestDocs.remove(minScoreDoc);
			bestDocs.add(docName);
		}
	}
	
	/**
	 * If docName has a worse score than the best document in the 
	 * worstDocs set, remove the best doc and insert docName.
	 * @param docName
	 */
	private void addWorstDoc(int docName){
		Double score = exactPageRanks.get(docName);
		if(score == null){
			System.out.println("ERROR WHEN CHECKING WORST SCORE, SCORE==NULL");
		}
		if(worstDocs.size() < 30){
			worstDocs.add(docName);
		} else {
			Iterator<Integer> it = worstDocs.iterator();
			double maxScore = Double.NEGATIVE_INFINITY, tempScore;
			int maxScoreDoc = -1, tempDoc;
			while(it.hasNext()){
				tempDoc = it.next();
				tempScore = exactPageRanks.get(tempDoc); 
				if(tempScore > maxScore){
					maxScore = tempScore;
					maxScoreDoc = tempDoc;
				}
			}
			worstDocs.remove(maxScoreDoc);
			worstDocs.add(docName);
		}
	}

	/**
	 *   Computes the pagerank of each document.
	 *   Returns the probability vector representing the probability
	 *   of a random surfer being at each document.
	 */
	private double[] computePagerank() {
		double[] stateProbs = new double[numberOfDocs];
		stateProbs[0] = 1;
		double[] newStateProbs;

		int numIterations = 0;
		boolean continueWithCalc = true;
		while (numIterations < MAX_NUMBER_OF_ITERATIONS && continueWithCalc){
			long time = System.currentTimeMillis();

			//Perform the multiplication
			newStateProbs = vectorTimesMatrix(stateProbs, P);

			//check difference
			double diff = Double.NEGATIVE_INFINITY, tempDiff = diff;
			for(int j = 0; j < stateProbs.length; j++){
				//System.out.printf("nsp[%d]:%.2f ", j, newStateProbs[j]);
				tempDiff = Math.abs(stateProbs[j] - newStateProbs[j]);
				if(tempDiff > diff) diff = tempDiff;
			}

			stateProbs = Arrays.copyOf(newStateProbs, newStateProbs.length);
			numIterations++;
			time = (System.currentTimeMillis()-time)/1000;
			System.err.println("Finished iteration " + numIterations + " in " + time + " sec with maxdiff = " + diff);

			//If no difference bigger than the threshold was found we can end the loop.
			if(diff <= EPSILON)continueWithCalc = false;
		}

		return stateProbs;
	}

	/**
	 * Creates a sparse representation of the P matrix from the link (adjacency) matrix.
	 * All values for sinks and non existing links are not included in the matrix.
	 */
	private HashMap<Integer, HashMap<Integer, Double>> createPMatrix(){
		HashMap<Integer, HashMap<Integer, Double>> P = new HashMap<Integer, HashMap<Integer, Double>>();
		Iterator<Integer> it = link.keySet().iterator();

		while(it.hasNext()){
			Integer fromDoc = it.next();
			HashMap<Integer, Double> toNodes = new HashMap<Integer, Double>();
			Iterator<Integer> it2 = link.get(fromDoc).keySet().iterator();
			double numOutLinks = out[fromDoc];//link.get(fromDoc).size();
			double prob = (1/numOutLinks) * (1-BORED) + BORED/(double)numberOfDocs;
			while(it2.hasNext()){
				Integer toNode = it2.next();
				toNodes.put(toNode, prob);
			}
			P.put(fromDoc, toNodes);
		}
		return P;
	}

	/**
	 * This method multiplies the parameterized vector with the parameterized
	 * matrix and returns the resulting vector.
	 */
	public double[] vectorTimesMatrix(double[] vector, HashMap<Integer, HashMap<Integer, Double>> matrix){
		double[] res = new double[vector.length];

		double value, vectorvalue, matrixvalue;
		HashMap<Integer, Double> fromRow;
		for(int i = 0; i < vector.length; i++){
			value = 0;
			for (int j = 0; j < vector.length; j++){
				vectorvalue = vector[j];
				// matrix value = matrix[j][i]
				matrixvalue = sink_value; //The case when we are in a sink document
				fromRow = matrix.get(j);
				if(fromRow != null){
					matrixvalue = no_link_value; //The case when there is no link from j to i
					Double mval = fromRow.get(i);
					if(mval != null) matrixvalue = mval; //The case when a link from j to i exists
				}
				value += vectorvalue * matrixvalue;
			}
			res[i] = value;
		}
		return res;
	}

	public static void main( String[] args ) {
		if ( args.length != 1 ) {
			System.err.println( "Please give the name of the link file" );
		}
		else {
			new PageRank( args[0] );
		}
	}
}
