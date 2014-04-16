package com.appenjoyment.lfnw;

import java.util.ArrayList;
import java.util.List;
import android.content.Intent;
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
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.appenjoyment.lfnw.main.MainFeature;
import com.appenjoyment.lfnw.main.MainFeatureInfo;
import com.appenjoyment.utility.DisplayMetricsUtility;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import ezvcard.Ezvcard;
import ezvcard.VCard;

public class MainActivity extends ActionBarActivity
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

	// @Override
	// protected void onCreate(Bundle savedInstanceState)
	// {
	// super.onCreate(savedInstanceState);
	// setContentView(R.layout.activity_main);
	//
	// if (savedInstanceState == null)
	// getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new MainFragment()).commit();
	// }

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTitle = mDrawerTitle = getTitle();

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

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close)
		{
			public void onDrawerClosed(View view)
			{
				getSupportActionBar().setTitle(mTitle);
				supportInvalidateOptionsMenu();
			}

			public void onDrawerOpened(View drawerView)
			{
				getSupportActionBar().setTitle(mDrawerTitle);
				supportInvalidateOptionsMenu();
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		if (savedInstanceState == null)
			selectItem(0);
	}

	// @Override
	// public boolean onCreateOptionsMenu(Menu menu)
	// {
	// MenuInflater inflater = getMenuInflater();
	// inflater.inflate(R.menu.main, menu);
	// return super.onCreateOptionsMenu(menu);
	// }
	//
	// /* Called whenever we call supportInvalidateOptionsMenu() */
	// @Override
	// public boolean onPrepareOptionsMenu(Menu menu)
	// {
	// // If the nav drawer is open, hide action items related to the content view
	// boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
	// menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
	// return super.onPrepareOptionsMenu(menu);
	// }

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (mDrawerToggle.onOptionsItemSelected(item))
			return true;
		return super.onOptionsItemSelected(item);

		// // Handle action buttons
		// switch (item.getItemId())
		// {
		// case R.id.action_websearch:
		// // create intent to perform web search for this planet
		// Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
		// intent.putExtra(SearchManager.QUERY, getSupportActionBar().getTitle());
		// // catch event that there's no activity to handle intent
		// if (intent.resolveActivity(getPackageManager()) != null)
		// {
		// startActivity(intent);
		// } else
		// {
		// Toast.makeText(this, R.string.app_not_available, Toast.LENGTH_LONG).show();
		// }
		// return true;
		// default:
		// return super.onOptionsItemSelected(item);
		// }
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
		// update the main content by replacing fragments
		Fragment fragment = new MainFragment();

		MainFeatureInfo featureInfo = mFeatures[position];
		switch (featureInfo.Feature)
		{
		case About:
			fragment = AboutFragment.newInstance();
			break;
		case Register:
			// fragment = WebViewFragment.newInstance("http://www.linuxfestnorthwest.org/node/2977/cod_registration");
			startActivity(new Intent(this, WebViewActivity.class).
					putExtra(WebViewActivity.KEY_URL, "http://www.linuxfestnorthwest.org/node/2977/cod_registration"));
			break;
		case Scan:
			IntentIntegrator scannerIntent = new IntentIntegrator(this);
			scannerIntent.initiateScan(IntentIntegrator.QR_CODE_TYPES);
			break;
		case Sessions:
			fragment = SessionsFragment.newInstance();
			break;
		case Sponsors:
			// fragment = WebViewFragment.newInstance("http://linuxfestnorthwest.org/sponsors");
			startActivity(new Intent(this, WebViewActivity.class).
					putExtra(WebViewActivity.KEY_URL, "http://linuxfestnorthwest.org/sponsors"));
			break;
		case Venue:
			// fragment = WebViewFragment.newInstance("http://linuxfestnorthwest.org/information/venue");
			startActivity(new Intent(this, WebViewActivity.class).
					putExtra(WebViewActivity.KEY_URL, "http://linuxfestnorthwest.org/information/venue"));
			break;
		default:
			break;

		}

		if (fragment != null)
		{
			FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
		}

		// update selected item and title, then close the drawer
		mDrawerList.setItemChecked(position, true);
		setTitle(mFeatures[position].TitleId);
		mDrawerLayout.closeDrawer(mDrawerList);
	}

	@Override
	public void setTitle(CharSequence title)
	{
		mTitle = title;
		getSupportActionBar().setTitle(mTitle);
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

	public void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (scanResult != null && !TextUtils.isEmpty(scanResult.getContents()))
		{
			VCard vcard = Ezvcard.parse(scanResult.getContents()).first();
			if (vcard != null)
			{
				startActivity(VCardContactUtility.createAddContactIntent(vcard));
			}
			else
			{
				Log.e(TAG, "vcard failed to parse");
				Toast.makeText(this, "No contact found", Toast.LENGTH_SHORT).show();
			}
		}
	}

	private static final String TAG = "MainActivity";

	private final MainFeatureInfo[] mFeatures;

	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private CharSequence mDrawerTitle;
	private CharSequence mTitle;
}
