package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.graphics.PointF;
import android.util.Log;

import java.util.LinkedList;

import static com.labgmail.pomona.greenberg.cnccarvingfactory.VNCButton.Button.*;

/**
 * Created by soniagrunwald on 7/19/17.
 */

public class VNCButton {
    private Button buttonType;

    public enum Button {
        A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z,
        ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, ZERO, DECIMAL,
        DELETE, ENTER, CLEAR, BACKWARD, FORWARD, CLEARALL, SIGNCHANGE,
        NUM_PAGE, A_TO_P_PAGE, Q_TO_Z_PAGE, COMMAND_ENTER_BAR, EMPTY
    }

    //Buttons can be created using either a button type, int, or string
    public VNCButton(Button buttonType){
        this.buttonType = buttonType;
    }
    public VNCButton(int i){
        buttonType = getButton(i);
    }
    public VNCButton(String s){
        buttonType = getButton(s);
    }

    //translates ints to buttons
    private Button getButton(int i) {
        switch (i){
            case 0:
                return ZERO;
            case 1:
                return ONE;
            case 2:
                return TWO;
            case 3:
                return THREE;
            case 4:
                return FOUR;
            case 5:
                return FIVE;
            case 6:
                return SIX;
            case 7:
                return SEVEN;
            case 8:
                return EIGHT;
            case 9:
                return NINE;
            default:
                return EMPTY;
        }
    }

    //translates ints to buttons
    private Button getButton(String s) {
        switch (s.toUpperCase()){
            case "A":
                return A;
            case "B":
                return B;
            case "C":
                return C;
            case "D":
                return D;
            case "E":
                return E;
            case "F":
                return F;
            case "G":
                return G;
            case "H":
                return H;
            case "I":
                return I;
            case "J":
                return J;
            case "K":
                return K;
            case "L":
                return L;
            case "M":
                return M;
            case "N":
                return N;
            case "O":
                return O;
            case "P":
                return P;
            case "Q":
                return Q;
            case "R":
                return R;
            case "S":
                return S;
            case "T":
                return T;
            case "U":
                return U;
            case "V":
                return V;
            case "W":
                return W;
            case "X":
                return X;
            case "Y":
                return Y;
            case "Z":
                return Z;
            case ".":
                return DECIMAL;
            default:
                return EMPTY;
        }
    }


    //Returns coordinates for the button type of this button
    public LinkedList<PointF> getCoordinates() {
        return getCoordinates(buttonType);
    }

