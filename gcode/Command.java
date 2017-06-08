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


  public Command (){
    type = '0';
    mode = 0;
  }

  public Command (Character t, int m){
    type = t;
    mode = m;
  }

  /* Returns the type of the command */
  public Character getType() {
    return type;
  }

  /* Returns the mode of the command */
  public int getMode() {
    return mode;
  }

  /* Allows you to set the type of the command */
  public void setType(Character c) {
    type = c;
  }

  /* Allows you to set the mode of the command */
  public void setMode(int i) {
    mode = i;
  }

  /* Returns a string representation of the command */
  public String toString() {
    return "Command: " + type + mode;
  }

}
