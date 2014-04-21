package com.appenjoyment.lfnw;

import android.content.Intent;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;

public final class BadgeContactIntentUtility
{
	/**
	 * Creates an intent which adds a contact with the information in the vcard. <br/>
	 * Returns null if there's no usable data in the contact.
	 */
	public static Intent createAddContactIntent(BadgeContactData contact)
	{
		Intent intentInsertEdit = new Intent(Intent.ACTION_INSERT_OR_EDIT);

		// TODO: handle other names -- get a sample badge so we can get all this info!!!
		// eg. http://zxing.appspot.com/generator/ uses N which Ezvcard is parsing as "family name"

		boolean hasData = false;
		String fullName = contact.buildFullName();
		if (fullName != null)
		{
			intentInsertEdit.putExtra(ContactsContract.Intents.Insert.NAME, fullName);
			hasData = true;
		}

		if (contact.email != null)
		{
			intentInsertEdit.putExtra(ContactsContract.Intents.Insert.EMAIL, contact.email);
			hasData = true;
		}

		if (contact.organization != null)
		{
			intentInsertEdit.putExtra(ContactsContract.Intents.Insert.COMPANY, contact.organization);
			hasData = true;
		}

		if (hasData)
		{
			intentInsertEdit.setType(Contacts.CONTENT_ITEM_TYPE);

			// compat for 4.0.3+ which doesn't return to this activity correctly
			// note: 4.0-4.0.2 has the bug with no workaround
			intentInsertEdit.putExtra("finishActivityOnSaveCompleted", true);
		}
		else
		{
			intentInsertEdit = null;
		}

		return intentInsertEdit;
	}
}
