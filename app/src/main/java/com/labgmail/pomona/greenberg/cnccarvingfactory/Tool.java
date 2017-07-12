package com.labgmail.pomona.greenberg.cnccarvingfactory;

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

public class Tool implements Serializable {

    //Includes Tool Diameter, Tool Number, Max Cut Depth, Insertion Speed, and Lateral Speed.

    private float diameter, cutDepth, inSpeed, latSpeed, toolLength;
    private int toolNum;

        public Tool(int toolNum, float diameter, float cutDepth, float inSpeed, float latSpeed, float toolLength) {

            this.toolNum = toolNum;
            this.diameter = diameter;
            this.cutDepth = cutDepth;
            this.inSpeed = inSpeed;
            this.latSpeed = latSpeed;
            this.toolLength = toolLength;
        }


        public float getDiameter () {
            return diameter;
        }

        public float getMaxCutDepth () {
            return cutDepth;
        }

        public float getInSpeed() {
            return inSpeed;
        }

        public float getLatSpeed() {
            return latSpeed;
        }

        public int getToolNum() {
            return toolNum;
        }

        public float getToolLength() { return  toolLength; }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(toolNum);
        out.writeFloat(diameter);
        out.writeFloat(cutDepth);
        out.writeFloat(inSpeed);
        out.writeFloat(latSpeed);
        out.writeFloat(toolLength);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        toolNum = in.readInt();
        diameter = in.readFloat();
        cutDepth = in.readFloat();
        inSpeed = in.readFloat();
        latSpeed = in.readFloat();
        toolLength = in.readInt();

    }

    @SuppressWarnings({"EmptyMethod", "RedundantThrows", "unused"})
    private void readObjectNoData() throws ObjectStreamException { }

    public static final long serialVersionUID = 42L;

}
