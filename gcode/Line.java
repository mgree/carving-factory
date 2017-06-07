import java.lang.Object;
import java.util.*;


public class Line {

  private double startX, startY, endX, endY, lineWidth;

  public Line (double startX, double startY, double endX, double endY, double lineWidth){
      this.startX = startX;
      this.startY = startY;
      this.endX = endX;
      this.endY = endY;
      this.lineWidth = lineWidth;

  }

  public double getStartX() {
    return startX;
  }

  public double getStartY() {
    return startY;
  }

  public double getEndX() {
    return endX;
  }

  public double getEndY() {
    return endY;
  }

  public double getLineWidth(){
    return lineWidth;
  }

  public String toGCode(){
    return "G1X" + endX + "Y" + endY + "Z0.0\n";
  }

  public String toString() {
    return "(" + startX + ", " + startY + ") ("
      + endX + ", " + endY + ") at width " + lineWidth;
  }


}
