package com.appenjoyment.lfnw;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;

/**
 * E.g.
 * 
 * <pre>
 * 	 {
 * 		node_field_data_field_slot_types_time_slot_title: "Sunday, April 28, 2013 - 13:30 to 14:20",
 * 		node_title: "Developing for Android's Uniqueness",
 * 		field_room_slots_types_allowed_field_collection_item_title: "CC 239",
 * 		nid: "3101",
 * 		Speaker(s): [
 * 			"DavidSchwegler"
 * 		],
 * 		Experience level: "Intermediate",
 * 		Track: "Mobile and Android"
 * 	},
 * </pre>
 */
public class SessionData
{
	public String timeSlot;
	public String title;
	public String room;
	public String nodeId;
	public String[] speakers;
	public String experienceLevel;
	public String track;

	public static List<SessionData> parseFromJson(String in)
	{
		List<SessionData> sessions = new ArrayList<SessionData>();

		try
		{
			// list of sessions
			JSONArray sessionsArray = new JSONArray(in);
			for (int index = 0; index < sessionsArray.length(); index++)
			{
				// session
				JSONObject sessionObject = (JSONObject) sessionsArray.get(index);
				sessions.add(parseSession(sessionObject));
			}
		}
		catch (JSONException e)
		{
			Log.e(TAG, "Error parsing Sessions Json", e);
		}

		return sessions;
	}

	private static SessionData parseSession(JSONObject sessionObject) throws JSONException
	{
		SessionData session = new SessionData();

		// note: weirdness in json data -- string values are being set to empty arrays if nonexistant
		Object timeSlotObject = sessionObject.get("node_field_data_field_slot_types_time_slot_title");
		if (timeSlotObject instanceof String)
			session.timeSlot = (String) timeSlotObject;

		Object titleObject = sessionObject.get("node_title");
		if (titleObject instanceof String)
			session.title = (String) titleObject;

		Object roomObject = sessionObject.get("field_room_slots_types_allowed_field_collection_item_title");
		if (roomObject instanceof String)
			session.room = (String) roomObject;

		// JSONObject.NULL
		Object nodeIdObject = sessionObject.get("nid");
		if (nodeIdObject instanceof String)
			session.nodeId = (String) nodeIdObject;

		Object experienceLevelObject = sessionObject.get("Experience level");
		if (experienceLevelObject instanceof String)
			session.experienceLevel = (String) experienceLevelObject;

		Object trackObject = sessionObject.get("Track");
		if (trackObject instanceof String)
			session.track = (String) trackObject;

		JSONArray speakersArray = sessionObject.getJSONArray("Speaker(s)");
		session.speakers = new String[speakersArray.length()];
		for (int index = 0; index < speakersArray.length(); index++)
			session.speakers[index] = speakersArray.getString(index);

		return session;
	}

	private static final String TAG = "SessionData";
}
