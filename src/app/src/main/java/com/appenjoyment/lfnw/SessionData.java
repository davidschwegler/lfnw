package com.appenjoyment.lfnw;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

/**
 * E.g.
 * <p/>
 * 2013
 * <p/>
 * <pre>
 *     {
 * 		node_field_data_field_slot_types_time_slot_title: "Sunday, April 28, 2013 - 13:30 to 14:20",
 * 		node_title: "Developing for Android's Uniqueness",
 * 		field_room_slots_types_allowed_field_collection_item_title: "CC 239",
 * 		nid: "3101",
 * 		Speaker(s): [
 * 			"DavidSchwegler"
 * 		],
 * 		Experience level: "Intermediate",
 * 		Track: "Mobile and Android"
 *    },
 * </pre>
 * <p/>
 * <p/>
 * 2014
 * <p/>
 * <pre>
 * {
 * 		node: {
 * 			nid: "3403",
 * 			title: "Developing for Android's Uniqueness",
 * 			name: "G-103",
 * 			day: "Sun, 27 Apr 2014",
 * 			time: "1:30 PM to 2:20 PM",
 * 			field_speakers: "DavidSchwegler",
 * 			field_session_track: "Mobile Solutions",
 * 			field_experience: "Intermediate"
 *            }
 *        },
 * </post>
 *
 *
 * 2015
 *
 * <pre>
 * {
 * 		node: {
 * 			nid: "3403",
 * 			title: "Developing for Android's Uniqueness",
 * 			name: "G-103",
 * 			day: "Sun, 27 Apr 2014",
 * 			time: "1:30 PM to 2:20 PM",
 * 			field_speakers: "David Schwegler",
 * 			field_session_track: "Mobile Solutions",
 * 			field_experience: "Intermediate"
 *            }
 *        },
 * </post>
 *
 *
 * 2016
 *
 * <pre>
 * {
 * 		node: {
 * 			nid: "3403",
 * 			title: "Developing for Android's Uniqueness",
 * 			name: "G-103",
 * 			day: "Sun, 27 Apr 2014",
 * 			time: "1:30 PM to 2:20 PM",
 * 			field_speakers: "David Schwegler",
 * 			field_session_track: "Mobile Solutions",
 * 			field_experience: "Intermediate"
 *            }
 *        },
 * </post>
 *
 * 2017
 *
 * <pre>
 *  {
 * 		"node": {
 * 			"nid": "4550",
 * 			"title": "Own Your Home: Private IoT devices with resinOS",
 * 			"name": "G-103",
 * 			"day": "Sat, 6 May 2017",
 * 			"time": "9:30 AM to 10:30 AM",
 * 			"field_speakers": "jack@brownjohnf.com",
 * 			"field_experience": "Learner",
 * 			"Track": "Code",
 * 			"My Schedule": "\/flag\/flag\/session_schedule\/4550?destination=node\/4430\/schedule.json&token=PBzhVapq4hOs976c7UOk7-ke2OXWxYoQMhiZIbjvfc4"
 *            }
 *        },
 * </pre>
 */
public class SessionData
{
	public String day;
	public String time;
	public String title;
	public String room;
	public String nodeId;
	public String speakers;
	public String experienceLevel;
	public String track;
	public String flagMyScheduleUrl;
	public boolean isBof;

	// e.g.
	// day: "Sun, 27 Apr 2014",
	// time: "1:30 PM to 2:20 PM",
	// or
	// time: "1:30pm to 2:20pm",
	// TODO: Timezone is always PST
	@SuppressWarnings("deprecation")
	public Pair<Date, Date> parseTimeSlotDateRange()
	{
		Date startTime = null;
		Date endTime = null;

		// in theory an event could be all-day, but there currently are none, so
		// ignore that for now
		if (TextUtils.isEmpty(day) || TextUtils.isEmpty(time))
			return null;

		// day: "Sun, 27 Apr 2014",
		SimpleDateFormat dateFormater = new SimpleDateFormat(
				"EEEE, dd MMM yyyy", Locale.US);
		Date parsedDay;
		try
		{
			parsedDay = dateFormater.parse(day);
		}
		catch (ParseException e)
		{
			return null;
		}

		if (parsedDay == null)
			return null;

		// time: "1:30 PM to 2:20 PM",
		// or
		// time: "1:30pm to 2:20pm",
		String[] timeStartEnd = time.split(" to ");
		if (timeStartEnd.length != 2)
			return null;

		SimpleDateFormat[] timeFormats = new SimpleDateFormat[2];
		timeFormats[0] = new SimpleDateFormat("h:mm a", Locale.US);
		timeFormats[1] = new SimpleDateFormat("h:mma", Locale.US);

		String startTimeString = timeStartEnd[0].trim();
		Date parsedStartTime = tryParseDate(startTimeString, timeFormats);
		if (parsedStartTime == null)
			return null;

		String endTimeString = timeStartEnd[1].trim();
		Date parsedEndTime = tryParseDate(endTimeString, timeFormats);
		if (parsedEndTime == null)
			return null;

		startTime = new Date(parsedDay.getYear(), parsedDay.getMonth(),
				parsedDay.getDate(), parsedStartTime.getHours(),
				parsedStartTime.getMinutes());
		endTime = new Date(parsedDay.getYear(), parsedDay.getMonth(),
				parsedDay.getDate(), parsedEndTime.getHours(),
				parsedEndTime.getMinutes());

		return new Pair<Date, Date>(startTime, endTime);
	}

