package com.appenjoyment.lfnw;

import java.util.Date;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Maintains the scanned badges.
 */
public final class ScannedBadgesManager
{
	/**
	 * Broadcast action indicating the list of scanned badges has been updated, and clients should reload their lists.
	 */
	public static final String UPDATED_SCANNED_BADGES_ACTION = ScannedBadgesManager.class.getCanonicalName() + ".UPDATED_SCANNED_BADGES";

	/**
	 * The contract for querying the database.
	 */
	public static final class ScannedBadges implements BaseColumns
	{
		public static final String TABLE_NAME = "ScannedBadges";
		public static final String COLUMN_NAME_DATE_SCANNED = "DateScanned";
		public static final String COLUMN_NAME_EMAIL = "Email";
		public static final String COLUMN_NAME_FIRST_NAME = "FirstName";
		public static final String COLUMN_NAME_LAST_NAME = "LastName";
		public static final String COLUMN_NAME_ORGANIZATION = "Organization";

		private ScannedBadges()
		{
		}
	}

	/**
	 * The rows returned when returning all sessions.
	 */
	public static final String[] ALL_SCANNED_BADGES_ROWS = new String[]
	{
			ScannedBadges._ID,
			ScannedBadges.COLUMN_NAME_FIRST_NAME,
			ScannedBadges.COLUMN_NAME_LAST_NAME,
			ScannedBadges.COLUMN_NAME_EMAIL,
			ScannedBadges.COLUMN_NAME_ORGANIZATION,
			ScannedBadges.COLUMN_NAME_DATE_SCANNED
	};

	/**
	 * Returns the static instance of this manager.
	 * <p>
	 * TODO: Expose this as a content provider, so this isn't needed?
	 */
	public synchronized static ScannedBadgesManager getInstance(Context context)
	{
		if (s_instance == null)
		{
			s_applicationContext = context.getApplicationContext();
			s_instance = new ScannedBadgesManager(s_applicationContext);
		}

		return s_instance;
	}

	/**
	 * Builds a ScannedBadgeData from a row of data (row must include all columns).
	 */
	public static ScannedBadgeData createFromCursor(Cursor cursor)
	{
		ScannedBadgeData data = new ScannedBadgeData();
		data.dateScanned = cursor.getLong(cursor.getColumnIndex(ScannedBadgesManager.ScannedBadges.COLUMN_NAME_DATE_SCANNED));

		data.contactData = new BadgeContactData();
		data.contactData.email = cursor.getString(cursor.getColumnIndex(ScannedBadgesManager.ScannedBadges.COLUMN_NAME_EMAIL));
		data.contactData.firstName = cursor.getString(cursor.getColumnIndex(ScannedBadges.COLUMN_NAME_FIRST_NAME));
		data.contactData.lastName = cursor.getString(cursor.getColumnIndex(ScannedBadges.COLUMN_NAME_LAST_NAME));
		data.contactData.organization = cursor.getString(cursor.getColumnIndex(ScannedBadges.COLUMN_NAME_ORGANIZATION));

		return data;
	}

	/**
	 * Returns all scanned badges, sorted by date scanned
	 */
	public Cursor getAllScannedBadges()
	{
		// all scanned badges, ordered by the date
		return query(false, ScannedBadges.TABLE_NAME, ALL_SCANNED_BADGES_ROWS, null, null, null, null, ScannedBadges.COLUMN_NAME_DATE_SCANNED, null);
	}

	/**
	 * Generic API for querying the database.
	 * 
	 * @see SQLiteDatabase#query(boolean, String, String[], String, String[], String, String, String, String)
	 */
	public Cursor query(boolean distinct, String table, String[] columns,
			String selection, String[] selectionArgs, String groupBy,
			String having, String orderBy, String limit)
	{
		SQLiteDatabase db = m_dbHelper.getReadableDatabase();
		return db.query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
	}

	/**
	 * Inserts an entry for this scanned badge.
	 */
	public void insert(ScannedBadgeData data)
	{
		insert(m_dbHelper.getWritableDatabase(), data);
	}

