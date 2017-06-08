package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.app.DialogFragment;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;

/**
 * Created by edinameshietedoho on 6/1/17.
 */

public class NumberPickerPreference extends DialogPreference {

    private int mDistance;
    private static final int DEFAULT_VALUE = 5;
    private NumberPicker mDistPicker;

    public NumberPickerPreference(Context context) { this(context, null, 0); }

    public NumberPickerPreference(Context context, AttributeSet attrs) { this(context, attrs, 0); }

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.picker_layout);
        setPositiveButtonText (android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);

        mDistance = getPersistedInt(DEFAULT_VALUE);

        mDistPicker = (NumberPicker) v.findViewById(R.id.measure_amount_id);
        mDistPicker.setMinValue(0);
        mDistPicker.setMaxValue(12 * 5); // 5' ... might need to convert to appropriate current unit
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
}

