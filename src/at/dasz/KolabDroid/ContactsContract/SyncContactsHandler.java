/*
 * Copyright 2010 Arthur Zaczek <arthur@dasz.at>, dasz.at OG; All rights reserved.
 * Copyright 2010 David Schmitt <david@dasz.at>, dasz.at OG; All rights reserved.
 *
 *  This file is part of Kolab Sync for Android.

 *  Kolab Sync for Android is free software: you can redistribute it
 *  and/or modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation, either version 3 of
 *  the License, or (at your option) any later version.

 *  Kolab Sync for Android is distributed in the hope that it will be
 *  useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 *  of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.

 *  You should have received a copy of the GNU General Public License
 *  along with Kolab Sync for Android.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package at.dasz.KolabDroid.ContactsContract;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.mail.imap.IMAPBodyPart;
import com.sun.mail.imap.protocol.BODYSTRUCTURE;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
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
import at.dasz.KolabDroid.R;
import at.dasz.KolabDroid.Utils;
import at.dasz.KolabDroid.Provider.LocalCacheProvider;
import at.dasz.KolabDroid.Settings.Settings;
import at.dasz.KolabDroid.Sync.AbstractSyncHandler;
import at.dasz.KolabDroid.Sync.CacheEntry;
import at.dasz.KolabDroid.Sync.SyncContext;
import at.dasz.KolabDroid.Sync.SyncException;

public class SyncContactsHandler extends AbstractSyncHandler
{
	// private static final String[] PEOPLE_ID_PROJECTION = new String[] {
	// People._ID };

	/*
	 * private static final String[] PHONE_PROJECTION = new String[] {
	 * Contacts.Phones.TYPE, Contacts.Phones.NUMBER }; private static final
	 * String[] EMAIL_PROJECTION = new String[] { Contacts.ContactMethods.TYPE,
	 * Contacts.ContactMethods.DATA }; //private static final String[]
	 * PEOPLE_NAME_PROJECTION = new String[] { People.NAME }; private static
	 * final String[] CONTACT_NAME_PROJECTION = new String[] {
	 * CommonDataKinds.StructuredName.DISPLAY_NAME }; private static final
	 * String[] ID_PROJECTION = new String[] { "_id" }; private static final
	 * String EMAIL_FILTER = Contacts.ContactMethods.KIND + "=" +
	 * Contacts.KIND_EMAIL;
	 */

	private final String				defaultFolderName;
	private final LocalCacheProvider	cacheProvider;
	private final ContentResolver		cr;
	private final Context				ctx;
	private HashMap<Integer, Contact>	localItemsCache;

	public SyncContactsHandler(Context context, Account account)
	{
		super(context, account);
		Settings s = new Settings(context);
		settings = s;
		defaultFolderName = s.getContactsFolder();
		cacheProvider = new LocalCacheProvider.ContactsCacheProvider(context);
		cr = context.getContentResolver();
		ctx = context;
		status.setTask("Contacts");
	}

	public String getDefaultFolderName()
	{
		return defaultFolderName;
	}

	public boolean shouldProcess()
	{
		boolean hasFolder = (defaultFolderName != null && !""
				.equals(defaultFolderName));
		return hasFolder;
	}

	public LocalCacheProvider getLocalCacheProvider()
	{
		return cacheProvider;
	}

	public Set<Integer> getAllLocalItemsIDs()
	{
		return localItemsCache.keySet();
	}

	public void fetchAllLocalItems() throws SyncException
	{
		localItemsCache = new HashMap<Integer, Contact>();
		Cursor personCursor = getAllLocalItemsCursor();
		try
		{
			while (personCursor.moveToNext())
			{
				Contact result = loadItem(personCursor);
				if(result != null) {
					localItemsCache.put(result.getId(), result);
				}
			}
		}
		finally
		{
			if (personCursor != null) personCursor.close();
		}
	}

	public Cursor getAllLocalItemsCursor()
	{
		// return only those which are not deleted by other programs
		// String where = ContactsContract.RawContacts.DELETED+"='0'";

		// return all again
		//return cr.query(ContactsContract.RawContacts.CONTENT_URI, null, null,
		//		null, null);
		
		//only return those from our account
		String where = ContactsContract.RawContacts.ACCOUNT_NAME +"=? and " +
						ContactsContract.RawContacts.ACCOUNT_TYPE + "=?";		
		
		//TODO: maybe we should use the account.name and account.type instead of string 
		
		return cr.query(ContactsContract.RawContacts.CONTENT_URI, null, where,
				new String[]{ctx.getString(R.string.SYNC_ACCOUNT_NAME), ctx.getString(R.string.SYNC_ACCOUNT_TYPE)}, null);
	}

	public int getIdColumnIndex(Cursor c)
	{
		return c.getColumnIndex(ContactsContract.RawContacts._ID);
	}

	@Override
	public void createLocalItemFromServer(Session session, Folder targetFolder,
			SyncContext sync) throws MessagingException,
			ParserConfigurationException, IOException, SyncException
	{
		Log.d("sync", "Downloading item ...");
		try
		{
			InputStream xmlinput = extractXml(sync.getMessage());
			Document doc = Utils.getDocument(xmlinput);
			updateLocalItemFromServer(sync, doc);
			updateCacheEntryFromMessage(sync, doc);

			/* TODO: We will ignore Merge by name for now
			if (this.settings.getMergeContactsByName())
			{
				Log.d("ConH", "Preparing upload of Contact after merge");
				sync.setLocalItem(null);
				getLocalItem(sync); // fetch updates which were just done

				Log.d("ConH",
						"Fetched data after merge for "
								+ ((Contact) sync.getLocalItem()).getFullName());

				updateServerItemFromLocal(sync, doc);

				Log.d("ConH", "Server item updated after merge");

				// Create & Upload new Message
				// IMAP needs a new Message uploaded
				String xml = Utils.getXml(doc);
				Message newMessage = wrapXmlInMessage(session, sync, xml);
				targetFolder.appendMessages(new Message[] { newMessage });
				newMessage.saveChanges();

				// Delete old message
				sync.getMessage().setFlag(Flag.DELETED, true);
				// Replace sync context with new message
				sync.setMessage(newMessage);

				Log.d("ConH", "IMAP Message replaced after merge");

				updateCacheEntryFromMessage(sync, doc);
			}
			*/

		}
		catch (SAXException ex)
		{
			throw new SyncException(getItemText(sync), "Unable to extract XML Document", ex);
		}
	}

	@Override
	protected void updateLocalItemFromServer(SyncContext sync, Document xml)
			throws SyncException
	{
		Log.d("ConH", "this is updateLocalItemFromServer");
		
		Contact contact = (Contact) sync.getLocalItem();
		if (contact == null)
		{
			Log.d("ConH", "NEW local contact");
			contact = new Contact();
		}
		else
			Log.d("ConH", "Existing local contact");
		
		Element root = xml.getDocumentElement();

		contact.setUid(Utils.getXmlElementString(root, "uid"));		
		Log.d("ConH", "Remote UID: " + contact.getUid());

		Element name = Utils.getXmlElement(root, "name");
		if (name != null)
		{
			// contact.setFullName(Utils.getXmlElementString(name,
			// "full-name"));
			String fullName = Utils.getXmlElementString(name, "full-name");
			if (fullName != null)
			{
				Log.d("ConH", "Full-name element exists => split to given and family name");
				String[] names = fullName.split(" ");
				if (names.length == 2)
				{
					contact.setGivenName(names[0]);
					contact.setFamilyName(names[1]);
				}
				else
				{
					Log.w("ConH", "Full-name element exists without space => set only given name");
					contact.setGivenName(fullName);
					contact.setFamilyName("");
				}
			}
			else
			{
				Log.w("ConH", "Full-name element does NOT exist => use given and last name");
				//use given and last name
				String givenName = Utils.getXmlElementString(name, "given-name");
				String lastName = Utils.getXmlElementString(name, "last-name");
				if(givenName != null && lastName != null)
				{
					contact.setGivenName(givenName);
					contact.setFamilyName(lastName);
				}
				else if(givenName != null && lastName == null)
				{
					Log.w("ConH", "Full-name element does NOT exist => ONLY given-name");
					contact.setGivenName(givenName);
					contact.setFamilyName("");
				}
				else
				{
					Log.w("ConH", "Full-name element does NOT exist => ONLY last-name");
					contact.setGivenName("");
					contact.setFamilyName(lastName);
				}
			}
		}

		contact.setBirthday(Utils.getXmlElementString(root, "birthday"));
		Log.d("ConH", "Set Birthday to: " + contact.getBirthday());

		contact.getContactMethods().clear();
		NodeList nl = Utils.getXmlElements(root, "phone");
		for (int i = 0; i < nl.getLength(); i++)
		{
			ContactMethod cm = new PhoneContact();
			cm.fromXml((Element) nl.item(i));
			contact.getContactMethods().add(cm);
			Log.d("ConH", "Add phone with: " + cm);
		}
		nl = Utils.getXmlElements(root, "email");
		for (int i = 0; i < nl.getLength(); i++)
		{
			ContactMethod cm = new EmailContact();
			cm.fromXml((Element) nl.item(i));
			contact.getContactMethods().add(cm);
			Log.d("ConH", "Add email with: " + cm);
		}

		byte[] photo = getPhotoFromMessage(sync.getMessage(), xml);
		contact.setPhoto(photo);
		Log.d("ConH", "Set Photo to: " + contact.getPhoto());

		contact.setNote(Utils.getXmlElementString(root, "body"));
		Log.d("ConH", "Set Notes to: " + contact.getNotes());

		sync.setCacheEntry(saveContact(contact));
	}

	@Override
	protected void updateServerItemFromLocal(SyncContext sync, Document xml)
			throws SyncException, MessagingException
	{
		Contact source = getLocalItem(sync);
		CacheEntry entry = sync.getCacheEntry();

		entry.setLocalHash(source.getLocalHash());
		final Date lastChanged = new Date();
		entry.setRemoteChangedDate(lastChanged);

		writeXml(sync, xml, source, lastChanged);
	}

	private void writeXml(SyncContext sync, Document xml, Contact source,
			final Date lastChanged)
	{
		Element root = xml.getDocumentElement();

		// TODO: needs to be above contact information (Kmail bug?)
		// Kmail seems to be picky about <phone> and <email> elements they
		// should be right after each other

		// remove it for now
		Utils.deleteXmlElements(root, "last-modification-date");
		// we do not need this one for now
		// if we need it, put below contact methods (otherwise kmail
		// complains)...
		// TODO: what shall we do with this entry? :)
		Utils.deleteXmlElements(root, "preferred-address");

		/*
		 * Utils.setXmlElementValue(xml, root, "last-modification-date", Utils
		 * .toUtc(lastChanged));
		 */
		Utils.setXmlElementValue(xml, root, "uid", source.getUid());

		Element name = Utils.getOrCreateXmlElement(xml, root, "name");
		Utils.setXmlElementValue(xml, name, "full-name", source.getFullName());
		Utils.setXmlElementValue(xml, name, "given-name", source.getGivenName());
		Utils.setXmlElementValue(xml, name, "last-name", source.getFamilyName());

		if(source.getBirthday() != null && !"".equals(source.getBirthday()))
			Utils.setXmlElementValue(xml, root, "birthday", source.getBirthday());

		if(source.getNotes() != null && !"".equals(source.getNotes()))
			Utils.setXmlElementValue(xml, root, "body", source.getNotes());

		if(source.getPhoto() != null && !"".equals(source.getPhoto()))
		{
			//Log.d("ConH", "writeXml Photo Hash: " + Utils.getBytesAsHexString(Utils.sha1Hash(source.getPhoto())));
			storePhotoInMessage(sync, xml, source.getPhoto());	
		}
		else
			Utils.deleteXmlElements(root, "picture"); //remove the picture

		Utils.deleteXmlElements(root, "phone");
		Utils.deleteXmlElements(root, "email");

		for (ContactMethod cm : source.getContactMethods())
		{
			cm.toXml(xml, root, source.getFullName());
		}
	}

	@Override
	protected String writeXml(SyncContext sync)
			throws ParserConfigurationException, SyncException,
			MessagingException
	{
		Contact source = getLocalItem(sync);
		CacheEntry entry = sync.getCacheEntry();

		entry.setLocalHash(source.getLocalHash());
		final Date lastChanged = new Date();
		entry.setRemoteChangedDate(lastChanged);
		final String newUid = getNewUid();
		entry.setRemoteId(newUid);
		source.setUid(newUid);

		Document xml = Utils.newDocument("contact");
		writeXml(sync, xml, source, lastChanged);

		return Utils.getXml(xml);
	}

	@Override
	protected String getMimeType()
	{
		return "application/x-vnd.kolab.contact";
	}

	public boolean hasLocalItem(SyncContext sync) throws SyncException,
			MessagingException
	{
		return getLocalItem(sync) != null;
	}

	public boolean hasLocalChanges(SyncContext sync) throws SyncException,
			MessagingException
	{
		CacheEntry e = sync.getCacheEntry();
		Contact contact = getLocalItem(sync);;
		String entryHash = e.getLocalHash();
		String contactHash = contact != null ? contact.getLocalHash() : "";
		return !entryHash.equals(contactHash);
	}

	@Override
	public void deleteLocalItem(int localId)
	{
		Log.d("ConH", "Deleting local item from Db with raw_contact ID: " + localId);
		
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		//TODO: who put this syncadapter to the first call and why is it still working? O_o
		
		// normal delete first, then with syncadapter flag
		Uri rawUri = addCallerIsSyncAdapterParameter(ContactsContract.RawContacts.CONTENT_URI);
		ops.add(ContentProviderOperation
				.newDelete(rawUri)
				.withSelection(ContactsContract.RawContacts._ID + "=?",
						new String[] { String.valueOf(localId) }).build());

		// remove contact from raw_contact table (this time with syncadapter
		// flag set)
		rawUri = addCallerIsSyncAdapterParameter(ContactsContract.RawContacts.CONTENT_URI);
		ops.add(ContentProviderOperation
				.newDelete(rawUri)
				.withSelection(ContactsContract.RawContacts._ID + "=?",
						new String[] { String.valueOf(localId) }).build());

		try
		{
			cr.applyBatch(ContactsContract.AUTHORITY, ops);
		}
		catch (Exception e)
		{
			Log.e("EE", e.toString());
		}

	}

	private void deleteLocalItemFinally(int localId)
	{
		Log.d("ConH", "Delete raw_contract from DB with raw_contact ID: " + localId);
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		// remove contact from raw_contact table (with syncadapter flag set)
		Uri rawUri = addCallerIsSyncAdapterParameter(ContactsContract.RawContacts.CONTENT_URI);
		ops.add(ContentProviderOperation
				.newDelete(rawUri)
				.withSelection(ContactsContract.RawContacts._ID + "=?",
						new String[] { String.valueOf(localId) }).build());

		try
		{
			cr.applyBatch(ContactsContract.AUTHORITY, ops);
		}
		catch (Exception e)
		{
			Log.e("EE", e.toString());
		}

	}

	@Override
	public void deleteServerItem(SyncContext sync) throws MessagingException,
			SyncException
	{
		Log.d("sync", "Deleting from server: " + sync.getMessage().getSubject());
		sync.getMessage().setFlag(Flag.DELETED, true);
		// remove contents too, to avoid confusing the butchered JAF
		// message.setContent("", "text/plain");
		// message.saveChanges();
		
		//TODO: the local item doesn't exist here anyway, does it?
		
		Log.d("sync", "Deleting local cache entry with id: " + sync.getCacheEntry().getId());
		getLocalCacheProvider().deleteEntry(sync.getCacheEntry());

		// make sure it gets flushed from the raw_contacts table on the phone as
		// well
		deleteLocalItemFinally(sync.getCacheEntry().getLocalId());
	}

	private CacheEntry saveContact(Contact contact) throws SyncException
	{				
		ContactDBHelper.saveContact(contact, this.ctx);

		CacheEntry result = new CacheEntry();
		//result.setLocalId((int) ContentUris.parseId(uri));
		result.setLocalId(contact.getId());
		result.setLocalHash(contact.getLocalHash());
		result.setRemoteId(contact.getUid());

		//localItemsCache.put(contact.getId(), contact);
		localItemsCache.put(contact.getId(), contact);
		
		Log.d("ConH", "Contact saved with: " + result);
		
		return result;
	}

	private Contact getLocalItem(SyncContext sync) throws SyncException,
			MessagingException
	{
		Log.d("ConH", "This is getLocalItem");
		
		if (sync.getLocalItem() != null)
		{
			Log.d("ConH", "return local item as contact");
			return (Contact) sync.getLocalItem();
		}

		Log.d("ConH", "Fetch contact from localcache with localID: " + sync.getCacheEntry().getLocalId());
		Contact c = localItemsCache.get(sync.getCacheEntry().getLocalId());
		if (c != null)
		{
			Log.d("ConH", "Change Uid of contact to cacheentry.remoteId: " + sync.getCacheEntry().getRemoteId());
			c.setUid(sync.getCacheEntry().getRemoteId());
		}
		sync.setLocalItem(c);
		return c;
	}

	private Contact loadItem(Cursor personCursor) throws SyncException
	{
		Cursor queryCursor = null;
		try
		{
			int idxID = personCursor
					.getColumnIndex(CommonDataKinds.StructuredName._ID);
			int id = personCursor.getInt(idxID);

			String where = ContactsContract.Data.RAW_CONTACT_ID + "=?";

			// Log.i("II", "where: " + where);
			
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

	private String getNewUid()
	{
		// Create Application and Type specific id
		// kd == Kolab Droid, ct = contact
		return "kd-ct-" + UUID.randomUUID().toString();
	}

	@Override
	protected String getMessageBodyText(SyncContext sync) throws SyncException,
			MessagingException
	{
		Contact contact = getLocalItem(sync);
		StringBuilder sb = new StringBuilder();

		String fullName = contact.getFullName();
		sb.append(fullName == null ? "(no name)" : fullName);
		sb.append("\n");
		sb.append("----- Contact Methods -----\n");
		for (ContactMethod cm : contact.getContactMethods())
		{
			sb.append(cm.getData());
			sb.append("\n");
		}

		return sb.toString();
	}

	@Override
	public String getItemText(SyncContext sync) throws MessagingException
	{
		if (sync.getLocalItem() != null)
		{
			Contact item = (Contact) sync.getLocalItem();
			return item.getFullName();
		}
		else
		{
			return sync.getMessage().getSubject();
		}
	}

	/**
	 * Extracts the contact photo from the given message if one exists and
	 * returns it as byte array.
	 * 
	 * @param message
	 *            The message whose contact photo is to be returned.
	 * @return A byte array of the contact photo of the given message or null if
	 *         no photo exists.
	 */
	private byte[] getPhotoFromMessage(Message message, Document messageXml)
	{
		Element root = messageXml.getDocumentElement();
		String photoFileName = Utils.getXmlElementString(root, "picture");
		try
		{
			if(message.getContent() instanceof Multipart) //hopefully prevents nullpointer (if my log message doesnt create a new one, that is :)
			{
				Multipart multipart = (Multipart) message.getContent();
	
				for (int i = 0, n = multipart.getCount(); i < n; i++)
				{
					Part part = multipart.getBodyPart(i);
					String disposition = part.getDisposition();
	
					if ((part.getFileName() != null)
							&& (part.getFileName().equals(photoFileName))
							&& (disposition != null)
							&& ((disposition.equalsIgnoreCase(Part.ATTACHMENT) || (disposition
									.equalsIgnoreCase(Part.INLINE))))) {
	
					return inputStreamToBytes(part.getInputStream()); }
				}
			}
			else
			{
				Log.w("ConH", "getPhotoFromMessage: Strange message content of type: " + message.getContent().getClass().getCanonicalName());
			}
		}
		catch (IOException ex)
		{
			Log.w("ConH", ex);
		}
		catch (MessagingException ex)
		{
			Log.w("ConH", ex);
		}

		return null;
	}

	/**
	 * Stores the photo in the given byte array as attachment of the given
	 * {@link Message} with the filename 'kolab-picture.png' and removes an
	 * existing contact photo if it exists.
	 * 
	 * @param message
	 *            The {@link Message} where the attachment is to be stored.
	 * @param messageXml
	 *            The xml document of the kolab message.
	 * @param photo
	 *            a byte array of the photo to be stored or <code>null</code> if
	 *            no photo is to be stored.
	 */
	private void storePhotoInMessage(SyncContext sync, Document messageXml,
			byte[] photo)
	{
		//We store the photo in SyncContext.newMessageContent because the Message itself is readonly!
		
		Element root = messageXml.getDocumentElement();
		Utils.setXmlElementValue(messageXml, root, "picture", "kolab-picture.png");
		
		Utils.setXmlElementValue(messageXml, root, "picture",
				"kolab-picture.png");

		// create new attachment for new photo
		// http://java.sun.com/developer/onlineTraining/JavaMail/contents.html#SendingAttachments explains how

		MimeMultipart mp = new MimeMultipart();		
		try
		{
			//add picture as kolab-picture.png
			if(photo != null)
			{
				BodyPart part = new MimeBodyPart();
				DataSource source = new ByteArrayDataSource(photo, "image/png");
				part.setDataHandler(new DataHandler(source));
				part.setFileName("kolab-picture.png");
				
				//Log.d("ConH", "storePhotoinMsg 1 Photo Hash: " + Utils.getBytesAsHexString(Utils.sha1Hash(photo)));
				
				mp.addBodyPart(part);
			}
			
			sync.setNewMessageContent(mp);
		}
		catch (MessagingException ex)
		{
			// TODO Auto-generated catch block
			Log.e("ConH", ex.toString());
		}
	}

	/**
	 * Reads the given {@link InputStream} and returns its contents as byte
	 * array.
	 * 
	 * @param in
	 *            The {@link InputStream} to be read.
	 * @return a byte array with the contents of the given {@link InputStream}.
	 * @throws IOException
	 */
	private byte[] inputStreamToBytes(InputStream in) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		byte[] buffer = new byte[1024];
		int len;

		while ((len = in.read(buffer)) >= 0)
			out.write(buffer, 0, len);

		in.close();
		out.close();
		return out.toByteArray();
	}
	
	private static Uri addCallerIsSyncAdapterParameter(Uri uri) {
        return uri.buildUpon().appendQueryParameter(
            ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
    }
}
