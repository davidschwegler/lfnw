package com.appenjoyment.lfnw;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import com.appenjoyment.utility.HttpUtility;
import com.appenjoyment.utility.StreamUtility;

public class UpdateSessionsService extends Service
{
	/**
	 * Broadcast action indicating an update has started.
	 */
	public static final String UPDATE_STARTED_ACTION = UpdateSessionsService.class.getCanonicalName() + ".UPDATE_STARTED";

	/**
	 * Broadcast action indicating an update has completed.
	 */
	public static final String UPDATE_COMPLETED_ACTION = UpdateSessionsService.class.getCanonicalName() + ".UPDATE_COMPLETED";

	/**
	 * Extra for sending the START_KIND.
	 */
	public static final String EXTRA_START_KIND = "startKind";

	/**
	 * Force an update of the sessions list
	 */
	public static final String START_KIND_FORCED = "forced";

	/**
	 * Update the sessions list if it's been long enough
	 */
	public static final String START_KIND_INTERVAL = "interval";

	/**
	 * Binder for clients to interact with us.
	 */
	public final class UpdateSessionsBinder extends Binder
	{
		UpdateSessionsService getService()
		{
			return UpdateSessionsService.this;
		}
	}

	public UpdateSessionsService()
	{
		m_binder = new UpdateSessionsBinder();
	}

	public boolean isUpdating()
	{
		return m_currentTask != null;
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return m_binder;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		// TODO: how to notify clients if task is killed? can I send the nessecary broadcasts here?
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if (m_currentTask == null)
		{
			// unless we were forced, don't start unless it's been long enough since our last successful update
			boolean start = START_KIND_FORCED.equals(intent.getStringExtra(EXTRA_START_KIND));
			if (!start)
			{
				SharedPreferences prefs = getSharedPreferences("UpdateSessionsService", MODE_PRIVATE);
				start = new Date().getTime() - prefs.getLong(PREF_LAST_SUCCESSFUL_UPDATE, 0) >= INTERVAL_UPDATE_DURATION;
			}

			if (start)
			{
				m_currentTask = new UpdateSessionsTask();
				m_currentTask.execute();
			}
		}

		return START_NOT_STICKY;
	}

	private class UpdateSessionsTask extends AsyncTask<Void, Void, Boolean>
	{
		@Override
		protected Boolean doInBackground(Void... params)
		{
			Log.i(TAG, "Starting sessions list update");
			sendBroadcast(new Intent(UPDATE_STARTED_ACTION));

			// load the json stream into a string
			Pair<Boolean, String> jsonResult = HttpUtility.getStringResponse("https://www.linuxfestnorthwest.org/2016/schedule.json");
			if(!jsonResult.first)
				return false;

			// parse the json
			List<SessionData> sessionData = SessionData.parseFromJson(jsonResult.second);
			if (sessionData == null)
				return false;

			// insert/update the records
			if (sessionData.size() != 0)
				SessionsManager.getInstance(UpdateSessionsService.this).insertOrUpdate(sessionData);

			return true;
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			m_currentTask = null;

			// remember that we succesfully updated now
			if (result != null && result.booleanValue())
			{
				SharedPreferences prefs = getSharedPreferences("UpdateSessionsService", MODE_PRIVATE);
				prefs.edit().putLong(PREF_LAST_SUCCESSFUL_UPDATE, new Date().getTime()).commit();
				Log.i(TAG, "Finished sessions list update success=true");
			} else
			{
				Log.i(TAG, "Finished sessions list update success=false");
			}

			sendBroadcast(new Intent(UPDATE_COMPLETED_ACTION));
		}

	}

	private static final String TAG = "UpdateSessionsService";
	private static final int INTERVAL_UPDATE_DURATION = 1000 * 60 * 10;
	private static final String PREF_LAST_SUCCESSFUL_UPDATE = "LastSuccessfulUpdate";

	private UpdateSessionsTask m_currentTask;
	private UpdateSessionsBinder m_binder;
}
