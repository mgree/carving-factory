package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.util.Log;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by soniagrunwald on 6/22/17.
 */

public class DepthMap {

    float stockWidth;
    float stockHeight;
    float minRadius;
    float scale;
    LinkedList<Anchor>[][] depthMap;
    int numBucketsX;
    int numBucketsY;

    public DepthMap(float stockWidth, float stockHeight, float minRadius, float scale) {
        this.stockWidth = stockWidth;
        this.stockHeight = stockHeight;
        this.minRadius = minRadius;
        this.scale = scale;

        //Calculate number of buckets
        numBucketsX = (int) Math.ceil((double) stockWidth / (double) minRadius);
        numBucketsY = (int) Math.ceil((double) stockHeight / (double) minRadius);

        //create and initialize the overall data structure
        depthMap = (LinkedList<Anchor>[][]) new LinkedList[numBucketsY][numBucketsX];
        for (int i = 0; i < numBucketsY; i++){
            for (int j = 0; j < numBucketsX; j ++){
                depthMap[i][j] = new LinkedList<>();
            }
        }
    }

    /* Takes an anchor and updates the depth map with a new anchor with an updated Z value*/
    public float updateZ (Anchor oldPoint, float currentRadius){
        boolean changed = false;

        //Find all relevant points
        LinkedList<Anchor> potentialPoints = (LinkedList<Anchor>) findNeighborPoints(findBuckets(oldPoint, currentRadius));

        // count neighbors in neighboring buckets after some time threshold
        float neighboringDepth = 0.0f;
        float newZ = Math.max(neighboringDepth, oldPoint.z);
        for (Anchor a : potentialPoints) {
            if (oldPoint.distance2D(a.x, a.y) <= currentRadius * scale && !changed) {
                   // && Math.abs(oldPoint.time - a.time) > TIME_THRESHOLD) { //prevents it from counting adjacent points
                                                                              //Unnecessary because depthmap updates on savestroke
                    neighboringDepth = Math.max(oldPoint.getGrayScale(), .01f);
                    changed = true;
            }
        }
        newZ += neighboringDepth;
        newZ = Math.max(0.1f, Math.min(newZ, 1.0f));

        return newZ;
    }

    /* Adds an anchor to the existing depth map */
    public void addPoint(Anchor a){
        findBucket(a).add(a);
    }

    /* Removes an anchor from the depth map */
    public void removePoint(Anchor a){ findBucket(a).remove(a); }

    /* Clears the depth map */
    public void clear(){
        for (LinkedList<Anchor>[] aList : depthMap){
            for (LinkedList<Anchor> a : aList){
                a.clear();
            }
        }
    }

    /* Finds the bucket the point is in */
    public LinkedList<Anchor> findBucket (Anchor a){
        return depthMap[findBucketIndexY(a)][findBucketIndexX(a)];
    }

    /* Finds the x index of the bucket this anchor is in in the array*/
    public int findBucketIndexX (Anchor a){
        int ans = (int) (a.x / (minRadius * scale));
        return Math.max(Math.min (ans, numBucketsX - 1), 0);
    }

    /* Finds the y index of the bucket this anchor is in in the array*/
    public int findBucketIndexY (Anchor a){
        int ans = (int) (a.y / (minRadius * scale));
        return Math.max(Math.min (ans, numBucketsY - 1), 0);
    }

    /* Returns all the surrounding buckets within one radius distance */
    public List<LinkedList<Anchor>> findBuckets (Anchor a, float currentRadius){
        LinkedList<LinkedList<Anchor>> ans = new LinkedList<>();

        int numBucketsAway = (int)Math.ceil(currentRadius/minRadius);
        int boundaryXMin = Math.max(0, findBucketIndexX(a) - numBucketsAway);
        int boundaryXMax = Math.min(numBucketsX - 1, findBucketIndexX(a) + numBucketsAway);
        int boundaryYMin = Math.max(0, findBucketIndexY(a) - numBucketsAway);
        int boundaryYMax = Math.min(numBucketsY - 1, findBucketIndexY(a) + numBucketsAway);

        for (int i = boundaryYMin; i <= boundaryYMax; i++){
            for (int j = boundaryXMin; j <= boundaryXMax; j++){
                    ans.add(depthMap[i][j]);
            }
        }
        return ans;
    }

    /* Returns all the neighboring points that are in a list of buckets (will be the surrounding buckets) */
    public List<Anchor> findNeighborPoints(List<LinkedList<Anchor>> listOfBuckets){
        LinkedList<Anchor> ans = new LinkedList<>();
        for (LinkedList<Anchor> l : listOfBuckets){
            for (Anchor a : l) {
                ans.add(a);
            }
        }
        return ans;
    }
}