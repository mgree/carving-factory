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

    //Includes Tool Number, Tool Diameter, Max Cut Depth, Insertion Speed, Lateral Speed, tool Length, and tool name

    //The tool length that the machin considers to be "zero" in inches
    private static final float ARBITRARY_ZERO_LENGTH = 5.0f;  //TODO UPDATE THIS SO IT'S ACCURATE


    private float diameter, cutDepth, inSpeed, latSpeed, toolLength;
    private int toolNum;
    private String toolName;

        public Tool(int toolNum, float diameter, float cutDepth, float inSpeed, float latSpeed, float toolLength, String toolName) {
            this.toolNum = toolNum;
            this.diameter = diameter;
            this.cutDepth = cutDepth;
            this.inSpeed = inSpeed;
            this.latSpeed = latSpeed;
            this.toolLength = toolLength;
            this.toolName = toolName;
        }


        public float getDiameter () { return diameter; }

        public float getMaxCutDepth () { return cutDepth; }

        public float getInSpeed() { return inSpeed; }

        public float getLatSpeed() { return latSpeed; }

        public int getToolNum() { return toolNum; }

        public String getToolName () { return toolName; }

        public float getToolLength() { return  toolLength; }

        //Calculate the tool offset given its length
        //If toolOffset is positive, the tool is too long (z is added to so the machine stays higher)
        //If toolOffset is negative, the tool is too short (z is subtracted from so the machine carves lower)
        public float getToolOffset() {
            return toolLength - ARBITRARY_ZERO_LENGTH;
        }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(toolNum);
        out.writeFloat(diameter);
        out.writeFloat(cutDepth);
        out.writeFloat(inSpeed);
        out.writeFloat(latSpeed);
        out.writeFloat(toolLength);
        out.writeChars(toolName);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        toolNum = in.readInt();
        diameter = in.readFloat();
        cutDepth = in.readFloat();
        inSpeed = in.readFloat();
        latSpeed = in.readFloat();
        toolLength = in.readInt();
        toolName = String.valueOf(in.readChar());

    }

    @SuppressWarnings({"EmptyMethod", "RedundantThrows", "unused"})
    private void readObjectNoData() throws ObjectStreamException { }

    public static final long serialVersionUID = 42L;

}
