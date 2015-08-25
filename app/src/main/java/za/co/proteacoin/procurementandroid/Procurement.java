package za.co.proteacoin.procurementandroid;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.crypto.Cipher;


public class Procurement extends Activity {
    private final String TAG = "MAIN";
    GoogleCloudMessaging gcm;
    private Handler mHandler = new Handler();
    private TableRow trCompanyRow;
    private boolean loginSuccessFull = false;
    private SharedPreferences sharedPref;
    private int numberrOfSecurityIDViews = -1;
    private Button btnLogin;
    private Spinner mCompaniesSpinner;
    private EditText pw, dn, un;
    private TextView error;
    private GlobalState gs;

    // Create a broadcast receiver to get message and show on screen
    private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String newMessage = intent.getExtras().getString(GCMConfig.EXTRA_MESSAGE);

            // Waking up mobile if it is sleeping
            gs.acquireWakeLock(getApplicationContext());

            Toast.makeText(getApplicationContext(), "Got Message: " + newMessage, Toast.LENGTH_LONG).show();

            // Releasing wake lock
            gs.releaseWakeLock();
        }
    };

    private ArrayList<Integer> SystemApplicationDatabaseIdList;
    private ArrayList<String> CompanyCodeNameList;
    private String password;
    private int calmDeviceId = 0;
    private Cipher cipher;
    private String url = "";
    private Hashtable companiesTable;
    /**
     * Called when the activity is first created.
     */
    private String encryptedOutput;
    private ProgressDialog pDialog;
    private boolean hasError = false;
    // for JSON
    private String ErrorMessage = "";

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            final long start = GlobalState.getStartTime();
            long millis = SystemClock.uptimeMillis() - start;
            if (millis > GlobalState.INACTIVE_TIMEOUT) {
                // Return to the Procurement screen
                Intent intent = new Intent(getApplicationContext(), Procurement.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("EXIT", true);
                startActivity(intent);
            } else {

                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
//
//            if (seconds < 10) {
//                mTimeLabel.setText("" + minutes + ":0" + seconds);
//            } else {
//                mTimeLabel.setText("" + minutes + ":" + seconds);
//            }

                mHandler.postAtTime(this,
                        start + (((minutes * 60) + seconds + 10) * 1000));
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2) {
            //---if the result is OK---
            if (resultCode == RESULT_OK) {
                //---get the result using getIntExtra()---
                int i = data.getIntExtra("sapDeviceId", 0);
                sharedPref = Procurement.this.getPreferences(Context.MODE_PRIVATE);
                // Create editor for editing Preferences
                SharedPreferences.Editor edit = sharedPref.edit();
                edit.putInt("SAPDeviceId", i);
                edit.apply();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        GlobalState.setStartTime(SystemClock.uptimeMillis());
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        pw.setText("");
    }

    @Override
    protected void onStart() {
        super.onStart();
        pw.setText("");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getBooleanExtra("EXIT", false)) {
            mHandler.removeCallbacksAndMessages(null);
        }

        setContentView(R.layout.main);

        gs = (GlobalState) getApplication();

        trCompanyRow = (TableRow) findViewById(R.id.companyRow);
        // Hide the company row because we will only need it to be visible after a successfull login
        trCompanyRow.setVisibility(View.GONE);
        un = (EditText) findViewById(R.id.uName);
        pw = (EditText) findViewById(R.id.password);
        dn = (EditText) findViewById(R.id.domainName);
        error = (TextView) findViewById(R.id.tv_error);
        error.setText("");
        btnLogin = (Button) findViewById(R.id.loginButton);

        mCompaniesSpinner = (Spinner) findViewById(R.id.companySpinner);
        mCompaniesSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        // get the actual value of the selected approver name
                        String name = mCompaniesSpinner.getSelectedItem().toString();
                        // Now get the corresponding userId
                        gs.setCompanyDatabaseId(SystemApplicationDatabaseIdList.get(position));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        gs.setCompanyDatabaseId(SystemApplicationDatabaseIdList.get(0));
                    }
                }
        );

        SystemApplicationDatabaseIdList = new ArrayList<>();
        CompanyCodeNameList = new ArrayList<>();

        // Get settings that is saved to Preferences file
        sharedPref = Procurement.this.getPreferences(Context.MODE_PRIVATE);
        gs.setIvKey(sharedPref.getString("IVKey", ""));
        gs.setGcmIdentification(sharedPref.getString("GCMIdentification", ""));
        gs.setCalmDeviceId(sharedPref.getInt("SAPDeviceId", 0));
        gs.setUserName(sharedPref.getString("UserName", ""));
        gs.setDomainName(sharedPref.getString("DomainName", ""));
        final String uniqueDeviceID = sharedPref.getString("UniqueDeviceID", "");
        numberrOfSecurityIDViews = sharedPref.getInt("NumberrOfSecurityIDViews", 0);

        // Create editor for editing Preferences
        SharedPreferences.Editor edit = sharedPref.edit();

        // Check if the Username has been set
        if (!gs.getUserName().equals("")) {
            // Set the Username EditView to the Username and set the field to display only
            un.setText(gs.getUserName());
            un.setEnabled(false);
        }

        // Check if the Domain Name has been set
        if (!gs.getDomainName().equals("")) {
            // Set the Domain Name EditView to the Domain Name and set the field to display only
            dn.setText(gs.getDomainName());
            dn.setEnabled(false);
        }

        // Check if the Device ID has been set
        if (uniqueDeviceID.equals("")) {
            edit.putString("UniqueDeviceID", gs.getUniqueDeviceId());
        }

        // Check if the Version numbver of the app has changed => regId will be ""
        final String regId = GCMRegistrar.getRegistrationId(this);
        // Check if regid already presents
        if (gs.getGcmIdentification().equals("") || regId.equals("")) {
            // Register with GCM
            GCMRegistrar.register(GlobalState.getAppContext(), GCMConfig.GOOGLE_SENDER_ID);
            GetGCMRegId();
        }

        if (gs.getIvKey().equals("")) {
            gs.setIvKey(CryptLib.generateRandomIV(16)); //16 bytes = 128 bit
            edit.putString("IVKey", gs.getIvKey());
        }

        // Apply changes to Preferences
        edit.apply();

        // Check if an Internet connection is present
        if (!gs.isConnectingToInternet()) {

            // Internet Connection is not present
            gs.showAlertDialog(Procurement.this,
                    "Internet Connection Error",
                    "Please connect to Internet connection", false);
            // stop executing code by return
            return;
        }

        // Make sure the device has the proper dependencies.
        GCMRegistrar.checkDevice(this);

        // Make sure the manifest permissions was properly set
        GCMRegistrar.checkManifest(this);

        // Register custom Broadcast receiver to show messages on activity
        registerReceiver(mHandleMessageReceiver, new IntentFilter(
                GCMConfig.DISPLAY_MESSAGE_ACTION));

        // Get the version number
        String versionName = "";
        try {
            final PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // Display the version number in the ActionBar
        ActionBar ab = getActionBar();
        if (ab != null)
            ab.setTitle("Procurement. Version: " + versionName);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (gs.getCalmDeviceId() == 0) {
                    gs.showAlertDialog(Procurement.this, "Registration", "You must first register your device.", false);

                } else {
                    if (loginSuccessFull) {
                        Intent i = new Intent("android.intent.action.ShowGrid_Activity");
//                        i.putExtras(extras);
                        startActivityForResult(i, 1);
                    } else {
                        gs.setUserName(un.getText().toString());
                        password = pw.getText().toString();
                        calmDeviceId = gs.getCalmDeviceId();
                        gs.setDomainName(dn.getText().toString());

                        final GetLoginResult downloader = new GetLoginResult();
                        downloader.execute();
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (downloader.getStatus() == AsyncTask.Status.RUNNING) {
                                    downloader.cancel(true);
                                    if (pDialog.isShowing()) {
                                        pDialog.dismiss();
                                    }
                                    new AlertDialog.Builder(Procurement.this)
                                            .setTitle("Result")
                                            .setMessage("Internet connection timed out. Try again?")
                                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    btnLogin.performClick();
                                                }
                                            })
                                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    android.os.Process.killProcess(android.os.Process.myPid());
                                                    System.exit(1);
                                                }
                                            })
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .show();
                                }
                            }
                        }, 60000);
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
//                Intent setupGCMIntent = new Intent("android.intent.action.GCMRegisterActivity");
//                startActivity(setupGCMIntent);
                return true;
            case R.id.showIVKey:
                // If the device has already been registered with the company then return
                if (gs.getCalmDeviceId() > 0) {
                    gs.showAlertDialog(Procurement.this, "Security Code", "You have already registered this device. ID: " + gs.getCalmDeviceId(), true);
                    return true;
                }
                // TODO Remove the below
                numberrOfSecurityIDViews = 0;
                if (++numberrOfSecurityIDViews > 2) {
                    gs.showAlertDialog(Procurement.this, "Security Code", "You have reached your maximum allowed chances to view yout Security code", false);
                } else {
                    String msg = "You have only " + (2 - numberrOfSecurityIDViews) + " chance left to view your Security Code";
                    new AlertDialog.Builder(Procurement.this)
                            .setTitle("Security Code")
                            .setMessage(msg)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent i = new Intent("android.intent.action.CompanyInit_Activity");
                                    startActivityForResult(i, 2);
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void GetGCMRegId() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
                    }
                    gs.setGcmIdentification(gcm.register(GlobalState.PROJECT_ID));
                    msg = "Device registered, registration ID=" + gs.getGcmIdentification();
                    Log.d("GCM", msg);

                } catch (IOException ex) {
                    msg = "Error trying to get GCM ID: " + ex.getMessage();

                }
                return msg;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                // Showing progress dialog
                pDialog = new ProgressDialog(Procurement.this);
                pDialog.setMessage("Please wait...");
                pDialog.show();
            }

            @Override
            protected void onPostExecute(String msg) {
                if (msg.startsWith("Error")) {
                    gs.showAlertDialog(Procurement.this, "Error", msg, false);
                } else {
                    // Save GCMIdentification to the Preferences file
                    sharedPref = Procurement.this.getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor edit = sharedPref.edit();
                    edit.putString("GCMIdentification", gs.getGcmIdentification());
                    edit.apply();
                }

            }
        }.execute(null, null, null);
    }


    private class GetLoginResult extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();

            // TODO - Remove hardcoded values
            password = "D@n13August";

            JSONObject obj = new JSONObject();
            try {
                obj.put("DomainName", gs.getDomainName());
                obj.put("Username", gs.getUserName());
                obj.put("Password", password);
            } catch (Exception e) {
                e.printStackTrace();
            }
            String source = obj.toString();

            String encryptedString = "";

            try {
                encryptedString = gs.bytesToHex(gs.encrypt(source));
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Making a request to url and getting response
            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();

            queryParams.add(new BasicNameValuePair("mobileDeviceId", String.valueOf(gs.getCalmDeviceId())));
            queryParams.add(new BasicNameValuePair("systemApplicationId", GlobalState.SYSTEM_APPLICATION_ID));
            queryParams.add(new BasicNameValuePair("encryptedPackage", encryptedString));

            String url = GlobalState.LOGIN_URL;
            String jsonStr = sh.makeServiceCall(url, ServiceHandler.POST, queryParams);

            // Decrypt jsonStr
            String decryptedString;
            if (jsonStr != null) {
                try {
                    byte[] decryptedJson = gs.decrypt(jsonStr);
                    JSONObject jsonObj = new JSONObject(new String(decryptedJson));

                    // Check for error
                    JSONArray data = jsonObj.getJSONArray("results");
                    jsonObj = data.getJSONObject(0);
                    try {
                        String error = jsonObj.getString("Error");
                        hasError = true;
                        ErrorMessage = "The following message occured while trying to login. \n" + error;
                        return null;
                    } catch (Exception e) {
                        // Intentially left blank
                    }

                    String firstName = jsonObj.getString("FirstName");
                    String surName = jsonObj.getString("Surname");
                    gs.setCommonUserId(jsonObj.getString("CommonUserId"));
                    // looping through All Companies
                    companiesTable = new Hashtable();
                    JSONArray companies = jsonObj.getJSONArray("Companies");
                    for (int i = 0; i < companies.length(); i++) {
                        JSONObject c = companies.getJSONObject(i);
                        int key = c.getInt("SystemApplicationDatabaseId");
                        String value = c.getString("CompanyCodeName");
                        SystemApplicationDatabaseIdList.add(i, key);
                        CompanyCodeNameList.add(i, value);
                    }
                    gs.setUserFirstName(firstName);
                    gs.setUserSurName(surName);

                    loginSuccessFull = true;

                } catch (Exception e) {
                    e.printStackTrace();
                    ErrorMessage = "The following Exception occured while trying to login. \n" + e.getMessage();
                }
            } else {
                Log.e("ServiceHandler", "Couldn't get any data from the url");
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(Procurement.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Dismiss the progress dialog

            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }
            if (loginSuccessFull) {
                btnLogin.setText("Continue");

                // Start the timer for inactivity action
                GlobalState.setStartTime(SystemClock.uptimeMillis());
                mHandler = new Handler();
                mHandler.postDelayed(mUpdateTimeTask, 100);

                // Save the username and domain name to the preferences file
                sharedPref = Procurement.this.getPreferences(Context.MODE_PRIVATE);
                // Create editor for editing Preferences
                SharedPreferences.Editor edit = sharedPref.edit();
                edit.putString("UserName", un.getText().toString());
                gs.setUserName(un.getText().toString());
                edit.putString("DomainName", dn.getText().toString());
                gs.setDomainName(dn.getText().toString());
                edit.apply();

                mCompaniesSpinner.setAdapter(new ArrayAdapter<String>(Procurement.this, android.R.layout.simple_spinner_dropdown_item, CompanyCodeNameList));
                trCompanyRow.setVisibility(View.VISIBLE);
            } else {
                btnLogin.setText("Login");
                error.setText("Invalid username or password.");
            }
        }
    }
}