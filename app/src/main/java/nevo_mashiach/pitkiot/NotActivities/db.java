package nevo_mashiach.pitkiot.NotActivities;

import android.content.Context;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

import nevo_mashiach.pitkiot.R;

/**
 * Created by Nevo2 on 10/12/2016.
 */

public class db {

    private static db instance = null;
    private static MediaPlayer mPlayer;

    //game:
    public static ArrayList<String> defs;
    public static ArrayList<String> temp;
    public static ArrayList<String>[] teamsNotes;
    public static int totalRoundNumber = 0;
    public static int[] teamsRoundNum;
    public static int[] scores;
    public static int currentSuccessNum = 0;
    public static int currentPlaying = 1;
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

    public static long smsTime;


    private db() {
        defs = new ArrayList<String>();
        temp = new ArrayList<String>();
        teamsNotes = new ArrayList[24];
        for(int i = 0; i < 24 ; i++){
            teamsNotes[i] = new ArrayList<String>();
        }
        teamsRoundNum = new int[24];
        scores = new int[24];
    }

    public static db getInstance() {
        if (instance == null) {
            instance = new db();
        }
        return instance;
    }


    public static void resetGame() {
        resetRound();
        teamsRoundNum = new int[24];
        scores = new int[24];
        currentSuccessNum = 0;
        currentPlaying = 0; //current playing team
        mMillisUntilFinished = db.timePerRound * 1000;
        gamePlayIsPaused = false;
        summaryIsPaused = false;
    }


    public static void resetRound() {
        for (int i = 0; i < 24 ; i++) {
            defs.addAll(teamsNotes[i]);
            teamsNotes[i].clear();
        }
        resetTemp();
    }

    public static void resetTemp() {
        defs.addAll(temp);
        temp.clear();
    }

    public static void defTransfer(String source, String currentDef) {
        if (source.equals("next")) {
            defs.remove(0);
            temp.add(currentDef);
        } else if (source.equals("success")) {
            defs.remove(0);
            teamsNotes[currentPlaying].add(currentDef);
        }
    }

    public static void increaseRoundMode() {
        totalRoundNumber++;
        if (totalRoundNumber == 3) totalRoundNumber = 0;
    }

    public static String getRoundMode() {
        switch (totalRoundNumber) {
            case 0:
                return "הסבר בלי שימוש במילה";
            case 1:
                return "הסבר במילה אחת";
            case 2:
                return "פנטומימה";
        }
        return null;
    }

    public static void increseRoundNum() {
        teamsRoundNum[currentPlaying]++;
    }

    public static void increseScore() {
        scores[currentPlaying]++;
    }

    public static int totalNoteAmount() {
        int ans = defs.size() + temp.size();
        for (int i = 0; i < 24; i++) {
            ans += teamsNotes[i].size();
        }

        return ans;
    }

    public static ArrayList<String> allNotes() {
        ArrayList<String> ans = new ArrayList<String>();
        for (int i = 0; i < 24; i++) {
            ans.addAll(teamsNotes[i]);
        }
        ans.addAll(temp);
        ans.addAll(defs);
        return ans;
    }

    public static boolean noteExists(String note){
        return allNotes().contains(note);
    }

    public static int roundNoteAmount() {
        return defs.size() + temp.size();
    }

    public static void makeSound(Context context, int resid) {
        if (db.soundCheckBox == false) return;
        mPlayer = MediaPlayer.create(context, resid);
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                // TODO Auto-generated method stub
                mp.reset();
                mp.release();
            }

        });
        mPlayer.start();
    }

    public static boolean onTouch(Context context, View view, MotionEvent motion) {
        if (motion.getAction() == MotionEvent.ACTION_UP) view.setPadding(0, 0, 0, 0);
        else if(motion.getAction() == MotionEvent.ACTION_MOVE) {
            Rect rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
            if(!rect.contains(view.getLeft() + (int) motion.getX(), view.getTop() + (int) motion.getY())) {
                view.setPadding(0, 0, 0, 0);
            }
        }
        else {
            view.setPadding(0, 10, 10, 0);
            makeSound(context, R.raw.button_press_sound);
        }
        return false;
    }

    public static boolean onTouchExplanation(Context context, View view, MotionEvent motion) {
        if (motion.getAction() == MotionEvent.ACTION_UP) view.setPadding(17, 0, 0, 0);
        else if(motion.getAction() == MotionEvent.ACTION_MOVE) {
            Rect rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
            if(!rect.contains(view.getLeft() + (int) motion.getX(), view.getTop() + (int) motion.getY())) {
                view.setPadding(17, 0, 0, 0);
            }
        }
        else {
            view.setPadding(17, 20, 20, 0);
            makeSound(context, R.raw.button_press_sound);
        }
        return false;
    }

    public static boolean onTouchEditIcon(Context context, View view, MotionEvent motion) {
        if (motion.getAction() == MotionEvent.ACTION_UP) view.setPadding(18, 0, 0, 18);
        else if(motion.getAction() == MotionEvent.ACTION_MOVE) {
            Rect rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
            if(!rect.contains(view.getLeft() + (int) motion.getX(), view.getTop() + (int) motion.getY())) {
                view.setPadding(18, 0, 0, 18);
            }
        }
        else {
            view.setPadding(18, 20, 20, 18);
            makeSound(context, R.raw.button_press_sound);
        }
        return false;
    }
}
