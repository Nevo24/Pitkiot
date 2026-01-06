package nevo_mashiach.pitkiot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.LocaleList;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import nevo_mashiach.pitkiot.NotActivities.DialogBag;
import nevo_mashiach.pitkiot.NotActivities.ExitActivity;
import nevo_mashiach.pitkiot.NotActivities.MyButton;
import nevo_mashiach.pitkiot.NotActivities.db;


public class MainActivity extends AppCompatActivity {

    static boolean firstLaunch = true;

    public DialogBag dialogBag;

    Context context;
    @BindView(R.id.noteCount)
    TextView mNoteCount;
    @BindView(R.id.playGame)
    MyButton mPlayGame;
    @BindView(R.id.animationFigure)
    ImageView mAnimationFigure;
    @BindView(R.id.gamePlayFigureHappy)
    ImageView mGamePlayFigureHappy;
    @BindView(R.id.settingsIcon)
    TextView mSettingsIcon;
    @BindView(R.id.languageToggle)
    TextView mLanguageToggle;
    static AppCompatActivity thisActivity;
    AnimationDrawable happyAanim;
    CountDownTimer animationTimer;

    SharedPreferences prefs;
    SharedPreferences.Editor spEditor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent().getBooleanExtra("EXIT", false)) {
            finish();
            System.exit(0);
        }
        super.onCreate(savedInstanceState);

        // Initialize preferences and load language first
        context = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        spEditor = prefs.edit();
        loadLanguagePreference();

        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ButterKnife.bind(this);
        dialogBag = new DialogBag(getFragmentManager(), this);
        thisActivity = this;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        db.getInstance();

        // Update flag icon based on current language
        updateLanguageFlag();

        if (firstLaunch) {
            loadDb();
            mNoteCount.setText(String.format(getString(R.string.note_count_database), db.totalNoteAmount()));
            firstLaunch = false;
        }
        mGamePlayFigureHappy.setVisibility(View.INVISIBLE);
        mGamePlayFigureHappy.setBackgroundResource(R.drawable.animation_happy);
        happyAanim = (AnimationDrawable) mGamePlayFigureHappy.getBackground();
    }

    protected void onResume() {
        super.onResume();
        mNoteCount.setText(String.format(getString(R.string.note_count_database), db.totalNoteAmount()));
        if (db.gamePlayIsPaused || db.summaryIsPaused) mPlayGame.setText(getString(R.string.button_return_to_game));
        else mPlayGame.setText(getString(R.string.button_start_new_game));

        mAnimationFigure.setBackgroundResource(R.drawable.animation_breathe);
        AnimationDrawable anim = (AnimationDrawable) mAnimationFigure.getBackground();
        anim.start();
    }

    private void loadDb() {
        //smsTime
        try {
            db.smsTime = prefs.getLong("smsTime", System.currentTimeMillis());
        } catch (Exception e) {
        }

        //Settings
        try {
            db.autoBalanceCheckBox = prefs.getBoolean("autoBalanceCheckBoxBackup", true);
        } catch (Exception e) {
        }
        try {
            db.soundCheckBox = prefs.getBoolean("soundCheckBoxBackup", true);
        } catch (Exception e) {
        }
        try {
            db.timePerRound = prefs.getInt("timePerRoundBackup", 60);
        } catch (Exception e) {
        }
        try {
            db.timeDownOnNext = prefs.getInt("timeDownOnNextBackup", 5);
        } catch (Exception e) {
        }
        try {
            db.amountOfTeams = prefs.getInt("amountOfTeams", 2);
        } catch (Exception e) {
        }

        //Game state
        Set<String> set;
        try {
            set = prefs.getStringSet("defs", null);
            db.defs = new ArrayList<>(set);
        } catch (Exception e) {
        }
        try {
            set = prefs.getStringSet("temp", null);
            db.temp = new ArrayList<>(set);
        } catch (Exception e) {
        }
        for (int i = 0; i < 24; i++) {
            try {
                set = prefs.getStringSet("team" + i + "Notes", null);
                db.teamsNotes[i] = new ArrayList<>(set);
            } catch (Exception e) {
            }
            try {
                db.teamsRoundNum[i] = prefs.getInt("team" + i + "RoundNum", 0);
            } catch (Exception e) {
            }
            try {
                db.scores[i] = prefs.getInt("team" + i + "Score", 0);
            } catch (Exception e) {
            }
        }
        try {
            db.totalRoundNumber = prefs.getInt("totalRoundNumber", 0);
        } catch (Exception e) {
        }
        try {
            db.currentSuccessNum = prefs.getInt("currentSuccessNum", 0);
        } catch (Exception e) {
        }
        try {
            db.currentPlaying = prefs.getInt("currentPlaying", 0);
        } catch (Exception e) {
        }
        try {
            db.mMillisUntilFinished = prefs.getLong("mMillisUntilFinished", 60 * 1000);
        } catch (Exception e) {
        }
        try {
            db.gamePlayIsPaused = prefs.getBoolean("gamePlayIsPaused", false);
        } catch (Exception e) {
        }
        try {
            db.summaryIsPaused = prefs.getBoolean("summaryIsPaused", false);
        } catch (Exception e) {
        }
    }


    //*********************** ON CLICKS ********************************
    @OnClick(R.id.playGame)
    public void startGamePlayActivity(View view) {
        if (db.totalNoteAmount() == 0) {
            dialogBag.dbEmptyBeforeGame();
            return;
        }
        if (db.summaryIsPaused || db.gamePlayIsPaused) {
            Runnable task = new Runnable() {
                public void run() {
                    if (db.summaryIsPaused) {
                        Intent intent = new Intent(context, Summary.class);
                        startActivity(intent);
                        return;
                    }
                    Intent intent = new Intent(context, GamePlay.class);
                    startActivity(intent);
                }
            };
            dialogBag.resumeGame(task);
        } else {
            Collections.shuffle(db.defs);
            Intent intent = new Intent(context, GamePlay.class);
            startActivity(intent);
        }
    }

    @OnClick(R.id.addNote)
    public void startNoteAdditionActivity(View view) {
        Intent intent = new Intent(context, NoteManagement.class);
        startActivity(intent);
    }

    @OnClick(R.id.reset)
    public void resetDialogBag(View view) {
        Runnable task = new Runnable() {
            public void run() {
                db.resetGame();
                mPlayGame.setText(getString(R.string.button_start_new_game));

                //reset game state:
                Set<String> set = new HashSet<String>();
                spEditor.putStringSet("temp", set);
                for (int i = 0; i < 24; i++) {
                    spEditor.putStringSet("team" + i + "Notes", set);
                    spEditor.putInt("team" + i + "RoundNum", 0);
                    spEditor.putInt("team" + i + "Score", 0);
                }
                spEditor.putInt("totalRoundNumber", 0);
                spEditor.putInt("currentSuccessNum", 0);
                spEditor.putInt("currentPlaying", 0);
                spEditor.putLong("mMillisUntilFinished", db.timePerRound * 1000);
                spEditor.putBoolean("summaryIsPaused", false);
                spEditor.putBoolean("gamePlayIsPaused", false);
                spEditor.commit();
            }
        };
        dialogBag.resetGame(task);
    }

    @OnClick(R.id.animationFigure)
    public void beHappy(View view) {
        mAnimationFigure.setVisibility(View.INVISIBLE);
        mGamePlayFigureHappy.setVisibility(View.VISIBLE);
        happyAanim.stop();
        happyAanim.start();
        animationTimer = new CountDownTimer(2000, 2000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                mAnimationFigure.setVisibility(View.VISIBLE);
                mGamePlayFigureHappy.setVisibility(View.INVISIBLE);
            }
        }.start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            dialogBag.exitTheGame();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @OnClick(R.id.settingsIcon)
    public void settings(View view) {
        Intent intent = new Intent(context, Settings.class);
        startActivity(intent);
    }

    public static void appExit() {
        Intent intent = new Intent(thisActivity, ExitActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        thisActivity.startActivity(intent);
        //int pid = android.os.Process.myPid();
        //android.os.Process.killProcess(pid);
    }

    @OnTouch({R.id.playGame, R.id.addNote, R.id.reset})
    boolean onTouch(View view, MotionEvent motion) {
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

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLayoutDirection(locale);
        }

        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    @OnClick(R.id.languageToggle)
    public void toggleLanguage(View view) {
        String currentLang = prefs.getString("app_language", "he");
        String newLang = currentLang.equals("he") ? "en" : "he";

        // Save new language preference
        spEditor.putString("app_language", newLang);
        spEditor.commit();

        // Apply new language and recreate activity
        setLocale(newLang);
        recreate();
    }

    private void updateLanguageFlag() {
        String currentLang = prefs.getString("app_language", "he");
        if (currentLang.equals("he")) {
            mLanguageToggle.setText("🇺🇸"); // Show US flag when in Hebrew (to switch to English)
        } else {
            mLanguageToggle.setText("🇮🇱"); // Show Israel flag when in English (to switch to Hebrew)
        }
    }
}