    //Returns coordinates for a given buttonType
    public LinkedList<PointF> getCoordinates(Button button) {

        LinkedList<PointF> ans = new LinkedList<>();

        switch (button){
            case A :
                ans.addAll(getCoordinates(A_TO_P_PAGE));
                ans.add(new PointF(200, 160));
            case B :
                ans.addAll(getCoordinates(A_TO_P_PAGE));
                ans.add(new PointF(280, 160));
            case C :
                ans.addAll(getCoordinates(A_TO_P_PAGE));
                ans.add(new PointF(360, 160));
            case D :
                ans.addAll(getCoordinates(A_TO_P_PAGE));
                ans.add(new PointF(440, 160));
            case E :
                ans.addAll(getCoordinates(A_TO_P_PAGE));
                ans.add(new PointF(200, 240));
            case F :
                ans.addAll(getCoordinates(A_TO_P_PAGE));
                ans.add(new PointF(280, 240));
            case G :
                ans.addAll(getCoordinates(A_TO_P_PAGE));
                ans.add(new PointF(360, 240));
            case H :
                ans.addAll(getCoordinates(A_TO_P_PAGE));
                ans.add(new PointF(440, 240));
            case I :
                ans.addAll(getCoordinates(A_TO_P_PAGE));
                ans.add(new PointF(200, 320));
            case J :
                ans.addAll(getCoordinates(A_TO_P_PAGE));
                ans.add(new PointF(280, 320));
            case K :
                ans.addAll(getCoordinates(A_TO_P_PAGE));
                ans.add(new PointF(360, 320));
            case L :
                ans.addAll(getCoordinates(A_TO_P_PAGE));
                ans.add(new PointF(440, 320));
            case M :
                ans.addAll(getCoordinates(A_TO_P_PAGE));
                ans.add(new PointF(200, 400));
            case N :
                ans.addAll(getCoordinates(A_TO_P_PAGE));
                ans.add(new PointF(280, 400));
            case O :
                ans.addAll(getCoordinates(A_TO_P_PAGE));
                ans.add(new PointF(360, 400));
            case P :
                ans.addAll(getCoordinates(A_TO_P_PAGE));
                ans.add(new PointF(440, 400));
            case Q :
                ans.addAll(getCoordinates(Q_TO_Z_PAGE));
                ans.add(new PointF(200, 160));
            case R :
                ans.addAll(getCoordinates(Q_TO_Z_PAGE));
                ans.add(new PointF(280, 160));
            case S :
                ans.addAll(getCoordinates(Q_TO_Z_PAGE));
                ans.add(new PointF(360, 160));
            case T :
                ans.addAll(getCoordinates(Q_TO_Z_PAGE));
                ans.add(new PointF(440, 160));
            case U :
                ans.addAll(getCoordinates(Q_TO_Z_PAGE));
                ans.add(new PointF(200, 240));
            case V :
                ans.addAll(getCoordinates(Q_TO_Z_PAGE));
                ans.add(new PointF(280, 240));
            case W :
                ans.addAll(getCoordinates(Q_TO_Z_PAGE));
                ans.add(new PointF(360, 240));
            case X :
                ans.addAll(getCoordinates(Q_TO_Z_PAGE));
                ans.add(new PointF(440, 240));
            case Y :
                ans.addAll(getCoordinates(Q_TO_Z_PAGE));
                ans.add(new PointF(200, 320));
            case Z :
                ans.addAll(getCoordinates(Q_TO_Z_PAGE));
                ans.add(new PointF(280, 320));
            case ONE :
                ans.addAll(getCoordinates(NUM_PAGE));
                ans.add(new PointF(200, 160));
            case TWO :
                ans.addAll(getCoordinates(NUM_PAGE));
                ans.add(new PointF(280, 160));
            case THREE :
                ans.addAll(getCoordinates(NUM_PAGE));
                ans.add(new PointF(360, 160));
            case FOUR :
                ans.addAll(getCoordinates(NUM_PAGE));
                ans.add(new PointF(200, 240));
            case FIVE :
                ans.addAll(getCoordinates(NUM_PAGE));
                ans.add(new PointF(280, 240));
            case SIX :
                ans.addAll(getCoordinates(NUM_PAGE));
                ans.add(new PointF(360, 240));
            case SEVEN :
                ans.addAll(getCoordinates(NUM_PAGE));
                ans.add(new PointF(200, 320));
            case EIGHT :
                ans.addAll(getCoordinates(NUM_PAGE));
                ans.add(new PointF(280, 320));
            case NINE :
                ans.addAll(getCoordinates(NUM_PAGE));
                ans.add(new PointF(360, 320));
            case ZERO :
                ans.addAll(getCoordinates(NUM_PAGE));
                ans.add(new PointF(280, 400));
            case DECIMAL:
                ans.addAll(getCoordinates(NUM_PAGE));
                ans.add(new PointF(360, 400));
            case SIGNCHANGE:
                ans.addAll(getCoordinates(NUM_PAGE));
                ans.add(new PointF(200, 400));
            case BACKWARD:
                ans.addAll(getCoordinates(Q_TO_Z_PAGE));
                ans.add(new PointF(200, 400));
            case FORWARD:
                ans.addAll(getCoordinates(Q_TO_Z_PAGE));
                ans.add(new PointF(280, 400));
            case CLEARALL:
                ans.addAll(getCoordinates(Q_TO_Z_PAGE));
                ans.add(new PointF(360, 400));
            case CLEAR:
                ans.add(new PointF(565, 160));
            case DELETE:
                ans.add(new PointF(565, 240));
            case ENTER:
                ans.add(new PointF(565, 320));
            case NUM_PAGE:
                ans.add(new PointF(90, 160));
            case A_TO_P_PAGE:
                ans.add(new PointF(90, 240));
            case Q_TO_Z_PAGE:
                ans.add(new PointF(90, 320));
            case COMMAND_ENTER_BAR:
                ans.add(new PointF(300, 75));
            default:
                Log.d("BUTTON", "Unknown button type.");
        }
        return ans;
    }
}
