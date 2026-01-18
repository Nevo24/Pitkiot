package nevo_mashiach.pitkiot;

import android.annotation.SuppressLint;
import android.app.Dialog;
import androidx.appcompat.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.TypedValue;
import androidx.preference.PreferenceManager;
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
import java.util.Locale;
import java.util.Set;

import androidx.appcompat.app.AppCompatActivity;
import nevo_mashiach.pitkiot.databinding.ActivityNoteManagementBinding;
import nevo_mashiach.pitkiot.NotActivities.DialogBag;
import nevo_mashiach.pitkiot.NotActivities.NoteCollectionSession;
import nevo_mashiach.pitkiot.NotActivities.db;

import static nevo_mashiach.pitkiot.NotActivities.db.defs;

@SuppressLint("SourceLockedOrientationActivity")
public class NoteManagement extends AppCompatActivity {

    EditText mTypeDef;
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
    // FIX: Synchronize HashMap to prevent concurrent modification from UI thread and Firebase listener thread
    private final java.util.Map<String, java.util.Set<String>> submitterNoteCounts =
        java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<>());
    private static final String FIREBASE_HOSTING_URL = "https://pitkiot-29650.web.app";

    // SharedPreferences keys for persisting collection session state
    private static final String PREF_COLLECTION_ACTIVE = "collectionSessionActive";
    private static final String PREF_COLLECTION_SESSION_ID = "collectionSessionId";
    private static final String PREF_COLLECTION_SHORT_CODE = "collectionShortCode";
    private static final String PREF_COLLECTION_RECEIVED_COUNT = "collectionReceivedCount";
    private static final String PREF_COLLECTION_SUBMITTER_DATA = "collectionSubmitterData";

    // Prevent double-launching activities
    private long lastActivityLaunchTime = 0;
    private static final long ACTIVITY_LAUNCH_COOLDOWN = 500; // milliseconds

    // Toast for showing feedback messages
    private Toast currentToast;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(updateBaseContextLocale(base));
    }

    private Context updateBaseContextLocale(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String language = prefs.getString("app_language", "he");
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            config.setLocales(localeList);
        } else {
            config.locale = locale;
        }

        config.setLayoutDirection(locale);
        return context.createConfigurationContext(config);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize preferences and load saved language
        context = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        spEditor = prefs.edit();
        loadLanguagePreference();

        ActivityNoteManagementBinding binding = ActivityNoteManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        // Initialize view references
        mTypeDef = binding.typeDef;
        mNoteCount = binding.noteCount;
        thisActivity = this;
        dialogBag = new DialogBag(getSupportFragmentManager(), this);



        // Set up click and touch listeners
        // Note: db.onTouch() internally calls view.performClick() for accessibility
        @SuppressLint("ClickableViewAccessibility")
        View.OnTouchListener touchListener = (v, motion) -> db.onTouch(context, v, motion);

        binding.addDef.setOnClickListener(this::addingDefToDb);
        binding.addDef.setOnTouchListener(touchListener);

        binding.deleteNotes.setOnClickListener(this::deleteNodesButton);
        binding.deleteNotes.setOnTouchListener(touchListener);

        binding.collectNotesOnline.setOnClickListener(this::startOnlineNoteCollection);
        binding.collectNotesOnline.setOnTouchListener(touchListener);

        // Handle hint visibility on focus change
        mTypeDef.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    // Clear hint when field gets focus
                    mTypeDef.setHint("");
                } else {
                    // Restore hint when field loses focus and is empty
                    if (mTypeDef.getText().toString().trim().isEmpty()) {
                        mTypeDef.setHint(R.string.hint_enter_character);
                    }
                }
            }
        });

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

        // Apply navigation bar settings after view is attached and insets are available
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                setupTransparentNavigationBar();
            }
        });
    }


    protected void onResume() {
        super.onResume();
        int totalNotes = db.totalNoteAmount();
        String noteCountText = totalNotes == 1
            ? getString(R.string.note_count_database_single)
            : String.format(getString(R.string.note_count_database_plural), totalNotes);
        mNoteCount.setText(noteCountText);

        // Restore collection session if one was active
        restoreCollectionSession();
    }


    @Override
    protected void onPause() {
        super.onPause();
        // Check if there's an active collection session
        boolean isCollectionActive = prefs.getBoolean(PREF_COLLECTION_ACTIVE, false);

        if (isCollectionActive) {
            // Don't dismiss dialog - it will be restored in onResume()
            // Just stop the Firebase listener to save resources
            if (noteCollectionSession != null) {
                noteCollectionSession.stopListening();
            }
        } else {
            // No active collection - dismiss dialog to prevent leaks
            if (collectionDialog != null && collectionDialog.isShowing()) {
                collectionDialog.dismiss();
            }
        }
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



    private boolean add(String name) {
        name = name.trim();
        if (!db.noteExists(name) && !name.isEmpty()) {
            defs.add(name);
            saveNotes();
            int totalNotes = db.totalNoteAmount();
            String noteCountText = totalNotes == 1
                ? getString(R.string.note_count_database_single)
                : String.format(getString(R.string.note_count_database_plural), totalNotes);
            mNoteCount.setText(noteCountText);
            return true;
        }
        return false;
    }

    private void saveNotes() {
        //save notes to shared preferences
        Set<String> set = new HashSet<String>(db.defs);
        spEditor.putStringSet("defs", set);
        spEditor.apply();
    }


    //*********************** ON CLICKS ********************************
    public void addingDefToDb(View view) {
        String input = mTypeDef.getText().toString();
        List<String> notes = parseNotes(input);
        int addedCount = 0;
        for (String note : notes) {
            if (add(note)) {
                addedCount++;
            }
        }
        mTypeDef.setText("");
        // Restore hint after adding
        mTypeDef.setHint(R.string.hint_enter_character);

        // Show toast notification if notes were added
        if (addedCount > 0) {
            String message = addedCount == 1
                ? getString(R.string.toast_notes_added_single)
                : String.format(getString(R.string.toast_notes_added_plural), addedCount);
            showToast(message);
        }
    }


    public void deleteNodesButton(View view) {
        // Prevent double-launching within cooldown period
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastActivityLaunchTime < ACTIVITY_LAUNCH_COOLDOWN) {
            android.util.Log.d("NoteManagement", "Activity launch ignored - too soon after previous launch");
            return;
        }
        lastActivityLaunchTime = currentTime;

        Intent intent = new Intent(context, NoteList.class);
        startActivity(intent);
    }


    public void startOnlineNoteCollection(View view) {
        // Prevent double-opening if dialog is already showing
        if (collectionDialog != null && collectionDialog.isShowing()) {
            android.util.Log.d("NoteManagement", "Dialog already showing, ignoring duplicate call");
            return;
        }

        // Check if there are existing notes in the repository
        int existingNotesCount = db.defs.size() + db.temp.size();
        for (int i = 0; i < 24; i++) {
            existingNotesCount += db.teamsNotes[i].size();
        }

        // If there are existing notes, show warning dialog
        if (existingNotesCount > 0) {
            showExistingNotesWarning(existingNotesCount);
            return;
        }

        // Continue with collection if no existing notes
        continueOnlineNoteCollection();
    }

    private void continueOnlineNoteCollection() {
        // Reset counters for new session
        receivedNotesCount = 0;
        // FIX: Synchronize clear() to prevent concurrent modification
        synchronized (submitterNoteCounts) {
            submitterNoteCounts.clear();
        }

        // Create a new collection session every time
        noteCollectionSession = new NoteCollectionSession(context);
        String shortCode = noteCollectionSession.createSession();
        String currentLang = prefs.getString("app_language", "he");
        String url = noteCollectionSession.getSubmissionUrl(FIREBASE_HOSTING_URL) + "&lang=" + currentLang;

        // Save initial state to SharedPreferences
        saveCollectionState();

        // Show the collection dialog
        showNoteCollectionDialog(shortCode, url);

        // Start listening for incoming notes
        startCollectionListener();
    }

    private void showExistingNotesWarning(int existingNotesCount) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.warning_existing_notes_title));
        builder.setMessage(String.format(getString(R.string.warning_existing_notes_message), existingNotesCount));

        // Continue Collecting button
        builder.setPositiveButton(getString(R.string.button_continue_collecting), (dialog, which) -> {
            dialog.dismiss();
            continueOnlineNoteCollection();
        });

        // Go Back button
        builder.setNegativeButton(getString(R.string.button_go_back), (dialog, which) -> {
            dialog.dismiss();
        });

        builder.setCancelable(false);
        AlertDialog dialog = builder.create();

        // Get app language from SharedPreferences
        String appLanguage = prefs.getString("app_language", "he");
        boolean isRTL = appLanguage.equals("he");

        // Set dialog window layout direction
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            if (isRTL) {
                window.getDecorView().setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
            } else {
                window.getDecorView().setLayoutDirection(android.view.View.LAYOUT_DIRECTION_LTR);
            }
        }

        // Make the positive button more prominent with bold dark green text
        dialog.setOnShowListener(dialogInterface -> {
            // Set text alignment for title and message
            int textGravity = isRTL ? android.view.Gravity.RIGHT : android.view.Gravity.LEFT;

            // Find and set gravity for title TextView
            int titleId = context.getResources().getIdentifier("alertTitle", "id", "android");
            if (titleId > 0) {
                android.widget.TextView titleView = dialog.findViewById(titleId);
                if (titleView != null) {
                    titleView.setGravity(textGravity | android.view.Gravity.CENTER_VERTICAL);
                }
            }

            // Find and set gravity for message TextView
            android.widget.TextView messageView = dialog.findViewById(android.R.id.message);
            if (messageView != null) {
                messageView.setGravity(textGravity);
            }

            // Set button panel gravity using absolute positioning
            int buttonPanelId = context.getResources().getIdentifier("buttonPanel", "id", "android");
            if (buttonPanelId > 0) {
                android.view.View buttonPanel = dialog.findViewById(buttonPanelId);
                if (buttonPanel != null && buttonPanel instanceof android.widget.LinearLayout) {
                    android.widget.LinearLayout buttonPanelLayout = (android.widget.LinearLayout) buttonPanel;
                    int gravity = isRTL ? android.view.Gravity.RIGHT : android.view.Gravity.LEFT;
                    buttonPanelLayout.setGravity(gravity);
                }
            }

            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            if (positiveButton != null) {
                // Get the dark green color from colorAccent
                int darkGreen = getResources().getColor(R.color.colorAccent);

                // Bold text for emphasis (Material 3 Expressive)
                positiveButton.setTypeface(null, Typeface.BOLD);

                // Slightly larger text size for primary action
                positiveButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

                // Dark green text color (no background)
                positiveButton.setTextColor(darkGreen);

                // Add glowing effect using shadow layer
                // Parameters: radius, dx, dy, color
                positiveButton.setShadowLayer(8, 0, 0, darkGreen);

                positiveButton.setAllCaps(false);
            }
            if (negativeButton != null) {
                negativeButton.setAllCaps(false);
                // Keep default style - no background, just text
            }
        });

        // Check lifecycle before showing dialog
        if (!isFinishing() && !isDestroyed()) {
            dialog.show();
        }
    }

    private void showNoteCollectionDialog(String sessionId, String url) {
        // Create dialog
        collectionDialog = new Dialog(context);
        // Passing null is acceptable for dialog layouts - the dialog window itself is the parent
        @SuppressLint("InflateParams")
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_note_collection, null);
        collectionDialog.setContentView(dialogView);
        collectionDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        collectionDialog.setCancelable(false); // Prevent automatic dismissal on back press

        // Set dialog layout direction based on app language
        String appLanguage = prefs.getString("app_language", "he");
        boolean isRTL = appLanguage.equals("he");
        android.view.Window window = collectionDialog.getWindow();
        if (window != null) {
            if (isRTL) {
                window.getDecorView().setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
            } else {
                window.getDecorView().setLayoutDirection(android.view.View.LAYOUT_DIRECTION_LTR);
            }
        }

        // Handle back button press
        collectionDialog.setOnKeyListener((dialog, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.getAction() == android.view.KeyEvent.ACTION_UP) {
                Runnable closeTask = () -> {
                    if (noteCollectionSession != null) {
                        noteCollectionSession.endSession();
                    }
                    collectionDialog.dismiss();
                    receivedNotesCount = 0;
                    // FIX: Synchronize clear() to prevent concurrent modification
                    synchronized (submitterNoteCounts) {
                        submitterNoteCounts.clear();
                    }
                    // Clear persisted state when intentionally closing via back button
                    clearCollectionPersistence();
                };
                dialogBag.confirmCloseCollection(closeTask);
                return true;
            }
            return false;
        });

        // Setup dialog views
        TextView sessionUrlText = dialogView.findViewById(R.id.sessionUrlText);
        ImageView qrCodeImage = dialogView.findViewById(R.id.qrCodeImage);
        nevo_mashiach.pitkiot.NotActivities.MyButton copyUrlButton = dialogView.findViewById(R.id.copyUrlButton);
        nevo_mashiach.pitkiot.NotActivities.MyButton closeSessionButton = dialogView.findViewById(R.id.closeSessionButton);
        TextView receivedNotesCountText = dialogView.findViewById(R.id.receivedNotesCount);

        // Set values
        sessionUrlText.setText(url);
        receivedNotesCountText.setText(getString(R.string.received_notes_count_zero));

        // Make URL clickable
        sessionUrlText.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
            startActivity(browserIntent);
        });

        // Generate QR code
        try {
            Bitmap qrBitmap = generateQRCode(url);
            qrCodeImage.setImageBitmap(qrBitmap);
        } catch (WriterException e) {
            showToast(getString(R.string.toast_qr_error));
        }

        // Copy URL button
        copyUrlButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Pitkiot URL", url);
            clipboard.setPrimaryClip(clip);
            showToast(getString(R.string.toast_link_copied));
        });
        @SuppressLint("ClickableViewAccessibility")
        View.OnTouchListener touchListener = (v, motion) -> db.onTouch(context, v, motion);
        copyUrlButton.setOnTouchListener(touchListener);

        // Close button
        closeSessionButton.setOnClickListener(v -> {
            if (noteCollectionSession != null) {
                noteCollectionSession.endSession();
            }

            // Calculate total unique notes before clearing
            // FIX: Synchronize access to prevent concurrent modification
            int totalUniqueNotes;
            synchronized (submitterNoteCounts) {
                java.util.Set<String> allUniqueNotes = new java.util.HashSet<>();
                for (java.util.Set<String> submitterNotes : submitterNoteCounts.values()) {
                    allUniqueNotes.addAll(submitterNotes);
                }
                totalUniqueNotes = allUniqueNotes.size();
            }

            collectionDialog.dismiss();
            String message = totalUniqueNotes == 1
                ? getString(R.string.toast_notes_added_single)
                : String.format(getString(R.string.toast_notes_added_plural), totalUniqueNotes);
            showToast(message);
            receivedNotesCount = 0;
            // FIX: Synchronize clear() to prevent concurrent modification
            synchronized (submitterNoteCounts) {
                submitterNoteCounts.clear();
            }
            // Clear persisted state when intentionally closing via "Finish and Save"
            clearCollectionPersistence();
        });
        closeSessionButton.setOnTouchListener(touchListener);

        // Check lifecycle before showing dialog
        if (!isFinishing() && !isDestroyed()) {
            collectionDialog.show();
        }
    }

    private void updateCollectionDialogUI(String submitterName, String noteContent) {
        if (collectionDialog != null && collectionDialog.isShowing()) {
            View dialogView = collectionDialog.findViewById(R.id.receivedNotesList);
            TextView countText = collectionDialog.findViewById(R.id.receivedNotesCount);

            if (dialogView == null || countText == null) {
                android.util.Log.e("NoteManagement", "Dialog views not found!");
                return;
            }

            // Count notes in this submission
            List<String> notes = parseNotes(noteContent);
            int notesInSubmission = notes.size();

            // Update unique notes for this submitter
            // FIX: Synchronize access to prevent concurrent modification
            int totalPlayers;
            int totalUniqueNotes;
            synchronized (submitterNoteCounts) {
                java.util.Set<String> currentNotes = submitterNoteCounts.get(submitterName);
                if (currentNotes == null) {
                    currentNotes = new java.util.HashSet<>();
                }
                currentNotes.addAll(notes);
                submitterNoteCounts.put(submitterName, currentNotes);

                // Update total count display (total unique notes and total players)
                totalPlayers = submitterNoteCounts.size();

                // Calculate total unique notes across all submitters
                java.util.Set<String> allUniqueNotes = new java.util.HashSet<>();
                for (java.util.Set<String> submitterNotes : submitterNoteCounts.values()) {
                    allUniqueNotes.addAll(submitterNotes);
                }
                totalUniqueNotes = allUniqueNotes.size();
            }

            // Set the appropriate text based on count
            if (totalUniqueNotes == 0) {
                countText.setText(getString(R.string.received_notes_count_zero));
            } else if (totalUniqueNotes == 1 && totalPlayers == 1) {
                countText.setText(getString(R.string.received_notes_count_single));
            } else if (totalPlayers == 1) {
                countText.setText(String.format(getString(R.string.received_notes_count_one_player), totalUniqueNotes));
            } else {
                countText.setText(String.format(getString(R.string.received_notes_count), totalUniqueNotes, totalPlayers));
            }

            android.util.Log.d("NoteManagement", "Updated count: " + totalUniqueNotes + " unique notes from " + totalPlayers + " people");

            // Rebuild the submitters list
            if (dialogView instanceof LinearLayout) {
                LinearLayout notesList = (LinearLayout) dialogView;

                // Clear all views
                notesList.removeAllViews();

                // Add a horizontal layout for each submitter with BiDi-safe text arrangement
                // FIX: Create a copy of entries to prevent concurrent modification during iteration
                java.util.List<java.util.Map.Entry<String, java.util.Set<String>>> entriesCopy;
                synchronized (submitterNoteCounts) {
                    entriesCopy = new java.util.ArrayList<>(submitterNoteCounts.entrySet());
                }
                for (java.util.Map.Entry<String, java.util.Set<String>> entry : entriesCopy) {
                    String name = entry.getKey();
                    int count = entry.getValue().size();

                    // Create horizontal container
                    LinearLayout itemContainer = new LinearLayout(context);
                    itemContainer.setOrientation(LinearLayout.HORIZONTAL);
                    itemContainer.setGravity(android.view.Gravity.CENTER);
                    itemContainer.setBackgroundResource(R.drawable.note_item_background);
                    itemContainer.setPadding(30, 20, 30, 20);
                    LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    containerParams.setMargins(0, 0, 0, 15);
                    itemContainer.setLayoutParams(containerParams);

                    // Check the game language mode (not the name language)
                    String gameLanguage = prefs.getString("app_language", "he");
                    boolean isGameInHebrew = gameLanguage.equals("he");

                    // Check if name is entirely Hebrew (no Latin characters) - for layout direction
                    boolean isNameHebrew = name.matches("[\\u0590-\\u05FF\\s]+");

                    // Special case: detect mixed Hebrew and numbers
                    boolean hasHebrewChars = name.matches(".*[\\u0590-\\u05FF].*");
                    boolean hasNumbers = name.matches(".*\\d.*");
                    boolean isMixedHebrewAndNumbers = hasHebrewChars && hasNumbers;

                    // Set layout direction based on name language
                    if (isNameHebrew || isMixedHebrewAndNumbers) {
                        itemContainer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                    } else {
                        itemContainer.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                    }

                    // Part 1: Checkmark (always LTR, separate from name)
                    TextView checkmarkPart = new TextView(context);
                    checkmarkPart.setText("✓");
                    checkmarkPart.setTextSize(16);
                    checkmarkPart.setTextColor(Color.parseColor("#406D3C"));
                    checkmarkPart.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    checkmarkPart.setTextDirection(TextView.TEXT_DIRECTION_LTR);
                    // Add margin to create space between checkmark and name
                    LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    if (isNameHebrew || isMixedHebrewAndNumbers) {
                        checkParams.setMarginEnd(10); // Space on the left side (visual) in RTL
                    } else {
                        checkParams.setMarginEnd(10); // Space on the right side in LTR
                    }
                    checkmarkPart.setLayoutParams(checkParams);

                    itemContainer.addView(checkmarkPart);

                    if (isGameInHebrew) {
                        // Hebrew game mode: display as "✓ name: X פתקים"
                        TextView namePart = new TextView(context);

                        // Special handling for mixed Hebrew and numbers
                        if (isMixedHebrewAndNumbers) {
                            // Use RLE (Right-to-Left Embedding) and PDF (Pop Directional Formatting)
                            // to force the name to display as a single RTL unit with proper ordering
                            namePart.setText("\u202B" + name + "\u202C: ");
                            namePart.setTextDirection(TextView.TEXT_DIRECTION_RTL);
                        } else {
                            namePart.setText(name + ": ");
                            namePart.setTextDirection(isNameHebrew ? TextView.TEXT_DIRECTION_RTL : TextView.TEXT_DIRECTION_FIRST_STRONG);
                        }

                        namePart.setTextSize(16);
                        namePart.setTextColor(Color.parseColor("#406D3C"));
                        namePart.setGravity(android.view.Gravity.CENTER_VERTICAL);
                        itemContainer.addView(namePart);

                        TextView countPart = new TextView(context);
                        // Hebrew game mode: number first, then word
                        if (count == 1) {
                            countPart.setText("פתק אחד");
                        } else {
                            countPart.setText(count + " " + getString(R.string.notes_word));
                        }
                        countPart.setTextSize(16);
                        countPart.setTextColor(Color.parseColor("#406D3C"));
                        countPart.setGravity(android.view.Gravity.CENTER_VERTICAL);
                        countPart.setTextDirection(TextView.TEXT_DIRECTION_RTL);
                        itemContainer.addView(countPart);
                    } else {
                        // English game mode: display as "✓ name: 2 notes"
                        TextView namePart = new TextView(context);
                        namePart.setText(name);
                        namePart.setTextSize(16);
                        namePart.setTextColor(Color.parseColor("#406D3C"));
                        namePart.setGravity(android.view.Gravity.CENTER_VERTICAL);
                        namePart.setTextDirection(isNameHebrew ? TextView.TEXT_DIRECTION_RTL : TextView.TEXT_DIRECTION_LTR);
                        itemContainer.addView(namePart);

                        TextView colonPart = new TextView(context);
                        colonPart.setText(": ");
                        colonPart.setTextSize(16);
                        colonPart.setTextColor(Color.parseColor("#406D3C"));
                        colonPart.setGravity(android.view.Gravity.CENTER_VERTICAL);
                        itemContainer.addView(colonPart);

                        TextView countPart = new TextView(context);
                        // English game mode: number first, then word
                        if (count == 1) {
                            countPart.setText("one note");
                        } else {
                            countPart.setText(count + " " + getString(R.string.notes_word));
                        }
                        countPart.setTextSize(16);
                        countPart.setTextColor(Color.parseColor("#406D3C"));
                        countPart.setGravity(android.view.Gravity.CENTER_VERTICAL);
                        countPart.setTextDirection(TextView.TEXT_DIRECTION_LTR);
                        itemContainer.addView(countPart);
                    }

                    notesList.addView(itemContainer);
                }
            }
        }
    }

    private Bitmap generateQRCode(String text) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 500, 500);

        Bitmap bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.RGB_565);
        for (int x = 0; x < 500; x++) {
            for (int y = 0; y < 500; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }

    private List<String> parseNotes(String content) {
        // Parse notes separated by commas or newlines
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

    private void showToast(String message) {
        // Cancel any existing toast to prevent queueing
        if (currentToast != null) {
            currentToast.cancel();
        }
        currentToast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        currentToast.show();
    }

    boolean onTouchCollectOnline(View view, MotionEvent motion) {
        return db.onTouch(context, view, motion);
    }

    private void loadLanguagePreference() {
        String language = prefs.getString("app_language", "he");
        setLocale(language);
    }

    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration config = resources.getConfiguration();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            config.setLocale(locale);
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            config.setLocales(localeList);
        } else {
            config.locale = locale;
        }

        config.setLayoutDirection(locale);

        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    private void setupTransparentNavigationBar() {
        // Check if we should extend content behind the navigation bar
        if (shouldExtendBehindNavigationBar()) {
            // For gesture navigation: set transparent and extend content behind
            getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        } else {
            // For 2-button & 3-button navigation: use system default color, content stops above
            // Don't set color - let system handle it
        }
    }

    private boolean shouldExtendBehindNavigationBar() {
        // Use system gesture insets to distinguish between navigation modes:
        // - Gesture navigation: has left/right gesture insets (edge back gesture) → EXTEND
        // - 2-button navigation: no gesture insets, thin bar (~24-30dp) → DON'T EXTEND
        // - 3-button navigation: no gesture insets, tall bar (~48dp) → DON'T EXTEND

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.view.WindowInsets insets = getWindow().getDecorView().getRootWindowInsets();
            if (insets != null) {
                // Get system gesture insets (for edge back gestures)
                int gestureLeft = insets.getInsets(android.view.WindowInsets.Type.systemGestures()).left;
                int gestureRight = insets.getInsets(android.view.WindowInsets.Type.systemGestures()).right;
                boolean hasGestureInsets = gestureLeft > 0 || gestureRight > 0;

                // Only extend behind bar for TRUE gesture navigation (has gesture insets)
                return hasGestureInsets;
            }
        }
        // Fallback for older Android versions
        // Use height-based detection: only extend if very thin (< 20dp = likely gesture)
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            int navBarHeight = getResources().getDimensionPixelSize(resourceId);
            float density = getResources().getDisplayMetrics().density;
            int navBarHeightDp = (int) (navBarHeight / density);
            return navBarHeightDp < 20; // Only very thin bars (gesture)
        }
        return false;
    }

    /**
     * Starts the Firebase listener for the current collection session
     */
    private void startCollectionListener() {
        // FIX: Add defensive check and logging
        if (noteCollectionSession == null) {
            android.util.Log.e("NoteManagement", "Cannot start listener: noteCollectionSession is null");
            return;
        }

        noteCollectionSession.startListening(new NoteCollectionSession.OnNoteReceivedListener() {
            @Override
            public void onNoteReceived(String submitterName, String noteContent) {
                android.util.Log.d("NoteManagement", "Note received from " + submitterName + ": " + noteContent);
                runOnUiThread(() -> {
                    // Parse and add notes from submission
                    List<String> notes = parseNotes(noteContent);
                    android.util.Log.d("NoteManagement", "Parsed " + notes.size() + " notes");

                    // Count unique notes in this submission
                    java.util.Set<String> uniqueNotes = new java.util.HashSet<>(notes);
                    int uniqueCount = uniqueNotes.size();

                    // Add notes to database
                    for (String note : notes) {
                        add(note);
                    }

                    // Update the dialog UI and persist state
                    receivedNotesCount += notes.size();
                    android.util.Log.d("NoteManagement", "Total notes received: " + receivedNotesCount);
                    updateCollectionDialogUI(submitterName, noteContent);
                    saveCollectionState();

                    // Show toast
                    if (uniqueCount > 0) {
                        String message = uniqueCount == 1
                            ? String.format(getString(R.string.toast_new_note_received_single), submitterName)
                            : String.format(getString(R.string.toast_new_note_received_plural), uniqueCount, submitterName);
                        showToast(message);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showToast(String.format(getString(R.string.toast_error), error));
                    // Clear stale session data on error
                    clearCollectionPersistence();
                });
            }
        });
    }

    /**
     * Clears all persisted collection session data from SharedPreferences
     */
    private void clearCollectionPersistence() {
        spEditor.remove(PREF_COLLECTION_ACTIVE);
        spEditor.remove(PREF_COLLECTION_SESSION_ID);
        spEditor.remove(PREF_COLLECTION_SHORT_CODE);
        spEditor.remove(PREF_COLLECTION_RECEIVED_COUNT);
        spEditor.remove(PREF_COLLECTION_SUBMITTER_DATA);
        spEditor.apply();
        android.util.Log.d("NoteManagement", "Cleared collection persistence");
    }

    /**
     * Restores a previously active collection session from SharedPreferences
     */
    private void restoreCollectionSession() {
        // Check if there's an active session to restore
        boolean isActive = prefs.getBoolean(PREF_COLLECTION_ACTIVE, false);
        if (!isActive) {
            return;
        }

        // If dialog is already showing, just restart the listener
        if (collectionDialog != null && collectionDialog.isShowing()) {
            android.util.Log.d("NoteManagement", "Dialog already showing, restarting listener");
            // FIX: Ensure noteCollectionSession exists before starting listener
            if (noteCollectionSession != null) {
                startCollectionListener();
            } else {
                android.util.Log.e("NoteManagement", "noteCollectionSession is null, cannot restart listener");
                // Session is invalid, clear persistence and dismiss dialog
                clearCollectionPersistence();
                collectionDialog.dismiss();
            }
            return;
        }

        String sessionId = prefs.getString(PREF_COLLECTION_SESSION_ID, null);
        String shortCode = prefs.getString(PREF_COLLECTION_SHORT_CODE, null);

        if (sessionId == null || shortCode == null) {
            android.util.Log.e("NoteManagement", "Invalid persisted session data, clearing");
            clearCollectionPersistence();
            return;
        }

        android.util.Log.d("NoteManagement", "Restoring collection session: " + sessionId);

        // Restore counters and submitter data
        receivedNotesCount = prefs.getInt(PREF_COLLECTION_RECEIVED_COUNT, 0);

        // FIX: Synchronize access and add validation for restored data
        synchronized (submitterNoteCounts) {
            submitterNoteCounts.clear();

            Set<String> submitterData = prefs.getStringSet(PREF_COLLECTION_SUBMITTER_DATA, new HashSet<>());
            // FIX: Check for null and empty strings
            if (submitterData == null || submitterData.isEmpty()) {
                android.util.Log.w("NoteManagement", "No submitter data to restore");
                return;
            }

            for (String entry : submitterData) {
                // FIX: Validate entry is not null or empty
                if (entry == null || entry.trim().isEmpty()) {
                    android.util.Log.w("NoteManagement", "Skipping null or empty entry");
                    continue;
                }

                // Parse format: "submitterName||note1,,note2,,note3"
                // FIX CORRECTION: Use regex that handles escaped delimiters properly
                // Split on || that is not preceded by backslash
                String[] parts = entry.split("(?<!\\\\)\\|\\|", 2);
                if (parts.length == 2) {
                    // Unescape the name: reverse order (delimiters first, then backslashes)
                    String name = parts[0].replace("\\||", "||").replace("\\,,", ",,").replace("\\\\", "\\");
                    // FIX: Validate name is not null or empty
                    if (name == null || name.trim().isEmpty()) {
                        android.util.Log.w("NoteManagement", "Skipping entry with empty name");
                        continue;
                    }

                    // Split notes by ,, that is not preceded by backslash
                    String[] notes = parts[1].split("(?<!\\\\),,");
                    Set<String> noteSet = new HashSet<>();
                    for (String note : notes) {
                        // Unescape: reverse order (delimiters first, then backslashes)
                        String unescapedNote = note.replace("\\||", "||").replace("\\,,", ",,").replace("\\\\", "\\");
                        if (unescapedNote != null && !unescapedNote.isEmpty()) {
                            noteSet.add(unescapedNote);
                        }
                    }
                    submitterNoteCounts.put(name, noteSet);
                }
            }
        }

        // Recreate session with existing session ID
        noteCollectionSession = new NoteCollectionSession(context, sessionId);
        String currentLang = prefs.getString("app_language", "he");
        String url = noteCollectionSession.getSubmissionUrl(FIREBASE_HOSTING_URL) + "&lang=" + currentLang;

        // Show the collection dialog with restored state
        showNoteCollectionDialog(shortCode, url);

        // Update dialog UI with restored submitter data
        // FIX: Synchronize access and add null checks
        synchronized (submitterNoteCounts) {
            if (receivedNotesCount > 0 && !submitterNoteCounts.isEmpty()) {
                // Get any submitter's first note to trigger UI rebuild
                // This will rebuild the entire list from submitterNoteCounts map
                java.util.Set<String> keys = submitterNoteCounts.keySet();
                if (keys != null && !keys.isEmpty()) {
                    String anyName = keys.iterator().next();
                    Set<String> anyNotes = submitterNoteCounts.get(anyName);
                    if (anyNotes != null && !anyNotes.isEmpty()) {
                        String anyNote = anyNotes.iterator().next();
                        if (anyNote != null) {
                            updateCollectionDialogUI(anyName, anyNote);
                        }
                    }
                }
            }
        }

        // Resume listening for new notes
        startCollectionListener();
    }

    /**
     * Saves the current collection session state to SharedPreferences
     */
    private void saveCollectionState() {
        spEditor.putBoolean(PREF_COLLECTION_ACTIVE, true);

        if (noteCollectionSession != null) {
            spEditor.putString(PREF_COLLECTION_SESSION_ID, noteCollectionSession.getSessionId());
            spEditor.putString(PREF_COLLECTION_SHORT_CODE, noteCollectionSession.getShortCode());
        }

        spEditor.putInt(PREF_COLLECTION_RECEIVED_COUNT, receivedNotesCount);

        // Serialize submitterNoteCounts to StringSet format with escaping
        // FIX CORRECTION: Proper escaping - escape backslashes first, then delimiters
        Set<String> serializedData = new HashSet<>();
        synchronized (submitterNoteCounts) {
            for (java.util.Map.Entry<String, java.util.Set<String>> entry : submitterNoteCounts.entrySet()) {
                String name = entry.getKey();
                // Proper escaping: backslash first, then delimiters
                String escapedName = name.replace("\\", "\\\\").replace("||", "\\||").replace(",,", "\\,,");

                // Escape notes as well
                java.util.Set<String> escapedNotes = new java.util.HashSet<>();
                for (String note : entry.getValue()) {
                    if (note != null && !note.isEmpty()) {
                        String escapedNote = note.replace("\\", "\\\\").replace("||", "\\||").replace(",,", "\\,,");
                        escapedNotes.add(escapedNote);
                    }
                }

                String notesJoined = String.join(",,", escapedNotes);
                serializedData.add(escapedName + "||" + notesJoined);
            }
        }
        spEditor.putStringSet(PREF_COLLECTION_SUBMITTER_DATA, serializedData);

        spEditor.apply();
        android.util.Log.d("NoteManagement", "Saved collection state");
    }
}