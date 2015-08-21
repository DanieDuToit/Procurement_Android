package za.co.proteacoin.procurementandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.*;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Created by dutoitd1 on 2015/02/26.
 */
public class ShowDetail_Activity extends Activity implements View.OnClickListener {
    // for JSON
    private static final String TAG_SUPPLIERCARDCODE = "SupplierCardCode";
    private static final String TAG_SUPPLIERCARDNAME = "SupplierCardName";
    private static final String TAG_SUPPLIERCONTACTNAME = "CntctPrsn";
    private static final String TAG_SUPPLIERTELEPHONE = "Phone1";
    private static final String TAG_SUPPLIEREMAIL = "E_Mail";
    private static final String TAG_REQUISITIONCONTACTPERSONNAME = "RequisitionContactPersonName";
    private static final String TAG_REQUISITIONCONTACTNUMBER = "RequisitionContactNumber";
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_APPROVERS = "Approvers";
    final String TAG = "SHOWDETAIL_ACTIVITY";
    String[] mSaveOptions = {
            "Send to Procurement", // 3
            "Send for Approval", // 2
            "Reject Request" // 7
    };
    Spinner mSaveOptionsSpinner;
    Spinner mApproversSpinner;
    int selectedAction;
    String packageName;
    SQLiteDatabase database;
    LinearLayout approvalLayout;
    GlobalState gs;
    ImageButton bViewRequisitionLines;
    ImageButton bViewAttachedDocuments;
    Button approveIt;
    TextView tvSupp_name;
    TextView tvSupp_contact_name;
    TextView tvSupp_contact_telephone;
    TextView tvSupp_contact_email;
    TextView tvReq_contact;
    TextView tvReq_phone;
    EditText etComment;
    String SupplierCardCode;
    String SupplierCardName;
    String SupplierContactName;
    String SupplierTelephone;
    String SupplierEmail;
    String RequisitionContactPersonName;
    String RequisitionContactNumber;
    String documentNumber = "0";
    Hashtable<Integer, String> Approvers;
    ArrayList<String> approversList;
    ArrayList<Integer> approversIdList;
    // Requisition's JSONArray
    JSONArray data = null;
    JSONObject dataOBJ = null;
    private String comment;
    private int nextApproverId = 0;
    private int approvalAction = 0;
    private String url = "";
    private ProgressDialog pDialog;
    private boolean updateSuccessFull = false;
    private boolean hasError = false;
    private String ErrorMessage = "";

    // for JSON
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.showdetail);

        gs = (GlobalState) getApplication();
        if (getActionBar() != null) {
            getActionBar().setTitle("Requisition nr. " + gs.getRequisitionId());
        }

        Approvers = new Hashtable<Integer, String>();

        approversList = new ArrayList<String>();
        approveIt = (Button) findViewById(R.id.approveIt);
        packageName = getPackageName();

        approvalLayout = (LinearLayout) findViewById(R.id.approvalContainer);
        bViewRequisitionLines = (ImageButton) findViewById(R.id.bViewRequisitionLines);
        bViewAttachedDocuments = (ImageButton) findViewById(R.id.bViewAttachedDocuments);
        bViewAttachedDocuments.setOnClickListener(this);
        bViewRequisitionLines.setOnClickListener(this);

        tvSupp_name = (TextView) findViewById(R.id.supp_name);
        tvSupp_contact_name = (TextView) findViewById(R.id.supp_contact_name);
        tvSupp_contact_telephone = (TextView) findViewById(R.id.supp_contact_tel);
        tvSupp_contact_email = (TextView) findViewById(R.id.supp_contact_email);
        tvReq_contact = (TextView) findViewById(R.id.req_contact);
        tvReq_phone = (TextView) findViewById(R.id.req_phone);

        etComment = (EditText) findViewById(R.id.etComment);

        mApproversSpinner = new Spinner(ShowDetail_Activity.this);
        mApproversSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        // get the actual value of the selected approver name
                        String name = mApproversSpinner.getSelectedItem().toString();
                        // Now get the corresponding userId
                        for (Map.Entry<Integer, String> entry : Approvers.entrySet()) {
                            if (entry.getValue().equals(name)) {
                                nextApproverId = entry.getKey();
                                break;
                            }
                            // ...
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );

        // Retrieve the passed parameters
        //---get the Bundle object passed in---
        //		Bundle bundle = getIntent().getExtras();
        //---get the data using the getString()---
        //		final int documentNumber = bundle.getInt("documentNumber");
        mSaveOptionsSpinner = (Spinner) findViewById(R.id.approveOptions);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_item, mSaveOptions);
        mSaveOptionsSpinner.setAdapter(adapter);
        mSaveOptionsSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        int index = mSaveOptionsSpinner.getSelectedItemPosition();
