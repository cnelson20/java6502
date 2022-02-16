import java.io.*;
import java.util.*;

public class Assembler {
  private final static targetPlatform cbm = new targetPlatform(new char[] {0x01, 0x08, 0x0B, 0x08, 0x30, 0x03, 0x9E, 0x32, 0x30, 0x36, 0x31, 0x00, 0x00, 0x00},2061,"CBM");
  
  /*
    parses a String representing a number in whatever base is implied
    ("%111") => 7
    ("$FF") => 255
    ("100") => 100
  */ 
  public static int parseNumber(String s) {
    int i = 0;
    while (s.charAt(i) == ' ') {i++;}
    if (s.charAt(i) == '%') {
      return Integer.parseInt(s.substring(i+1),2);
    } else if (s.charAt(i) == '$') {
      return Integer.parseInt(s.substring(i+1),16);
    } else {
      return Integer.parseInt(s,10);
    }
  }

  /* 
    Returns a string representing the characters in string as bytes
    ex. ("Hello") => "$48 $65 $6c $6c $6f"
  */
  private static String hexBytes(String s) {
    String r = "";
    for (int i = 0; i < s.length(); i++) {
      r += "$" + Integer.toHexString(s.charAt(i)) + " ";
    }
    return r;
  }

  /*
    Given a hex value in s starting at index, return the number the hex represents typecasted to a char 
    ex. ("$41" , 1) => 'A'
  */
  private static char getCharFromString(String s, int index) {
    return (char)Integer.parseInt(s.substring(index,index+2),16);
  }
  // Returns if a character is a letter A-Z, a-z
  private static boolean isLetter(char c) {
    return ((c >= 0x41 && c <= 0x5A) || (c >= 0x61 && c <= 0x7A));
  }

  /*
    Calls parseTHHelper with the correct limit  
  */
  private static void parseT(ArrayList<Character> output, String s, HashMap<String,Integer> labels, String lastLabel) {
    //System.out.println("parseT(): s='" + s + "'");
    while (s.charAt(0) == ' ') {s = s.substring(1);}
    if (s.charAt(0) == '_') {
      switch (s.substring(0,6)) {
          case "__UFF_":
            int outputSize = output.size();
            parseTHelper(255,output,s.substring(6),labels,lastLabel);
            while (output.size() > outputSize + 1) {
              output.remove(output.size() - 1);
            }
            break;
          case "__REL_":
            break;
          default:
            System.out.println("Assembler Error! This shouldn't happen.");
            System.exit(1);
            break;
      }
    } else {
      parseTHelper(65535,output,s,labels,lastLabel);
    }
  }
  /* 
    Given limit (limit a label can be, mostly for absolute lds)
    output (output to write to)
    s, a label
    labels, a hashmap of labels
    and lastLabel, the last non-@ label
  */
  private static void parseTHelper(int limit, ArrayList<Character> output, String s, HashMap<String,Integer> labels, String lastLabel) {
    while (s.charAt(s.length() - 1) == ' ') {
      s = s.substring(0,s.length()-1);
    }
    if (s.charAt(0) == '<') {
      parseTHelper(limit,output,s.substring(1),labels,lastLabel);
      if (limit > 256) {output.remove(output.size() - 1);}
    } else if (s.charAt(0) == '>') {
      parseTHelper(limit,output,s.substring(1),labels,lastLabel);
      output.remove(output.size() - 2);
    } else if (isLetter(s.charAt(0)) || s.charAt(0) == '@') {
      int temp = parseLabel(s,labels,lastLabel);
      output.add((char)(temp % 256));
      output.add((char)((temp % 65536) / 256));
    } else {
      char[] toadd;
      toadd = parseNT(s,0,limit);
      output.add(toadd[0]);
      output.add(toadd[1]);
    }
  }
  /* Get value of a label */
  private static int parseLabel(String s, HashMap<String,Integer> labels, String lastLabel) {
    if (s.charAt(0) == '@') {s = lastLabel + "_" + s;}
    //System.out.println(labels);
    s = s.replaceAll("\\s+","");
    //System.out.println("S: '" + s + '\'');
    return labels.get(s);
  }

