package nevo_mashiach.pitkiot.NotActivities;

import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import nevo_mashiach.pitkiot.MainActivity;


/**
 * Created by Nevo2 on 9/14/2016.
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
        MyDialogFragment dialog = new MyDialogFragment("שימו לב משנים מוד משחק!", "מעכשיו " + db.getRoundMode());
        dialog = dialog.setNaturalButton("הבנתי", null);
        dialog.show(fragmentManager, "ModeChanged"); //The second one is just a string tag that we can use to refer to it.
    }

    public void dbEmptyBeforeGame() {
        MyDialogFragment dialog = new MyDialogFragment("אין פתקים", "אוי לא");
        dialog = dialog.setNaturalButton("חזרה לתפריט הראשי", null);
        dialog.show(fragmentManager, "OutOfNotes"); //The second one is just a string tag that we can use to refer to it.
    }

    public void normalGameOver(int winningTeam, int winningTotal, int loserTotal) {
        db.gameOverDialogActivated = true;
        db.resetGame();
        MyDialogFragment dialog = new MyDialogFragment("נגמר המשחק!", "קבוצה " + winningTeam +  " ניצחה " + loserTotal + " : " + winningTotal);
        dialog = dialog.setPositiveButton("חזרה לתפריט הראשי", new DialogInterface.OnClickListener() {
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
        MyDialogFragment dialog = new MyDialogFragment("נגמר המשחק!", "תיקו " + score + " : " + score);
        dialog = dialog.setNaturalButton("חזרה לתפריט הראשי", new DialogInterface.OnClickListener() {
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
        MyDialogFragment dialog = new MyDialogFragment("נגמר המשחק!", "");
        dialog = dialog.setPositiveButton("צפייה בניקוד הסופי", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                multiGameOverFinalScore(scores);
            }
        });
        dialog.show(fragmentManager, "NormalGameOver"); //The second one is just a string tag that we can use to refer to it.
    }

    private void multiGameOverFinalScore(int[] scores) {
        String scoreSummary =  "";
        TreeMap<Integer, ArrayList<String>> treeMap = new TreeMap<>(Collections.reverseOrder());
        for (int i = 0; i < db.amountOfTeams; i++) {
            if(!treeMap.containsKey(scores[i])){
                treeMap.put(scores[i], new ArrayList<String>());
            }
            treeMap.get(scores[i]).add("קבוצה " + (i + 1) + ": " + scores[i] + "\n");
        }
        Set set = treeMap.entrySet();
        Iterator iterator = set.iterator();
        while(iterator.hasNext()) {
            Map.Entry mentry = (Map.Entry)iterator.next();
            for (int j = 0; j < ((ArrayList)mentry.getValue()).size(); j++) {
                scoreSummary += ((ArrayList)mentry.getValue()).get(j);
            }
        }
        MyDialogFragment dialog = new MyDialogFragment("ניקוד סופי", scoreSummary);
        dialog = dialog.setPositiveButton("חזרה לתפריט הראשי", new DialogInterface.OnClickListener() {
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
        MyDialogFragment dialog = new MyDialogFragment("הסבר האפשרות", "לפעמים המשחק נגמר כך שחלק מהקבוצות שיחקו סיבוב אחד פחות מקבוצות אחרות (או קיבלו פחות זמן בסיבוב האחרון שלהן). על מנת להימנע מאי צדק נבזי שכזה, בסיום המשחק, אפשרות זו תוסיף לקבוצות המקופחות נקודות בהתאם לביצועיהן.\nבקיצור אם אתם רוצים משחק הוגן סמנו את האופציה בווי.");
        dialog = dialog.setNaturalButton("הבנתי", null);
        dialog.show(fragmentManager, "UnevenExplanation"); //The second one is just a string tag that we can use to refer to it.
    }

    public void invalidInput() {
        MyDialogFragment dialog = new MyDialogFragment("קלט לא תקין!", "נא להזין מספר שלם בין 1 ל 300");
        dialog = dialog.setNaturalButton("בסדר", null);
        dialog.show(fragmentManager, "InvalidInput"); //The second one is just a string tag that we can use to refer to it.
    }

    public void backToMainMenu(final String currentActivity) {
        MyDialogFragment dialog = new MyDialogFragment("חזרה לתפריט הראשי", "האם אתה בטוח שברצונך לחזור לתפריט הראשי? (תוכל לבצע שינויים ולשוב לאותו מצב משחק).");
        dialog = dialog.setPositiveButton("חזרה לתפריט הראשי", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(currentActivity.equals("nevo_mashiach.pitkiot.GamePlay")) db.gamePlayIsPaused = true;
                else if(currentActivity.equals("nevo_mashiach.pitkiot.Summary")) db.summaryIsPaused = true;
                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
            }
        });
        dialog = dialog.setNegativeButton("המשך לשחק", null);
        dialog.show(fragmentManager, "BackToMainMenu"); //The second one is just a string tag that we can use to refer to it.
    }

    public void exitTheGame() {
        MyDialogFragment dialog = new MyDialogFragment("יציאה מהמשחק", "האם אתה בטוח שברצונך לצאת מהמשחק?");
        dialog = dialog.setPositiveButton("יציאה", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MainActivity.appExit();
            }
        });
        dialog = dialog.setNegativeButton("המשך לשחק", null);
        dialog.show(fragmentManager, "ExitTheGame"); //The second one is just a string tag that we can use to refer to it.
    }

    public void confirmCharacterDelete(final Runnable task) {
        MyDialogFragment dialog = new MyDialogFragment("הסרת דמות", "האם אתה בטוח שברצונך למחוק את הדמות?");
        dialog = dialog.setPositiveButton("כן", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                task.run();
            }
        });
        dialog = dialog.setNegativeButton("לא", null);
        dialog.show(fragmentManager, "ConfirmCharacterRemove"); //The second one is just a string tag that we can use to refer to it.
    }

    public void confirmDeleteAll(final Runnable task) {
        MyDialogFragment dialog = new MyDialogFragment("ריקון מאגר פתקים", "האם אתה בטוח שברצונך למחוק את כל הפתקים מהמאגר?");
        dialog = dialog.setPositiveButton("כן", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                task.run();
            }
        });
        dialog = dialog.setNegativeButton("לא", null);
        dialog.show(fragmentManager, "ConfirmCharacterRemove"); //The second one is just a string tag that we can use to refer to it.
    }

    public void smsExplanation() {
        MyDialogFragment dialog = new MyDialogFragment("סרוק סמסים", "בעת הלחיצה על הכפתור יסרקו כל הסמסים שהתקבלו מאז הזמן המפורט. סמס בפורמט המתאים ישמש כמקור הוספת פתקים למאגר. לצורך כך יש לשלוח סמס בתבנית הבאה:\n\"פתקיות: דמות1, דמות2, דמות3....\"\nאין משמעות לרווחים בסמס אך חשוב שבתחילתו יהיה רשום \"פיתקיות\"\nאו \"פתקיות\", לאחר מכן נקודותיים ולבסוף שמות הדמויות מופרדים בפסיקים.");
        dialog = dialog.setNaturalButton("הבנתי", null);
        dialog.show(fragmentManager, "SmsExplanation"); //The second one is just a string tag that we can use to refer to it.
    }

    public void smsScaned(int amount) {
        MyDialogFragment dialog = new MyDialogFragment("סמסים נסרקו", "מספר הודעות מתאימות שהתקבלו: " + amount);
        dialog = dialog.setNaturalButton("אוקיי", null);
        dialog.show(fragmentManager, "SmsScaned"); //The second one is just a string tag that we can use to refer to it.
    }

    public void resetGame(final Runnable task) {
        MyDialogFragment dialog = new MyDialogFragment("איפוס משחק", "האם אתה בטוח שברצונך לאפס את המשחק? שים לב כי המשחק יתחיל מההתחלה אך הפתקים ישארו.\nלמחיקת הפתקים עבור ל\"ניהול פתקים\".");
        dialog = dialog.setPositiveButton("כן", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
               task.run();
            }
        });
        dialog = dialog.setNegativeButton("לא", null);
        dialog.show(fragmentManager, "ResetGame"); //The second one is just a string tag that we can use to refer to it.
    }

    public void resetSettings(final Runnable task) {
        MyDialogFragment dialog = new MyDialogFragment("איפוס הגדרות", "האם אתה בטוח שברצונך לאפס את ההגדרות? שים לב כי ניקוד הקבוצות יאופס רק בעת איפוס המשחק.");
        dialog = dialog.setPositiveButton("כן", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                task.run();
            }
        });
        dialog = dialog.setNegativeButton("לא", null);
        dialog.show(fragmentManager, "ResetGame"); //The second one is just a string tag that we can use to refer to it.
    }

    public void resumeGame(final Runnable task) {
        MyDialogFragment dialog = new MyDialogFragment("חזרה למשחק מתנהל", "שים לב, אתה עומד לחזור למשחק שמתנהל. אם ברצונך להתחיל משחק חדש הישאר בעמוד הראשי ולחץ על \"איפוס משחק\".");
        dialog = dialog.setPositiveButton("חזור למשחק מתנהל", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                task.run();
            }
        });
        dialog = dialog.setNegativeButton("הישאר בעמוד ראשי", null);
        dialog.show(fragmentManager, "FirstNoteLoad"); //The second one is just a string tag that we can use to refer to it.
    }

    public void clickAgainPlease() {
        MyDialogFragment dialog = new MyDialogFragment("נא ללחוץ שוב על הכפתור", "במידה ואישרת יש ללחוץ שוב על הכפתור \"סרוק סמסים\" על מנת להתחיל בתהליך הסריקה.");
        dialog = dialog.setNaturalButton("הבנתי", null);
        dialog.show(fragmentManager, "ClickAgainPlease"); //The second one is just a string tag that we can use to refer to it.
    }

    public void allowingAccessToSms(final Runnable task) {
        MyDialogFragment dialog = new MyDialogFragment("בקשת גישה לסמסים", "על מנת שהאפליקציה תוכל לסרוק את הסמסים שעל מכשירך, עליה לקבל אישור גישה חד פעמי. אנא לחץ על 'התר' בחלון הבא. אל תהיה פרנואיד, אין לי באמת מה לעשות עם הסמסים שלך.");
        dialog = dialog.setPositiveButton("הבנתי", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                task.run();
            }
        });
        dialog.show(fragmentManager, "AllowingAccessToSms"); //The second one is just a string tag that we can use to refer to it.
    }

    public void cannotEditScore() {
        MyDialogFragment dialog = new MyDialogFragment("לא ניתן לערוך נקודות", "אין משחק שמתנהל ברקע. על מנת לערוך ניקוד קבוצות ראשית התחל משחק חדש ולאחר מכן חזור לחלון ההגדרות.");
        dialog = dialog.setNaturalButton("הבנתי", null);
        dialog.show(fragmentManager, "ClickAgainPlease"); //The second one is just a string tag that we can use to refer to it.
    }

    public void cannotEditTeamsAmount() {
        MyDialogFragment dialog = new MyDialogFragment("לא ניתן לערוך מספר קבוצות", "יש משחק שמתנהל ברקע. על מנת לערוך מספר קבוצות יש לסיים או לאפס את המשחק.");
        dialog = dialog.setNaturalButton("הבנתי", null);
        dialog.show(fragmentManager, "ClickAgainPlease"); //The second one is just a string tag that we can use to refer to it.
    }
}