//                        "Send to Procurement", // 3
//                        "Send for Approval", // 2
//                        "Reject Request" // 7
                        switch (index) {
                            case 0:
                                selectedAction = SaveActions.SENDTOPROCUREMENT.getNumericType();
                                break;
                            case 1:
                                selectedAction = SaveActions.SENDFORAPPROVAL.getNumericType();
                                break;
                            case 2:
                                selectedAction = SaveActions.REJECTREQUEST.getNumericType();
                                break;
                        }

                        // Add a dynamic spinner for the Approvers
                        if (position == 1) {
                            /**
                             * Updating parsed JSON data into Approvers ListView
                             * */
                            mApproversSpinner.setAdapter(new ArrayAdapter<String>(ShowDetail_Activity.this, android.R.layout.simple_spinner_dropdown_item, approversList));
                            approvalLayout.addView(mApproversSpinner, 0);
                        } else {
                            try {
                                approvalLayout.removeViewAt(0);
                            } catch (Exception e) {
                                // left blank
                            }
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        selectedAction = SaveActions.SENDTOPROCUREMENT.getNumericType();
                    }
                }

        );
        approveIt.setOnClickListener(this);
        documentNumber = gs.getRequisitionId();

        final GetRequisition downloader = new GetRequisition();
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
                    new AlertDialog.Builder(ShowDetail_Activity.this)
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

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.bViewAttachedDocuments:
                Intent iAttachedDocs = new Intent("android.intent.action.ShowAttachedDocuments_Activity");
                startActivity(iAttachedDocs);
                break;
            case R.id.bViewRequisitionLines:
                Intent iShowReqLines = new Intent("android.intent.action.ShowRequisitionLines_Activity");
                startActivity(iShowReqLines);
                break;
            case R.id.approveIt:
                comment = etComment.getText().toString();
                final UpdateApproval downloader = new UpdateApproval();
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
                            new AlertDialog.Builder(ShowDetail_Activity.this)
                                    .setTitle("Result")
                                    .setMessage("Internet connection timed out. Try again?")
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            approveIt.performClick();
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
                break;
        }
    }

    public enum SaveActions {
        SENDTOPROCUREMENT(3),
        SENDFORAPPROVAL(2),
        REJECTREQUEST(7);

        private int type;

        SaveActions(int i) {
            this.type = i;
        }

        public int getNumericType() {
            return type;
        }
    }

    /**
     * Async task class to get json by making HTTP call
     */
    private class GetRequisition extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            // Get Requisition detail
            url = GlobalState.INTERNET_URL + "RequisitionJsons.php?functionName=getRequisitionDetail";
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();

            // Making a request to url and getting response
            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
