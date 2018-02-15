/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */  

//package ir;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.io.Serializable;

/**
 *   A list of postings for a given word.
 */
public class PostingsList implements Serializable {

	/** The postings list as a sorted linked list. */
	private List<PostingsEntry> list;
	
	public PostingsList(){
		list = new ArrayList<PostingsEntry>();
	}
	
	public PostingsList(String typeOfList) throws Exception{
		switch (typeOfList) {
		case "ArrayList":
			list = new ArrayList<PostingsEntry>();
			break;
		case "LinkedList":
			list = new LinkedList<PostingsEntry>();
			break;
		default:
			throw new Exception("Wrongly specified type of list: " + typeOfList);
		}
	}

	/**  Number of postings in this list  */
	public int size() {
		return list.size();
	}

	/**  Returns the ith posting */
	public PostingsEntry get( int i ) {
		return list.get( i );
	}

	/** 
	 * Adds a new entry to the PostingsList
	 */
	public void add(int docID, int offset){
		//If the list is empty or this document is newer than the others, create and add an element to the end of the list.
		if(list.isEmpty() || docID > list.get(list.size()-1).docID){
			PostingsEntry pe = new PostingsEntry(docID);
			pe.addOffset(offset);
			list.add(pe);
		} else { //This document is already in the list, we only need to add the new offset to the PostingsEntry
			int index = list.size()-1;
			list.get(index).addOffset(offset);
			/*
			int index = findPostingsEntry(docID);
			if(index > -1) list.get(index).addOffset(offset);
			*/
		}
		/*PostingsEntry pe;
		try{
			pe = list.get(list.size()-1);
			if(pe.docID == docID){
				pe.addOffset(offset);
			} else throw new Exception();
		}catch (Exception e){ //
			pe = new PostingsEntry(docID);
			pe.addOffset(offset);
			list.add(pe);
		}*/
	}
	
	/**
	 * Adds the parameterized PostingsEntry to the end of the list.
	 */
	public void add(PostingsEntry pe){
		list.add(pe);
	}
	
	/**
	 * Adds the parameterized PostingsEntry to the list in a way that 
	 * keeps the list sorted, according to docIDs.
	 */
	public void addInSortedPosition(PostingsEntry pe){
		if(list.isEmpty() || pe.docID > list.get(list.size()-1).docID){
			list.add(pe);
		} else {//we need to find the correct position...
			Iterator<PostingsEntry> it = list.iterator();
			int index = 0;
			while(it.hasNext()){
				if(pe.docID < it.next().docID) {
					list.add(index, pe);
					break;
				}
				index++;
			}
		}
	}
	
	/**
	 * Finds and returns the index of the PostingsEntry with the
	 * parameterized docID. Returns -1 if the docID is not part
	 * of the list.
	 * 
	 * Requires that the list is sorted in ascending order with
	 * respect to the docID:s!
	 * @param docID
	 * @return The index of the PostingsEntry with docID == docID or -1
	 * 			if it cannot be found.
	 */
	public int findPostingsEntry(int docID){
		int ret = -1, min = 0, max = list.size()-1, guess;
		PostingsEntry temp = null;
		while(min <= max){
			guess = (min+max)/2;
			temp = list.get(guess);
			if(docID == temp.docID){
				ret = guess;
				break;
			} else if(docID > temp.docID){
				min = guess + 1;
			} else max = guess - 1;
		}
		return ret;
	}
	
	/**
	 * Sorts the PostingsList with respect to the PostingsEntries
	 * scores in descending order.
	 */
	public void sortList(){
		list.sort(null);
	}
}



