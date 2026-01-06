package nevo_mashiach.pitkiot.NotActivities;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.Random;

/**
 * Manages note collection sessions using Firebase Firestore.
 * Allows creating sessions and listening for note submissions in real-time.
 */
public class NoteCollectionSession {

    private static final String TAG = "NoteCollectionSession";
    private static final String COLLECTION_SESSIONS = "sessions";
    private static final String COLLECTION_SUBMISSIONS = "submissions";

    private FirebaseFirestore firestore;
    private String sessionId;
    private ListenerRegistration listenerRegistration;
    private OnNoteReceivedListener noteReceivedListener;

    public interface OnNoteReceivedListener {
        void onNoteReceived(String submitterName, String noteContent);
        void onError(String error);
    }

    public NoteCollectionSession() {
        firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Creates a new collection session with a unique 4-digit code
     * @return The session ID (e.g., "1234")
     */
    public String createSession() {
        // Generate a 4-digit random session code
        Random random = new Random();
        sessionId = String.format("%04d", random.nextInt(10000));

        Log.d(TAG, "Created session: " + sessionId);
        return sessionId;
    }

    /**
     * Gets the current session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets an existing session ID (for reconnecting)
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Generates the web form URL for this session
     * @param baseUrl Your Firebase Hosting URL (e.g., "https://pitkiot-app.web.app")
     */
    public String getSubmissionUrl(String baseUrl) {
        if (sessionId == null) {
            throw new IllegalStateException("Session not created. Call createSession() first.");
        }
        return baseUrl + "/submit?s=" + sessionId;
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
                        if (noteReceivedListener != null) {
                            noteReceivedListener.onError(error.getMessage());
                        }
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        // Process new submissions
                        for (DocumentSnapshot doc : snapshots.getDocumentChanges().stream()
                                .filter(dc -> dc.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED)
                                .map(dc -> dc.getDocument())
                                .toArray(DocumentSnapshot[]::new)) {

                            String submitterName = doc.getString("submitterName");
                            String noteContent = doc.getString("noteContent");

                            if (noteContent != null && !noteContent.trim().isEmpty()) {
                                Log.d(TAG, "New note from " + submitterName + ": " + noteContent);
                                if (noteReceivedListener != null) {
                                    noteReceivedListener.onNoteReceived(
                                            submitterName != null ? submitterName : "אנונימי",
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
        if (sessionId != null) {
            // We'll let Firestore TTL policies handle cleanup, or you can delete manually:
            // firestore.collection(COLLECTION_SESSIONS).document(sessionId).delete();
        }
        sessionId = null;
    }
}
