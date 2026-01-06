package nevo_mashiach.pitkiot;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import nevo_mashiach.pitkiot.NotActivities.DialogBag;
import nevo_mashiach.pitkiot.NotActivities.NoteCollectionSession;
import nevo_mashiach.pitkiot.NotActivities.db;

import static nevo_mashiach.pitkiot.NotActivities.db.defs;

public class NoteManagement extends AppCompatActivity {

    @BindView(R.id.typeDef)
    EditText mTypeDef;
    @BindView(R.id.noteCount)
    TextView mNoteCount;

    Context context;
    AppCompatActivity thisActivity;
    DialogBag dialogBag;

    SharedPreferences prefs;
    SharedPreferences.Editor spEditor;

    // Firebase note collection
    private NoteCollectionSession noteCollectionSession;
    private Dialog collectionDialog;
    private int receivedNotesCount = 0;
    private java.util.HashMap<String, Integer> submitterNoteCounts = new java.util.HashMap<>();
    private static final String FIREBASE_HOSTING_URL = "https://pitkiot-29650.web.app";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_management);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ButterKnife.bind(this);
        context = this;
        thisActivity = this;
        dialogBag = new DialogBag(getFragmentManager(), this);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        spEditor = prefs.edit();

        mTypeDef.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addingDefToDb(null);

                    //Closing keyboard:
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                return false;
            }
        });
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    protected void onResume() {
        super.onResume();
        mNoteCount.setText("מספר הפתקים במאגר: " + db.totalNoteAmount());
    }


    public void onDestroy() {
        super.onDestroy();
        // Clean up Firebase session
        if (noteCollectionSession != null) {
            noteCollectionSession.endSession();
        }
        if (collectionDialog != null && collectionDialog.isShowing()) {
            collectionDialog.dismiss();
        }
    }


    private void add(String name) {
        name = name.trim();
        if (!db.noteExists(name) && !name.equals("")) {
            defs.add(name);
            saveNotes();
            mNoteCount.setText("מספר הפתקים במאגר: " + db.totalNoteAmount());
        }
    }

    private void saveNotes() {
        //save notes to shared preferences
        Set<String> set = new HashSet<String>();
        set.addAll(db.defs);
        spEditor.putStringSet("defs", set);
        spEditor.commit();
    }


    //*********************** ON CLICKS ********************************
    @OnClick(R.id.addDef)
    public void addingDefToDb(View view) {
        String input = mTypeDef.getText().toString();
        List<String> notes = parseNotes(input);
        for (String note : notes) {
            add(note);
        }
        mTypeDef.setText("");
    }


    @OnClick(R.id.deleteNotes)
    public void deleteNodesButton(View view) {
        Intent intent = new Intent(context, NoteList.class);
        startActivity(intent);
    }

    @OnTouch({R.id.addDef, R.id.deleteNotes})
    boolean onTouch(View view, MotionEvent motion) {
        return db.onTouch(context, view, motion);
    }

    @OnClick(R.id.collectNotesOnline)
    public void startOnlineNoteCollection(View view) {
        // Reset counters for new session
        receivedNotesCount = 0;
        submitterNoteCounts.clear();

        // Create a new collection session
        noteCollectionSession = new NoteCollectionSession();
        String sessionId = noteCollectionSession.createSession();
        String url = noteCollectionSession.getSubmissionUrl(FIREBASE_HOSTING_URL);

        // Show the collection dialog
        showNoteCollectionDialog(sessionId, url);

        // Start listening for incoming notes
        noteCollectionSession.startListening(new NoteCollectionSession.OnNoteReceivedListener() {
            @Override
            public void onNoteReceived(String submitterName, String noteContent) {
                runOnUiThread(() -> {
                    // Parse and add notes (same logic as SMS)
                    List<String> notes = parseNotes(noteContent);
                    for (String note : notes) {
                        add(note);
                    }

                    // Update the dialog UI
                    receivedNotesCount++;
                    updateCollectionDialogUI(submitterName, noteContent);
                    Toast.makeText(context, "פתק חדש התקבל מ" + submitterName, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(context, "שגיאה: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showNoteCollectionDialog(String sessionId, String url) {
        // Create dialog
        collectionDialog = new Dialog(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_note_collection, null);
        collectionDialog.setContentView(dialogView);
        collectionDialog.setCancelable(false);

        // Setup dialog views
        TextView sessionUrlText = dialogView.findViewById(R.id.sessionUrlText);
        ImageView qrCodeImage = dialogView.findViewById(R.id.qrCodeImage);
        Button copyUrlButton = dialogView.findViewById(R.id.copyUrlButton);
        Button closeSessionButton = dialogView.findViewById(R.id.closeSessionButton);
        TextView receivedNotesCountText = dialogView.findViewById(R.id.receivedNotesCount);

        // Set values
        sessionUrlText.setText(url);
        receivedNotesCountText.setText("0 פתקים התקבלו מ-0 אנשים");

        // Make URL clickable
        sessionUrlText.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
            startActivity(browserIntent);
        });

        // Generate QR code
        try {
            Bitmap qrBitmap = generateQRCode(url, 500, 500);
            qrCodeImage.setImageBitmap(qrBitmap);
        } catch (WriterException e) {
            Toast.makeText(context, "שגיאה ביצירת QR", Toast.LENGTH_SHORT).show();
        }

        // Copy URL button
        copyUrlButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Pitkiot URL", url);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "הקישור הועתק!", Toast.LENGTH_SHORT).show();
        });

        // Close button
        closeSessionButton.setOnClickListener(v -> {
            if (noteCollectionSession != null) {
                noteCollectionSession.endSession();
            }
            collectionDialog.dismiss();
            Toast.makeText(context, receivedNotesCount + " פתקים נוספו למאגר", Toast.LENGTH_SHORT).show();
            receivedNotesCount = 0;
            submitterNoteCounts.clear();
        });

        collectionDialog.show();
    }

    private void updateCollectionDialogUI(String submitterName, String noteContent) {
        if (collectionDialog != null && collectionDialog.isShowing()) {
            View dialogView = collectionDialog.findViewById(R.id.receivedNotesList);
            TextView countText = collectionDialog.findViewById(R.id.receivedNotesCount);

            // Count notes in this submission
            List<String> notes = parseNotes(noteContent);
            int notesInSubmission = notes.size();

            // Update total count for this submitter
            Integer currentCount = submitterNoteCounts.get(submitterName);
            if (currentCount == null) {
                currentCount = 0;
            }
            submitterNoteCounts.put(submitterName, currentCount + notesInSubmission);

            // Update total count display (total notes and total people)
            int totalPeople = submitterNoteCounts.size();
            countText.setText(receivedNotesCount + " פתקים התקבלו מ-" + totalPeople + " אנשים");

            // Rebuild the submitters list
            if (dialogView instanceof LinearLayout) {
                LinearLayout notesList = (LinearLayout) dialogView;

                // Clear all views
                notesList.removeAllViews();

                // Add a TextView for each submitter with styled appearance
                for (java.util.Map.Entry<String, Integer> entry : submitterNoteCounts.entrySet()) {
                    String name = entry.getKey();
                    int count = entry.getValue();

                    TextView submitterItem = new TextView(context);
                    submitterItem.setText("✓ " + name + ": " + count + " פתקים");
                    submitterItem.setTextSize(16);
                    submitterItem.setTextColor(Color.parseColor("#406D3C"));
                    submitterItem.setBackgroundResource(R.drawable.note_item_background);
                    submitterItem.setPadding(30, 20, 30, 20);
                    submitterItem.setGravity(android.view.Gravity.CENTER);
                    submitterItem.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 0, 0, 15);
                    submitterItem.setLayoutParams(params);

                    notesList.addView(submitterItem);
                }
            }
        }
    }

    private Bitmap generateQRCode(String text, int width, int height) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }

    private List<String> parseNotes(String content) {
        // Parse notes separated by commas or newlines (same as SMS logic)
        List<String> notes = new ArrayList<>();
        String[] parts = content.replace("\n", ",").split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                notes.add(trimmed);
            }
        }
        return notes;
    }

    @OnTouch(R.id.collectNotesOnline)
    boolean onTouchCollectOnline(View view, MotionEvent motion) {
        return db.onTouch(context, view, motion);
    }
}