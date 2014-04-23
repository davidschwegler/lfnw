package com.appenjoyment.lfnw;

import java.text.SimpleDateFormat;
import java.util.Date;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import com.emilsjolander.components.stickylistheaders.StickyListHeadersAdapter;
import com.emilsjolander.components.stickylistheaders.StickyListHeadersListView;

public class SessionsListFragment extends Fragment
{
	public static final String ARGUMENT_DATE = "Date";
	public static final String ARGUMENT_STARRED_ONLY = "StarredOnly";

	public static SessionsListFragment newInstance(Date date, boolean starredOnly)
	{
		Bundle args = new Bundle();
		args.putLong(ARGUMENT_DATE, date.getTime());
		args.putBoolean(ARGUMENT_STARRED_ONLY, starredOnly);

		SessionsListFragment fragment = new SessionsListFragment();
		fragment.setArguments(args);
		return fragment;
	}

	public Date getDate()
	{
		return m_date;
	}

	public boolean starredOnly()
	{
		return m_starredOnly;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		m_date = new Date(getArguments().getLong(ARGUMENT_DATE));
		m_starredOnly = getArguments().getBoolean(ARGUMENT_STARRED_ONLY);

		m_updateSessionsReceiver = new UpdateSessionsReceiver();

		IntentFilter filter = new IntentFilter();
		filter.addAction(SessionsManager.UPDATED_SESSIONS_ACTION);
		getActivity().registerReceiver(m_updateSessionsReceiver, filter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.session_list, container, false);

		StickyListHeadersListView listView = (StickyListHeadersListView) rootView.findViewById(R.id.sessions_listview);
		listView.setEmptyView(rootView.findViewById(R.id.sessions_list_empty));

		Cursor cursor = SessionsManager.getInstance(getActivity()).getAllSessionsOnDay(m_date, m_starredOnly);
		m_adapter = new SessionsAdapter(getActivity(), cursor);
		listView.setAdapter(m_adapter);

		return rootView;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		getActivity().unregisterReceiver(m_updateSessionsReceiver);
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
			final View view = getActivity().getLayoutInflater().inflate(R.layout.session_list_item, null);

			final ViewHolder viewHolder = new ViewHolder();
			viewHolder.title = (TextView) view.findViewById(R.id.session_title);
			viewHolder.subtitle = (TextView) view.findViewById(R.id.session_subtitle);
			viewHolder.star = (CheckBox) view.findViewById(R.id.session_star);

			viewHolder.star.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					SessionsManager.getInstance(getActivity()).starSession(viewHolder.rowId, viewHolder.star.isChecked());
					cursor.requery();
				}
			});

			view.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					startActivity(new Intent(getActivity(), WebViewActivity.class)
							.putExtra(WebViewActivity.KEY_URL, "http://linuxfestnorthwest.org/node/" + viewHolder.nodeId)
							.putExtra(WebViewActivity.KEY_TITLE, viewHolder.title.getText()));
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

			String room = cursor.getString(cursor.getColumnIndex(SessionsManager.Sessions.COLUMN_NAME_ROOM));
			if (!TextUtils.isEmpty(room))
			{
				if (subtitle.length() != 0)
					subtitle.append(" | ");

				subtitle.append(getString(R.string.sessions_room_prefix) + " " + room);
			}

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
				convertView = getActivity().getLayoutInflater().inflate(R.layout.sessions_list_header, parent, false);
				holder.text = (TextView) convertView.findViewById(R.id.text);
				convertView.setTag(holder);
			}
			else
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

	private final class UpdateSessionsReceiver extends BroadcastReceiver
	{
		@SuppressWarnings("deprecation")
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent.getAction().equals(SessionsManager.UPDATED_SESSIONS_ACTION))
				m_adapter.getCursor().requery();
		}
	}

	private UpdateSessionsReceiver m_updateSessionsReceiver;
	private SessionsAdapter m_adapter;
	private Date m_date;
	private boolean m_starredOnly;
}