	private static Date tryParseDate(String raw, SimpleDateFormat[] formats)
	{
		for (SimpleDateFormat format : formats)
		{
			try
			{
				return format.parse(raw);
			}
			catch (ParseException e)
			{
			}
		}

		return null;
	}

	public static List<SessionData> parseFromJson(String in)
	{
		try
		{
			List<SessionData> sessions = new ArrayList<SessionData>();

			JSONObject containerObject = new JSONObject(in);

			// list of sessions
			JSONArray sessionsArray = containerObject.getJSONArray("nodes");
			for (int index = 0; index < sessionsArray.length(); index++)
			{
				// ultra weird, but each item in the array is a json object
				// containing a node, not the node itself...
				JSONObject sessionContainerObject = (JSONObject) sessionsArray
						.get(index);
				JSONObject sessionObject = (JSONObject) sessionContainerObject
						.get("node");
				sessions.add(parseSession(sessionObject));
			}

			return sessions;
		}
		catch (JSONException e)
		{
			// all-or-nothing for now...could be more lenient and skip failing
			// sessions,
			// but that could also lead to "dropping" a certain session and
			// never knowing it
			Log.e(TAG, "Error parsing Sessions Json", e);
			return null;
		}
		catch (ClassCastException e)
		{
			// all-or-nothing for now...could be more lenient and skip failing
			// sessions,
			// but that could also lead to "dropping" a certain session and
			// never knowing it
			Log.e(TAG, "Error parsing Sessions Json", e);
			return null;
		}
	}

	private static SessionData parseSession(JSONObject sessionObject)
			throws JSONException
	{
		SessionData session = new SessionData();

		Object dayObject = sessionObject.get("day");
		if (dayObject instanceof String)
			session.day = (String) dayObject;

		Object timeObject = sessionObject.get("time");
		if (timeObject instanceof String)
			session.time = (String) timeObject;

		Object titleObject = sessionObject.get("title");
		if (titleObject instanceof String)
			session.title = (String) titleObject;

		Object roomObject = sessionObject.get("name");
		if (roomObject instanceof String)
			session.room = (String) roomObject;

		// JSONObject.NULL
		Object nodeIdObject = sessionObject.get("nid");
		if (nodeIdObject instanceof String)
			session.nodeId = (String) nodeIdObject;

		Object experienceLevelObject = sessionObject.get("field_experience");
		if (experienceLevelObject instanceof String)
			session.experienceLevel = (String) experienceLevelObject;

		if (sessionObject.has("field_session_track"))
		{
			Object trackObject = sessionObject.get("field_session_track");
			if (trackObject instanceof String)
				session.track = (String) trackObject;
		}
		else if (sessionObject.has("Track"))
		{
			// alternate for 2016, and used in 2017
			Object trackObject = sessionObject.get("Track");
			if (trackObject instanceof String)
				session.track = (String) trackObject;
		}

		Object speakersObject = sessionObject.get("field_speakers");
		if (speakersObject instanceof String)
			session.speakers = (String) speakersObject;

		Object myScheduleObject = sessionObject.get("My Schedule");
		if (myScheduleObject instanceof String && !((String) myScheduleObject).isEmpty())
			session.flagMyScheduleUrl = (String) myScheduleObject;

		return session;
	}

	private static final String TAG = "SessionData";
}
