package com.appenjoyment.lfnw;

import android.support.v4.widget.SwipeRefreshLayout;

public class SwipeRefreshLayoutUtility
{
	public static void applyTheme(SwipeRefreshLayout layout)
	{
		layout.setColorScheme(
				R.color.swipe_refresh_progress_1,
				R.color.swipe_refresh_progress_2,
				R.color.swipe_refresh_progress_3,
				R.color.swipe_refresh_progress_4);
	}
}
