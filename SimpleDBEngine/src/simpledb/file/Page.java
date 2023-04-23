package simpledb.file;

import java.nio.ByteBuffer;
import java.nio.charset.*;

public class Page {
   private ByteBuffer bb;
   public static Charset CHARSET = StandardCharsets.US_ASCII;

   // For creating data buffers
   public Page(int blocksize) {
      bb = ByteBuffer.allocateDirect(blocksize);
   }
   
   // For creating log pages
   public Page(byte[] b) {
      bb = ByteBuffer.wrap(b);
   }

   public int getInt(int offset) {
      return bb.getInt(offset);
   }

   public void setInt(int offset, int n) {
	   // Every integer is 4 bytes so offset + 4 must be lower than capacity or equals to capacity
	   if(offset + 4 > bb.capacity()) {
		   System.out.println("ERROR: The integer "+ n +" does not fit at location " + offset+ " of the page.");
	   }
	   else {
		   bb.putInt(offset, n);
	   }
   }

   public byte[] getBytes(int offset) {
      bb.position(offset);
      int length = bb.getInt();
      byte[] b = new byte[length];
      bb.get(b);
      return b;
   }

   public void setBytes(int offset, byte[] b) {
	   // It puts bytes length in int so we add 4 bytes then it put the byte content so we add b.length
	   if(offset + 4 + b.length > bb.capacity()) {
		   System.out.println("ERROR: The byte length "+ b.length +" does not fit at location " + offset + " of the page.");
	   }
	   else {
      bb.position(offset);
      bb.putInt(b.length);
      bb.put(b);
	   }
   }
   
   public String getString(int offset) {
	    bb.position(offset);
	    StringBuilder sb = new StringBuilder();
	    char c;
	    while ((c = bb.getChar()) != '\0') {
	        sb.append(c);
	    }
	    return sb.toString();
   }

   public void setString(int offset, String s) {
	   if(offset + ( (s.length() * 2) + 2 ) > bb.capacity()) {
	        System.out.println("ERROR: The string "+ s +" does not fit at location " + offset + " of the page.");
	    }
	   else {
		   byte[] b = new byte[s.length() * 2 + 2];
		    for (int i = 0; i < s.length(); i++) {
		    	bb.putChar(offset + i * 2, s.charAt(i));
		    }
		    bb.putChar((offset + s.length() * 2),  '\0'); // add null character at end
	   }
   }

   public static int maxLength(int strlen) {
	  // We know that every character takes two bytes since we use putChar and getChar
      int bytesPerChar = 2;
      // We add plus two for '/0' character
      return (strlen * (int)bytesPerChar) + 2;
   }

   // a package private method, needed by FileMgr
   ByteBuffer contents() {
      bb.position(0);
      return bb;
   }
}
