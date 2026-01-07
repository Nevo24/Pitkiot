package nevo_mashiach.pitkiot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.LocaleList;
import android.os.SystemClock;
import android.os.Vibrator;
import androidx.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import java.util.Locale;

import java.util.HashSet;
import java.util.Set;

import androidx.appcompat.app.AppCompatActivity;
import nevo_mashiach.pitkiot.NotActivities.DialogBag;
import nevo_mashiach.pitkiot.NotActivities.MyTextView;
import nevo_mashiach.pitkiot.NotActivities.db;
import nevo_mashiach.pitkiot.databinding.ActivityGamePlayBinding;

@SuppressLint("SourceLockedOrientationActivity")
public class GamePlay extends AppCompatActivity {

    public static final String TAG = GamePlay.class.getName();

    Context context;

    MyTextView mCurrentDef;
    MyTextView mTeamNum;
    MyTextView mTotalNotes;
    MyTextView mCurrentSuccess;
    MyTextView mTime;
    MyTextView mRoundModeGame;
    ImageView mGamePlayFigureHappy;
    ImageView mGamePlayFigureSad;
    Button mSuccess;
    Button mNext;

    public DialogBag dialogBag;
    String currentDef = "nada";
    long fixedMillisUntilFinished;
    int sec;
    long oldMillis;
    boolean firstTime = true;
    AnimationDrawable happyAanim;
    AnimationDrawable sadAanim;

    SharedPreferences prefs;
    SharedPreferences.Editor spEditor;

    private final long minimumInterval = 150;
    private long previousClickTimestamp = 0;

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
        ActivityGamePlayBinding binding = ActivityGamePlayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Initialize view references
        mCurrentDef = binding.currentDef;
        mTeamNum = binding.teamNum;
        mTotalNotes = binding.editScoreHeadline;
        mCurrentSuccess = binding.currentSuccess;
        mTime = binding.time;
        mRoundModeGame = binding.roundModeGame;
        mGamePlayFigureHappy = binding.gamePlayFigureHappy;
        mGamePlayFigureSad = binding.gamePlayFigureSad;
        mSuccess = binding.success;
        mNext = binding.next;

        Typeface myTypeFace = Typeface.createFromAsset(getAssets(), "gan.ttf");
        mTeamNum.setTypeface(myTypeFace);
        context = this;
        dialogBag = new DialogBag(getSupportFragmentManager(), this);

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        spEditor = prefs.edit();

        mGamePlayFigureHappy.setBackgroundResource(R.drawable.animation_happy);
        mGamePlayFigureSad.setVisibility(View.VISIBLE);
        happyAanim = (AnimationDrawable) mGamePlayFigureHappy.getBackground();

        mGamePlayFigureSad.setBackgroundResource(R.drawable.animation_sad);
        mGamePlayFigureSad.setVisibility(View.INVISIBLE);
        sadAanim = (AnimationDrawable) mGamePlayFigureSad.getBackground();

