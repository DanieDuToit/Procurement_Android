package za.co.proteacoin.procurementandroid;

import za.co.proteacoin.procurementandroid.GCMConstants;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class GCMBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GCMBroadcastReceiver";
    private static boolean mReceiverSet = false;

    @Override
    public final void onReceive(Context context, Intent intent) {
        Log.v(TAG, "onReceive: " + intent.getAction());
        // do a one-time check if app is using a custom GCMBroadcastReceiver
        if (!mReceiverSet) {
            mReceiverSet = true;
            String myClass = getClass().getName();
            if (!myClass.equals(GCMBroadcastReceiver.class.getName())) {
                GCMRegistrar.setRetryReceiverClassName(myClass);
            }
        }
        String className = getGCMIntentServiceClassName(context);
        Log.v(TAG, "GCM IntentService class: " + className);
        // Delegates to the application-specific intent service.
        GCMBaseIntentService.runIntentInService(context, intent, className);
        setResult(Activity.RESULT_OK, null /* data */, null /* extra */);
    }

    /**
     * Gets the class name of the intent service that will handle GCM messages.
     */
    protected String getGCMIntentServiceClassName(Context context) {
        return getDefaultIntentServiceClassName(context);
    }

    /**
     * Gets the default class name of the intent service that will handle GCM
     * messages.
     */
    static final String getDefaultIntentServiceClassName(Context context) {
        String className = context.getPackageName() +
                GCMConstants.DEFAULT_INTENT_SERVICE_CLASS_NAME;
        return className;
    }
}
