/*
 * Command Class -- creates objects of type Command which hold a type and a mode
 * of a gcode command
 *
 * @author Sonia Grunwald
 * @version June 7th, 2017
 * Michael Greenberg Lab
 *
 */

import java.lang.Object;
import java.util.*;


public class Command {
  private Character type; //G or M
  private int mode; //Number (ie for G01, 01 is the mode)
  private int linenum;


  public Command (){
    type = '0';
    mode = 0;
    linenum = 0;
  }

  public Command (Character t, int m, int line){
    type = t;
    mode = m;
    linenum = line;
  }

  /* Returns the type of the command */
  public Character getType() {
    return type;
  }

  /* Returns the mode of the command */
  public int getMode() {
    return mode;
  }

  /* Returns the number of command it it */
  public int getLineNum() {
    return linenum;
  }

  /* Allows you to set the type of the command */
  public void setType(Character c) {
    type = c;
  }

  /* Allows you to set the mode of the command */
  public void setMode(int i) {
    mode = i;
  }

  /* Allows you to set the line number of the command */
  public void setLineNum(int l) {
    linenum = l;
  }

  /* Returns a string representation of the command */
  public String toString() {
    return "Command: " + type + mode;
  }

}
