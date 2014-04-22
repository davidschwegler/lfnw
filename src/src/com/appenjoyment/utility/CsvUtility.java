package com.appenjoyment.utility;

public class CsvUtility
{
	public static String toCsvString(String[] row)
	{
		StringBuilder csvString = new StringBuilder();
		for (int column = 0; column < row.length; column++)
		{
			if (column != 0)
				csvString.append(SEPARATOR_CHARACTER);

			String columnValue = row[column];
			if (columnValue != null)
			{
				csvString.append(QUOTE_CHARACTER);
				csvString.append(escapeValue(columnValue));
				csvString.append(QUOTE_CHARACTER);
			}
		}

		csvString.append(NEWLINE_CHARACTER);

		return csvString.toString();
	}

	private static String escapeValue(String value)
	{
		String escapedValue = value;
		if (value.indexOf(NEWLINE_CHARACTER) != -1)
		{
			StringBuilder escapedCharacters = new StringBuilder();
			for (int charValue = 0; charValue < value.length(); charValue++)
			{
				char nextCharacter = value.charAt(charValue);
				if (nextCharacter == QUOTE_CHARACTER)
					escapedCharacters.append(QUOTE_CHARACTER).append(nextCharacter);
				else
					escapedCharacters.append(nextCharacter);
			}

			escapedValue = escapedCharacters.toString();
		}

		return escapedValue;
	}

	private static final char SEPARATOR_CHARACTER = ',';
	private static final char QUOTE_CHARACTER = '"';
	private static final char NEWLINE_CHARACTER = '\n';
}
