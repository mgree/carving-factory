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

    private float tDiameter, mDepth, iSpeed, lSpeed;
    private int tNum;

        public Tool(int toolNum, float diameter, float cutDepth, float inSpeed, float latSpeed) {

            tDiameter = diameter;
            mDepth = cutDepth;
            iSpeed = inSpeed;
            lSpeed = latSpeed;
            tNum = toolNum;
        }


        public float getDiameter () {
            return tDiameter;
        }

        public float getMaxCutDepth () {
            return mDepth;
        }

        public float getInSpeed() {
            return iSpeed;
        }

        public float getLatSpeed() {
            return lSpeed;
        }

        public int getToolNum() {
            return tNum;
        }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeFloat(tDiameter);
        out.writeFloat(mDepth);
        out.writeFloat(iSpeed);
        out.writeFloat(lSpeed);
        out.writeInt(tNum);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        tDiameter = in.readFloat();
        mDepth = in.readFloat();
        iSpeed = in.readFloat();
        lSpeed = in.readFloat();
        tNum = in.readInt();

    }

    @SuppressWarnings({"EmptyMethod", "RedundantThrows", "unused"})
    private void readObjectNoData() throws ObjectStreamException { }

    public static final long serialVersionUID = 42L;

}
