package com.appenjoyment.lfnw.accounts;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.appenjoyment.lfnw.OurApp;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;

/**
 * Maintains the users credentials and info.
 */
public final class AccountManager
{
	public static synchronized AccountManager getInstance()
	{
		if (s_instance == null)
			s_instance = new AccountManager();

		return s_instance;
	}

	public void init()
	{
		CookieManager cookieManager = new CookieManager();
		CookieHandler.setDefault(cookieManager);

		Account account = getAccount();
		if (account != null)
		{
			setupCookies(account);
			Log.i(TAG, "Loaded info");
		}

		Log.i(TAG, "No account info");
	}

	public void setLogin(Account account, User user)
	{
		SharedPreferences prefs = OurApp.getInstance().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();

		editor.putString("SessionId", account.sessionId);
		editor.putString("SessionName", account.sessionName);
		editor.putString("Token", account.token);

		editor.putInt("UserId", user.userId);
		editor.putString("Username", user.userName);
		editor.putString("Email", user.email);
		editor.putString("FirstName", user.firstName);
		editor.putString("LastName", user.lastName);
		editor.putString("AvatarUrl", user.avatarUrl);

		editor.commit();

		setupCookies(account);
	}

	public boolean isSignedIn()
	{
		SharedPreferences prefs = OurApp.getInstance().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
		return prefs.contains("SessionId");
	}

	public Account getAccount()
	{
		SharedPreferences prefs = OurApp.getInstance().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
		if (!prefs.contains("SessionId"))
			return null;

		Account authentication = new Account();
		authentication.sessionId = prefs.getString("SessionId", null);
		authentication.sessionName = prefs.getString("SessionName", null);
		authentication.token = prefs.getString("Token", null);

		return authentication;
	}

	public User getUser()
	{
		SharedPreferences prefs = OurApp.getInstance().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
		if (!prefs.contains("UserId"))
			return null;

		User user = new User();
		user.userId = prefs.getInt("UserId", -1);
		user.userName = prefs.getString("Username", null);
		user.email = prefs.getString("Email", null);
		user.firstName = prefs.getString("FirstName", null);
		user.lastName = prefs.getString("LastName", null);
		user.avatarUrl = prefs.getString("AvatarUrl", null);

		return user;
	}

	private void setupCookies(Account account)
	{
		String domain = ".linuxfestnorthwest.org";
		HttpCookie cookie = new HttpCookie(account.sessionName, account.sessionId);
		cookie.setDomain(domain);

		// setVersion(0) is required for linuxfestnorthwest.org to work, probably for the reasons described at
		// Sessions With Cookies on http://developer.android.com/reference/java/net/HttpURLConnection.html
		cookie.setVersion(0);
		((CookieManager) CookieHandler.getDefault()).getCookieStore().add(null, cookie);
	}

	private static final String TAG = "AccountManager";
	private static final String PREFERENCES_NAME = "AccountManager";
	private static AccountManager s_instance;
}
