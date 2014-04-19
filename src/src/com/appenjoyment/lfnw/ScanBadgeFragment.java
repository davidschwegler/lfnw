package com.appenjoyment.lfnw;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

		return view;
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
			if (!TextUtils.isEmpty(scanResult.getContents()))
			{
				VCard vcard = Ezvcard.parse(scanResult.getContents()).first();
				if (vcard != null)
				{
					startActivity(VCardContactUtility.createAddContactIntent(vcard));
				}
				else
				{
					Log.e(TAG, "vcard failed to parse");
					Toast.makeText(getActivity(), "No contact found", Toast.LENGTH_SHORT).show();
				}
			}
		}
		else
		{
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private static final String TAG = "ScanBadgeFragment";
}
