package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

/**
 * Custom number picker for integers.
 *
 * Created by edinameshietedoho on 6/1/17.
 */

public class NumberPickerPreference extends DialogPreference {

    private int mDistance;

    public NumberPickerPreference(Context context) { this(context, null, 0); }

    public NumberPickerPreference(Context context, AttributeSet attrs) { this(context, attrs, 0); }

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setDialogLayoutResource(R.layout.picker_layout);
        setPositiveButtonText (android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);

        mDistance = getPersistedInt(-1);

        NumberPicker mDistPicker = (NumberPicker) v.findViewById(R.id.measure_amount_id);
        mDistPicker.setMinValue(0);
        mDistPicker.setMaxValue(2540); // 5' ... might need to convert to appropriate current unit So should we put unit conversion here too to accomodate or just set to 2540?
        mDistPicker.setValue(mDistance);

        mDistPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                mDistance = newVal;
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positive) {
        if(positive && shouldPersist()) {
            persistInt(mDistance);
        }
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        if (restore) {
            mDistance = getPersistedInt(-1);
        } else {
            mDistance = (Integer) defaultValue;
            persistInt(mDistance);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, -1);
    }
}

