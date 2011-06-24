// Source: http://developer.android.com/resources/samples/SampleSyncAdapter/src/com/example/android/samplesync/platform/ContactOperations.html
/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package at.dasz.KolabDroid.ContactsContract;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.text.TextUtils;
import at.dasz.KolabDroid.Utils;

/**
 * Helper class for storing data in the platform content providers.
 */
public class ContactOperations
{
	private final ContentValues					mValues;

	private ContentProviderOperation.Builder	mBuilder;

	private final BatchOperation				mBatchOperation;

	private boolean								mYield;

	private long								mRawContactId;

	private int									mBackReference;

	private boolean								mIsNewContact;

	/**
	 * Returns an instance of ContactOperations instance for adding new contact
	 * to the platform contacts provider.
	 * 
	 * @param userId
	 *            the userId of the sample SyncAdapter user object
	 * @param accountName
	 *            the username of the current login
	 * @return instance of ContactOperations
	 */
	public static ContactOperations createNewContact(
			int userId, String accountName, BatchOperation batchOperation)
	{

		return new ContactOperations(userId, accountName,
				batchOperation);
	}

	/**
	 * Returns an instance of ContactOperations for updating existing contact in
	 * the platform contacts provider.
	 * 
	 * @param rawContactId
	 *            the unique Id of the existing rawContact
	 * @return instance of ContactOperations
	 */
	public static ContactOperations updateExistingContact(
			long rawContactId, BatchOperation batchOperation)
	{

		return new ContactOperations(rawContactId, batchOperation);
	}

	public ContactOperations(BatchOperation batchOperation)
	{
		mValues = new ContentValues();
		mYield = true;
		mBatchOperation = batchOperation;
	}

	public ContactOperations(int userId, String accountName,
			BatchOperation batchOperation)
	{

		this(batchOperation);
		mBackReference = mBatchOperation.size();
		mIsNewContact = true;
		mValues.put(RawContacts.SOURCE_ID, userId);
		mValues.put(RawContacts.ACCOUNT_TYPE, Utils.SYNC_ACCOUNT_TYPE);
		mValues.put(RawContacts.ACCOUNT_NAME, accountName);
		mBuilder = newInsertCpo(RawContacts.CONTENT_URI, true).withValues(
				mValues);
		mBatchOperation.add(mBuilder.build());
	}

	public ContactOperations(long rawContactId,
			BatchOperation batchOperation)
	{
		this(batchOperation);
		mIsNewContact = false;
		mRawContactId = rawContactId;
	}

	/**
	 * Adds a contact name
	 * 
	 * @param name
	 *            Name of contact
	 * @param nameType
	 *            type of name: family name, given name, etc.
	 * @return instance of ContactOperations
	 */
	public ContactOperations addName(String firstName, String lastName, String fullName)
	{

		mValues.clear();
		if (!TextUtils.isEmpty(firstName))
		{
			mValues.put(StructuredName.GIVEN_NAME, firstName);
			mValues.put(StructuredName.MIMETYPE,
					StructuredName.CONTENT_ITEM_TYPE);
		}
		if (!TextUtils.isEmpty(lastName))
		{
			mValues.put(StructuredName.FAMILY_NAME, lastName);
			mValues.put(StructuredName.MIMETYPE,
					StructuredName.CONTENT_ITEM_TYPE);
		}
		if (!TextUtils.isEmpty(fullName))
		{
			mValues.put(StructuredName.DISPLAY_NAME, fullName);
			mValues.put(StructuredName.MIMETYPE,
					StructuredName.CONTENT_ITEM_TYPE);
		}
		if (mValues.size() > 0)
		{
			addInsertOp();
		}
		return this;
	}

	/**
	 * Adds an email
	 * 
	 * @param new email for user
	 * @return instance of ContactOperations
	 */
	public ContactOperations addEmail(String email, int type)
	{
		mValues.clear();
		if (!TextUtils.isEmpty(email))
		{
			mValues.put(Email.DATA, email);
			mValues.put(Email.TYPE, type);
			mValues.put(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE);
			addInsertOp();
		}
		return this;
	}

	/**
	 * Adds a phone number
	 * 
	 * @param phone
	 *            new phone number for the contact
	 * @param phoneType
	 *            the type: cell, home, etc.
	 * @return instance of ContactOperations
	 */
	public ContactOperations addPhone(String phone, int phoneType)
	{
		mValues.clear();
		if (!TextUtils.isEmpty(phone))
		{
			mValues.put(Phone.NUMBER, phone);
			mValues.put(Phone.TYPE, phoneType);
			mValues.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
			addInsertOp();
		}
		return this;
	}

	public ContactOperations addBirthday(String birthday)
	{
		mValues.clear();
		if (!TextUtils.isEmpty(birthday))
		{
			mValues.put(Event.START_DATE, birthday);
			mValues.put(Event.TYPE, Event.TYPE_BIRTHDAY);
			mValues.put(Event.MIMETYPE, Event.CONTENT_ITEM_TYPE);
			addInsertOp();
		}
		return this;
	}

	public ContactOperations addNotes(String notes)
	{
		mValues.clear();
		if (!TextUtils.isEmpty(notes))
		{
			mValues.put(Note.NOTE, notes);
			mValues.put(Note.MIMETYPE, Note.CONTENT_ITEM_TYPE);
			addInsertOp();
		}
		return this;
	}

