package nevo_mashiach.pitkiot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import androidx.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Locale;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import androidx.appcompat.app.AppCompatActivity;
import nevo_mashiach.pitkiot.databinding.ActivitySummaryBinding;
import nevo_mashiach.pitkiot.NotActivities.DialogBag;
import nevo_mashiach.pitkiot.NotActivities.MyButton;
import nevo_mashiach.pitkiot.NotActivities.db;

@SuppressLint("SourceLockedOrientationActivity")
public class Summary extends AppCompatActivity {

    public static final String TAG = Summary.class.getName();

    TextView mT1Total;
    TextView mT2Total;
    TextView mT1Round;
    TextView mT2Round;
    TextView mTotalNotes;
    TextView mSummaryHeadline;
    TextView mRoundModeSummary;
    MyButton mReady;
    TextView mT1Plus;
    TextView mT2Plus;
    TextView mTeam1Headline;
    TextView mTeam2Headline;
    ImageView mPressHereFigure;
    View mSummarySpinner;
    TextView mMultiTeamsPlus;
    TextView mMultiTeamTotalScore;
    TextView mMultiTeamRound;


    Context context;
    public DialogBag dialogBag;
    boolean onCreate = false;
    boolean firstTime = true;
    SharedPreferences prefs;
    SharedPreferences.Editor spEditor;
    int selectedSpinner = 1;
    int teamThatJustPlayed = -1;
    int successCountForTeamThatJustPlayed = 0;

