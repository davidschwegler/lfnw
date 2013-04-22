package com.appenjoyment.lfnw;

import java.util.ArrayList;
import java.util.List;
import android.content.Intent;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import com.appenjoyment.utility.ArrayUtility;
import ezvcard.VCard;
import ezvcard.types.AddressType;
import ezvcard.types.NoteType;
import ezvcard.types.RoleType;
import ezvcard.types.TitleType;
import ezvcard.types.UrlType;

public final class VCardContactUtility
{
	/**
	 * Creates an intent which adds a contact with the information in the vcard. <br/>
	 * TODO: If there's no usable data in the vcard, show something to the user rather than creating an intent with no data.
	 */
	public static Intent createAddContactIntent(VCard vcard)
	{
		Intent intentInsertEdit = new Intent(Intent.ACTION_INSERT_OR_EDIT);
		intentInsertEdit.setType(Contacts.CONTENT_ITEM_TYPE);

		// compat for 4.0.3+ which doesn't return to this activity correctly
		// note: 4.0-4.0.2 has the bug with no workaround
		intentInsertEdit.putExtra("finishActivityOnSaveCompleted", true);

		// name
		if (vcard.getFormattedName() != null)
			intentInsertEdit.putExtra(ContactsContract.Intents.Insert.NAME, vcard.getFormattedName().getValue());

		// job title/roles
		if (vcard.getTitles().size() != 0)
		{
			List<String> titles = new ArrayList<String>();
			for (RoleType role : vcard.getRoles())
				titles.add(role.getValue());
			for (TitleType title : vcard.getTitles())
				titles.add(title.getValue());
			intentInsertEdit.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, ArrayUtility.join(", ", titles));
		}

		// company
		if (vcard.getOrganization() != null && vcard.getOrganization().getValues().size() != 0)
			intentInsertEdit.putExtra(ContactsContract.Intents.Insert.COMPANY, ArrayUtility.join(", ", vcard.getOrganization().getValues()));

		// phone numbers
		if (vcard.getTelephoneNumbers().size() > 0)
			intentInsertEdit.putExtra(ContactsContract.Intents.Insert.PHONE, vcard.getTelephoneNumbers().get(0).getValue());
		if (vcard.getTelephoneNumbers().size() > 1)
			intentInsertEdit.putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, vcard.getTelephoneNumbers().get(1).getValue());
		if (vcard.getTelephoneNumbers().size() > 2)
			intentInsertEdit.putExtra(ContactsContract.Intents.Insert.TERTIARY_PHONE, vcard.getTelephoneNumbers().get(2).getValue());

		// emails
		if (vcard.getEmails().size() > 0)
			intentInsertEdit.putExtra(ContactsContract.Intents.Insert.EMAIL, vcard.getEmails().get(0).getValue());
		if (vcard.getEmails().size() > 1)
			intentInsertEdit.putExtra(ContactsContract.Intents.Insert.SECONDARY_EMAIL, vcard.getEmails().get(1).getValue());
		if (vcard.getEmails().size() > 2)
			intentInsertEdit.putExtra(ContactsContract.Intents.Insert.TERTIARY_EMAIL, vcard.getEmails().get(2).getValue());

		// addresses
		if (vcard.getAddresses().size() != 0)
		{
			AddressType address = vcard.getAddresses().get(0);
			String rendered = address.getLabel();

			if (rendered == null)
			{
				List<String> values = new ArrayList<String>();
				if (address.getPoBox() != null)
					values.add(address.getPoBox());
				if (address.getStreetAddress() != null)
					values.add(address.getStreetAddress());
				if (address.getExtendedAddress() != null)
					values.add(address.getExtendedAddress());
				if (address.getLocality() != null)
					values.add(address.getLocality());
				if (address.getRegion() != null)
					values.add(address.getRegion());
				if (address.getPostalCode() != null)
					values.add(address.getPostalCode());
				if (address.getCountry() != null)
					values.add(address.getCountry());

				rendered = ArrayUtility.join("\n", values);
			}
			intentInsertEdit.putExtra(ContactsContract.Intents.Insert.POSTAL, rendered);
		}

		// notes go into the notes field along with urls and anything else that doesn't have a corresponding Intents.Insert field
		List<String> values = new ArrayList<String>();
		for (NoteType value : vcard.getNotes())
			values.add(value.getValue());

		for (UrlType url : vcard.getUrls())
			values.add(url.getValue());

		if (values.size() != 0)
			intentInsertEdit.putExtra(ContactsContract.Intents.Insert.NOTES, ArrayUtility.join("\n", values));

		return intentInsertEdit;
	}
}
