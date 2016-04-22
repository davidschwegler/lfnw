package com.appenjoyment.lfnw.tickets;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.appenjoyment.lfnw.IDrawerActivity;
import com.appenjoyment.lfnw.IDrawerFragment;
import com.appenjoyment.lfnw.R;
import com.appenjoyment.lfnw.SwipeRefreshLayoutUtility;
import com.appenjoyment.lfnw.WebViewActivity;
import com.appenjoyment.lfnw.accounts.AccountManager;
import com.appenjoyment.lfnw.signin.SignInActivity;
import com.appenjoyment.utility.HttpUtility;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class TicketsFragment extends Fragment implements IDrawerFragment
{
	private SwipeRefreshLayout m_swipeRefreshLayout;

	public static TicketsFragment newInstance()
	{
		return new TicketsFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.tickets, container, false);

		m_swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
		SwipeRefreshLayoutUtility.applyTheme(m_swipeRefreshLayout);
		m_swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
		{
			@Override
			public void onRefresh()
			{
				if (m_updateTicketsTask != null)
				{
					m_updateTicketsTask.cancel(false);
					m_updateTicketsTask = null;
				}

				m_swipeRefreshLayout.setRefreshing(false);
				refresh();
			}
		});

		m_messageLink = (TextView) view.findViewById(R.id.message_link);
		m_ticketsViewPager = (ViewPager) view.findViewById(R.id.tickets_view_pager);
		m_ticketsPagerAdapter = new TicketsPagerAdapter(getChildFragmentManager());
		m_ticketsViewPager.setAdapter(m_ticketsPagerAdapter);
		m_ticketsViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
		{
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
			{
			}

			@Override
			public void onPageSelected(int position)
			{
			}

			@Override
			public void onPageScrollStateChanged(int state)
			{
				m_swipeRefreshLayout.setEnabled(state == ViewPager.SCROLL_STATE_IDLE);
			}
		});

		if (AccountManager.getInstance().isSignedIn())
		{
			m_messageLink.setVisibility(View.GONE);
			m_ticketsViewPager.setVisibility(View.VISIBLE);

			reloadTickets();
			refresh();
		}
		else
		{
			m_swipeRefreshLayout.setEnabled(false);

			m_messageLink.setText("Sign in to view or\nget your free event tickets!");
			m_messageLink.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (!AccountManager.getInstance().isSignedIn())
						startActivity(new Intent(getActivity(), SignInActivity.class));
				}
			});
		}

		setHasOptionsMenu(true);

		return view;
	}

	@Override
	public void onResume()
	{
		super.onResume();

		if (AccountManager.getInstance().isSignedIn() && m_updateTicketsTask == null && m_ticketsPagerAdapter.getCount() == 0)
			refresh();
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroy();

		if (m_updateTicketsTask != null)
		{
			m_updateTicketsTask.cancel(false);
			m_updateTicketsTask = null;
		}

		m_closed = true;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		if (AccountManager.getInstance().isSignedIn() && (!(getActivity() instanceof IDrawerActivity) || !((IDrawerActivity) getActivity()).isDrawerOpen()))
			inflater.inflate(R.menu.tickets, menu);
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
	}

	@Override
	public void onDrawerClosed()
	{
		getActivity().setTitle(R.string.tickets_title);
		getActivity().supportInvalidateOptionsMenu();
	}

	private void refresh()
	{
		m_updateTicketsTask = new AsyncTask<Void, Void, Boolean>()
		{
			@Override
			protected void onPreExecute()
			{
				m_swipeRefreshLayout.setRefreshing(true);
			}

			@Override
			protected Boolean doInBackground(Void... params)
			{
				int userId = AccountManager.getInstance().getUser().userId;
				String url = "https://www.linuxfestnorthwest.org/user/" + userId + "/tickets.json?entity_id=3835";

				Pair<Boolean, String> response = HttpUtility.getStringResponse(url);
				if (response != null && response.first)
				{
					if (isCancelled())
						return null;

					TicketsData data = TicketsData.parseFromJson(response.second);
					if (data != null)
					{
						int year = new GregorianCalendar().get(Calendar.YEAR);
						List<TicketData> yearTickets = new ArrayList<>();
						for (TicketData ticket : data.tickets)
						{
							if (ticket.year == year)
								yearTickets.add(ticket);
						}

						if (isCancelled())
							return null;

						TicketsManager.getInstance().insertOrUpdate(yearTickets);
						return true;
					}
				}

				return null;
			}

			@Override
			protected void onPostExecute(Boolean result)
			{
				if (!isCancelled() && !m_closed)
				{
					m_swipeRefreshLayout.setRefreshing(false);
					m_updateTicketsTask = null;

					if (result != null && result.booleanValue())
						reloadTickets();
					else
						Toast.makeText(getActivity(), "Couldn't refresh tickets - check your internet connection", Toast.LENGTH_SHORT).show();
				}
			}
		}.execute();
	}

	private void reloadTickets()
	{
		List<TicketData> tickets = TicketsManager.getInstance().getTicketsForYear(new GregorianCalendar().get(GregorianCalendar.YEAR));

		m_ticketsPagerAdapter.setTickets(tickets);

		if (tickets.size() != 0)
		{
			m_ticketsViewPager.setVisibility(View.VISIBLE);
			m_messageLink.setVisibility(View.GONE);
		}
		else
		{
			m_ticketsViewPager.setVisibility(View.GONE);
			m_messageLink.setVisibility(View.VISIBLE);
			m_messageLink.setText("Looks like you don't have tickets just yet.\nTap for event ticket registration!");
			m_messageLink.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					startActivity(new Intent(getActivity(), WebViewActivity.class)
							.putExtra(WebViewActivity.KEY_TITLE, getString(R.string.register_title))
							.putExtra(WebViewActivity.KEY_URL, "https://www.linuxfestnorthwest.org/2016/registration"));
				}
			});
		}
	}

	// TODO: I'd rather do FragmentPagerAdapter, but the images stop showing up after paging thru once...
	private class TicketsPagerAdapter extends FragmentStatePagerAdapter
	{
		public TicketsPagerAdapter(FragmentManager fm)
		{
			super(fm);

			m_tickets = new ArrayList<>();
		}

		@Override
		public int getCount()
		{
			return m_tickets.size();
		}

		@Override
		public int getItemPosition(Object object)
		{
			return POSITION_NONE;
		}

		@Override
		public Fragment getItem(int position)
		{
			return TicketFragment.newInstance(m_tickets.get(position)._id, position, m_tickets.size());
		}

		public void setTickets(List<TicketData> tickets)
		{
			m_tickets = tickets;
			notifyDataSetChanged();
		}

		private List<TicketData> m_tickets;
	}

	private boolean m_closed;
	private ViewPager m_ticketsViewPager;
	private TextView m_messageLink;
	private TicketsPagerAdapter m_ticketsPagerAdapter;
	private AsyncTask<?, ?, ?> m_updateTicketsTask;
}
