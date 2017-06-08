/*
 * Line Class -- creates objects of type Line which hold the information needed
 * to draw an line in swing or using gcode.
 *
 * @author Sonia Grunwald
 * @version June 7th, 2017
 * Michael Greenberg Lab
 *
 */
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

/* Returns the x value of the starting point */
  public double getStartX() {
    return startX;
  }

  /* Returns the y value of the starting point */
  public double getStartY() {
    return startY;
  }

  /* Returns the x value of the ending point */
  public double getEndX() {
    return endX;
  }

  /* Returns the y value of the ending point */
  public double getEndY() {
    return endY;
  }

  /* Returns the line width the line should be drawn at */
  public double getLineWidth(){
    return lineWidth;
  }

  /* Returns the a gcode representation of the line using G1 */
  public String toGCode(){
    return "G1 X" + endX + " Y" + endY + " Z0.0\n";
  }

  /* Returns a string representation of the line */
  public String toString() {
    return "(" + startX + ", " + startY + ") ("
      + endX + ", " + endY + ") at width " + lineWidth;
  }


}
