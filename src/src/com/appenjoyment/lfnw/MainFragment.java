package com.appenjoyment.lfnw;

import com.google.zxing.integration.android.IntentIntegrator;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

public class MainFragment extends Fragment
{
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.main, container, false);

		view.findViewById(R.id.main_scan_badge).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				IntentIntegrator scannerIntent = new IntentIntegrator(getActivity());
				scannerIntent.initiateScan(IntentIntegrator.QR_CODE_TYPES);
			}
		});
//		view.findViewById(R.id.main_sessions).setOnClickListener(new OnClickListener()
//		{
//			@Override
//			public void onClick(View v)
//			{
//				startActivity(new Intent(getActivity(), SessionsActivity.class));
//			}
//		});
		view.findViewById(R.id.main_venue).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivity(new Intent(getActivity(), WebViewActivity.class).
						putExtra(WebViewActivity.KEY_URL, "http://linuxfestnorthwest.org/information/venue"));
			}
		});
		view.findViewById(R.id.main_sponsors).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivity(new Intent(getActivity(), WebViewActivity.class).
						putExtra(WebViewActivity.KEY_URL, "http://linuxfestnorthwest.org/sponsors"));
			}
		});
		view.findViewById(R.id.main_register).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivity(new Intent(getActivity(), WebViewActivity.class).
						putExtra(WebViewActivity.KEY_URL, "http://www.linuxfestnorthwest.org/node/2977/cod_registration"));
			}
		});
//		view.findViewById(R.id.main_about).setOnClickListener(new OnClickListener()
//		{
//			@Override
//			public void onClick(View v)
//			{
//				startActivity(new Intent(getActivity(), AboutActivity.class));
//			}
//		});
//		
		return view;
	}

}
