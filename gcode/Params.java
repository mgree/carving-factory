import java.lang.Object;
import java.util.*;


public class Params {
  private Character type;
  private double value;

  public Params (Character t, double v){
    type = t;
    value = v;
  }

  public Character getType(){
    return type;
  }

  public double getValue(){
    return value;
  }

  public String toString() {
    return "(" + type + ", " + value + ")";
  }


}
