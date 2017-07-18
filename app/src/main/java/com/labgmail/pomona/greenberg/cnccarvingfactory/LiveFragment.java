package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Created by edinameshietedoho on 7/17/17.
 */

public class LiveFragment extends DialogFragment {

    public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//            builder.create();
            builder.setMessage("Entering Live Mode, enter the following:");
//            builder.setView(R.layout.live_dialog);
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
          {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
            return builder.create();
        }

}

