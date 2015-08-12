package za.co.proteacoin.procurementandroid;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Procurement extends Activity {
    /**
     * Called when the activity is first created.
     */
    final String TAG = "MAIN";
    Button loginButton;
    Spinner mCompaniesSpinner;
    String selectedCompany;
    EditText un, pw;
    TextView error;
    HashMap<String, Integer> companyList;
    AssetManager assets;
    String packageName;
    GlobalState gs;
    boolean loginSuccessFull = false;
    SharedPreferences sharedPref;
    GoogleCloudMessaging gcm;
    // Hashmap for ListView
    ArrayList<HashMap<String, String>> resultList;
    // Requisition's JSONArray
    JSONArray data = null;
    private ProgressDialog pDialog;
    private boolean hasError = false;
    private String ErrorMessage = "";
    // for JSON

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //---check if the request code is 1---
        if (requestCode == 1) {
            //---if the result is OK---
            if (resultCode == RESULT_OK) {
                //---get the result using getIntExtra()---
                Toast.makeText(this, Integer.toString(
                                data.getIntExtra("age3", 0)),
                        Toast.LENGTH_SHORT).show();
                //---get the result using getData()---
                Uri url = data.getData();
                Toast.makeText(this, url.toString(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        //        un = (EditText) findViewById(R.id.userName);
        //        pw = (EditText) findViewById(R.id.password);
        un.setText("");
        pw.setText("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        //        un = (EditText) findViewById(R.id.userName);
        //        pw = (EditText) findViewById(R.id.password);
        un.setText("");
        pw.setText("");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gs = (GlobalState) getApplication();

        // Get settings that is saved to Preferences file
        sharedPref = Procurement.this.getPreferences(Context.MODE_PRIVATE);
        gs.setIvKey(sharedPref.getString("IVKey", ""));
        gs.setGcmIdentification(sharedPref.getString("GCMIdentification", ""));
        final String uniqueDeviceID = sharedPref.getString("UniqueDeviceID", "");

        // Create editor for editing Preferences
        SharedPreferences.Editor edit = sharedPref.edit();

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

        try {
            CryptLib _crypt = new CryptLib();
            String output = "";
            String plainText = "This is the text to be encrypted.";
            String key = CryptLib.SHA256("procurementandroid", 32); //32 bytes = 256 bit
//            String iv = CryptLib.generateRandomIV(16); //16 bytes = 128 bit
            output = _crypt.encrypt(plainText, key, gs.getIvKey()); //encrypt
            System.out.println("encrypted text=" + output);
            output = _crypt.decrypt(output, key, gs.getIvKey()); //decrypt
            System.out.println("decrypted text=" + output);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        companyList = new HashMap<String, Integer>();

        setContentView(R.layout.main);

        getActionBar().setTitle("Procurement");

        assets = getAssets();
        packageName = getPackageName();
        //        db = new DBAdapter(this, assets, packageName);
        un = (EditText) findViewById(R.id.userName);
        pw = (EditText) findViewById(R.id.password);
        error = (TextView) findViewById(R.id.tv_error);
        error.setText("");

        // Spinner View
        mCompaniesSpinner = (Spinner) findViewById(R.id.companies);
        mCompaniesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCompany = (String) mCompaniesSpinner.getSelectedItem();
                gs.setCompanyName(selectedCompany);
                gs.setCompanyDatabase(companyList.get(selectedCompany));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
//                selectedCompany = companyList
            }
        });

        // Login button
        loginButton = (Button) findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                                            loginButton.performClick();
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
        });
        final GetCompanys downloader = new GetCompanys();
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
                                    loginButton.performClick();
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

    /**
     * Async task class to get json by making HTTP call
     */
    private class GetLoginResult extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();


            // Uncomment below for actual login
            return null;


//            // Making a request to url and getting response
//            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
//            queryParams.add(new BasicNameValuePair("username", un.getText().toString()));
//            queryParams.add(new BasicNameValuePair("password", pw.getText().toString()));
//            queryParams.add(new BasicNameValuePair("companyDatabase", selectedCompany));
//
//            url = gs.getInternetURL() + "checkCredentials.php";
//            String jsonStr = sh.makeServiceCall(url, ServiceHandler.POST, queryParams);
//
//            Log.d("Response: ", "> " + jsonStr);
//
//            if (jsonStr != null) {
//                try {
//                    JSONObject jsonObj = new JSONObject(jsonStr);
//                    // Getting JSON Array node
//                    data = jsonObj.getJSONArray("result");
//                    // There should only be one result
//                    JSONObject c = data.getJSONObject(0);
//                    String result = c.getString("success");
//                    String commonUserId = c.getString("commonUserId");
//                    loginSuccessFull = result.equals("1");
//                    gs.setSystemUserId(commonUserId);
//                    gs.setCalmToken(c.getString("calmToken"));
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            } else {
//                Log.e("ServiceHandler", "Couldn't get any data from the url");
//            }
//            return null;
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

            // For testing - bypass credential checking
            loginSuccessFull = true;


            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }
            if (loginSuccessFull) {
                // Create bundle to pass to activity
//                Bundle extras = new Bundle();
//                extras.putString("companyName", selectedCompany);
//                extras.putString("userName", un.getText().toString());
//                extras.putString("password", pw.getText().toString());

                Intent i = new Intent("android.intent.action.ShowGrid_Activity");
//                i.putExtras(extras);
                startActivityForResult(i, 1);
            } else {
                error.setText("Invalid username or password.");
            }
        }
    }

    /**
     * Async task class to get json by making HTTP call
     */
    private class GetCompanys extends AsyncTask<Void, Void, Void> {

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
        protected Void doInBackground(Void... arg0) {
            String url = GlobalState.getInternetURL() + "RequisitionJsons.php?functionName=getCompanies";
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();

            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
//            queryParams.add(new BasicNameValuePair("functionName", "getCompanies"));
            String jsonStr = sh.makeServiceCall(url, ServiceHandler.POST, null);

            Log.d("Response: ", "> " + jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);
                    data = jsonObj.getJSONArray("companies");
                    // Check for error
                    JSONObject jo = data.getJSONObject(0);
                    try {
                        String error = jo.getString("Error");
                        hasError = true;
                        ErrorMessage = "The following message occured while trying to retrieve a list of companies:\n " + error;
                        return null;
                    } catch (Exception e) {
                        // Intentially left blank
                    }
                    // looping through All Contacts
                    for (int i = 0; i < data.length(); i++) {
                        // Data node is JSON Object
                        jo = data.getJSONObject(i);
                        companyList.put(jo.getString("CompanyName"), jo.getInt("SapDatabaseId"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e("ServiceHandler", "Couldn't get any data from the url");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            // Dismiss the progress dialog
            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }
            if (hasError) {
                hasError = false;
                new AlertDialog.Builder(Procurement.this)
                        .setTitle("Error")
                        .setMessage(ErrorMessage)
                        .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                android.os.Process.killProcess(android.os.Process.myPid());
                                System.exit(0);
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
            ArrayList<String> l = new ArrayList<String>();
            for (Map.Entry<String, Integer> entry : companyList.entrySet()) {
                l.add(entry.getKey());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(Procurement.this, R.layout.spinner_item, l);
            mCompaniesSpinner.setAdapter(adapter);
        }
    }

    public void GetGCMRegId(){
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
                    Log.d("GCM",  msg);

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
}