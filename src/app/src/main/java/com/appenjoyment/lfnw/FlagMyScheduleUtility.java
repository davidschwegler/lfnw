package com.appenjoyment.lfnw;

public class FlagMyScheduleUtility
{
	/**
	 * ASSUMES URL HAS BEEN VALIDATED BY isFlaggedOnServer
	 */
	public static String flipUrl(String currentUrl, boolean isFlaggedOnServer)
	{
		return isFlaggedOnServer ? FLAG_START + currentUrl.substring(UNFLAG_START.length()) : UNFLAG_START + currentUrl.substring(FLAG_START.length());
	}

	public static Boolean isFlaggedOnServer(String url)
	{
		if (url != null)
		{
			if (url.startsWith(UNFLAG_START))
				return true;

			if (url.startsWith(FLAG_START))
				return false;
		}

		return null;
	}

	private static final String UNFLAG_START = "/flag/unflag/";
	private static final String FLAG_START = "/flag/flag/";
}
