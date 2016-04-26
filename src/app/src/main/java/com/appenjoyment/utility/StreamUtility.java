package com.appenjoyment.utility;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import ezvcard.util.IOUtils;

public class StreamUtility
{
	public static String readAsString(InputStream stream) throws IOException
	{
		return IOUtils.toString(new InputStreamReader(stream));
	}

	public static byte[] readAsBytes(InputStream stream) throws IOException
	{
		return IOUtils.toByteArray(stream);
	}

	public static void writeString(OutputStream stream, String value) throws IOException
	{
		BufferedWriter br = new BufferedWriter(new OutputStreamWriter(stream));
		br.write(value);
		br.flush();
	}

	public static boolean writeToFile(File file, byte[] bytes)
	{
		file.getParentFile().mkdirs();
		FileOutputStream os = null;
		try
		{
			os = new FileOutputStream(file);
			os.write(bytes);
			os.flush();

			return true;
		}
		catch (IOException e)
		{
			Log.w(TAG, "Couldn't write to file", e);
			return false;
		}
		finally
		{
			if(os != null)
				IOUtils.closeQuietly(os);
		}
	}

	private final static String TAG = "StreamUtility";
}
