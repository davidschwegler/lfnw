package com.appenjoyment.lfnw;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class WebViewFragment extends Fragment implements IHandleKeyDown, IDrawerFragment
{
	public static WebViewFragment newInstance(String url, String title)
	{
		if (url == null || url.length() == 0)
			throw new IllegalArgumentException("No Url");

		if (title == null || title.length() == 0)
			throw new IllegalArgumentException("No Title");

		WebViewFragment fragment = new WebViewFragment();

		Bundle args = new Bundle();
		args.putString(KEY_URL, url);
		args.putString(KEY_TITLE, title);
		fragment.setArguments(args);

		return fragment;
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if (getActivity() instanceof IDrawerActivity)
			m_drawerOpen = ((IDrawerActivity) getActivity()).isDrawerOpen();

		CookieSyncManager.createInstance(getActivity());

		setHasOptionsMenu(true);

		m_title = getArguments().getString(KEY_TITLE);
		getActivity().setTitle(m_title);

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
				if (!m_drawerOpen)
					m_swipeRefreshLayout.setRefreshing(m_isLoading);
			}
		});

		CookieManager.getInstance().setAcceptCookie(true);
		m_webView.getSettings().setJavaScriptEnabled(true);
		setZoomSettings();

		m_webView.setWebViewClient(new WebViewClient()
		{
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
			{
				Log.w(TAG, "OnReceivedError: " + description + " url: " + failingUrl);
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url)
			{

				if (HandleFileDownloadUri(url))
					return true;

				return super.shouldOverrideUrlLoading(view, url);
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon)
			{
				super.onPageStarted(view, url, favicon);
				Log.d(TAG, "OnPageStarted: " + url);

				m_isLoading = true;
				if (!m_drawerOpen)
					m_swipeRefreshLayout.setRefreshing(true);
			}

			@Override
			public void onPageFinished(WebView view, String url)
			{
				super.onPageFinished(view, url);
				m_isLoading = false;
				if (!m_drawerOpen)
					m_swipeRefreshLayout.setRefreshing(false);

				CookieSyncManager.getInstance().sync();
			}

			@TargetApi(Build.VERSION_CODES.HONEYCOMB)
			private boolean HandleFileDownloadUri(String url)
			{
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && url.endsWith(".pdf"))
				{
					DownloadManager manager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);

					Uri uri = Uri.parse(url);
					if (uri != null)
					{
						Request request = new Request(uri);
						int lastSlash = url.lastIndexOf('/');
						request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, url.substring(lastSlash == -1 ? 0 : lastSlash));
						request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
						manager.enqueue(request);

						return true;
					}
				}

				return false;
			}
		});

		Bundle args = getArguments();
		m_requestedUrl = args == null ? null : args.getString(KEY_URL);
		if (m_requestedUrl == null || m_requestedUrl.length() == 0)
			throw new IllegalArgumentException("No Url");

		m_webView.loadUrl(m_requestedUrl);

		return m_swipeRefreshLayout;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setZoomSettings()
	{
		m_webView.getSettings().setBuiltInZoomControls(true);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB)
			m_webView.getSettings().setDisplayZoomControls(false);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		CookieSyncManager.getInstance().startSync();
	}

	@Override
	public void onPause()
	{
		super.onPause();

		CookieSyncManager.getInstance().stopSync();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		if (!(getActivity() instanceof IDrawerActivity) || !((IDrawerActivity) getActivity()).isDrawerOpen())
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
				try
				{
					startActivity(new Intent(Intent.ACTION_VIEW, uri));
				}
				catch (ActivityNotFoundException e)
				{
					Toast.makeText(getActivity(), "Couldn't open in browser.", Toast.LENGTH_SHORT).show();
				}
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

	@Override
	public void onDrawerOpened()
	{
		getActivity().supportInvalidateOptionsMenu();

		// hack -- if we swap features while refreshing, the view gets stuck
		m_swipeRefreshLayout.setRefreshing(false);
		m_drawerOpen = true;
	}

	@Override
	public void onDrawerClosed()
	{
		getActivity().setTitle(m_title);
		getActivity().supportInvalidateOptionsMenu();

		m_drawerOpen = false;
		if (m_isLoading)
			m_swipeRefreshLayout.setRefreshing(true);
	}

	private static final String TAG = WebViewFragment.class.getName();
	private static final String KEY_URL = "KEY_URL";
	private static final String KEY_TITLE = "KEY_TITLE";

	private SwipeRefreshLayout m_swipeRefreshLayout;
	private WebView m_webView;
	private String m_requestedUrl;
	private String m_title;
	private boolean m_isLoading;
	private boolean m_drawerOpen;
}
