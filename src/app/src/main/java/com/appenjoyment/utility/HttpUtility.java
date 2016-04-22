package com.appenjoyment.utility;

import android.support.annotation.StringRes;
import android.util.Log;
import android.util.Pair;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class HttpUtility
{
	public  static void setTimeouts(HttpURLConnection connection)
	{
		connection.setConnectTimeout(15000);
		connection.setReadTimeout(15000);
	}

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

		String result;
		HttpURLConnection urlConnection = null;
		try
		{
			urlConnection = (HttpURLConnection) url.openConnection();
			setTimeouts(urlConnection);
			result = StreamUtility.readAsString(urlConnection.getInputStream());
		}
		catch (IOException e)
		{
			Log.w(TAG, "getStringResponse: IOException", e);
			return Pair.create(false, null);
		}
		finally
		{
			if (urlConnection != null)
				urlConnection.disconnect();
		}

		return Pair.create(true, result);
	}

	public static Pair<Boolean, byte[]> getByteResponse(String urlString)
	{
		URL url = createURL(urlString);
		if (url == null)
			return Pair.create(false, null);

		byte[] result;
		HttpURLConnection urlConnection = null;
		try
		{
			urlConnection = (HttpURLConnection) url.openConnection();
			setTimeouts(urlConnection);
			result = StreamUtility.readAsBytes(urlConnection.getInputStream());
		}
		catch (IOException e)
		{
			Log.w(TAG, "getByteResponse: IOException", e);
			return Pair.create(false, null);
		}
		finally
		{
			if (urlConnection != null)
				urlConnection.disconnect();
		}

		return Pair.create(true, result);
	}

	public static class StringResponse
	{
		public int responseCode;

		public String result;
	}

	public static StringResponse sendPost(String urlString, String postContent)
	{
		URL url = createURL(urlString);
		if (url == null)
			return null;

		StringResponse response = new StringResponse();

		HttpURLConnection urlConnection = null;
		try
		{
			urlConnection = (HttpURLConnection) url.openConnection();
			setTimeouts(urlConnection);

			try
			{
				urlConnection.setRequestMethod("POST");
			}
			catch(ProtocolException e)
			{
				// will never happen
			}

			urlConnection.setDoOutput(true);
			urlConnection.setFixedLengthStreamingMode(postContent.getBytes().length);
			urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
			urlConnection.connect();

			StreamUtility.writeString(urlConnection.getOutputStream(), postContent);

			response.result = StreamUtility.readAsString(urlConnection.getInputStream());
			response.responseCode = urlConnection.getResponseCode();
		}
		catch (IOException e)
		{
			Log.w(TAG, "sendPost: IOException", e);
			try
			{
				response.responseCode = urlConnection.getResponseCode();
			}
			catch (IOException e2)
			{
				Log.w(TAG, "sendPost: IOException getting response code after IOException - should never happen", e2);
				return null;
			}
		}
		finally
		{
			if (urlConnection != null)
				urlConnection.disconnect();
		}

		return response;
	}

	private static final String TAG = "HttpUtility";
}
