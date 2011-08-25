package at.dasz.KolabDroid.ContactsContract;

import java.util.ArrayList;
import java.util.HashSet;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Data;
import android.util.Log;
import at.dasz.KolabDroid.R;
import at.dasz.KolabDroid.Sync.SyncException;

public class ContactDBHelper
{
	//wipe those contacts which are marked as deleted (we call that before a sync)
	public static void wipeDeletedContacts(ContentResolver cr)
	{
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		
		ops.add(ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build()).
				withSelection("deleted=?", new String[]{String.valueOf(1)}).
				
				build());    	
		
		try {
			cr.applyBatch(ContactsContract.AUTHORITY, ops);
		} catch (Exception e) {
			Log.e("EE", e.toString());
		}
	}
	
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
					result.addContactMethod(pc);
				}
				else if (mimeType.equals(Email.CONTENT_ITEM_TYPE))
				{
					EmailContact ec = new EmailContact();
					ec.setData(queryCursor
							.getString(DataQuery.COLUMN_EMAIL_ADDRESS));
					ec.setType(queryCursor.getInt(DataQuery.COLUMN_EMAIL_TYPE));
					result.addContactMethod(ec);
				}
				else if (mimeType.equals(StructuredPostal.CONTENT_ITEM_TYPE))
				{
					AddressContact ac = new AddressContact();
					ac.setCity(queryCursor
							.getString(DataQuery.COLUMN_ADDRESS_CITY));
					ac.setCountry(queryCursor
							.getString(DataQuery.COLUMN_ADDRESS_COUNTRY));
					ac.setPostalcode(queryCursor
							.getString(DataQuery.COLUMN_ADDRESS_POSTALCODE));
					ac.setRegion(queryCursor
							.getString(DataQuery.COLUMN_ADDRESS_REGION));
					ac.setStreet(queryCursor
							.getString(DataQuery.COLUMN_ADDRESS_STREET));
					ac.setType(queryCursor
							.getInt(DataQuery.COLUMN_ADDRESS_TYPE));
					ac.updateData();
					result.addContactMethod(ac);
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
				else if (mimeType.equals(Website.CONTENT_ITEM_TYPE))
				{
					String webpage = queryCursor
							.getString(DataQuery.COLUMN_WEBPAGE);
					result.setWebpage(webpage);
				}
				else if (mimeType.equals(Organization.CONTENT_ITEM_TYPE))
				{
					String org = queryCursor
							.getString(DataQuery.COLUMN_ORGANIZATION);
					result.setOrganization(org);
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

		final long rawContactId = contact.getId();

		if (contact.getId() == 0)
		{
			contactOp = ContactOperations.createNewContact(0, accountName,
					batchOperation);
			contactOp.addName(contact.getGivenName(), contact.getFamilyName(),
					contact.getFullName());
		}
		else
		{
			contactOp = ContactOperations.updateExistingContact(rawContactId,
					batchOperation);
		}

		saveContactDetails(ctx, ctx.getContentResolver(), accountName, contact,
				rawContactId, contactOp);

		long id = batchOperation.execute(contact.getId());

		if (contact.getId() == 0)
		{
			if (id == 0) throw new SyncException(contact.getFullName(),
					"Unable to get ID of newly created item");
			contact.setId((int) id);
		}
	}

	private static void saveContactDetails(Context context,
			ContentResolver resolver, String accountName, Contact contact,
			long rawContactId, ContactOperations contactOp)
	{
		boolean photoUpdated = false;
		boolean notesUpdated = false;
		boolean birthdayUpdated = false;
		boolean webpageUpdated = false;
		boolean orgUpdated = false;

		HashSet<PhoneContact> updatedPhoneContacts = new HashSet<PhoneContact>();
		HashSet<EmailContact> updatedEmailContacts = new HashSet<EmailContact>();
		HashSet<AddressContact> updatedAddressContacts = new HashSet<AddressContact>();

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
								contact.getFamilyName(), contact.getFullName());
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
					else if (mimeType.equals(Website.CONTENT_ITEM_TYPE))
					{
						webpageUpdated = true;
						contactOp.updateWebpage(uri, contact.getWebpage());
					}
					else if (mimeType.equals(Organization.CONTENT_ITEM_TYPE))
					{
						orgUpdated = true;
						contactOp.updateOrganization(uri,
								contact.getOrganization());
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
						final int type = c
								.getInt(DataQuery.COLUMN_ADDRESS_TYPE);
						AddressContact ac = contact.findAddress(type);
						if (ac != null)
						{
							contactOp.updateAddress(uri, ac.getStreet(),
									ac.getCity(), ac.getRegion(),
									ac.getPostalcode(), ac.getCountry(),
									ac.getType());
							updatedAddressContacts.add(ac);
						}
						else
						{
							contactOp.delete(uri);
						}
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
		if (!webpageUpdated)
		{
			contactOp.addWebpage(contact.getWebpage());
		}
		if (!orgUpdated)
		{
			contactOp.addOrganization(contact.getOrganization());
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

			if (cm instanceof AddressContact
					&& !updatedAddressContacts.contains(cm))
			{
				AddressContact ac = (AddressContact) cm;
				contactOp.addAddress(ac.getStreet(), ac.getCity(),
						ac.getRegion(), ac.getPostalcode(), ac.getCountry(),
						ac.getType());
			}
		}
	}
}
