package nevo_mashiach.pitkiot;

import android.app.Application;
import nevo_mashiach.pitkiot.NotActivities.db;

public class PitkiotApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        db.getInstance();
        db.initializeSounds(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        db.releaseSounds();
    }
}
