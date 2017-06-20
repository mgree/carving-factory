package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.graphics.Path;
import android.graphics.PointF;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Custom anchor point class that allows 3 dimensions and time to be saved
 *
 * Created by soniagrunwald on 5/25/17.
 */

@SuppressWarnings("WeakerAccess")
public class Anchor  {

    float x, y, z, time;

    public Anchor(float x, float y, float z, float time) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.time = time;
    }

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }

    public float getZ(){
        return z;
    }

    public float getTime(){
        return time;
    }

    public void setX(float newX) {
        x = newX;
    }

    public void setY(float newY) {
        y = newY;
    }

    public void setZ(float newZ) {
        z = newZ;
    }

    public void setTime(float newTime) {
        time = newTime;
    }

    public void set(float newX, float newY, float newZ, float newT){
        x = newX;
        y = newY;
        z = newZ;
        time = newT;
    }


}