	public ContactOperations addPhoto(byte[] photo)
	{
		mValues.clear();
		if (photo != null)
		{
			mValues.put(Photo.PHOTO, photo);
			mValues.put(Photo.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
			addInsertOp();
		}
		return this;
	}
	
	public ContactOperations addWebpage(String webpage)
	{
		mValues.clear();
		if (!TextUtils.isEmpty(webpage))
		{
			mValues.put(Website.URL, webpage);
			mValues.put(Website.MIMETYPE, Website.TYPE_OTHER);
			addInsertOp();
		}
		return this;
	}	

	public ContactOperations addOrganization(String org)
	{
		mValues.clear();
		if (!TextUtils.isEmpty(org))
		{
			mValues.put(Organization.COMPANY, org);
			mValues.put(Organization.MIMETYPE, Organization.TYPE_WORK);
			addInsertOp();
		}
		return this;
	}

	public ContactOperations updateEmail(Uri uri, String email, int type)
	{
		mValues.clear();
		mValues.put(Email.DATA, email);
		mValues.put(Email.TYPE, type);
		addUpdateOp(uri);
		return this;
	}

	public ContactOperations updateName(Uri uri, String firstName,
			String lastName, String fullName)
	{

		mValues.clear();
		mValues.put(StructuredName.GIVEN_NAME, firstName);
		mValues.put(StructuredName.FAMILY_NAME, lastName);
		mValues.put(StructuredName.DISPLAY_NAME, fullName);
		addUpdateOp(uri);
		return this;
	}

	public ContactOperations updatePhone(Uri uri, String phone, int type)
	{
		mValues.clear();
		mValues.put(Phone.NUMBER, phone);
		mValues.put(Phone.TYPE, type);
		addUpdateOp(uri);
		return this;
	}

	public ContactOperations updatePhoto(Uri uri, byte[] photo)
	{
		if (photo != null)
		{
			mValues.clear();
			mValues.put(Photo.PHOTO, photo);
			addUpdateOp(uri);
		}
		else
		{
			addDeleteOp(uri);
		}
		return this;
	}

	public ContactOperations updateNotes(Uri uri, String notes)
	{
		if (!TextUtils.isEmpty(notes))
		{
			mValues.clear();
			mValues.put(Note.NOTE, notes);
			addUpdateOp(uri);
		}
		else
		{
			addDeleteOp(uri);
		}
		return this;
	}
	
	public ContactOperations updateWebpage(Uri uri, String webpage)
	{
		if (!TextUtils.isEmpty(webpage))
		{
			mValues.clear();
			mValues.put(Website.URL, webpage);
			addUpdateOp(uri);
		}
		else
		{
			addDeleteOp(uri);
		}
		return this;
	}
	
	public ContactOperations updateOrganization(Uri uri, String org)
	{
		if (!TextUtils.isEmpty(org))
		{
			mValues.clear();
			mValues.put(Organization.COMPANY, org);
			addUpdateOp(uri);
		}
		else
		{
			addDeleteOp(uri);
		}
		return this;
	}

	public ContactOperations updateBirthday(Uri uri, String birthday)
	{
		if (!TextUtils.isEmpty(birthday))
		{
			mValues.clear();
			mValues.put(Event.START_DATE, birthday);
			addUpdateOp(uri);
		}
		else
		{
			addDeleteOp(uri);
		}
		return this;
	}

	public void delete(Uri uri)
	{
		addDeleteOp(uri);
	}

	/**
	 * Adds an insert operation into the batch
	 */
	private void addInsertOp()
	{

		if (!mIsNewContact)
		{
			mValues.put(Phone.RAW_CONTACT_ID, mRawContactId);
		}
		mBuilder = newInsertCpo(
				addCallerIsSyncAdapterParameter(Data.CONTENT_URI), mYield);
		mBuilder.withValues(mValues);
		if (mIsNewContact)
		{
			mBuilder.withValueBackReference(Data.RAW_CONTACT_ID, mBackReference);
		}
		mYield = false;
		mBatchOperation.add(mBuilder.build());
	}

	/**
	 * Adds an update operation into the batch
	 */
	private void addUpdateOp(Uri uri)
	{
		mBuilder = newUpdateCpo(uri, mYield).withValues(mValues);
		mYield = false;
		mBatchOperation.add(mBuilder.build());
	}

	private void addDeleteOp(Uri uri)
	{
		mBuilder = newDeleteCpo(uri, mYield);
		mYield = false;
		mBatchOperation.add(mBuilder.build());
	}

	public static ContentProviderOperation.Builder newInsertCpo(Uri uri,
			boolean yield)
	{
		return ContentProviderOperation.newInsert(
				addCallerIsSyncAdapterParameter(uri)).withYieldAllowed(yield);
	}

	public static ContentProviderOperation.Builder newUpdateCpo(Uri uri,
			boolean yield)
	{
		return ContentProviderOperation.newUpdate(
				addCallerIsSyncAdapterParameter(uri)).withYieldAllowed(yield);
	}

	public static ContentProviderOperation.Builder newDeleteCpo(Uri uri,
			boolean yield)
	{
		return ContentProviderOperation.newDelete(
				addCallerIsSyncAdapterParameter(uri)).withYieldAllowed(yield);
	}

	private static Uri addCallerIsSyncAdapterParameter(Uri uri)
	{
		return uri
				.buildUpon()
				.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER,
						"true").build();
	}

	
}
