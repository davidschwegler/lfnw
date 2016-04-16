package com.appenjoyment.lfnw.main;

public class MainFeatureInfo
{
	public MainFeatureInfo(MainFeature feature, int titleId, int drawableId)
	{
		Feature = feature;
		TitleId = titleId;
		DrawableId = drawableId;
	}

	public final MainFeature Feature;

	public final int TitleId;

	public final int DrawableId;
}