package simpledb.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class PageTest {
	public static void main(String[] args) throws IOException {
	      Page page = new Page(400);
	      // Part a test
	      	// Will print error
	      	Integer tempInt = 12;
	      	page.setInt(398, tempInt);
	      	// Will work as expected
	      	page.setInt(396, tempInt);
	      	System.out.println("It should print: " + tempInt + " | Printed value: " + page.getInt(396));
	      	System.out.println("-------------- Part A finished! --------------");
	      // Part b test
	      	// Will print error
	      	String tempString = "t1";
	      	System.out.println("String length is: " + Page.maxLength(tempString.length()));
	      	page.setString(396, tempString);
	      	// Will work as expected
	      	tempString = "task";
	      	System.out.println("String length is: " + Page.maxLength(tempString.length()));
	      	page.setString(390, tempString);
	      	System.out.println("It should print: " + tempString + " | Printed value: " +page.getString(390));
	      	System.out.println("-------------- Part B finished! --------------");
	   }
}
