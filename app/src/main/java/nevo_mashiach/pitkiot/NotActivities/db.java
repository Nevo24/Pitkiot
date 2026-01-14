package nevo_mashiach.pitkiot.NotActivities;

import android.content.Context;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.CountDownTimer;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;
import java.util.ArrayList;

import nevo_mashiach.pitkiot.R;

/**
 * Created by Nevo Mashiach on 10/12/2016.
 */

public class db {

    private static db instance = null;
    private static SoundPool soundPool;
    private static HashMap<Integer, Integer> soundMap;

    //game:
    public static ArrayList<String> defs;
    public static ArrayList<String> temp;
    public static ArrayList<String>[] teamsNotes;
    public static int totalRoundNumber = 0;
    public static int[] teamsRoundNum;
    public static int[] scores;
    public static int currentSuccessNum = 0;
    public static int currentPlaying = 0;
    public static long mMillisUntilFinished = 60 * 1000;
    public static CountDownTimer timer;
    public static double team2AverageAnswersPerSecond;
    public static boolean gameOverDialogActivated = false;


    //settings:
    public static int amountOfTeams;
    public static Boolean autoBalanceCheckBox;
    public static boolean soundCheckBox;
    public static int timePerRound;
    public static int timeDownOnNext;

    //pauses:
    public static boolean gamePlayIsPaused = false;
    public static boolean summaryIsPaused = false;


    @SuppressWarnings("unchecked")
    private db() {
        defs = new ArrayList<>();
        temp = new ArrayList<>();
        teamsNotes = new ArrayList[24];
        for(int i = 0; i < 24 ; i++){
            teamsNotes[i] = new ArrayList<>();
        }
        teamsRoundNum = new int[24];
        scores = new int[24];
    }

    public static void getInstance() {
        if (instance == null) {
            instance = new db();
        }
    }

    public static synchronized void initializeSounds(Context context) {
        if (soundPool == null) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(audioAttributes)
                    .build();

            soundMap = new HashMap<>();
            soundMap.put(R.raw.button_press_sound, soundPool.load(context, R.raw.button_press_sound, 1));
            soundMap.put(R.raw.succsess_sound, soundPool.load(context, R.raw.succsess_sound, 1));
            soundMap.put(R.raw.pass_sound, soundPool.load(context, R.raw.pass_sound, 1));
            soundMap.put(R.raw.tick_sound, soundPool.load(context, R.raw.tick_sound, 1));
            soundMap.put(R.raw.time_is_up_sound, soundPool.load(context, R.raw.time_is_up_sound, 1));
        }
    }

    public static synchronized void releaseSounds() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
            soundMap = null;
        }
    }


    public static synchronized void resetGame() {
        resetRound();
        teamsRoundNum = new int[24];
        scores = new int[24];
        currentSuccessNum = 0;
        currentPlaying = 0; //current playing team
        mMillisUntilFinished = db.timePerRound * 1000L;
        gamePlayIsPaused = false;
        summaryIsPaused = false;
        totalRoundNumber = 0;
    }


    public static synchronized void resetRound() {
        for (int i = 0; i < 24 ; i++) {
            defs.addAll(teamsNotes[i]);
            teamsNotes[i].clear();
        }
        resetTemp();
    }

    public static synchronized void resetTemp() {
        defs.addAll(temp);
        temp.clear();
    }

    public static synchronized void defTransfer(String source, String currentDef) {
        if (source.equals("next")) {
            if (!defs.isEmpty()) {
                defs.remove(0);
            }
            temp.add(currentDef);
        } else if (source.equals("success")) {
            if (!defs.isEmpty()) {
                defs.remove(0);
            }
            teamsNotes[currentPlaying].add(currentDef);
        }
    }

    public static void increaseRoundMode() {
        totalRoundNumber++;
        if (totalRoundNumber == 3) totalRoundNumber = 0;
    }

    public static String getRoundMode(Context context) {
        switch (totalRoundNumber) {
            case 0:
                return context.getString(R.string.mode_explain_without_word);
            case 1:
                return context.getString(R.string.mode_explain_one_word);
            case 2:
                return context.getString(R.string.mode_pantomime);
            default:
                return context.getString(R.string.mode_explain_without_word);
        }
    }

    public static synchronized void increaseRoundNum() {
        teamsRoundNum[currentPlaying]++;
    }

    public static synchronized void increaseScore() {
        scores[currentPlaying]++;
    }

    public static synchronized int totalNoteAmount() {
        int ans = defs.size() + temp.size();
        for (int i = 0; i < 24; i++) {
            ans += teamsNotes[i].size();
        }

        return ans;
    }

    public static synchronized ArrayList<String> allNotes() {
        ArrayList<String> ans = new ArrayList<String>();
        for (int i = 0; i < 24; i++) {
            ans.addAll(teamsNotes[i]);
        }
        ans.addAll(temp);
        ans.addAll(defs);
        return ans;
    }

    public static synchronized boolean noteExists(String note){
        return allNotes().contains(note);
    }

    public static synchronized int roundNoteAmount() {
        return defs.size() + temp.size();
    }

    public static synchronized void makeSound(Context context, int resid) {
        if (!db.soundCheckBox) return;
        if (soundPool != null && soundMap != null && soundMap.containsKey(resid)) {
            Integer soundId = soundMap.get(resid);
            if (soundId != null) {
                soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
            }
        }
    }

    public static boolean onTouch(Context context, View view, MotionEvent motion) {
        if (motion.getAction() == MotionEvent.ACTION_UP) {
            view.setPressed(false);
            view.setPadding(0, 0, 0, 0);
            view.performClick();
            return true; // Consume the event to prevent double-click
        }
        else if(motion.getAction() == MotionEvent.ACTION_MOVE) {
            Rect rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
            if(!rect.contains(view.getLeft() + (int) motion.getX(), view.getTop() + (int) motion.getY())) {
                view.setPressed(false);
                view.setPadding(0, 0, 0, 0);
            }
        }
        else if(motion.getAction() == MotionEvent.ACTION_DOWN) {
            view.setPadding(0, 10, 10, 0);
            makeSound(context, R.raw.button_press_sound);
        }
        return false;
    }

    public static boolean onTouchExplanation(Context context, View view, MotionEvent motion) {
        // Convert dp to pixels for proper padding
        float density = context.getResources().getDisplayMetrics().density;
        int paddingTopPx = (int) (4 * density);  // Normal state
        int paddingTopPressedPx = (int) (6 * density);  // Pressed state (4 + 2 for movement)

        if (motion.getAction() == MotionEvent.ACTION_UP) {
            view.setPressed(false);
            view.setPadding(0, paddingTopPx, 0, 0);
            view.performClick();
            return true; // Consume the event to prevent double-click
        }
        else if(motion.getAction() == MotionEvent.ACTION_MOVE) {
            Rect rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
            if(!rect.contains(view.getLeft() + (int) motion.getX(), view.getTop() + (int) motion.getY())) {
                view.setPressed(false);
                view.setPadding(0, paddingTopPx, 0, 0);
            }
        }
        else if(motion.getAction() == MotionEvent.ACTION_DOWN) {
            view.setPressed(true);
            view.setPadding(0, paddingTopPressedPx, 0, 0);  // Add 2dp movement when pressed
            makeSound(context, R.raw.button_press_sound);
        }
        return false;
    }
}
