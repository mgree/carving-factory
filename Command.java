package GCodeReader2;

import java.lang.Object;
import java.util.*;


public class Command {
  private Character type; //G or M
  private int mode; //Number (ie for G01, 01 is the mode)
  private ArrayList<Params> params; //parameters are pairs of type and value


  public Command (){
    type = '0';
    mode = 0;
    params = new ArrayList<Params>();
  }

  public Command (Character t, int m, ArrayList<Params> p){
    type = t;
    mode = m;
    params = p;
  }

  public Character getType() {
    return type;
  }

  public int getMode() {
    return mode;
  }

  public ArrayList<Params> getParams(){
    return params;
  }

  public void setType(Character c) {
    type = c;
  }

  public void setMode(int i) {
    mode = i;
  }

  public void setParams(ArrayList<Params> p){
    params = p;
  }


  public void addParam(Params p){
    params.add(p);
  }

  public String toString() {
    return "";
  }


}
