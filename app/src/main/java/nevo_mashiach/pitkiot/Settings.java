package nevo_mashiach.pitkiot;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MotionEvent;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import butterknife.OnTouch;
import nevo_mashiach.pitkiot.NotActivities.DialogBag;
import nevo_mashiach.pitkiot.NotActivities.MyEditText;
import nevo_mashiach.pitkiot.NotActivities.db;

public class Settings extends AppCompatActivity {

    public DialogBag dialogBag;
    Context context;

    @BindView(R.id.autoBalaceCheckBox)
    CheckBox mAutoBalaceCheckBox;
    @BindView(R.id.soundCheckBox)
    CheckBox mSoundCheckBox;
    @BindView(R.id.editRoundTime)
    MyEditText mEditRoundTime;
    @BindView(R.id.editNextTime)
    MyEditText mEditNextTime;
    @BindView(R.id.amoutOfTeams)
    TextView mAmoutOfTeams;
    @BindView(R.id.teamEditableScore)
    TextView mTeamEditableScore;
    @BindView(R.id.balanceExplanation)
    TextView mBalanceExplanation;

    @BindView(R.id.increase1)
    Button mIncrease1;
    @BindView(R.id.decrease1)
    Button mDecrease1;
    @BindView(R.id.decrease2)
    Button mDecrease2;

    int num;
    int selectedSpinner = 1;
    SharedPreferences prefs;
    SharedPreferences.Editor spEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        context = this;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        dialogBag = new DialogBag(getFragmentManager(), this);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        spEditor = prefs.edit();

        Typeface tf = Typeface.createFromAsset(getAssets(), "gan.ttf");
        mAutoBalaceCheckBox.setTypeface(tf);
        mSoundCheckBox.setTypeface(tf);


        mEditNextTime.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mEditRoundTime.clearFocus();
                    mEditNextTime.clearFocus();

