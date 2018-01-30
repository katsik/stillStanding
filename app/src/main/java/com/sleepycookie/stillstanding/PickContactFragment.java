package com.sleepycookie.stillstanding;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;

/**
 * Created by George Katsikopoulos on 1/26/2018.
 *
 * Based on code found https://developer.android.com/guide/topics/ui/dialogs.html#DialogFragment
 */

public class PickContactFragment extends DialogFragment {
    public interface PickContactListener{
        void onPickContactPositive();
        void onPickContactNegative();
    }

    // Use this instance of the interface to deliver action events
    PickContactListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (PickContactListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstance){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        //Setting the title of the dialog
        builder.setMessage(R.string.pick_contact_msg)
                // Add action buttons
                .setPositiveButton(R.string.pick_contact_pos, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onPickContactPositive();
                    }
                })
                .setNegativeButton(R.string.pick_contact_neg, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onPickContactNegative();
                    }
                });
        return builder.create();
    }
}
