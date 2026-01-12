package nevo_mashiach.pitkiot;

import android.annotation.SuppressLint;
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
import androidx.preference.PreferenceManager;
import android.view.KeyEvent;
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
import nevo_mashiach.pitkiot.NotActivities.DialogBag;
import nevo_mashiach.pitkiot.NotActivities.ExitActivity;
import nevo_mashiach.pitkiot.NotActivities.MyButton;
import nevo_mashiach.pitkiot.NotActivities.db;
import nevo_mashiach.pitkiot.databinding.ActivityMainBinding;

@SuppressLint("SourceLockedOrientationActivity")
public class MainActivity extends AppCompatActivity {

    static boolean firstLaunch = true;

    public DialogBag dialogBag;

    Context context;

    TextView mNoteCount;
    MyButton mPlayGame;
    ImageView mAnimationFigure;
    ImageView mGamePlayFigureHappy;
    TextView mSettingsIcon;
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

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Initialize view references
        mNoteCount = binding.noteCount;
        mPlayGame = binding.playGame;
        mAnimationFigure = binding.animationFigure;
        mGamePlayFigureHappy = binding.gamePlayFigureHappy;
        mSettingsIcon = binding.settingsIcon;
        mLanguageToggle = binding.languageToggle;

        dialogBag = new DialogBag(getSupportFragmentManager(), this);
        thisActivity = this;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Make navigation bar transparent
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        db.getInstance();

        // Update flag icon based on current language
        updateLanguageFlag();

        if (firstLaunch) {
            loadDb();
            mNoteCount.setText(String.format(getString(R.string.note_count_database), db.totalNoteAmount()));
            firstLaunch = false;
        }

        // Check for app updates every time MainActivity is created
        UpdateChecker updateChecker = new UpdateChecker(this, getSupportFragmentManager());
        updateChecker.checkForUpdate();
        mGamePlayFigureHappy.setVisibility(View.INVISIBLE);
        mGamePlayFigureHappy.setBackgroundResource(R.drawable.animation_happy);
        happyAanim = (AnimationDrawable) mGamePlayFigureHappy.getBackground();

        // Set up click listeners
        binding.playGame.setOnClickListener(v -> startGamePlayActivity());
        binding.addNote.setOnClickListener(v -> startNoteAdditionActivity());
        binding.reset.setOnClickListener(v -> resetDialogBag());
        binding.animationFigure.setOnClickListener(v -> beHappy());
        binding.settingsIcon.setOnClickListener(v -> settings());
        binding.languageToggle.setOnClickListener(v -> toggleLanguage());

        // Set up touch listeners
        // Note: db.onTouch() internally calls view.performClick() for accessibility
        @SuppressLint("ClickableViewAccessibility")
        View.OnTouchListener touchListener = (v, motion) -> db.onTouch(context, v, motion);
        binding.playGame.setOnTouchListener(touchListener);
        binding.addNote.setOnTouchListener(touchListener);
        binding.reset.setOnTouchListener(touchListener);
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
        //Settings
        try {
            db.autoBalanceCheckBox = prefs.getBoolean("autoBalanceCheckBoxBackup", true);
        } catch (Exception ignored) {
        }
        try {
            db.soundCheckBox = prefs.getBoolean("soundCheckBoxBackup", true);
        } catch (Exception ignored) {
        }
        try {
            db.timePerRound = prefs.getInt("timePerRoundBackup", 60);
        } catch (Exception ignored) {
        }
        try {
            db.timeDownOnNext = prefs.getInt("timeDownOnNextBackup", 5);
        } catch (Exception ignored) {
        }
        try {
            db.amountOfTeams = prefs.getInt("amountOfTeams", 2);
        } catch (Exception ignored) {
        }

