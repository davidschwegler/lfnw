package com.appenjoyment.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamUtility
{
	public static String readAsString(InputStream stream) throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(stream));

		StringBuffer sb = new StringBuffer();
		String inputLine = "";
		while ((inputLine = br.readLine()) != null) {
			sb.append(inputLine);
		}

		return sb.toString();
	}
}
