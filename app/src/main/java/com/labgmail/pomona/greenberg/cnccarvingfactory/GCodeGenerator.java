package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Centralized GCode generation.
 *
 * Created by mgree on 2017-6-22.
 */
public class GCodeGenerator {


    /**
     * If we want max cut by tools
     * 1) Pick deepest val you're willing to go and scale z according to that
     * 2) store a current max single cut depth
     * 3) if the z you're trying to cut to is lower than that, cut to the max cut depth and
     *          calculate the discrepancy between the current depth and the goal depth
     *          If needed repeat until goal depth is achieved
     * Issues:
     *      Make sure to account for starting and ending points on the second cut through
     *      (finish the first, go deeper, go to start, go to end)
     *      Remember to raise up before next stroke
     *      Sort strokes by which cut they're on?
     *      When do you tell the stroke to go back and do that second cut (especially if you're going in real time)??
     *
     */


    private final StringWriter buf;
    private final PrintWriter out;

    private int line = 1; // current line
    private Map<String,Float> fParams = new TreeMap<>(); // current floating-point params
    private Map<String,Integer> iParams = new TreeMap<>(); // current integer params
    private static Tool curTool = null;

    public static final float MAX_SINGLE_CUT_DEPTH = 0.4f;
    public static final float CLEARANCE = .25f;

    /**
     * Single-pass stroke milling.
     *
     * @param strokes
     * @param stockWidth width of stock in stockUnit
     * @param stockLength
     * @param stockDepth
     * @param stockUnit
     * @param cutoffRight width of drawing canvas in pixels
     * @param spoilDepth depth of spoil board in stockUnit
     * @return a GCodeGenerator with all code appropriately generated
     */
    public static GCodeGenerator singlePass(List<Stroke> strokes,
                                            float stockWidth, float stockLength, float stockDepth, String stockUnit,
                                            float cutoffRight,
                                            float spoilDepth) {
        GCodeGenerator gcg = new GCodeGenerator();

        // metadata
        gcg.comment("generated by CNC Carving Factory");
        gcg.comment(String.format("expected stock dimensions: %1.4fx%1.4fx%1.4f %s",
                stockWidth, stockLength, stockDepth, stockUnit));

        float boardHeight = spoilDepth + stockDepth;
        final float clearancePlane = boardHeight + CLEARANCE;
        final float maxCutDepth = spoilDepth + stockDepth - MAX_SINGLE_CUT_DEPTH; //TODO Tool specific Max cut depth

        float ipp = stockWidth / cutoffRight; // scaling factor

        // standard prelude
        gcg.prelude();

        // write the strokes into the file in GCode format
        int numStrokes = strokes.size();
        gcg.comment("Carving " + strokes.size() + " strokes in a single pass");

        int curStroke = 0;
        for (Stroke s : strokes) {
            // TODO: transformations on strokes (convert to stock coordinate system, Y inversion)

            boolean cutting = false;

            curStroke += 1;
            gcg.comment(String.format("Stroke %d/%d", curStroke, numStrokes));

            Anchor last = null;
            for (Anchor point : s) {
                if (point.equals(last)) {
                    continue;
                }

                //if the tool has changed, add that to the gcode file and update
                if (s.getTool() != curTool){
                    gcg.tool(s.getTool());
                    curTool = s.getTool();
                }

                if (!cutting) {
                    // emit G00 moves, move in w/slow feed rate (first iteration)
                    gcg.cmd(new G(0).X(point.x * ipp).Y(stockLength - point.y * ipp));
                    gcg.cmd(new G(0).Z(clearancePlane));

                    // bound the z insertion depth!
                    float z = boardHeight - (point.z * MAX_SINGLE_CUT_DEPTH); //TODO Change these to tool specific max cut depths???
                    z = Math.max(z, maxCutDepth);
                    z = Math.min(z, clearancePlane);
                    gcg.cmd(new G(1).Z(z).F(curTool.getInSpeed()));

                    cutting = true;
                } else {
                    // first G01 move, set high feedrate (second iteration)
                    // bound the z insertion depth!
                    float z = boardHeight - (point.z * MAX_SINGLE_CUT_DEPTH); //TODO Change these to tool specific max cut depths???
                    z = Math.max(z, maxCutDepth);
                    z = Math.min(z, clearancePlane);

                    gcg.cmd(new G(1).X(point.x * ipp).Y(stockLength - point.y * ipp).Z(z).F(curTool.getLatSpeed()));

                }
                last = point;
            }
            // emit pull out
            if (last != null) {
                gcg.cmd(new G(0).X(last.x * ipp).Y(stockLength - last.y * ipp), true);
                gcg.cmd(new G(0).Z(clearancePlane));
            }
        }

        gcg.outlude();

        return gcg;
    }

    public GCodeGenerator() {
        buf = new StringWriter();
        out = new PrintWriter(buf);
    }

    public int numLines() { return line-1; }