        //Game state
        Set<String> set;
        try {
            set = prefs.getStringSet("defs", null);
            db.defs = new ArrayList<>(set);
        } catch (Exception ignored) {
        }
        try {
            set = prefs.getStringSet("temp", null);
            db.temp = new ArrayList<>(set);
        } catch (Exception ignored) {
        }
        for (int i = 0; i < 24; i++) {
            try {
                set = prefs.getStringSet("team" + i + "Notes", null);
                db.teamsNotes[i] = new ArrayList<>(set);
            } catch (Exception ignored) {
            }
            try {
                db.teamsRoundNum[i] = prefs.getInt("team" + i + "RoundNum", 0);
            } catch (Exception e) {
            }
            try {
                db.scores[i] = prefs.getInt("team" + i + "Score", 0);
            } catch (Exception ignored) {
            }
        }
        try {
            db.totalRoundNumber = prefs.getInt("totalRoundNumber", 0);
        } catch (Exception ignored) {
        }
        try {
            db.currentSuccessNum = prefs.getInt("currentSuccessNum", 0);
        } catch (Exception ignored) {
        }
        try {
            db.currentPlaying = prefs.getInt("currentPlaying", 0);
        } catch (Exception ignored) {
        }
        try {
            db.mMillisUntilFinished = prefs.getLong("mMillisUntilFinished", 60 * 1000);
        } catch (Exception ignored) {
        }
        try {
            db.gamePlayIsPaused = prefs.getBoolean("gamePlayIsPaused", false);
        } catch (Exception e) {
        }
        try {
            db.summaryIsPaused = prefs.getBoolean("summaryIsPaused", false);
        } catch (Exception ignored) {
        }
    }


    //*********************** ON CLICKS ********************************
    public void startGamePlayActivity() {
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

    public void startNoteAdditionActivity() {
        Intent intent = new Intent(context, NoteManagement.class);
        startActivity(intent);
    }

    public void resetDialogBag() {
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
                spEditor.putLong("mMillisUntilFinished", db.timePerRound * 1000L);
                spEditor.putBoolean("summaryIsPaused", false);
                spEditor.putBoolean("gamePlayIsPaused", false);
                spEditor.commit();
            }
        };
        dialogBag.resetGame(task);
    }

    public void beHappy() {
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

    public void settings() {
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

    private void loadLanguagePreference() {
        // Check if this is first launch (no language preference set)
        if (!prefs.contains("app_language")) {
            // First launch - detect location and set language
            detectLocationAndSetLanguage();
        } else {
            // Use saved preference
            String language = prefs.getString("app_language", "he");
            setLocale(language);
        }
    }

    private void detectLocationAndSetLanguage() {
        // Try to detect country via IP geolocation
        new Thread(() -> {
            String detectedLanguage = detectCountryFromIP();

            // Save and apply language on main thread
            runOnUiThread(() -> {
                spEditor.putString("app_language", detectedLanguage);
                spEditor.commit();
                setLocale(detectedLanguage);
                recreate(); // Recreate activity to apply the new locale
            });
        }).start();
    }

    private String detectCountryFromIP() {
        try {
            // Use ipapi.co free API to detect country (30k requests/month free)
            java.net.URL url = new java.net.URL("https://ipapi.co/json/");
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Pitkiot-Android-App");
            connection.setConnectTimeout(5000); // 5 second timeout
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String jsonResponse = response.toString();

                // Parse JSON manually to extract country_code
                // Response format: {"country_code": "IL",...} or {"country_code": "US",...}
                String countryCode = null;
                if (jsonResponse.contains("\"country_code\"")) {
                    int keyStart = jsonResponse.indexOf("\"country_code\"");
                    int colonPos = jsonResponse.indexOf(":", keyStart);
                    int start = jsonResponse.indexOf("\"", colonPos) + 1;
                    int end = jsonResponse.indexOf("\"", start);
                    if (start > 0 && end > start) {
                        countryCode = jsonResponse.substring(start, end);
                    }
                }

                // If Israel, return Hebrew, else English
                if ("IL".equalsIgnoreCase(countryCode)) {
                    return "he";
                } else {
                    return "en";
                }
            }
        } catch (Exception e) {
            // API failed, use fallback
        }

        // Fallback: use system locale
        return detectLanguageFromSystemLocale();
    }

    private String detectLanguageFromSystemLocale() {
        String countryCode = Locale.getDefault().getCountry();

        // If Israel, return Hebrew, else English
        if ("IL".equalsIgnoreCase(countryCode)) {
            return "he";
        } else {
            return "en";
        }
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

    public void toggleLanguage() {
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
