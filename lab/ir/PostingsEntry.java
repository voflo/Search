/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */  

//package ir;

import java.io.Serializable; 
import java.util.LinkedList;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {
    
    public int docID;
    public double score = -1; //if no score is set, it is automatically set to -1.
    public LinkedList<Integer> offsets = new LinkedList<>(); //Stores the positions of where the token occurs in the document.

    public PostingsEntry(){};
    
    public PostingsEntry(int docID){
    	this.docID = docID;
    }
    
    /**
     *  PostingsEntries are compared by their score (only relevant 
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in 
     *  descending order.
     */
    public int compareTo( PostingsEntry other ) {
	return Double.compare( other.score, score );
    }
    
    /**
     * Returns a shallow copy of this PostingsEntry
     */
	public PostingsEntry clone(){
    	PostingsEntry ret = new PostingsEntry();
    	ret.docID = new Integer(docID);
    	ret.score = new Double(score);
    	ret.offsets = (LinkedList<Integer>) offsets.clone();
    	
    	return ret;
    }
	
	/**
	 * Adds an offset to the end of the offsets-list.
	 * Should be greater than the previous ones in the list.
	 */
	public void addOffset(int offset){
		offsets.addLast(offset);
	}
}

    
