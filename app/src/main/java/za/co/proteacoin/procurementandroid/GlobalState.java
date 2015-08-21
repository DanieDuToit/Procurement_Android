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
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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
	private String commonUserId;
	private String domainId;
	private String uniqueDeviceId;
	private String gcmIdentification;
	private String ivKey;
	private int CalmDeviceId;
	private Cipher cipher;
	private IvParameterSpec ivspec;
	private SecretKeySpec keyspec;
	private String userFirstName;
	private String userSurName;

	public final static String PROJECT_ID = "564345734817";
	public final static String SYSTEM_APPLICATION_ID = "3";
//	public final static String INTERNET_URL = "http://172.24.0.239:9001/SAPWebXPHP/Main/AjaxPages/";
	public final static String INTERNET_URL = "http://172.24.1.221/SAPWebXPHP/AjaxPages/";
	public final static IvParameterSpec IV_KEY = new IvParameterSpec(("23342DFA23342DFA").getBytes());
	public final static SecretKeySpec CALM_APPLICATION__KEY = new SecretKeySpec(("8qxRdXT169oH77r8").getBytes(), "AES");

	private static Context context;

	public void onCreate(){
		super.onCreate();
		context = getApplicationContext();
		uniqueDeviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
		try {
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

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
	public String getCommonUserId() {
		return commonUserId;
	}
	public void setCommonUserId(String commonUserId) {
		this.commonUserId = commonUserId;
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

	public void setFileName(String file) {
		int position = this.fileNames.indexOf(file);
		if (position < 0) {
			this.fileNames.add(file);
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
		// TODO remve hardcoded Id
//		return companyDatabaseId;
		return 2;
	}
	public void setCompanyDatabase(Integer companyDatabaseId) {
		this.companyDatabaseId = companyDatabaseId;
	}

	public String getDomainId() {
		return domainId;
	}

	public void setDomainId(String domainId) {
		this.domainId = domainId;
	}

	public String getRequisitionId() {
		return requisitionId;
	}
	public void setRequisitionId(String requisitionId) {
		this.requisitionId = requisitionId;
	}

	public int getCalmDeviceId() {
		return CalmDeviceId;
	}
	public void setCalmDeviceId(int calmDeviceId) {
		this.CalmDeviceId = calmDeviceId;
	}

	public String getUserFirstName() {
		return userFirstName;
	}
	public void setUserFirstName(String userFirstName) {
		this.userFirstName = userFirstName;
	}

	public String getUserSurName() {
		return userSurName;
	}
	public void setUserSurName(String userSurName) {
		this.userSurName = userSurName;
	}
	// End Getter & Setters

	public void removeFileName(String file) {
		int position = this.fileNames.indexOf(file);
		if (position > 0) {
			this.fileNames.remove(position);
		}
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

		String serverUrl = INTERNET_URL + "GCM_Register.php/unregister";
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
	public byte[] encrypt(String text) throws Exception {
		if (text == null || text.length() == 0) {
			throw new Exception("Empty string");
		}
		byte[] encrypted = null;
		try {
			cipher.init(Cipher.ENCRYPT_MODE, CALM_APPLICATION__KEY, IV_KEY);
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
			cipher.init(Cipher.DECRYPT_MODE, CALM_APPLICATION__KEY, IV_KEY);
			decrypted = cipher.doFinal(hexToBytes(code));
		} catch (Exception e) {
			throw new Exception("[decrypt] " + e.getMessage());
		}
		return decrypted;
	}

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
}
