/*
 * Arc Class -- creates objects of type Arc which hold the information needed
 * to draw an arc in swing or using gcode. This arc class is built for
 * compatibility with gcode. The constructor is overloaded so that one can
 * either create an arc from two points and a radius or two points and the center
 * of the circle. This class takes those inputs and finds all the necessary
 * information you need to draw it in swing.
 *
 * @author Sonia Grunwald
 * @version June 7th, 2017
 * Michael Greenberg Lab
 *
 */

import java.io.*;
import java.lang.*;
import java.util.*;


public class Arc {

double r, x0, y0, x1, y1, lineWidth; //inputs

double d;         //distance between points
double m1;        //the midpoint x
double m2;        //the midpoint y
double u;         //the normal
double v;         //the normal
double h;         //height (distance to center)
int e = 1;        //counterclockwise pt 1 to pt2 is 1
                  //clockwise pt 1 to pt 2 is -1
double a, b;      //the x and y of the center
int rectx, recty; //bounding box top left corner
double startAngle, endAngle, deltaAngle; //the calculated angles

double[] drawingInfo;

//CONSTRUCTOR WITH RADIUS AND TWO POINTS
public Arc (double r, double x0, double y0, double x1, double y1, int e, double lineWidth){
        this.r = r;
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.e = e;
        this.lineWidth = lineWidth;

        //calculate the distance between them
        d = Math.sqrt(Math.pow((x1-x0), 2) + Math.pow((y1-y0), 2));

        //find the midpoint
        m1 = (x0+x1)/2;
        m2 = (y0 + y1)/2;

        //find the normal
        u = (x1-x0)/d;
        v = (y1 - y0)/d;

        //find the height
        h = Math.sqrt(Math.pow(r,2) - (Math.pow(d,2)/4));

        //get the center
        a = m1- e*h*v;
        b = m2 + e*h*u;

        //find rectangle dimensions
        rectx =  (int)Math.round(a - r);
        recty = (int)Math.round(b - r);

        //calculate angles
        startAngle = (180/Math.PI * Math.atan2(b-y0, x0-a));
        endAngle = (180/Math.PI * Math.atan2(b-y1, x1-a));
        deltaAngle = endAngle - startAngle;

        drawingInfo = new double[] {rectx, recty, 2*r, 2*r, (int)Math.round(startAngle), (int)Math.round(deltaAngle)};

}

//CONSTRUCTOR WITH CENTER AND TWO POINTS
public Arc (double a, double b, double x0, double y0, double x1, double y1, int e, double lineWidth){
        this.a = a;
        this.b = b;
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.e = e;
        this.lineWidth = lineWidth;

        //calculate the distance between them
        r = Math.sqrt(Math.pow((x1-a), 2) + Math.pow((y1-a), 2));

        //find the midpoint
        m1 = (x0+x1)/2;
        m2 = (y0 + y1)/2;

        //find rectangle dimensions
        rectx =  (int)Math.round(a - r);
        recty = (int)Math.round(b - r);

        //calculate angles
        startAngle = (180/Math.PI * Math.atan2(b-y0, x0-a));
        endAngle = (180/Math.PI * Math.atan2(b-y1, x1-a));
        deltaAngle = endAngle - startAngle;

        drawingInfo = new double[] {rectx, recty, 2*r, 2*r, (int)Math.round(startAngle), (int)Math.round(deltaAngle)};
}


/* Returns the inverse of the arc (the remainder of the circle the arc is a section of)
To get the reverse of the arc, swap the start and end points and subtract 360 from the deltaAngle */
public void inverseArc(){
        double xtemp = x0;
        double ytemp = y0;
        x0 = x1;
        y0 = y1;
        x1 = xtemp;
        y1 = ytemp;

        //recalculate the angles
        startAngle = (180/Math.PI * Math.atan2(b-y0, x0-a));
        endAngle = (180/Math.PI * Math.atan2(b-y1, x1-a));
        deltaAngle = endAngle - startAngle;

        //update the drawingInfo
        drawingInfo[4] = (int)Math.round(startAngle);
        drawingInfo[5] = (int)Math.round(deltaAngle) - 360;
}


/* Returns an array of doubles with the information needed to draw a swing arc*/
public double[] getDrawingInfo(){
        return drawingInfo;
}

/* Returns the x coordinate of the center of the circle */
public double getCenterX() {
        return a;
}

/* Returns the y coordinate of the center of the circle */
public double getCenterY(){
        return b;
}

/* Returns the radius of the circle */
public double getRadius(){
        return r;
}

/* Returns the angle the first point is at relative to the positive x axis
    moving counterclockwise */
public double getStartAngle(){
        return startAngle;
}

/* Returns the angle the final point is at relative to the positive x axis
    moving counterclockwise */
public double getEndAngle(){
        return endAngle;
}

/* Returns the line width for that arc */
public double getLineWidth(){
        return lineWidth;
}

/* Creates a string representation of the arc */
public String toString(){
        String things =  "Arc:\n" + "Rectangle: (" + rectx + ", " + recty + ", " +
                        2*r + ", " + 2*r + ", " + ") \nStart Angle: " + startAngle +
                        "\nEnd Angle: " + endAngle + "\nChange in angles: " + deltaAngle +
                        "\nCenter: (" + a + ", " + b +")";
        return things;
}

}
