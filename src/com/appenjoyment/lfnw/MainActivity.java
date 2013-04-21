package com.appenjoyment.lfnw;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;

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
			}
		});
		findViewById(R.id.main_sessions).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivity(new Intent(MainActivity.this, WebViewActivity.class).
						putExtra(WebViewActivity.KEY_URL, "http://linuxfestnorthwest.org/2013/schedule/sessions"));
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
}
