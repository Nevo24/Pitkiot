package nevo_mashiach.pitkiot.NotActivities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

/**
 * Created by Nevo2 on 9/14/2016.
 */
public class MyDialogFragment extends DialogFragment {
    String title;
    String message;

    String positiveButtonText = null;
    DialogInterface.OnClickListener positiveButtonListener = null;

    String negativeButtonText = null;
    DialogInterface.OnClickListener negativeButtonListener = null;

    String naturalButtonText = null;
    DialogInterface.OnClickListener naturalButtonListener = null;

    public MyDialogFragment(String _title, String _message){
        title = _title;
        message = _message;
    }

    public MyDialogFragment(String _title, String _message, String _positiveButtonText, DialogInterface.OnClickListener _positiveButtonListener, String _negativeButtonText, DialogInterface.OnClickListener _negativeButtonListener, String _naturalButtonText, DialogInterface.OnClickListener _naturalButtonListener){
        title = _title;
        message = _message;
        positiveButtonText = _positiveButtonText;
        positiveButtonListener = _positiveButtonListener;
        negativeButtonText = _negativeButtonText;
        negativeButtonListener = _negativeButtonListener;
        naturalButtonText = _naturalButtonText;
        naturalButtonListener = _naturalButtonListener;
    }


    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message);
        if(positiveButtonText != null) builder.setPositiveButton(positiveButtonText, positiveButtonListener);
        if(negativeButtonText != null) builder.setNegativeButton(negativeButtonText, negativeButtonListener);
        if(naturalButtonText != null) builder.setNeutralButton(naturalButtonText, naturalButtonListener);
        AlertDialog dialog = builder.create();
        return dialog;
    }

    public MyDialogFragment setPositiveButton (String text, DialogInterface.OnClickListener listener) {
        return new MyDialogFragment(title, message, text, listener, negativeButtonText, negativeButtonListener, naturalButtonText, naturalButtonListener);
    }

    public MyDialogFragment setNegativeButton (String text, DialogInterface.OnClickListener listener) {
        return new MyDialogFragment(title, message, positiveButtonText, positiveButtonListener, text, listener, naturalButtonText, naturalButtonListener);
    }

    public MyDialogFragment setNaturalButton (String text, DialogInterface.OnClickListener listener) {
        return new MyDialogFragment(title, message, positiveButtonText, positiveButtonListener, negativeButtonText, negativeButtonListener, text, listener);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.setCancelable(false);

        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