    /**
     * Gets the generated GCode so far.
     */
    public String toString() {
        out.flush();
        buf.flush();

        return buf.toString();
    }

    /**
     * Emits a comment.
     *
     * @param comment
     */
    public void comment(String comment) {
        out.printf("(%s)\n", comment);
    }

    /**
     * Emits a command, which will be prefaced by a line number.
     *
     * Side-effect: the line number will increment.
     *
     * @param cmd command to emit (line number will be assigned automatically)
     */
    public void cmd(String cmd) {
        out.printf("N%d %s\n", line++, cmd);
    }

    /**
     * Emits a G-Code command, which will be prefaced by a line number.
     * Redundant commands---which have no new parameters at all---will be skipped.
     *
     * Side-effect: the line number will increment.
     *
     * @param code command to emit (line number will be assigned automatically)
     */
    public void cmd(G code) { cmd(code, false); }

    /**
     * Emits a G-Code command, which will be prefaced by a line number.
     * Redundant commands---which have no new parameters at all---will be skipped
     * unless force is set to true, in which case redundant params will not be pruned.
     *
     * Side-effect: the line number will increment.
     *
     * @param code command to emit (line number will be assigned automatically)
     * @param force set to true to disable redundancy checking
     */
    public void cmd(G code, boolean force) {
        if (!force && code.isRedundant(fParams, iParams)) {
            Log.d("GCODE", "skipped redundant command " + code.toString());
            return;
        }

        cmd(force ? code.toString() : code.emit(fParams, iParams));

        // save params for next command, to avoid redundancy
        fParams.putAll(code.fParams);
        iParams.putAll(code.iParams);
    }

    public void prelude() {
        comment("prelude");
        cmd("G00 G17 G40 G90");
        cmd("G70");
        cmd("G54");
    }

    public void outlude() {
        comment("closing arguments");
        cmd("D0");
        cmd("G00 Z0");
        cmd("G53");
        cmd("G00 Y0.0000");
        cmd("G00 X0.0000 M05");
        cmd("G54");
        cmd("M30");
        // some samples indicate a % sign after to ensure a newline, but our CNC code checker rejects it
    }

    public void tool(Tool tool) {
        cmd(String.format("T%d M06", tool.getToolNum()));
        cmd("D1");
        cmd("M03 S18000"); // TODO RPMs is per-tool
    }

    public static class G {
        private final int code;
        private Map<String,Float> fParams = new TreeMap<>();
        private Map<String,Integer> iParams = new TreeMap<>();

        public G(int code) {
            this.code = code;
        }

        public G X(float v) { fParams.put("X", v); return this; }
        public G Y(float v) { fParams.put("Y", v); return this; }
        public G Z(float v) { fParams.put("Z", v); return this; }
        public G F(float v) { fParams.put("F", v); return this; }
        /**
         * Once arcs are implemented, add these
         * public G R(float v) { fParams.put("R", v); return this; }
         * public G I(float v) { fParams.put("I", v); return this; }
         * public G J(float v) { fParams.put("J", v); return this; }
         * public G K(float v) { fParams.put("K", v); return this; }
         *
         */

        /**
         * Returns a string representation of the G-Code instruction.
         *
         * Any parameters that are the same as those in the input map are ignored.
         *
         * @param lastF
         * @param lastI
         * @return
         */
        public String emit(Map<String, Float> lastF, Map<String, Integer> lastI) {
            StringBuilder params = new StringBuilder();

            for (Map.Entry<String,Float> e : fParams.entrySet()) {
                String key = e.getKey();
                float val = e.getValue();

                if (lastF == null || !lastF.containsKey(key) || !lastF.get(key).equals(val)) {
                    if (key.equals("F")) {
                        params.append(String.format("F%1.1f ", val));
                    } else {
                        params.append(String.format("%s%1.4f ", key, val));
                    }
                }
            }

            for (Map.Entry<String,Integer> e : iParams.entrySet()) {
                String key = e.getKey();
                int val = e.getValue();

                if (lastI == null || !lastI.containsKey(key) || !lastI.get(key).equals(val)) {
                    params.append(String.format("%s%d ", key, val));
                }
            }

            return String.format("G%d %s", code, params.toString().trim());
        }

        /**
         * Determines whether a G-Code command is redundant, i.e., whether all of the params are the
         * same as those given in the arguments.
         *
         * @param lastF
         * @param lastI
         * @return
         */
        public boolean isRedundant(Map<String, Float> lastF, Map<String, Integer> lastI) {
            for (Map.Entry<String, Float> e : fParams.entrySet()) {
                if (!lastF.containsKey(e.getKey()) || !lastF.get(e.getKey()).equals(e.getValue())) {
                    return false;
                }
            }

            for (Map.Entry<String, Integer> e : iParams.entrySet()) {
                if (!lastI.containsKey(e.getKey()) || !lastI.get(e.getKey()).equals(e.getValue())) {
                    return false;
                }
            }

            return true;
        }

        public String toString() {
            return emit(null, null);
        }
    }
}
