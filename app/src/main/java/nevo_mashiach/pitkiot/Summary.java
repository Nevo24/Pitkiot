package nevo_mashiach.pitkiot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import nevo_mashiach.pitkiot.NotActivities.DialogBag;
import nevo_mashiach.pitkiot.NotActivities.MyButton;
import nevo_mashiach.pitkiot.NotActivities.db;

public class Summary extends AppCompatActivity {

    public static final String TAG = Summary.class.getName();

    @BindView(R.id.t1Total)
    TextView mT1Total;
    @BindView(R.id.t2Total)
    TextView mT2Total;
    @BindView(R.id.t1Round)
    TextView mT1Round;
    @BindView(R.id.t2Round)
    TextView mT2Round;
    @BindView(R.id.editScoreHeadline)
    TextView mTotalNotes;
    @BindView(R.id.summaryHeadline)
    TextView mSummaryHeadline;
    @BindView(R.id.roundModeSummary)
    TextView mRoundModeSummary;
    @BindView(R.id.ready)
    MyButton mReady;
    @BindView(R.id.t1Plus)
    TextView mT1Plus;
    @BindView(R.id.t2Plus)
    TextView mT2Plus;
    @BindView(R.id.team1Headline)
    TextView mTeam1Headline;
    @BindView(R.id.team2Headline)
    TextView mTeam2Headline;
    @BindView(R.id.pressHereFigure)
    ImageView mPressHereFigure;

    @BindView(R.id.summarySpinner)
    View mSummarySpinner;
    @BindView(R.id.multiTeamsPlus)
    TextView mMultiTeamsPlus;
    @BindView(R.id.multiTeamTotalScore)
    TextView mMultiTeamTotalScore;
    @BindView(R.id.multiTeamRound)
    TextView mMultiTeamRound;

