package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.graphics.Color;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;


/**
 * Custom anchor point class that allows three dimensions and time to be saved
 *
 * Created by soniagrunwald on 5/25/17.
 */

@SuppressWarnings("WeakerAccess")
public class Anchor  implements Serializable{

    public float x, y, z;
    long time;

    public Anchor(float x, float y, float z, long time) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.time = time;
    }

    public String toString(){
        return new String("Anchor Point at (" + x + ", " + y + ", " + z + ") at time " + time);
    }

    public int getAlpha() {
        return Math.round(255 * Math.max(Math.min(z, 1.0f), 0.0f));
    }

    public double distance2D(float x, float y) {
        return Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.y - y, 2));
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeFloat(x);
        out.writeFloat(y);
        out.writeFloat(z);
        out.writeLong(time);
        out.flush();
    }

    private void readObject(ObjectInputStream in) throws IOException {
        x = in.readFloat();
        y = in.readFloat();
        z = in.readFloat();
        time = in.readLong();
    }

    @SuppressWarnings({"EmptyMethod", "RedundantThrows", "unused"})
    private void readObjectNoData() throws ObjectStreamException { }

    public static final long serialVersionUID = 42L;
}
