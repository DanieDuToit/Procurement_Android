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
import android.view.View;
import android.widget.*;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Created by dutoitd1 on 2015/02/23.
 */
public class ShowGrid_Activity extends ListActivity implements View.OnClickListener {
	// for JSON
	private static final String TAG_REQUISITIONID = "RequisitionId";
	private static final String TAG_SUPPLIERCARDCODE = "SupplierCardCode";
	private static final String TAG_SUPPLIERCARDNAME = "SupplierCardName";
	private String url = "";
	private static final String TAG_DATA = "requisitions";
	// Hashmap for ListView
	ArrayList<HashMap<String, String>> requisitionList;
	// Requisition's JSONArray
	JSONArray data = null;
	private ProgressDialog pDialog;
	// for JSON

	private ListView lv;
	private static final String TAG = "SHOWGRID_ACTIVITY";

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btnFilter:
				requisitionList.clear();
				final GetRequisitions downloader = new GetRequisitions();
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
							new AlertDialog.Builder(ShowGrid_Activity.this)
									  .setTitle("Result")
									  .setMessage("Internet connection timed out. Try again?")
									  .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
										  public void onClick(DialogInterface dialog, int which) {
											  btnFilter.performClick();
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
			default:
				break;
		}
	}

	GlobalState gs;
	Button btnFilter;
	EditText etFilter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.showgrid);

		gs = (GlobalState) getApplication();
		if (getActionBar() != null) {
			getActionBar().setTitle("Requisitions");
		}
		btnFilter = (Button) findViewById(R.id.btnFilter);
		etFilter = (EditText) findViewById(R.id.etFilter);
		btnFilter.setOnClickListener(this);

		requisitionList = new ArrayList<HashMap<String, String>>();
		lv = getListView();

		// Retrieve the passed parameters
		//---get the Bundle object passed in---
//		Bundle bundle = getIntent().getExtras();
		//---get the data using the getString()---

//		String userName = bundle.getString("userName");
//		String password = bundle.getString("password");
//		String companyName = bundle.getString("companyName");

		// Listview on item click listener
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
			                        int position, long id) {
				// getting values from selected ListItem
				String RequisitionId = ((TextView) view.findViewById(R.id.tvRequisitionId))
						  .getText().toString();
				String SupplierCardCode = ((TextView) view.findViewById(R.id.tvSupplierCardCode))
						  .getText().toString();
				String SupplierCardName = ((TextView) view.findViewById(R.id.tvSupplierCardName))
						  .getText().toString();

				gs.setRequisitionId(RequisitionId);
				gs.setCompanyName(SupplierCardName);
				gs.setCompanyCode(SupplierCardCode);

				// Starting single contact activity
				Intent in = new Intent(getApplicationContext(), ShowDetail_Activity.class);
				in.putExtra(TAG_REQUISITIONID, RequisitionId);
				in.putExtra(TAG_SUPPLIERCARDCODE, SupplierCardCode);
				in.putExtra(TAG_SUPPLIERCARDNAME, SupplierCardName);
				startActivityForResult(in, 1);
			}
		});

		final GetRequisitions downloader = new GetRequisitions();
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
					new AlertDialog.Builder(ShowGrid_Activity.this)
							  .setTitle("Result")
							  .setMessage("Internet connection timed out. Try again?")
							  .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
								  public void onClick(DialogInterface dialog, int which) {
									  lv.performClick();
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

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			if (data == null) {
				return;
			}
			String result = data.getStringExtra("result");
			new AlertDialog.Builder(ShowGrid_Activity.this)
					  .setTitle("Result")
					  .setMessage(result)
					  .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						  public void onClick(DialogInterface dialog, int which) {
							  // do nothing
						  }
					  })
					  .setIcon(android.R.drawable.ic_dialog_alert)
					  .show();
			if (resultCode == RESULT_OK) {
				// do nothing
			}
			if (resultCode == RESULT_CANCELED) {
				//Write your code if there's no result
			}
		}
		btnFilter.callOnClick();
	}//onActivityResult

	/**
	 * Async task class to get json by making HTTP call
	 */
	private class GetRequisitions extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// Showing progress dialog
			pDialog = new ProgressDialog(ShowGrid_Activity.this);
			pDialog.setMessage("Please wait...");
			pDialog.setCancelable(true);
			pDialog.show();

		}

		@Override
		protected Void doInBackground(Void... arg0) {
			url = gs.getInternetURL() + "RequisitionJsons.php?functionName=getRequisitions";
//			url = gs.getInternetURL() + "GetRequisitions.php";
			// Creating service handler class instance
			ServiceHandler sh = new ServiceHandler();

			List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
			queryParams.add(new BasicNameValuePair("filter", etFilter.getText().toString()));
			queryParams.add(new BasicNameValuePair("CurrentSapDatabaseId", gs.getCompanyDatabase().toString()));

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
						new AlertDialog.Builder(ShowGrid_Activity.this)
								.setTitle("Error")
								.setMessage("The following message occured while trying to retrieve a list of requisitions: " + errorMsg)
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
						String RequisitionId = c.getString(TAG_REQUISITIONID);
						String SupplierCardCode = c.getString(TAG_SUPPLIERCARDCODE);
						String SupplierCardName = c.getString(TAG_SUPPLIERCARDNAME);
						// tmp hashmap for single data object
						HashMap<String, String> data = new HashMap<String, String>();
						// adding each child node to HashMap key => value
						data.put(TAG_REQUISITIONID, RequisitionId);
						data.put(TAG_SUPPLIERCARDCODE, SupplierCardCode);
						data.put(TAG_SUPPLIERCARDNAME, SupplierCardName);

						// adding contact to contact list
						requisitionList.add(data);
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
			/**
			 * Updating parsed JSON data into ListView
			 * */
			ListAdapter adapter = new SimpleAdapter(
					  ShowGrid_Activity.this, requisitionList,
					  R.layout.requisition,
					  new String[]{
								 TAG_REQUISITIONID,
								 TAG_SUPPLIERCARDCODE,
								 TAG_SUPPLIERCARDNAME
					  },
					  new int[]{
								 R.id.tvRequisitionId,
								 R.id.tvSupplierCardCode,
								 R.id.tvSupplierCardName
					  }
			);

			setListAdapter(adapter);
		}

	}
}
