package com.appenjoyment.lfnw.tickets;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.appenjoyment.lfnw.OurApp;

import java.util.ArrayList;
import java.util.List;

public final class TicketsManager
{
	public static final String UPDATED_TICKETS_ACTION = TicketsManager.class.getCanonicalName() + ".UPDATED_TICKETS";

	public static final class Tickets implements BaseColumns
	{
		public static final String TABLE_NAME = "Tickets";
		public static final String COLUMN_NAME_CODE_URL = "CodeUrl";
		public static final String COLUMN_NAME_TICKET_TYPE = "TicketType";
		public static final String COLUMN_NAME_USERNAME = "Username";
		public static final String COLUMN_NAME_YEAR = "Year";

		private Tickets()
		{
		}
	}

	public synchronized static TicketsManager getInstance()
	{
		if (s_instance == null)
			s_instance = new TicketsManager(OurApp.getInstance());

		return s_instance;
	}

	public TicketData getTicket(long id)
	{
		// get all the ticket start times (small list)
		Cursor cursor = query(false, Tickets.TABLE_NAME, ALL_TICKETS_ROWS,
				Tickets._ID + "=?", new String[]{String.valueOf(id)}, null, null, null, null);

		TicketData ticket = null;
		if (cursor.moveToNext())
			ticket = readTicket(cursor);

		cursor.close();

		return ticket;
	}

	public List<TicketData> getTicketsForYear(int year)
	{
		// get all the ticket start times (small list)
		Cursor cursor = query(false, Tickets.TABLE_NAME, ALL_TICKETS_ROWS,
				Tickets.COLUMN_NAME_YEAR + "=?", new String[]{String.valueOf(year)}, null, null, null, null);

		List<TicketData> tickets = new ArrayList<>();
		while (cursor.moveToNext())
			tickets.add(readTicket(cursor));

		cursor.close();

		return tickets;
	}

	public void insertOrUpdate(List<TicketData> tickets)
	{
		insertOrUpdate(m_dbHelper.getWritableDatabase(), tickets);
	}

	private Cursor query(boolean distinct, String table, String[] columns,
						 String selection, String[] selectionArgs, String groupBy,
						 String having, String orderBy, String limit)
	{
		SQLiteDatabase db = m_dbHelper.getReadableDatabase();
		return db.query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
	}

	private TicketData readTicket(Cursor cursor)
	{
		TicketData ticket = new TicketData();
		ticket._id = cursor.getLong(cursor.getColumnIndex(Tickets._ID));
		ticket.codeUrl = cursor.getString(cursor.getColumnIndex(Tickets.COLUMN_NAME_CODE_URL));
		ticket.username = cursor.getString(cursor.getColumnIndex(Tickets.COLUMN_NAME_USERNAME));
		ticket.ticketType = cursor.getString(cursor.getColumnIndex(Tickets.COLUMN_NAME_TICKET_TYPE));
		ticket.year = cursor.getInt(cursor.getColumnIndex(Tickets.COLUMN_NAME_YEAR));
		return ticket;
	}

	private void insertOrUpdate(SQLiteDatabase db, List<TicketData> tickets)
	{
		db.beginTransaction();
		try
		{
			if (!tickets.isEmpty())
				db.execSQL("DELETE FROM " + Tickets.TABLE_NAME + " WHERE " + Tickets.COLUMN_NAME_YEAR + " = " + tickets.get(0).year);

			for (TicketData ticket : tickets)
			{
				ContentValues values = new ContentValues();
				values.put(Tickets.COLUMN_NAME_CODE_URL, ticket.codeUrl);
				values.put(Tickets.COLUMN_NAME_TICKET_TYPE, ticket.ticketType);
				values.put(Tickets.COLUMN_NAME_USERNAME, ticket.username);
				values.put(Tickets.COLUMN_NAME_YEAR, ticket.year);

				db.insert(Tickets.TABLE_NAME, null, values);
			}

			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}

		OurApp.getInstance().sendBroadcast(new Intent(UPDATED_TICKETS_ACTION));
	}

	private TicketsManager(Context context)
	{
		m_dbHelper = new TicketsDatabase(context);
	}

	private class TicketsDatabase extends SQLiteOpenHelper
	{
		// v1: initial version
		public static final int DATABASE_VERSION = 1;
		public static final String DATABASE_NAME = "Tickets.db";

		public TicketsDatabase(Context context)
		{
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE " + Tickets.TABLE_NAME + " ("
					+ Tickets._ID + " INTEGER PRIMARY KEY,"
					+ Tickets.COLUMN_NAME_CODE_URL + " TEXT,"
					+ Tickets.COLUMN_NAME_TICKET_TYPE + " TEXT,"
					+ Tickets.COLUMN_NAME_USERNAME + " TEXT,"
					+ Tickets.COLUMN_NAME_YEAR + " INTEGER"
					+ ");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			// v1: initial version
		}
	}

	private static final String TAG = "TicketsManager";
	private static final String[] ALL_TICKETS_ROWS = new String[]{Tickets._ID, Tickets.COLUMN_NAME_YEAR, Tickets.COLUMN_NAME_TICKET_TYPE,
			Tickets.COLUMN_NAME_USERNAME, Tickets.COLUMN_NAME_CODE_URL};
	private static TicketsManager s_instance;

	private final TicketsDatabase m_dbHelper;
}
