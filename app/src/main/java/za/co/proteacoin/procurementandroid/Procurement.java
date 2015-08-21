package za.co.proteacoin.procurementandroid;

import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;


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
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class Procurement extends Activity {


    private final String TAG = "MAIN";
    private LinearLayout companySelectorContainer;
    private boolean loginSuccessFull = false;
    private SharedPreferences sharedPref;
    GoogleCloudMessaging gcm;
    private int numberrOfSecurityIDViews = -1;
    // Hashmap for ListView
    private ArrayList<HashMap<String, String>> resultList;
    // Requisition's JSONArray
    private JSONArray data = null;
    private Button loginButton;
    private Spinner mCompaniesSpinner;
    private Spinner mApproversSpinner;
    private String selectedCompany;
    private EditText un, pw, dn;
    private TextView error;
    private HashMap<String, Integer> companyList;
    private AssetManager assets;
    private String packageName;
    private GlobalState gs;
    private ArrayList<String> SystemApplicationDatabaseIdList;
    private ArrayList<String> CompanyCodeNameList;
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
    private String username;
    private String password;
    private String domainName;
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

    public static String bytesToHex(byte[] data) {
        if (data == null) {
            return null;
        }
        int len = data.length;
        String str = "";
        for (int i = 0; i < len; i++) {
            if ((data[i] & 0xFF) < 16) {
                str = str + "0" + java.lang.Integer.toHexString(data[i] & 0xFF);
            } else {
                str = str + java.lang.Integer.toHexString(data[i] & 0xFF);
            }
        }
        return str;
    }

    public static byte[] hexToBytes(String str) {
        if (str == null) {
            return null;
        } else if (str.length() < 2) {
            return null;
        } else {
            int len = str.length() / 2;
            byte[] buffer = new byte[len];
            for (int i = 0; i < len; i++) {
                buffer[i] = (byte) Integer.parseInt(str.substring(i * 2, i * 2 + 2), 16);
            }
            return buffer;
        }
    }

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

        setContentView(R.layout.main);

        gs = (GlobalState) getApplication();

        companySelectorContainer = (LinearLayout) findViewById(R.id.companySelectorContainer);

        SystemApplicationDatabaseIdList = new ArrayList<>();
        CompanyCodeNameList = new ArrayList<>();

        // Get settings that is saved to Preferences file
        sharedPref = Procurement.this.getPreferences(Context.MODE_PRIVATE);
        Context ctx = Procurement.this;
        gs.setIvKey(sharedPref.getString("IVKey", ""));
        gs.setGcmIdentification(sharedPref.getString("GCMIdentification", ""));
        gs.setCalmDeviceId(sharedPref.getInt("SAPDeviceId", 0));
        final String uniqueDeviceID = sharedPref.getString("UniqueDeviceID", "");
        numberrOfSecurityIDViews = sharedPref.getInt("NumberrOfSecurityIDViews", 0);

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

        companyList = new HashMap<String, Integer>();

        assets = getAssets();
        packageName = getPackageName();
        //        db = new DBAdapter(this, assets, packageName);
        un = (EditText) findViewById(R.id.userName);
        pw = (EditText) findViewById(R.id.password);
        dn = (EditText) findViewById(R.id.domainName);
        error = (TextView) findViewById(R.id.tv_error);
        error.setText("");

//        companySelectorContainer.removeViewAt(0);

        // Login button
        loginButton = (Button) findViewById(R.id.loginButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (gs.getCalmDeviceId() == 0) {
                    gs.showAlertDialog(Procurement.this, "Registration", "You must first register your device.", false);

                } else {
                    username = un.getText().toString();
                    password = pw.getText().toString();
                    calmDeviceId = gs.getCalmDeviceId();
                    domainName = dn.getText().toString();

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
            }
        });

//        final GetCompanys downloader = new GetCompanys();
//        downloader.execute();
//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (downloader.getStatus() == AsyncTask.Status.RUNNING) {
//                    downloader.cancel(true);
//                    if (pDialog.isShowing()) {
//                        pDialog.dismiss();
//                    }
//                    new AlertDialog.Builder(Procurement.this)
//                            .setTitle("Result")
//                            .setMessage("Internet connection timed out. Try again?")
//                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int which) {
//                                    loginButton.performClick();
//                                }
//                            })
//                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int which) {
//                                    android.os.Process.killProcess(android.os.Process.myPid());
//                                    System.exit(1);
//                                }
//                            })
//                            .setIcon(android.R.drawable.ic_dialog_alert)
//                            .show();
//                }
//            }
//        }, 60000);
    }
