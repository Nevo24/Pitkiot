package nevo_mashiach.pitkiot.NotActivities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
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

    public void normalGameOver(int winningTeam, int winningTotal, int loserTotal, boolean autoBalanceApplied) {
        db.gameOverDialogActivated = true;

        // CRITICAL FIX: Save game over state so dialog can be reshown after app closes
        // Don't reset the game yet - wait until user clicks "Return to Main"
        db.shouldShowGameOverDialog = true;
        db.gameOverDialogType = "normal";
        db.gameOverWinningTeam = winningTeam;
        db.gameOverWinningScore = winningTotal;
        db.gameOverLosingScore = loserTotal;
        db.gameOverAutoBalanceApplied = autoBalanceApplied;
        // Save current scores before they're reset
        for (int i = 0; i < 2; i++) {
            db.gameOverAllScores[i] = db.scores[i];
        }

        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_game_over_title),
                ""
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_view_final_score), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                normalGameOverFinalScore(winningTeam, db.gameOverAllScores, autoBalanceApplied);
            }
        });
        dialog.show(fragmentManager, "NormalGameOver");
    }

    private void normalGameOverFinalScore(int winningTeam, int[] scores, boolean autoBalanceApplied) {
        // Build title: "Team X wins!"
        String title = String.format(context.getString(nevo_mashiach.pitkiot.R.string.dialog_team_wins), winningTeam);

        // Build scores list (sorted by score, highest first)
        String scoresText = buildScoresList(scores);

        // Build clickable message
        CharSequence message = buildScoreMessageWithClickableHelp(scoresText, autoBalanceApplied);

        MyDialogFragment dialog = new MyDialogFragment(
                title,
                message
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_return_main), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // CRITICAL FIX: Reset game only when user dismisses the final dialog
                db.resetGame();
                Toast.makeText(context, context.getString(nevo_mashiach.pitkiot.R.string.toast_game_reset), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
                db.gameOverDialogActivated = false;
            }
        });
        dialog.show(fragmentManager, "NormalGameOverFinalScore");
    }

    public void drawGameOver(int score, boolean autoBalanceApplied) {
        db.gameOverDialogActivated = true;

        // CRITICAL FIX: Save game over state so dialog can be reshown after app closes
        // Don't reset the game yet - wait until user clicks "Return to Main"
        db.shouldShowGameOverDialog = true;
        db.gameOverDialogType = "draw";
        db.gameOverWinningScore = score; // Both teams have this score
        db.gameOverAutoBalanceApplied = autoBalanceApplied;
        // Save current scores before they're reset
        for (int i = 0; i < 2; i++) {
            db.gameOverAllScores[i] = db.scores[i];
        }

        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_game_over_title),
                ""
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_view_final_score), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                drawGameOverFinalScore(db.gameOverAllScores, autoBalanceApplied);
            }
        });
        dialog.show(fragmentManager, "DrawGameOver");
    }

    private void drawGameOverFinalScore(int[] scores, boolean autoBalanceApplied) {
        // Build title: "Draw!"
        String title = context.getString(nevo_mashiach.pitkiot.R.string.dialog_draw_title);

        // Build scores list (sorted by score, highest first)
        String scoresText = buildScoresList(scores);

        // Build clickable message
        CharSequence message = buildScoreMessageWithClickableHelp(scoresText, autoBalanceApplied);

        MyDialogFragment dialog = new MyDialogFragment(
                title,
                message
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_return_main), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // CRITICAL FIX: Reset game only when user dismisses the final dialog
                db.resetGame();
                Toast.makeText(context, context.getString(nevo_mashiach.pitkiot.R.string.toast_game_reset), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
                db.gameOverDialogActivated = false;
            }
        });
        dialog.show(fragmentManager, "DrawGameOverFinalScore");
    }

    public void multiGameOver(final int[] scores, final boolean autoBalanceApplied) {
        db.gameOverDialogActivated = true;

        // CRITICAL FIX: Save game over state so dialog can be reshown after app closes
        // Don't reset the game yet - wait until user clicks "Return to Main"
        db.shouldShowGameOverDialog = true;
        db.gameOverDialogType = "multi";
        db.gameOverAutoBalanceApplied = autoBalanceApplied;
        // Save all team scores before they're reset
        for (int i = 0; i < scores.length; i++) {
            db.gameOverAllScores[i] = scores[i];
        }

        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_game_over_title),
                ""
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_view_final_score), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                multiGameOverFinalScore(db.gameOverAllScores, autoBalanceApplied);
            }
        });
        dialog.show(fragmentManager, "MultiGameOver");
    }

    private void multiGameOverFinalScore(int[] scores, boolean autoBalanceApplied) {
        // CRITICAL FIX: Only consider teams that actually played (db.amountOfTeams)
        int teamsToCheck = Math.min(db.amountOfTeams, scores.length);

        // Find the highest score and check if there's a tie
        int maxScore = scores[0];
        int winningTeam = 1;

        for (int i = 1; i < teamsToCheck; i++) {
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                winningTeam = i + 1;
            }
        }

        // Count how many teams have the max score
        int teamsWithMaxScore = 0;
        for (int i = 0; i < teamsToCheck; i++) {
            if (scores[i] == maxScore) {
                teamsWithMaxScore++;
                if (teamsWithMaxScore == 1) {
                    winningTeam = i + 1;
                }
            }
        }

        // Build title: "Team X wins!" or "Draw!" if multiple teams have the highest score
        String title;
        if (teamsWithMaxScore > 1) {
            title = context.getString(nevo_mashiach.pitkiot.R.string.dialog_draw_title);
        } else {
            title = String.format(context.getString(nevo_mashiach.pitkiot.R.string.dialog_team_wins), winningTeam);
        }

        // Build scores list (sorted by score, highest first)
        String scoresText = buildScoresList(scores);

        // Build clickable message
        CharSequence message = buildScoreMessageWithClickableHelp(scoresText, autoBalanceApplied);

        MyDialogFragment dialog = new MyDialogFragment(
                title,
                message
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_return_main), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // CRITICAL FIX: Reset game only when user dismisses the final dialog
                db.resetGame();
                Toast.makeText(context, context.getString(nevo_mashiach.pitkiot.R.string.toast_game_reset), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
                db.gameOverDialogActivated = false;
            }
        });
        dialog.show(fragmentManager, "MultiGameOverFinalScore");
    }

    public void unevenExplanation() {
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_uneven_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_uneven_msg)
        );
        dialog = dialog.setNaturalButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_understood), null);
        dialog.show(fragmentManager, "UnevenExplanation"); //The second one is just a string tag that we can use to refer to it.
    }

    private CharSequence buildScoreMessageWithClickableHelp(String scoresText, boolean autoBalanceApplied) {
        String fullMessage = scoresText;

        if (autoBalanceApplied) {
            fullMessage += context.getString(nevo_mashiach.pitkiot.R.string.dialog_auto_balance_note);
            fullMessage += context.getString(nevo_mashiach.pitkiot.R.string.dialog_auto_balance_explanation_link);
        }

        SpannableString spannableString = new SpannableString(fullMessage);

        if (autoBalanceApplied) {
            String explanationLink = context.getString(nevo_mashiach.pitkiot.R.string.dialog_auto_balance_explanation_link).trim();
            int start = fullMessage.lastIndexOf(explanationLink);
            int end = start + explanationLink.length();

            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    unevenExplanation();
                }
            };

            int blueColor = ContextCompat.getColor(context, nevo_mashiach.pitkiot.R.color.colorPrimary);
            spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new ForegroundColorSpan(blueColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannableString;
    }

    private String buildScoresList(int[] scores) {
        StringBuilder scoreSummary = new StringBuilder();
        TreeMap<Integer, ArrayList<String>> treeMap = new TreeMap<>(Collections.reverseOrder());

        // CRITICAL FIX: Only show teams that actually played (db.amountOfTeams), not all 24
        int teamsToShow = Math.min(db.amountOfTeams, scores.length);
        for (int i = 0; i < teamsToShow; i++) {
            if(!treeMap.containsKey(scores[i])){
                treeMap.put(scores[i], new ArrayList<String>());
            }
            treeMap.get(scores[i]).add(String.format(context.getString(nevo_mashiach.pitkiot.R.string.dialog_team_score_with_points), (i + 1), scores[i]));
        }

        for (Map.Entry<Integer, ArrayList<String>> entry : treeMap.entrySet()) {
            for (String teamScore : entry.getValue()) {
                scoreSummary.append(teamScore);
            }
        }

        return scoreSummary.toString();
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
                // CRITICAL FIX: Set pause flags in memory
                if(currentActivity.equals("nevo_mashiach.pitkiot.GamePlay")) {
                    db.gamePlayIsPaused = true;
                    db.summaryIsPaused = false;
                } else if(currentActivity.equals("nevo_mashiach.pitkiot.Summary")) {
                    db.summaryIsPaused = true;
                    db.gamePlayIsPaused = false;
                }

                // CRITICAL FIX: Save pause flags to SharedPreferences immediately
                // Don't wait for onPause() - app could be killed before then
                android.content.SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
                android.content.SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("gamePlayIsPaused", db.gamePlayIsPaused);
                editor.putBoolean("summaryIsPaused", db.summaryIsPaused);
                editor.apply();

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
                MainActivity.appExit(context);
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
        // Only show this dialog if there's a game running in the background
        if (!db.gamePlayIsPaused && !db.summaryIsPaused) {
            return;
        }

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

    public void noGameRunning(final Runnable deleteNotesTask) {
        // Only show this dialog if there are notes in the database
        if (db.totalNoteAmount() == 0) {
            return;
        }

        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_no_game_running_title),
                context.getString(nevo_mashiach.pitkiot.R.string.dialog_no_game_running_msg)
        );
        dialog = dialog.setPositiveButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_no), null);
        dialog = dialog.setNegativeButton(context.getString(nevo_mashiach.pitkiot.R.string.dialog_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteNotesTask.run();
            }
        });
        dialog.show(fragmentManager, "NoGameRunning");
    }

    public void askDeleteNotesAfterReset(final Runnable deleteNotesTask) {
        // Only show this dialog if there are notes in the database
        if (db.totalNoteAmount() == 0) {
            return;
        }

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
