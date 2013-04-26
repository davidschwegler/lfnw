package com.appenjoyment.lfnw;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

public class SessionsActivity extends ActionBarActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminate(true);

		super.onCreate(savedInstanceState);

		setContentView(R.layout.sessions);

		// temp code to load temp sessions file
		InputStream stream = null;
		ByteArrayOutputStream stringOutput = null;
		String json = null;
		try
		{
			stream = getAssets().open("sessions.json");
			stringOutput = new ByteArrayOutputStream();
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
				stringOutput.close();
			}
			catch (IOException e)
			{
			}
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

		m_updateSessionsReceiver = new UpdateSessionsReceiver();
		m_updateSessionsServiceConnection = new UpdateSessionsServiceConnection();

		// TODO: onResume? onCreate?
		IntentFilter filter = new IntentFilter();
		filter.addAction(UpdateSessionsService.UPDATE_STARTED_ACTION);
		filter.addAction(UpdateSessionsService.UPDATE_COMPLETED_ACTION);
		filter.addAction(SessionsManager.UPDATED_SESSIONS_ACTION);
		registerReceiver(m_updateSessionsReceiver, filter);

		// tell the update sessions service that now would be a good time to update the sessions if it hasn't in a while
		// calling startService() explicitly, rather than just bind(), also ensures the service lives on beyond the bound life
		startService(new Intent(this, UpdateSessionsService.class).putExtra(UpdateSessionsService.EXTRA_START_KIND, UpdateSessionsService.START_KIND_INTERVAL));
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		// TODO: onResume? onCreate?
		bindService(new Intent(this, UpdateSessionsService.class), m_updateSessionsServiceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		unbindService(m_updateSessionsServiceConnection);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		unregisterReceiver(m_updateSessionsReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.sessions, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.sessions_menu_refresh:
			startService(new Intent(this, UpdateSessionsService.class)
					.putExtra(UpdateSessionsService.EXTRA_START_KIND, UpdateSessionsService.START_KIND_FORCED));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private final class SessionsListPagerAdapter extends FragmentPagerAdapter
	{
		public SessionsListPagerAdapter(FragmentManager fm)
		{
			super(fm);

			m_days = SessionsManager.getInstance(SessionsActivity.this).getSessionDays();
		}

		@Override
		public void notifyDataSetChanged()
		{
			m_days = SessionsManager.getInstance(SessionsActivity.this).getSessionDays();

			super.notifyDataSetChanged();
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
			return DateFormat.getDateInstance(DateFormat.MEDIUM).format(m_days.get(position));
		}

		private List<Date> m_days;
	}

	private final class UpdateSessionsServiceConnection implements ServiceConnection
	{
		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			m_boundUpdateSessionsService = ((UpdateSessionsService.UpdateSessionsBinder) service).getService();
			setProgressBarIndeterminateVisibility(m_boundUpdateSessionsService.isUpdating());
		}

		@Override
		public void onServiceDisconnected(ComponentName className)
		{
			// should never happen
			m_boundUpdateSessionsService = null;
		}
	}

	private final class UpdateSessionsReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent.getAction().equals(UpdateSessionsService.UPDATE_STARTED_ACTION))
				setProgressBarIndeterminateVisibility(true);
			else if (intent.getAction().equals(UpdateSessionsService.UPDATE_COMPLETED_ACTION))
				setProgressBarIndeterminateVisibility(false);
			else if (intent.getAction().equals(SessionsManager.UPDATED_SESSIONS_ACTION))
				m_sessionsListPagerAdapter.notifyDataSetChanged();
		}
	}

	private UpdateSessionsService m_boundUpdateSessionsService;
	private ServiceConnection m_updateSessionsServiceConnection;
	private BroadcastReceiver m_updateSessionsReceiver;
	private SessionsListPagerAdapter m_sessionsListPagerAdapter;
	private ViewPager mViewPager;
}
