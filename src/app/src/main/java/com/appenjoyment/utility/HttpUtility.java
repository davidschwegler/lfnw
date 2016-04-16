package com.appenjoyment.utility;

import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpUtility
{
	public static URL createURL(String urlString)
	{
		try
		{
			return new URL(urlString);
		}
		catch (MalformedURLException e)
		{
			Log.w(TAG, "Should never happen", e);
			return null;
		}
	}

	public static Pair<Boolean, String> getStringResponse(String urlString)
	{
		URL url = createURL(urlString);
		if (url == null)
			return Pair.create(false, null);

		String json;
		HttpURLConnection urlConnection = null;
		try
		{
			urlConnection = (HttpURLConnection) url.openConnection();
			json = StreamUtility.readAsString(urlConnection.getInputStream());
		}
		catch (IOException e)
		{
			Log.w(TAG, "IOException while getting content stream", e);
			return Pair.create(false, null);
		}
		finally
		{
			if (urlConnection != null)
				urlConnection.disconnect();
		}

		return Pair.create(true, json);
	}

	private static final String TAG = "HttpUtility";
}
