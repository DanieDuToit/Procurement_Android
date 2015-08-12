package za.co.proteacoin.procurementandroid;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class GlobalState extends Application {

	private final Random random = new Random();
	private PowerManager.WakeLock wakeLock;
	private String requisitionId;
	private String requisitionDocumentId;
	private String companyName;
	private String companyCode;
	private Integer companyDatabaseId;

	private ArrayList<String> fileNames = new ArrayList<String>();
	private String currentFile;
	private String documentMimeType;
	private String systemUserId;
	private String uniqueDeviceId;
	private String gcmIdentification;
	private String ivKey;
	public static String PROJECT_ID = "564345734817";

	private static Context context;

	public void onCreate(){
		super.onCreate();
		context = getApplicationContext();
		uniqueDeviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
	}

	public static String getInternetURL() {
//		return "http://172.24.0.239/SAPWebXPHP/AjaxPages/";
		return "http://172.24.0.239:9001/SAPWebXPHP/Main/AjaxPages/";
	}

	public static Context getAppContext() {
		return GlobalState.context;
	}

	public String getUniqueDeviceId() {return uniqueDeviceId;}

	public String getIvKey() {
		return ivKey;
	}
	public void setIvKey(String ivKey) {
		this.ivKey = ivKey;
	}

	public String getGcmIdentification() {
		return gcmIdentification;
	}
	public void setGcmIdentification(String gcmIdentification) {
		this.gcmIdentification = gcmIdentification;
	}

	// TODO - Change the hardcoded userID
	public String getSystemUserId() {
//		return systemUserId;
		return "1256";
	}
	public void setSystemUserId(String systemUserId) {
		this.systemUserId = systemUserId;
	}

	public String getDocumentMimeType() {
		return documentMimeType;
	}
	public void setDocumentMimeType(String documentMimeType) {
		this.documentMimeType = documentMimeType;
	}

	public String getRequisitionDocumentId() {
		return requisitionDocumentId;
	}
	public void setRequisitionDocumentId(String requisitionDocumentId) {
		this.requisitionDocumentId = requisitionDocumentId;
	}

	public String getCurrentFile() {
		return currentFile;
	}
	public void setCurrentFile(String currentFile) {
		this.currentFile = currentFile;
	}

	public ArrayList<String> getFileNames() {
		return fileNames;
	}
	public void setFileName(String file) {
		int position = this.fileNames.indexOf(file);
		if (position < 0) {
			this.fileNames.add(file);
		}
	}

	public void removeFileName(String file) {
		int position = this.fileNames.indexOf(file);
		if (position > 0) {
			this.fileNames.remove(position);
		}
	}

	public void setCompanyCode(String companyCode) {
		this.companyCode = companyCode;
	}

	public String getCompanyName() {
		return companyName;
	}
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public Integer getCompanyDatabase() {
		return companyDatabaseId;
	}
	public void setCompanyDatabase(Integer companyDatabaseId) {
		this.companyDatabaseId = companyDatabaseId;
	}

	public String getRequisitionId() {
		return requisitionId;
	}
	public void setRequisitionId(String requisitionId) {
		this.requisitionId = requisitionId;
	}

	public String toDouble(Double doubleValue, boolean isMoney) {
		DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
		Locale locale = new Locale("en", "ZA");
		Currency currency = Currency.getInstance(locale);

		if (isMoney) {
			formatSymbols.setCurrency(currency);
		}
		formatSymbols.setDecimalSeparator('.');
		formatSymbols.setPatternSeparator(' ');
		String pattern = "0.00";
		DecimalFormat df = new DecimalFormat(pattern, formatSymbols);
		return df.format(doubleValue);
	}

	// Checking for all possible internet providers
	public boolean isConnectingToInternet() {

		ConnectivityManager connectivity =
				(ConnectivityManager) getSystemService(
						Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null)
				for (int i = 0; i < info.length; i++)
					if (info[i].getState() == NetworkInfo.State.CONNECTED) {
						return true;
					}

		}
		return false;
	}

	//Function to display simple Alert Dialog
	public void showAlertDialog(Context context, String title, String message,
								Boolean status) {
		AlertDialog alertDialog = new AlertDialog.Builder(context).create();

		// Set Dialog Title
		alertDialog.setTitle(title);

		// Set Dialog Message
		alertDialog.setMessage(message);

		if (status != null)
			// Set alert dialog icon
			alertDialog.setIcon((status) ? R.drawable.success : R.drawable.fail);

		// Set OK Button
		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {

			}
		});

		// Show Alert Message
		alertDialog.show();
	}

	// Notifies UI to display a message.
	void displayMessageOnScreen(Context context, String message) {

		Intent intent = new Intent(GCMConfig.DISPLAY_MESSAGE_ACTION);
		intent.putExtra(GCMConfig.EXTRA_MESSAGE, message);

		// Send Broadcast to Broadcast receiver with message
		context.sendBroadcast(intent);

	}

	// Unregister this account/device pair within the server.
	void unregister(final Context context, final String regId) {

		Log.i(GCMConfig.TAG, "unregistering device (regId = " + regId + ")");

		String serverUrl = getInternetURL() + "GCM_Register.php/unregister";
		Map<String, String> params = new HashMap<String, String>();
		params.put("regId", regId);

		try {
			post(serverUrl, params);
			GCMRegistrar.setRegisteredOnServer(context, false);
			String message = context.getString(R.string.server_unregistered);
			displayMessageOnScreen(context, message);
		} catch (IOException e) {

			// At this point the device is unregistered from GCM, but still
			// registered in the our server.
			// We could try to unregister again, but it is not necessary:
			// if the server tries to send a message to the device, it will get
			// a "NotRegistered" error message and should unregister the device.

			String message = context.getString(R.string.server_unregister_error,
					e.getMessage());
			displayMessageOnScreen(context, message);
		}
	}

	// Issue a POST request to the server.
	private static void post(String endpoint, Map<String, String> params)
			throws IOException {

		URL url;
		try {

			url = new URL(endpoint);

		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("invalid url: " + endpoint);
		}

		StringBuilder bodyBuilder = new StringBuilder();
		Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();

		// constructs the POST body using the parameters
		while (iterator.hasNext()) {
			Map.Entry<String, String> param = iterator.next();
			bodyBuilder.append(param.getKey()).append('=')
					.append(param.getValue());
			if (iterator.hasNext()) {
				bodyBuilder.append('&');
			}
		}

		String body = bodyBuilder.toString();

		Log.v(GCMConfig.TAG, "Posting '" + body + "' to " + url);

		byte[] bytes = body.getBytes();

		HttpURLConnection conn = null;
		try {

			Log.e("URL", "> " + url);

			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setFixedLengthStreamingMode(bytes.length);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded;charset=UTF-8");
			// post the request
			OutputStream out = conn.getOutputStream();
			out.write(bytes);
			out.close();

			// handle the response
			int status = conn.getResponseCode();

			// If response is not success
			if (status != 200) {

				throw new IOException("Post failed with error code " + status);
			}
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	public void acquireWakeLock(Context context) {
		if (wakeLock != null) wakeLock.release();

		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

		wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
				PowerManager.ACQUIRE_CAUSES_WAKEUP |
				PowerManager.ON_AFTER_RELEASE, "WakeLock");

		wakeLock.acquire();
	}

	public void releaseWakeLock() {
		if (wakeLock != null) wakeLock.release();
		wakeLock = null;
	}
}
