package com.appenjoyment.lfnw;

import java.util.Date;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
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
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
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
		// setHasOptionsMenu(true);

		View view = inflater.inflate(R.layout.scan_badge, container, false);
		Button scanButton = (Button) view.findViewById(R.id.scan_button);
		scanButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				IntentIntegrator scannerIntent = new IntentIntegrator(getActivity());
				scannerIntent.initiateScan(IntentIntegrator.QR_CODE_TYPES);
			}
		});

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
		// inflater.inflate(R.menu.scan_badge, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// switch (item.getItemId())
		// {
		// case R.id.menu_export:
		// return true;
		// }
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
					new AlertDialog.Builder(getActivity())
							.setItems(new String[] { "Add contact" }, new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									if (which == 0)
									{
										Intent intent = BadgeContactIntentUtility.createAddContactIntent(data.contactData);
										if (intent != null)
											startActivity(intent);
										else
											Toast.makeText(getActivity(), "Couldn't add contact", Toast.LENGTH_SHORT).show();
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
