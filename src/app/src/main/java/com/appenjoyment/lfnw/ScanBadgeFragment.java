package com.appenjoyment.lfnw;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
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
import com.appenjoyment.utility.HttpUtility;
import com.appenjoyment.utility.StreamUtility;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.io.chain.ChainingTextStringParser;

public class ScanBadgeFragment extends Fragment implements IDrawerFragment
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
		getActivity().setTitle(R.string.scan_badge_title);

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

		if (m_downloadVcfTask != null)
		{
			m_downloadVcfTask.cancel(false);
			m_downloadVcfTask = null;
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		if (!(getActivity() instanceof IDrawerActivity) || !((IDrawerActivity) getActivity()).isDrawerOpen())
			inflater.inflate(R.menu.scan_badge, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.menu_scan_badge:
				if (m_downloadVcfTask == null)
				{
					IntentIntegrator scannerIntent = new IntentIntegrator(getActivity(), this);
					scannerIntent.initiateScan(IntentIntegrator.QR_CODE_TYPES);
					return true;
				}
				break;
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
			Log.d(TAG, "ScanResult: " + scanResult.toString());
			boolean success = false;
			String contents = scanResult.getContents();
			if (!TextUtils.isEmpty(contents))
			{
				Uri uri = Uri.parse(contents);
				if (uri != null && (uri.getScheme().equals("http") || uri.getScheme().equals("https")))
				{
					Log.i(TAG, "Found http uri");
					if (uri.getSchemeSpecificPart().endsWith(".vcf"))
					{
						Log.i(TAG, "Found uri to vcf, attempting download");
						m_downloadVcfTask = (DownloadVcfTask) new DownloadVcfTask(uri).execute();
						success = true;
					}
				}
				else
				{
					Log.i(TAG, "Attempting to insert as VCF data");
					success = insertRawVcfBadge(contents);
				}
			}

			if (!success)
				Toast.makeText(getActivity(), "No contact found", Toast.LENGTH_SHORT).show();
		}
		else
		{
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public void onDrawerOpened()
	{
		getActivity().supportInvalidateOptionsMenu();
	}

	@Override
	public void onDrawerClosed()
	{
		getActivity().setTitle(R.string.scan_badge_title);
		getActivity().supportInvalidateOptionsMenu();
	}

	private boolean insertRawVcfBadge(String contents)
	{
		Log.i(TAG, "Inserting " + contents);
		ChainingTextStringParser parse = Ezvcard.parse(contents);
		VCard vcard = parse.first();
		if (vcard != null)
		{
			Log.i(TAG, "Parsed ezvcard");
			BadgeContactData contact = VCardBadgeContactUtility.parseBadgeContact(vcard);
			if (contact != null)
			{
				Log.i(TAG, "Parsed Vcf badge");
				ScannedBadgeData scannedBadge = new ScannedBadgeData();
				scannedBadge.contactData = contact;
				scannedBadge.dateScanned = new Date().getTime();
				ScannedBadgesManager.getInstance(getActivity()).insert(scannedBadge);

				Toast.makeText(getActivity(), "Added contact", Toast.LENGTH_SHORT).show();
				return true;
			}
		}

		Log.i(TAG, "Failed to parse vcard");

		return false;
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
			File appDirectory = new File(Environment.getExternalStorageDirectory(), "LinuxFest Northwest");
			File csvFile = new File(appDirectory, csvFileName);
			try
			{
				if (!appDirectory.exists() && !appDirectory.mkdirs())
				{
					Log.e(TAG, "Couldn't mkdirs for " + appDirectory.getAbsolutePath());
					success = false;
				}
				else
				{
					FileOutputStream stream = new FileOutputStream(csvFile);
					stream.write(csvString.getBytes());
					stream.close();
				}
			}
			catch (IOException e)
			{
				success = false;
				e.printStackTrace();
			}

			// share it
			if (success)
			{
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("text/plain");
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
								@TargetApi(Build.VERSION_CODES.HONEYCOMB)
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

	private final class DownloadVcfTask extends AsyncTask<Void, Void, String>
	{
		public DownloadVcfTask(Uri uri)
		{
			m_uri = uri;
		}

		@Override
		protected void onPreExecute()
		{
			m_downloadVcfDialog = new ProgressDialog(getActivity());
			m_downloadVcfDialog.setMessage(getResources().getString(R.string.scan_badge_downloading_contact));
			m_downloadVcfDialog.setCancelable(false);
			m_downloadVcfDialog.setCanceledOnTouchOutside(false);
			m_downloadVcfDialog.show();
		}

		@Override
		protected String doInBackground(Void... params)
		{
			Log.i(TAG, "Starting vcf download");
			return doDownload(m_uri.toString(), 0);
		}

		private String doDownload(String urlString, int redirectCount)
		{
			URL url = HttpUtility.createURL(urlString);
			if (url == null)
				return null;

			// load the stream into a string
			HttpURLConnection urlConnection = null;
			try
			{
				urlConnection = (HttpURLConnection) url.openConnection();
				int responseCode = urlConnection.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK)
					return StreamUtility.readAsString(urlConnection.getInputStream());

				if (redirectCount >= 4)
				{
					Log.e(TAG, "Reached redirect limit");
					return null;
				}

				String redirectUrl = urlConnection.getHeaderField("Location");
				if (redirectUrl != null)
				{
					Log.w(TAG, "Got redirect location url " + redirectUrl);

					// call again the same downloading method with new URL
					return doDownload(redirectUrl, redirectCount + 1);
				}
				else
				{
					Log.w(TAG, "No redirect location url");
					return null;
				}
			}
			catch (IOException e)
			{
				Log.w(TAG, "IOException while getting vcf stream", e);
				return null;
			}
			finally
			{
				if (urlConnection != null)
					urlConnection.disconnect();
			}
		}

		@Override
		protected void onPostExecute(String result)
		{
			if (!isCancelled())
			{
				Log.i(TAG, "Finished vcf download success=" + (result == null ? "false" : "true"));
				m_downloadVcfDialog.dismiss();
				m_downloadVcfDialog = null;
				m_downloadVcfTask = null;

				boolean inserted = result != null && insertRawVcfBadge(result);
				if (!inserted)
				{
					new AlertDialog.Builder(getActivity())
							.setMessage(R.string.scan_badge_download_contact_failed)
							.setNeutralButton("Cancel", new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									Log.i(TAG, "Inserting url to vcf badge");
									BadgeContactData contact = new BadgeContactData();
									contact.firstName = m_uri.toString();

									ScannedBadgeData scannedBadge = new ScannedBadgeData();
									scannedBadge.contactData = contact;
									scannedBadge.dateScanned = new Date().getTime();
									ScannedBadgesManager.getInstance(getActivity()).insert(scannedBadge);

									Toast.makeText(getActivity(), "Added url of contact's vcard", Toast.LENGTH_SHORT).show();
								}
							})
							.setPositiveButton("Retry", new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									Log.i(TAG, "Retrying...");
									m_downloadVcfTask = (DownloadVcfTask) new DownloadVcfTask(m_uri).execute();
								}
							})
							.show();
				}
			}
		}

		private final Uri m_uri;
		private ProgressDialog m_downloadVcfDialog;
	}

	private UpdateScannedBadgesReceiver m_updateScannedBadgesReceiver;
	private ScannedBadgesAdapter m_adapter;
	private DownloadVcfTask m_downloadVcfTask;
	private static final String TAG = "ScanBadgeFragment";
}