//            queryParams.add(new BasicNameValuePair("CurrentSapDatabaseId", gs.getCompanyDatabase().toString()));
            queryParams.add(new BasicNameValuePair("requisitionId", String.valueOf(gs.getRequisitionId())));
            String jsonStr = sh.makeServiceCall(url, ServiceHandler.POST, queryParams);

            Log.d("Response: ", "> " + jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    data = jsonObj.getJSONArray("requisitionDetail");

                    // Check for error
                    JSONObject jo = data.getJSONObject(0);
                    try {
                        String error = jo.getString("Error");
                        hasError = true;
                        ErrorMessage = "The following message occured while trying to retrieve a list of requisitions: \n" + error;
                        return null;
                    } catch (Exception e) {
                        // Intentially left blank
                    }

                    // Should only be one Requisition
                    // Data node is JSON Object
                    SupplierCardCode = jo.getString(TAG_SUPPLIERCARDCODE);
                    SupplierCardName = jo.getString(TAG_SUPPLIERCARDNAME);
                    SupplierContactName = jo.getString(TAG_SUPPLIERCONTACTNAME);
                    SupplierTelephone = jo.getString(TAG_SUPPLIERTELEPHONE);
                    SupplierEmail = jo.getString(TAG_SUPPLIEREMAIL);
                    RequisitionContactPersonName = jo.getString(TAG_REQUISITIONCONTACTPERSONNAME);
                    RequisitionContactNumber = jo.getString(TAG_REQUISITIONCONTACTNUMBER);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e("ServiceHandler", "Couldn't get any data from the url");
            }

            // ********************************************************************
            // Get Approvers List
            url = GlobalState.INTERNET_URL + "RequisitionJsons.php?functionName=getApprovers";
            // Creating service handler class instance
            sh = new ServiceHandler();

            // Making a request to url and getting response
            queryParams = new ArrayList<NameValuePair>();
            queryParams.add(new BasicNameValuePair("systemUserId", String.valueOf(gs.getCommonUserId())));
            jsonStr = sh.makeServiceCall(url, ServiceHandler.POST, queryParams);

            Log.d("Response: ", "> " + jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

//                    // Check if there was an error
//                    boolean response = jsonObj.getBoolean("responseOK");
//                    if (!response) {
//                        // Handle error
//                        String errorMsg = jsonObj.getString("responseMessage");
//                        new AlertDialog.Builder(ShowDetail_Activity.this)
//                                .setTitle("Error")
//                                .setMessage("The following eror occured while trying to retrieve the requisition detail: " + errorMsg)
//                                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog, int which) {
//                                        android.os.Process.killProcess(android.os.Process.myPid());
//                                        System.exit(1);
//                                    }
//                                })
//                                .setIcon(android.R.drawable.ic_dialog_alert)
//                                .show();
//
//                        return null;
//                    }
//
                    data = jsonObj.getJSONArray(TAG_APPROVERS);

                    // Check for error
                    JSONObject jo = data.getJSONObject(0);
                    try {
                        String error = jo.getString("Error");
                        hasError = true;
                        ErrorMessage = "The following message occured while trying to retrieve the requisition detail: \n" + error;
                        return null;
                    } catch (Exception e) {
                        // Intentially left blank
                    }

                    // looping through All Approvers
                    for (int i = 0; i < data.length(); i++) {
                        // Data node is JSON Object
                        JSONObject c = data.getJSONObject(i);

                        // get the first of the Id'd and put it in nextApproverId
                        if (i == 0)
                            nextApproverId = c.getInt("ApproverId");
                        String NextUser = c.getString("NextUser");
                        Integer approverId = c.getInt("ApproverId");
                        // adding approver to approvers list
                        Approvers.put(approverId, NextUser);
                        approversList.add(NextUser);
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
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(ShowDetail_Activity.this);
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
                new AlertDialog.Builder(ShowDetail_Activity.this)
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

            tvSupp_name.setText(SupplierCardName);
            tvSupp_contact_name.setText(SupplierContactName);
            tvSupp_contact_telephone.setText(SupplierTelephone);
            tvSupp_contact_email.setText(SupplierEmail);
            tvReq_contact.setText(RequisitionContactPersonName);
            tvReq_phone.setText(RequisitionContactNumber);

        }
    }

    /**
     * Async task class to get json by making HTTP call
     */

    private class UpdateApproval extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();

            url = GlobalState.INTERNET_URL + "RequisitionJsons.php?functionName=requsitionIterationsaveOption";

            // Making a request to url and getting response
            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
            queryParams.add(new BasicNameValuePair("requisitionId", gs.getRequisitionId()));
            queryParams.add(new BasicNameValuePair("iterationComments", comment));
            queryParams.add(new BasicNameValuePair("approvedBy", gs.getCommonUserId()));
            queryParams.add(new BasicNameValuePair("requisitionSaveOption", String.valueOf(selectedAction)));
            if (selectedAction == SaveActions.SENDFORAPPROVAL.getNumericType())
                queryParams.add(new BasicNameValuePair("requisitionApprover", String.valueOf(nextApproverId)));
            String jsonStr = sh.makeServiceCall(url, ServiceHandler.POST, queryParams);

            Log.d("Response: ", "> " + jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Check for error
                    JSONObject jo = data.getJSONObject(0);
                    try {
                        String error = jo.getString("Error");
                        hasError = true;
                        ErrorMessage = "The following message occured while trying to retrieve Attachment file names:: \n" + error;
                        updateSuccessFull = false;
                        return null;
                    } catch (Exception e) {
                        // Intentially left blank
                    }
                    updateSuccessFull = true;
                } catch (JSONException e) {
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
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Intent returnIntent = new Intent();
            // Return to ShowGrid_Activity
            if (updateSuccessFull) {
                if (selectedAction == SaveActions.SENDTOPROCUREMENT.getNumericType()) {
                    returnIntent.putExtra("result", "Transaction was successfully send to procurement");
                } else if (selectedAction == SaveActions.SENDFORAPPROVAL.getNumericType()) {
                    returnIntent.putExtra("result", "Transaction was successfully send for approval");
                } else if (selectedAction == SaveActions.REJECTREQUEST.getNumericType()) {
                    returnIntent.putExtra("result", "Transaction was rejected");
                } else {
                    returnIntent.putExtra("result", "Unknown result");
                }
            }
//            if (updateSuccessFull) {
//                switch (SaveActions.values()[selectedAction]) {
//                    case SENDTOPROCUREMENT:
//                        returnIntent.putExtra("result", "Transaction was successfully send to procurement");
//                        break;
//                    case SENDFORAPPROVAL:
//                        returnIntent.putExtra("result", "Transaction was successfully send for approval");
//                        break;
//                    case REJECTREQUEST:
//                        returnIntent.putExtra("result", "Transaction was rejected");
//                        break;
//                    default:
//                        returnIntent.putExtra("result", "Unknown result");
//                }
//            }
            setResult(RESULT_OK, returnIntent);
            finish();
        }
    }
}