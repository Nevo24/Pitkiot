package nevo_mashiach.pitkiot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import androidx.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.RelativeLayout;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import nevo_mashiach.pitkiot.databinding.ActivitySettingsBinding;
import nevo_mashiach.pitkiot.NotActivities.DialogBag;
import nevo_mashiach.pitkiot.NotActivities.MyEditText;
import nevo_mashiach.pitkiot.NotActivities.db;

public class Settings extends AppCompatActivity {

    public DialogBag dialogBag;
    Context context;

    CheckBox mAutoBalaceCheckBox;
    CheckBox mSoundCheckBox;
    MyEditText mRoundTimeValue;
    MyEditText mPassTimeValue;
    TextView mAmountOfTeams;
    TextView mTeamEditableScore;
    TextView mBalanceExplanation;
    TextView mRoundTimeConstraint;
    TextView mPassTimeConstraint;
    TextView mTeamsConstraint;
    TextView mTeam1Text;
    TextView mEditScoreHeadline;
    TextView mTextView1;
    TextView mTeam1Headline;

    Button mIncrease1;
    Button mDecrease1;
    Button mDecrease2;
    Button mIncreaseRoundTime;
    Button mDecreaseRoundTime;
    Button mIncreasePassTime;
    Button mDecreasePassTime;

    int num;
    int selectedSpinner = 1;
    SharedPreferences prefs;
    SharedPreferences.Editor spEditor;

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
        ActivitySettingsBinding binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize view references
        mAutoBalaceCheckBox = binding.autoBalaceCheckBox;
        mSoundCheckBox = binding.soundCheckBox;
        mRoundTimeValue = binding.roundTimeValue;
        mPassTimeValue = binding.passTimeValue;
        mAmountOfTeams = binding.amoutOfTeams;
        mTeamEditableScore = binding.teamEditableScore;
        mBalanceExplanation = binding.balanceExplanation;
        mRoundTimeConstraint = binding.roundTimeConstraint;
        mPassTimeConstraint = binding.passTimeConstraint;
        mTeamsConstraint = binding.teamsConstraint;
        mTeam1Text = binding.team1Text;
        mEditScoreHeadline = binding.editScoreHeadline;
        mTextView1 = binding.textView1;
        mTeam1Headline = binding.team1Headline;
        mIncrease1 = binding.increase1;
        mDecrease1 = binding.decrease1;
        mDecrease2 = binding.decrease2;
        mIncreaseRoundTime = binding.increaseRoundTime;
        mDecreaseRoundTime = binding.decreaseRoundTime;
        mIncreasePassTime = binding.increasePassTime;
        mDecreasePassTime = binding.decreasePassTime;

