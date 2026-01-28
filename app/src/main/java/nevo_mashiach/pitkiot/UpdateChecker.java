package nevo_mashiach.pitkiot;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import nevo_mashiach.pitkiot.NotActivities.MyDialogFragment;

/**
 * Checks for app updates using Firebase Firestore
 */
public class UpdateChecker {

    private static final String TAG = "UpdateChecker";
    private static final String FIRESTORE_COLLECTION = "app_config";
    private static final String FIRESTORE_DOCUMENT = "version_info";
    private static final String PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=nevo_mashiach.pitkiot";

    // Track if we've already checked for updates in this app session
    private static boolean hasCheckedThisSession = false;

    private Context context;
    private FragmentManager fragmentManager;
    private FirebaseFirestore db;

    public UpdateChecker(Context context, FragmentManager fragmentManager) {
        this.context = context;
        this.fragmentManager = fragmentManager;
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Get the current app version code
     */
    private int getCurrentVersionCode() {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .versionCode;
        } catch (Exception e) {
            Log.e(TAG, "Error getting version code", e);
            return 0;
        }
    }

    /**
     * Check for updates and show dialog if needed
     */
    public void checkForUpdate() {
        // Only check once per app session
        if (hasCheckedThisSession) {
            Log.d(TAG, "Already checked for updates this session, skipping");
            return;
        }

        hasCheckedThisSession = true;
        Log.d(TAG, "Starting update check...");
        db.collection(FIRESTORE_COLLECTION)
                .document(FIRESTORE_DOCUMENT)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        Log.d(TAG, "Firestore query completed. Success: " + task.isSuccessful());
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            Log.d(TAG, "Document exists: " + document.exists());
                            if (document.exists()) {
                                handleVersionInfo(document);
                            } else {
                                Log.d(TAG, "Version info document does not exist in Firestore");
                            }
                        } else {
                            Log.e(TAG, "Error fetching version info", task.getException());
                        }
                    }
                });
    }

    private void handleVersionInfo(DocumentSnapshot document) {
        try {
            // Get version info from Firestore
            Long latestVersionCode = document.getLong("latestVersionCode");
            String latestVersionName = document.getString("latestVersionName");
            Long minimumVersionCode = document.getLong("minimumVersionCode");

            Log.d(TAG, "Firestore values - latest: " + latestVersionCode + ", minimum: " + minimumVersionCode + ", name: " + latestVersionName);

            if (latestVersionCode == null || latestVersionName == null) {
                Log.e(TAG, "Invalid version info in Firestore");
                return;
            }

            // Get current app version from PackageManager
            int currentVersionCode = getCurrentVersionCode();
            Log.d(TAG, "Current app version: " + currentVersionCode);

            // Check if update is required (current version is below minimum)
            if (minimumVersionCode != null && currentVersionCode < minimumVersionCode) {
                Log.d(TAG, "Showing UPDATE REQUIRED dialog");
                showUpdateRequiredDialog(latestVersionName);
            }
            // Check if update is available (current version is below latest)
            else if (currentVersionCode < latestVersionCode) {
                Log.d(TAG, "Showing UPDATE AVAILABLE dialog");
                showUpdateAvailableDialog(latestVersionName);
            }
            // App is up to date
            else {
                Log.d(TAG, "App is up to date - no dialog needed");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing version info", e);
        }
    }

    /**
     * Show dialog for optional update
     */
    private void showUpdateAvailableDialog(String versionName) {
        Log.d(TAG, "Creating UPDATE AVAILABLE dialog for version: " + versionName);
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(R.string.dialog_update_available_title),
                String.format(context.getString(R.string.dialog_update_available_msg), versionName)
        );

        dialog = dialog.setPositiveButton(
                context.getString(R.string.button_update_now),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        openPlayStore();
                    }
                }
        );

        dialog = dialog.setNegativeButton(
                context.getString(R.string.button_later),
                null
        );

        dialog.show(fragmentManager, "UpdateAvailable");
        Log.d(TAG, "UPDATE AVAILABLE dialog shown");
    }

    /**
     * Show dialog for required update (user must update)
     */
    private void showUpdateRequiredDialog(String versionName) {
        Log.d(TAG, "Creating UPDATE REQUIRED dialog for version: " + versionName);
        MyDialogFragment dialog = new MyDialogFragment(
                context.getString(R.string.dialog_update_required_title),
                String.format(context.getString(R.string.dialog_update_required_msg), versionName)
        );

        dialog = dialog.setNaturalButton(
                context.getString(R.string.button_update),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        openPlayStore();
                        // Close the app completely so user must update before using it again
                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).finishAffinity();
                        }
                        // Force exit the entire app process
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                }
        );

        // Make dialog non-cancelable for required updates
        dialog.setCancelable(false);

        dialog.show(fragmentManager, "UpdateRequired");
        Log.d(TAG, "UPDATE REQUIRED dialog shown");
    }

    /**
     * Open the app's Play Store page
     */
    private void openPlayStore() {
        try {
            // Try to open in Play Store app
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=nevo_mashiach.pitkiot"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            // If Play Store app is not available, open in browser
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
