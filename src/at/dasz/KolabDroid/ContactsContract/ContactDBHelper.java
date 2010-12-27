package at.dasz.KolabDroid.ContactsContract;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.util.Log;
import at.dasz.KolabDroid.Sync.CacheEntry;
import at.dasz.KolabDroid.Sync.SyncException;

public class ContactDBHelper
{
	public static Contact getContactByRawURI(Uri uri, ContentResolver cr) throws SyncException
	{
		//ContentResolver cr = getContentResolver();
		Cursor queryCursor = null;
		try
		{
			//TODO: hack to get Raw ID
			String tmp = uri.toString();
			String[] a = tmp.split("/");
			int idx = a.length -1;
			int id = Integer.parseInt(a[idx]);

			String where = ContactsContract.Data.RAW_CONTACT_ID + "=?";
			
			String[] projection = new String[] {
					Contacts.Data.MIMETYPE,
					StructuredName.GIVEN_NAME,
					StructuredName.FAMILY_NAME,
					Phone.NUMBER,
					Phone.TYPE,
					Email.DATA,
					Event.START_DATE,
					Photo.PHOTO,
					Note.NOTE
			};
			
			queryCursor = cr.query(ContactsContract.Data.CONTENT_URI, projection,
					where, new String[] { Integer.toString(id) }, null);

			if (queryCursor == null) throw new SyncException("",
					"cr.query returned null");
			if (!queryCursor.moveToFirst()) return null;

			Contact result = new Contact();
			result.setId(id);

			int idxMimeType = queryCursor.getColumnIndex(ContactsContract.Contacts.Data.MIMETYPE);
			String mimeType;
			
			do
			{
				mimeType = queryCursor.getString(idxMimeType);
				if (mimeType
						.equals(StructuredName.CONTENT_ITEM_TYPE))
				{
					int idxFirst = queryCursor
							.getColumnIndex(StructuredName.GIVEN_NAME);
					int idxLast = queryCursor
							.getColumnIndex(StructuredName.FAMILY_NAME);

					result.setGivenName(queryCursor.getString(idxFirst));
					result.setFamilyName(queryCursor.getString(idxLast));
				}
				else if (mimeType.equals(Phone.CONTENT_ITEM_TYPE))
				{
					int numberIdx = queryCursor.getColumnIndex(Phone.NUMBER);
					int typeIdx = queryCursor.getColumnIndex(Phone.TYPE);
					PhoneContact pc = new PhoneContact();
					pc.setData(queryCursor.getString(numberIdx));
					pc.setType(queryCursor.getInt(typeIdx));
					result.getContactMethods().add(pc);

				}
				else if (mimeType.equals(Email.CONTENT_ITEM_TYPE))
				{
					int dataIdx = queryCursor.getColumnIndex(Email.DATA);
					// int typeIdx =
					// emailCursor.getColumnIndex(CommonDataKinds.Email.TYPE);
					EmailContact pc = new EmailContact();
					pc.setData(queryCursor.getString(dataIdx));
					// pc.setType(emailCursor.getInt(typeIdx));
					result.getContactMethods().add(pc);

				}
				else if (mimeType.equals(Event.CONTENT_ITEM_TYPE))
				{
					int dateIdx = queryCursor.getColumnIndex(Event.START_DATE);
					String bday = queryCursor.getString(dateIdx);
					result.setBirthday(bday);

				}
				else if (mimeType.equals(Photo.CONTENT_ITEM_TYPE))
				{
					int colIdx = queryCursor.getColumnIndex(Photo.PHOTO);
					byte[] photo = queryCursor.getBlob(colIdx);
					result.setPhoto(photo);

				}
				else if (mimeType.equals(Note.CONTENT_ITEM_TYPE))
				{
					int colIdx = queryCursor.getColumnIndex(Note.NOTE);
					String note = queryCursor.getString(colIdx);
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
	
	public static void saveContact(Contact contact, ContentResolver cr) throws SyncException
	{
		Uri uri = null;

		String name = contact.getFullName();
		String firstName = contact.getGivenName();
		String lastName = contact.getFamilyName();

		String email = "";
		String phone = "";

		Log.d("ConH", "Saving Contact: \"" + name + "\"");

		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		boolean doMerge = false;

		/*
		if (contact.getId() == 0 && this.settings.getMergeContactsByName())
		{
			// find raw_contact by name
			String w = CommonDataKinds.StructuredName.DISPLAY_NAME + "='"
					+ name + "'";

			// Cursor c = cr.query(ContactsContract.RawContacts.CONTENT_URI,
			// null, w, null, null);
			Cursor c = cr.query(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI), null, w,
					null, null);

			if (c == null)
			{
				Log.d("ConH", "SC: faild to query for merge with contact: "
						+ name);
			}

			if (c.getCount() > 0)
			{
				c.moveToFirst();
				int rawIdIdx = c
						.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID);
				int rawID = c.getInt(rawIdIdx);
				contact.setId(rawID);
				doMerge = true;

				Log.d("ConH", "SC: Found Entry ID: " + rawID + " for contact: "
						+ name + " -> will merge now");
			}

			if (c != null) c.close();
		}
		*/

		if (contact.getId() == 0)
		{
			Log.d("ConH", "SC: Contact " + name + " is NEW -> insert");
			
//			String accountName = settings.getAccountName();
//			if("".equals(accountName)) accountName = null;
//			String accountType = settings.getAccountType();
//			if("".equals(accountType)) accountType = null;

			
//TODO: WE REALLY NEED AN ACCOUNT HERE, OTHERWISE => EXCEPTION !!!!
			
/*			
			ops.add(ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.RawContacts.CONTENT_URI))
	                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
	                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
	                .build());
*/	        
			ops.add(ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI))
	                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
	                .withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
	                .withValue(CommonDataKinds.StructuredName.DISPLAY_NAME, name)
	                .withValue(CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
	                .withValue(CommonDataKinds.StructuredName.FAMILY_NAME, lastName)
	                .build());
			
			if (contact.getBirthday() != null)
				ops.add(ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI))
						.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
		                .withValue(ContactsContract.Data.MIMETYPE,
		                        CommonDataKinds.Event.CONTENT_ITEM_TYPE)
		                .withValue(CommonDataKinds.Event.START_DATE, contact.getBirthday())
		                .withValue(CommonDataKinds.Event.TYPE, CommonDataKinds.Event.TYPE_BIRTHDAY)
		                .build());
			
			if (contact.getPhoto() != null)
				ops.add(ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI))
						.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
						.withValue(Photo.PHOTO, contact.getPhoto())
						.build());
			
			if (contact.getNotes() != null)
				ops.add(ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI))
						.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(ContactsContract.Data.MIMETYPE, Note.CONTENT_ITEM_TYPE)
						.withValue(Note.NOTE, contact.getNotes())
						.build());
			
			for (ContactMethod cm : contact.getContactMethods())
			{
				if (cm instanceof EmailContact)
				{
					email = cm.getData();

					ops.add(ContentProviderOperation
							.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI))
							.withValueBackReference(
									ContactsContract.Data.RAW_CONTACT_ID, 0)
							.withValue(ContactsContract.Data.MIMETYPE,
									CommonDataKinds.Email.CONTENT_ITEM_TYPE)
							.withValue(CommonDataKinds.Email.DATA, email)
							.withValue(CommonDataKinds.Email.TYPE, cm.getType())
							.build());
				}

				if (cm instanceof PhoneContact)
				{
					phone = cm.getData();

					ops.add(ContentProviderOperation
							.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI))
							.withValueBackReference(
									ContactsContract.Data.RAW_CONTACT_ID, 0)
							.withValue(ContactsContract.Data.MIMETYPE,
									CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
							.withValue(CommonDataKinds.Phone.NUMBER, phone)
							.withValue(CommonDataKinds.Phone.TYPE, cm.getType())
							.build());
				}
			}
		}
		else
		{
			Log.d("ConH", "SC. Contact " + name
					+ " already in Android book, MergeFlag: " + doMerge);

			Uri updateUri = ContactsContract.Data.CONTENT_URI;

			List<ContactMethod> cms = null;
			List<ContactMethod> mergedCms = new ArrayList<ContactMethod>();

			// first remove stuff that is in addressbook
			Cursor queryCursor;

			// update name (broken at the moment :()
			/*
			 * ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.
			 * CONTENT_URI) .withValue(ContactsContract.Data.RAW_CONTACT_ID,
			 * contact.getId()) .withValue(ContactsContract.Data.MIMETYPE,
			 * CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
			 * .withValue(CommonDataKinds.StructuredName.DISPLAY_NAME, name)
			 * .withValue(CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
			 * .withValue(CommonDataKinds.StructuredName.FAMILY_NAME, lastName)
			 * .build());
			 */

			// TODO All merge operations below should not delete the row if one
			// exists and re-insert it afterwards but instead run an update.
			// http://developer.android.com/reference/android/provider/ContactsContract.Data.html
			// states:
			//
			// RowID: Sync adapter should try to preserve row IDs during
			// updates. In other words, it would
			// be a bad idea to delete and reinsert a data row. A sync adapter
			// should always do an update instead.

			// birthday
			if (contact.getBirthday() != null
					&& !contact.getBirthday().equals(""))
			{

				String w = ContactsContract.Data.RAW_CONTACT_ID + "='"
						+ contact.getId() + "' AND "
						+ ContactsContract.Contacts.Data.MIMETYPE + " = '"
						+ CommonDataKinds.Event.CONTENT_ITEM_TYPE + "' AND "
						+ CommonDataKinds.Event.TYPE + " = '"
						+ CommonDataKinds.Event.TYPE_BIRTHDAY + "'";

				// Log.i("II", "w: " + w);

				queryCursor = cr.query(updateUri,
						new String[] { BaseColumns._ID }, w, null, null);

				if (queryCursor == null) throw new SyncException("EE",
						"cr.query returned null");

				if (queryCursor.moveToFirst()) // otherwise no events
				{
					int idCol = queryCursor.getColumnIndex(BaseColumns._ID);
					long id = queryCursor.getLong(idCol);

					ops.add(ContentProviderOperation
							.newUpdate(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI))
							.withSelection(BaseColumns._ID + "= ?",
									new String[] { String.valueOf(id) })
							.withValue(CommonDataKinds.Event.START_DATE,
									contact.getBirthday()).withExpectedCount(1)
							.build());

					Log.d("ConH", "Updating birthday: " + contact.getBirthday()
							+ " for contact " + name);
				}
				else
				{
					ops.add(ContentProviderOperation
							.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI))
							.withValue(ContactsContract.Data.RAW_CONTACT_ID,
									contact.getId())
							.withValue(ContactsContract.Data.MIMETYPE,
									CommonDataKinds.Event.CONTENT_ITEM_TYPE)
							.withValue(CommonDataKinds.Event.START_DATE,
									contact.getBirthday())
							.withValue(CommonDataKinds.Event.TYPE,
									CommonDataKinds.Event.TYPE_BIRTHDAY)
							.build());

					Log.d("ConH",
							"Inserting birthday: " + contact.getBirthday()
									+ " for contact " + name);
				}
			}

			// contact notes
			if (contact.getNotes() != null && !contact.getNotes().equals(""))
			{
				String w = ContactsContract.Data.RAW_CONTACT_ID + "='"
						+ contact.getId() + "' AND "
						+ ContactsContract.Contacts.Data.MIMETYPE + " = '"
						+ CommonDataKinds.Note.CONTENT_ITEM_TYPE + "'";

				queryCursor = cr.query(updateUri,
						new String[] { BaseColumns._ID }, w, null, null);

				if (queryCursor.moveToFirst())
				{
					long id = queryCursor.getLong(queryCursor
							.getColumnIndex(BaseColumns._ID));

					ops.add(ContentProviderOperation
							.newUpdate(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI))
							.withSelection(BaseColumns._ID + "= ?",
									new String[] { String.valueOf(id) })
							.withValue(CommonDataKinds.Note.NOTE,
									contact.getNotes()).withExpectedCount(1)
							.build());

					Log.d("ConH", "Updating notes for contact " + name);
				}
				else
				{
					ops.add(ContentProviderOperation
							.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI))
							.withValue(ContactsContract.Data.RAW_CONTACT_ID,
									contact.getId())
							.withValue(ContactsContract.Data.MIMETYPE,
									Note.CONTENT_ITEM_TYPE)
							.withValue(CommonDataKinds.Note.NOTE,
									contact.getNotes()).build());

					Log.d("ConH", "Inserting notes for contact " + name);
				}
			}

			// contact photo
			if (contact.getPhoto() != null)
			{
				String w = ContactsContract.Data.RAW_CONTACT_ID + "='"
						+ contact.getId() + "' AND "
						+ ContactsContract.Contacts.Data.MIMETYPE + " = '"
						+ CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'";

				queryCursor = cr.query(updateUri,
						new String[] { BaseColumns._ID }, w, null, null);

				if (queryCursor == null) throw new SyncException("EE",
						"cr.query returned null");

				if (queryCursor.moveToFirst()) // otherwise no photo
				{
					int colIdx = queryCursor.getColumnIndex(BaseColumns._ID);
					long id = queryCursor.getLong(colIdx);

					ops.add(ContentProviderOperation
							.newUpdate(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI))
							.withSelection(BaseColumns._ID + "= ?",
									new String[] { String.valueOf(id) })
							.withValue(CommonDataKinds.Photo.PHOTO,
									contact.getPhoto()).withExpectedCount(1)
							.build());

					Log.d("ConH", "Updating photo for contact " + name);
				}
				else
				{
					ops.add(ContentProviderOperation
							.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI))
							.withValue(ContactsContract.Data.RAW_CONTACT_ID,
									contact.getId())
							.withValue(ContactsContract.Data.MIMETYPE,
									CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
							.withValue(CommonDataKinds.Photo.PHOTO,
									contact.getPhoto()).build());
				}

				Log.d("ConH", "Inserting photo for contact " + name);
			}

			// phone
			{
				String w = ContactsContract.Data.RAW_CONTACT_ID + "='"
						+ contact.getId() + "' AND "
						+ ContactsContract.Contacts.Data.MIMETYPE + " = '"
						+ CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";

				// Log.i("II", "w: " + w);

				queryCursor = cr.query(updateUri, null, w, null, null);

				if (queryCursor == null) throw new SyncException("EE",
						"cr.query returned null");

				if (queryCursor.getCount() > 0) // otherwise no phone numbers
				{
					if (!queryCursor.moveToFirst()) return;
					int idCol = queryCursor
							.getColumnIndex(ContactsContract.Data._ID);
					int numberCol = queryCursor
							.getColumnIndex(CommonDataKinds.Phone.NUMBER);
					int typeCol = queryCursor
							.getColumnIndex(CommonDataKinds.Phone.TYPE);

					if (!doMerge)
					{
						do
						{
							ops.add(ContentProviderOperation
									.newDelete(
											addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI))
									.withSelection(
											ContactsContract.Data._ID + "=?",
											new String[] { String
													.valueOf(queryCursor
															.getInt(idCol)) })
									.build());
						} while (queryCursor.moveToNext());
					}
					else
					{
						for (ContactMethod cm : contact.getContactMethods())
						{
							if (!(cm instanceof PhoneContact)) continue;

							boolean found = false;
							String newNumber = cm.getData();
							int newType = cm.getType();

							do
							{
								String numberIn = queryCursor
										.getString(numberCol);
								int typeIn = queryCursor.getInt(typeCol);

								if (typeIn == newType
										&& numberIn.equals(newNumber))
								{
									Log.d("ConH", "SC: Found phone: "
											+ numberIn + " for contact " + name
											+ " -> wont add");
									found = true;
									break;
								}

							} while (queryCursor.moveToNext());

							if (!found)
							{
								mergedCms.add(cm);
							}
						}
					}
				}
				else
				{
					if (doMerge)
					{
						Log.d("ConH", "SC: No numbers in android for contact "
								+ name + " -> adding all");
						// we can add all new Numbers
						for (ContactMethod cm : contact.getContactMethods())
						{
							if (!(cm instanceof PhoneContact)) continue;
							mergedCms.add(cm);
						}
					}
				}
			}

			// mail
			{
				String w = ContactsContract.Data.RAW_CONTACT_ID + "='"
						+ contact.getId() + "' AND "
						+ ContactsContract.Contacts.Data.MIMETYPE + " = '"
						+ CommonDataKinds.Email.CONTENT_ITEM_TYPE + "'";

				queryCursor = cr.query(updateUri, null, w, null, null);

				if (queryCursor == null) throw new SyncException("EE",
						"cr.query returned null");

				if (queryCursor.getCount() > 0) // otherwise no email addresses
				{
					if (!queryCursor.moveToFirst()) return;
					int idCol = queryCursor
							.getColumnIndex(ContactsContract.Data._ID);
					int mailCol = queryCursor
							.getColumnIndex(CommonDataKinds.Email.DATA);
					int typeCol = queryCursor
							.getColumnIndex(CommonDataKinds.Email.TYPE);

					if (!doMerge)
					{
						do
						{
							ops.add(ContentProviderOperation
									.newDelete(
											addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI))
									.withSelection(
											ContactsContract.Data._ID + "=?",
											new String[] { String
													.valueOf(queryCursor
															.getInt(idCol)) })
									.build());
						} while (queryCursor.moveToNext());
					}
					else
					{
						for (ContactMethod cm : contact.getContactMethods())
						{
							if (!(cm instanceof EmailContact)) continue;

							boolean found = false;
							String newMail = cm.getData();
							int newType = cm.getType();

							do
							{
								String emailIn = queryCursor.getString(mailCol);
								int typeIn = queryCursor.getInt(typeCol);

								if (typeIn == newType
										&& emailIn.equals(newMail))
								{
									Log.d("ConH", "SC. Found email: " + emailIn
											+ " for contact " + name
											+ " -> wont add");
									found = true;
									break;
								}

							} while (queryCursor.moveToNext());

							if (!found)
							{
								mergedCms.add(cm);
							}
						}
					}
				}
				else
				{
					if (doMerge)
					{
						Log.d("ConH", "SC: No email in android for contact "
								+ name + " -> adding all");
						// we can add all new Numbers
						for (ContactMethod cm : contact.getContactMethods())
						{
							if (!(cm instanceof EmailContact)) continue;
							mergedCms.add(cm);
						}
					}
				}
			}

			// insert again
			if (doMerge)
			{
				cms = mergedCms;
			}
			else
			{
				cms = contact.getContactMethods();
			}

			// for (ContactMethod cm : contact.getContactMethods())
			for (ContactMethod cm : cms)
			{
				if (cm instanceof EmailContact)
				{
					email = cm.getData();
					Log.d("ConH", "SC: Writing mail: " + email
							+ " for contact " + name);

					ops.add(ContentProviderOperation
							.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI))
							.withValue(ContactsContract.Data.RAW_CONTACT_ID,
									contact.getId())
							.withValue(ContactsContract.Data.MIMETYPE,
									CommonDataKinds.Email.CONTENT_ITEM_TYPE)
							.withValue(CommonDataKinds.Email.DATA, email)
							.withValue(CommonDataKinds.Email.TYPE, cm.getType())
							.build());
				}

				if (cm instanceof PhoneContact)
				{
					phone = cm.getData();
					Log.d("ConH", "Writing phone: " + phone + " for contact "
							+ name);

					ops.add(ContentProviderOperation
							.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI))
							.withValue(ContactsContract.Data.RAW_CONTACT_ID,
									contact.getId())
							.withValue(ContactsContract.Data.MIMETYPE,
									CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
							.withValue(CommonDataKinds.Phone.NUMBER, phone)
							.withValue(CommonDataKinds.Phone.TYPE, cm.getType())
							.build());
				}
			}

			if (queryCursor != null) queryCursor.close();
		}

		// Log.i("II", "Creating contact: " + firstName + " " + lastName);
		try
		{
			ContentProviderResult[] results = cr.applyBatch(
					ContactsContract.AUTHORITY, ops);
			// store the first result: it contains the uri of the raw contact
			// with its ID
			if (contact.getId() == 0)
			{
				int newID = 0;
				uri = results[0].uri;
				//TODO: get ID from uri (this is just a dirty hack)
				String tmp = results[0].uri.toString();
				String[] a = tmp.split("/");
				int idx = a.length -1;
				
				//continue hack because of ?callerIsSyncAdapter=true behind uri
				if(a[idx] != null && a[idx].contains("?"))
				{
					String[] b = a[idx].split("\\?");
					newID = Integer.parseInt(b[0]);
				}
				
				contact.setId(newID);
			}
			else
			{
				uri = ContentUris.withAppendedId(
						ContactsContract.RawContacts.CONTENT_URI,
						contact.getId());
			}
			Log.d("ConH", "SC: Affected Uri was: " + uri);

		}
		catch (Exception e)
		{
			// Log exception
			Log.e("EE",
					"Exception encountered while inserting contact: "
							+ e.getMessage() + e.getStackTrace());
		}
/*
		CacheEntry result = new CacheEntry();
		result.setLocalId((int) ContentUris.parseId(uri));
		result.setLocalHash(contact.getLocalHash());
		result.setRemoteId(contact.getUid());
*/
		//localItemsCache.put(contact.getId(), contact);
		//return result;
	}
	
	private static Uri addCallerIsSyncAdapterParameter(Uri uri) {
        return uri.buildUpon().appendQueryParameter(
            ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
    }
}
