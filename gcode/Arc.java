import java.io.*;
import java.lang.*;
import java.util.*;

/**
   This arc class is built for compatibility with gcode
   radius and two points are the easiest way to input an arc
   in gcode. This class takes those inputs and finds all
   the information you need to draw it in swing


   Constructor is overloaded so that you can either input a radius or a center to draw
 **/

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


//To get the reverse of the arc, swap the start and end points and subtract 360 from the deltaAngle
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


        drawingInfo[4] = (int)Math.round(startAngle);
        drawingInfo[5] = (int)Math.round(deltaAngle) - 360;
}



public double[] getDrawingInfo(){
        return drawingInfo;
}

public double getCenterX() {
        return a;
}

public double getCenterY(){
        return b;
}

public double getRadius(){
        return r;
}

public double getStartAngle(){
        return startAngle;
}

public double getEndAngle(){
        return endAngle;
}

public double getLineWidth(){
        return lineWidth;
}

public String toString(){
        String things =  "Arc:\n" + "Rectangle: (" + rectx + ", " + recty + ", " +
                        2*r + ", " + 2*r + ", " + ") \nStart Angle: " + startAngle +
                        "\nEnd Angle: " + endAngle + "\nChange in angles: " + deltaAngle +
                        "\nCenter: (" + a + ", " + b +")";
        return things;
}


}
