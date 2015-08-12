package za.co.proteacoin.procurementandroid;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
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
public class ShowRequisitionLines_Activity extends ListActivity {
	final String TAG = "SHOWREQUISITIONLINES_ACTIVITY";
	private GlobalState gs;
	private double totalValue = 0.00;
	ListView lv;

	// for JSON
	// Hashmap for ListView
	ArrayList<HashMap<String, String>> requisitionLinesList;
	private static final String TAG_LINENUMBER = "LineNumber";
	private static final String TAG_ITEMDESCRIPTION = "ItemDescription";
	private static final String TAG_ACCTCODE = "AcctCode";
	private static final String TAG_QUANTITY = "Quantity";
	private static final String TAG_LINETOTAL = "LineTotal";
	private static final String TAG_DATA = "requisitionLines";
	// Requisition's JSONArray
	JSONArray data = null;
	private ProgressDialog pDialog;
	// for JSON

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getActionBar() != null) {
			getActionBar().setTitle("Requisition Lines");
		}
		setContentView(R.layout.show_requisition_lines);
		gs = (GlobalState) getApplication();
		requisitionLinesList = new ArrayList<HashMap<String, String>>();

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
			String url = gs.getInternetURL() + "RequisitionJsons.php?functionName=getRequisitionLines";
			// Creating service handler class instance
			ServiceHandler sh = new ServiceHandler();

			List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
			queryParams.add(new BasicNameValuePair("requisitionId", gs.getRequisitionId()));

			String jsonStr = sh.makeServiceCall(url, ServiceHandler.POST, queryParams);

			Log.d("Response: ", "> " + jsonStr);

			if (jsonStr != null) {
				try {
					JSONObject jsonObj = new JSONObject(jsonStr);

					// Check if there was an error
					boolean response = jsonObj.getBoolean("responseOK");
					if (!response) {
						// Handle error
						String errorMsg = jsonObj.getString("responseMessage");
						new AlertDialog.Builder(ShowRequisitionLines_Activity.this)
								.setTitle("Error")
								.setMessage("The following message occured while trying to retrieve the requisition detail: " + errorMsg)
								.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										android.os.Process.killProcess(android.os.Process.myPid());
										System.exit(1);
									}
								})
								.setIcon(android.R.drawable.ic_dialog_alert)
								.show();

						return null;
					}

					// Getting JSON Array node
					data = jsonObj.getJSONArray(TAG_DATA);

					// looping through All Contacts
					for (int i = 0; i < data.length(); i++) {
						// Data node is JSON Object
						JSONObject c = data.getJSONObject(i);

						String lineNUmber = c.getString(TAG_LINENUMBER);
						String itemDescription = c.getString(TAG_ITEMDESCRIPTION);
						String acctCode = c.getString(TAG_ACCTCODE);
						String quantity = c.getString(TAG_QUANTITY);
						String lineTotal = c.getString(TAG_LINETOTAL);

						// tmp hashmap for single data object
						HashMap<String, String> data = new HashMap<String, String>();

						// adding each child node to HashMap key => value
						data.put(TAG_LINENUMBER, lineNUmber);
						data.put(TAG_ITEMDESCRIPTION, itemDescription);
						data.put(TAG_ACCTCODE, acctCode);
						double dbl = Double.valueOf(quantity);
						data.put(TAG_QUANTITY, gs.toDouble(dbl, false));
						try {
							dbl = Double.valueOf(lineTotal);
						} catch (Exception e) {
							continue;
						}
						data.put(TAG_LINETOTAL, gs.toDouble(dbl, true));

						totalValue += dbl;

						// adding requisition line to requisitionLinesList
						requisitionLinesList.add(data);
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
			/**
			 * Updating parsed JSON data into ListView
			 * */
			ListAdapter adapter = new SimpleAdapter(
					  ShowRequisitionLines_Activity.this, requisitionLinesList,
					  R.layout.requisition_line,
					  new String[]{TAG_LINENUMBER,
					               TAG_ITEMDESCRIPTION,
					               TAG_ACCTCODE,
					               TAG_QUANTITY,
					               TAG_LINETOTAL},
					  new int[]{R.id.tvLineNumber,
					            R.id.tvItemDescription,
					            R.id.tvAccCode,
					            R.id.tvQuantity,
					            R.id.tvLineTotal});

			setListAdapter(adapter);
			if (getActionBar() != null) {
				getActionBar().setTitle("Requisition #" + gs.getRequisitionId() + " TOTAL: " + gs.toDouble(totalValue, true));
			}
		}
	}
}
