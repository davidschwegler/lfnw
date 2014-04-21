package com.appenjoyment.lfnw;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;
import android.util.Pair;
import com.appenjoyment.utility.ArrayUtility;

/**
 * Maintains the sessions and user state about them.
 */
public final class SessionsManager
{
	/**
	 * Broadcast action indicating the session list has been updated, and clients should reload their lists (i.e. excluding star toggles).
	 */
	public static final String UPDATED_SESSIONS_ACTION = SessionsManager.class.getCanonicalName() + ".UPDATED_SESSIONS";

	/**
	 * The contract for querying the database.
	 */
	public static final class Sessions implements BaseColumns
	{
		public static final String TABLE_NAME = "Sessions";
		public static final String COLUMN_NAME_NODE_ID = "NodeId";
		public static final String COLUMN_NAME_TITLE = "Title";
		public static final String COLUMN_NAME_ROOM = "Room";
		public static final String COLUMN_NAME_START_TIME = "StartTime";
		public static final String COLUMN_NAME_END_TIME = "EndTime";
		public static final String COLUMN_NAME_SPEAKERS = "Speakers";
		public static final String COLUMN_NAME_EXPERIENCE_LEVEL = "Experience";
		public static final String COLUMN_NAME_TRACK = "Track";
		public static final String COLUMN_NAME_STARRED = "Starred";

		private Sessions()
		{
		}
	}

	/**
	 * The rows returned when returning all sessions.
	 */
	public static final String[] ALL_SESSIONS_ROWS = new String[] { Sessions._ID, Sessions.COLUMN_NAME_NODE_ID, Sessions.COLUMN_NAME_TITLE,
			Sessions.COLUMN_NAME_ROOM, Sessions.COLUMN_NAME_START_TIME, Sessions.COLUMN_NAME_END_TIME, Sessions.COLUMN_NAME_SPEAKERS,
			Sessions.COLUMN_NAME_EXPERIENCE_LEVEL, Sessions.COLUMN_NAME_TRACK, Sessions.COLUMN_NAME_STARRED };

	/**
	 * Returns the static instance of this manager.
	 * <p>
	 * TODO: Expose this as a content provider, so this isn't needed?
	 */
	public synchronized static SessionsManager getInstance(Context context)
	{
		if (s_instance == null)
		{
			s_applicationContext = context.getApplicationContext();
			s_instance = new SessionsManager(s_applicationContext);
		}

		return s_instance;
	}

	/**
	 * Returns the days of all sessions, sorted by date
	 */
	@SuppressWarnings("deprecation")
	public List<Date> getSessionDays()
	{
		// get all the session start times (small list)
		Cursor cursor = query(true, Sessions.TABLE_NAME,
				new String[] { Sessions.COLUMN_NAME_START_TIME }, null, null,
				Sessions.COLUMN_NAME_START_TIME, null,
				Sessions.COLUMN_NAME_START_TIME, null);

		// aggregate the dates based on day (way to do this in sqlite? using string functions?)
		List<Date> dates = new ArrayList<Date>();
		Date lastDate = null;
		while (cursor.moveToNext())
		{
			Date date = new Date(cursor.getLong(0));
			if (lastDate == null || date.getDate() != lastDate.getDate())
			{
				lastDate = date;
				dates.add(date);
			}
		}

		cursor.close();

		return dates;
	}

	/**
	 * Returns all sessions on this date, sorted by date, then by title
	 */
	@SuppressWarnings("deprecation")
	public Cursor getAllSessionsOnDay(Date day)
	{
		long timeMidnightAM = new Date(day.getYear(), day.getMonth(), day.getDate()).getTime();
		long timeMidnightPM = new Date(day.getYear(), day.getMonth(), day.getDate() + 1).getTime();

		// all sessions which start before midnight tonight and end after midnight this morning, ordered by the start time then the end time
		return query(false, Sessions.TABLE_NAME, ALL_SESSIONS_ROWS, Sessions.COLUMN_NAME_START_TIME + " < ? AND " + Sessions.COLUMN_NAME_END_TIME + " > ?",
				new String[] { String.valueOf(timeMidnightPM), String.valueOf(timeMidnightAM) }, null, null,
				Sessions.COLUMN_NAME_START_TIME + ", " + Sessions.COLUMN_NAME_END_TIME + "," + Sessions.COLUMN_NAME_TITLE, null);
	}

