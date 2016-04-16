package com.appenjoyment.lfnw;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.OnNavigationListener;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.appenjoyment.utility.HashCodeUtility;

public class SessionsFragment extends Fragment implements IDrawerFragment
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

		ActionBarActivity actionBarActivity = (ActionBarActivity) getActivity();
		actionBarActivity.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		loadNavigationOptions();
		if (m_navigationOptions.size() != 0)
		{
			if (savedInstanceState != null)
			{
				NavigationOption navigationOption = NavigationOption.createFromBundle(savedInstanceState.getBundle(KEY_NAVIGATION_OPTION));
				if (m_navigationOptions.indexOf(navigationOption) != -1)
					m_navigationOption = navigationOption;
			}
			if (m_navigationOption == null)
				m_navigationOption = loadLastNavigationOption();
			if (m_navigationOption == null)
				m_navigationOption = m_navigationOptions.get(0);
		}

		m_actionBarNavigationListener = new OnNavigationListener()
		{
			@Override
			public boolean onNavigationItemSelected(int itemPosition, long itemId)
			{
				m_navigationOption = m_navigationOptions.get(itemPosition);
				saveLastNavigationOption();
				m_sessionsListPagerAdapter.notifyDataSetChanged();
				return true;
			}
		};

		m_navigationListAdapter = createNavigationListAdapter();
		actionBarActivity.getSupportActionBar().setListNavigationCallbacks(m_navigationListAdapter, m_actionBarNavigationListener);
		if (m_navigationOption != null)
			actionBarActivity.getSupportActionBar().setSelectedNavigationItem(m_navigationOptions.indexOf(m_navigationOption));

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

		m_noDaysTextView = view.findViewById(R.id.sessions_list_empty);

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

		// can't populate the navigation list unless the navigation mode is List, so populate the navigation list first, then set it back to standard if needed
		if (getActivity() instanceof IDrawerActivity && ((IDrawerActivity) getActivity()).isDrawerOpen())
			actionBarActivity.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		else
			getActivity().setTitle("");

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
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);

		if (m_navigationOption != null)
			outState.putBundle(KEY_NAVIGATION_OPTION, m_navigationOption.saveToBundle());
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroy();

		getActivity().unregisterReceiver(m_updateSessionsReceiver);

		ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
		actionBar.setListNavigationCallbacks(null, null);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		if (!(getActivity() instanceof IDrawerActivity) || !((IDrawerActivity) getActivity()).isDrawerOpen())
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

	@Override
	public void onDrawerOpened()
	{
		getActivity().supportInvalidateOptionsMenu();

		ActionBarActivity actionBarActivity = (ActionBarActivity) getActivity();
		actionBarActivity.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	}

	@Override
	public void onDrawerClosed()
	{
		getActivity().setTitle("");
		getActivity().supportInvalidateOptionsMenu();

		ActionBarActivity actionBarActivity = (ActionBarActivity) getActivity();
		actionBarActivity.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
	}

	private void refresh()
	{
		getActivity().startService(new Intent(getActivity(), UpdateSessionsService.class)
				.putExtra(UpdateSessionsService.EXTRA_START_KIND, UpdateSessionsService.START_KIND_FORCED));
	}

	private NavigationOption loadLastNavigationOption()
	{
		SharedPreferences prefs = getActivity().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
		if (prefs.contains(PREFERENCE_NAVIGATION_OPTION_YEAR) &&
				prefs.contains(PREFERENCE_NAVIGATION_OPTION_STARRED_ONLY) &&
				prefs.contains(PREFERENCE_NAVIGATION_OPTION_TITLE))
		{
			return new NavigationOption(prefs.getInt(PREFERENCE_NAVIGATION_OPTION_YEAR, -1),
					prefs.getBoolean(PREFERENCE_NAVIGATION_OPTION_STARRED_ONLY, false),
					prefs.getString(PREFERENCE_NAVIGATION_OPTION_TITLE, null));
		}

		return null;
	}

	private void saveLastNavigationOption()
	{
		if (m_navigationOption != null)
		{
			getActivity().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
					.putInt(PREFERENCE_NAVIGATION_OPTION_YEAR, m_navigationOption.year)
					.putBoolean(PREFERENCE_NAVIGATION_OPTION_STARRED_ONLY, m_navigationOption.starredOnly)
					.putString(PREFERENCE_NAVIGATION_OPTION_TITLE, m_navigationOption.title)
					.commit();
		}
	}

	private void loadNavigationOptions()
	{
		m_navigationOptions = new ArrayList<NavigationOption>();
		for (int year : SessionsManager.getInstance(getActivity()).getYears())
		{
			m_navigationOptions.add(new NavigationOption(year, false, getResources().getString(R.string.sessions_navigation_year_format, year)));
			m_navigationOptions.add(new NavigationOption(year, true, getResources().getString(R.string.sessions_navigation_year_starred_format, year)));
		}
	}

	private BaseAdapter createNavigationListAdapter()
	{
		return new BaseAdapter()
		{
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				if (convertView == null)
					convertView = getActivity().getLayoutInflater().inflate(R.layout.action_bar_spinner_title_list_item, parent, false);

				TextView text = (TextView) convertView.findViewById(android.R.id.text1);
				text.setText(m_navigationOptions.get(position).title);

				return convertView;
			}

			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent)
			{
				if (convertView == null)
					convertView = getActivity().getLayoutInflater().inflate(R.layout.action_bar_spinner_list_item, parent, false);

				TextView text = (TextView) convertView.findViewById(android.R.id.text1);
				text.setText(m_navigationOptions.get(position).title);

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
				return m_navigationOptions.get(position);
			}

			@Override
			public int getCount()
			{
				return m_navigationOptions.size();
			}
		};
	}

	private final class SessionsListPagerAdapter extends FragmentStatePagerAdapter
	{
		public SessionsListPagerAdapter(FragmentManager fm)
		{
			super(fm);

			loadDays();
		}

		@Override
		public void notifyDataSetChanged()
		{
			loadDays();

			super.notifyDataSetChanged();
		}

		@Override
		public int getItemPosition(Object object)
		{
			SessionsListFragment fragment = (SessionsListFragment) object;

			int position = m_days.indexOf(fragment.getDate());
			if (position != -1 && m_starredOnly == fragment.starredOnly())
				return position;
			else
				return POSITION_NONE;
		}

		@Override
		public Fragment getItem(int i)
		{
			return SessionsListFragment.newInstance(m_days.get(i), m_starredOnly);
		}

		@Override
		public int getCount()
		{
			return m_days.size();
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			// position can be > 0 if, for example, we went to page 2 then removed this page from the result list (e.g. by filtering to My Sessions)
			if (position < m_days.size())
				return SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM).format(m_days.get(position));

			return "";
		}

		private void loadDays()
		{
			if (m_navigationOption == null)
			{
				m_days = new ArrayList<Date>();
			}
			else
			{
				SessionsManager manager = SessionsManager.getInstance(getActivity());
				m_days = manager.getSessionDaysForYear(m_navigationOption.year, m_navigationOption.starredOnly);
				m_starredOnly = m_navigationOption.starredOnly;
			}

			m_noDaysTextView.setVisibility(m_days.size() == 0 ? View.VISIBLE : View.GONE);
		}

		private boolean m_starredOnly;
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
				m_navigationListAdapter.notifyDataSetChanged();
				m_sessionsListPagerAdapter.notifyDataSetChanged();
			}
		}
	}

	private static final class NavigationOption
	{
		public static NavigationOption createFromBundle(Bundle savedBundle)
		{
			return new NavigationOption(savedBundle.getInt(KEY_YEAR), savedBundle.getBoolean(KEY_STARRED_ONLY), savedBundle.getString(KEY_TITLE));
		}

		public NavigationOption(int year, boolean starredOnly, String title)
		{
			this.year = year;
			this.starredOnly = starredOnly;
			this.title = title;
		}

		public Bundle saveToBundle()
		{
			Bundle bundle = new Bundle();
			bundle.putInt(KEY_YEAR, year);
			bundle.putBoolean(KEY_STARRED_ONLY, starredOnly);
			bundle.putString(KEY_TITLE, title);

			return bundle;
		}

		public final int year;

		public final boolean starredOnly;

		public final String title;

		@Override
		public boolean equals(Object o)
		{
			if (o == null || o.getClass() != this.getClass())
				return false;

			NavigationOption other = (NavigationOption) o;
			return year == other.year && starredOnly == other.starredOnly;
		}

		@Override
		public int hashCode()
		{
			return HashCodeUtility.hash(year, starredOnly);
		}

		private static final String KEY_YEAR = "Year";
		private static final String KEY_STARRED_ONLY = "StarredOnly";
		private static final String KEY_TITLE = "Title";
	}

	private static final String KEY_NAVIGATION_OPTION = "NavigationOption";
	private static final String PREFERENCES_NAME = "Sessions";
	private static final String PREFERENCE_NAVIGATION_OPTION_YEAR = "Year";
	private static final String PREFERENCE_NAVIGATION_OPTION_STARRED_ONLY = "StarredOnly";
	private static final String PREFERENCE_NAVIGATION_OPTION_TITLE = "Title";

	private UpdateSessionsService m_boundUpdateSessionsService;
	private ServiceConnection m_updateSessionsServiceConnection;
	private BroadcastReceiver m_updateSessionsReceiver;
	private SessionsListPagerAdapter m_sessionsListPagerAdapter;
	private ViewPager m_viewPager;
	private View m_noDaysTextView;
	private SwipeRefreshLayout m_swipeRefreshLayout;
	private List<NavigationOption> m_navigationOptions;
	private OnNavigationListener m_actionBarNavigationListener;
	private BaseAdapter m_navigationListAdapter;
	private NavigationOption m_navigationOption;
	private boolean m_isRefreshing;
}
