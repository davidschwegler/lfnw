package com.appenjoyment.lfnw;

import android.app.Application;
import com.appenjoyment.lfnw.accounts.AccountManager;

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

	private static OurApp s_instance;
}
