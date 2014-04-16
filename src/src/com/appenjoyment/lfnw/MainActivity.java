package com.appenjoyment.lfnw;

import android.content.Intent;
import android.content.res.Configuration;
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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import ezvcard.Ezvcard;
import ezvcard.VCard;

public class MainActivity extends ActionBarActivity
{
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;

	private CharSequence mDrawerTitle;
	private CharSequence mTitle;
	private String[] mPlanetTitles;

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
		mPlanetTitles = new String[] { "Thing1", "Thing2" };
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, mPlanetTitles));
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

		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

		// update selected item and title, then close the drawer
		mDrawerList.setItemChecked(position, true);
		setTitle(mPlanetTitles[position]);
		mDrawerLayout.closeDrawer(mDrawerList);
	}

	@Override
	public void setTitle(CharSequence title)
	{
		mTitle = title;
		getSupportActionBar().setTitle(mTitle);
	}

	/**
	 * When using the ActionBarDrawerToggle, you must call it during onPostCreate() and onConfigurationChanged()...
	 */

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggls
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
}
