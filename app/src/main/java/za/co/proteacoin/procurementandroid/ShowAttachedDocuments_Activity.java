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
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by dutoitd1 on 2015/03/13.
 */
public class ShowAttachedDocuments_Activity extends ListActivity {
    private static final String TAG_ATTACHEDDOCUMENTNAME = "DocumentFileName";
    private static final String TAG_REQUISITIONDOCUMENTID = "RequisitionDocumentId";
    private static final String TAG_DOCUMENTMIMETYPE = "DocumentMimeType";
    private static final String TAG_DATA = "requisitionDocuments";
    // for JSON
    // Hashmap for ListView
    ArrayList<HashMap<String, String>> attachedDocumentsList;
    // Requisition's JSONArray
    JSONArray data = null;
    private GlobalState gs;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_attached_document_lines);

        gs = (GlobalState) getApplication();
        attachedDocumentsList = new ArrayList<HashMap<String, String>>();

        final GetAttachedDocumentsLines downloader = new GetAttachedDocumentsLines();
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
                    new AlertDialog.Builder(ShowAttachedDocuments_Activity.this)
                            .setTitle("Result")
                            .setMessage("Internet connection timed out.")
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

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        TextView tvAttachedDocument = (TextView) v.findViewById(R.id.tvAttachedDocument);
        if (tvAttachedDocument.getText().toString().toLowerCase().contains("no documents were found")) {
            return;
        }
        TextView tvRequisitionDocumentId = (TextView) v.findViewById(R.id.tvRequisitionDocumentId);
        TextView tvDocumentMimeType = (TextView) v.findViewById(R.id.tvDocumentMimeType);

        String AttachedDocument = tvAttachedDocument.getText().toString();
        String RequisitionDocumentId = tvRequisitionDocumentId.getText().toString();
        String DocumentMimeType = tvDocumentMimeType.getText().toString();

        gs.setRequisitionDocumentId(RequisitionDocumentId);
        gs.setCurrentFile(AttachedDocument);
        gs.setDocumentMimeType(DocumentMimeType);

        Intent i = new Intent("android.intent.action.DisplayRemoteFile");
        startActivity(i);
    }

    /**
     * Async task class to get json by making HTTP call
     */
    private class GetAttachedDocumentsLines extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            String url = GlobalState.INTERNET_URL + "RequisitionJsons.php?functionName=getAttachedDocuments";
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
                    data = jsonObj.getJSONArray(TAG_DATA);

                    // Check for error
                    JSONObject jo = data.getJSONObject(0);
                    try {
                        String error = jo.getString("Error");
                        hasError = true;
                        ErrorMessage = "The following message occured while trying to retrieve the list of attached documents: \n" + error;
                        return null;
                    } catch (Exception e) {
                        // Intentially left blank
                    }

                    // looping through All Contacts
                    for (int i = 0; i < data.length(); i++) {
                        // Data node is JSON Object
                        JSONObject c = data.getJSONObject(i);

                        String documentName = c.getString(TAG_ATTACHEDDOCUMENTNAME) + "." + c.getString("DocumentExtension");
                        String requisitionDocumentId = c.getString(TAG_REQUISITIONDOCUMENTID);
                        String DocumentMimeType = c.getString(TAG_DOCUMENTMIMETYPE);

                        // tmp hashmap for single data object
                        HashMap<String, String> hm = new HashMap<String, String>();

                        // adding each child node to HashMap key => value
                        hm.put(TAG_ATTACHEDDOCUMENTNAME, documentName);
                        hm.put(TAG_REQUISITIONDOCUMENTID, requisitionDocumentId);
                        hm.put(TAG_DOCUMENTMIMETYPE, DocumentMimeType);

                        // adding line to attachedDocumentsList
                        attachedDocumentsList.add(hm);
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
            pDialog = new ProgressDialog(ShowAttachedDocuments_Activity.this);
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
                new AlertDialog.Builder(ShowAttachedDocuments_Activity.this)
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
                        ShowAttachedDocuments_Activity.this, attachedDocumentsList,
                        R.layout.attached_document_line,
                        new String[]{
                                TAG_ATTACHEDDOCUMENTNAME,
                                TAG_REQUISITIONDOCUMENTID,
                                TAG_DOCUMENTMIMETYPE
                        },
                        new int[]{
                                R.id.tvAttachedDocument,
                                R.id.tvRequisitionDocumentId,
                                R.id.tvDocumentMimeType
                        }
                );

                setListAdapter(adapter);
            }
        }
    }
}
