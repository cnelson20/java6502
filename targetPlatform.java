import java.util.*;
import java.io.*;

public class targetPlatform {
  public char[] header;
  public int startAddr;
  public String name;
  public int min_size;

  public targetPlatform(char[] h, int a, String name) {
    header = new char[h.length];
    for (int i = 0; i < h.length; i++) {
      header[i] = h[i];
    }
    startAddr = a;
  }
  private static char[] getHeaderData(String s) {
    char[] r = new char[(s.length()+1)/3];
    for (int i = 0; i < s.length(); i += 3) {
      r[i/3] = (char)Integer.parseInt(s.substring(i,i+2),16);
    }
    return r;
  }
  public static targetPlatform[] getAllPlatforms() {
    targetPlatform[] platforms;
    ArrayList<String> lines = new ArrayList();
    try {
      Scanner scan = new Scanner(new File("platforms.txt"));
      while (scan.hasNextLine()) {
        lines.add(scan.nextLine());
      }
    } catch (FileNotFoundException e) {
      System.out.println("Cannot open \"platforms.txt\"");
      System.exit(-1);
    }
    platforms = new targetPlatform[lines.size()];
    String[] temp;
    for (int i = 0; i < platforms.length; i++) {
      temp = lines.get(i).split(" ",0);
      try {
        if (temp[2].charAt(0) == '_' && temp[2].charAt(1) == '_') {
          throw new ArrayIndexOutOfBoundsException();
        }
        platforms[i] = new targetPlatform(getHeaderData(temp[2]),Assembler.parseNumber(temp[1]),temp[0]);
      } catch (ArrayIndexOutOfBoundsException e) {
        platforms[i] = new targetPlatform(new char[0],Assembler.parseNumber(temp[1]),temp[0]);
      }
      if (temp[temp.length-1].length() >= 2 && temp[temp.length-1].substring(0,2).equals("__")) {
        platforms[i].min_size = Integer.parseInt(temp[temp.length-1].substring(2));
      }
    }
    return platforms;
  }
}
