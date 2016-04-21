package com.appenjoyment.lfnw.accounts;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * E.g.
 * <p/>
 * 2016
 * <p/>
 * <pre>
 * {
 * "sessid": "F2FBqnQVzlr5fXMJF1NSu-Ug7AS9ZjIJ6PtmdPB1PQk",
 * "session_name": "SSESS7266919d7c0f4fdb089f61af84915da9",
 * "token": "0BUjUYonhBx6R4DFDr16sIsoh_uq60wCtFFvpA5fbB4",
 * "user": {
 * "uid": "4395",
 * "name": "DavidSchwegler",
 * "mail": "dschwegler777@gmail.com",
 * "picture": {
 * "url": "https:\/\/www.linuxfestnorthwest.org\/sites\/default\/files\/pictures\/picture-4395-1363766752.jpg"
 * },
 * "field_profile_first": {
 * "und": [
 * {
 * "value": "David"
 * }
 * ]
 * },
 * "field_profile_last": {
 * "und": [
 * {
 * "value": "Schwegler"
 * }
 * ]
 * }
 * },
 * </pre>
 */
public class SignInResponseData
{
	public Account account;
	public User user;

	public static SignInResponseData parseFromJson(String in)
	{
		try
		{
			SignInResponseData response = new SignInResponseData();

			JSONObject containerObject = new JSONObject(in);

			response.account = new Account();
			response.account.sessionId = containerObject.getString("sessid");
			response.account.sessionName = containerObject.getString("session_name");
			response.account.token = containerObject.getString("token");

			JSONObject userObject = containerObject.getJSONObject("user");
			response.user = new User();
			response.user.userId = Integer.parseInt(userObject.getString("uid"));
			response.user.userName = userObject.getString("name");
			response.user.email = userObject.getString("mail");

			// allow this stuff to fail and us to still accept the login
			parseNonCriticalDataFromJson(userObject, response);

			return response;
		}
		catch (JSONException e)
		{
			// all-or-nothing for now
			Log.e(TAG, "Error parsing sign in response Json", e);
			return null;
		}
		catch (ClassCastException e)
		{
			// all-or-nothing for now
			Log.e(TAG, "Error parsing sign in response Json", e);
			return null;
		}
		catch (NumberFormatException e)
		{
			// all-or-nothing for now
			Log.e(TAG, "Error parsing sign in response Json", e);
			return null;
		}
	}

	private static void parseNonCriticalDataFromJson(JSONObject userObject, SignInResponseData response)
	{
		// since this is such a critical path, be as lenient as possible, but if all else fails, we can ignore this stuff
		try
		{
			if (userObject.has("picture") && !userObject.isNull("picture"))
			{
				JSONObject pictureObject = userObject.getJSONObject("picture");
				if (pictureObject.has("url") && !pictureObject.isNull("url"))
				{
					response.user.avatarUrl = pictureObject.getString("url");
					if (response.user.avatarUrl != null)
						response.user.avatarUrl = response.user.avatarUrl.replace("\\/", "/");
				}
			}

			if (userObject.has("field_profile_first") && !userObject.isNull("field_profile_first"))
			{
				JSONObject firstNameObject = userObject.getJSONObject("field_profile_first");
				if (firstNameObject.has("und") && !firstNameObject.isNull("und"))
				{
					JSONArray firstNameArray = firstNameObject.getJSONArray("und");
					if (firstNameArray.length() > 0)
					{
						JSONObject firstNameArrayObject = firstNameArray.getJSONObject(0);
						if (firstNameArrayObject.has("value") && !firstNameArrayObject.isNull("value"))
							response.user.firstName = firstNameArrayObject.getString("value");
					}
				}
			}

			if (userObject.has("field_profile_last") && !userObject.isNull("field_profile_last"))
			{
				JSONObject lastNameObject = userObject.getJSONObject("field_profile_last");
				if (lastNameObject.has("und") && !lastNameObject.isNull("und"))
				{
					JSONArray lastNameArray = lastNameObject.getJSONArray("und");
					if (lastNameArray.length() > 0)
					{
						JSONObject lastNameArrayObject = lastNameArray.getJSONObject(0);
						if (lastNameArrayObject.has("value") && !lastNameArrayObject.isNull("value"))
							response.user.lastName = lastNameArrayObject.getString("value");
					}
				}
			}
		}
		catch (JSONException e)
		{
			// all-or-nothing for now
			Log.e(TAG, "Error parsing non-critical sign-in response Json", e);
		}
		catch (ClassCastException e)
		{
			// all-or-nothing for now
			Log.e(TAG, "Error parsing non-critical sign-in response Json", e);
		}
		catch (NumberFormatException e)
		{
			// all-or-nothing for now
			Log.e(TAG, "Error parsing non-critical sign-in response Json", e);
		}
	}

	private static final String TAG = "SessionData";
}
