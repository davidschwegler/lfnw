package com.appenjoyment.lfnw;

import java.text.SimpleDateFormat;
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
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class SessionsFragment extends Fragment
{
	public static SessionsFragment newInstance()
	{
		return new SessionsFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.sessions, container, false);
		setHasOptionsMenu(true);

		m_swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
		SwipeRefreshLayoutUtility.applyTheme(m_swipeRefreshLayout);
		
		// TODO: support both SwipeRefreshLayout and StickyListHeadersListView -- for now, just disable the pull action on the SwipeRefreshLayout
		// m_swipeRefreshLayout.setOnRefreshListener(new OnRefreshListener()
		// {
		// @Override
		// public void onRefresh()
		// {
		// refresh();
		//
		// // use our callbacks, don't assume we'll succesfully start refreshing
		// m_swipeRefreshLayout.setRefreshing(m_isRefreshing);
		// }
		// });
		m_swipeRefreshLayout.setEnabled(false);

		m_sessionsListPagerAdapter = new SessionsListPagerAdapter(getChildFragmentManager());
		m_viewPager = (ViewPager) view.findViewById(R.id.sessions_pager);
		m_viewPager.setAdapter(m_sessionsListPagerAdapter);

		m_updateSessionsReceiver = new UpdateSessionsReceiver();
		m_updateSessionsServiceConnection = new UpdateSessionsServiceConnection();

		// TODO: onResume? onCreate?
		IntentFilter filter = new IntentFilter();
		filter.addAction(UpdateSessionsService.UPDATE_STARTED_ACTION);
		filter.addAction(UpdateSessionsService.UPDATE_COMPLETED_ACTION);
		filter.addAction(SessionsManager.UPDATED_SESSIONS_ACTION);
		getActivity().registerReceiver(m_updateSessionsReceiver, filter);

		// tell the update sessions service that now would be a good time to update the sessions if it hasn't in a while
		// calling startService() explicitly, rather than just bind(), also ensures the service lives on beyond the bound life
		getActivity().startService(new Intent(getActivity(), UpdateSessionsService.class).
				putExtra(UpdateSessionsService.EXTRA_START_KIND, UpdateSessionsService.START_KIND_INTERVAL));

		return view;
	}

	@Override
	public void onResume()
	{
		super.onResume();

		// TODO: onResume? onCreate?
		getActivity().bindService(new Intent(getActivity(), UpdateSessionsService.class), m_updateSessionsServiceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		getActivity().unbindService(m_updateSessionsServiceConnection);
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroy();
		getActivity().unregisterReceiver(m_updateSessionsReceiver);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.sessions, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.menu_refresh:
			refresh();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void refresh()
	{
		getActivity().startService(new Intent(getActivity(), UpdateSessionsService.class)
				.putExtra(UpdateSessionsService.EXTRA_START_KIND, UpdateSessionsService.START_KIND_FORCED));
	}

	private final class SessionsListPagerAdapter extends FragmentPagerAdapter
	{
		public SessionsListPagerAdapter(FragmentManager fm)
		{
			super(fm);

			m_days = SessionsManager.getInstance(getActivity()).getSessionDays();
		}

		@Override
		public void notifyDataSetChanged()
		{
			m_days = SessionsManager.getInstance(getActivity()).getSessionDays();

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
			return SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM).format(m_days.get(position));
		}

		private List<Date> m_days;
	}

	private final class UpdateSessionsServiceConnection implements ServiceConnection
	{
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			m_boundUpdateSessionsService = ((UpdateSessionsService.UpdateSessionsBinder) service).getService();
			m_isRefreshing = m_boundUpdateSessionsService.isUpdating();
			m_swipeRefreshLayout.setRefreshing(m_isRefreshing);
		}

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
			{
				m_isRefreshing = true;
				m_swipeRefreshLayout.setRefreshing(true);
			}
			else if (intent.getAction().equals(UpdateSessionsService.UPDATE_COMPLETED_ACTION))
			{
				m_isRefreshing = false;
				m_swipeRefreshLayout.setRefreshing(false);
			}
			else if (intent.getAction().equals(SessionsManager.UPDATED_SESSIONS_ACTION))
			{
				m_sessionsListPagerAdapter.notifyDataSetChanged();
			}
		}
	}

	private UpdateSessionsService m_boundUpdateSessionsService;
	private ServiceConnection m_updateSessionsServiceConnection;
	private BroadcastReceiver m_updateSessionsReceiver;
	private SessionsListPagerAdapter m_sessionsListPagerAdapter;
	private ViewPager m_viewPager;
	private SwipeRefreshLayout m_swipeRefreshLayout;
	private boolean m_isRefreshing;
}
