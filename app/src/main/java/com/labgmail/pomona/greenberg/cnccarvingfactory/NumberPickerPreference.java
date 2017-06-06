package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.app.DialogFragment;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

/**
 * Created by edinameshietedoho on 6/1/17.
 */

public class NumberPickerPreference extends DialogPreference {

    private int mCValue;
    private static final int DEFAULT_VALUE = 5;
    private NumberPicker numPicker;
    private static final int mDialogLayout = R.layout.picker_layout;

    public NumberPickerPreference(Context context) {
        this(context, null, 0);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.picker_layout);
        setPositiveButtonText (android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }



    @Override
    public int getDialogLayoutResource() {
        return mDialogLayout;
    }


    @Override
    protected View onCreateDialogView() {
        numPicker = new NumberPicker(getContext());
        numPicker.setMinValue(0);
        numPicker.setMaxValue(60);

        numPicker.setValue(mCValue);
        return numPicker;
    }
    @Override
    protected void onDialogClosed(boolean positive) {
        if(positive) {
            numPicker.clearFocus();

//            persistInt(mCValue);
//            setValue(numPicker.getValue());
//            int apple = 0;
            try {
               int apple = Integer.parseInt(String.valueOf(numPicker.getValue()));
                setValue(apple);
            } catch (NumberFormatException e) {
            }

        }
    }

    @Override
    protected void onSetInitialValue(boolean rpv, Object defaultV) {

        setValue(rpv ? getPersistedInt(mCValue) : (Integer) defaultV);
        if(rpv) {
            mCValue = this.getPersistedInt(DEFAULT_VALUE);
        } else {
            mCValue = (Integer) defaultV;
            persistInt(mCValue);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, DEFAULT_VALUE);
    }

    public void setValue(int value) {
        if (shouldPersist()) {
            try {
                int apple = Integer.parseInt(" " + value);
                setValue(apple);
                persistInt(value);
            } catch (NumberFormatException e) {
            }

        }

        if (value != mCValue) {
            mCValue = value;
            notifyChanged();
        }
    }

    protected Parcelable onSuperInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();

        if (isPersistent()) {
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.value = mCValue;
        return myState;
    }

    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        numPicker.setValue(myState.value);
    }

    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {

    }

    private static class SavedState extends View.BaseSavedState {
        int value;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            value = source.readInt();
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

