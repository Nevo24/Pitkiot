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
        dialog = dialog.setNaturalButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_return_main), null);
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
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_return_main), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(currentActivity.equals("nevo_mashiach.pitkiot.GamePlay")) db.gamePlayIsPaused = true;
                else if(currentActivity.equals("nevo_mashiach.pitkiot.Summary")) db.summaryIsPaused = true;
                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
            }
        });
        dialog = dialog.setNegativeButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_continue_play), null);
        dialog.show(fragmentManager, "BackToMainMenu"); //The second one is just a string tag that we can use to refer to it.
    }

    public void exitTheGame() {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_exit_game_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_exit_game_msg)
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_exit), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MainActivity.appExit();
            }
        });
        dialog = dialog.setNegativeButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_continue_play), null);
        dialog.show(fragmentManager, "ExitTheGame"); //The second one is just a string tag that we can use to refer to it.
    }

    public void confirmCharacterDelete(final Runnable task) {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_delete_char_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_delete_char_msg)
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                task.run();
            }
        });
        dialog = dialog.setNegativeButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_no), null);
        dialog.show(fragmentManager, "ConfirmCharacterRemove"); //The second one is just a string tag that we can use to refer to it.
    }

    public void confirmDeleteAll(final Runnable task) {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_delete_all_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_delete_all_msg)
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                task.run();
            }
        });
        dialog = dialog.setNegativeButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_no), null);
        dialog.show(fragmentManager, "ConfirmCharacterRemove"); //The second one is just a string tag that we can use to refer to it.
    }

    public void smsExplanation() {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_sms_explanation_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_sms_explanation_msg)
        );
        dialog = dialog.setNaturalButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_button_understood_alt), null);
        dialog.show(fragmentManager, "SmsExplanation"); //The second one is just a string tag that we can use to refer to it.
    }

    public void smsScaned(int amount) {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_sms_scanned_title),
                String.format(context.getString(nevo_mashiach.pitkiot.R.string.dialog_sms_scanned_msg), amount)
        );
        dialog = dialog.setNaturalButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_button_okay), null);
        dialog.show(fragmentManager, "SmsScaned"); //The second one is just a string tag that we can use to refer to it.
    }

    public void resetGame(final Runnable task) {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_reset_game_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_reset_game_msg)
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
               task.run();
            }
        });
        dialog = dialog.setNegativeButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_no), null);
        dialog.show(fragmentManager, "ResetGame"); //The second one is just a string tag that we can use to refer to it.
    }

    public void resetSettings(final Runnable task) {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_reset_settings_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_reset_settings_msg)
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                task.run();
            }
        });
        dialog = dialog.setNegativeButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_no), null);
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

    public void clickAgainPlease() {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_click_again_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_click_again_msg)
        );
        dialog = dialog.setNaturalButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_button_understood_alt), null);
        dialog.show(fragmentManager, "ClickAgainPlease"); //The second one is just a string tag that we can use to refer to it.
    }

    public void allowingAccessToSms(final Runnable task) {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_sms_access_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_sms_access_msg)
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_button_understood_alt), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                task.run();
            }
        });
        dialog.show(fragmentManager, "AllowingAccessToSms"); //The second one is just a string tag that we can use to refer to it.
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
}
