package com.appenjoyment.lfnw;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

public class WebViewActivity extends ActionBarActivity
{
	public static String KEY_TITLE = "KEY_TITLE";
	public static String KEY_URL = "KEY_URL";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		setContentView(R.layout.activity_web_view);

		String title = getIntent().getStringExtra(KEY_TITLE);
		if (title == null || title.length() == 0)
			throw new IllegalArgumentException("No Title");
		getSupportActionBar().setTitle(title);

		if (savedInstanceState == null)
		{
			String url = getIntent().getStringExtra(KEY_URL);
			if (url == null || url.length() == 0)
				throw new IllegalArgumentException("No Url");

			getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, WebViewFragment.newInstance(url)).commit();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// respond to the action bar's Up/Home button
		// I don't think we have to do this if we specify the parent in the manifest, but since this is a generic activity we sacrifice that capability
		if (item.getItemId() == android.R.id.home)
			finish();

		return super.onOptionsItemSelected(item);
	}
}
