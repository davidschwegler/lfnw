package com.appenjoyment.lfnw;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
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
import com.appenjoyment.utility.StreamUtility;

import ezvcard.util.IOUtils;

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
		public static final String COLUMN_NAME_IS_BOF = "IsBof";
		public static final String COLUMN_NAME_FLAG_MY_SCHEDULE_URL = "FlagMySchedule";
		public static final String COLUMN_NAME_FLAG_MY_SCHEDULE_DIRTY_TIME = "FlagMyScheduleDirtyTime";

		private Sessions()
		{
		}
	}

	/**
	 * The rows returned when returning all sessions.
	 */
	public static final String[] ALL_SESSIONS_ROWS = new String[]{Sessions._ID, Sessions.COLUMN_NAME_NODE_ID, Sessions.COLUMN_NAME_TITLE,
			Sessions.COLUMN_NAME_ROOM, Sessions.COLUMN_NAME_START_TIME, Sessions.COLUMN_NAME_END_TIME, Sessions.COLUMN_NAME_SPEAKERS,
			Sessions.COLUMN_NAME_EXPERIENCE_LEVEL, Sessions.COLUMN_NAME_TRACK, Sessions.COLUMN_NAME_STARRED, Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_URL,
			Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_DIRTY_TIME, Sessions.COLUMN_NAME_IS_BOF};

	/**
	 * Returns the static instance of this manager.
	 * <p/>
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
	 * Returns a list of gregorian years that have sessions, most recent first.
	 */
	@SuppressWarnings("deprecation")
	public List<Integer> getYears()
	{
		// get all the session start times (small list)
		Cursor cursor = query(true, Sessions.TABLE_NAME,
				new String[]{Sessions.COLUMN_NAME_START_TIME}, null, null,
				Sessions.COLUMN_NAME_START_TIME, null,
				Sessions.COLUMN_NAME_START_TIME + " desc", null);

		// aggregate the dates based on year (way to do this in sqlite? using string functions?)
		List<Integer> years = new ArrayList<Integer>();
		int lastYear = -1;
		while (cursor.moveToNext())
		{
			Date date = new Date(cursor.getLong(0));
			if (lastYear == -1 || date.getYear() != lastYear)
			{
				lastYear = date.getYear();
				years.add(lastYear + 1900);
			}
		}

		cursor.close();

		return years;
	}

	/**
	 * Returns the days of all sessions on the specified gregorian calendar year, sorted by date, optionally only those which are starred.
	 */
	@SuppressWarnings("deprecation")
	public List<Date> getSessionDaysForYear(int year, boolean starredOnly)
	{
		// get all the session start times (small list)
		Cursor cursor = query(true, Sessions.TABLE_NAME,
				new String[]{Sessions.COLUMN_NAME_START_TIME}, starredOnly ? Sessions.COLUMN_NAME_STARRED + "=1" : null, null,
				Sessions.COLUMN_NAME_START_TIME, null,
				Sessions.COLUMN_NAME_START_TIME, null);

		// aggregate the dates based on day (way to do this in sqlite? using string functions?)
		List<Date> dates = new ArrayList<Date>();
		int dateYear = year - 1900;
		Date lastDate = null;
		while (cursor.moveToNext())
		{
			Date date = new Date(cursor.getLong(0));
			if (date.getYear() == dateYear && (lastDate == null || date.getDate() != lastDate.getDate()))
			{
				lastDate = date;
				dates.add(date);
			}
		}

		cursor.close();

		return dates;
	}

	/**
	 * Returns all sessions on this date, sorted by date, then by title, optionally only those which are starred.
	 */
	@SuppressWarnings("deprecation")
	public Cursor getAllSessionsOnDay(Date day, boolean starredOnly)
	{
		long timeMidnightAM = new Date(day.getYear(), day.getMonth(), day.getDate()).getTime();
		long timeMidnightPM = new Date(day.getYear(), day.getMonth(), day.getDate() + 1).getTime();

		// all sessions which start before midnight tonight and end after midnight this morning, ordered by the start time then the end time
		return query(false, Sessions.TABLE_NAME, ALL_SESSIONS_ROWS, Sessions.COLUMN_NAME_START_TIME + " < ? AND " + Sessions.COLUMN_NAME_END_TIME + " > ?" +
						(starredOnly ? " AND " + Sessions.COLUMN_NAME_STARRED + "=1" : ""),
				new String[]{String.valueOf(timeMidnightPM), String.valueOf(timeMidnightAM)}, null, null,
				Sessions.COLUMN_NAME_START_TIME + ", " + Sessions.COLUMN_NAME_END_TIME + "," + Sessions.COLUMN_NAME_TITLE, null);
	}

	/**
	 * Returns all sessions with a flag my schedule url
	 */
	@SuppressWarnings("deprecation")
	public Cursor getAllSessionsWithFlagMyScheduleUrl()
	{
		return query(false, Sessions.TABLE_NAME, new String[]{Sessions.COLUMN_NAME_NODE_ID, Sessions.COLUMN_NAME_STARRED, Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_DIRTY_TIME, Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_URL},
				Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_URL + " IS NOT NULL", null, null, null, null, null);
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
			values.put(Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_DIRTY_TIME, new GregorianCalendar().getTimeInMillis());

			int rows = db.update(Sessions.TABLE_NAME, values, Sessions._ID + "=?", new String[]{String.valueOf(rowId)});

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

	public void clearDirtyFlag(String nodeId, long dirtyTime)
	{
		SQLiteDatabase db = m_dbHelper.getWritableDatabase();
		db.beginTransaction();
		try
		{
			// enforce our dirty times match
			ContentValues values = new ContentValues();
			values.put(Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_DIRTY_TIME, 0);

			db.update(Sessions.TABLE_NAME, values, Sessions.COLUMN_NAME_NODE_ID + "=? AND " + Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_DIRTY_TIME + "=?",
					new String[]{nodeId, String.valueOf(dirtyTime)});

			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}
	}

	public void clearDirtyFlagWithUrl(String nodeId, long dirtyTime, String url)
	{
		SQLiteDatabase db = m_dbHelper.getWritableDatabase();
		db.beginTransaction();
		try
		{
			// enforce our dirty times match
			ContentValues values = new ContentValues();
			values.put(Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_URL, url);
			values.put(Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_DIRTY_TIME, 0);

			db.update(Sessions.TABLE_NAME, values, Sessions.COLUMN_NAME_NODE_ID + "=? AND " + Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_DIRTY_TIME + "=?",
					new String[]{nodeId, String.valueOf(dirtyTime)});

			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}
	}

	public void clearDirtyFlagWithStarred(String nodeId, long dirtyTime, boolean starred)
	{
		SQLiteDatabase db = m_dbHelper.getWritableDatabase();
		db.beginTransaction();
		try
		{
			// enforce our dirty times match
			ContentValues values = new ContentValues();
			values.put(Sessions.COLUMN_NAME_STARRED, starred);
			values.put(Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_DIRTY_TIME, 0);

			db.update(Sessions.TABLE_NAME, values, Sessions.COLUMN_NAME_NODE_ID + "=? AND " + Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_DIRTY_TIME + "=?",
					new String[]{nodeId, String.valueOf(dirtyTime)});

			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}
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
				values.put(Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_URL, session.flagMyScheduleUrl);
				values.put(Sessions.COLUMN_NAME_IS_BOF, session.isBof);

				// TODO: do this better
				values.put(Sessions.COLUMN_NAME_SPEAKERS, ArrayUtility.join(", ", Arrays.asList(session.speakers)));

				// look for a row that already exists
				Long rowId = null;
				Boolean starred = null;
				Long dirtyTime = null;
				Cursor cursorExisting = db.query(Sessions.TABLE_NAME, new String[]{Sessions._ID, Sessions.COLUMN_NAME_STARRED, Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_DIRTY_TIME}, Sessions.COLUMN_NAME_NODE_ID + "=?",
						new String[]{session.nodeId}, null, null, null);
				if (cursorExisting.moveToFirst())
				{
					rowId = cursorExisting.getLong(0);
					starred = cursorExisting.getInt(1) == 1;
					dirtyTime = cursorExisting.getLong(2);
				}
				cursorExisting.close();

				if (rowId != null)
				{
					// if not dirty, just update starred to match server, otherwise let sync figure it out
					// TODO: should this code actually be in sync?
					if (dirtyTime.longValue() == 0)
					{
						Boolean isFlaggedOnServer = FlagMyScheduleUtility.isFlaggedOnServer(session.flagMyScheduleUrl);
						if (isFlaggedOnServer != null && isFlaggedOnServer.booleanValue() != starred)
						{
							Log.d(TAG, "Updated " + session.nodeId + " with server starred=" + isFlaggedOnServer.booleanValue());
							values.put(Sessions.COLUMN_NAME_STARRED, isFlaggedOnServer);
						}
					}

					db.update(Sessions.TABLE_NAME, values, Sessions._ID + "=?", new String[]{String.valueOf(rowId)});
				}
				else
				{
					// always up to date when first sync'd down
					values.put(Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_DIRTY_TIME, 0);

					Boolean isFlaggedOnServer = FlagMyScheduleUtility.isFlaggedOnServer(session.flagMyScheduleUrl);
					if (isFlaggedOnServer != null)
						values.put(Sessions.COLUMN_NAME_STARRED, isFlaggedOnServer.booleanValue());

					db.insert(Sessions.TABLE_NAME, null, values);
				}

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
		Log.i(TAG, "Inserting initial bofs/sessions list");

		insertInitialData(db, "bofs.json", "sessions.json");
	}

	private String loadAsset(String assetName)
	{
		InputStream stream = null;
		try
		{
			stream = s_applicationContext.getAssets().open(assetName);
			return StreamUtility.readAsString(stream);
		}
		catch (IOException e)
		{
			Log.w(TAG, "Couldn't read asset", e);
			return null;
		}
		finally
		{
			IOUtils.closeQuietly(stream);
		}
	}

	private void insertInitialData(SQLiteDatabase db, String bofsAssetName, String sessionsAssetName)
	{
		String bofSessionsJson = loadAsset(bofsAssetName);
		if (bofSessionsJson != null)
		{
			List<SessionData> bofSessionsData = SessionData.parseFromJson(bofSessionsJson);
			if (bofSessionsData != null)
			{
				for (SessionData bofSession : bofSessionsData)
					bofSession.isBof = true;
				insertOrUpdate(db, bofSessionsData);
			}
		}

		String sessionsJson = loadAsset(sessionsAssetName);
		if (sessionsJson != null)
		{
			List<SessionData> sessionsData = SessionData.parseFromJson(sessionsJson);
			if (sessionsData != null)
				insertOrUpdate(db, sessionsData);
		}
	}

	private SessionsManager(Context context)
	{
		m_dbHelper = new SessionsDatabase(context);
	}

	private class SessionsDatabase extends SQLiteOpenHelper
	{
		// v1: initial version
		// v2: update with embedded 2014 data
		// v3: update with embedded 2015 data
		// v4: update with embedded 2016 data, add isBof
		public static final int DATABASE_VERSION = 4;
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
					+ Sessions.COLUMN_NAME_STARRED + " INTEGER,"
					+ Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_URL + " INTEGER,"
					+ Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_DIRTY_TIME + " INTEGER DEFAULT -1,"
					+ Sessions.COLUMN_NAME_IS_BOF + " INTEGER"
					+ ");");

			insertInitialData(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			// v1: initial version
			// v2: update with embedded 2014 data
			// v3: update with embedded 2015 data
			// v4: update with embedded 2016 data, add isBof
			if (oldVersion < 4)
			{
				// can only add one column per alter table call
				db.execSQL("ALTER TABLE " + Sessions.TABLE_NAME + " "
						+ "ADD COLUMN " + Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_URL + " TEXT"
						+ ";");
				db.execSQL("ALTER TABLE " + Sessions.TABLE_NAME + " "
						+ "ADD COLUMN " + Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_DIRTY_TIME + " INTEGER DEFAULT -1"
						+ ";");
				db.execSQL("ALTER TABLE " + Sessions.TABLE_NAME + " "
						+ "ADD COLUMN " + Sessions.COLUMN_NAME_IS_BOF + " INTEGER"
						+ ";");
			}

			insertInitialData(db);
		}
	}

	private static final String TAG = "SessionsManager";
	private static SessionsManager s_instance;
	private static Context s_applicationContext;

	private final SessionsDatabase m_dbHelper;
}
