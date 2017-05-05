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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
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
	 * Triggers a sync update of the sessions list
	 */
	public static final String START_KIND_SYNC = "sync";

	/**
	 * Update the sessions list if it's been long enough
	 */
	public static final String START_KIND_INTERVAL = "interval";

	/**
	 * Extra for sending the START_INTERRUPT.
	 */
	public static final String EXTRA_START_INTERRUPT = "startInterrupt";

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

		// TODO: how to notify clients if task is killed? can I send the necessary broadcasts here?
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		boolean interrupt = intent.getBooleanExtra(EXTRA_START_INTERRUPT, false);
		if (m_currentTask != null && interrupt)
		{
			m_currentTask.cancel(false);
			m_currentTask = null;
		}

		boolean sync = START_KIND_SYNC.equals(intent.getStringExtra(EXTRA_START_KIND));
		if (m_currentTask == null)
		{
			// unless we were forced, don't start unless it's been long enough since our last successful update
			boolean start = sync || START_KIND_FORCED.equals(intent.getStringExtra(EXTRA_START_KIND));
			if (!start)
				start = new Date().getTime() - s_lastSuccessfulUpdateTime >= INTERVAL_UPDATE_DURATION;

			if (start)
			{
				m_currentTask = new UpdateSessionsTask();
				m_currentTask.execute();
			}
		}
		else if (sync)
		{
			m_shouldCallAgain = true;
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

			// No BOFs for 2017
////			Pair<Boolean, String> bofJsonResult = HttpUtility.getStringResponse("https://www.linuxfestnorthwest.org/2017/bofs.json");
////			if (!bofJsonResult.first)
////				return false;
////
////			if (isCancelled())
////				return null;
////
////			List<SessionData> bofSessionData = SessionData.parseFromJson(bofJsonResult.second);
////			if (bofSessionData == null)
////				return false;
////
////			if (isCancelled())
////				return null;
//
//			if (bofSessionData.size() != 0)
//			{
//				for (SessionData bofSession : bofSessionData)
//					bofSession.isBof = true;
//				SessionsManager.getInstance(UpdateSessionsService.this).insertOrUpdate(bofSessionData);
//			}
//
//			if (isCancelled())
//				return null;

			Pair<Boolean, String> jsonResult = HttpUtility.getStringResponse("https://www.linuxfestnorthwest.org/2017/schedule.json");
			if (!jsonResult.first)
				return false;

			if (isCancelled())
				return null;

			List<SessionData> sessionData = SessionData.parseFromJson(jsonResult.second);
			if (sessionData == null)
				return false;

			if (isCancelled())
				return null;

			if (sessionData.size() != 0)
				SessionsManager.getInstance(UpdateSessionsService.this).insertOrUpdate(sessionData);

			if (isCancelled())
				return null;

			// TODO: consider using mine.json
//			Pair<Boolean, String> mySessionsJsonResult = HttpUtility.getStringResponse("https://www.linuxfestnorthwest.org/2016/schedule/mine.json");
//			if (!jsonResult.first)
//				return false;

			// get flag data for rows that have a my schedule url
			List<FlaggableSessionData> flaggableSessions = new ArrayList<>();
			Cursor cursor = SessionsManager.getInstance(OurApp.getInstance()).getAllSessionsWithFlagMyScheduleUrl();
			try
			{
				while (cursor.moveToNext())
				{
					FlaggableSessionData flaggableSession = new FlaggableSessionData();
					flaggableSession.nodeId = cursor.getString(cursor.getColumnIndex(SessionsManager.Sessions.COLUMN_NAME_NODE_ID));
					flaggableSession.starred = cursor.getInt(cursor.getColumnIndex(SessionsManager.Sessions.COLUMN_NAME_STARRED)) == 1;
					flaggableSession.dirtyTime = cursor.getLong(cursor.getColumnIndex(SessionsManager.Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_DIRTY_TIME));
					flaggableSession.url = cursor.getString(cursor.getColumnIndex(SessionsManager.Sessions.COLUMN_NAME_FLAG_MY_SCHEDULE_URL));
					flaggableSessions.add(flaggableSession);
				}
			}
			finally
			{
				cursor.close();
			}

			if (isCancelled())
				return null;

			// clear out sessions which were resolved by sync down
			int notDirty = 0;
			int invalidUrls = 0;
			int matching = 0;
			int uncheckedServerFlagged = 0;
			int uncheckedStarred = 0;
			for (int i = flaggableSessions.size() - 1; i >= 0; i--)
			{
				if (isCancelled())
					return null;

				FlaggableSessionData flaggableSession = flaggableSessions.get(i);
				if (flaggableSession.dirtyTime == 0)
				{
					// not dirty is 0
					notDirty++;
					flaggableSessions.remove(flaggableSession);
					continue;
				}

				// validate url
				Boolean isFlaggedOnServer = FlagMyScheduleUtility.isFlaggedOnServer(flaggableSession.url);
				if (isFlaggedOnServer == null)
				{
					invalidUrls++;
					flaggableSessions.remove(flaggableSession);
					continue;
				}

				flaggableSession.isFlaggedOnServer = isFlaggedOnServer.booleanValue();
				if (resolveIfMatching(flaggableSession))
				{
					matching++;
					Log.v(TAG, "Resolving matching " + flaggableSession.nodeId);
					flaggableSessions.remove(flaggableSession);
					continue;
				}

				if (flaggableSession.dirtyTime == -1)
				{
					// never checked is -1 -> use starred values on both
					if (flaggableSession.starred)
					{
						Log.v(TAG, "Resolving unchecked " + flaggableSession.nodeId + " using server flagged=" + flaggableSession.isFlaggedOnServer);
						uncheckedServerFlagged++;
						resolveUsingServer(flaggableSession);
					}
					else
					{
						Log.v(TAG, "Resolving unchecked " + flaggableSession.nodeId + " using our starred=" + flaggableSession.starred);
						uncheckedStarred++;
						resolveUsingOurs(flaggableSession);
					}

					flaggableSessions.remove(flaggableSession);
					continue;
				}
			}

			Log.w(TAG, "Syncing up " + flaggableSessions.size());
			int syncUps = 0;
			int failedSyncUps = 0;
			// call server with values that were modified locally
			for (FlaggableSessionData flaggableSession : flaggableSessions)
			{
				int retryCount = 1;
				while (retryCount >= 0)
				{
					if (isCancelled())
						return null;

					Log.i(TAG, "Syncing up " + flaggableSession.nodeId + " with starred=" + flaggableSession.starred);
					if (syncUp(flaggableSession))
					{
						Log.i(TAG, "Resolving successful sync up " + flaggableSession.nodeId + " with starred=" + flaggableSession.starred);
						resolveUsingOurs(flaggableSession);
						break;
					}

					Log.w(TAG, "Error - retrying in 10s");
					try
					{
						Thread.sleep(10000);
						retryCount--;
					}
					catch (InterruptedException e)
					{
					}
				}

				if (retryCount == -1)
				{
					Log.w(TAG, "Sync failed");
					failedSyncUps++;
					break;
				}
			}

			Log.i(TAG, "SYNC SUMMARY");
			Log.i(TAG, "Not dirty: " + notDirty);
			Log.i(TAG, "Invalid urls: " + invalidUrls);
			Log.i(TAG, "Matching: " + matching);
			Log.i(TAG, "Unchecked, using flagged on server: " + uncheckedServerFlagged);
			Log.i(TAG, "Unchecked, using starred on device: " + uncheckedStarred);
			Log.i(TAG, "Sync ups: " + syncUps);
			Log.i(TAG, "Sync failed: " + (failedSyncUps > 0));

			return failedSyncUps == 0;
		}

		private boolean syncUp(FlaggableSessionData data)
		{
			String newUrl = "https://www.linuxfestnorthwest.org" + data.url;
			Pair<Boolean, String> jsonResult = HttpUtility.getStringResponse(newUrl);
			if (!jsonResult.first)
				return false;

			return true;
		}

		private void resolveUsingOurs(FlaggableSessionData data)
		{
			String url = FlagMyScheduleUtility.flipUrl(data.url, data.isFlaggedOnServer);

			SessionsManager.getInstance(OurApp.getInstance()).clearDirtyFlagWithUrl(data.nodeId, data.dirtyTime, url);
		}

		private void resolveUsingServer(FlaggableSessionData data)
		{
			SessionsManager.getInstance(OurApp.getInstance()).clearDirtyFlagWithStarred(data.nodeId, data.dirtyTime, data.isFlaggedOnServer);
		}

		private boolean resolveIfMatching(FlaggableSessionData data)
		{
			if (!isMatching(data))
				return false;

			resolveAsEqual(data);

			return true;
		}

		private void resolveAsEqual(FlaggableSessionData data)
		{
			SessionsManager.getInstance(OurApp.getInstance()).clearDirtyFlag(data.nodeId, data.dirtyTime);
		}

		private boolean isMatching(FlaggableSessionData data)
		{
			return data.isFlaggedOnServer == data.starred;
		}


		private class FlaggableSessionData
		{
			public String nodeId;
			public String url;
			public boolean starred;
			public long dirtyTime;
			public boolean isFlaggedOnServer;
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			if (!isCancelled())
			{
				m_currentTask = null;

				// remember that we successfully updated now
				if (result != null && result.booleanValue())
				{
					if (!m_shouldCallAgain)
						s_lastSuccessfulUpdateTime = new Date().getTime();

					Log.i(TAG, "Finished sessions list update success=true m_shouldCallAgain=" + m_shouldCallAgain);
					sendBroadcast(new Intent(UPDATE_COMPLETED_ACTION));

					if (m_shouldCallAgain)
					{
						m_shouldCallAgain = false;
						m_currentTask = new UpdateSessionsTask();
						m_currentTask.execute();
					}
				}
				else
				{
					s_lastSuccessfulUpdateTime = 0;

					// TODO: auto-trigger a re-sync soon...for now will happen at minimum after 10 min or next time user visits schedule
					Log.i(TAG, "Finished sessions list update success=false m_shouldCallAgain=" + m_shouldCallAgain);
					sendBroadcast(new Intent(UPDATE_COMPLETED_ACTION));
				}

			}
		}
	}

	private static long s_lastSuccessfulUpdateTime;

	private static final String TAG = "UpdateSessionsService";
	private static final int INTERVAL_UPDATE_DURATION = 1000 * 60 * 10;

	private UpdateSessionsTask m_currentTask;
	private UpdateSessionsBinder m_binder;
	private boolean m_shouldCallAgain;
}
