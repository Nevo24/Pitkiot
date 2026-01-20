package nevo_mashiach.pitkiot.NotActivities;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import nevo_mashiach.pitkiot.R;

/**
 * Manages note collection sessions using Firebase Firestore.
 * Allows creating sessions and listening for note submissions in real-time.
 */
public class NoteCollectionSession {

    private static final String TAG = "NoteCollectionSession";
    private static final String COLLECTION_SESSIONS = "sessions";
    private static final String COLLECTION_SUBMISSIONS = "submissions";

    private final FirebaseFirestore firestore;
    private final Context context;
    private String sessionId;
    private String deviceId;
    private ListenerRegistration listenerRegistration;
    private OnNoteReceivedListener noteReceivedListener;

    public interface OnNoteReceivedListener {
        void onNoteReceived(String submissionId, String submitterName, String noteContent);
        void onError(String error);
    }

    public NoteCollectionSession(Context context) {
        this.context = context;
        firestore = FirebaseFirestore.getInstance();
        deviceId = getDeviceId();
    }

    /**
     * Constructor for restoring an existing session with a known session ID
     * @param context Application context
     * @param existingSessionId The session ID (device ID)
     */
    public NoteCollectionSession(Context context, String existingSessionId) {
        this.context = context;
        firestore = FirebaseFirestore.getInstance();
        this.deviceId = existingSessionId != null ? existingSessionId : getDeviceId();
        this.sessionId = this.deviceId;
    }

    /**
     * Gets the device ID (Android ID) for this device
     * @return The device ID
     */
    public String getDeviceId() {
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        // Use first 8 characters for brevity
        return androidId != null && androidId.length() >= 8 ? androidId.substring(0, 8) : "default";
    }

    /**
     * Creates a new collection session using the device ID
     * @return The session ID (device ID)
     */
    public String createSession() {
        // Use device ID as the session ID (one permanent room per device)
        sessionId = deviceId;

        Log.d(TAG, "Created/reusing session: " + sessionId);
        return deviceId; // Return device ID for display
    }

    /**
     * Gets the current session ID (device ID)
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Gets the session code for display (same as device ID)
     */
    public String getShortCode() {
        return sessionId;
    }

    /**
     * Generates the web form URL for this session
     * @param baseUrl Your Firebase Hosting URL (e.g., "https://pitkiot-app.web.app")
     */
    public String getSubmissionUrl(String baseUrl) {
        if (sessionId == null) {
            throw new IllegalStateException("Session not created. Call createSession() first.");
        }
        return baseUrl + "/submit?room=" + sessionId;
    }

    /**
     * Starts listening for note submissions in real-time
     * @param listener Callback for when notes are received
     */
    public void startListening(OnNoteReceivedListener listener) {
        if (sessionId == null) {
            throw new IllegalStateException("Session not created. Call createSession() first.");
        }

        this.noteReceivedListener = listener;

        // Listen to the submissions subcollection
        listenerRegistration = firestore
                .collection(COLLECTION_SESSIONS)
                .document(sessionId)
                .collection(COLLECTION_SUBMISSIONS)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed: " + error.getMessage());
                        stopListening();
                        if (noteReceivedListener != null) {
                            noteReceivedListener.onError(error.getMessage());
                        }
                        return;
                    }

                    // FIX: Handle null snapshots case properly
                    if (snapshots == null) {
                        Log.w(TAG, "Received null snapshot data - possible network issue or permission error");
                        // Don't call onError for null snapshots during initial load
                        // This can happen during first connection or temporary network issues
                        // The listener will retry automatically
                        return;
                    }

                    // Process new submissions only (not initial data)
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            DocumentSnapshot doc = dc.getDocument();

                            String submissionId = doc.getId();
                            String submitterName = doc.getString("submitterName");
                            String noteContent = doc.getString("noteContent");

                            if (noteContent != null && !noteContent.trim().isEmpty()) {
                                Log.d(TAG, "New note from " + submitterName + ": " + noteContent);
                                if (noteReceivedListener != null) {
                                    noteReceivedListener.onNoteReceived(
                                            submissionId,
                                            submitterName != null ? submitterName : context.getString(R.string.submitter_anonymous),
                                            noteContent
                                    );
                                }
                            }
                        }
                    }
                });

        Log.d(TAG, "Started listening for session: " + sessionId);
    }

    /**
     * Stops listening for submissions
     */
    public void stopListening() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
            Log.d(TAG, "Stopped listening");
        }
    }

    /**
     * Stops listening for submissions (session persists in Firestore)
     */
    public void endSession() {
        stopListening();
    }

    /**
     * Clears all submissions from the current session (but keeps the session itself)
     * Call this when the user finishes collecting and saves notes
     */
    public void clearSubmissions() {
        if (sessionId == null) {
            Log.w(TAG, "Cannot clear submissions: sessionId is null");
            return;
        }

        Log.d(TAG, "Clearing submissions for session: " + sessionId);

        // Delete all submissions from the session
        firestore.collection(COLLECTION_SESSIONS)
                .document(sessionId)
                .collection(COLLECTION_SUBMISSIONS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Delete each submission document
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete()
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Deleted submission: " + doc.getId()))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete submission: " + doc.getId(), e));
                    }
                    Log.d(TAG, "Successfully cleared submissions for session: " + sessionId);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch submissions for deletion", e));
    }
}
