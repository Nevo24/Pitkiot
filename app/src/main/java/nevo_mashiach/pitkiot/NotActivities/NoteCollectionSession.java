package nevo_mashiach.pitkiot.NotActivities;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.Random;

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
        void onNoteReceived(String submitterName, String noteContent);
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
     * @param existingSessionId The full session ID (format: "deviceId_shortCode")
     */
    public NoteCollectionSession(Context context, String existingSessionId) {
        this.context = context;
        firestore = FirebaseFirestore.getInstance();
        this.sessionId = existingSessionId;
        // Extract device ID from session ID (format: "deviceId_shortCode")
        if (existingSessionId != null && existingSessionId.contains("_")) {
            this.deviceId = existingSessionId.split("_")[0];
        } else {
            this.deviceId = getDeviceId();
        }
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
     * Creates a new collection session with a unique 3-digit code combined with device ID
     * @return The session ID (e.g., "123")
     */
    public String createSession() {
        // Generate a 3-digit random session code
        Random random = new Random();
        String shortCode = String.format("%03d", random.nextInt(1000));

        // Combine with device ID for uniqueness
        sessionId = deviceId + "_" + shortCode;

        Log.d(TAG, "Created session: " + sessionId + " (short code: " + shortCode + ")");
        return shortCode; // Return only the short code for display purposes
    }

    /**
     * Gets the current full session ID (with device ID)
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Gets the short code (3 digits only) for display
     */
    public String getShortCode() {
        if (sessionId != null && sessionId.contains("_")) {
            return sessionId.split("_")[1];
        }
        return sessionId;
    }

    /**
     * Generates the web form URL for this session
     * @param baseUrl Your Firebase Hosting URL (e.g., "<a href="https://pitkiot-app.web.app">...</a>")
     */
    public String getSubmissionUrl(String baseUrl) {
        if (sessionId == null) {
            throw new IllegalStateException("Session not created. Call createSession() first.");
        }
        String shortCode = getShortCode();
        return baseUrl + "/submit?d=" + deviceId + "&s=" + shortCode;
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

                            String submitterName = doc.getString("submitterName");
                            String noteContent = doc.getString("noteContent");

                            if (noteContent != null && !noteContent.trim().isEmpty()) {
                                Log.d(TAG, "New note from " + submitterName + ": " + noteContent);
                                if (noteReceivedListener != null) {
                                    noteReceivedListener.onNoteReceived(
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
     * Cleans up the session (call when done)
     */
    public void endSession() {
        stopListening();
        // Optionally delete the session from Firestore
        // We'll let Firestore TTL policies handle cleanup, or you can delete manually:
        // firestore.collection(COLLECTION_SESSIONS).document(sessionId).delete();
        sessionId = null;
    }
}