//    public static String encrypt(String valueToEnc) throws Exception {
//        Key key = generateKey();
//        Cipher c = Cipher.getInstance("UTF-8");
//        c.init(Cipher.ENCRYPT_MODE, key);
//        byte[] encValue = c.doFinal(valueToEnc.getBytes());
//        String encryptedValue = Base64.encodeToString(encValue, Base64.DEFAULT);
//        return encryptedValue;
//    }
//
//    public static String decrypt(String encryptedValue) throws Exception {
//        Key key = generateKey();
//        Cipher c = Cipher.getInstance("UTF-8");
//        c.init(Cipher.DECRYPT_MODE, key);
//        byte[] decordedValue = Base64.decode(encryptedValue, Base64.DEFAULT);
//        byte[] decValue = c.doFinal(decordedValue);
//        String decryptedValue = new String(decValue);
//        return decryptedValue;
//    }
//
//    private static Key generateKey() throws Exception {
//        Key key = new SecretKeySpec(commonKey, "UTF-8");
//        // SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
//        // key = keyFactory.generateSecret(new DESKeySpec(keyValue));
//        return key;
//    }

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
            String url = GlobalState.INTERNET_URL + "RequisitionJsons.php?functionName=getCompanies";
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

    public byte[] encrypt(String text) throws Exception {
        if (text == null || text.length() == 0) {
            throw new Exception("Empty string");
        }
        byte[] encrypted = null;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, GlobalState.CALM_APPLICATION__KEY, GlobalState.IV_KEY);
            encrypted = cipher.doFinal(text.getBytes("UTF-8"));
        } catch (Exception e) {
            throw new Exception("[encrypt] " + e.getMessage());
        }
        return encrypted;
    }

    public byte[] decrypt(String code) throws Exception {
        if (code == null || code.length() == 0) {
            throw new Exception("Empty string");
        }
        byte[] decrypted = null;
        try {
            cipher.init(Cipher.DECRYPT_MODE, GlobalState.CALM_APPLICATION__KEY, GlobalState.IV_KEY);
            decrypted = cipher.doFinal(hexToBytes(code));
        } catch (Exception e) {
            throw new Exception("[decrypt] " + e.getMessage());
        }
        return decrypted;
    }

    /**
     * Async task class to get json by making HTTP call
     */
    private class GetLoginResult extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();

            try {
                cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            byte[] byteArray = {0x0};

            // TODO - Remove hardcoded values
            domainName = "pcg";
            username = "dutoitd1";
            password = "D@n13August";
            String source = "{\"DomainName\":\"" + domainName + "\",\"Username\":\"" + username + "\",\"Password\":\"" + password + "\"}";
            String encryptedString = "";

            try {
                encryptedString = Procurement.bytesToHex(encrypt(source));
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Making a request to url and getting response
            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();

            queryParams.add(new BasicNameValuePair("mobileDeviceId", String.valueOf(gs.getCalmDeviceId())));
            queryParams.add(new BasicNameValuePair("systemApplicationId", GlobalState.SYSTEM_APPLICATION_ID));
            queryParams.add(new BasicNameValuePair("encryptedPackage", encryptedString));

            url = "http://172.24.1.221/CALM/api/mobileApplicationLogin.php";
            String jsonStr = sh.makeServiceCall(url, ServiceHandler.POST, queryParams);

            Log.d("Response: ", "> " + jsonStr);

            // Decrypt jsonStr
            String decryptedString;
            if (jsonStr != null) {
                try {
                    decryptedString = new String(decrypt(jsonStr));
                    decryptedString = URLDecoder.decode(decryptedString, "UTF-8");

                    byte[] decryptedJson = gs.decrypt(jsonStr);
                    JSONObject jsonObj = new JSONObject(new String(decryptedJson));
                    // Check for error
                    data = jsonObj.getJSONArray("results");
                    jsonObj = data.getJSONObject(0);
                    try {
                        String error = jsonObj.getString("Error");
                        hasError = true;
                        ErrorMessage = "The following message occured while trying to login with the credentials given: \n" + error;
                        return null;
                    } catch (Exception e) {
                        // Intentially left blank
                    }
                    // Save the username
                    // Get settings from the Preferences file
                    sharedPref = Procurement.this.getPreferences(Context.MODE_PRIVATE);
                    // Create editor for editing Preferences
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("username", username);
                    editor.apply();

                    String firstName = jsonObj.getString("FirstName");
                    String surName = jsonObj.getString("Surname");
                    gs.setCommonUserId(jsonObj.getString("CommonUserId"));
                    // looping through All Companies
                    companiesTable = new Hashtable();
                    JSONArray companies = jsonObj.getJSONArray("Companies");
                    for (int i = 0; i < companies.length(); i++) {
                        JSONObject c = companies.getJSONObject(i);
                        String key = c.getString("SystemApplicationDatabaseId");
                        String value = c.getString("CompanyCodeName");
                        SystemApplicationDatabaseIdList.add(i, key);
                        CompanyCodeNameList.add(i, value);
                    }
                    gs.setUserFirstName(firstName);
                    gs.setUserSurName(surName);

                    loginSuccessFull = true;

                } catch (Exception e) {
                    e.printStackTrace();
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

//            // For testing - bypass credential checking
//            loginSuccessFull = true;
//
//
            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }
            if (loginSuccessFull) {
                // Create bundle to pass to activity
//                Bundle extras = new Bundle();
//                extras.putString("companyName", selectedCompany);
//                extras.putString("userName", un.getText().toString());
//                extras.putString("password", pw.getText().toString());

                mApproversSpinner = new Spinner(Procurement.this);
                mApproversSpinner.setOnItemSelectedListener(
                        new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                // get the actual value of the selected approver name
                                String name = mApproversSpinner.getSelectedItem().toString();
                                // Now get the corresponding userId
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                            }
                        }
                );

                mApproversSpinner.setAdapter(new ArrayAdapter<String>(Procurement.this, android.R.layout.simple_spinner_dropdown_item, CompanyCodeNameList));
                companySelectorContainer.addView(mApproversSpinner, 0);

//                Intent i = new Intent("android.intent.action.ShowGrid_Activity");
////                i.putExtras(extras);
//                startActivityForResult(i, 1);
            } else {
                error.setText("Invalid username or password.");
            }
        }
    }
}