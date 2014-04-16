package com.appenjoyment.lfnw;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewFragment extends Fragment implements IHandleKeyDown
{
	public static WebViewFragment newInstance(String url)
	{
		if (url == null || url.length() == 0)
			throw new IllegalArgumentException("No Url");

		WebViewFragment fragment = new WebViewFragment();

		Bundle args = new Bundle();
		args.putString(KEY_URL, url);
		fragment.setArguments(args);

		return fragment;
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		setHasOptionsMenu(true);

		m_swipeRefreshLayout = new SwipeRefreshLayout(getActivity());
		m_swipeRefreshLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		m_webView = new WebView(getActivity());
		m_webView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		m_swipeRefreshLayout.addView(m_webView);

		SwipeRefreshLayoutUtility.applyTheme(m_swipeRefreshLayout);
		m_swipeRefreshLayout.setOnRefreshListener(new OnRefreshListener()
		{
			@Override
			public void onRefresh()
			{
				m_webView.reload();

				// use our callbacks, don't assume we'll succesfully start reloading
				m_swipeRefreshLayout.setRefreshing(false);
			}
		});

		m_webView.getSettings().setJavaScriptEnabled(true);

		m_webView.setWebViewClient(new WebViewClient()
		{
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
			{
				// TODO:!
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon)
			{
				super.onPageStarted(view, url, favicon);
				m_swipeRefreshLayout.setRefreshing(true);
			}

			@Override
			public void onPageFinished(WebView view, String url)
			{
				super.onPageFinished(view, url);
				m_swipeRefreshLayout.setRefreshing(false);
			}
		});

		Bundle args = getArguments();
		m_requestedUrl = args == null ? null : args.getString(KEY_URL);
		if (m_requestedUrl == null || m_requestedUrl.length() == 0)
			throw new IllegalArgumentException("No Url");

		m_webView.loadUrl(m_requestedUrl);

		return m_swipeRefreshLayout;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.webview, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.menu_open_in_browser:
			String currentUrl = m_webView.getUrl();
			Uri uri = Uri.parse(currentUrl != null && currentUrl.length() != 0 ? currentUrl : m_requestedUrl);
			startActivity(new Intent(Intent.ACTION_VIEW, uri));
			return true;
		case R.id.menu_refresh:
			m_webView.reload();
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
		return false;
	}

	private SwipeRefreshLayout m_swipeRefreshLayout;
	private WebView m_webView;
	private String m_requestedUrl;
	private static String KEY_URL = "KEY_URL";
}
