package com.appenjoyment.lfnw.tickets;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.appenjoyment.lfnw.IDrawerActivity;
import com.appenjoyment.lfnw.IDrawerFragment;
import com.appenjoyment.lfnw.R;
import com.appenjoyment.lfnw.accounts.AccountManager;
import com.appenjoyment.utility.HttpUtility;
import com.appenjoyment.utility.StreamUtility;

import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class TicketFragment extends Fragment implements IDrawerFragment
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

		int ticketPosition = getArguments().getInt(KEY_TICKET_POSITION);
		int ticketCount = getArguments().getInt(KEY_TICKET_COUNT);

		((TextView) view.findViewById(R.id.position)).setText((ticketPosition + 1) + " of " + ticketCount);

		TicketData ticket = TicketsManager.getInstance().getTicket(getArguments().getLong(KEY_TICKET_ID));
		if (ticket != null)
		{
			((TextView) view.findViewById(R.id.username)).setText(ticket.username);
			((TextView) view.findViewById(R.id.ticket_type)).setText(ticket.ticketType);
			m_codeUrl = ticket.codeUrl;

			m_loadQrCodeTask = new LoadQrCodeTask().execute();
		}

		return view;
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
				m_ticketProgress.setVisibility(View.GONE);

				if (bitmap != null)
					m_ticketImage.setImageBitmap(bitmap);
				else
					Toast.makeText(getActivity(), "Couldn't download QR code - check your internet connection", Toast.LENGTH_SHORT).show();
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

	@Override
	public void onDrawerOpened()
	{
		getActivity().supportInvalidateOptionsMenu();
	}

	@Override
	public void onDrawerClosed()
	{
		getActivity().setTitle(R.string.tickets_title);
		getActivity().supportInvalidateOptionsMenu();
	}

	private static final String TAG = "TicketFragment";
	private static final String KEY_TICKET_ID = "TicketId";
	private static final String KEY_TICKET_POSITION = "TicketPosition";
	private static final String KEY_TICKET_COUNT = "TicketCount";
	private boolean m_closed;
	private String m_codeUrl;
	private ImageView m_ticketImage;
	private ProgressBar m_ticketProgress;
	private AsyncTask<?, ?, ?> m_loadQrCodeTask;
}
