package nevo_mashiach.pitkiot;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import nevo_mashiach.pitkiot.NotActivities.DialogBag;
import nevo_mashiach.pitkiot.NotActivities.Sms;
import nevo_mashiach.pitkiot.NotActivities.db;

import static nevo_mashiach.pitkiot.NotActivities.db.defs;

public class NoteManagement extends AppCompatActivity {

    @BindView(R.id.typeDef)
    EditText mTypeDef;
    @BindView(R.id.lastScanDate)
    TextView mLastScanDate;
    @BindView(R.id.lastScanTime)
    TextView mLastScanTime;
    @BindView(R.id.noteCount)
    TextView mNoteCount;
    @BindView(R.id.smsExplanation)
    TextView mSmsExplanation;
    @BindView(R.id.editDateIcon)
    ImageButton mEditDateIcon;
    @BindView(R.id.editTimeIcon)
    ImageButton mEditTimeIcon;

    Context context;
    AppCompatActivity thisActivity;
    DialogBag dialogBag;
    int currentColonIndex, scannedAmount;
    List<Integer> colonsIndex;
    Cursor c;

    SharedPreferences prefs;
    SharedPreferences.Editor spEditor;
    SimpleDateFormat fullDateAndTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_management);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ButterKnife.bind(this);
        context = this;
        thisActivity = this;
        dialogBag = new DialogBag(getFragmentManager(), this);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        spEditor = prefs.edit();

        mTypeDef.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addingDefToDb(null);

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

    protected void onResume() {
        super.onResume();
        updatePresentedDateAndTime();
        mNoteCount.setText("מספר הפתקים במאגר: " + db.totalNoteAmount());
        mSmsExplanation.setPadding(17, 0, 0, 0);
        mEditDateIcon.setPadding(18, 0, 0, 18);
        mEditTimeIcon.setPadding(18, 0, 0, 18);
    }


    public void onDestroy() {
        super.onDestroy();
        if (c != null) {
            c.close();
        }
    }

    private void updatePresentedDateAndTime() {
        Date currentDate = new Date(db.smsTime);
        String dateString = new SimpleDateFormat("dd/MM/yyyy").format(currentDate);
        String timeString = new SimpleDateFormat("HH:mm").format(currentDate);
        mLastScanDate.setText(dateString);
        mLastScanTime.setText(timeString);
    }


    public List<Sms> getAllSms() {
        List<Sms> lstSms = new ArrayList<>();
        Sms objSms;
        Uri message = Uri.parse("content://sms/");
        ContentResolver cr = this.getContentResolver();

        c = cr.query(message, null, null, null, null);
        int totalSMS = c.getCount();

        if (c.moveToFirst()) {
            for (int i = 0; i < totalSMS; i++) {
                if (smsValid(c.getString(c.getColumnIndexOrThrow("body")),
                        Long.parseLong(c.getString(c.getColumnIndexOrThrow("date"))))) {
                    scannedAmount++;
                    objSms = new Sms();
                    objSms.setId(c.getString(c.getColumnIndexOrThrow("_id")));
                    objSms.setAddress(c.getString(c
                            .getColumnIndexOrThrow("address")));
                    objSms.setMsg(c.getString(c.getColumnIndexOrThrow("body")));
                    objSms.setReadState(c.getString(c.getColumnIndex("read")));
                    objSms.setTime(c.getString(c.getColumnIndexOrThrow("date")));
                    if (c.getString(c.getColumnIndexOrThrow("type")).contains("1")) {
                        objSms.setFolderName("inbox");
                    } else {
                        objSms.setFolderName("sent");
                    }
                    lstSms.add(objSms);
                }
                c.moveToNext();
            }
        }
        // else {
        // throw new RuntimeException("You have no SMS");
        // }
        return lstSms;
    }

    private boolean smsValid(String str, long receiveTime) {
        if (receiveTime < db.smsTime) return false;
        try {
            if (!str.substring(0, 8).equals("פיתקיות:") && !str.substring(0, 7).equals("פתקיות:"))
                return false;
        } catch (StringIndexOutOfBoundsException e) {
            return false;
        }
        currentColonIndex = str.indexOf(':');
        colonsIndex.add(currentColonIndex);
        return true;
    }

    private void add(String name) {
        name = name.trim();
        if (!db.noteExists(name) && !name.equals("")) {
            defs.add(name);
            saveNotes();
            mNoteCount.setText("מספר הפתקים במאגר: " + db.totalNoteAmount());
        }
    }

    private void saveNotes() {
        //save notes to shared preferences
        Set<String> set = new HashSet<String>();
        set.addAll(db.defs);
        spEditor.putStringSet("defs", set);
        spEditor.commit();
    }


    public void updateSharedPreferencesDate(String currentDateString){
        try {
            Date d = fullDateAndTimeFormat.parse(currentDateString);
            db.smsTime = d.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        spEditor.putLong("smsTime", db.smsTime );
        spEditor.commit();
    }


    //*********************** ON CLICKS ********************************
    @OnClick(R.id.addDef)
    public void addingDefToDb(View view) {
        add(mTypeDef.getText().toString());
        mTypeDef.setText("");
    }

    @OnClick(R.id.addDefsFromSms)
    public void addDefsFromSms(View view) {
        scannedAmount = 0;
        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
            colonsIndex = new ArrayList<>();
            List<Sms> smsList = getAllSms();
            List<String> defsFromSms;
            for (int i = 0; i < smsList.size(); i++) {
                defsFromSms = Arrays.asList(smsList.get(i).getMsg().substring(colonsIndex.get(i) + 1).replace("\n", ",").split(","));
                for (int j = 0; j < defsFromSms.size(); j++) {
                    add(defsFromSms.get(j).toString());
                }
            }
            dialogBag.smsScaned(scannedAmount);
        } else {
            Runnable task = new Runnable() {
                public void run() {
                    final int REQUEST_CODE_ASK_PERMISSIONS = 123;
                    ActivityCompat.requestPermissions(thisActivity, new String[]{"android.permission.READ_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);
                    dialogBag.clickAgainPlease();
                }
            };
            dialogBag.allowingAccessToSms(task);
        }
    }

    @OnClick(R.id.smsExplanation)
    public void smsExplanation(View view) {
        dialogBag.smsExplanation();
    }

    @OnClick({R.id.editDateIcon, R.id.lastScanDate})
    public void datePicker(View view) {

        final Calendar myCalendar = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                // TODO Auto-generated method stub
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                String myFormat = "dd/MM/yyyy"; // your format
                SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());

                String selectedDate = sdf.format(myCalendar.getTime());
                mLastScanDate.setText(selectedDate);
                updateSharedPreferencesDate(selectedDate + " " + mLastScanTime.getText());
            }
        };
        new DatePickerDialog(context, date, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();

    }

    @OnClick({R.id.editTimeIcon, R.id.lastScanTime})
    public void timePicker(View view) {

        Calendar mcurrentTime = Calendar.getInstance();
        int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        int minute = mcurrentTime.get(Calendar.MINUTE);
        TimePickerDialog mTimePicker;
        mTimePicker = new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                String selectedTime = selectedHour + ":" + selectedMinute;
                mLastScanTime.setText(selectedTime);
                updateSharedPreferencesDate(mLastScanDate.getText() + " " + selectedTime);
            }
        }, hour, minute, true);//Yes 24 hour time
        mTimePicker.setTitle("בחר שעה");
        mTimePicker.show();
    }

    @OnClick(R.id.deleteNotes)
    public void deleteNodesButton(View view) {
        Intent intent = new Intent(context, NoteList.class);
        startActivity(intent);
    }

    @OnClick(R.id.updateToCurrentTime)
    public void updateToCurrentDateAndTime(View view) {
        db.smsTime = System.currentTimeMillis();
        spEditor.putLong("smsTime", db.smsTime );
        spEditor.commit();
        updatePresentedDateAndTime();
    }

    @OnTouch({R.id.addDefsFromSms, R.id.addDef, R.id.deleteNotes, R.id.updateToCurrentTime})
    boolean onTouch(View view, MotionEvent motion) {
        return db.onTouch(context, view, motion);
    }

    @OnTouch(R.id.smsExplanation)
    boolean onTouchExplanation(View view, MotionEvent motion) {return db.onTouchExplanation(context, view, motion);}

    @OnTouch({R.id.editDateIcon, R.id.editTimeIcon})
    boolean onTouchEditIcon(View view, MotionEvent motion) {return db.onTouchEditIcon(context, view, motion);}
}