        if (firstTime) {
            mTeamNum.setText(prefs.getString("teamNum", ""));
            mCurrentSuccess.setText(prefs.getString("currentSuccess", ""));
            mRoundModeGame.setText(prefs.getString("roundModeGame", ""));
            mCurrentDef.setText(prefs.getString("currentDef", ""));
            fixedMillisUntilFinished = prefs.getLong("fixedMillisUntilFinished", 60);
            currentDef = prefs.getString("currentDefString", "");
            firstTime = false;
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Set up click listeners
        binding.next.setOnClickListener(v -> removeDefFromDB());
        binding.success.setOnClickListener(v -> addingDefToTeam());

        // Set up touch listeners
        // Note: db.onTouch() internally calls view.performClick() for accessibility
        @SuppressLint("ClickableViewAccessibility")
        View.OnTouchListener touchListener = (v, motion) -> db.onTouch(context, v, motion);
        binding.success.setOnTouchListener(touchListener);
        binding.next.setOnTouchListener(touchListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        db.timer.cancel();
        happyAanim.stop();
        sadAanim.stop();

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
        spEditor.putLong("team2AverageAnswersPerSecond", Double.doubleToRawLongBits(db.team2AverageAnswersPerSecond));
        spEditor.putInt("totalRoundNumber", db.totalRoundNumber);
        spEditor.putInt("currentSuccessNum", db.currentSuccessNum);
        spEditor.putInt("currentPlaying", db.currentPlaying);
        spEditor.putLong("mMillisUntilFinished", db.mMillisUntilFinished);
        spEditor.putBoolean("summaryIsPaused", false);
        spEditor.putBoolean("gamePlayIsPaused", true);

        //Save text-info
        spEditor.putString("teamNum", mTeamNum.getText().toString());
        spEditor.putString("currentSuccess", mCurrentSuccess.getText().toString());
        spEditor.putString("roundModeGame", mRoundModeGame.getText().toString());
        spEditor.putString("currentDef", mCurrentDef.getText().toString());
        spEditor.putLong("fixedMillisUntilFinished", fixedMillisUntilFinished);
        spEditor.putString("currentDefString", currentDef);
        spEditor.commit();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();
        mSuccess.setEnabled(true);
        mNext.setEnabled(true);
        if (!db.gamePlayIsPaused) {
            mTeamNum.setText(getString(R.string.game_team_label) + (db.currentPlaying + 1));
            mRoundModeGame.setText(db.getRoundMode(context));
            db.resetTemp();
            setDef();
        } else {
            db.gamePlayIsPaused = false;
            // Refresh localized strings in case language was changed
            mTeamNum.setText(getString(R.string.game_team_label) + (db.currentPlaying + 1));
            mRoundModeGame.setText(db.getRoundMode(context));
            mTotalNotes.setText(getString(R.string.game_notes_remaining, db.roundNoteAmount()));
            if(db.defs.isEmpty() || !mCurrentDef.getText().equals(db.defs.get(0))) setDef();
        }
        timeGenerate("regular");
        mCurrentSuccess.setText(getString(R.string.game_successes_this_round) + db.currentSuccessNum);
    }

    @SuppressLint("SetTextI18n")
    public void setDef() {
        mTotalNotes.setText(getString(R.string.game_notes_remaining, db.roundNoteAmount()));
        if (db.roundNoteAmount() == 0) {
            db.timer.cancel();
            db.increaseRoundMode();
            summaryDisplay(getString(R.string.game_notes_finished), getString(R.string.game_continue_team_turn) + (db.currentPlaying + 1));
            return;
        }
        if (db.defs.isEmpty() && !db.temp.isEmpty()) {
            db.resetTemp();
        }
        currentDef = db.defs.get(0);
        mCurrentDef.setText(currentDef);
    }

    @SuppressLint("SetTextI18n")
    private synchronized void timeGenerate(String str) {
        if (str.equals("next")) {
            fixedMillisUntilFinished = Math.max(0, db.mMillisUntilFinished - db.timeDownOnNext * 1000L);
            db.timer.cancel();
            if(fixedMillisUntilFinished == 0){
                db.timer.onFinish();
                return;
            }
        } else fixedMillisUntilFinished = db.mMillisUntilFinished;
        db.timer = new CountDownTimer(fixedMillisUntilFinished, 100) {
            public void onTick(long millisUntilFinished) {
                oldMillis = db.mMillisUntilFinished;
                db.mMillisUntilFinished = millisUntilFinished;
                if (millisUntilFinished / 1000 == oldMillis / 1000) return;
                sec = (int) millisUntilFinished / 1000 + 1;
                mTime.setText(getString(R.string.game_time_remaining) + sec);
                if ((sec <= 10) && (sec > 0)) db.makeSound(context, R.raw.tick_sound);
            }

            public void onFinish() {
                if (!db.defs.isEmpty()) {
                    mSuccess.setEnabled(false);
                    mNext.setEnabled(false);
                    mTime.setText(getString(R.string.game_time_remaining) + "0");
                    db.makeSound(context, R.raw.time_is_up_sound);
                    vibrate();
                    int turn = ((db.currentPlaying + 1) % db.amountOfTeams) + 1;
                    summaryDisplay(getString(R.string.game_time_up), getString(R.string.game_start_team_turn) + turn);
                }
            }
        }.start();
        sec = (int) fixedMillisUntilFinished / 1000;
        mTime.setText(getString(R.string.game_time_remaining) + sec);
    }

    public void summaryDisplay(String headline, String buttonText) {
        Intent intent = new Intent(context, Summary.class);
        intent.putExtra("summaryHeadline", headline);
        intent.putExtra("readyText", buttonText);
        startActivity(intent);
    }


    public void vibrate() {
        Vibrator v = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        v.vibrate(500);
    }


    //*********************** ON CLICKS ********************************
    @SuppressLint("NonConstantResourceId")
    public synchronized void removeDefFromDB() {

        //Fast clicking defence
        long currentTimestamp = SystemClock.uptimeMillis();
        long currentPreviousClickTimestamp = previousClickTimestamp;
        previousClickTimestamp = currentTimestamp;
        if(!(currentPreviousClickTimestamp == 0 || (currentTimestamp - currentPreviousClickTimestamp > minimumInterval))) {
            return;
        }

        //onClick
        mSuccess.setEnabled(false);
        mNext.setEnabled(false);
        db.makeSound(context, R.raw.pass_sound);
        mGamePlayFigureHappy.setVisibility(View.INVISIBLE);
        mGamePlayFigureSad.setVisibility(View.VISIBLE);
        sadAanim.stop();
        sadAanim.start();
        db.defTransfer("next", currentDef);
        setDef();
        timeGenerate("next");
        mSuccess.setEnabled(true);
        mNext.setEnabled(true);
    }

    @SuppressLint("SetTextI18n")
    public void addingDefToTeam() {

        //Fast clicking defence
        long currentTimestamp = SystemClock.uptimeMillis();
        long currentPreviousClickTimestamp = previousClickTimestamp;
        previousClickTimestamp = currentTimestamp;
        if(!(currentPreviousClickTimestamp == 0 || (currentTimestamp - currentPreviousClickTimestamp > minimumInterval))) {
            return;
        }


        //onClick
        mSuccess.setEnabled(false);
        mNext.setEnabled(false);
        db.makeSound(context, R.raw.succsess_sound);
        mGamePlayFigureSad.setVisibility(View.INVISIBLE);
        mGamePlayFigureHappy.setVisibility(View.VISIBLE);
        happyAanim.stop();
        happyAanim.start();
        db.currentSuccessNum++;
        db.increseScore();
        mCurrentSuccess.setText(getString(R.string.game_successes_this_round) + db.currentSuccessNum);

        db.defTransfer("success", currentDef);
        setDef();
        mSuccess.setEnabled(true);
        mNext.setEnabled(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            dialogBag.backToMainMenu(TAG);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
