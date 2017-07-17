package com.labgmail.pomona.greenberg.cnccarvingfactory;

/**
 * Created by soniagrunwald on 7/17/17.
 */

public class Ticker implements Runnable {

    public static final long WAIT_TIME = 20;

    private DrawingView dv;
    private boolean shouldStop = false;

    public Ticker(DrawingView dv) {
        this.dv = dv;
    }

    public void stop() {
        shouldStop = true;
    }

    @Override
    public void run() {
        long prevTime = System.currentTimeMillis();
        while (!shouldStop) {
            try {
                Thread.sleep(WAIT_TIME);
            } catch (InterruptedException e) { }

            long now = System.currentTimeMillis();
            dv.tick(now - prevTime);
            prevTime = now;
        }
    }


}
