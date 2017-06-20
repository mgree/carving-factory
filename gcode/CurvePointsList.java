import java.lang.System;
import java.util.*;
import java.io.*;
import java.lang.*;


/*
 * CurvePointsList
 * This class literally only exists because I wanted to make my own iterator
 * This is an arraylist with a custom iterator
 */
public class CurvePointsList<E> {

private List<E> points;

public CurvePointsList() {
        points = new ArrayList<E>();
}

public CurvePointsList(List<E> list) {
        points = (ArrayList<E>)list;
}

public Iterator<E> iterator(int n) {
        return new Iterator<E>() {
          private int currPos = 0;

          public boolean hasNext() {
            return currPos < points.size();
          }

          public E next() {
            E v = null;

            if (currPos < points.size()) {
                v = points.get(currPos);
            }

            if (currPos + n < points.size()) {
              currPos += n;
            } else if (currPos < points.size() - 1) {
              currPos = points.size() - 1;
            } else {
              currPos = points.size(); // which will stop hasNext/next
            }

            return v;
          }
        };
}

public List<E> getPoints(){
        return points;
}

public boolean add(E d){
        return points.add(d);
}

public void add(int index, E d){
        points.add(index,d);
}

public E get(int i){
        return points.get(i);
}

public E set (int i, E d){
        return points.set(i, d);
}


public int size(){
        return points.size();
}

}
