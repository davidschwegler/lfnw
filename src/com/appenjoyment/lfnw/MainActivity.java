package com.appenjoyment.lfnw;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import ezvcard.Ezvcard;
import ezvcard.VCard;

public class MainActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		findViewById(R.id.main_scan_badge).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				IntentIntegrator scannerIntent = new IntentIntegrator(MainActivity.this);
				scannerIntent.initiateScan(IntentIntegrator.QR_CODE_TYPES);
			}
		});
		findViewById(R.id.main_sessions).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivity(new Intent(MainActivity.this, SessionsActivity.class));
			}
		});
		findViewById(R.id.main_venue).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivity(new Intent(MainActivity.this, WebViewActivity.class).
						putExtra(WebViewActivity.KEY_URL, "http://linuxfestnorthwest.org/information/venue"));
			}
		});
		findViewById(R.id.main_sponsors).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivity(new Intent(MainActivity.this, WebViewActivity.class).
						putExtra(WebViewActivity.KEY_URL, "http://linuxfestnorthwest.org/sponsors"));
			}
		});
		findViewById(R.id.main_register).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivity(new Intent(MainActivity.this, WebViewActivity.class).
						putExtra(WebViewActivity.KEY_URL, "http://www.linuxfestnorthwest.org/node/2977/cod_registration"));
			}
		});
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (scanResult != null)
		{
			VCard vcard = Ezvcard.parse(scanResult.getContents()).first();
			if (vcard != null)
			{
				startActivity(VCardContactUtility.createAddContactIntent(vcard));
			}
			else
			{
				Log.e(TAG, "vcard failed to parse");
				Toast.makeText(this, "No contact found", Toast.LENGTH_SHORT).show();
			}
		}
	}

	private static final String TAG = "MainActivity";
}
