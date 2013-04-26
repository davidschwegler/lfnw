package com.appenjoyment.lfnw;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewActivity extends ActionBarActivity
{
	public static String KEY_URL = "KEY_URL";

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminate(true);

		super.onCreate(savedInstanceState);

		m_webView = new WebView(this);
		m_webView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		setContentView(m_webView);

		m_webView.getSettings().setJavaScriptEnabled(true);

		m_webView.setWebViewClient(new WebViewClient()
		{
			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
			{
				// TODO:!
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon)
			{
				super.onPageStarted(view, url, favicon);
				setProgressBarIndeterminateVisibility(true);
			}

			@Override
			public void onPageFinished(WebView view, String url)
			{
				super.onPageFinished(view, url);
				setProgressBarIndeterminateVisibility(false);
			}
		});

		m_requestedUrl = getIntent().getExtras().getString(KEY_URL);
		if (m_requestedUrl == null || m_requestedUrl.length() == 0)
			throw new IllegalArgumentException("No Url");

		m_webView.loadUrl(m_requestedUrl);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.webview, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.menu_go_home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.menu_open_in_browser:
			String currentUrl = m_webView.getUrl();
			startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse(currentUrl != null && currentUrl.length() != 0 ? currentUrl : m_requestedUrl)));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		// Check if the key event was the Back button and if there's history
		if ((keyCode == KeyEvent.KEYCODE_BACK) && m_webView.canGoBack())
		{
			m_webView.goBack();
			return true;
		}

		// If it wasn't the Back key or there's no web page history, bubble up to the default
		// system behavior (probably exit the activity)
		return super.onKeyDown(keyCode, event);
	}

	private WebView m_webView;
	private String m_requestedUrl;
}
