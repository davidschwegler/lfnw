package com.appenjoyment.lfnw;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
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
		final List<SessionModel> sessions = new ArrayList<SessionModel>();
		for (SessionData data : sessionData)
			sessions.add(new SessionModel(data));

		listView.setAdapter(new BaseAdapter()
		{
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				ViewHolder viewHolder;
				if (convertView != null)
				{
					viewHolder = (ViewHolder) convertView.getTag();
				}
				else
				{
					final View newView = getLayoutInflater().inflate(R.layout.session_list_item, null);
					convertView = newView;

					viewHolder = new ViewHolder();
					viewHolder.title = (TextView) newView.findViewById(R.id.session_title);
					viewHolder.subtitle = (TextView) newView.findViewById(R.id.session_subtitle);
					viewHolder.star = (CheckBox) newView.findViewById(R.id.session_star);

					viewHolder.star.setOnCheckedChangeListener(new OnCheckedChangeListener()
					{
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
						{
							ViewHolder viewHolder = (ViewHolder) newView.getTag();
							viewHolder.session.setStarred(isChecked);
						}
					});

					newView.setTag(viewHolder);
				}

				SessionModel session = sessions.get(position);
				viewHolder.session = session;
				viewHolder.title.setText(session.getSessionData().title);

				StringBuilder subtitle = new StringBuilder();
				if (session.getSessionData().speakers.length != 0)
					subtitle.append(ArrayUtility.join(", ", Arrays.asList(session.getSessionData().speakers)));

				if (!TextUtils.isEmpty(session.getSessionData().experienceLevel))
				{
					if (subtitle.length() != 0)
						subtitle.append(" | ");

					subtitle.append(session.getSessionData().experienceLevel);
				}

				if (!TextUtils.isEmpty(session.getSessionData().track))
				{
					if (subtitle.length() != 0)
						subtitle.append(" | ");

					subtitle.append(session.getSessionData().track);
				}
				viewHolder.subtitle.setText(subtitle);

				viewHolder.star.setChecked(session.isStarred());

				return convertView;
			}

			@Override
			public long getItemId(int position)
			{
				return position;
			}

			@Override
			public Object getItem(int position)
			{
				return sessions.get(position);
			}

			@Override
			public int getCount()
			{
				return sessions.size();
			}

			final class ViewHolder
			{
				public CheckBox star;
				public TextView title;
				public TextView subtitle;
				public SessionModel session;
			}
		});
	}
}
