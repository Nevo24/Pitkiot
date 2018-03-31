package nevo_mashiach.pitkiot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import java.util.HashSet;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import nevo_mashiach.pitkiot.NotActivities.DialogBag;
import nevo_mashiach.pitkiot.NotActivities.MyTextView;
import nevo_mashiach.pitkiot.NotActivities.db;

public class GamePlay extends AppCompatActivity {

    public static final String TAG = GamePlay.class.getName();

    Context context;
    @BindView(R.id.currentDef)
    MyTextView mCurrentDef;
    @BindView(R.id.teamNum)
    MyTextView mTeamNum;
    @BindView(R.id.editScoreHeadline)
    MyTextView mTotalNotes;
    @BindView(R.id.currentSuccess)
    MyTextView mCurrentSuccess;
    @BindView(R.id.time)
    MyTextView mTime;
    @BindView(R.id.roundModeGame)
    MyTextView mRoundModeGame;
    @BindView(R.id.gamePlayFigureHappy)
    ImageView mGamePlayFigureHappy;
    @BindView(R.id.gamePlayFigureSad)
    ImageView mGamePlayFigureSad;

    @BindView(R.id.success)
    Button mSuccess;
    @BindView(R.id.next)
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_play);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ButterKnife.bind(this);
        Typeface myTypeFace = Typeface.createFromAsset(getAssets(), "gan.ttf");
        mTeamNum.setTypeface(myTypeFace);
        context = this;
        dialogBag = new DialogBag(getFragmentManager(), this);

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
    }

    @Override
    protected void onPause() {
        super.onPause();
        db.timer.cancel();
        happyAanim.stop();
        sadAanim.stop();

        //Save game state:
        Set<String> set = new HashSet<String>();
        set.addAll(db.temp);
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

    @Override
    protected void onResume() {
        super.onResume();
        mSuccess.setEnabled(true);
        mNext.setEnabled(true);
        if (!db.gamePlayIsPaused) {
            mTeamNum.setText("קבוצה " + (db.currentPlaying + 1));
            mRoundModeGame.setText(db.getRoundMode());
            db.resetTemp();
            setDef();
        } else {
            db.gamePlayIsPaused = false;
            mTotalNotes.setText("מספר פתקים נותרים לסבב: " + db.roundNoteAmount());
            if(!db.noteExists((String)mCurrentDef.getText())) setDef();
        }
        timeGenerate("regular");
        mCurrentSuccess.setText("מספר הצלחות בסיבוב זה: " + db.currentSuccessNum);
    }

    public void setDef() {
        mTotalNotes.setText("מספר פתקים נותרים לסבב: " + db.roundNoteAmount());
        if (db.roundNoteAmount() == 0) {
            db.timer.cancel();
            db.increaseRoundMode();
            summaryDisplay("נגמרו הפתקים!", "המשך! תור קבוצה " + (db.currentPlaying + 1));
            return;
        }
        if (db.defs.size() == 0 && db.temp.size() > 0) {
            db.resetTemp();
        }
        currentDef = db.defs.get(0);
        mCurrentDef.setText(currentDef);
    }

    private synchronized void timeGenerate(String str) {
        if (str.equals("next")) {
            fixedMillisUntilFinished = Math.max(0, db.mMillisUntilFinished - db.timeDownOnNext * 1000);
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
                mTime.setText("זמן נותר: " + sec);
                if ((sec <= 10) && (sec > 0)) db.makeSound(context, R.raw.tick_sound);
            }

            public void onFinish() {
                if (db.defs.size() > 0) {
                    mSuccess.setEnabled(false);
                    mNext.setEnabled(false);
                    mTime.setText("זמן נותר: 0");
                    db.makeSound(context, R.raw.time_is_up_sound);
                    vibrate();
                    int turn = ((db.currentPlaying + 2)%(db.amountOfTeams + 1));
                    if (turn == 0) turn = 1;
                    summaryDisplay("נגמר הזמן!", "התחל! תור קבוצה " + turn);
                }
            }
        }.start();
        sec = (int) fixedMillisUntilFinished / 1000;
        mTime.setText("זמן נותר: " + sec);
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
    @OnClick(R.id.next)
    public synchronized void removeDefFromDB(View view) {

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

    @OnClick(R.id.success)
    public void addingDefToTeam(View view) {

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
        mCurrentSuccess.setText("מספר הצלחות בסיבוב זה: " + db.currentSuccessNum);

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

    @OnTouch({R.id.success, R.id.next})
    boolean onTouch(View view, MotionEvent motion) {
        return db.onTouch(context, view, motion);
    }
}