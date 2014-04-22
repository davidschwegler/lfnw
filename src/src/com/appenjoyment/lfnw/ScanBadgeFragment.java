package com.appenjoyment.lfnw;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.appenjoyment.utility.CsvUtility;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import ezvcard.Ezvcard;
import ezvcard.VCard;

public class ScanBadgeFragment extends Fragment
{
	public static ScanBadgeFragment newInstance()
	{
		return new ScanBadgeFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		m_updateScannedBadgesReceiver = new UpdateScannedBadgesReceiver();

		IntentFilter filter = new IntentFilter();
		filter.addAction(ScannedBadgesManager.UPDATED_SCANNED_BADGES_ACTION);
		getActivity().registerReceiver(m_updateScannedBadgesReceiver, filter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		setHasOptionsMenu(true);

		View view = inflater.inflate(R.layout.scan_badge, container, false);

		ListView listView = (ListView) view.findViewById(R.id.scan_badge_list);
		listView.setEmptyView(view.findViewById(android.R.id.empty));

		Cursor cursor = ScannedBadgesManager.getInstance(getActivity()).getAllScannedBadges();
		m_adapter = new ScannedBadgesAdapter(getActivity(), cursor);
		listView.setAdapter(m_adapter);

		return view;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		getActivity().unregisterReceiver(m_updateScannedBadgesReceiver);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.scan_badge, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.menu_scan_badge:
			IntentIntegrator scannerIntent = new IntentIntegrator(getActivity());
			scannerIntent.initiateScan(IntentIntegrator.QR_CODE_TYPES);
			return true;
		case R.id.menu_scan_badge_export:
			exportToCsv();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
		if (scanResult != null)
		{
			boolean success = false;
			if (!TextUtils.isEmpty(scanResult.getContents()))
			{
				VCard vcard = Ezvcard.parse(scanResult.getContents()).first();
				if (vcard != null)
				{
					BadgeContactData contact = VCardBadgeContactUtility.parseBadgeContact(vcard);
					if (contact != null)
					{
						ScannedBadgeData scannedBadge = new ScannedBadgeData();
						scannedBadge.contactData = contact;
						scannedBadge.dateScanned = new Date().getTime();
						ScannedBadgesManager.getInstance(getActivity()).insert(scannedBadge);
						success = true;
					}
				}
			}

			if (!success)
			{
				Log.e(TAG, "vcard failed to parse");
				Toast.makeText(getActivity(), "No contact found", Toast.LENGTH_SHORT).show();
			}
		}
		else
		{
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@SuppressLint("WorldReadableFiles")
	private void exportToCsv()
	{
		String csvString = buildCsvString();
		if (TextUtils.isEmpty(csvString))
		{
			Toast.makeText(getActivity(), "Nothing to export", Toast.LENGTH_SHORT).show();
		}
		else
		{
			// create the CSV
			boolean success = true;
			String csvFileName = "ScannedBadges-" + new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.US).format(new Date()) + ".csv";
			try
			{
				// I think we want this world-readable here, as otherwise external apps we send it to wouldn't have access to our cache directory
				@SuppressWarnings("deprecation")
				FileOutputStream stream = getActivity().openFileOutput(csvFileName, Context.MODE_WORLD_READABLE);
				stream.write(csvString.getBytes());
				stream.close();
			}
			catch (IOException e)
			{
				success = false;
				e.printStackTrace();
			}

			// email it
			if (success)
			{
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("text/plain");
				File csvFile = new File(getActivity().getFilesDir(), csvFileName);
				intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(csvFile));
				try
				{
					startActivity(intent);
				}
				catch (ActivityNotFoundException e)
				{
					success = false;
				}
			}

			if (!success)
				Toast.makeText(getActivity(), "Couldn't export scanned badges", Toast.LENGTH_SHORT).show();
		}
	}

	private String buildCsvString()
	{
		Cursor cursor = ScannedBadgesManager.getInstance(getActivity()).getAllScannedBadges();

		StringBuilder csvString = new StringBuilder(128);
		while (cursor.moveToNext())
		{
			ScannedBadgeData data = ScannedBadgesManager.createFromCursor(cursor);

			String[] dataCsv = new String[4];
			dataCsv[0] = data.contactData.firstName;
			dataCsv[1] = data.contactData.lastName;
			dataCsv[2] = data.contactData.email;
			dataCsv[3] = data.contactData.organization;
			csvString.append(CsvUtility.toCsvString(dataCsv));
		}

		return csvString.toString();
	}

	// TODO: use a loader, rather than all this deprecated stuff!
	@SuppressWarnings("deprecation")
	private class ScannedBadgesAdapter extends CursorAdapter implements ListAdapter
	{
		public ScannedBadgesAdapter(Context context, Cursor cursor)
		{
			super(context, cursor);
		}

		@Override
		public View newView(Context context, final Cursor cursor, ViewGroup parent)
		{
			final View view = getActivity().getLayoutInflater().inflate(R.layout.scanned_badge_list_item, null);

			final ViewHolder viewHolder = new ViewHolder();
			viewHolder.name = (TextView) view.findViewById(R.id.scanned_badge_name);
			viewHolder.organization = (TextView) view.findViewById(R.id.scanned_badge_organization);
			viewHolder.email = (TextView) view.findViewById(R.id.scanned_badge_email);

			view.setTag(viewHolder);

			bindView(view, context, cursor);

			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			ViewHolder viewHolder = (ViewHolder) view.getTag();
			final ScannedBadgeData data = ScannedBadgesManager.createFromCursor(cursor);
			viewHolder.name.setText(data.contactData.buildFullName());

			if (!TextUtils.isEmpty(data.contactData.organization))
			{
				viewHolder.organization.setVisibility(View.VISIBLE);
				viewHolder.organization.setText(data.contactData.organization);
			}
			else
			{
				viewHolder.organization.setVisibility(View.GONE);
			}

			if (!TextUtils.isEmpty(data.contactData.email))
			{
				viewHolder.email.setVisibility(View.VISIBLE);
				viewHolder.email.setText(data.contactData.email);
			}
			else
			{
				viewHolder.email.setVisibility(View.GONE);
			}

			view.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					final List<Integer> titleIds = new ArrayList<Integer>();
					titleIds.add(R.string.scanned_badge_add_to_contacts);
					if (!TextUtils.isEmpty(data.contactData.email))
						titleIds.add(R.string.scanned_badge_send_email);
					titleIds.add(R.string.generic_copy);

					String[] titles = new String[titleIds.size()];
					for (int index = 0; index < titleIds.size(); index++)
						titles[index] = getResources().getString(titleIds.get(index).intValue());

					new AlertDialog.Builder(getActivity())
							.setItems(titles, new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									switch (titleIds.get(which).intValue())
									{
									case R.string.scanned_badge_add_to_contacts:
									{
										Intent intent = BadgeContactIntentUtility.createAddContactIntent(data.contactData);

										boolean success = false;
										if (intent != null)
										{
											try
											{
												startActivity(intent);
												success = true;
											}
											catch (ActivityNotFoundException e)
											{
											}
										}

										if (!success)
											Toast.makeText(getActivity(), "Couldn't add contact", Toast.LENGTH_SHORT).show();
										break;
									}
									case R.string.scanned_badge_send_email:
									{
										Intent intent = new Intent(Intent.ACTION_VIEW);
										Uri intentData = Uri.parse("mailto:" + data.contactData.email);
										intent.setData(intentData);

										boolean success = false;
										try
										{
											startActivity(intent);
											success = true;
										}
										catch (ActivityNotFoundException e)
										{
										}

										if (!success)
											Toast.makeText(getActivity(), "Couldn't send email", Toast.LENGTH_SHORT).show();
										break;
									}
									case R.string.generic_copy:
									{
										StringBuilder text = new StringBuilder(data.contactData.buildFullName());
										if (!TextUtils.isEmpty(data.contactData.organization))
										{
											if (text.length() != 0)
												text.append("\n");
											text.append(data.contactData.organization);
										}

										if (!TextUtils.isEmpty(data.contactData.email))
										{
											if (text.length() != 0)
												text.append("\n");
											text.append(data.contactData.email);
										}

										if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
										{
											android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getActivity()
													.getSystemService(Context.CLIPBOARD_SERVICE);
											clipboard.setText(text);
										}
										else
										{
											android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity()
													.getSystemService(Context.CLIPBOARD_SERVICE);
											ClipData clip = ClipData.newPlainText(null, text);
											clipboard.setPrimaryClip(clip);
										}

										break;
									}
									}
								}
							})
							.create()
							.show();
				}
			});
		}

		final class ViewHolder
		{
			public TextView name;
			public TextView organization;
			public TextView email;
		}
	}

	private final class UpdateScannedBadgesReceiver extends BroadcastReceiver
	{
		@SuppressWarnings("deprecation")
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent.getAction().equals(ScannedBadgesManager.UPDATED_SCANNED_BADGES_ACTION))
				m_adapter.getCursor().requery();
		}
	}

	private UpdateScannedBadgesReceiver m_updateScannedBadgesReceiver;
	private ScannedBadgesAdapter m_adapter;
	private static final String TAG = "ScanBadgeFragment";
}
