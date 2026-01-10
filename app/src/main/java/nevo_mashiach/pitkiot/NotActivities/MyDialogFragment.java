package nevo_mashiach.pitkiot.NotActivities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

/**
 * Created by Nevo Mashiach on 9/14/2016.
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


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message);
        if(positiveButtonText != null) builder.setPositiveButton(positiveButtonText, positiveButtonListener);
        if(negativeButtonText != null) builder.setNegativeButton(negativeButtonText, negativeButtonListener);
        if(naturalButtonText != null) builder.setNeutralButton(naturalButtonText, naturalButtonListener);
        AlertDialog dialog = builder.create();

        // Make the positive button more prominent after dialog is shown
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                // Get the dark green color from colorAccent
                int darkGreen = ContextCompat.getColor(context, nevo_mashiach.pitkiot.R.color.colorAccent);

                // Make positive button prominent with bold dark green text
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (positiveButton != null) {
                    // Bold text for emphasis (Material 3 Expressive)
                    positiveButton.setTypeface(null, Typeface.BOLD);

                    // Slightly larger text size for primary action
                    positiveButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

                    // Dark green text color (no background)
                    positiveButton.setTextColor(darkGreen);

                    // Add glowing effect using shadow layer
                    // Parameters: radius, dx, dy, color
                    positiveButton.setShadowLayer(8, 0, 0, darkGreen);

                    positiveButton.setAllCaps(false); // Better readability for Hebrew
                }

                // Keep negative button as text button (less prominent)
                Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                if (negativeButton != null) {
                    negativeButton.setAllCaps(false); // Better readability for Hebrew
                    // Keep default style - no background, just text
                }

                // Style neutral button as primary (for single-option dialogs)
                Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                if (neutralButton != null) {
                    // Bold text for emphasis (Material 3 Expressive)
                    neutralButton.setTypeface(null, Typeface.BOLD);

                    // Slightly larger text size for primary action
                    neutralButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

                    // Dark green text color (no background)
                    neutralButton.setTextColor(darkGreen);

                    // Add glowing effect using shadow layer
                    neutralButton.setShadowLayer(8, 0, 0, darkGreen);

                    neutralButton.setAllCaps(false); // Better readability for Hebrew
                }
            }
        });

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
