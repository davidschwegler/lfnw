package com.appenjoyment.utility;

import java.util.List;

public final class ArrayUtility
{
	public static String join(String joiner, List<? extends Object> values)
	{
		StringBuilder result = new StringBuilder();

		for (Object value : values)
		{
			if (value != null)
			{
				String string = value.toString();
				if (string.length() != 0)
				{
					if (result.length() != 0)
						result.append(joiner);

					result.append(string);
				}
			}
		}

		return result.toString();
	}

	private ArrayUtility()
	{
	}
}
