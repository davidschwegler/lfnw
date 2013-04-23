package com.appenjoyment.lfnw;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import android.content.ContentValues;
import android.content.Context;
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
			s_instance = new SessionsManager(context.getApplicationContext());

		return s_instance;
	}

	/**
	 * Returns all sessions sorted by date.
	 */
	public Cursor getAllSessions()
	{
		return query(false, Sessions.TABLE_NAME, ALL_SESSIONS_ROWS, null, null, null, null, Sessions.COLUMN_NAME_START_TIME + ", "
				+ Sessions.COLUMN_NAME_END_TIME, null);
	}

	/**
	 * Returns all sessions on this date, sorted by date, then by title
	 * 
	 * @see Date#Date(int, int, int)
	 */
	@SuppressWarnings("deprecation")
	public Cursor getAllSessionsOnDay(int year, int month, int day)
	{
		long timeMidnightAM = new Date(year, month, day).getTime();
		long timeMidnightPM = new Date(year, month, day + 1).getTime();

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
		SQLiteDatabase db = m_dbHelper.getWritableDatabase();
		db.beginTransaction();
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

				if (rowId != null)
					db.update(Sessions.TABLE_NAME, values, Sessions._ID + "=?", new String[] { String.valueOf(rowId) });
				else
					db.insert(Sessions.TABLE_NAME, null, values);
			}

			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}
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
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			db.execSQL("DROP TABLE IF EXISTS " + Sessions.TABLE_NAME);
			onCreate(db);
		}
	}

	private static final String TAG = "SessionsManager";
	private static SessionsManager s_instance;

	private final SessionsDatabase m_dbHelper;
}
