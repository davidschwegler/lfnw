package com.appenjoyment.lfnw;

import android.app.Application;

import com.appenjoyment.lfnw.accounts.AccountManager;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

public final class OurApp extends Application
{
	@Override
	public void onCreate()
	{
		super.onCreate();

		s_instance = this;
		AccountManager.getInstance().init();
	}

	public static OurApp getInstance()
	{
		return s_instance;
	}

	synchronized public Tracker getDefaultTracker()
	{
		if (mTracker == null)
		{
			GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);

			if (BuildConfig.DEBUG)
				analytics.setDryRun(true);

			mTracker = analytics.newTracker(R.xml.global_tracker);
		}
		return mTracker;
	}

	private static OurApp s_instance;
	private Tracker mTracker;
}
