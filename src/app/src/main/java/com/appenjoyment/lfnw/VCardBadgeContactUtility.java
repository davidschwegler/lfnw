package com.appenjoyment.lfnw;

import ezvcard.VCard;

public final class VCardBadgeContactUtility
{
	public static BadgeContactData parseBadgeContact(VCard vcard)
	{
		// TODO: handle other names -- get a sample badge so we can get all this info!!!
		// eg. http://zxing.appspot.com/generator/ uses N which Ezvcard is parsing as "family name"

		BadgeContactData data = new BadgeContactData();

		// name
		if (vcard.getFormattedName() != null)
			data.firstName = vcard.getFormattedName().getValue();
		if (data.firstName == null && vcard.getStructuredName() != null)
		{
			data.firstName = vcard.getStructuredName().getGiven();
			data.lastName = vcard.getStructuredName().getFamily();
		}

		// email
		if (vcard.getEmails() != null && vcard.getEmails().size() != 0)
			data.email = vcard.getEmails().get(0).getValue();

		// company
		if (vcard.getOrganization() != null && vcard.getOrganization().getValues().size() != 0)
			data.organization = vcard.getOrganization().getValues().get(0);

		return data;
	}
}
