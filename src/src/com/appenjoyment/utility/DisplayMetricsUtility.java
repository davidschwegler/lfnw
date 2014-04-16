package com.appenjoyment.utility;

import android.content.Context;

public class DisplayMetricsUtility
{
	public static int dpToPx(Context context, float dp)
	{
		float scale = context.getResources().getDisplayMetrics().density;
		return Math.round(dp * scale);
	}

	public static float pxToDp(Context context, int px)
	{
		float scale = context.getResources().getDisplayMetrics().density;
		return px / scale;
	}
}