    // Semantic state for proper localization on resume
    String summaryState = "";  // "time_up" or "notes_finished"
    int nextTeamForButton = -1;  // Which team number to show in button


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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLayoutDirection(locale);
            return context.createConfigurationContext(config);
        } else {
            resources.updateConfiguration(config, resources.getDisplayMetrics());
            return context;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySummaryBinding binding = ActivitySummaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        // Initialize view references
        mT1Total = binding.t1Total;
        mT2Total = binding.t2Total;
        mT1Round = binding.t1Round;
        mT2Round = binding.t2Round;
        mTotalNotes = binding.editScoreHeadline;
        mSummaryHeadline = binding.summaryHeadline;
        mRoundModeSummary = binding.roundModeSummary;
        mReady = binding.ready;
        mT1Plus = binding.t1Plus;
        mT2Plus = binding.t2Plus;
        mTeam1Headline = binding.team1Headline;
        mTeam2Headline = binding.team2Headline;
        mPressHereFigure = binding.pressHereFigure;
        mSummarySpinner = binding.summarySpinner;
        mMultiTeamsPlus = binding.multiTeamsPlus;
        mMultiTeamTotalScore = binding.multiTeamTotalScore;
        mMultiTeamRound = binding.multiTeamRound;
        context = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        spEditor = prefs.edit();
        dialogBag = new DialogBag(getSupportFragmentManager(), this);
        if (firstTime) {
            updateInfoFromSharedPreferences();
            firstTime = false;
        }

        // Set up click listener
        binding.ready.setOnClickListener(v -> backToGamePlay());

        // Set up touch listener
        // Note: db.onTouch() internally calls view.performClick() for accessibility
        @SuppressLint("ClickableViewAccessibility")
        View.OnTouchListener touchListener = (v, motion) -> db.onTouch(context, v, motion);
        binding.ready.setOnTouchListener(touchListener);
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Apply navigation bar settings after view is attached and insets are available
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                setupTransparentNavigationBar();
            }
        });

        onCreate = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Handle English layout adjustments
        String language = prefs.getString("app_language", "he");
        adjustLayoutForLanguage(language);

        if (!onCreate) return; //preventing round++ when minimizing the app
        mTotalNotes.setText(getString(R.string.game_notes_remaining, db.roundNoteAmount()));
        if (!db.summaryIsPaused) {
            mRoundModeSummary.setText(db.getRoundMode(context));
            // Check if Intent has extras (might be null when resuming from MainActivity)
            if (getIntent().getExtras() != null) {
                String readyText = getIntent().getExtras().getString("readyText");
                String summaryHeadline = getIntent().getExtras().getString("summaryHeadline");
                mReady.setText(readyText);
                mSummaryHeadline.setText(summaryHeadline);

                // Determine and save semantic state for future resumes
                if (summaryHeadline.equals(getString(R.string.game_time_up))) {
                    summaryState = "time_up";
                } else if (summaryHeadline.equals(getString(R.string.game_notes_finished))) {
                    summaryState = "notes_finished";
                }

                // Extract team number from button text for future localization
                // readyText format: "START! Team turn X" or "CONTINUE! Team turn X"
                try {
                    String[] parts = readyText.split(" ");
                    if (parts.length > 0) {
                        String lastPart = parts[parts.length - 1];
                        nextTeamForButton = Integer.parseInt(lastPart);
                    }
                } catch (NumberFormatException e) {
                    // If parsing fails, calculate from current playing
                    nextTeamForButton = ((db.currentPlaying + 1) % db.amountOfTeams) + 1;
                }
            } else {
                // Fallback to loading from SharedPreferences
                updateInfoFromSharedPreferences();
            }

            // Use flag instead of UI text comparison for reliable logic
            if (db.wasTimeUp) { //If we are here because of time out
                if(db.amountOfTeams == 2){
                    if (db.currentPlaying == 0) {
                        mT1Plus.setText(String.format(Locale.US, "+%d", db.currentSuccessNum));
                        mT1Plus.setVisibility(View.VISIBLE);
                        mT2Plus.setVisibility(View.INVISIBLE);
                    } else {
                        mT2Plus.setText(String.format(Locale.US, "+%d", db.currentSuccessNum));
                        mT2Plus.setVisibility(View.VISIBLE);
                        mT1Plus.setVisibility(View.INVISIBLE);
                    }
                    teamThatJustPlayed = db.currentPlaying;
                    successCountForTeamThatJustPlayed = db.currentSuccessNum;
                }
                else{
                    mMultiTeamsPlus.setText(String.format(Locale.US, "+%d", db.currentSuccessNum));
                    mMultiTeamsPlus.setVisibility(View.VISIBLE);
                    teamThatJustPlayed = db.currentPlaying;
                    successCountForTeamThatJustPlayed = db.currentSuccessNum;
                }
                db.currentSuccessNum = 0;
                db.increaseRoundNum();
            } else { //If the notes are out
                if (db.totalRoundNumber == 0) { //If the game is over
                    mPressHereFigure.setVisibility(View.INVISIBLE);
                    db.increaseRoundNum();
                    //db.currentPlaying = (db.currentPlaying + 1)%db.amountOfTeams; -- Maybe consider adding it
                    if (db.autoBalanceCheckBox) autoBalance();  //If auto balance is checked
                    gameOverDialogCall();
                } else {
                    // Notes finished mid-round - still show the indicator!
                    if(db.amountOfTeams == 2){
                        if (db.currentPlaying == 0) {
                            mT1Plus.setText(String.format(Locale.US, "+%d", db.currentSuccessNum));
                            mT1Plus.setVisibility(View.VISIBLE);
                            mT2Plus.setVisibility(View.INVISIBLE);
                        } else {
                            mT2Plus.setText(String.format(Locale.US, "+%d", db.currentSuccessNum));
                            mT2Plus.setVisibility(View.VISIBLE);
                            mT1Plus.setVisibility(View.INVISIBLE);
                        }
                        teamThatJustPlayed = db.currentPlaying;
                        successCountForTeamThatJustPlayed = db.currentSuccessNum;
                    }
                    else{
                        mMultiTeamsPlus.setText(String.format(Locale.US, "+%d", db.currentSuccessNum));
                        mMultiTeamsPlus.setVisibility(View.VISIBLE);
                        teamThatJustPlayed = db.currentPlaying;
                        successCountForTeamThatJustPlayed = db.currentSuccessNum;
                    }
                    db.currentSuccessNum = 0;
                    dialogBag.modeChanged();
                    mTotalNotes.setText(getString(R.string.game_notes_remaining, db.totalNoteAmount()));
                }
            }
            if (db.amountOfTeams == 2) {
                multiTeamsVisibility(false);
                twoTeamsVisibility(true);
                mT1Round.setText(getString(R.string.game_number_of_rounds, db.teamsRoundNum[0]));
                mT2Round.setText(getString(R.string.game_number_of_rounds, db.teamsRoundNum[1]));
            } else {
                multiTeamsVisibility(true);
                twoTeamsVisibility(false);
                createGroupSpinner();
                mMultiTeamRound.setText(getString(R.string.game_number_of_rounds, db.teamsRoundNum[db.currentPlaying]));
            }
        } else {
            db.summaryIsPaused = false;
            // Refresh localized strings in case language was changed
            mRoundModeSummary.setText(db.getRoundMode(context));
            // Restore indicator when returning from main menu
            if (db.amountOfTeams == 2) {
                // Restore 2-team indicators from saved state
                if (teamThatJustPlayed == 0 && successCountForTeamThatJustPlayed >= 0) {
                    mT1Plus.setText(String.format(Locale.US, "+%d", successCountForTeamThatJustPlayed));
                    mT1Plus.setVisibility(View.VISIBLE);
                } else {
                    mT1Plus.setText("");
                    mT1Plus.setVisibility(View.INVISIBLE);
                }
                if (teamThatJustPlayed == 1 && successCountForTeamThatJustPlayed >= 0) {
                    mT2Plus.setText(String.format(Locale.US, "+%d", successCountForTeamThatJustPlayed));
                    mT2Plus.setVisibility(View.VISIBLE);
                } else {
                    mT2Plus.setText("");
                    mT2Plus.setVisibility(View.INVISIBLE);
                }
            } else {
                // Restore multi-team indicator
                if (teamThatJustPlayed == db.currentPlaying && successCountForTeamThatJustPlayed >= 0) {
                    mMultiTeamsPlus.setText(String.format(Locale.US, "+%d", successCountForTeamThatJustPlayed));
                    mMultiTeamsPlus.setVisibility(View.VISIBLE);
                } else {
                    mMultiTeamsPlus.setText("");
                    mMultiTeamsPlus.setVisibility(View.INVISIBLE);
                }
            }
        }
        if (db.amountOfTeams == 2) {
            multiTeamsVisibility(false);
            twoTeamsVisibility(true);
            mT1Total.setText(getString(R.string.game_total_score, db.scores[0]));
            mT2Total.setText(getString(R.string.game_total_score, db.scores[1]));
            // Restore 2-team indicators after activity destruction
            if (teamThatJustPlayed == 0 && successCountForTeamThatJustPlayed >= 0) {
                mT1Plus.setText(String.format(Locale.US, "+%d", successCountForTeamThatJustPlayed));
                mT1Plus.setVisibility(View.VISIBLE);
            } else {
                mT1Plus.setText("");
                mT1Plus.setVisibility(View.INVISIBLE);
            }
            if (teamThatJustPlayed == 1 && successCountForTeamThatJustPlayed >= 0) {
                mT2Plus.setText(String.format(Locale.US, "+%d", successCountForTeamThatJustPlayed));
                mT2Plus.setVisibility(View.VISIBLE);
            } else {
                mT2Plus.setText("");
                mT2Plus.setVisibility(View.INVISIBLE);
            }
        } else {
            multiTeamsVisibility(true);
            twoTeamsVisibility(false);
            createGroupSpinner();
            mMultiTeamTotalScore.setText(getString(R.string.game_total_score, db.scores[db.currentPlaying]));
            // Restore indicator if viewing the team that just played
            if (teamThatJustPlayed == db.currentPlaying && successCountForTeamThatJustPlayed >= 0) {
                mMultiTeamsPlus.setText(String.format(Locale.US, "+%d", successCountForTeamThatJustPlayed));
                mMultiTeamsPlus.setVisibility(View.VISIBLE);
            } else {
                mMultiTeamsPlus.setText("");
                mMultiTeamsPlus.setVisibility(View.INVISIBLE);
            }
        }
        onCreate = false;
    }


    private void updateInfoFromSharedPreferences() {
        // Load semantic state instead of localized text
        summaryState = prefs.getString("summaryState", "");
        nextTeamForButton = prefs.getInt("nextTeamForButton", -1);

        // Regenerate localized strings from semantic state
        if (!summaryState.isEmpty()) {
            if (summaryState.equals("time_up")) {
                mSummaryHeadline.setText(getString(R.string.game_time_up));
                if (nextTeamForButton >= 0) {
                    mReady.setText(getString(R.string.game_start_team_turn) + nextTeamForButton);
                }
            } else if (summaryState.equals("notes_finished")) {
                mSummaryHeadline.setText(getString(R.string.game_notes_finished));
                if (nextTeamForButton >= 0) {
                    mReady.setText(getString(R.string.game_continue_team_turn) + nextTeamForButton);
                }
            }
        }

        mRoundModeSummary.setText(db.getRoundMode(context));

        // Regenerate localized strings for scores and rounds
        if (db.amountOfTeams == 2) {
            mT1Total.setText(getString(R.string.game_total_score, db.scores[0]));
            mT1Round.setText(getString(R.string.game_number_of_rounds, db.teamsRoundNum[0]));
            mT2Total.setText(getString(R.string.game_total_score, db.scores[1]));
            mT2Round.setText(getString(R.string.game_number_of_rounds, db.teamsRoundNum[1]));
        } else {
            mMultiTeamTotalScore.setText(getString(R.string.game_total_score, db.scores[db.currentPlaying]));
            mMultiTeamRound.setText(getString(R.string.game_number_of_rounds, db.teamsRoundNum[db.currentPlaying]));
        }

        db.team2AverageAnswersPerSecond = Double.longBitsToDouble(prefs.getLong("team2AverageAnswersPerSecond", Double.doubleToLongBits(0)));
        teamThatJustPlayed = prefs.getInt("teamThatJustPlayed", -1);
        successCountForTeamThatJustPlayed = prefs.getInt("successCountForTeamThatJustPlayed", 0);
    }

        public void gameOverDialogCall() {
        if (db.amountOfTeams == 2) {
            if (db.scores[0] == db.scores[1]) dialogBag.drawGameOver(db.scores[0]);
            else {
                if (db.scores[0] >  db.scores[1])
                    dialogBag.normalGameOver(1, db.scores[0], db.scores[1]);
                else dialogBag.normalGameOver(2, db.scores[1], db.scores[0]);
            }
        } else {
            dialogBag.multiGameOver(db.scores);
        }
    }

    public void autoBalance() {
        for (int i = 1; i < db.amountOfTeams; i++) {
            if (db.teamsRoundNum[i] == 0) continue;
            if (db.teamsRoundNum[0] != db.teamsRoundNum[i]) { //If the round amount is unequal
                db.team2AverageAnswersPerSecond = (double) db.scores[i] / ((double) db.teamsRoundNum[i] * (double) db.timePerRound);
                double timeToReduce = (db.currentPlaying == 0) ? (double) db.mMillisUntilFinished / 1000 : 0; //In case the first team was the lasst to play
                db.scores[i] = (int) Math.round((double) db.scores[i] + ((double) db.timePerRound - timeToReduce) * db.team2AverageAnswersPerSecond);
            } else {//If the round amount is equal
                if(db.currentPlaying != i) continue; //If the last team to play is not the current selected
                double totalPlayingTime = (((double) db.teamsRoundNum[i] - 1) * (double) db.timePerRound) + ((double) db.timePerRound - (double) db.mMillisUntilFinished / 1000);
                // Prevent division by zero if team finished instantly
                if (totalPlayingTime > 0) {
                    db.team2AverageAnswersPerSecond = (double) db.scores[i] / totalPlayingTime;
                    db.scores[i] = (int) Math.round((double) db.scores[i] + (double) db.mMillisUntilFinished / 1000 * db.team2AverageAnswersPerSecond);
                }
            }
        }
    }

    private void adjustLayoutForLanguage(String language) {
        android.widget.RelativeLayout.LayoutParams imageParams =
            (android.widget.RelativeLayout.LayoutParams) mPressHereFigure.getLayoutParams();
        android.widget.RelativeLayout.LayoutParams t1PlusParams =
            (android.widget.RelativeLayout.LayoutParams) mT1Plus.getLayoutParams();
        android.widget.RelativeLayout.LayoutParams t2PlusParams =
            (android.widget.RelativeLayout.LayoutParams) mT2Plus.getLayoutParams();
        android.widget.RelativeLayout.LayoutParams multiTeamsPlusParams =
            (android.widget.RelativeLayout.LayoutParams) mMultiTeamsPlus.getLayoutParams();

        if (language.equals("en")) {
            // English: align all text to the left
            mTeam1Headline.setGravity(android.view.Gravity.LEFT);
            mTeam2Headline.setGravity(android.view.Gravity.LEFT);
            mT1Total.setGravity(android.view.Gravity.LEFT);
            mT2Total.setGravity(android.view.Gravity.LEFT);
            mT1Round.setGravity(android.view.Gravity.LEFT);
            mT2Round.setGravity(android.view.Gravity.LEFT);
            mMultiTeamTotalScore.setGravity(android.view.Gravity.LEFT);
            mMultiTeamRound.setGravity(android.view.Gravity.LEFT);

            // Circle badges stay centered but align left in layout
            t1PlusParams.removeRule(android.widget.RelativeLayout.ALIGN_PARENT_RIGHT);
            t1PlusParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_LEFT);
            t2PlusParams.removeRule(android.widget.RelativeLayout.ALIGN_PARENT_RIGHT);
            t2PlusParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_LEFT);
            multiTeamsPlusParams.removeRule(android.widget.RelativeLayout.ALIGN_PARENT_RIGHT);
            multiTeamsPlusParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_LEFT);

            // Move image to the right and flip it horizontally
            imageParams.removeRule(android.widget.RelativeLayout.ALIGN_PARENT_LEFT);
            imageParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_RIGHT);
            mPressHereFigure.setScaleX(-1f); // Flip horizontally
        } else {
            // Hebrew: align all text to the right (default)
            mTeam1Headline.setGravity(android.view.Gravity.RIGHT);
            mTeam2Headline.setGravity(android.view.Gravity.RIGHT);
            mT1Total.setGravity(android.view.Gravity.RIGHT);
            mT2Total.setGravity(android.view.Gravity.RIGHT);
            mT1Round.setGravity(android.view.Gravity.RIGHT);
            mT2Round.setGravity(android.view.Gravity.RIGHT);
            mMultiTeamTotalScore.setGravity(android.view.Gravity.RIGHT);
            mMultiTeamRound.setGravity(android.view.Gravity.RIGHT);

            // Circle badges stay centered but align right in layout
            t1PlusParams.removeRule(android.widget.RelativeLayout.ALIGN_PARENT_LEFT);
            t1PlusParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_RIGHT);
            t2PlusParams.removeRule(android.widget.RelativeLayout.ALIGN_PARENT_LEFT);
            t2PlusParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_RIGHT);
            multiTeamsPlusParams.removeRule(android.widget.RelativeLayout.ALIGN_PARENT_LEFT);
            multiTeamsPlusParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_RIGHT);

            // Move image to the left
            imageParams.removeRule(android.widget.RelativeLayout.ALIGN_PARENT_RIGHT);
            imageParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_LEFT);
            mPressHereFigure.setScaleX(1f); // Normal orientation
        }

        mPressHereFigure.setLayoutParams(imageParams);
        mT1Plus.setLayoutParams(t1PlusParams);
        mT2Plus.setLayoutParams(t2PlusParams);
        mMultiTeamsPlus.setLayoutParams(multiTeamsPlusParams);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //Save game state:
        Set<String> set = new HashSet<>(db.temp);
        spEditor.putStringSet("temp", set);
        set.clear();
        set.addAll(db.defs);
        spEditor.putStringSet("defs", set);
        set.clear();
        for (int i = 0; i < db.amountOfTeams; i++) {
            set.addAll(db.teamsNotes[i]);
            spEditor.putStringSet("team" + i + "Notes", set);
            set.clear();
            spEditor.putInt("team" + i + "RoundNum", db.teamsRoundNum[i]);
            spEditor.putInt("team" + i + "Score", db.scores[i]);
        }
        spEditor.putInt("totalRoundNumber", db.totalRoundNumber);
        spEditor.putInt("currentSuccessNum", db.currentSuccessNum);
        spEditor.putInt("currentPlaying", db.currentPlaying);
        spEditor.putLong("mMillisUntilFinished", db.mMillisUntilFinished);
        if(!db.gameOverDialogActivated) spEditor.putBoolean("summaryIsPaused", true);
        spEditor.putBoolean("gamePlayIsPaused", false);
        spEditor.putBoolean("wasTimeUp", db.wasTimeUp);
        spEditor.putLong("team2AverageAnswersPerSecond", Double.doubleToRawLongBits(db.team2AverageAnswersPerSecond));
        spEditor.putInt("teamThatJustPlayed", teamThatJustPlayed);
        spEditor.putInt("successCountForTeamThatJustPlayed", successCountForTeamThatJustPlayed);

        //Save semantic state (not localized text) so it can be regenerated in any language
        spEditor.putString("summaryState", summaryState);
        spEditor.putInt("nextTeamForButton", nextTeamForButton);
        // Note: Scores, rounds, and round mode are saved via db state and regenerated from there

        spEditor.apply();
    }

    private void twoTeamsVisibility(boolean visible) {
        if (visible) {
            mTeam1Headline.setVisibility(View.VISIBLE);
            mTeam2Headline.setVisibility(View.VISIBLE);
            mT1Round.setVisibility(View.VISIBLE);
            mT2Round.setVisibility(View.VISIBLE);
            mT1Total.setVisibility(View.VISIBLE);
            mT2Total.setVisibility(View.VISIBLE);
        } else {
            mTeam1Headline.setVisibility(View.INVISIBLE);
            mTeam2Headline.setVisibility(View.INVISIBLE);
            mT1Round.setVisibility(View.INVISIBLE);
            mT2Round.setVisibility(View.INVISIBLE);
            mT1Total.setVisibility(View.INVISIBLE);
            mT2Total.setVisibility(View.INVISIBLE);
        }
    }

    private void multiTeamsVisibility(boolean visible) {
        if (visible) {
            mSummarySpinner.setVisibility(View.VISIBLE);
            mMultiTeamRound.setVisibility(View.VISIBLE);
            mMultiTeamTotalScore.setVisibility(View.VISIBLE);
        } else {
            mSummarySpinner.setVisibility(View.INVISIBLE);
            mMultiTeamRound.setVisibility(View.INVISIBLE);
            mMultiTeamTotalScore.setVisibility(View.INVISIBLE);
        }
    }

    private void createGroupSpinner() {

        String[] items = new String[db.amountOfTeams];
        for (int i = 0; i < db.amountOfTeams; i++) {
            items[i] = getString(R.string.game_team_label) + (i + 1);
        }

        String language = prefs.getString("app_language", "he");
        int textGravity = language.equals("en") ? android.view.Gravity.LEFT : android.view.Gravity.RIGHT;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items) {

            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                Typeface externalFont = Typeface.createFromAsset(getAssets(), "gan.ttf");
                ((TextView) v).setTypeface(externalFont);
                ((TextView) v).setTextColor(0x88000000);
                ((TextView) v).setTextSize(25);
                ((TextView) v).setGravity(textGravity);
                return v;
            }

            @NonNull
            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);

                Typeface externalFont = Typeface.createFromAsset(getAssets(), "gan.ttf");
                ((TextView) v).setTypeface(externalFont);
                ((TextView) v).setTextColor(0x88000000);
                ((TextView) v).setTextSize(25);
                return v;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinner = findViewById(R.id.summarySpinner);
        spinner.setAdapter(adapter);
        // Ensure currentPlaying is within valid range
        int selection = (db.currentPlaying >= 0 && db.currentPlaying < db.amountOfTeams) ? db.currentPlaying : 0;
        spinner.setSelection(selection);

        // Set spinner layout direction to show arrow on correct side
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (language.equals("he")) {
                // Hebrew: set spinner layout direction to RTL (arrow on the left - end of RTL reading)
                spinner.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
            } else {
                // English: set spinner layout direction to LTR (arrow on the right - end of LTR reading)
                spinner.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_LTR);
            }
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                selectedSpinner = position;
                mMultiTeamTotalScore.setText(getString(R.string.game_total_score, db.scores[selectedSpinner]));
                mMultiTeamRound.setText(getString(R.string.game_number_of_rounds, db.teamsRoundNum[selectedSpinner]));

                // Show indicator only if this is the team that just played
                if (teamThatJustPlayed == selectedSpinner && successCountForTeamThatJustPlayed >= 0) {
                    mMultiTeamsPlus.setText(String.format(Locale.US, "+%d", successCountForTeamThatJustPlayed));
                    mMultiTeamsPlus.setVisibility(View.VISIBLE);
                } else {
                    mMultiTeamsPlus.setText("");
                    mMultiTeamsPlus.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // my code here
            }

        });
    }


    //*********************** ON CLICKS ********************************
    public void backToGamePlay() {
        // Use flag instead of UI text comparison for reliable logic
        if (db.wasTimeUp) {
            db.mMillisUntilFinished = db.timePerRound * 1000L;
            db.currentPlaying = (db.currentPlaying + 1)%db.amountOfTeams;
        } else {
            db.resetRound();
        }
        // Clear indicator tracking since we're starting a new turn
        teamThatJustPlayed = -1;
        successCountForTeamThatJustPlayed = 0;
        // Reset pause flag so next Summary will display results properly
        db.summaryIsPaused = false;
        Collections.shuffle(db.defs);
        Intent intent = new Intent(context, GamePlay.class);
        startActivity(intent);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            dialogBag.backToMainMenu(TAG);
            return true;
        }

        return super.onKeyDown(keyCode, event);
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

}