package com.appenjoyment.lfnw;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;
import com.appenjoyment.utility.ArrayUtility;

public class SessionsActivity extends ActionBarActivity
{
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.sessions);

		ListView listView = (ListView) findViewById(R.id.sessions_listview);

		// temp code to read in temp sessions file
		InputStream stream = null;
		String json = null;
		try
		{
			stream = getAssets().open("sessions.json");
			ByteArrayOutputStream stringOutput = new ByteArrayOutputStream();
			byte[] bytes = new byte[8192];
			while (stream.read(bytes) != -1)
				stringOutput.write(bytes);
			json = stringOutput.toString("utf-8");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				stream.close();
			}
			catch (IOException e)
			{
			}
		}

		List<SessionData> sessionData = json == null ? new ArrayList<SessionData>() : SessionData.parseFromJson(json);

		SessionsManager.getInstance(this).insertOrUpdate(sessionData);

		// final List<SessionModel> sessions = new ArrayList<SessionModel>();
		// for (SessionData data : sessionData)
		// sessions.add(new SessionModel(data));

		Date now = new Date();
		Cursor cursor = SessionsManager.getInstance(this).getAllSessionsOnDay(2013 - 1900, 4 - 1, 28);

		// TODO: use a loader, rather than all this deprecated stuff!
		listView.setAdapter(new CursorAdapter(this, cursor)
		{
			@Override
			public View newView(Context context, final Cursor cursor, ViewGroup parent)
			{
				final View view = getLayoutInflater().inflate(R.layout.session_list_item, null);

				final ViewHolder viewHolder = new ViewHolder();
				viewHolder.title = (TextView) view.findViewById(R.id.session_title);
				viewHolder.subtitle = (TextView) view.findViewById(R.id.session_subtitle);
				viewHolder.star = (CheckBox) view.findViewById(R.id.session_star);

				viewHolder.star.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View view)
					{
						SessionsManager.getInstance(SessionsActivity.this).starSession(viewHolder.rowId, viewHolder.star.isChecked());
						cursor.requery();
					}
				});

				view.setTag(viewHolder);

				bindView(view, context, cursor);

				return view;
			}

			@Override
			public void bindView(View view, Context context, Cursor cursor)
			{
				ViewHolder viewHolder = (ViewHolder) view.getTag();

				viewHolder.rowId = cursor.getLong(cursor.getColumnIndex(SessionsManager.Sessions._ID));
				viewHolder.title.setText(cursor.getString(cursor.getColumnIndex(SessionsManager.Sessions.COLUMN_NAME_TITLE)));

				StringBuilder subtitle = new StringBuilder();
				subtitle.append(cursor.getString(cursor.getColumnIndex(SessionsManager.Sessions.COLUMN_NAME_SPEAKERS)));

				String experienceLevel = cursor.getString(cursor.getColumnIndex(SessionsManager.Sessions.COLUMN_NAME_EXPERIENCE_LEVEL));
				if (!TextUtils.isEmpty(experienceLevel))
				{
					if (subtitle.length() != 0)
						subtitle.append(" | ");

					subtitle.append(experienceLevel);
				}

				String track = cursor.getString(cursor.getColumnIndex(SessionsManager.Sessions.COLUMN_NAME_TRACK));
				if (!TextUtils.isEmpty(track))
				{
					if (subtitle.length() != 0)
						subtitle.append(" | ");

					subtitle.append(track);
				}
				viewHolder.subtitle.setText(subtitle);

				viewHolder.star.setChecked(cursor.getInt(cursor.getColumnIndex(SessionsManager.Sessions.COLUMN_NAME_STARRED)) != 0);
			}

			final class ViewHolder
			{
				public long rowId;
				public CheckBox star;
				public TextView title;
				public TextView subtitle;
			}
		});
	}
}