	private void insert(SQLiteDatabase db, ScannedBadgeData data)
	{
		db.beginTransaction();
		boolean success = false;
		try
		{
			ContentValues values = new ContentValues();
			values.put(ScannedBadges.COLUMN_NAME_DATE_SCANNED, data.dateScanned);
			values.put(ScannedBadges.COLUMN_NAME_EMAIL, data.contactData.email);
			values.put(ScannedBadges.COLUMN_NAME_FIRST_NAME, data.contactData.firstName);
			values.put(ScannedBadges.COLUMN_NAME_LAST_NAME, data.contactData.lastName);
			values.put(ScannedBadges.COLUMN_NAME_ORGANIZATION, data.contactData.organization);

			db.insert(ScannedBadges.TABLE_NAME, null, values);
			db.setTransactionSuccessful();
			success = true;
		}
		finally
		{
			db.endTransaction();
		}

		if (success)
			s_applicationContext.sendBroadcast(new Intent(UPDATED_SCANNED_BADGES_ACTION));
	}

	private ScannedBadgesManager(Context context)
	{
		m_dbHelper = new ScannedBadgesDatabase(context);
	}

	private class ScannedBadgesDatabase extends SQLiteOpenHelper
	{
		public static final int DATABASE_VERSION = 1;
		public static final String DATABASE_NAME = "ScannedBadges.db";

		public ScannedBadgesDatabase(Context context)
		{
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@SuppressWarnings("unused")
		@Override
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE " + ScannedBadges.TABLE_NAME + " ("
					+ ScannedBadges._ID + " INTEGER PRIMARY KEY,"
					+ ScannedBadges.COLUMN_NAME_DATE_SCANNED + " INTEGER,"
					+ ScannedBadges.COLUMN_NAME_EMAIL + " TEXT,"
					+ ScannedBadges.COLUMN_NAME_FIRST_NAME + " TEXT,"
					+ ScannedBadges.COLUMN_NAME_LAST_NAME + " INTEGER,"
					+ ScannedBadges.COLUMN_NAME_ORGANIZATION + " TEXT"
					+ ");");

			if (BuildConfig.DEBUG && TEST_DEBUG_DATA)
			{
				ScannedBadgeData data = new ScannedBadgeData();
				data.contactData = new BadgeContactData();
				data.contactData.email = "developer@appenjoyment.com";
				data.contactData.firstName = "David";
				data.contactData.lastName = "Schwegler";
				data.contactData.organization = "App Enjoyment";
				data.dateScanned = new Date().getTime();
				insert(db, data);

				data = new ScannedBadgeData();
				data.contactData = new BadgeContactData();
				data.contactData.firstName = "1";
				data.contactData.lastName = "1s";
				data.contactData.organization = "1 Co";
				data.dateScanned = new Date().getTime() + 1;
				insert(db, data);

				data = new ScannedBadgeData();
				data.contactData = new BadgeContactData();
				data.contactData.email = "2@2.com";
				data.contactData.lastName = "2s";
				data.contactData.organization = "2 Co";
				data.dateScanned = new Date().getTime() + 2;
				insert(db, data);

				data = new ScannedBadgeData();
				data.contactData = new BadgeContactData();
				data.contactData.email = "3@3.com";
				data.contactData.firstName = "3";
				data.contactData.organization = "3 Co";
				data.dateScanned = new Date().getTime() + 3;
				insert(db, data);

				data = new ScannedBadgeData();
				data.contactData = new BadgeContactData();
				data.contactData.email = "4@4.com";
				data.contactData.organization = "4 Co";
				data.dateScanned = new Date().getTime() + 4;
				insert(db, data);

				data = new ScannedBadgeData();
				data.contactData = new BadgeContactData();
				data.contactData.organization = "5 Co";
				data.dateScanned = new Date().getTime() + 5;
				insert(db, data);

				data = new ScannedBadgeData();
				data.contactData = new BadgeContactData();
				data.contactData.email = "6@6.com";
				data.contactData.firstName = "6";
				data.contactData.lastName = "6s";
				data.dateScanned = new Date().getTime() + 6;
				insert(db, data);

				data = new ScannedBadgeData();
				data.contactData = new BadgeContactData();
				data.contactData.email = "7@7.com";
				data.dateScanned = new Date().getTime() + 7;
				insert(db, data);

				data = new ScannedBadgeData();
				data.contactData = new BadgeContactData();
				data.contactData.firstName = "8";
				data.contactData.lastName = "8s";
				data.dateScanned = new Date().getTime() + 8;
				insert(db, data);

				data = new ScannedBadgeData();
				data.contactData = new BadgeContactData();
				data.dateScanned = new Date().getTime() + 9;
				insert(db, data);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			// version 1 -- nothing to upgrade yet!
		}
	}

	private static ScannedBadgesManager s_instance;
	private static Context s_applicationContext;

	private static final boolean TEST_DEBUG_DATA = false;

	private final ScannedBadgesDatabase m_dbHelper;
}
