package com.appenjoyment.lfnw.tickets;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * E.g.
 * <p/>
 * 2016
 * <p/>
 * <pre>
 * {
 * 	nodes: [
 *        {
 * 			node: {
 * 			Event: "2016",
 * 			Ticket type: "Standard (Free) Registration",
 * 			name: "DavidSchwegler",
 * 			code: "https://www.linuxfestnorthwest.org/phpqrcode?data=4776&level=Q&size=6&margin=1"
 *            }
 *        },
 * ]
 * </pre>
 */
public class TicketsData
{
	public ArrayList<TicketData> tickets;

	public static TicketsData parseFromJson(String in)
	{
		try
		{
			TicketsData tickets = new TicketsData();
			tickets.tickets = new ArrayList<>();

			JSONObject containerObject = new JSONObject(in);
			JSONArray nodeArray = containerObject.getJSONArray("nodes");
			if (nodeArray.length() > 0)
			{
				for (int i = 0; i < nodeArray.length(); i++)
				{
					JSONObject nodeContainerObject = nodeArray.getJSONObject(i);
					JSONObject nodeObject = nodeContainerObject.getJSONObject("node");
					TicketData ticket = new TicketData();
					ticket.codeUrl = nodeObject.getString("code");
					ticket.ticketType = nodeObject.getString("Ticket type");
					ticket.username = nodeObject.getString("name");
					ticket.year = Integer.parseInt(nodeObject.getString("Event"));
					tickets.tickets.add(ticket);
				}
			}

			return tickets;
		}
		catch (JSONException e)
		{
			// all-or-nothing for now
			Log.e(TAG, "Error parsing tickets response Json", e);
			return null;
		}
		catch (ClassCastException e)
		{
			// all-or-nothing for now
			Log.e(TAG, "Error parsing tickets response Json", e);
			return null;
		}
		catch (NumberFormatException e)
		{
			// all-or-nothing for now
			Log.e(TAG, "Error parsing tickets response Json", e);
			return null;
		}
	}

	private static final String TAG = "SessionData";
}
