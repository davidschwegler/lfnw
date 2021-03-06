package com.appenjoyment.lfnw.tickets;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.appenjoyment.lfnw.R;
import com.appenjoyment.utility.HttpUtility;

public class TicketFragment extends Fragment
{
	public static TicketFragment newInstance(long ticketId, int ticketPosition, int ticketCount)
	{
		Bundle bundle = new Bundle();
		bundle.putLong(KEY_TICKET_ID, ticketId);
		bundle.putInt(KEY_TICKET_POSITION, ticketPosition);
		bundle.putInt(KEY_TICKET_COUNT, ticketCount);

		TicketFragment fragment = new TicketFragment();
		fragment.setArguments(bundle);
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.ticket, container, false);

		m_ticketImage = (ImageView) view.findViewById(R.id.ticket_image);
		m_ticketProgress = (ProgressBar) view.findViewById(R.id.ticket_image_progress);
		m_message = (TextView) view.findViewById(R.id.ticket_message);

		int ticketPosition = getArguments().getInt(KEY_TICKET_POSITION);
		int ticketCount = getArguments().getInt(KEY_TICKET_COUNT);

		((TextView) view.findViewById(R.id.position)).setText((ticketPosition + 1) + " of " + ticketCount);

		TicketData ticket = TicketsManager.getInstance().getTicket(getArguments().getLong(KEY_TICKET_ID));
		if (ticket != null)
		{
			((TextView) view.findViewById(R.id.username)).setText(ticket.username);
			((TextView) view.findViewById(R.id.ticket_type)).setText(ticket.ticketType);
			m_codeUrl = ticket.codeUrl;

			load();
		}
		else
		{
			// should never happen
		}

		return view;
	}

	private void load()
	{
		if (m_loadQrCodeTask != null)
			return;

		m_loadQrCodeTask = new LoadQrCodeTask().execute();
		updateMessage(false);
	}

	private class LoadQrCodeTask extends AsyncTask<Void, Void, Bitmap>
	{

		@Override
		protected void onPreExecute()
		{
			m_ticketProgress.setVisibility(View.VISIBLE);
		}

		@Override
		protected Bitmap doInBackground(Void... params)
		{
			Bitmap bitmap = null;

			// TODO: cache
//			File avatarFile = new File(getActivity().getFilesDir(), user.userId + ".jpg");
//			if (avatarFile.exists())
//			{
//				Log.d(TAG, "Trying to load cached avatar");
//				bitmap = BitmapFactory.decodeFile(avatarFile.getAbsolutePath());
//
//				if (bitmap == null)
//					Log.w(TAG, "Couldn't load cached ticket qr code");
//			}

			if (!isCancelled() && bitmap == null)
			{
				Log.d(TAG, "Trying to download ticket qr code");
				Pair<Boolean, byte[]> bytes = HttpUtility.getByteResponse(m_codeUrl);
				if (bytes.first)
				{
					bitmap = BitmapFactory.decodeByteArray(bytes.second, 0, bytes.second.length);

					if (bitmap == null)
						Log.w(TAG, "Couldn't download ticket qr code");
				}

//				if (!isCancelled() && bitmap != null)
//				{
//					Log.d(TAG, "Trying to save ticket qr code");
//					if (!StreamUtility.writeToFile(avatarFile, bytes.second))
//						Log.w(TAG, "Couldn't save ticket qr code");
//				}
			}

			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap)
		{
			if (!isCancelled() && !m_closed)
			{
				m_loadQrCodeTask = null;

				m_ticketProgress.setVisibility(View.GONE);

				if (bitmap != null)
					m_ticketImage.setImageBitmap(bitmap);
				else
					updateMessage(true);
			}
		}
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroy();

		if (m_loadQrCodeTask != null)
		{
			m_loadQrCodeTask.cancel(false);
			m_loadQrCodeTask = null;
		}

		m_closed = true;
	}

	private void updateMessage(boolean networkError)
	{
		if (networkError && m_loadQrCodeTask == null)
		{
			m_message.setVisibility(View.VISIBLE);
			m_message.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					load();
				}
			});
		}
		else
		{
			m_message.setVisibility(View.GONE);
		}
	}

	private static final String TAG = "TicketFragment";
	private static final String KEY_TICKET_ID = "TicketId";
	private static final String KEY_TICKET_POSITION = "TicketPosition";
	private static final String KEY_TICKET_COUNT = "TicketCount";
	private boolean m_closed;
	private String m_codeUrl;
	private ImageView m_ticketImage;
	private ProgressBar m_ticketProgress;
	private TextView m_message;
	private AsyncTask<?, ?, ?> m_loadQrCodeTask;
}
