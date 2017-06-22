package com.labgmail.pomona.greenberg.cnccarvingfactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by soniagrunwald on 6/22/17.
 */

public class DepthMap {

    float stockWidth;
    float stockHeight;
    float minRadius;

    public DepthMap(float stockWidth, float stockHeight, float minRadius) {
        this.stockWidth = stockWidth;
        this.stockHeight = stockHeight;
        this.minRadius = minRadius;

        //Calculate number of buckets
        int numBucketsX = (int) Math.ceil((double) stockWidth / (double) minRadius);
        int numBucketsY = (int) Math.ceil((double) stockHeight / (double) minRadius);

        //create the overall data structure
        //This is a 2D array of buckets. Buckets are represented as LinkedLists of Anchors
        LinkedList<Anchor>[][] depthMap = (LinkedList<Anchor>[][])new Object[numBucketsX][numBucketsY];
        //is this already initialized???
        //why are the previous points not working????


    }

    /* Takes an anchor and returns a new anchor with an updated Z value according to the depth map*/
    public Anchor updateZ (Anchor oldZ){
        //LinkedList<Anchor> potentialPoints = findNeighborPoints(findBuckets(oldZ, currentRadius))
        //for all potential points do math

        return null;
    }

    /* Adds an anchor to the existing depth map */
    public void addPoint(Anchor a){

    }

    /* Removes an anchor from the depth map */
    public void removePoint(Anchor a){

    }

    /* Finds the x coordinate of the bucket this anchor is in */
    public float findBucketCoordX (Anchor a){
        int bucket = (int) (a.x / minRadius);
        return bucket * minRadius;
    }

    /* Finds the y coordinate of the bucket this anchor is in */
    public float findBucketCoordY (Anchor a){
        int bucket = (int) (a.y / minRadius);
        return bucket * minRadius;
    }

    /* Returns all the surrounding buckets within one radius distance */
    public List<LinkedList<Anchor>> findBuckets (Anchor a, float currentRadius){
        //for (a-currentRadius).getxbucket to (a+currentRadius.getxbucket)
            //for (a-currentRadius).getybucket to (a+currentRadius).getYbucket
                //get the bucket from the depthmap (in coordinates)
                //add to the answer list

        return null;
    }

    /* Returns all the neighboring points that are in the surrounding buckets*/
    public List<Anchor> findNeighborPoints(List<LinkedList<Anchor>> listOfBuckets){
        //for each linkedlist
            //add all the anchors to the final list
        return null;
    }


}
