package com.appenjoyment.lfnw;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.appenjoyment.lfnw.main.MainFeature;
import com.appenjoyment.lfnw.main.MainFeatureInfo;
import com.appenjoyment.utility.DisplayMetricsUtility;

public class MainActivity extends ActionBarActivity implements IDrawerActivity
{
	public MainActivity()
	{
		List<MainFeatureInfo> features = new ArrayList<MainFeatureInfo>();
		features.add(new MainFeatureInfo(MainFeature.Sessions, R.string.sessions_title, R.drawable.ic_sessions_calendar_blank));
		features.add(new MainFeatureInfo(MainFeature.Scan, R.string.scan_badge_title, R.drawable.ic_scan_contact));
		features.add(new MainFeatureInfo(MainFeature.Venue, R.string.venue_title, R.drawable.ic_venue_place));
		features.add(new MainFeatureInfo(MainFeature.Sponsors, R.string.sponsors_title, R.drawable.ic_sponsors_heart));
		features.add(new MainFeatureInfo(MainFeature.Register, R.string.register_title, R.drawable.ic_register_nametag));
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
				TextView textView;
				if (convertView != null)
					textView = (TextView) convertView;
				else
					textView = (TextView) getLayoutInflater().inflate(R.layout.drawer_list_item, parent, false);

				MainFeatureInfo featureInfo = mFeatures[position];
				textView.setText(featureInfo.TitleId);
				textView.setCompoundDrawablePadding(DisplayMetricsUtility.dpToPx(MainActivity.this, 4));
				Drawable drawable = getResources().getDrawable(featureInfo.DrawableId).mutate();
				drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
				int drawableSize = DisplayMetricsUtility.dpToPx(MainActivity.this, 32);
				drawable.setBounds(0, 0, drawableSize, drawableSize);
				textView.setCompoundDrawables(drawable, null, null, null);

				return textView;
			}

			@Override
			public int getCount()
			{
				return mFeatures.length;
			}

			@Override
			public Object getItem(int position)
			{
				return mFeatures[position];
			}

			@Override
			public long getItemId(int position)
			{
				return position;
			}
		});
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close)
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
						.apply();
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		if (savedInstanceState == null)
			doSelectItem(0);

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

	private class DrawerItemClickListener implements ListView.OnItemClickListener
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			selectItem(position);
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

		MainFeatureInfo featureInfo = mFeatures[position];
		switch (featureInfo.Feature)
		{
		case About:
			fragment = AboutFragment.newInstance();
			break;
		case Register:
			fragment = WebViewFragment.newInstance("http://linuxfestnorthwest.org/node/2977/cod_registration", getResources().getString(featureInfo.TitleId));
			break;
		case Scan:
			fragment = ScanBadgeFragment.newInstance();
			break;
		case Sessions:
			fragment = SessionsFragment.newInstance();
			break;
		case Sponsors:
			fragment = WebViewFragment.newInstance("http://linuxfestnorthwest.org/sponsors", getResources().getString(featureInfo.TitleId));
			break;
		case Venue:
			fragment = WebViewFragment.newInstance("http://linuxfestnorthwest.org/information/venue", getResources().getString(featureInfo.TitleId));
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

	private static final String PREFERENCE_USER_CLOSED_DRAWER = "UserClosedDrawer";
	private static final String PREFERENCES_NAME = "MainActivity";

	private final MainFeatureInfo[] mFeatures;

	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private CharSequence mDrawerTitle;
}