        // Set up listeners
        mAutoBalaceCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> onAutoBalanceChecked(isChecked));
        mSoundCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> onSoundChecked(isChecked));
        mBalanceExplanation.setOnClickListener(this::explanationBalanceDialog);
        binding.resetSettings.setOnClickListener(this::resetAllSettings);
        mIncrease1.setOnClickListener(this::increaseTeam1Score);
        binding.increase2.setOnClickListener(this::increaseTeam2Score);
        mDecrease1.setOnClickListener(this::decreaseTeam1Score);
        mDecrease2.setOnClickListener(this::decreaseTeam2Score);
        mIncreaseRoundTime.setOnClickListener(this::increaseRoundTime);
        mDecreaseRoundTime.setOnClickListener(this::decreaseRoundTime);
        mIncreasePassTime.setOnClickListener(this::increasePassTime);
        mDecreasePassTime.setOnClickListener(this::decreasePassTime);

        // Set up focus listeners for direct editing
        mRoundTimeValue.setOnFocusChangeListener((v, hasFocus) -> focusChangedRoundTimeValue(hasFocus));
        mPassTimeValue.setOnFocusChangeListener((v, hasFocus) -> focusChangedPassTimeValue(hasFocus));
        mAmountOfTeams.setOnFocusChangeListener((v, hasFocus) -> focusChangedAmountOfTeams(hasFocus));

        // Set up editor action listeners to close keyboard on done
        TextView.OnEditorActionListener doneListener = (v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        };
        mRoundTimeValue.setOnEditorActionListener(doneListener);
        mPassTimeValue.setOnEditorActionListener(doneListener);
        mAmountOfTeams.setOnEditorActionListener(doneListener);

        // Set up input filters to prevent invalid values during typing
        mRoundTimeValue.setFilters(new android.text.InputFilter[] { new InputFilterMinMax(1, 300) });
        mPassTimeValue.setFilters(new android.text.InputFilter[] { new InputFilterMinMax(1, 300) });
        mAmountOfTeams.setFilters(new android.text.InputFilter[] { new InputFilterMinMax(2, 24) });

        // Set up touch listeners - Note: db.onTouch()/onTouchExplanation() internally call view.performClick()
        @SuppressLint("ClickableViewAccessibility")
        View.OnTouchListener resetTouchListener = (v, motion) -> db.onTouch(context, v, motion);
        binding.resetSettings.setOnTouchListener(resetTouchListener);

        @SuppressLint("ClickableViewAccessibility")
        View.OnTouchListener explanationTouchListener = (v, motion) -> db.onTouchExplanation(context, v, motion);
        // MyTextView overrides performClick() and db.onTouchExplanation() calls it - this is a lint false positive
        //noinspection AndroidLintClickableViewAccessibility
        mBalanceExplanation.setOnTouchListener(explanationTouchListener);
        
        context = this;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        dialogBag = new DialogBag(getSupportFragmentManager(), this);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        spEditor = prefs.edit();

        // Set checkbox and button positions for language
        String language = prefs.getString("app_language", "he");
        RelativeLayout.LayoutParams checkboxParams1 = (RelativeLayout.LayoutParams) mAutoBalaceCheckBox.getLayoutParams();
        RelativeLayout.LayoutParams checkboxParams2 = (RelativeLayout.LayoutParams) mSoundCheckBox.getLayoutParams();
        RelativeLayout.LayoutParams buttonParams = (RelativeLayout.LayoutParams) mBalanceExplanation.getLayoutParams();

        if (language.equals("en")) {
            // In English: checkboxes LEFT, button to the RIGHT of checkbox
            checkboxParams1.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            checkboxParams1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

            checkboxParams2.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            checkboxParams2.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

            // Button positioned to the right of checkbox
            buttonParams.removeRule(RelativeLayout.LEFT_OF);
            buttonParams.removeRule(RelativeLayout.START_OF);
            buttonParams.addRule(RelativeLayout.RIGHT_OF, R.id.autoBalaceCheckBox);
            buttonParams.addRule(RelativeLayout.END_OF, R.id.autoBalaceCheckBox);
            // Left margin after checkbox
            buttonParams.setMargins(
                (int) (8 * getResources().getDisplayMetrics().density), // left
                (int) (-8 * getResources().getDisplayMetrics().density), // top (negative to move up)
                0, // right
                0  // bottom
            );
        } else {
            // In Hebrew: checkboxes RIGHT, button to the LEFT of checkbox (default from XML)
            // Right margin before checkbox
            buttonParams.setMargins(
                0, // left
                (int) (-8 * getResources().getDisplayMetrics().density), // top (negative to move up)
                (int) (8 * getResources().getDisplayMetrics().density), // right
                0  // bottom
            );
        }

        mAutoBalaceCheckBox.setLayoutParams(checkboxParams1);
        mSoundCheckBox.setLayoutParams(checkboxParams2);
        mBalanceExplanation.setLayoutParams(buttonParams);

        Typeface tf = Typeface.createFromAsset(getAssets(), "gan.ttf");
        mAutoBalaceCheckBox.setTypeface(tf);
        mSoundCheckBox.setTypeface(tf);

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



    @Override
    protected void onResume() {
        super.onResume();

        // Adjust checkbox and button positions based on locale
        String language = prefs.getString("app_language", "he");
        boolean isHebrew = language.equals("he");

        androidx.percentlayout.widget.PercentRelativeLayout.LayoutParams checkboxParams =
            (androidx.percentlayout.widget.PercentRelativeLayout.LayoutParams) mAutoBalaceCheckBox.getLayoutParams();
        androidx.percentlayout.widget.PercentRelativeLayout.LayoutParams soundParams =
            (androidx.percentlayout.widget.PercentRelativeLayout.LayoutParams) mSoundCheckBox.getLayoutParams();
        androidx.percentlayout.widget.PercentRelativeLayout.LayoutParams buttonParams =
            (androidx.percentlayout.widget.PercentRelativeLayout.LayoutParams) mBalanceExplanation.getLayoutParams();

        // Get layout params for controls and constraints
        View roundTimeControls = findViewById(R.id.roundTimeControls);
        View passTimeControls = findViewById(R.id.passTimeControls);
        View team1Controls = findViewById(R.id.team1Controls);
        View team2Controls = findViewById(R.id.team2Controls);
        Spinner settingsSpinner = findViewById(R.id.settingsSpinner);
        View topLayout = findViewById(R.id.topLayout);
        View bottomLayout = findViewById(R.id.bottomLayout);

        // Elements inside middleContentContainer LinearLayout use LinearLayout.LayoutParams
        android.widget.LinearLayout.LayoutParams roundTimeControlsParams =
            (android.widget.LinearLayout.LayoutParams) roundTimeControls.getLayoutParams();
        android.widget.LinearLayout.LayoutParams passTimeControlsParams =
            (android.widget.LinearLayout.LayoutParams) passTimeControls.getLayoutParams();
        android.widget.LinearLayout.LayoutParams roundTimeConstraintParams =
            (android.widget.LinearLayout.LayoutParams) mRoundTimeConstraint.getLayoutParams();
        android.widget.LinearLayout.LayoutParams passTimeConstraintParams =
            (android.widget.LinearLayout.LayoutParams) mPassTimeConstraint.getLayoutParams();

        // Elements inside topLayout PercentRelativeLayout use PercentRelativeLayout.LayoutParams
        androidx.percentlayout.widget.PercentRelativeLayout.LayoutParams team1ControlsParams =
            (androidx.percentlayout.widget.PercentRelativeLayout.LayoutParams) team1Controls.getLayoutParams();
        androidx.percentlayout.widget.PercentRelativeLayout.LayoutParams teamsConstraintParams =
            (androidx.percentlayout.widget.PercentRelativeLayout.LayoutParams) mTeamsConstraint.getLayoutParams();

        // Elements inside bottomLayout PercentRelativeLayout use PercentRelativeLayout.LayoutParams
        androidx.percentlayout.widget.PercentRelativeLayout.LayoutParams team2ControlsParams =
            (androidx.percentlayout.widget.PercentRelativeLayout.LayoutParams) team2Controls.getLayoutParams();
        androidx.percentlayout.widget.PercentRelativeLayout.LayoutParams spinnerParams =
            (androidx.percentlayout.widget.PercentRelativeLayout.LayoutParams) settingsSpinner.getLayoutParams();

        if (isHebrew) {
            // Hebrew: checkboxes on right, button to the left of checkbox, checkbox icon on right of text
            checkboxParams.addRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_END);
            checkboxParams.removeRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_START);
            soundParams.addRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_END);
            soundParams.removeRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_START);

            // Button positioned to the left of the checkbox
            buttonParams.removeRule(RelativeLayout.RIGHT_OF);
            buttonParams.removeRule(RelativeLayout.END_OF);
            buttonParams.addRule(RelativeLayout.LEFT_OF, R.id.autoBalaceCheckBox);
            buttonParams.addRule(RelativeLayout.START_OF, R.id.autoBalaceCheckBox);
            buttonParams.removeRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_START);
            buttonParams.removeRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_END);
            // Set margins for Hebrew - right margin before checkbox
            buttonParams.setMargins(
                0, // left
                (int) (-8 * context.getResources().getDisplayMetrics().density), // top (negative to move up)
                (int) (8 * context.getResources().getDisplayMetrics().density), // right
                0  // bottom
            );

            mAutoBalaceCheckBox.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
            mSoundCheckBox.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
            // Move checkbox icon to right side (end) of text for Hebrew - create separate drawables for each checkbox
            android.graphics.drawable.Drawable checkboxDrawable1 = androidx.appcompat.content.res.AppCompatResources.getDrawable(
                context, androidx.appcompat.R.drawable.abc_btn_check_material);
            android.graphics.drawable.Drawable checkboxDrawable2 = androidx.appcompat.content.res.AppCompatResources.getDrawable(
                context, androidx.appcompat.R.drawable.abc_btn_check_material);
            if (checkboxDrawable1 != null) checkboxDrawable1 = checkboxDrawable1.mutate();
            if (checkboxDrawable2 != null) checkboxDrawable2 = checkboxDrawable2.mutate();
            mAutoBalaceCheckBox.setCompoundDrawablesWithIntrinsicBounds(null, null, checkboxDrawable1, null);
            mSoundCheckBox.setCompoundDrawablesWithIntrinsicBounds(null, null, checkboxDrawable2, null);

            // Hebrew: align all plus-minus controls and constraints to the right
            // For LinearLayout children, use gravity instead of rules
            roundTimeControlsParams.gravity = android.view.Gravity.END;
            passTimeControlsParams.gravity = android.view.Gravity.END;
            roundTimeConstraintParams.gravity = android.view.Gravity.END;
            passTimeConstraintParams.gravity = android.view.Gravity.END;

            // For RelativeLayout children, use rules
            team1ControlsParams.addRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_END);
            team1ControlsParams.removeRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_START);
            team2ControlsParams.addRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_END);
            team2ControlsParams.removeRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_START);

            teamsConstraintParams.addRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_END);
            teamsConstraintParams.removeRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_START);

            // Hebrew: spinner on right
            spinnerParams.addRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_END);
            spinnerParams.removeRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_START);

            // Hebrew: set spinner layout direction to RTL (arrow on the left - end of RTL reading)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                settingsSpinner.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
            }

            // Hebrew: align layout contents to the right
            if (topLayout instanceof androidx.percentlayout.widget.PercentRelativeLayout) {
                ((androidx.percentlayout.widget.PercentRelativeLayout) topLayout).setGravity(android.view.Gravity.RIGHT | android.view.Gravity.CENTER_VERTICAL);
            }
            if (bottomLayout instanceof androidx.percentlayout.widget.PercentRelativeLayout) {
                ((androidx.percentlayout.widget.PercentRelativeLayout) bottomLayout).setGravity(android.view.Gravity.RIGHT | android.view.Gravity.CENTER_VERTICAL);
            }

            // Hebrew: align title texts to the right
            mTeam1Text.setGravity(android.view.Gravity.RIGHT | android.view.Gravity.CENTER_VERTICAL);
            mEditScoreHeadline.setGravity(android.view.Gravity.RIGHT | android.view.Gravity.CENTER_VERTICAL);
            mTextView1.setGravity(android.view.Gravity.RIGHT | android.view.Gravity.CENTER_VERTICAL);
            mTeam1Headline.setGravity(android.view.Gravity.RIGHT | android.view.Gravity.CENTER_VERTICAL);
        } else {
            // English: checkboxes on left, button to the right of checkbox, checkbox icon on left of text
            checkboxParams.addRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_START);
            checkboxParams.removeRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_END);
            soundParams.addRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_START);
            soundParams.removeRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_END);

            // Button positioned to the right of the checkbox
            buttonParams.removeRule(RelativeLayout.LEFT_OF);
            buttonParams.removeRule(RelativeLayout.START_OF);
            buttonParams.addRule(RelativeLayout.RIGHT_OF, R.id.autoBalaceCheckBox);
            buttonParams.addRule(RelativeLayout.END_OF, R.id.autoBalaceCheckBox);
            buttonParams.removeRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_START);
            buttonParams.removeRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_END);
            // Set margins for English - left margin after checkbox
            buttonParams.setMargins(
                (int) (8 * context.getResources().getDisplayMetrics().density), // left
                (int) (-8 * context.getResources().getDisplayMetrics().density), // top (negative to move up)
                0, // right
                0  // bottom
            );

            mAutoBalaceCheckBox.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
            mSoundCheckBox.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
            // Keep checkbox icon on left side (start) of text for English - create separate drawables for each checkbox
            android.graphics.drawable.Drawable checkboxDrawable1 = androidx.appcompat.content.res.AppCompatResources.getDrawable(
                context, androidx.appcompat.R.drawable.abc_btn_check_material);
            android.graphics.drawable.Drawable checkboxDrawable2 = androidx.appcompat.content.res.AppCompatResources.getDrawable(
                context, androidx.appcompat.R.drawable.abc_btn_check_material);
            if (checkboxDrawable1 != null) checkboxDrawable1 = checkboxDrawable1.mutate();
            if (checkboxDrawable2 != null) checkboxDrawable2 = checkboxDrawable2.mutate();
            mAutoBalaceCheckBox.setCompoundDrawablesWithIntrinsicBounds(checkboxDrawable1, null, null, null);
            mSoundCheckBox.setCompoundDrawablesWithIntrinsicBounds(checkboxDrawable2, null, null, null);

            // English: align all plus-minus controls and constraints to the left
            // For LinearLayout children, use gravity instead of rules
            roundTimeControlsParams.gravity = android.view.Gravity.START;
            passTimeControlsParams.gravity = android.view.Gravity.START;
            roundTimeConstraintParams.gravity = android.view.Gravity.START;
            passTimeConstraintParams.gravity = android.view.Gravity.START;

            // For RelativeLayout children, use rules
            team1ControlsParams.addRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_START);
            team1ControlsParams.removeRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_END);
            team2ControlsParams.addRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_START);
            team2ControlsParams.removeRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_END);

            teamsConstraintParams.addRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_START);
            teamsConstraintParams.removeRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_END);

            // English: spinner on left
            spinnerParams.addRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_START);
            spinnerParams.removeRule(androidx.percentlayout.widget.PercentRelativeLayout.ALIGN_PARENT_END);

            // English: set spinner layout direction to LTR (arrow on the right - end of LTR reading)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                settingsSpinner.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_LTR);
            }

            // English: align layout contents to the left
            if (topLayout instanceof androidx.percentlayout.widget.PercentRelativeLayout) {
                ((androidx.percentlayout.widget.PercentRelativeLayout) topLayout).setGravity(android.view.Gravity.LEFT | android.view.Gravity.CENTER_VERTICAL);
            }
            if (bottomLayout instanceof androidx.percentlayout.widget.PercentRelativeLayout) {
                ((androidx.percentlayout.widget.PercentRelativeLayout) bottomLayout).setGravity(android.view.Gravity.LEFT | android.view.Gravity.CENTER_VERTICAL);
            }

            // English: align title texts to the left
            mTeam1Text.setGravity(android.view.Gravity.LEFT | android.view.Gravity.CENTER_VERTICAL);
            mEditScoreHeadline.setGravity(android.view.Gravity.LEFT | android.view.Gravity.CENTER_VERTICAL);
            mTextView1.setGravity(android.view.Gravity.LEFT | android.view.Gravity.CENTER_VERTICAL);
            mTeam1Headline.setGravity(android.view.Gravity.LEFT | android.view.Gravity.CENTER_VERTICAL);
        }

        mAutoBalaceCheckBox.setLayoutParams(checkboxParams);
        mSoundCheckBox.setLayoutParams(soundParams);
        mBalanceExplanation.setLayoutParams(buttonParams);

        // Apply layout params for controls and constraints
        roundTimeControls.setLayoutParams(roundTimeControlsParams);
        passTimeControls.setLayoutParams(passTimeControlsParams);
        team1Controls.setLayoutParams(team1ControlsParams);
        team2Controls.setLayoutParams(team2ControlsParams);
        settingsSpinner.setLayoutParams(spinnerParams);

        mRoundTimeConstraint.setLayoutParams(roundTimeConstraintParams);
        mPassTimeConstraint.setLayoutParams(passTimeConstraintParams);
        mTeamsConstraint.setLayoutParams(teamsConstraintParams);

        // Make all title texts the same size - measure and use the largest
        TextView[] titleTexts = {mTextView1, mTeam1Headline, mTeam1Text, mEditScoreHeadline};

        // Measure the text width for each title
        float maxWidth = 0f;
        float maxTextSize = 24f; // Default size in sp

        for (TextView tv : titleTexts) {
            if (tv != null) {
                android.text.TextPaint paint = tv.getPaint();
                String text = tv.getText().toString();
                float width = paint.measureText(text);
                if (width > maxWidth) {
                    maxWidth = width;
                    maxTextSize = tv.getTextSize() / getResources().getDisplayMetrics().scaledDensity;
                }
            }
        }

        // Set all titles to the same text size
        for (TextView tv : titleTexts) {
            if (tv != null) {
                tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24f); // Ensure uniform size
            }
        }

        // Temporarily remove listeners to avoid triggering them during initialization
        mAutoBalaceCheckBox.setOnCheckedChangeListener(null);
        mSoundCheckBox.setOnCheckedChangeListener(null);

        mAutoBalaceCheckBox.setChecked(db.autoBalanceCheckBox);
        mSoundCheckBox.setChecked(db.soundCheckBox);

        // Re-add listeners after setting initial state
        mAutoBalaceCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> onAutoBalanceChecked(isChecked));
        mSoundCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> onSoundChecked(isChecked));
        mRoundTimeValue.setText(String.valueOf(db.timePerRound));
        mPassTimeValue.setText(String.valueOf(db.timeDownOnNext));
        mAmountOfTeams.setText(String.valueOf(db.amountOfTeams));
        mTeamEditableScore.setText(String.valueOf(db.scores[0]));
        createGroupSpinner();
        if(db.amountOfTeams == 2) mDecrease1.setEnabled(false);
        else if(db.amountOfTeams == 24) mIncrease1.setEnabled(false);
        if(db.scores[selectedSpinner] == 0) mDecrease2.setEnabled(false);

        // Enable/disable round time and pass time buttons based on current values
        if(db.timePerRound <= 1) mDecreaseRoundTime.setEnabled(false);
        else if(db.timePerRound >= 300) mIncreaseRoundTime.setEnabled(false);
        if(db.timeDownOnNext <= 1) mDecreasePassTime.setEnabled(false);
        else if(db.timeDownOnNext >= 300) mIncreasePassTime.setEnabled(false);
    }

    private void createGroupSpinner() {

        String [] items = new String[db.amountOfTeams];
        for(int i = 0; i < db.amountOfTeams; i++){
            items[i]= getString(R.string.game_team_label) + (i + 1);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item_tight, items) {

            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);

                Typeface externalFont=Typeface.createFromAsset(getAssets(), "gan.ttf");
                TextView textView = (TextView) v;
                textView.setTypeface(externalFont);
                textView.setTextColor(0x88000000);
                textView.setTextSize(24);
                textView.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
                textView.setPadding(0, 0, 0, 0);

                // Get language to determine layout direction
                String language = prefs.getString("app_language", "he");

                // Set layout direction and arrow position
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    if (language.equals("he")) {
                        // Hebrew RTL: arrow on the left (end of RTL)
                        textView.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
                    } else {
                        // English LTR: arrow on the right (end of LTR)
                        textView.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_LTR);
                    }
                }

                // Calculate space width for natural spacing
                android.graphics.Paint paint = textView.getPaint();
                float spaceWidth = paint.measureText(" ");

                // Add dropdown arrow with space-width padding at END position
                textView.setCompoundDrawablePadding((int) spaceWidth);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    textView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);
                } else {
                    textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);
                }
                return v;
            }

            @NonNull
            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);

                Typeface externalFont=Typeface.createFromAsset(getAssets(), "gan.ttf");
                ((TextView) v).setTypeface(externalFont);
                ((TextView) v).setTextColor(0x88000000);
                ((TextView) v).setTextSize(24);
                ((TextView) v).setGravity(android.view.Gravity.CENTER);
                return v;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinner = findViewById(R.id.settingsSpinner);
        spinner.setAdapter(adapter);
        // Remove all spinner padding to minimize gap
        spinner.setPadding(0, 0, 0, 0);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                selectedSpinner = position;
                mTeamEditableScore.setText(String.valueOf(db.scores[selectedSpinner]));
                if (db.scores[selectedSpinner] == 0) mDecrease2.setEnabled(false);
                else mDecrease2.setEnabled(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });
    }

    //*********************** ON CLICKS ********************************
    void onAutoBalanceChecked(boolean checked) {
        //save autoBalanceCheckBox to shared preferences
        db.autoBalanceCheckBox = checked;
        spEditor.putBoolean("autoBalanceCheckBoxBackup", checked);
        spEditor.apply();
    }

    void onSoundChecked(boolean checked) {
        db.soundCheckBox = checked;
        //save soundCheckBox to shared preferences
        spEditor.putBoolean("soundCheckBoxBackup", checked);
        spEditor.apply();
    }

    public void explanationBalanceDialog(View view) {
        dialogBag.unevenExplanation();
    }


    public void increaseRoundTime(View view) {
        if (db.timePerRound >= 300) return;
        db.timePerRound++;
        mRoundTimeValue.setText(String.valueOf(db.timePerRound));
        spEditor.putInt("timePerRoundBackup", db.timePerRound);
        spEditor.apply();

        // Update button states
        mDecreaseRoundTime.setEnabled(true);
        if (db.timePerRound >= 300) mIncreaseRoundTime.setEnabled(false);
    }

    public void decreaseRoundTime(View view) {
        if (db.timePerRound <= 1) return;
        db.timePerRound--;
        mRoundTimeValue.setText(String.valueOf(db.timePerRound));
        spEditor.putInt("timePerRoundBackup", db.timePerRound);
        spEditor.apply();

        // Update button states
        mIncreaseRoundTime.setEnabled(true);
        if (db.timePerRound <= 1) mDecreaseRoundTime.setEnabled(false);
    }

    public void increasePassTime(View view) {
        if (db.timeDownOnNext >= 300) return;
        db.timeDownOnNext++;
        mPassTimeValue.setText(String.valueOf(db.timeDownOnNext));
        spEditor.putInt("timeDownOnNextBackup", db.timeDownOnNext);
        spEditor.apply();

        // Update button states
        mDecreasePassTime.setEnabled(true);
        if (db.timeDownOnNext >= 300) mIncreasePassTime.setEnabled(false);
    }

    public void decreasePassTime(View view) {
        if (db.timeDownOnNext <= 1) return;
        db.timeDownOnNext--;
        mPassTimeValue.setText(String.valueOf(db.timeDownOnNext));
        spEditor.putInt("timeDownOnNextBackup", db.timeDownOnNext);
        spEditor.apply();

        // Update button states
        mIncreasePassTime.setEnabled(true);
        if (db.timeDownOnNext <= 1) mDecreasePassTime.setEnabled(false);
    }

    public void resetAllSettings(View view) {
        Runnable task = new Runnable() {
            public void run() {
                db.autoBalanceCheckBox = true;
                db.soundCheckBox = true;
                db.timePerRound = 60;
                db.timeDownOnNext = 5;
                spEditor.putBoolean("autoBalanceCheckBoxBackup", true);
                spEditor.putBoolean("soundCheckBoxBackup", true);
                spEditor.putInt("timePerRoundBackup", 60);
                spEditor.putInt("timeDownOnNextBackup", 5);
                spEditor.apply();

                mAutoBalaceCheckBox.setChecked(true);
                mSoundCheckBox.setChecked(true);
                mRoundTimeValue.setText(String.valueOf(60));
                mPassTimeValue.setText(String.valueOf(5));

                // Update button states
                mIncreaseRoundTime.setEnabled(true);
                mDecreaseRoundTime.setEnabled(true);
                mIncreasePassTime.setEnabled(true);
                mDecreasePassTime.setEnabled(true);
            }
        };
        dialogBag.resetSettings(task);
    }

    public void increaseTeam1Score(View view) {
        if (db.amountOfTeams == 24) return;
        if (db.gamePlayIsPaused || db.summaryIsPaused) {
            dialogBag.cannotEditTeamsAmount();
            return;
        }
        db.amountOfTeams++;
        if(db.amountOfTeams == 24) mIncrease1.setEnabled(false);
        mDecrease1.setEnabled(true);
        spEditor.putInt("amountOfTeams", db.amountOfTeams);
        spEditor.apply();
        createGroupSpinner();
        mAmountOfTeams.setText(String.valueOf(db.amountOfTeams));
    }

    public void increaseTeam2Score(View view) {
        if (!db.gamePlayIsPaused && !db.summaryIsPaused) {
            dialogBag.cannotEditScore();
            return;
        }
        db.scores[selectedSpinner]++;
        mDecrease2.setEnabled(true);
        spEditor.putInt("team" + selectedSpinner + "Score", db.scores[selectedSpinner]);
        spEditor.apply();
        mTeamEditableScore.setText(String.valueOf(db.scores[selectedSpinner]));
    }

    public void decreaseTeam1Score(View view) {
        if (db.amountOfTeams == 2) return;
        if (db.gamePlayIsPaused || db.summaryIsPaused) {
            dialogBag.cannotEditTeamsAmount();
            return;
        }
        db.amountOfTeams--;
        if(db.amountOfTeams == 2) mDecrease1.setEnabled(false);
        mIncrease1.setEnabled(true);
        spEditor.putInt("amountOfTeams", db.amountOfTeams);
        spEditor.apply();
        createGroupSpinner();
        mAmountOfTeams.setText(String.valueOf(db.amountOfTeams));
    }

    public void decreaseTeam2Score(View view) {
        if (db.scores[selectedSpinner] == 0) return;
        if (!db.gamePlayIsPaused && !db.summaryIsPaused) {
            dialogBag.cannotEditScore();
            return;
        }
        db.scores[selectedSpinner]--;
        if (db.scores[selectedSpinner] == 0) mDecrease2.setEnabled(false);
        spEditor.putInt("team" + selectedSpinner + "Score", db.scores[selectedSpinner]);
        spEditor.apply();
        mTeamEditableScore.setText(String.valueOf(db.scores[selectedSpinner]));
    }

    // Direct editing focus change handlers
    void focusChangedRoundTimeValue(boolean hasFocus) {
        if (!hasFocus) {
            String text = mRoundTimeValue.getText().toString();
            if (text.isEmpty()) {
                mRoundTimeValue.setText(String.valueOf(db.timePerRound));
                return;
            }
            try {
                int value = Integer.parseInt(text);
                if (value < 1 || value > 300) {
                    dialogBag.invalidInput();
                    mRoundTimeValue.setText(String.valueOf(db.timePerRound));
                    return;
                }
                db.timePerRound = value;
                spEditor.putInt("timePerRoundBackup", value);
                spEditor.apply();
                mRoundTimeValue.setText(String.valueOf(value));

                // Update button states
                mDecreaseRoundTime.setEnabled(value > 1);
                mIncreaseRoundTime.setEnabled(value < 300);
            } catch (NumberFormatException e) {
                dialogBag.invalidInput();
                mRoundTimeValue.setText(String.valueOf(db.timePerRound));
            }
        }
    }

    void focusChangedPassTimeValue(boolean hasFocus) {
        if (!hasFocus) {
            String text = mPassTimeValue.getText().toString();
            if (text.isEmpty()) {
                mPassTimeValue.setText(String.valueOf(db.timeDownOnNext));
                return;
            }
            try {
                int value = Integer.parseInt(text);
                if (value < 1 || value > 300) {
                    dialogBag.invalidInput();
                    mPassTimeValue.setText(String.valueOf(db.timeDownOnNext));
                    return;
                }
                db.timeDownOnNext = value;
                spEditor.putInt("timeDownOnNextBackup", value);
                spEditor.apply();
                mPassTimeValue.setText(String.valueOf(value));

                // Update button states
                mDecreasePassTime.setEnabled(value > 1);
                mIncreasePassTime.setEnabled(value < 300);
            } catch (NumberFormatException e) {
                dialogBag.invalidInput();
                mPassTimeValue.setText(String.valueOf(db.timeDownOnNext));
            }
        }
    }

    void focusChangedAmountOfTeams(boolean hasFocus) {
        if (!hasFocus) {
            String text = mAmountOfTeams.getText().toString();
            if (text.isEmpty()) {
                mAmountOfTeams.setText(String.valueOf(db.amountOfTeams));
                return;
            }
            try {
                int value = Integer.parseInt(text);
                if (value < 2 || value > 24) {
                    // Show custom dialog for teams
                    dialogBag.invalidInput();
                    mAmountOfTeams.setText(String.valueOf(db.amountOfTeams));
                    return;
                }
                if (db.gamePlayIsPaused || db.summaryIsPaused) {
                    dialogBag.cannotEditTeamsAmount();
                    mAmountOfTeams.setText(String.valueOf(db.amountOfTeams));
                    return;
                }
                db.amountOfTeams = value;
                spEditor.putInt("amountOfTeams", value);
                spEditor.apply();
                mAmountOfTeams.setText(String.valueOf(value));
                createGroupSpinner();

                // Update button states
                mDecrease1.setEnabled(value > 2);
                mIncrease1.setEnabled(value < 24);
            } catch (NumberFormatException e) {
                dialogBag.invalidInput();
                mAmountOfTeams.setText(String.valueOf(db.amountOfTeams));
            }
        }
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

    // Input filter to restrict number input to a range
    private static class InputFilterMinMax implements android.text.InputFilter {
        private final int min;
        private final int max;

        public InputFilterMinMax(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, android.text.Spanned dest, int dstart, int dend) {
            try {
                // Build the resulting string after this edit
                String newVal = dest.subSequence(0, dstart).toString() + source.subSequence(start, end) + dest.subSequence(dend, dest.length());

                // Allow empty string (user is deleting)
                if (newVal.isEmpty()) {
                    return null;
                }

                int input = Integer.parseInt(newVal);
                if (isInRange(min, max, input)) {
                    return null; // Accept the input
                }
            } catch (NumberFormatException e) {
                // Invalid number format - reject
            }
            return ""; // Reject the input
        }

        private boolean isInRange(int a, int b, int c) {
            return c >= a && c <= b;
        }
    }
}