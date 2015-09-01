package za.co.proteacoin.procurementandroid;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ShowRequisitionLines_Activity extends ListActivity {
    private static final String TAG_LINENUMBER = "LineNumber";
    private static final String TAG_ITEMDESCRIPTION = "ItemDescription";
    private static final String TAG_ACCTNAME = "AcctName";
    private static final String TAG_QUANTITY = "Quantity";
    private static final String TAG_LINETOTAL = "LineTotal";
    private static final String TAG_DATA = "requisitionLines";
    final String TAG = "SHOWREQUISITIONLINES_ACTIVITY";
    // for JSON
    // Hashmap for ListView
    ArrayList<HashMap<String, String>> requisitionLinesList;
    // Requisition's JSONArray
    JSONArray data = null;
    private GlobalState gs;
    private double totalValue = 0.00;
    private boolean hasError = false;
    private String ErrorMessage = "";
    private ProgressDialog pDialog;
    // for JSON

    @Override
    protected void onResume() {
        super.onResume();
        GlobalState.setStartTime(SystemClock.uptimeMillis());
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        GlobalState.setStartTime(SystemClock.uptimeMillis());
    }

    @Override
    protected void onStart() {
        super.onStart();
        GlobalState.setStartTime(SystemClock.uptimeMillis());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActionBar() != null) {
            getActionBar().setTitle("Requisition Lines");
        }
        setContentView(R.layout.show_requisition_lines);
        gs = (GlobalState) getApplication();
        requisitionLinesList = new ArrayList<>();

        final GetRequisitionLines downloader = new GetRequisitionLines();
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
                    new AlertDialog.Builder(ShowRequisitionLines_Activity.this)
                            .setTitle("Result")
                            .setMessage("Internet connection timed out. Try again?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = getIntent();
                                    finish();
                                    startActivity(intent);
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
        }, 30000);
    }

    /**
     * Async task class to get json by making HTTP call
     */
    private class GetRequisitionLines extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            String url = GlobalState.INTERNET_URL + "RequisitionJsons.php?functionName=getRequisitionLines";
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();

            JSONObject obj = new JSONObject();
            try {
                obj.put("requisitionId", gs.getRequisitionId());
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

            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
            queryParams.add(new BasicNameValuePair("mobileDeviceId", String.valueOf(gs.getCalmDeviceId())));
            queryParams.add(new BasicNameValuePair("systemApplicationId", GlobalState.SYSTEM_APPLICATION_ID));
            queryParams.add(new BasicNameValuePair("encryptedPackage", encryptedString));

            String jsonStr = sh.makeServiceCall(url, ServiceHandler.POST, queryParams);


            if (jsonStr != null) {
                try {
                    byte[] decryptedJson = gs.decrypt(jsonStr);
                    JSONObject jsonObj = new JSONObject(new String(decryptedJson));

                    // Getting JSON Array node
                    JSONArray data = jsonObj.getJSONArray(TAG_DATA);

                    // Check for error
                    JSONObject jo = data.getJSONObject(0);
                    try {
                        String error = jo.getString("Error");
                        hasError = true;
                        ErrorMessage = "The following message occured while trying to retrieve requisition lines: \n" + error;
                        return null;
                    } catch (Exception e) {
                        // Intentially left blank
                    }

                    // looping through All Contacts
                    for (int i = 0; i < data.length(); i++) {
                        // Data node is JSON Object
                        JSONObject c = data.getJSONObject(i);
                        String lineNUmber = c.getString(TAG_LINENUMBER);
                        String itemDescription = c.getString(TAG_ITEMDESCRIPTION);
                        String acctCode = c.getString(TAG_ACCTNAME);
                        String quantity = c.getString(TAG_QUANTITY);
                        String lineTotal = c.getString(TAG_LINETOTAL);

                        // tmp hashmap for single data object
                        HashMap<String, String> hm = new HashMap<>();

                        // adding each child node to HashMap key => value
                        hm.put(TAG_LINENUMBER, lineNUmber);
                        hm.put(TAG_ITEMDESCRIPTION, itemDescription);
                        hm.put(TAG_ACCTNAME, acctCode);

                        String pattern = "#,###.00";
                        DecimalFormat decimalFormat = new DecimalFormat(pattern);
                        double dbl;
                        try {
                            dbl = Double.valueOf(lineTotal);
                            lineTotal = decimalFormat.format(dbl);
                            totalValue += dbl;

                            dbl = Double.valueOf(quantity);
                            quantity = decimalFormat.format(dbl);
                        } catch (Exception e) {
                            continue;
                        }
                        hm.put(TAG_LINETOTAL, lineTotal);
                        hm.put(TAG_QUANTITY, quantity);


                        // adding requisition line to requisitionLinesList
                        requisitionLinesList.add(hm);
                    }
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
            pDialog = new ProgressDialog(ShowRequisitionLines_Activity.this);
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

            if (hasError) {
                hasError = false;
                new AlertDialog.Builder(ShowRequisitionLines_Activity.this)
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
            } else {

                /**
                 * Updating parsed JSON data into ListView
                 * */
                ListAdapter adapter = new SimpleAdapter(
                        ShowRequisitionLines_Activity.this, requisitionLinesList,
                        R.layout.requisition_line,
                        new String[]{TAG_LINENUMBER,
                                TAG_ITEMDESCRIPTION,
                                TAG_ACCTNAME,
                                TAG_QUANTITY,
                                TAG_LINETOTAL},
                        new int[]{R.id.tvLineNumber,
                                R.id.tvItemDescription,
                                R.id.tvAccCode,
                                R.id.tvQuantity,
                                R.id.tvLineTotal});

                setListAdapter(adapter);
                if (getActionBar() != null) {
                    String pattern = "#,###.00";
                    DecimalFormat decimalFormat = new DecimalFormat(pattern);
                    String tv = decimalFormat.format(totalValue);

                    getActionBar().setTitle("Requisition #" + gs.getRequisitionId() + " TOTAL: R" + tv);
                }
            }
        }
    }
}