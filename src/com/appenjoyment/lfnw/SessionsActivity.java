package com.appenjoyment.lfnw;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import com.emilsjolander.components.stickylistheaders.StickyListHeadersAdapter;
import com.emilsjolander.components.stickylistheaders.StickyListHeadersListView;

public class SessionsActivity extends ActionBarActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.sessions);

		StickyListHeadersListView listView = (StickyListHeadersListView) findViewById(R.id.sessions_listview);
		listView.setEmptyView(findViewById(R.id.sessions_list_empty));

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

		// TODO: use date pager/selector
		Cursor cursor = SessionsManager.getInstance(this).getAllSessionsOnDay(2013 - 1900, 4 - 1, 28);

		listView.setAdapter(new SessionsAdapter(this, cursor));
	}

	// TODO: use a loader, rather than all this deprecated stuff!
	@SuppressWarnings("deprecation")
	private class SessionsAdapter extends CursorAdapter implements StickyListHeadersAdapter // , SectionIndexer
	{
		public SessionsAdapter(Context context, Cursor cursor)
		{
			super(context, cursor);
		}

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

			view.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					startActivity(new Intent(SessionsActivity.this, WebViewActivity.class).
							putExtra(WebViewActivity.KEY_URL, "http://linuxfestnorthwest.org/node/" + viewHolder.nodeId));
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
			viewHolder.nodeId = cursor.getString(cursor.getColumnIndex(SessionsManager.Sessions.COLUMN_NAME_NODE_ID));
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

		@Override
		public View getHeaderView(int position, View convertView, ViewGroup parent)
		{
			HeaderViewHolder holder;
			if (convertView == null)
			{
				holder = new HeaderViewHolder();
				convertView = getLayoutInflater().inflate(R.layout.sessions_list_header, parent, false);
				holder.text = (TextView) convertView.findViewById(R.id.text);
				convertView.setTag(holder);
			} else
			{
				holder = (HeaderViewHolder) convertView.getTag();
			}

			String headerText = "";
			if (getCursor().moveToPosition(position))
			{
				Date startTime = new Date(getCursor().getLong(getCursor().getColumnIndex(SessionsManager.Sessions.COLUMN_NAME_START_TIME)));
				headerText = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(startTime);
			}

			holder.text.setText(headerText);
			return convertView;
		}

		@Override
		public long getHeaderId(int position)
		{
			if (getCursor().moveToPosition(position))
				return new Date(getCursor().getLong(getCursor().getColumnIndex(SessionsManager.Sessions.COLUMN_NAME_START_TIME))).getHours();
			else
				return 0;
		}

		final class HeaderViewHolder
		{
			TextView text;
		}

		final class ViewHolder
		{
			public long rowId;
			public String nodeId;
			public CheckBox star;
			public TextView title;
			public TextView subtitle;
		}

	}
}
