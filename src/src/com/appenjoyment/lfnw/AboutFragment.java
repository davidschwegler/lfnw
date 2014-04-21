package com.appenjoyment.lfnw;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class AboutFragment extends Fragment
{
	public static AboutFragment newInstance()
	{
		return new AboutFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.about, container, false);

		PackageInfo packageInfo = null;
		try
		{
			packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
		}
		catch (NameNotFoundException e)
		{
			// can't happen
		}

		TextView version = (TextView) view.findViewById(R.id.about_version);
		version.setText(getString(R.string.about_version) + ": " + packageInfo.versionName);

		SpannableStringBuilder builder = new SpannableStringBuilder(getString(R.string.about_support_email) + ": ");
		final String supportEmail = getString(R.string.support_email);
		builder.append(supportEmail);
		builder.setSpan(new URLSpan(supportEmail), builder.length() - supportEmail.length(), builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

		TextView email = (TextView) view.findViewById(R.id.about_email);
		email.setText(builder);

		final PackageInfo packageInfoFinal = packageInfo;
		email.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				String subject = getString(R.string.support_email_subject_prefix) + ": " + getString(R.string.app_name) +
						" " + packageInfoFinal.versionName;
				String body = "\n\n----\nVersion: " + packageInfoFinal.versionName +
						"\nVersion Code: " + packageInfoFinal.versionCode +
						"\n" + android.os.Build.MANUFACTURER + " | " + android.os.Build.MODEL + " | " + android.os.Build.BOARD;

				Intent intent = new Intent(Intent.ACTION_VIEW);
				Uri data = Uri.parse("mailto:" + supportEmail + "?subject=" + subject + "&body=" + body);
				intent.setData(data);
				try
				{
					startActivity(intent);
				}
				catch (ActivityNotFoundException e)
				{
					Toast.makeText(getActivity(), "Couldn't send email", Toast.LENGTH_SHORT).show();
				}
			}
		});

		return view;
	}
}
