package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.app.DialogFragment;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

/**
 * Created by edinameshietedoho on 6/1/17.
 */

// ??? I just realized this is going to need to run two numberpickers at the same time for length and width hmmm...
public class NumberPickerPreference extends DialogPreference {

    private int mValue;
    private int mCValue;
    private static final int DEFAULT_VALUE = 5;
    private NumberPicker numPicker;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        // ! Will probably have to create own layout but lets pretend this will work for a while
//        setDialogLayoutResource(R.layout.pref_dialog_npicker);
        setPositiveButtonText (android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }

    @Override
    protected View onCreateDialogView() {
        numPicker = new NumberPicker(getContext());
        numPicker.setMinValue(5);
        numPicker.setMaxValue(10);

        numPicker.setValue(mCValue);
        return numPicker;
    }
    protected void onDialogClosed(boolean positive) {
        if(positive) {
            persistInt(mValue);
        }
    }

    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, DEFAULT_VALUE);
    }

    protected void onSetInitialValue(boolean rpv, Object defaultV) {
        if(rpv) {
            mCValue = this.getPersistedInt(DEFAULT_VALUE);
        } else {
            mCValue = (Integer) defaultV;
            persistInt(mCValue);
        }
    }

    protected Parcelable onSuperInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();

        if(isPersistent()) {
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.value = mValue;
        return myState;
    }

    protected void onRestoreInstanceState(Parcelable state) {
        if(state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
//      ???  Where did they get this random NumberPicker from :/
//        mNumberPicker.setValue(myState.value);
    }
// ??? Is this how that works??? I'm almost positive that is not how this works. Supposed to be a subclass of the preference subclass
    private static class SavedState extends View.BaseSavedState {
        int value;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest,flags);
            dest.writeInt(value);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

    }

}

