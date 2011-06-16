package at.dasz.KolabDroid.ContactsContract;

import java.util.HashSet;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import at.dasz.KolabDroid.R;
import at.dasz.KolabDroid.Sync.SyncException;

public class ContactDBHelper
{
	public static Contact getContactByRawID(long contactID, ContentResolver cr)
			throws SyncException
	{
		Cursor queryCursor = null;
		try
		{
			queryCursor = cr.query(ContactsContract.Data.CONTENT_URI,
					DataQuery.PROJECTION, DataQuery.SELECTION,
					new String[] { Long.toString(contactID) }, null);

			if (queryCursor == null) throw new SyncException("",
					"cr.query returned null");
			if (!queryCursor.moveToFirst()) return null;

			Contact result = new Contact();
			result.setId((int) contactID);
			String mimeType;

			do
			{
				mimeType = queryCursor.getString(DataQuery.COLUMN_MIMETYPE);
				if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE))
				{
					result.setGivenName(queryCursor
							.getString(DataQuery.COLUMN_GIVEN_NAME));
					result.setFamilyName(queryCursor
							.getString(DataQuery.COLUMN_FAMILY_NAME));
					result.setFullName(queryCursor
							.getString(DataQuery.COLUMN_FULL_NAME));
				}
				else if (mimeType.equals(Phone.CONTENT_ITEM_TYPE))
				{
					PhoneContact pc = new PhoneContact();
					pc.setData(queryCursor
							.getString(DataQuery.COLUMN_PHONE_NUMBER));
					pc.setType(queryCursor.getInt(DataQuery.COLUMN_PHONE_TYPE));
					result.getContactMethods().add(pc);

				}
				else if (mimeType.equals(Email.CONTENT_ITEM_TYPE))
				{
					EmailContact pc = new EmailContact();
					pc.setData(queryCursor
							.getString(DataQuery.COLUMN_EMAIL_ADDRESS));
					pc.setType(queryCursor.getInt(DataQuery.COLUMN_EMAIL_TYPE));
					result.getContactMethods().add(pc);

				}
				else if (mimeType.equals(Event.CONTENT_ITEM_TYPE))
				{
					String bday = queryCursor
							.getString(DataQuery.COLUMN_EVENT_START_DATE);
					result.setBirthday(bday);
				}
				else if (mimeType.equals(Photo.CONTENT_ITEM_TYPE))
				{
					byte[] photo = queryCursor.getBlob(DataQuery.COLUMN_PHOTO);
					result.setPhoto(photo);
				}
				else if (mimeType.equals(Note.CONTENT_ITEM_TYPE))
				{
					String note = queryCursor.getString(DataQuery.COLUMN_NOTE);
					result.setNote(note);
				}
			} while (queryCursor.moveToNext());

			return result;
		}
		finally
		{
			if (queryCursor != null) queryCursor.close();
		}
	}

	public static void saveContact(Contact contact, Context ctx)
			throws SyncException
	{
		final BatchOperation batchOperation = new BatchOperation(ctx,
				ctx.getContentResolver());
		final String accountName = ctx.getString(R.string.SYNC_ACCOUNT_NAME);

		ContactOperations contactOp;

		final int rawContactId = contact.getId();

		if (contact.getId() == 0)
		{
			contactOp = ContactOperations.createNewContact(0, accountName,
					batchOperation);
			contactOp.addName(contact.getGivenName(), contact.getFamilyName());
		}
		else
		{
			contactOp = ContactOperations.updateExistingContact(rawContactId,
					batchOperation);
		}

		saveContactDetails(ctx, ctx.getContentResolver(), accountName, contact,
				rawContactId, contactOp);

		batchOperation.execute();
	}

	private static void saveContactDetails(Context context,
			ContentResolver resolver, String accountName, Contact contact,
			int rawContactId, ContactOperations contactOp)
	{
		boolean photoUpdated = false;
		boolean notesUpdated = false;
		boolean birthdayUpdated = false;
		HashSet<PhoneContact> updatedPhoneContacts = new HashSet<PhoneContact>();
		HashSet<EmailContact> updatedEmailContacts = new HashSet<EmailContact>();

		// Update existing contact fields
		if (rawContactId != 0)
		{
			Uri uri;
			final Cursor c = resolver.query(Data.CONTENT_URI,
					DataQuery.PROJECTION, DataQuery.SELECTION,
					new String[] { String.valueOf(rawContactId) }, null);
			try
			{
				while (c.moveToNext())
				{
					final long id = c.getLong(DataQuery.COLUMN_ID);
					final String mimeType = c
							.getString(DataQuery.COLUMN_MIMETYPE);
					uri = ContentUris.withAppendedId(Data.CONTENT_URI, id);
					if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE))
					{
						contactOp.updateName(uri, contact.getGivenName(),
								contact.getFamilyName());
					}
					else if (mimeType.equals(Photo.CONTENT_ITEM_TYPE))
					{
						photoUpdated = true;
						contactOp.updatePhoto(uri, contact.getPhoto());
					}
					else if (mimeType.equals(Note.CONTENT_ITEM_TYPE))
					{
						notesUpdated = true;
						contactOp.updateNotes(uri, contact.getNotes());
					}
					else if (mimeType.equals(Event.CONTENT_ITEM_TYPE))
					{
						birthdayUpdated = true;
						contactOp.updateBirthday(uri, contact.getBirthday());
					}
					else if (mimeType.equals(Phone.CONTENT_ITEM_TYPE))
					{
						final String phone = c
								.getString(DataQuery.COLUMN_PHONE_NUMBER);
						PhoneContact pc = contact.findPhone(phone);
						if (pc != null)
						{
							contactOp.updatePhone(uri, pc.getData(),
									pc.getType());
							updatedPhoneContacts.add(pc);
						}
						else
						{
							contactOp.delete(uri);
						}
					}
					else if (mimeType.equals(Email.CONTENT_ITEM_TYPE))
					{
						final String mail = c
								.getString(DataQuery.COLUMN_EMAIL_ADDRESS);
						EmailContact ec = contact.findEmail(mail);
						if (ec != null)
						{
							contactOp.updateEmail(uri, ec.getData(),
									ec.getType());
							updatedEmailContacts.add(ec);
						}
						else
						{
							contactOp.delete(uri);
						}
					}
					else if (mimeType
							.equals(StructuredPostal.CONTENT_ITEM_TYPE))
					{
					}
				} // while
			}
			finally
			{
				c.close();
			}
		}

		// Insert all Fields if they where not updated
		if (!photoUpdated)
		{
			contactOp.addPhoto(contact.getPhoto());
		}
		if (!notesUpdated)
		{
			contactOp.addNotes(contact.getNotes());
		}
		if (!birthdayUpdated)
		{
			contactOp.addBirthday(contact.getBirthday());
		}

		for (ContactMethod cm : contact.getContactMethods())
		{
			if (cm instanceof EmailContact
					&& !updatedEmailContacts.contains(cm))
			{
				contactOp.addEmail(cm.getData(), cm.getType());
			}

			if (cm instanceof PhoneContact
					&& !updatedPhoneContacts.contains(cm))
			{
				contactOp.addPhone(cm.getData(), cm.getType());
			}

			// if (cm instanceof AddressContact)
			// {
			// AddressContact acm = (AddressContact)cm;
			// contactOp.
			// }
		}
	}
}
