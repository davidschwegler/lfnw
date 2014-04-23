package com.appenjoyment.utility;

import java.util.Arrays;

public class HashCodeUtility
{
	public static int hash(Object... values)
	{
		return Arrays.hashCode(values);
	}
}