    Context context;
    public DialogBag dialogBag;
    boolean onCreate = false;
    boolean firstTime = true;
    SharedPreferences prefs;
    SharedPreferences.Editor spEditor;
    int selectedSpinner = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ButterKnife.bind(this);
        context = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        spEditor = prefs.edit();
        dialogBag = new DialogBag(getFragmentManager(), this);
        if (firstTime) {
            updateInfoFromSharedPreferences();
            firstTime = false;
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        onCreate = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!onCreate) return; //preventing round++ when minimizing the app
        mTotalNotes.setText("מספר פתקים נותרים לסבב: " + db.roundNoteAmount());
        if (!db.summaryIsPaused) {
            mRoundModeSummary.setText(db.getRoundMode());
            mReady.setText(getIntent().getExtras().getString("readyText"));

            mSummaryHeadline.setText(getIntent().getExtras().getString("summaryHeadline"));
            if (mSummaryHeadline.getText().equals("נגמר הזמן!")) { //If we are here because of time out
                if(db.amountOfTeams == 2){
                    if (db.currentPlaying == 0) mT1Plus.setText("+" + db.currentSuccessNum);
                    else mT2Plus.setText("+" + db.currentSuccessNum);
                }
                else{
                    mMultiTeamsPlus.setText("+" + db.currentSuccessNum);
                }
                db.currentSuccessNum = 0;
                db.increseRoundNum();
            } else { //If the notes are out
                if (db.totalRoundNumber == 0) { //If the game is over
                    mPressHereFigure.setVisibility(View.INVISIBLE);
                    db.increseRoundNum();
                    //db.currentPlaying = (db.currentPlaying + 1)%db.amountOfTeams; -- Maybe consider adding it
                    if (db.autoBalanceCheckBox) autoBalance();  //If auto balance is checked
                    gameOverDialogCall();
                } else {
                    dialogBag.modeChanged();
                    mTotalNotes.setText("מספר פתקים נותרים לסבב: " + db.totalNoteAmount());
                }
            }
            if (db.amountOfTeams == 2) {
                multiTeamsVisibility(false);
                twoTeamsVisibility(true);
                mT1Round.setText("מספר סיבובים: " + db.teamsRoundNum[0]);
                mT2Round.setText("מספר סיבובים: " + db.teamsRoundNum[1]);
            } else {
                multiTeamsVisibility(true);
                twoTeamsVisibility(false);
                createGroupSpinner();
                mMultiTeamRound.setText("מספר סיבובים: " + db.teamsRoundNum[db.currentPlaying]);
            }
        } else {
            db.summaryIsPaused = false;
        }
        if (db.amountOfTeams == 2) {
            multiTeamsVisibility(false);
            twoTeamsVisibility(true);
            mT1Total.setText("ניקוד כולל: " + db.scores[0]);
            mT2Total.setText("ניקוד כולל: " + db.scores[1]);
        } else {
            multiTeamsVisibility(true);
            twoTeamsVisibility(false);
            createGroupSpinner();
            mMultiTeamTotalScore.setText("ניקוד כולל: " + db.scores[db.currentPlaying]);
        }
        onCreate = false;
    }


    private void updateInfoFromSharedPreferences() {
        mSummaryHeadline.setText(prefs.getString("summaryHeadline", ""));
        mReady.setText(prefs.getString("ready", ""));
        mRoundModeSummary.setText(prefs.getString("roundModeSummary", ""));
        if (db.amountOfTeams == 2) {
            mT1Total.setText(prefs.getString("t1Total", ""));
            mT1Round.setText(prefs.getString("t1Round", ""));
            mT2Total.setText(prefs.getString("t2Total", ""));
            mT2Round.setText(prefs.getString("t2Round", ""));
        } else {
            mMultiTeamTotalScore.setText(prefs.getString("t" + db.currentPlaying + "Total", ""));
            mMultiTeamRound.setText(prefs.getString("t" + db.currentPlaying + "Round", ""));
        }
        db.team2AverageAnswersPerSecond = Double.longBitsToDouble(prefs.getLong("team2AverageAnswersPerSecond", Double.doubleToLongBits(0)));
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
                db.team2AverageAnswersPerSecond = (double) db.scores[i] / totalPlayingTime;
                db.scores[i] = (int) Math.round((double) db.scores[i] + (double) db.mMillisUntilFinished / 1000 * db.team2AverageAnswersPerSecond);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

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
        spEditor.putInt("totalRoundNumber", db.totalRoundNumber);
        spEditor.putInt("currentSuccessNum", db.currentSuccessNum);
        spEditor.putInt("currentPlaying", db.currentPlaying);
        spEditor.putLong("mMillisUntilFinished", db.mMillisUntilFinished);
        if(!db.gameOverDialogActivated) spEditor.putBoolean("summaryIsPaused", true);
        spEditor.putBoolean("gamePlayIsPaused", false);
        spEditor.putLong("team2AverageAnswersPerSecond", Double.doubleToRawLongBits(db.team2AverageAnswersPerSecond));

        //Save text-info
        spEditor.putString("summaryHeadline", mSummaryHeadline.getText().toString());
        spEditor.putString("ready", mReady.getText().toString());
        spEditor.putString("roundModeSummary", mRoundModeSummary.getText().toString());
        if (db.amountOfTeams == 2) {
            spEditor.putString("t1Total", mT1Total.getText().toString());
            spEditor.putString("t1Round", mT1Round.getText().toString());
            spEditor.putString("t2Total", mT2Total.getText().toString());
            spEditor.putString("t2Round", mT2Round.getText().toString());
        } else {
            spEditor.putString("multiTeamTotalScore", mMultiTeamTotalScore.getText().toString());
            spEditor.putString("multiTeamRound", mMultiTeamRound.getText().toString());
        }

        spEditor.commit();
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
            items[i] = "קבוצה " + (i + 1);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, items) {

            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                Typeface externalFont = Typeface.createFromAsset(getAssets(), "gan.ttf");
                ((TextView) v).setTypeface(externalFont);
                ((TextView) v).setTextColor(0x88000000);
                ((TextView) v).setTextSize(25);
                return v;
            }

            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);

                Typeface externalFont = Typeface.createFromAsset(getAssets(), "gan.ttf");
                ((TextView) v).setTypeface(externalFont);
                ((TextView) v).setTextColor(0x88000000);
                ((TextView) v).setTextSize(25);
                mMultiTeamsPlus.setText("");
                return v;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinner = (Spinner) findViewById(R.id.summarySpinner);
        spinner.setAdapter(adapter);
        spinner.setSelection(db.currentPlaying);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                selectedSpinner = position;
                mMultiTeamTotalScore.setText("ניקוד כולל: " + db.scores[selectedSpinner]);
                mMultiTeamRound.setText("מספר סיבובים: " + db.teamsRoundNum[selectedSpinner]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // my code here
            }

        });
    }


    //*********************** ON CLICKS ********************************
    @OnClick(R.id.ready)
    public void backToGamePlay(View view) {
        if (mSummaryHeadline.getText().equals("נגמר הזמן!")) {
            db.mMillisUntilFinished = db.timePerRound * 1000;
            db.currentPlaying = (db.currentPlaying + 1)%db.amountOfTeams;
        } else db.resetRound();
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

    @OnTouch({R.id.ready})
    boolean onTouch(View view, MotionEvent motion) {
        return db.onTouch(context, view, motion);
    }
}