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

        // CRITICAL FIX: Check if game over dialog should be shown (app was closed during dialog)
        if (db.shouldShowGameOverDialog) {
            // Hide the ready button and round mode summary for game over state
            mReady.setVisibility(View.INVISIBLE);
            mRoundModeSummary.setVisibility(View.INVISIBLE);
            // Set the summary headline to "Game is over"
            mSummaryHeadline.setText(getString(R.string.dialog_game_over_title));
            mPressHereFigure.setVisibility(View.INVISIBLE);

            // Show the appropriate game over dialog based on saved state
            if (db.gameOverDialogType.equals("normal")) {
                dialogBag.normalGameOver(db.gameOverWinningTeam, db.gameOverWinningScore,
                    db.gameOverLosingScore, db.gameOverAutoBalanceApplied);
            } else if (db.gameOverDialogType.equals("draw")) {
                dialogBag.drawGameOver(db.gameOverWinningScore, db.gameOverAutoBalanceApplied);
            } else if (db.gameOverDialogType.equals("multi")) {
                dialogBag.multiGameOver(db.gameOverAllScores, db.gameOverAutoBalanceApplied);
            }
            // Don't return - continue with normal onResume to update UI
        }

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
                    // Hide the ready button and round mode summary for game over state
                    mReady.setVisibility(View.INVISIBLE);
                    mRoundModeSummary.setVisibility(View.INVISIBLE);
                    // Set the summary headline to "Game is over"
                    mSummaryHeadline.setText(getString(R.string.dialog_game_over_title));
                    // Show the "+x points" indicator for the team that just played
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
                    db.increaseRoundNum();
                    //db.currentPlaying = (db.currentPlaying + 1)%db.amountOfTeams; -- Maybe consider adding it
                    boolean autoBalanceApplied = db.autoBalanceCheckBox;
                    if (autoBalanceApplied) autoBalance();  //If auto balance is checked
                    gameOverDialogCall(autoBalanceApplied);
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

            // Check if game is over - if so, keep UI elements hidden
            if (db.shouldShowGameOverDialog) {
                mReady.setVisibility(View.INVISIBLE);
                mRoundModeSummary.setVisibility(View.INVISIBLE);
                mSummaryHeadline.setText(getString(R.string.dialog_game_over_title));
                mPressHereFigure.setVisibility(View.INVISIBLE);
            } else {
                // Normal case - ensure views are visible
                mReady.setVisibility(View.VISIBLE);
                mRoundModeSummary.setVisibility(View.VISIBLE);
                mPressHereFigure.setVisibility(View.VISIBLE);
                // Refresh localized strings in case language was changed
                mRoundModeSummary.setText(db.getRoundMode(context));
            }
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

        // FIX: Check if value exists before using default to prevent precision loss
        if (prefs.contains("team2AverageAnswersPerSecond")) {
            db.team2AverageAnswersPerSecond = Double.longBitsToDouble(prefs.getLong("team2AverageAnswersPerSecond", 0));
        } else {
            db.team2AverageAnswersPerSecond = 0.0;
        }
        teamThatJustPlayed = prefs.getInt("teamThatJustPlayed", -1);
        successCountForTeamThatJustPlayed = prefs.getInt("successCountForTeamThatJustPlayed", 0);

        // Load game over dialog state
        db.shouldShowGameOverDialog = prefs.getBoolean("shouldShowGameOverDialog", false);
        db.gameOverDialogType = prefs.getString("gameOverDialogType", "");
        db.gameOverWinningTeam = prefs.getInt("gameOverWinningTeam", -1);
        db.gameOverWinningScore = prefs.getInt("gameOverWinningScore", 0);
        db.gameOverLosingScore = prefs.getInt("gameOverLosingScore", 0);
        db.gameOverAutoBalanceApplied = prefs.getBoolean("gameOverAutoBalanceApplied", false);
        // Load all team scores for multi-team game over
        for (int i = 0; i < db.amountOfTeams; i++) {
            db.gameOverAllScores[i] = prefs.getInt("gameOverScore" + i, 0);
        }
    }

    public void gameOverDialogCall(boolean autoBalanceApplied) {
        if (db.amountOfTeams == 2) {
            if (db.scores[0] == db.scores[1]) dialogBag.drawGameOver(db.scores[0], autoBalanceApplied);
            else {
                if (db.scores[0] >  db.scores[1])
                    dialogBag.normalGameOver(1, db.scores[0], db.scores[1], autoBalanceApplied);
                else dialogBag.normalGameOver(2, db.scores[1], db.scores[0], autoBalanceApplied);
            }
        } else {
            dialogBag.multiGameOver(db.scores, autoBalanceApplied);
        }
    }

    /**
     * Calculates the actual playing time in seconds for a given team.
     * Takes into account whether the team completed all rounds or stopped mid-round.
     *
     * @param teamIndex The team index (0 for team 1, 1 for team 2, etc.)
     * @return The total playing time in seconds
     */
    private double calculateTeamPlayingTime(int teamIndex) {
        if (db.currentPlaying == teamIndex) {
            // This team played last - the last round is partial
            return (db.teamsRoundNum[teamIndex] - 1) * db.timePerRound
                 + (db.timePerRound - db.mMillisUntilFinished / 1000.0);
        } else {
            // This team completed all its rounds
            return db.teamsRoundNum[teamIndex] * db.timePerRound;
        }
    }

    public void autoBalance() {
        // Step 1: Calculate how many seconds team 1 played
        double team1PlayingTime = calculateTeamPlayingTime(0);

        // Step 2: Balance all other teams against team 1
        for (int i = 1; i < db.amountOfTeams; i++) {
            // Skip teams that didn't play at all
            if (db.teamsRoundNum[i] == 0) continue;

            // Calculate how many seconds this team played
            double teamPlayingTime = calculateTeamPlayingTime(i);

            // Add points only if this team played less time than team 1
            if (teamPlayingTime < team1PlayingTime && teamPlayingTime > 0) {
                double missingTime = team1PlayingTime - teamPlayingTime;
                double averagePointsPerSecond = db.scores[i] / teamPlayingTime;
                int pointsToAdd = (int) Math.round(missingTime * averagePointsPerSecond);
                db.scores[i] += pointsToAdd;
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

        // CRITICAL FIX: Save the correct pause state based on current situation
        // If transitioning to GamePlay, save gamePlayIsPaused=true
        // If game over dialog is active, don't mark as paused (user will resume to dialog)
        // Otherwise, this is a regular pause (backgrounding), save summaryIsPaused=true
        // FIX: Don't clear the transition flag here - it will be cleared after commit succeeds
        boolean isTransitioning = db.isTransitioningToGamePlay;
        if (isTransitioning) {
            spEditor.putBoolean("summaryIsPaused", false);
            spEditor.putBoolean("gamePlayIsPaused", true);
        } else if (db.shouldShowGameOverDialog) {
            // Game over dialog is pending - keep summaryIsPaused true so we resume to Summary
            // and show the dialog there
            spEditor.putBoolean("summaryIsPaused", true);
            spEditor.putBoolean("gamePlayIsPaused", false);
        } else {
            spEditor.putBoolean("summaryIsPaused", true);
            spEditor.putBoolean("gamePlayIsPaused", false);
        }

        spEditor.putBoolean("wasTimeUp", db.wasTimeUp);
        // FIX: Capture value to local variable to prevent race condition
        double team2AvgSnapshot = db.team2AverageAnswersPerSecond;
        spEditor.putLong("team2AverageAnswersPerSecond", Double.doubleToRawLongBits(team2AvgSnapshot));
        spEditor.putInt("teamThatJustPlayed", teamThatJustPlayed);
        spEditor.putInt("successCountForTeamThatJustPlayed", successCountForTeamThatJustPlayed);

        //Save semantic state (not localized text) so it can be regenerated in any language
        spEditor.putString("summaryState", summaryState);
        spEditor.putInt("nextTeamForButton", nextTeamForButton);

        // Save game over dialog state so it can be reshown after app is closed
        spEditor.putBoolean("shouldShowGameOverDialog", db.shouldShowGameOverDialog);
        spEditor.putString("gameOverDialogType", db.gameOverDialogType);
        spEditor.putInt("gameOverWinningTeam", db.gameOverWinningTeam);
        spEditor.putInt("gameOverWinningScore", db.gameOverWinningScore);
        spEditor.putInt("gameOverLosingScore", db.gameOverLosingScore);
        spEditor.putBoolean("gameOverAutoBalanceApplied", db.gameOverAutoBalanceApplied);
        // Save all team scores for multi-team game over
        for (int i = 0; i < db.amountOfTeams; i++) {
            spEditor.putInt("gameOverScore" + i, db.gameOverAllScores[i]);
        }

        // FIX: Use commit() for critical game state to ensure synchronous write
        // This guarantees state is saved before the activity is destroyed
        if (!spEditor.commit()) {
            android.util.Log.e(TAG, "Failed to save game state!");
        }

        // FIX: Clear transition flag AFTER commit succeeds to prevent race condition
        // This ensures the flag is saved correctly before being cleared
        if (isTransitioning) {
            db.isTransitioningToGamePlay = false;
        }
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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item_tight, items) {

            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                Typeface externalFont = Typeface.createFromAsset(getAssets(), "gan.ttf");
                TextView textView = (TextView) v;
                textView.setTypeface(externalFont);
                textView.setTextColor(0x88000000);
                textView.setTextSize(25);
                textView.setGravity(android.view.Gravity.CENTER);

                // Set layout direction based on language
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    if (language.equals("he")) {
                        // Hebrew RTL: arrow on the left (end of RTL)
                        textView.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
                    } else {
                        // English LTR: arrow on the right (end of LTR)
                        textView.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_LTR);
                    }
                }

                // Add dropdown arrow with padding
                textView.setCompoundDrawablePadding(8);
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
                TextView textView;
                if (convertView == null) {
                    textView = new TextView(Summary.this);
                    float scale = getResources().getDisplayMetrics().density;
                    textView.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    textView.setPadding((int) (24 * scale), (int) (16 * scale), (int) (24 * scale), (int) (16 * scale));
                } else {
                    textView = (TextView) convertView;
                }

                Typeface externalFont = Typeface.createFromAsset(getAssets(), "gan.ttf");
                textView.setTypeface(externalFont);
                textView.setTextColor(0x88000000);
                textView.setTextSize(25);
                textView.setGravity(android.view.Gravity.CENTER);
                textView.setText(getItem(position));

                return textView;
            }
        };

        Spinner spinner = findViewById(R.id.summarySpinner);
        spinner.setAdapter(adapter);

        // Calculate dropdown width based on text content
        android.graphics.Paint paint = new android.graphics.Paint();
        Typeface externalFont = Typeface.createFromAsset(getAssets(), "gan.ttf");
        paint.setTypeface(externalFont);
        paint.setTextSize(25 * getResources().getDisplayMetrics().scaledDensity);

        float maxWidth = 0;
        for (String item : items) {
            float textWidth = paint.measureText(item);
            if (textWidth > maxWidth) {
                maxWidth = textWidth;
            }
        }

        // Add padding to the width
        float scale = getResources().getDisplayMetrics().density;
        int dropdownWidth = (int) (maxWidth + 48 * scale);
        spinner.setDropDownWidth(dropdownWidth);

        // Remove all spinner padding to minimize gap
        spinner.setPadding(0, 0, 0, 0);

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

        // Override spinner click to manually control popup position
        spinner.setOnTouchListener(new View.OnTouchListener() {
            private android.widget.ListPopupWindow customPopup;

            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    if (customPopup != null && customPopup.isShowing()) {
                        customPopup.dismiss();
                        return true;
                    }

                    // Create custom popup
                    customPopup = new android.widget.ListPopupWindow(Summary.this);

                    // Create simple adapter without checkmarks, with custom styling
                    android.widget.ArrayAdapter<String> simpleAdapter = new android.widget.ArrayAdapter<String>(
                        Summary.this,
                        android.R.layout.simple_list_item_1,
                        android.R.id.text1,
                        items
                    ) {
                        @NonNull
                        @Override
                        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                            View v = super.getView(position, convertView, parent);
                            Typeface externalFont = Typeface.createFromAsset(getAssets(), "gan.ttf");
                            TextView textView = (TextView) v;
                            textView.setTypeface(externalFont);
                            textView.setTextColor(0x88000000);
                            textView.setTextSize(25);
                            textView.setGravity(android.view.Gravity.CENTER);
                            // Add extra horizontal padding to prevent text cutoff
                            float scale = getResources().getDisplayMetrics().density;
                            textView.setPadding((int) (16 * scale), textView.getPaddingTop(), (int) (16 * scale), textView.getPaddingBottom());
                            return v;
                        }
                    };
                    customPopup.setAdapter(simpleAdapter);
                    customPopup.setAnchorView(spinner);

                    // Get positions for manual calculation
                    int[] spinnerLocation = new int[2];
                    spinner.getLocationInWindow(spinnerLocation);
                    int spinnerY = spinnerLocation[1];
                    int spinnerHeight = spinner.getHeight();

                    int[] belowLocation = new int[2];
                    mMultiTeamTotalScore.getLocationInWindow(belowLocation);
                    int belowY = belowLocation[1];

                    // Calculate offset - dropdown should start at the element below (belowY position)
                    // verticalOffset is relative to the bottom of the anchor view (spinner)
                    int offsetFromSpinnerBottom = belowY - (spinnerY + spinnerHeight);
                    int screenHeight = getResources().getDisplayMetrics().heightPixels;
                    int dropdownY = belowY;
                    int availableHeight = screenHeight - dropdownY - 50;

                    customPopup.setWidth(spinner.getWidth());

                    // Calculate the actual height needed for the content
                    // Measure actual item view height more accurately
                    float scale = getResources().getDisplayMetrics().density;
                    float textSizePx = 25 * getResources().getDisplayMetrics().scaledDensity;
                    // Fine-tuned padding for tight but not too tight spacing
                    int itemHeight = (int) (24 * scale + textSizePx * 0.96f);
                    int contentHeight = items.length * itemHeight;

                    // Use content height if it fits, otherwise use available height
                    int dropdownHeight = Math.min(contentHeight, availableHeight);
                    customPopup.setHeight(Math.max(dropdownHeight, 100));
                    customPopup.setVerticalOffset(offsetFromSpinnerBottom);
                    customPopup.setModal(true);

                    // Access underlying PopupWindow to force position
                    try {
                        java.lang.reflect.Field popupField = android.widget.ListPopupWindow.class.getDeclaredField("mPopup");
                        popupField.setAccessible(true);
                        Object popupObj = popupField.get(customPopup);

                        if (popupObj instanceof android.widget.PopupWindow) {
                            android.widget.PopupWindow popupWindow = (android.widget.PopupWindow) popupObj;
                            popupWindow.setClippingEnabled(false);
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                popupWindow.setOverlapAnchor(false);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    customPopup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            spinner.setSelection(position);
                            customPopup.dismiss();
                        }
                    });

                    customPopup.show();
                    return true;
                }
                return false;
            }
        });

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

        // CRITICAL FIX: Set transition flag ONLY
        // The transition flag tells onPause() to save gamePlayIsPaused=true to SharedPreferences
        db.isTransitioningToGamePlay = true;
        db.summaryIsPaused = false; // Clear this so we don't think we're paused

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