package com.appenjoyment.lfnw;

/**
 * Contact data encoded onto badges
 */
public class BadgeContactData
{
	public String firstName;

	public String lastName;

	public String email;

	public String organization;

	public String buildFullName()
	{
		if (firstName != null && lastName != null)
			return firstName + " " + lastName;

		if (lastName == null)
			return firstName;

		return lastName;
	}
}
