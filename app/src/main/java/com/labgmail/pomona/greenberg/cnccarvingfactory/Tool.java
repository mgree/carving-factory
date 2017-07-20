package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by edinameshietedoho on 6/22/17.
 */

public class Tool implements Serializable, Comparable {

    //TODO ADD RPMS

    //Includes Tool Number, Tool Diameter, Max Cut Depth, Insertion Speed, Lateral Speed, and tool name

    private float diameter, cutDepth, inSpeed, latSpeed;
    private int toolNum;
    private String toolName;

        public Tool(int toolNum, float diameter, float cutDepth, float inSpeed, float latSpeed, String toolName) {
            this.toolNum = toolNum;
            this.diameter = diameter;
            this.cutDepth = cutDepth;
            this.inSpeed = inSpeed;
            this.latSpeed = latSpeed;
            this.toolName = toolName;
        }

        public float getDiameter () { return diameter; }

        public float getMaxCutDepth () { return cutDepth; }

        public float getInSpeed() { return inSpeed; }

        public float getLatSpeed() { return latSpeed; }

        public int getToolNum() { return toolNum; }

        public String getToolName () { return toolName; }


    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(toolNum);
        out.writeFloat(diameter);
        out.writeFloat(cutDepth);
        out.writeFloat(inSpeed);
        out.writeFloat(latSpeed);
        out.writeChars(toolName);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        toolNum = in.readInt();
        diameter = in.readFloat();
        cutDepth = in.readFloat();
        inSpeed = in.readFloat();
        latSpeed = in.readFloat();
        toolName = String.valueOf(in.readChar());

    }

    @SuppressWarnings({"EmptyMethod", "RedundantThrows", "unused"})
    private void readObjectNoData() throws ObjectStreamException { }

    public static final long serialVersionUID = 42L;

    @Override
    public int compareTo(@NonNull Object o) {
        if (o instanceof Tool){
            if (  toolNum == ((Tool) o).toolNum &&
                    diameter == ((Tool) o).diameter &&
                    cutDepth == ((Tool) o).cutDepth &&
                    inSpeed == ((Tool) o).inSpeed &&
                    latSpeed == ((Tool) o).latSpeed &&
                    toolName.equals(((Tool) o).toolName)){
                return 0;
            } else {
                return -1;
            }
        } else {
            throw new IllegalArgumentException("Object must be a Tool");
        }
    }
}
