package za.co.proteacoin.procurementandroid;

public interface GCMConfig {
    // CONSTANTS
    // Google project id
    static final String GOOGLE_SENDER_ID = "564345734817";  // Place here your Google project id

    /**
     * Tag used on log messages.
     */
    static final String TAG = "GCM - PROCUREMENT";

    static final String DISPLAY_MESSAGE_ACTION =
            "za.co.proteacoin.procurementandroid.DISPLAY_MESSAGE";

    static final String EXTRA_MESSAGE = "message";
}
