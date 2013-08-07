package org.projectproto.yuscope;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Simple settings database access helper class. Defines the basic CRUD operations
 * and gives the ability to list all settings as well as
 * retrieve or modify a specific setting.
 */
public class DbAdapter {

    public static final String KEY_FIELD = "key";
    public static final String VALUE_FIELD = "value";
    public static final String ID_FIELD = "_id";

    private static final String TAG = "BPODbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    private static final String DATABASE_NAME = "bpodb";
    private static final String DATABASE_TABLE = "bposettings";
    private static final int DATABASE_VERSION = 2;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
        "create table " + DATABASE_TABLE
        + " (" + ID_FIELD + " integer primary key autoincrement, "
        + KEY_FIELD + " text not null, "
        + VALUE_FIELD + " not null);";
    
    private static final String INIT_TABLE = 
    	"insert into " + DATABASE_TABLE + "(" + KEY_FIELD + ", " + VALUE_FIELD + ") "
    	+ "select 'dest_host' as " + KEY_FIELD  + ", '127.0.0.1' as " + VALUE_FIELD + " "
    	+ "union select 'dest_port', '12345' "
    	+ "union select 'udp_stream', 'disable' "
    	+ ";";

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_CREATE);
            db.execSQL(INIT_TABLE);
            
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public DbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public DbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }


    /**
     * Create a new setting using the key and value provided. If the setting is
     * successfully created return the new rowId, otherwise return
     * a -1 to indicate failure.
     * 
     * @param key the key of the setting
     * @param value the value of the setting
     * @return _id or -1 if failed
     */
    public long createSetting(String key, String value) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_FIELD, key);
        initialValues.put(VALUE_FIELD, value);

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    /**
     * Delete the setting with the given rowId
     * 
     * @param rowId id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteSetting(long rowId) {

        return mDb.delete(DATABASE_TABLE, ID_FIELD + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all settings in the database
     * 
     * @return Cursor over all settings
     */
    public Cursor fetchAllSettings() {

        return mDb.query(DATABASE_TABLE, new String[] {ID_FIELD, KEY_FIELD,
                VALUE_FIELD}, null, null, null, null, null);
    }

    /**
     * Return a Cursor positioned at the setting that matches the given rowId
     * 
     * @param rowId _id of setting to retrieve
     * @return Cursor positioned to matching setting, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchSettingById(long rowId) throws SQLException {

        Cursor mCursor =

            mDb.query(true, DATABASE_TABLE, new String[] {ID_FIELD,
                    KEY_FIELD, VALUE_FIELD}, ID_FIELD + "=" + rowId, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }
    
    /**
     * Return a Cursor positioned at the setting that matches the given rowId
     * 
     * @param rowId _id of setting to retrieve
     * @return Cursor positioned to matching setting, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchSettingByKey(String key) throws SQLException {

        Cursor mCursor =

            mDb.query(true, DATABASE_TABLE, new String[] {ID_FIELD,
                    KEY_FIELD, VALUE_FIELD}, KEY_FIELD + "=" + key, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    /**
     * Update the setting using the details provided. The setting to be updated is
     * specified using the rowId, and it is altered to use the key and value passed in
     * 
     * @param rowId id of note to update
     * @param key to set setting key to
     * @param value to set setting value to
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateSetting(long rowId, String key, String value) {
        ContentValues args = new ContentValues();
        args.put(KEY_FIELD, key);
        args.put(VALUE_FIELD, value);

        return mDb.update(DATABASE_TABLE, args, ID_FIELD + "=" + rowId, null) > 0;
    }
}

