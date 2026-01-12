package nevo_mashiach.pitkiot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        // Check if using gesture navigation (no buttons)
        if (isGestureNavigationEnabled()) {
            // For gesture navigation: extend content behind the navigation bar
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        // For button navigation: content stops above buttons (no flags needed)
    }

    private boolean isGestureNavigationEnabled() {
        // Check if gesture navigation is enabled by looking at navigation bar height
        // In gesture mode, the navigation bar is much smaller (typically around 16-24dp)
        // In button mode, it's larger (typically 48dp+)
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            int navBarHeight = getResources().getDimensionPixelSize(resourceId);
            float density = getResources().getDisplayMetrics().density;
            int navBarHeightDp = (int) (navBarHeight / density);
            // If navigation bar is less than 30dp, it's likely gesture navigation
            return navBarHeightDp < 30;
        }
        return false;
    }

}
