import java.util.HashMap;
import java.util.Iterator;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public abstract class OnFileIndex implements Index{

	public final String INDEX_DIRECTORY = "./index";
	public final String INDEX_FILE = INDEX_DIRECTORY + "/index.index";
	private HashMap<Integer, String> termIDs = new HashMap<>(); //termID mapped to token
	private int nextTermID = 0;
	private RandomAccessFile raf = null;

	/**
	 * Creates the index directory and file if they do not exist.
	 */
	public OnFileIndex(){
		//Create directory and file...
		try {
			raf = new RandomAccessFile(INDEX_FILE, "rw");
			createDirectory(INDEX_DIRECTORY, true);
			createFile(INDEX_FILE, true);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private boolean createDirectory(String path, boolean debugPrints){		
		boolean ret = false;
		File dir = new File(path);
		if(dir.exists() && dir.isDirectory()){
			ret = true;
			if(debugPrints) System.err.println("Directory already exists"); System.out.println("Path: " + dir.getAbsolutePath());
		} else{
			ret = dir.mkdir();
			if(debugPrints){
				if(ret){
					System.err.println("Directory did not exist and was created successfully"); System.out.println("Path: " + dir.getAbsolutePath());
				} else System.err.println("Directory did not exist, failed to create it");
			}
		}
		return ret;
	}

	private boolean createFile(String path, boolean debugPrints) throws IOException{
		boolean ret = false;
		File file = new File(path);
		ret = file.createNewFile();
		if(ret){
			System.err.println("File was created successfully");
		} else System.err.println("Failed to create file, it already existed");
		return ret;
	}
	/**
	 * Problem: Sortera listan.
	 * 			Just nu skriver jag över varje gång jag skriver, jag vill skriva emellan (som vanligt i dokument...)
	 */
	@Override
	public void insert(String token, int docID, int offset) {
		if(!termIDs.containsValue(token)){//A new token...
			//System.out.println("Inside the insert-method, a new token");
			termIDs.put(nextTermID++, token);
			try {
				raf.seek(raf.length());
				byte[] b = (token + " (docid: " + docID + " offset: " + offset + ")\n").getBytes();
				raf.write(b);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else{//Not a new token
			try {
				//Find the correct line
				raf.seek(0);
				String tempString = raf.readLine().split(" ")[0];
				while(!token.equals(tempString)){
					tempString = raf.readLine().split(" ")[0];
				}
				raf.seek(raf.getFilePointer()-tempString.length()-1);//Backup to the beginning of the line.
				//First check if the token has been seen in this document
				int indexOfDocID = tempString.indexOf("(docid: " + docID);
				if(indexOfDocID == -1){//this is a new docID
					raf.seek(raf.getFilePointer()+tempString.length());
					raf.writeBytes(" (docid: " + docID + " offset: " + offset + ")");
				} else {//this is not a new docID, find it and add the offset...
					int lengthToSeek = tempString.indexOf(")", indexOfDocID);
					raf.seek(raf.getFilePointer() + lengthToSeek);
					raf.writeBytes(" " + offset);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public Iterator<String> getDictionary() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PostingsList getPostings(String token) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PostingsList search(Query query, int queryType, int rankingType, int structureType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void cleanup() {
		try {
			raf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}