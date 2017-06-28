package com.labgmail.pomona.greenberg.cnccarvingfactory;

import java.util.List;

/**
 * Created by edinameshietedoho on 6/22/17.
 */

public class Tools {

    //Includes Tool Diameter, Tool Number, Max Cut Depth, Insertion Speed, and Lateral Speed.

    //(Summary)
    // Tool 1 - Half Inch (0.5)
    // Diameter:
    // Max Cut Depth:
    // Insertion Speed:
    //Lateral Speed:

    private float tDiameter, mDepth, iSpeed, lSpeed;
    private int tNum;
    public Tools selectedTool;

        public Tools(int toolNum, float diameter, float cutDepth, float inSpeed, float latSpeed) {

            this.tDiameter = diameter;
            this.mDepth = cutDepth;
            this.iSpeed = inSpeed;
            this.lSpeed = latSpeed;
            this.tNum = toolNum;
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

        public int getSelectedTool(List<Tools> tools) { //????
            return tools.lastIndexOf(tools);

        }
}
