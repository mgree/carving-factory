package com.labgmail.pomona.greenberg.cnccarvingfactory;

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

    public float x, y, z, time;

    public Anchor(float x, float y, float z, float time) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.time = time;
    }


    public String toString(){
        return new String("Anchor Point at (" + x + ", " + y + ", " + z + ") at time " + time);
    }


    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeFloat(x);
        out.writeFloat(y);
        out.writeFloat(z);
        out.writeFloat(time);
        out.flush();
    }

    private void readObject(ObjectInputStream in) throws IOException {
        x = in.readFloat();
        y = in.readFloat();
        z = in.readFloat();
        time = in.readFloat();
    }

    @SuppressWarnings({"EmptyMethod", "RedundantThrows", "unused"})
    private void readObjectNoData() throws ObjectStreamException { }

    public static final long serialVersionUID = 42L;

}
