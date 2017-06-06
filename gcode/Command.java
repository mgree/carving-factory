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

  public Character getType() {
    return type;
  }

  public int getMode() {
    return mode;
  }

  public void setType(Character c) {
    type = c;
  }

  public void setMode(int i) {
    mode = i;
  }

  public String toString() {
    return "Command: " + type + mode;
  }

}
