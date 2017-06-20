import java.lang.System;
import java.util.*;
import java.io.*;
import java.lang.*;


/*
 * CurvePointsList
 * This class literally only exists because I wanted to make my own iterator
 * This is an arraylist of doubles (eventually I should expand this to any type)
 * with a custom iterator
 */
public class CurvePointsList {

private final CurvePointsIterator curveItr;
private List<Double> points;

public CurvePointsList() {
        curveItr = new CurvePointsIterator();
        points = new ArrayList<Double>();
}

public CurvePointsList(List<Double> list) {
        curveItr = new CurvePointsIterator();
        points = list;
}

public CurvePointsIterator iterator() {
        return curveItr;
}

public List<Double> getPoints(){
        return points;
}

public boolean add(Double d){
        return points.add(d);
}

public void add(int index, Double d){
        points.add(index,d);
}

public Double get(int i){
        return points.get(i);
}

public Double set (int i, Double d){
        return points.set(i, d);
}

public Double[] toArray(){
        return points.toArray(new Double[points.size()]);
}

public int size(){
        return points.size();
}






/*
 * Iterator for the CurvePointsList class. Allows you to get the
 * list of every nth element including the first and last points
 */
public class CurvePointsIterator {
private int currPos = 0;

public CurvePointsIterator() {
}

public boolean hasNext() {
        return (currPos < points.size());
}

public Double next() {
        if (!hasNext()) { return 0.0; }
        Double d = points.get(currPos);
        currPos++;
        return d;
}

//Go forward n elements and return the value
//If you reach the end, return that value instead
public Double nextNth(int n) {
        //skip the ones you don't care about
        for (int i = 0; i < (n-1); i++) {
                //if you can skip forward, do so
                if(hasNext()) {
                        next();
                }
                //Otherwise just return the last point
                else {
                        return points.get(currPos-1);
                }
        }
        //if you successfully skipped the unwanted values, return the next val
        if (hasNext()) {
                return next();
        }
        //if there is no next value, return the last value
        else {
                return points.get(currPos-1);
        }
}


/*
 * This returns the list of every nth element including the first and last items
 */
public CurvePointsList getNthList(int n){
        currPos = 0;
        CurvePointsList ans = new CurvePointsList();

        if(points.size() < 1) { return ans;}

        ans.add(next());
        while (hasNext()) {
                ans.add(nextNth(n));
        }
        return ans;
}

}

}
