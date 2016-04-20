package com.appenjoyment.lfnw;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.appenjoyment.lfnw.accounts.AccountManager;
import com.appenjoyment.lfnw.accounts.User;
import com.appenjoyment.lfnw.main.MainFeature;
import com.appenjoyment.lfnw.main.MainFeatureInfo;
import com.appenjoyment.lfnw.signin.SignInActivity;
import com.appenjoyment.lfnw.tickets.TicketsFragment;
import com.appenjoyment.utility.DisplayMetricsUtility;
import com.appenjoyment.utility.HttpUtility;
import com.appenjoyment.utility.StreamUtility;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ActionBarActivity implements IDrawerActivity
{
	public MainActivity()
	{
		List<MainFeatureInfo> features = new ArrayList<MainFeatureInfo>();
		features.add(new MainFeatureInfo(MainFeature.Sessions, R.string.sessions_title, R.drawable.ic_sessions_calendar_blank));
		features.add(new MainFeatureInfo(MainFeature.Scan, R.string.scan_badge_title, R.drawable.ic_scan_contact));
		features.add(new MainFeatureInfo(MainFeature.Tickets, R.string.tickets_title, R.drawable.ic_register_nametag));
		features.add(new MainFeatureInfo(MainFeature.Venue, R.string.venue_title, R.drawable.ic_venue_place));
		features.add(new MainFeatureInfo(MainFeature.Sponsors, R.string.sponsors_title, R.drawable.ic_sponsors_heart));
//		features.add(new MainFeatureInfo(MainFeature.Register, R.string.register_title, R.drawable.ic_register_nametag));
		features.add(new MainFeatureInfo(MainFeature.About, R.string.about_title, R.drawable.ic_about_info));
		mFeatures = features.toArray(new MainFeatureInfo[0]);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		setContentView(R.layout.activity_main);

		mDrawerTitle = getTitle();

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		mDrawerList.setAdapter(new BaseAdapter()
		{
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				if (position == 0)
				{
					UserViewHolder holder = convertView != null ? (UserViewHolder) convertView.getTag() : null;
					if (holder != null)
					{
						holder.avatarDownloadTask.cancel(false);
					}
					else
					{
						holder = new UserViewHolder();
						holder.view = getLayoutInflater().inflate(R.layout.drawer_user_item, parent, false);

						holder.title = (TextView) holder.view.findViewById(R.id.drawer_user_title);
						holder.subtitle = (TextView) holder.view.findViewById(R.id.drawer_user_subtitle);
						holder.avatarView = (ImageView) holder.view.findViewById(R.id.drawer_user_avatar);
					}

					if (isSignedIn())
					{
						final User user = AccountManager.getInstance().getUser();
						if (user == null)
						{
							holder.title.setVisibility(View.GONE);
							holder.subtitle.setVisibility(View.GONE);
						}
						else
						{
							final String avatarUrl = user.avatarUrl;
							final UserViewHolder finalHolder = holder;
							if (avatarUrl != null)
							{
								holder.avatarDownloadTask = new AsyncTask<Void, Void, Bitmap>()
								{
									@Override
									protected Bitmap doInBackground(Void... params)
									{
										Bitmap bitmap = null;
										File avatarFile = new File(getFilesDir(), user.userId + ".jpg");
										if (avatarFile.exists())
										{
											Log.d(TAG, "Trying to load cached avatar");
											bitmap = BitmapFactory.decodeFile(avatarFile.getAbsolutePath());

											if (bitmap == null)
												Log.w(TAG, "Couldn't load cached avatar");
										}

										if (!isCancelled() && bitmap == null)
										{
											Log.d(TAG, "Trying to download avatar");
											Pair<Boolean, byte[]> bytes = HttpUtility.getByteResponse(avatarUrl);
											if (bytes.first)
											{
												bitmap = BitmapFactory.decodeByteArray(bytes.second, 0, bytes.second.length);

												if (bitmap == null)
													Log.w(TAG, "Couldn't download avatar");
											}

											if (!isCancelled() && bitmap != null)
											{
												Log.d(TAG, "Trying to save avatar");
												if (!StreamUtility.writeToFile(avatarFile, bytes.second))
													Log.w(TAG, "Couldn't save avatar");
											}
										}

										return bitmap;
									}

									@Override
									protected void onPostExecute(Bitmap bitmap)
									{
										if (!isCancelled() && !m_closed && finalHolder.avatarDownloadTask == this && bitmap != null)
											finalHolder.avatarView.setImageBitmap(bitmap);
									}
								}.execute();
							}

							StringBuilder realName = new StringBuilder();
							if (user.firstName != null)
								realName.append(user.firstName);

							if (user.lastName != null && !user.lastName.isEmpty())
							{
								if (realName.length() != 0)
									realName.append(" ");

								realName.append(user.lastName);
							}

							String userName = user.userName != null ? user.userName : "";

							// show username/email in real name view as a fallback
							if (realName.length() == 0 && !userName.isEmpty())
							{
								realName.append(userName);
								userName = "";
							}

							if (realName.length() != 0)
							{
								holder.title.setVisibility(View.VISIBLE);
								holder.title.setText(realName.toString());
							}
							else
							{
								holder.title.setVisibility(View.GONE);
							}

							if (!userName.isEmpty())
							{
								holder.subtitle.setVisibility(View.VISIBLE);
								holder.subtitle.setText(userName);
							}
							else
							{
								holder.subtitle.setVisibility(View.GONE);
							}
						}
					}
					else
					{
						holder.title.setText("Sign in");
						holder.title.setVisibility(View.VISIBLE);
						holder.subtitle.setVisibility(View.GONE);
					}

					return holder.view;
				}
				else
				{
					// TODO: viewholder
					TextView textView = (TextView) convertView;
					if (textView == null)
						textView = (TextView) getLayoutInflater().inflate(R.layout.drawer_list_item, parent, false);

					MainFeatureInfo featureInfo = mFeatures[position - 1];
					textView.setText(featureInfo.TitleId);
					textView.setCompoundDrawablePadding(DisplayMetricsUtility.dpToPx(MainActivity.this, 4));
					Drawable drawable = getResources().getDrawable(featureInfo.DrawableId).mutate();
					drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
					int drawableSize = DisplayMetricsUtility.dpToPx(MainActivity.this, 32);
					drawable.setBounds(0, 0, drawableSize, drawableSize);
					textView.setCompoundDrawables(drawable, null, null, null);

					return textView;
				}
			}

			@Override
			public boolean areAllItemsEnabled()
			{
				return false;
			}

			@Override
			public boolean isEnabled(int position)
			{
				return position != 0 || !isSignedIn();
			}

			@Override
			public int getItemViewType(int position)
			{
				return position == 0 ? 0 : 1;
			}

			@Override
			public int getViewTypeCount()
			{
				return 2;
			}

			@Override
			public int getCount()
			{
				return mFeatures.length + 1;
			}

			@Override
			public Object getItem(int position)
			{
				return position == 0 ? null : mFeatures[position - 1];
			}

			@Override
			public long getItemId(int position)
			{
				return position;
			}

			class UserViewHolder
			{
				View view;
				TextView title;
				TextView subtitle;
				ImageView avatarView;
				AsyncTask<?, ?, ?> avatarDownloadTask;
			}
		});
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close)
		{
			public void onDrawerClosed(View view)
			{
				supportInvalidateOptionsMenu();

				IDrawerFragment drawerFragment = (IDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);
				if (drawerFragment != null)
					drawerFragment.onDrawerClosed();
			}

			public void onDrawerOpened(View drawerView)
			{
				getSupportActionBar().setTitle(mDrawerTitle);
				supportInvalidateOptionsMenu();

				IDrawerFragment drawerFragment = (IDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);
				if (drawerFragment != null)
					drawerFragment.onDrawerOpened();

				getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
						.edit()
						.putBoolean(PREFERENCE_USER_CLOSED_DRAWER, true)
						.commit();
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		if (savedInstanceState == null)
			doSelectItem(DEFAULT_FEATURE_INDEX + 1);

		if (!getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).getBoolean(PREFERENCE_USER_CLOSED_DRAWER, false))
			mDrawerLayout.openDrawer(GravityCompat.START);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (mDrawerToggle.onOptionsItemSelected(item))
			return true;
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void setTitle(CharSequence title)
	{
		getSupportActionBar().setTitle(title);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if (m_isSignedInAtLastPause != null && AccountManager.getInstance().isSignedIn() != m_isSignedInAtLastPause.booleanValue())
		{
			// restart activity
			finish();
			startActivity(new Intent(MainActivity.this, MainActivity.class));
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		m_isSignedInAtLastPause = AccountManager.getInstance().isSignedIn();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		m_closed = true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
		if (currentFragment instanceof IHandleKeyDown)
		{
			if (((IHandleKeyDown) currentFragment).onKeyDown(keyCode, event))
				return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean isDrawerOpen()
	{
		return mDrawerLayout.isDrawerOpen(GravityCompat.START);
	}

	private boolean isSignedIn()
	{
		return AccountManager.getInstance().isSignedIn();
	}

	private class DrawerItemClickListener implements ListView.OnItemClickListener
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			if (position == 0)
			{
				if (!isSignedIn())
					startActivity(new Intent(MainActivity.this, SignInActivity.class));
			}
			else
			{
				selectItem(position);
			}
		}
	}

	private void selectItem(int position)
	{
		doSelectItem(position);

		mDrawerLayout.closeDrawer(mDrawerList);
	}

	private void doSelectItem(int position)
	{
		// update the main content by replacing fragments
		Fragment fragment = null;

		MainFeatureInfo featureInfo = mFeatures[position - 1];
		switch (featureInfo.Feature)
		{
			case About:
				fragment = AboutFragment.newInstance();
				break;
			case Register:
				fragment = WebViewFragment.newInstance("https://www.linuxfestnorthwest.org/2016/registration", getResources().getString(featureInfo.TitleId));
				break;
			case Scan:
				fragment = ScanBadgeFragment.newInstance();
				break;
			case Sessions:
				fragment = SessionsFragment.newInstance();
				break;
			case Sponsors:
				fragment = WebViewFragment.newInstance("https://www.linuxfestnorthwest.org/2016/sponsors", getResources().getString(featureInfo.TitleId));
				break;
			case Tickets:
				fragment = TicketsFragment.newInstance();
				break;
			case Venue:
				fragment = WebViewFragment.newInstance("https://www.linuxfestnorthwest.org/2016/hotels", getResources().getString(featureInfo.TitleId));
				break;
			default:
				break;
		}

		// select the new tab and insert the fragment if there is one, otherwise it's just a link so don't change the tab
		if (fragment != null)
		{
			FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

			mDrawerList.setItemChecked(position, true);
		}
	}

	private static final String TAG = MainActivity.class.getName();
	private static final String PREFERENCE_USER_CLOSED_DRAWER = "UserClosedDrawer2016";
	private static final String PREFERENCES_NAME = "MainActivity";
	private static final int DEFAULT_FEATURE_INDEX = 0;

	private final MainFeatureInfo[] mFeatures;

	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private CharSequence mDrawerTitle;
	private boolean m_closed;
	private Boolean m_isSignedInAtLastPause;
}
