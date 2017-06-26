package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.graphics.Color;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Objects;


/**
 * Custom anchor point class that allows three dimensions and time to be saved
 *
 * Created by soniagrunwald on 5/25/17.
 */

@SuppressWarnings("WeakerAccess")
public class Anchor  implements Serializable{

    //TODO check if these not being final is ok
    //if not deal with the readObject method which wants to read in things and set them
    public float x, y, z;
    public long time;

    public Anchor(float x, float y, float z, long time) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.time = time;
    }

    /* return a string representation of the anchor */
    public String toString(){
        return new String("Anchor (" + x + ", " + y + ", " + z + ") at time " + time);
    }

    /* Return the alpha value (given the z) */
    public int getAlpha() {
        return Math.round(255 * Math.max(Math.min(z, 1.0f), 0.0f));
    }

    /* .equals method which determines equality based on the time stamp */
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Anchor)) {
            return false;
        }
        Anchor a = (Anchor) o;
        return (a.time == time);
    }

    /* Hash code implementation for the .equals method */
    public int hashCode() {
        return Objects.hash(x, y, z, time);
    }

    /* Distance to method (from an anchor to a pair of points) */
    public double distance2D(float x, float y) {
        return Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.y - y, 2));
    }

    /* Write object in specific order */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeFloat(x);
        out.writeFloat(y);
        out.writeFloat(z);
        out.writeLong(time);
        out.flush();
    }

    /* Read object in the same order */
    private void readObject(ObjectInputStream in) throws IOException {
        x = in.readFloat();
        y = in.readFloat();
        z = in.readFloat();
        time = in.readLong();
    }

    /* Etc things so that it is serializable */
    @SuppressWarnings({"EmptyMethod", "RedundantThrows", "unused"})
    private void readObjectNoData() throws ObjectStreamException { }

    public static final long serialVersionUID = 42L;
}
