package nevo_mashiach.pitkiot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import androidx.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import nevo_mashiach.pitkiot.NotActivities.DialogBag;
import nevo_mashiach.pitkiot.NotActivities.db;

public class NoteList extends AppCompatActivity {

    ArrayList<String> notes = new ArrayList<>();
    Context context;

    SharedPreferences prefs;
    SharedPreferences.Editor spEditor;

    DialogBag dialogBag;
    ListView listView;

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
        setContentView(R.layout.sample_main);

        // Make navigation bar transparent
        setupTransparentNavigationBar();

        context = this;
        dialogBag = new DialogBag(getSupportFragmentManager(), this);

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        spEditor = prefs.edit();

        listView = findViewById(android.R.id.list);
        listView.setAdapter(mListAdapter);

        findViewById(R.id.deleteAll).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Runnable task = new Runnable() {
                    public void run() {
                        deleteAllNotes();
                        reloadActivityWithoutAnimation();
                    }
                };
                dialogBag.confirmDeleteAll(task);
            }
        });

        notes = db.allNotes();
        Collections.sort(notes);
    }


    private final BaseAdapter mListAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return notes.size();
        }

        @Override
        public Object getItem(int position) {
            return notes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position + 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_item, container, false);
            }
            TextView note = (TextView) convertView.findViewById(R.id.note);
            final String noteText = notes.get(position);
            note.setText(noteText);

            // Because the list item contains multiple touch targets, you should not override
            // onListItemClick. Instead, set a click listener for each target individually.

            convertView.findViewById(R.id.primary_target).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                        }
                    });

            convertView.findViewById(R.id.secondary_action).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Runnable task = new Runnable() {
                                public void run() {
                                    deleteNote(noteText);
                                    reloadActivityWithoutAnimation();
                                }
                            };
                            dialogBag.confirmCharacterDelete(task);
                        }
                    });
            return convertView;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return true;
    }


    public void deleteNote(String note) {
        Set<String> set = new HashSet<String>();
        if(db.defs.contains(note)){
            db.defs.remove(note);
            set.addAll(db.defs);
            spEditor.putStringSet("defs", set);
        }
        else if(db.temp.contains(note)){
            db.temp.remove(note);
            set.addAll(db.temp);
            spEditor.putStringSet("temp", set);
        }
        else{
            for (int i = 0; i < 24; i++) {
                db.teamsNotes[i].remove(note);
                set.clear();
                set.addAll(db.teamsNotes[i]);
                spEditor.putStringSet("team" + i + "Notes", set);
            }
        }

        spEditor.commit();
    }

    public void deleteAllNotes() {
        db.defs.clear();
        db.temp.clear();
        for (int i = 0; i < 24; i++) {
            db.teamsNotes[i].clear();
        }
        Set<String> set = new HashSet<String>();
        spEditor.putStringSet("defs", set);
        spEditor.putStringSet("temp", set);
        for (int i = 0; i < 24; i++) {
            spEditor.putStringSet("team" + i + "Notes", set);
        }
        spEditor.commit();
    }

    public void reloadActivityWithoutAnimation(){
        // Update the notes list and refresh the adapter
        notes.clear();
        notes.addAll(db.allNotes());
        Collections.sort(notes);
        mListAdapter.notifyDataSetChanged();
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
