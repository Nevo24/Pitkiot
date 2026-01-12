package nevo_mashiach.pitkiot.NotActivities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import nevo_mashiach.pitkiot.MainActivity;


/**
 * Created by Nevo Mashiach on 9/14/2016.
 */
public class DialogBag {

    FragmentManager fragmentManager;
    Context context;

    public DialogBag (FragmentManager _fragmentManager, Context _context){

        fragmentManager = _fragmentManager;
        context = _context;
    }

    public void modeChanged() {
        db.resetRound();
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_mode_changed_title),
                String.format(context.getString(nevo_mashiach.pitkiot.R.string.dialog_mode_changed_msg), db.getRoundMode(context))
        );
        dialog = dialog.setNaturalButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_understood), null);
        dialog.show(fragmentManager, "ModeChanged");
    }

    public void dbEmptyBeforeGame() {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_no_notes_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_no_notes_msg)
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_go_to_notes_management), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(context, nevo_mashiach.pitkiot.NoteManagement.class);
                context.startActivity(intent);
            }
        });
        dialog = dialog.setNegativeButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_stay_here), null);
        dialog.show(fragmentManager, "OutOfNotes");
    }

    public void normalGameOver(int winningTeam, int winningTotal, int loserTotal) {
        db.gameOverDialogActivated = true;
        db.resetGame();
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_game_over_title),
                String.format(context.getString(nevo_mashiach.pitkiot.R.string.dialog_team_won), winningTeam, winningTotal, loserTotal)
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_return_main), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
                db.gameOverDialogActivated = false;
            }
        });
        dialog.show(fragmentManager, "NormalGameOver"); //The second one is just a string tag that we can use to refer to it.
    }

    public void drawGameOver(int score) {
        db.gameOverDialogActivated = true;
        db.resetGame();
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_game_over_title),
                String.format(context.getString(nevo_mashiach.pitkiot.R.string.dialog_draw), score, score)
        );
        dialog = dialog.setNaturalButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_return_main), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
                db.gameOverDialogActivated = false;
            }
        });
        dialog.show(fragmentManager, "DrawGameOver"); //The second one is just a string tag that we can use to refer to it.
    }

    public void multiGameOver(final int[] scores) {
        db.gameOverDialogActivated = true;
        db.resetGame();
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_game_over_title),
                ""
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_view_final_score), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                multiGameOverFinalScore(scores);
            }
        });
        dialog.show(fragmentManager, "NormalGameOver"); //The second one is just a string tag that we can use to refer to it.
    }

    private void multiGameOverFinalScore(int[] scores) {
        StringBuilder scoreSummary = new StringBuilder();
        TreeMap<Integer, ArrayList<String>> treeMap = new TreeMap<>(Collections.reverseOrder());
        for (int i = 0; i < db.amountOfTeams; i++) {
            if(!treeMap.containsKey(scores[i])){
                treeMap.put(scores[i], new ArrayList<String>());
            }
            treeMap.get(scores[i]).add(String.format(context.getString(nevo_mashiach.pitkiot.R.string.dialog_team_score), (i + 1), scores[i]));
        }
        for (Map.Entry<Integer, ArrayList<String>> entry : treeMap.entrySet()) {
            for (String teamScore : entry.getValue()) {
                scoreSummary.append(teamScore);
            }
        }
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_final_score_title),
                scoreSummary.toString()
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_return_main), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
                db.gameOverDialogActivated = false;
            }
        });
        dialog.show(fragmentManager, "NormalGameOver"); //The second one is just a string tag that we can use to refer to it.
    }

    public void unevenExplanation() {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_uneven_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_uneven_msg)
        );
        dialog = dialog.setNaturalButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_understood), null);
        dialog.show(fragmentManager, "UnevenExplanation"); //The second one is just a string tag that we can use to refer to it.
    }

    public void invalidInput() {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_invalid_input_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_invalid_input_msg)
        );
        dialog = dialog.setNaturalButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_ok), null);
        dialog.show(fragmentManager, "InvalidInput"); //The second one is just a string tag that we can use to refer to it.
    }

    public void backToMainMenu(final String currentActivity) {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_back_main_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_back_main_msg)
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_continue_play), null);
        dialog = dialog.setNegativeButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_return_main), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(currentActivity.equals("nevo_mashiach.pitkiot.GamePlay")) db.gamePlayIsPaused = true;
                else if(currentActivity.equals("nevo_mashiach.pitkiot.Summary")) db.summaryIsPaused = true;
                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
            }
        });
        dialog.show(fragmentManager, "BackToMainMenu"); //The second one is just a string tag that we can use to refer to it.
    }

    public void exitTheGame() {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_exit_game_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_exit_game_msg)
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_continue_play), null);
        dialog = dialog.setNegativeButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_exit), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MainActivity.appExit();
            }
        });
        dialog.show(fragmentManager, "ExitTheGame"); //The second one is just a string tag that we can use to refer to it.
    }

    public void confirmCharacterDelete(final Runnable task) {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_delete_char_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_delete_char_msg)
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_no), null);
        dialog = dialog.setNegativeButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                task.run();
            }
        });
        dialog.show(fragmentManager, "ConfirmCharacterRemove"); //The second one is just a string tag that we can use to refer to it.
    }

    public void confirmDeleteAll(final Runnable deleteTask, final Runnable resetGameTask) {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_delete_all_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_delete_all_msg)
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_no), null);
        dialog = dialog.setNegativeButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteTask.run();
                askResetGameAfterDelete(resetGameTask);
            }
        });
        dialog.show(fragmentManager, "ConfirmDeleteAll");
    }

    public void askResetGameAfterDelete(final Runnable resetGameTask) {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_notes_deleted_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_notes_deleted_msg)
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_reset_game_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resetGameTask.run();
            }
        });
        dialog = dialog.setNegativeButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_keep_game), null);
        dialog.show(fragmentManager, "AskResetGameAfterDelete");
    }


    public void resetGame(final Runnable resetTask, final Runnable deleteNotesTask) {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_reset_game_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_reset_game_msg)
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_no), null);
        dialog = dialog.setNegativeButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
               resetTask.run();
               askDeleteNotesAfterReset(deleteNotesTask);
            }
        });
        dialog.show(fragmentManager, "ResetGame"); //The second one is just a string tag that we can use to refer to it.
    }

    public void askDeleteNotesAfterReset(final Runnable deleteNotesTask) {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_game_reset_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_game_reset_msg)
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_delete_notes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteNotesTask.run();
            }
        });
        dialog = dialog.setNegativeButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_keep_notes), null);
        dialog.show(fragmentManager, "AskDeleteNotesAfterReset");
    }

    public void resetSettings(final Runnable task) {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_reset_settings_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_reset_settings_msg)
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_no), null);
        dialog = dialog.setNegativeButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                task.run();
            }
        });
        dialog.show(fragmentManager, "ResetGame"); //The second one is just a string tag that we can use to refer to it.
    }

    public void resumeGame(final Runnable task) {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_resume_game_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_resume_game_msg)
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_resume_game_btn), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                task.run();
            }
        });
        dialog = dialog.setNegativeButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_stay_main), null);
        dialog.show(fragmentManager, "FirstNoteLoad"); //The second one is just a string tag that we can use to refer to it.
    }


    public void cannotEditScore() {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_cannot_edit_score_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_cannot_edit_score_msg)
        );
        dialog = dialog.setNaturalButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_understood), null);
        dialog.show(fragmentManager, "ClickAgainPlease"); //The second one is just a string tag that we can use to refer to it.
    }

    public void cannotEditTeamsAmount() {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_cannot_edit_teams_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_cannot_edit_teams_msg)
        );
        dialog = dialog.setNaturalButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_understood), null);
        dialog.show(fragmentManager, "ClickAgainPlease"); //The second one is just a string tag that we can use to refer to it.
    }

    public void confirmCloseCollection(final Runnable closeTask) {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_close_collection_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_close_collection_msg)
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_stay_collecting), null);
        dialog = dialog.setNegativeButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_close_without_saving), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                closeTask.run();
            }
        });
        dialog.show(fragmentManager, "ConfirmCloseCollection");
    }
}