                    //Closing keyboard:
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                return false;
            }
        });
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mAutoBalaceCheckBox.setChecked(db.autoBalanceCheckBox);
        mSoundCheckBox.setChecked(db.soundCheckBox);
        mEditRoundTime.setText("" + db.timePerRound);
        mEditNextTime.setText("" + db.timeDownOnNext);
        mAmoutOfTeams.setText("" + db.amountOfTeams);
        mTeamEditableScore.setText("" + db.scores[0]);
        mBalanceExplanation.setPadding(17, 0, 0, 0);
        createGroupSpinner();
        if(db.amountOfTeams == 2) mDecrease1.setEnabled(false);
        else if(db.amountOfTeams == 24) mIncrease1.setEnabled(false);
        if(db.scores[selectedSpinner] == 0) mDecrease2.setEnabled(false);
    }

    private void createGroupSpinner() {

        String [] items = new String[db.amountOfTeams];
        for(int i = 0; i < db.amountOfTeams; i++){
            items[i]= "קבוצה " + (i + 1);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, items) {

            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);

                Typeface externalFont=Typeface.createFromAsset(getAssets(), "gan.ttf");
                ((TextView) v).setTypeface(externalFont);
                ((TextView) v).setTextColor(0x88000000);
                ((TextView) v).setTextSize(18);
                return v;
            }

            public View getDropDownView(int position,  View convertView,  ViewGroup parent) {
                View v =super.getDropDownView(position, convertView, parent);

                Typeface externalFont=Typeface.createFromAsset(getAssets(), "gan.ttf");
                ((TextView) v).setTypeface(externalFont);
                ((TextView) v).setTextColor(0x88000000);
                ((TextView) v).setTextSize(18);
                return v;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinner = (Spinner) findViewById(R.id.settingsSpinner);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                selectedSpinner = position;
                mTeamEditableScore.setText("" + db.scores[selectedSpinner]);
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
    @OnCheckedChanged(R.id.autoBalaceCheckBox)
    void onAutoBalanceChecked(boolean checked) {
        //save autoBalanceCheckBox to shared preferences
        db.autoBalanceCheckBox = checked;
        spEditor.putBoolean("autoBalanceCheckBoxBackup", checked);
        spEditor.commit();
    }

    @OnCheckedChanged(R.id.soundCheckBox)
    void onSoundChecked(boolean checked) {
        db.soundCheckBox = checked;
        //save autoBalanceCheckBox to shared preferences
        db.autoBalanceCheckBox = checked;
        spEditor.putBoolean("soundCheckBoxBackup", checked);
        spEditor.commit();
    }

    @OnClick(R.id.balanceExplanation)
    public void explanationBalanceDialog(View view) {
        dialogBag.unevenExplanation();
    }


    @OnFocusChange(R.id.editRoundTime)
    void focusChangedEditRoundTime(boolean hasFocus) {
        if (hasFocus == false) {
            if (mEditRoundTime.getText().toString().equals("")) {
                mEditRoundTime.setText("" + db.timePerRound);
                return;
            }
            try {
                num = Integer.parseInt(mEditRoundTime.getText().toString());
            } catch (NumberFormatException e) {
                dialogBag.invalidInput();
                mEditRoundTime.setText("" + db.timePerRound);
                return;
            }
            if (num > 300 || num < 1) {
                dialogBag.invalidInput();
                mEditRoundTime.setText("" + db.timePerRound);
                return;
            }
            db.timePerRound = num;
            mEditRoundTime.setText("" + db.timePerRound);
            //save roundTime to shared preferences
            spEditor.putInt("timePerRoundBackup", num);
            spEditor.commit();
        } else mEditRoundTime.setText("");
    }

    @OnFocusChange(R.id.editNextTime)
    void focusChangedEditNextTime(boolean hasFocus) {
        if (hasFocus == false) {
            if (mEditNextTime.getText().toString().equals("")) {
                mEditNextTime.setText("" + db.timeDownOnNext);
                return;
            }
            try {
                num = Integer.parseInt(mEditNextTime.getText().toString());
            } catch (NumberFormatException e) {
                dialogBag.invalidInput();
                mEditNextTime.setText("" + db.timeDownOnNext);
                return;
            }
            if (num > 300 || num < 1) {
                dialogBag.invalidInput();
                mEditNextTime.setText("" + db.timeDownOnNext);
                return;
            }
            db.timeDownOnNext = num;
            mEditNextTime.setText("" + db.timeDownOnNext);
            //save nextTime to shared preferences
            spEditor.putInt("timeDownOnNextBackup", num);
            spEditor.commit();
        } else mEditNextTime.setText("");
    }

    @OnClick(R.id.resetSettings)
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
                spEditor.commit();

                mAutoBalaceCheckBox.setChecked(true);
                mSoundCheckBox.setChecked(true);
                mEditRoundTime.setText("60");
                mEditNextTime.setText("5");
            }
        };
        dialogBag.resetSettings(task);
    }

    @OnClick(R.id.increase1)
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
        spEditor.commit();
        createGroupSpinner();
        mAmoutOfTeams.setText("" + db.amountOfTeams);
    }

    @OnClick(R.id.increase2)
    public void increaseTeam2Score(View view) {
        if (!db.gamePlayIsPaused && !db.summaryIsPaused) {
            dialogBag.cannotEditScore();
            return;
        }
        db.scores[selectedSpinner]++;
        mDecrease2.setEnabled(true);
        spEditor.putInt("team" + selectedSpinner + "Score", db.scores[selectedSpinner]);
        spEditor.commit();
        mTeamEditableScore.setText("" + db.scores[selectedSpinner]);
    }

    @OnClick(R.id.decrease1)
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
        spEditor.commit();
        createGroupSpinner();
        mAmoutOfTeams.setText("" + db.amountOfTeams);
    }

    @OnClick(R.id.decrease2)
    public void decreaseTeam2Score(View view) {
        if (db.scores[selectedSpinner] == 0) return;
        if (!db.gamePlayIsPaused && !db.summaryIsPaused) {
            dialogBag.cannotEditScore();
            return;
        }
        db.scores[selectedSpinner]--;
        if (db.scores[selectedSpinner] == 0) mDecrease2.setEnabled(false);
        spEditor.putInt("team" + selectedSpinner + "score", db.scores[selectedSpinner]);
        spEditor.commit();
        mTeamEditableScore.setText("" + db.scores[selectedSpinner]);
    }

    @OnTouch({R.id.resetSettings})
    boolean onTouch(View view, MotionEvent motion) {
        return db.onTouch(context, view, motion);
    }

    @OnTouch({R.id.balanceExplanation})
    boolean onTouchExplanation(View view, MotionEvent motion) {return db.onTouchExplanation(context, view, motion);}
}