  /* 
    Returns if char c could be a number
  */
  private static boolean isNumberChar(char c, boolean isFirstCharacter) {
    if (c >= '0' && c <= '9') {
      return true;
    }
    if (isFirstCharacter) {
      if (c == '$' || c == '%') {
        return true;
      }
    } else {
      if ((c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f')) {
        return true;
      }
    }
    return false;
  }

  /* 
    returns true if c = A-Z, a-z, @ or _
  */
  private static boolean isLetterExtended(char c) {
    return isLetter(c) || c == '_' || c == '@';
  }

  /* 
    does operations with statement
    First splits s into tokens, then operates on them.
  */
  public static int parseStatement(String s, HashMap<String,Integer> labels, String lastLabel) {
    ArrayList<String> parts = new ArrayList();
    char type = isLetterExtended(s.charAt(0)) ? 'l' : (isNumberChar(s.charAt(0), true) ? '0' : '+');
    String currentString = (type == 'l') ? "L_" : ((type == '0') ? "N_" : "O_");
    currentString += s.charAt(0);
    for (int i = 1; i < s.length(); i++) {
      if (type == 'l') {
        if (isLetterExtended(s.charAt(i))) {
          currentString += s.charAt(i);
        } else {
          parts.add(currentString);
          if (s.charAt(i) == ' ') {
            i++;
            if (i >= s.length()) {currentString = ""; break;}
          }
          type = isLetterExtended(s.charAt(i)) ? 'l' : (isNumberChar(s.charAt(i), true) ? '0' : '+');  
          currentString = (type == 'l') ? "L_" : ((type == '0') ? "N_" : "O_");
          currentString += s.charAt(i) + "";
        }
      } else if (type == '0') {
        if (isNumberChar(s.charAt(i), false)) {
          currentString += s.charAt(i);
        } else {
          parts.add(currentString);
          if (s.charAt(i) == ' ') {
            i++;
            if (i >= s.length()) {currentString = ""; break;}
          }
          type = isLetterExtended(s.charAt(i)) ? 'l' : (isNumberChar(s.charAt(i), true) ? '0' : '+');  
          currentString = (type == 'l') ? "L_" : ((type == '0') ? "N_" : "O_");
          currentString += s.charAt(i) + "";
        }
      } else /* type == '+'' */ {
        if (s.charAt(i) == '+' || s.charAt(i) == '-' || s.charAt(i) == '*' || s.charAt(i) == '/') {
          currentString += s.charAt(i);
        } else {
          parts.add(currentString);
          if (s.charAt(i) == ' ') {
            i++;
            if (i >= s.length()) {currentString = ""; break;}
          }
          type = isLetterExtended(s.charAt(i)) ? 'l' : (isNumberChar(s.charAt(i), true) ? '0' : '+');  
          currentString = (type == 'l') ? "L_" : ((type == '0') ? "N_" : "O_");
          currentString += s.charAt(i) + "";
        }
      }
    }
    if (currentString.length() != 0) {
      parts.add(currentString);
    }
    int[] array = new int[parts.size()];
    for (int i = 0; i < parts.size(); i++) {
      if (parts.get(i).charAt(0) == 'L') {
        //System.out.println(parts.get(i).substring(2));
        array[i] = parseLabel(parts.get(i).substring(2), labels, lastLabel);
      } else if (parts.get(i).charAt(0) == 'N') {
        array[i] = parseNumber(parts.get(i).substring(2));
      } else {
        array[i] = -1000;
      }
    }
    for (int i = 1; i < parts.size() - 1; i++) {
      if (array[i] == -1000) {
        switch (parts.get(i).charAt(2)) {
          case '+':
            array[i] = array[i-1] + array[i+1];
            array[i] = 0;
            break;
          case '-':
            array[i] = array[i-1] - array[i+1];
            array[i] = 0;
            break;
          case '*':
            array[i] = array[i-1] * array[i+1];
            array[i] = 0;
            break;
          case '/':
            array[i] = array[i-1] / array[i+1];
            array[i] = 0;
            break;
          }
      }
    }
    int sum = 0;
    for (int i = 0; i < parts.size(); i++) {
      sum += array[i];
    }
    /*
    System.out.println("parseStatement(" + s + ") => " + sum);
    System.out.println(Arrays.toString(array));
    */
    return sum;
  }

  /*
  Parses a String s representing a number, and bounds check it using min and max.
  Returns the number s is as bytes in a char[]. 
  */
  private static char[] parseNT(String s,int min, int max) {
    char[] r = new char[2];
    if (max < 0) {max = Integer.MAX_VALUE;}
    int i = parseNumber(s);
    if (i > max || i < min) {
      System.out.println("Error: Number \"" + s + "\" out of bounds for range [" + min + ","  + max + "]");
      System.exit(1);
    }
    r[0] = (char)(i % 256);
    r[1] = (char)((i % 65536) / 256);
    return r;
  }

  /*
    split filename's content into lines
    Returns an ArrayList of the lines 
  */
  private static ArrayList<String> getLines(String check, String filename) {
    ArrayList<String> lines = new ArrayList();
    Scanner scan = null;
    if (check.equals(filename)) {
      System.out.println("Error: A file cannot include itself");
      System.exit(1);
    }
    try {
      scan = new Scanner(new File(filename));
    } catch (FileNotFoundException e) {
      System.out.println("File " + filename + " not found.");
      System.exit(1);
    }
    while(scan.hasNextLine()) {
      String line = scan.nextLine().toLowerCase();
      int i = line.indexOf(';');
      if (i >= 0) {line = line.substring(0,i);}
      while (line.length() > 0 && (line.charAt(0) == '\t' || line.charAt(0) == ' ')) {
        line = line.substring(1);
      }
      lines.add(line);
    }
    return lines;
  }

  /* 
    main
  */
  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage:\n\tjava Assembler filename -options");
      System.exit(1);
    }
    String input = args[0];
    String outputName = input.substring(0,input.lastIndexOf(".")) + ".prg";
    boolean verbose = false;
    targetPlatform[] all_platforms = new targetPlatform[0]; // targetPlatform.getAllPlatforms();
    targetPlatform platform = cbm; // all_platforms[0];
    boolean plFd;
    // Parse Flags
    for (int i = 2; i <= args.length; i++) {
      switch(args[i-1]) {
        case "-o":
          outputName = args[i];
          break;
        case "-t":
          plFd = false;
          for (targetPlatform p : all_platforms) {
            if (p.name.equals(args[i])) {
              platform = p;
              plFd = true;
              break;
            }
          }
          if (!plFd) {
            System.out.println("Platform \"" + args[i] + "\" does not exist");
            System.exit(1);
          }
          break;
        case "-verbose":
          verbose = true;
          break;
        case "-h":
        case "-help":
          System.out.println("Options:\n");
          System.exit(1);
        default:
          break;
      }
    }
    ArrayList<String> lines = new ArrayList();
    lines = getLines("",input);
    lines.add("prgm_end:");
    ArrayList<Integer> opSizes = new ArrayList();
    ArrayList<String> tempCode = new ArrayList();
    ArrayList<String> forMerge;
    ArrayList<String> forMergeB;
    int temp;
    String lastGlobalLabel = null;
    for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
      String line = lines.get(lineNum);
      int i = line.indexOf(';');
      if (line.length() == 0) {continue;}
      // Leave assembler instructions for later
      if (line.charAt(0) == '.') {
        if (line.length() >= 8 && line.substring(1,8).equals("include")) {
          forMerge = new ArrayList(lines.subList(0,lineNum));
          forMergeB = getLines(input,line.substring(line.indexOf('"')+1,line.lastIndexOf('"')));
          forMerge.add(forMergeB.get(0)); // Not sure at all why this is necessary
          forMerge.addAll(forMergeB);
          forMerge.addAll(lines.subList(1+lineNum,lines.size()));
          lines = forMerge;
          i--;
        } else {
          opSizes.add(0);
          tempCode.add(line);
        }
      } else {
        //System.out.println("Instruction: '" + line + "'");
        while (line.length() < 4) {
          line = line + " ";
        }
        switch (line.substring(0,4)) {
          case "adc ":
            temp = line.indexOf(' ');
            if (line.charAt(temp + 1) == '#') {
              tempCode.add("69 __UFF_" + line.substring(temp + 2));
              opSizes.add(2);
            } else {
              if (line.indexOf('(') != -1) {
                opSizes.add(2);
                if (line.indexOf('x') != -1) {
                  tempCode.add("61 __UFF_" +line.substring(line.indexOf('(')+1,line.indexOf(',')));
                } else {
                  tempCode.add("71 __UFF_" +line.substring(line.indexOf('(')+1,line.indexOf(')')));
                }
              } else {
                opSizes.add(3);
                if (line.indexOf(',') != -1) {
                  tempCode.add((line.indexOf('x') != 1 ? "7D " : "79 ") + line.substring(temp+1,line.indexOf(',')));
                } else {
                  tempCode.add("6D" + line.substring(temp));
                }
              }
            }
            break;
          case "sbc ":
            temp = line.indexOf(' ');
            if (line.charAt(temp + 1) == '#') {
              tempCode.add("29 __UFF_" + line.substring(temp + 2));
              opSizes.add(2);
            } else {
              if (line.indexOf('(') != -1) {
                opSizes.add(2);
                if (line.indexOf('x') != -1) {
                  tempCode.add("21 __UFF_" +line.substring(line.indexOf('(')+1,line.indexOf(',')));
                } else {
                  tempCode.add("31 __UFF_" +line.substring(line.indexOf('(')+1,line.indexOf(')')));
                }
              } else {
                opSizes.add(3);
                if (line.indexOf(',') != -1) {
                  tempCode.add((line.indexOf('x') != 1 ? "3D " : "39 ") + line.substring(temp+1,line.indexOf(',')));
                } else {
                  tempCode.add("2D" + line.substring(temp));
                }
              }
            }
            break;
          case "ora ":
            temp = line.indexOf(' ');
            if (line.charAt(temp + 1) == '#') {
              tempCode.add("09 __UFF_" + line.substring(temp + 2));
              opSizes.add(2);
            } else {
              if (line.indexOf('(') != -1) {
                opSizes.add(2);
                if (line.indexOf('x') != -1) {
                  tempCode.add("01 __UFF_" +line.substring(line.indexOf('(')+1,line.indexOf(',')));
                } else {
                  tempCode.add("11 __UFF_" +line.substring(line.indexOf('(')+1,line.indexOf(')')));
                }
              } else {
                opSizes.add(3);
                if (line.indexOf(',') != -1) {
                  tempCode.add((line.indexOf('x') != 1 ? "1D " : "19 ") + line.substring(temp+1,line.indexOf(',')));
                } else {
                  tempCode.add("0D" + line.substring(temp));
                }
              }
            }
            break;
          case "and ":
            temp = line.indexOf(' ');
            if (line.charAt(temp + 1) == '#') {
              tempCode.add("29 __UFF_" + line.substring(temp + 2));
              opSizes.add(2);
            } else {
              if (line.indexOf('(') != -1) {
                opSizes.add(2);
                if (line.indexOf('x') != -1) {
                  tempCode.add("21 __UFF_" +line.substring(line.indexOf('(')+1,line.indexOf(',')));
                } else {
                  tempCode.add("31 __UFF_" +line.substring(line.indexOf('(')+1,line.indexOf(')')));
                }
              } else {
                opSizes.add(3);
                if (line.indexOf(',') != -1) {
                  tempCode.add((line.indexOf('x') != 1 ? "3D " : "39 ") + line.substring(temp+1,line.indexOf(',')));
                } else {
                  tempCode.add("2D" + line.substring(temp));
                }
              }
            }
            break;
          case "eor ":
            temp = line.indexOf(' ');
            if (line.charAt(temp + 1) == '#') {
              tempCode.add("49 __UFF_" + line.substring(temp + 2));
              opSizes.add(2);
            } else {
              if (line.indexOf('(') != -1) {
                opSizes.add(2);
                if (line.indexOf('x') != -1) {
                  tempCode.add("41 __UFF_" +line.substring(line.indexOf('(')+1,line.indexOf(',')));
                } else {
                  tempCode.add("51 __UFF_" +line.substring(line.indexOf('(')+1,line.indexOf(')')));
                }
              } else {
                opSizes.add(3);
                if (line.indexOf(',') != -1) {
                  tempCode.add((line.indexOf('x') != 1 ? "5D " : "59 ") + line.substring(temp+1,line.indexOf(',')));
                } else {
                  tempCode.add("4D" + line.substring(temp));
                }
              }
            }
            break;
          case "bit ":
            tempCode.add("2C" + line.substring(line.indexOf(' ')));
            opSizes.add(3);
            break;

          case "bcc ":
            opSizes.add(2);
            tempCode.add("90 __REL_" + line.substring(line.indexOf(' ')+1));
            break;
          case "bcs ":
            opSizes.add(2);
            tempCode.add("B0 __REL_" + line.substring(line.indexOf(' ')+1));
            break;
          case "beq ":
            opSizes.add(2);
            tempCode.add("F0 __REL_" + line.substring(line.indexOf(' ')+1));
            break;
          case "bne ":
            opSizes.add(2);
            tempCode.add("D0 __REL_" + line.substring(line.indexOf(' ')+1));
            break;
          case "bmi  ":
            opSizes.add(2);
            tempCode.add("30 __REL_" + line.substring(line.indexOf(' ')+1));
            break;
          case "bpl ":
            opSizes.add(2);
            tempCode.add("10 __REL_" + line.substring(line.indexOf(' ')+1));
            break;
          case "bvc ":
            opSizes.add(2);
            tempCode.add("50 __REL_" + line.substring(line.indexOf(' ')+1));
            break;
          case "bvs ":
            opSizes.add(2);
            tempCode.add("70 __REL_" + line.substring(line.indexOf(' ')+1));
            break;

          case "brk ":
            opSizes.add(1);
            tempCode.add("00");
            break;

          case "clc ":
            opSizes.add(1);
            tempCode.add("18");
            break;
          case "cld ":
            opSizes.add(1);
            tempCode.add("D8");
            break;
          case "cli ":
            opSizes.add(1);
            tempCode.add("58");
            break;
          case "clv ":
            opSizes.add(1);
            tempCode.add("B8");
            break;
          case "sec ":
            opSizes.add(1);
            tempCode.add("38");
            break;
          case "sed ":
            opSizes.add(1);
            tempCode.add("F8");
            break;
          case "sei ":
            opSizes.add(1);
            tempCode.add("78");
            break;

          case "cmp ":
            temp = line.indexOf(' ');
            if (line.charAt(temp + 1) == '#') {
              tempCode.add("C9 __UFF_" + line.substring(temp + 2));
              opSizes.add(2);
            } else {
              if (line.indexOf('(') != -1) {
                opSizes.add(2);
                if (line.indexOf('x') != -1) {
                  tempCode.add("C1 __UFF_" +line.substring(line.indexOf('(')+1,line.indexOf(',')));
                } else {
                  tempCode.add("D1 __UFF_" +line.substring(line.indexOf('(')+1,line.indexOf(')')));
                }
              } else {
                opSizes.add(3);
                if (line.indexOf(',') != -1) {
                  tempCode.add((line.indexOf('x') != 1 ? "DD " : "D9 ") + line.substring(temp+1,line.indexOf(',')));
                } else {
                  tempCode.add("CD" + line.substring(temp));
                }
              }
            }
            break;
          case "cpx ":
            temp = line.indexOf(' ');
            if (line.charAt(temp + 1) == '#') {
              tempCode.add("E0 __UFF_" + line.substring(temp + 2));
              opSizes.add(2);
            } else {
              tempCode.add("EC" + line.substring(line.indexOf(' ')));
              opSizes.add(3);
            }
            break;
          case "cpy ":
            temp = line.indexOf(' ');
            if (line.charAt(temp + 1) == '#') {
              tempCode.add("C0 __UFF_" + line.substring(temp + 2));
              opSizes.add(2);
            } else {
              tempCode.add("CC" + line.substring(temp));
              opSizes.add(3);
            }
            break;
          case "dec ":
            if (line.length() <= 4 || line.charAt(line.indexOf(' ') + 1) == 'a') {
              // DEC A 65c02 only opcode
              tempCode.add("3A");
              opSizes.add(1);
            } else {
              opSizes.add(3);
              if (line.indexOf('x') != -1) {
                tempCode.add("DE" + line.substring(line.indexOf(' '),line.indexOf(',')));
              } else {
                tempCode.add("CE" + line.substring(line.indexOf(' ')));
              }
            }
            break;
          case "inc ":
            if (line.length() <= 4 || line.charAt(line.indexOf(' ') + 1) == 'a') {
              // INC A 65c02 only opcode
              tempCode.add("1A");
              opSizes.add(1);
            } else {
              opSizes.add(3);
              if (line.indexOf('x') != -1) {
                tempCode.add("FE" + line.substring(line.indexOf(' '),line.indexOf(',')));
              } else {
                tempCode.add("EE" + line.substring(line.indexOf(' ')));
              }
            }
            break;
          case "jmp ":
            opSizes.add(3);
            if (line.indexOf('(') != -1) {
              tempCode.add("6C " + line.substring(line.indexOf('(') + 1,line.indexOf(')')));
            } else {
              tempCode.add("4C" + line.substring(line.indexOf(' ')));
            }
            break;
          case "jsr ":
            tempCode.add("20 " + line.substring(line.indexOf(' ') + 1));
            opSizes.add(3);
            break;
          case "nop ":
            opSizes.add(1);
            tempCode.add("EA");
            break;

          case "lda ":
            temp = line.indexOf(' ');
            if (line.charAt(temp + 1) == '#') {
              tempCode.add("A9 __UFF_" + line.substring(temp + 2));
              opSizes.add(2);
            } else {
              if (line.indexOf('(') != -1) {
                opSizes.add(2);
                if (line.indexOf('x') != -1) {
                  tempCode.add("A1 __UFF_" + line.substring(line.indexOf('(')+1,line.indexOf(',')));
                } else {
                  tempCode.add("B1 __UFF_" + line.substring(line.indexOf('(')+1,line.indexOf(')')));
                }
              } else {
                opSizes.add(3);
                if (line.indexOf(',') != -1) {
                  tempCode.add((line.indexOf('x') != 1 ? "BD " : "B9 ") + line.substring(temp+1,line.indexOf(',')));
                } else {
                  tempCode.add("AD" + line.substring(temp));
                }
              }
            }
            break;
          case "ldx ":
            temp = line.indexOf(' ');
            if (line.charAt(temp + 1) == '#') {
              tempCode.add("A2 __UFF_" + line.substring(temp + 2));
              opSizes.add(2);
            } else {
              opSizes.add(3);
              if (line.indexOf(',') != -1) {
                tempCode.add("BE" + line.substring(temp,line.indexOf(',')));
              } else {
                tempCode.add("AE" + line.substring(temp));
              }
            }
            break;
          case "ldy ":
            temp = line.indexOf(' ');
            if (line.charAt(temp + 1) == '#') {
              tempCode.add("A0 __UFF_" + line.substring(temp + 2));
              opSizes.add(2);
            } else {
              opSizes.add(3);
              if (line.indexOf(',') != -1) {
                tempCode.add("BC" + line.substring(temp,line.indexOf(',')));
              } else {
                tempCode.add("AC" + line.substring(temp));
              }
            }
            break;
          case "asl ":
            if (line.length() <= 4 || line.charAt(line.indexOf(' ') + 1) == 'a') {
              tempCode.add("0A");
              opSizes.add(1);
            } else {
              opSizes.add(3);
              if (line.indexOf('x') != -1) {
                tempCode.add("1E" + line.substring(line.indexOf(' '),line.indexOf(',')));
              } else {
                tempCode.add("0E" + line.substring(line.indexOf(' ')));
              }
            }
            break;
          case "lsr ":
            if (line.length() <= 4 || line.charAt(line.indexOf(' ') + 1) == 'a') {
              tempCode.add("4A");
              opSizes.add(1);
            } else {
              opSizes.add(3);
              if (line.indexOf('x') != -1) {
                tempCode.add("5E" + line.substring(line.indexOf(' '),line.indexOf(',')));
              } else {
                tempCode.add("4E" + line.substring(line.indexOf(' ')));
              }
            }
            break;
          case "rol ":
            if (line.length() <= 4 || line.charAt(line.indexOf(' ') + 1) == 'a') {
              tempCode.add("2A");
              opSizes.add(1);
            } else {
              opSizes.add(3);
              if (line.indexOf('x') != -1) {
                tempCode.add("3E" + line.substring(line.indexOf(' '),line.indexOf(',')));
              } else {
                tempCode.add("2E" + line.substring(line.indexOf(' ')));
              }
            }
            break;
          case "ror ":
            if (line.length() <= 4 || line.charAt(line.indexOf(' ') + 1) == 'a') {
              tempCode.add("6A");
              opSizes.add(1);
            } else {
              opSizes.add(3);
              if (line.indexOf('x') != -1) {
                tempCode.add("7E" + line.substring(line.indexOf(' '),line.indexOf(',')));
              } else {
                tempCode.add("6E" + line.substring(line.indexOf(' ')));
              }
            }
            break;
          case "sta ":
            temp = line.indexOf(' ');
            if (line.indexOf('(') != -1) {
              opSizes.add(2);
              if (line.indexOf('x') != -1) {
                tempCode.add("81 __UFF_" +line.substring(line.indexOf('(')+1,line.indexOf(',')));
              } else {
                tempCode.add("91 __UFF_" +line.substring(line.indexOf('(')+1,line.indexOf(')')));
              }
            } else {
              opSizes.add(3);
              if (line.indexOf(',') != -1) {
                tempCode.add((line.indexOf('x') != 1 ? "9D " : "99 ") + line.substring(temp+1,line.indexOf(',')));
              } else {
                tempCode.add("8D" + line.substring(temp));
              }
            }
            break;
          case "stx ":
            if (line.indexOf(',') != -1) {
              tempCode.add("96 __UFF_" + line.substring(line.indexOf(' ')+1,line.indexOf(',')));
              opSizes.add(2);
            } else {
              tempCode.add("8E " + line.substring(line.indexOf(' ')+1));
              opSizes.add(3);
            }
            break;
          case "sty ":
            if (line.indexOf(',') != -1) {
              tempCode.add("94 __UFF_" + line.substring(line.indexOf(' ')+1,line.indexOf(',')));
              opSizes.add(2);
            } else {
              tempCode.add("8C " + line.substring(line.indexOf(' ')+1));
              opSizes.add(3);
            }
            break;

          case "txs ":
            opSizes.add(1);
            tempCode.add("9A");
            break;
          case "tsx ":
            opSizes.add(1);
            tempCode.add("BA");
            break;
          case "pha ":
            opSizes.add(1);
            tempCode.add("48");
            break;
          case "php ":
            opSizes.add(1);
            tempCode.add("08");
            break;
          case "pla ":
            opSizes.add(1);
            tempCode.add("68");
            break;
          case "plp ":
            opSizes.add(1);
            tempCode.add("28");
            break;

          case "rti ":
            opSizes.add(1);
            tempCode.add("40");
            break;
          case "rts ":
            opSizes.add(1);
            tempCode.add("60");
            break;

          case "tax ":
            opSizes.add(1);
            tempCode.add("AA");
            break;
          case "txa ":
            opSizes.add(1);
            tempCode.add("8A");
            break;
          case "dex ":
            opSizes.add(1);
            tempCode.add("CA");
            break;
          case "inx ":
            opSizes.add(1);
            tempCode.add("E8");
            break;
          case "tay ":
            opSizes.add(1);
            tempCode.add("A8");
            break;
          case "tya ":
            opSizes.add(1);
            tempCode.add("98");
            break;
          case "dey ":
            opSizes.add(1);
            tempCode.add("88");
            break;
          case "iny ":
            opSizes.add(1);
            tempCode.add("C8");
            break;

          default:
            if (line.indexOf(':') != -1) {
              // Code with labels //
              opSizes.add(0);
              if (line.charAt(0) == '@') {
                if (lastGlobalLabel == null) {
                  System.out.println(input + "(" + (lineNum + 1) + "): Error: Local labels are not allowed before the first global label");
                  System.exit(1);
                } else {
                  tempCode.add("__LBL_" + line.substring(0,line.lastIndexOf(':')));
                }
              } else {
                lastGlobalLabel = line.substring(0,line.lastIndexOf(':'));
                tempCode.add("__LBL_" + lastGlobalLabel);
              }
            } else if (line.indexOf('=') != -1) {
              opSizes.add(0);
              tempCode.add("__LBB_" + line.replaceAll("\\s+",""));
            } else {
              System.out.println(input + "(" + (lineNum + 1) + "): Error: Incorrect opcode or other error");
              System.exit(1);
            }
            break;
        }
      }
    }
    // Calculate the value of labels
    String[] tempStringArray;
    String tempString;
    char[] bytesToAdd;
    int forORG;
    String forRES;
    String forORGString;
    lastGlobalLabel = null;
    HashMap<String,Integer> labels = new HashMap();
    int PrgmCounter = platform.startAddr;
    for (int i = 0; i < tempCode.size(); i++) {
      String tempLine = tempCode.get(i);
      if (tempLine.length() >= 5 && tempLine.substring(0,5).equals("__LBL")) {
        if (tempLine.charAt(6) == '@') {
          labels.put(lastGlobalLabel + '_' + tempLine.substring(6),PrgmCounter);
        } else {
          lastGlobalLabel = tempLine.substring(6);
          labels.put(lastGlobalLabel,PrgmCounter);
        }
      } else if (tempLine.length() >= 5 && tempLine.substring(0,5).equals("__LBB")) {
        String val = tempLine.substring(6);
        //System.out.println(val);
        labels.put(val.substring(0, val.indexOf('=')), parseStatement(val.substring(val.indexOf('=') + 1), labels, lastGlobalLabel));
      }
      //System.out.println(tempLine);
      if (tempLine.charAt(0) == '.') {
        switch(tempLine.split(" ")[0]) {
          case ".byte":
          case ".db":
            if (tempLine.indexOf('"') != -1) {
              tempString = tempLine.substring(tempLine.indexOf('"')+1,tempLine.lastIndexOf('"'));
              tempCode.set(i,"_DAT " + hexBytes(tempString));
              opSizes.set(i,tempString.length());
            } else {
              tempStringArray = tempLine.substring(tempLine.indexOf(' ') + 1).split(", ");
              tempString = "";
              for (int t = 0; t < tempStringArray.length; t++) {
                if (parseNumber(tempStringArray[t]) < 256 && parseNumber(tempStringArray[t]) >= 0) {
                  tempString += "$" + (parseNumber(tempStringArray[t]) > 15 ? "" : "0") + Integer.toHexString(parseNumber(tempStringArray[t])) + " ";
                } else {
                  System.out.println("Number " + parseNumber(tempStringArray[t]) + " out of bounds of range [0,255]");
                }
              }
              tempCode.set(i,"_DAT " + tempString);
              opSizes.set(i,tempStringArray.length);
            }
            break;
          case ".word":
          case ".dw":
			if (tempLine.indexOf('"') != -1) {
              System.out.println("Error: .word does not accept string literals.");
			  System.exit(1);
            } else {
              tempStringArray = tempLine.substring(tempLine.indexOf(' ') + 1).split(", ");
              tempString = "";
              for (int t = 0; t < tempStringArray.length; t++) {
                if (parseNumber(tempStringArray[t]) < 65536 && parseNumber(tempStringArray[t]) >= 0) {
                  tempString += "$" + (parseNumber(tempStringArray[t]) > 15 ? "" : "0") + Integer.toHexString(parseNumber(tempStringArray[t])) + " ";
                } else {
                  System.out.println("Number " + parseNumber(tempStringArray[t]) + " out of bounds of range [0,255]");
                }
              }
              tempCode.set(i,"_DAT " + tempString);
              opSizes.set(i,tempStringArray.length);
            }
            break;
          case ".define":
            labels.put(tempLine.split(" ")[1],parseNumber(tempLine.split(" ")[2]));
            break;
          case ".incbin":
            forORGString = "";
            try {
              FileInputStream incBinStream = new FileInputStream(tempLine.substring(tempLine.indexOf('"')+1,tempLine.lastIndexOf('"')));
              int incBinInt = 0;
              while ((incBinInt = incBinStream.read()) != -1) {
                forORGString += '$' + (incBinInt > 15 ? "" : "0") + Integer.toHexString(incBinInt) + ' ';
              }
            } catch (IOException e) {
              System.out.println("File " + tempLine.substring(tempLine.indexOf('"')+1,tempLine.lastIndexOf('"')) + " not found.");
              System.exit(1);
            }
            tempCode.set(i,"_DAT " + forORGString);
            opSizes.set(i,forORGString.length() / 4);
            break;
          case ".res":
            forORG = parseNumber(tempLine.substring(tempLine.indexOf(',')+1));
            if (forORG < 0 || forORG > 255) {
              System.out.println("Number \"" + forORG + "\" out of range [0,255]");
              System.exit(1);
            }
            forRES = "$" + (forORG > 15 ? "" : "0") + Integer.toHexString(forORG) + " ";
            forORG = parseNumber(tempLine.substring(tempLine.indexOf(' ')+1,tempLine.indexOf(',')));
            forORGString = "_DAT ";
            for (int j = 0; j < forORG; j++) {
              forORGString += forRES;
            }
            tempCode.add(forORGString);
            opSizes.add(forORG - PrgmCounter);
            i++;
            break;
          case ".org":
            forORG = parseNumber(tempLine.substring(tempLine.indexOf(' ')+1));
            if (forORG > PrgmCounter) {
              forORGString = "_DAT ";
              for (int j = PrgmCounter; j < forORG; j++) {
                forORGString += "$FF ";
              }
              tempCode.add(forORGString);
              opSizes.add(forORG - PrgmCounter);
              i++;
              PrgmCounter = forORG;
            } else if (forORG == PrgmCounter) { // do nothing
            } else {
              System.out.println("Error: .org must change location to higher address");
              System.exit(1);
            }
            break;
          default:
            System.out.println("Error: Unknown assembler directive \"" + tempLine.split(" ")[0] + "\"");
            System.exit(1);
        }
      }
      PrgmCounter += opSizes.get(i);
    }
    if (verbose) {
      System.out.println(labels);
    }

    int outputBytes_sizePrev = 0;
    ArrayList<Character> outputBytes = new ArrayList();
    for (int i = 0; i < platform.header.length; i++) {
      outputBytes.add(platform.header[i]);
    }
    lastGlobalLabel = null;
    PrgmCounter = platform.startAddr;
    for (int i = 0; i < tempCode.size(); i++) {
      /* This is so you can print all the bytes from the previous iteration */
      if (verbose && outputBytes_sizePrev < outputBytes.size()) {
        if (outputBytes.size() - outputBytes_sizePrev < 40) {
          if (i == 0) {
            System.out.print("Program Header: ");
          }
          for (int tempFor = outputBytes_sizePrev; tempFor < outputBytes.size(); tempFor++) {
            System.out.print((tempFor == outputBytes_sizePrev ? "[$" : ", $") + Integer.toHexString(outputBytes.get(tempFor)));
          }
          System.out.println("]");
        } else {
          System.out.println("[ Many bytes just added... " + (i == 0 ? "(Likely a program header)" : "(.incbin perhaps)")  + " ]");
        }
      }
      outputBytes_sizePrev = outputBytes.size();

      String line = tempCode.get(i);
      if (verbose) {
        if (line.length() >= 40) {
          System.out.println(line.substring(0,40) + " (CONT)");
        } else {
          System.out.println(line);
        }
      }
      if (line.charAt(0) == '_' || line.charAt(0) == '.' || (line.length() >= 9 && line.substring(3,9).equals("__REL_"))) {
        if (line.substring(0,4).equals("_DAT")) {
          for (String s : line.substring(5).split(" ",0)) {
            outputBytes.add(getCharFromString(s,1));
          }
        } else if (line.substring(0,6).equals("__LBL_")) {
          if (line.indexOf('@') == -1) {
            lastGlobalLabel = line.substring(6);
          }
        } else if (line.substring(3,9).equals("__REL_")) {
          if (isLetter(line.charAt(9)) || line.charAt(9) == '@') {
            /*if (verbose) {
              System.out.println("rel addr:");
              System.out.println("label = '" + line.substring(9) + "'");
              System.out.println("lgl = '" + lastGlobalLabel + "'");
            }*/
            temp = parseLabel(line.substring(9),labels,lastGlobalLabel) - PrgmCounter - 2;
            if (temp < -128 || temp > 127) {
              System.out.println("Error: Branch distance " + temp + " out of range [-128,127]");
              System.exit(1);
            }
            outputBytes.add(getCharFromString(line,0));
            outputBytes.add((char)((byte)temp));
          } else {
            outputBytes.add(getCharFromString(line,0));
            outputBytes.add((char)parseNumber(line.substring(line.lastIndexOf('_') + 1)));
          }
        }
      } else {
        //Actual instructions
        outputBytes.add(getCharFromString(line,0)); // instruction opcode
        // now we figure out the bytes proceding
        //if (verbose) {System.out.println(line);}
        if (line.indexOf(' ') != -1) {          
          if (line.indexOf("__UFF_") != -1 || line.indexOf('<') != -1 || line.indexOf('>') != -1) {
            parseT(outputBytes,line.substring(line.indexOf(' ') + 1),labels,lastGlobalLabel);
          } else {
            int itemp = parseStatement(line.substring(line.indexOf(' ') + 1), labels, lastGlobalLabel);
            parseT(outputBytes, itemp + "", labels, lastGlobalLabel);
          }
        }
      }
      PrgmCounter += opSizes.get(i);
    }
    try {
      FileOutputStream toWrite = null;
      try {
        toWrite = new FileOutputStream(outputName);
      } catch (FileNotFoundException fe) {
        System.out.println("File " + input + " not found.");
        System.exit(1);
      }
      for (int c : outputBytes) {
        toWrite.write(c);
      }
      toWrite.close();
    } catch (IOException e) {
      System.out.println("Cannot write to file \"" + outputName + "\"");
      System.exit(1);
    }
  }
}
