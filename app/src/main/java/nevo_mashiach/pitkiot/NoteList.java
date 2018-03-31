package nevo_mashiach.pitkiot;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import nevo_mashiach.pitkiot.NotActivities.DialogBag;
import nevo_mashiach.pitkiot.NotActivities.db;

public class NoteList extends ListActivity {

    ArrayList<String> notes = new ArrayList<>();
    Context context;

    SharedPreferences prefs;
    SharedPreferences.Editor spEditor;

    DialogBag dialogBag;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_main);
        context = this;
        dialogBag = new DialogBag(getFragmentManager(), this);

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        spEditor = prefs.edit();

        setListAdapter(mListAdapter);

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

    private BaseAdapter mListAdapter = new BaseAdapter() {
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
    public boolean onOptionsItemSelected(MenuItem item) {
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
                set.addAll(db.teamsNotes[i]);
                spEditor.putStringSet("team" + i + "Notes", set);
            }
        }

        spEditor.commit();
    }

    public void deleteAllNotes() {
        db.defs.clear();
        db.teamsNotes[0].clear();
        db.teamsNotes[1].clear();
        db.temp.clear();
        Set<String> set = new HashSet<String>();
        spEditor.putStringSet("defs", set);
        for (int i = 0; i < 24; i++) {
            spEditor.putStringSet("team" + i + "Notes", set);
        }
        spEditor.putStringSet("temp", set);
        spEditor.commit();
    }

    public void reloadActivityWithoutAnimation(){
        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
    }

}
