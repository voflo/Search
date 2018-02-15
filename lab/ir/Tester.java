import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

public class Tester{
	public static void main(String[] args){
		//OnFileIndex si = new OnFileIndex();
		
		try {
			RandomAccessFile raf = new RandomAccessFile(new File("./../temp.txt"), "rw");
			raf.seek(0);
			raf.writeBytes("lalal");
			raf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}