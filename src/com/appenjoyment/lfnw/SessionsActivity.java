package com.appenjoyment.lfnw;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

public class SessionsActivity extends ActionBarActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.sessions);

		// temp code to load temp sessions file
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

		m_sessionsListPagerAdapter = new SessionsListPagerAdapter(getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.sessions_pager);
		mViewPager.setAdapter(m_sessionsListPagerAdapter);
	}

	public class SessionsListPagerAdapter extends FragmentPagerAdapter
	{
		public SessionsListPagerAdapter(FragmentManager fm)
		{
			super(fm);

			m_days = SessionsManager.getInstance(SessionsActivity.this).getSessionDays();
		}

		@Override
		public Fragment getItem(int i)
		{
			Fragment fragment = new SessionsListFragment();
			Bundle args = new Bundle();

			args.putLong(SessionsListFragment.ARG_DATE, m_days.get(i).getTime());
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount()
		{
			return m_days.size();
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			return SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM).format(m_days.get(position));
		}

		private final List<Date> m_days;
	}

	private SessionsListPagerAdapter m_sessionsListPagerAdapter;
	private ViewPager mViewPager;
}
