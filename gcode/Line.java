package GCodeReader2;

import java.lang.Object;
import java.util.*;


public class Line {

  private double[] theLine;

  public Line (double startX, double startY, double endX, double endY){
    theLine = new double[] {startX, startY, endX, endY};
  }

  public double getStartX() {
    return theLine[0];
  }

  public double getStartY() {
    return theLine[1];
  }

  public double getEndX() {
    return theLine[2];
  }

  public double getEndY() {
    return theLine[3];
  }

  public String toString() {
    return "(" + theLine[0] + ", " + theLine[1] + ") ("
      + theLine[2] + ", " + theLine[3] + ")";
  }


}
