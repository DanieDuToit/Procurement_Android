package za.co.proteacoin.procurementandroid;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.ArrayMap;
import android.util.Log;

public class DBAdapter {
	static final String KEY_ROWID = "_id";
	static final String KEY_NAME = "name";
	static final String KEY_EMAIL = "email";
	static final String TAG = "DBAdapter";
	static final String DATABASE_NAME = "MyDB";
	static final String DATABASE_REQUISITIONTABLE = "requisition";
	static final int DATABASE_VERSION = 1;
	static final String DATABASE_CREATE =
			  "create table requisition (" +
						 "_id integer primary key autoincrement, " +
						 "SupplierCardCode text, " +
						 "SupplierCardName text, " +
						 "SupplierContactName text, " +
						 "SupplierTelephone text, " +
						 "SupplierEmail text, " +
						 "RequisitionContactPersonName text, " +
						 "RequisitionContactNumber text)";
	public ArrayMap<String, String> requisitionMap = new ArrayMap<String, String>();

	final Context context;

	DatabaseHelper DBHelper;
	SQLiteDatabase db;

	public DBAdapter(Context ctx) {
		this.context = ctx;
		DBHelper = new DatabaseHelper(context);

		// Requisition
		requisitionMap.put("SupplierCardCode", "");
		requisitionMap.put("SupplierCardName", "");
		requisitionMap.put("SupplierContactName", "");
		requisitionMap.put("SupplierTelephone", "");
		requisitionMap.put("SupplierEmail", "");
		requisitionMap.put("RequisitionContactPersonName", "");
		requisitionMap.put("RequisitionContactNumber", "");
		insertRequisition(requisitionMap);
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			try {
				db.execSQL(DATABASE_CREATE);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					  + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS contacts");
			onCreate(db);
		}
	}

	//---opens the database---
	public DBAdapter open() throws SQLException {
		db = DBHelper.getWritableDatabase();
		return this;
	}

	//---closes the database---
	public void close() {
		DBHelper.close();
	}

	//---insert a requisition into the database---
	//	"SupplierCardCode
	//	"SupplierCardName
	//	"SupplierContactName
	//	"SupplierTelephone
	//	"SupplierEmail
	//	"RequisitionContactPersonName
	//	"RequisitionContactNumber
	public long insertRequisition(ArrayMap<String, String> requisition) {
		ContentValues initialValues = new ContentValues();
		for (int i = 0; i < requisition.size(); i++) {
			initialValues.put(requisition.keyAt(i), requisition.get(requisition.keyAt(i)));
		}
		return db.insert(DATABASE_REQUISITIONTABLE, null, initialValues);
	}

	//---deletes a particular contact---
	public boolean deleteRequisition(long rowId) {
		return db.delete(DATABASE_REQUISITIONTABLE, "_id = " + rowId, null) > 0;
	}

	//---retrieves all the contacts---
	public Cursor getRequisition() {
		return db.query(DATABASE_REQUISITIONTABLE, new String[]{KEY_ROWID, KEY_NAME,
		                                                        KEY_EMAIL}, null, null, null, null, null);
	}

	//---retrieves all the contacts---
	public int countRequisitions() {
		int count = 0;
		Cursor mCursor = db.rawQuery("Select count(_id) from " + DATABASE_REQUISITIONTABLE, new String[]{});
		if (mCursor != null) {
			count = mCursor.getCount();
		}
		return count;
	}

	//---retrieves a particular contact---
	public Cursor getContact(long rowId) throws SQLException {
		Cursor mCursor =
				  db.query(true, DATABASE_REQUISITIONTABLE, new String[]{KEY_ROWID,
				                                                         KEY_NAME, KEY_EMAIL}, KEY_ROWID + "=" + rowId, null,
							 null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	//---updates a contact---
	public boolean updateContact(long rowId, String name, String email) {
		ContentValues args = new ContentValues();
		args.put(KEY_NAME, name);
		args.put(KEY_EMAIL, email);
		return db.update(DATABASE_REQUISITIONTABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}
}