	/**
	 * Sets whether a session is starred.
	 * 
	 * @return True if the session existed and the state was set.
	 */
	public boolean starSession(long rowId, boolean starred)
	{
		boolean success = false;
		SQLiteDatabase db = m_dbHelper.getWritableDatabase();
		db.beginTransaction();
		try
		{
			ContentValues values = new ContentValues();
			values.put(Sessions.COLUMN_NAME_STARRED, starred);

			int rows = db.update(Sessions.TABLE_NAME, values, Sessions._ID + "=?", new String[] { String.valueOf(rowId) });

			success = rows > 0;
			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}

		return success;
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
	 * Inserts the list of sessions, updating the existing ones' data while preserving additional user data.
	 */
	public void insertOrUpdate(List<SessionData> sessions)
	{
		insertOrUpdate(m_dbHelper.getWritableDatabase(), sessions);
	}

	private void insertOrUpdate(SQLiteDatabase db, List<SessionData> sessions)
	{
		db.beginTransaction();
		boolean processedAny = false;
		try
		{
			for (SessionData session : sessions)
			{
				Pair<Date, Date> dateRange = session.parseTimeSlotDateRange();

				// skip entries without a valid date
				if (dateRange == null)
				{
					Log.e(TAG, "Skipping entry without date, node=" + session.nodeId + ", title=" + session.title);
					continue;
				}

				ContentValues values = new ContentValues();
				values.put(Sessions.COLUMN_NAME_NODE_ID, session.nodeId);
				values.put(Sessions.COLUMN_NAME_TITLE, session.title);
				values.put(Sessions.COLUMN_NAME_ROOM, session.room);
				values.put(Sessions.COLUMN_NAME_EXPERIENCE_LEVEL, session.experienceLevel);
				values.put(Sessions.COLUMN_NAME_TRACK, session.track);
				values.put(Sessions.COLUMN_NAME_START_TIME, dateRange.first.getTime());
				values.put(Sessions.COLUMN_NAME_END_TIME, dateRange.second.getTime());

				// TODO: do this better
				values.put(Sessions.COLUMN_NAME_SPEAKERS, ArrayUtility.join(", ", Arrays.asList(session.speakers)));

				// look for a row that already exists
				Long rowId = null;
				Cursor cursorExisting = db.query(Sessions.TABLE_NAME, new String[] { Sessions._ID }, Sessions.COLUMN_NAME_NODE_ID + "=?",
						new String[] { session.nodeId }, null, null, null);
				if (cursorExisting.moveToFirst())
					rowId = cursorExisting.getLong(0);
				cursorExisting.close();

				if (rowId != null)
					db.update(Sessions.TABLE_NAME, values, Sessions._ID + "=?", new String[] { String.valueOf(rowId) });
				else
					db.insert(Sessions.TABLE_NAME, null, values);

				processedAny = true;
			}

			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}

		if (processedAny)
			s_applicationContext.sendBroadcast(new Intent(UPDATED_SESSIONS_ACTION));
	}

	// TODO: where would be a better place to do this kind of initialization?
	private void insertInitialData(SQLiteDatabase db)
	{
		Log.i(TAG, "Inserting initial sessions list");

		// temp code to load temp sessions file
		InputStream stream = null;
		ByteArrayOutputStream stringOutput = null;
		String json = null;
		try
		{
			if (BuildConfig.DEBUG && DEBUG_TEST_SESSIONS)
				stream = s_applicationContext.getAssets().open("sessions_test_one_day.json");
			else
				stream = s_applicationContext.getAssets().open("sessions.json");

			stringOutput = new ByteArrayOutputStream();
			byte[] bytes = new byte[8192];
			while (stream.read(bytes) != -1)
				stringOutput.write(bytes);
			json = stringOutput.toString("utf-8");
		}
		catch (IOException e)
		{
			Log.w(TAG, "Couldn't load embedded json", e);
		}
		finally
		{
			try
			{
				if (stringOutput != null)
					stringOutput.close();
			}
			catch (IOException e)
			{
			}
			try
			{
				if (stream != null)
					stream.close();
			}
			catch (IOException e)
			{
			}
		}

		List<SessionData> sessionData = json == null ? new ArrayList<SessionData>() : SessionData.parseFromJson(json);
		if (sessionData != null)
			insertOrUpdate(db, sessionData);
	}

	private SessionsManager(Context context)
	{
		m_dbHelper = new SessionsDatabase(context);
	}

	private class SessionsDatabase extends SQLiteOpenHelper
	{
		public static final int DATABASE_VERSION = 1;
		public static final String DATABASE_NAME = "Sessions.db";

		public SessionsDatabase(Context context)
		{
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE " + Sessions.TABLE_NAME + " ("
					+ Sessions._ID + " INTEGER PRIMARY KEY,"
					+ Sessions.COLUMN_NAME_NODE_ID + " TEXT,"
					+ Sessions.COLUMN_NAME_TITLE + " TEXT,"
					+ Sessions.COLUMN_NAME_ROOM + " TEXT,"
					+ Sessions.COLUMN_NAME_START_TIME + " INTEGER,"
					+ Sessions.COLUMN_NAME_END_TIME + " INTEGER,"
					+ Sessions.COLUMN_NAME_SPEAKERS + " TEXT,"
					+ Sessions.COLUMN_NAME_EXPERIENCE_LEVEL + " TEXT,"
					+ Sessions.COLUMN_NAME_TRACK + " TEXT,"
					+ Sessions.COLUMN_NAME_STARRED + " INTEGER"
					+ ");");

			insertInitialData(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			// version 1 -- nothing to upgrade yet!
		}
	}

	private static final String TAG = "SessionsManager";
	private static final boolean DEBUG_TEST_SESSIONS = false;
	private static SessionsManager s_instance;
	private static Context s_applicationContext;

	private final SessionsDatabase m_dbHelper;
}
