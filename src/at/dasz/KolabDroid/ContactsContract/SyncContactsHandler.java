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

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Flags.Flag;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
		return cr.query(ContactsContract.RawContacts.CONTENT_URI, null, null,
				null, null);
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

		}
		catch (SAXException ex)
		{
			throw new SyncException(getItemText(sync),
					"Unable to extract XML Document", ex);
		}
	}

	@Override
	protected void updateLocalItemFromServer(SyncContext sync, Document xml)
			throws SyncException
	{
		Contact contact = (Contact) sync.getLocalItem();
		if (contact == null)
		{
			contact = new Contact();
		}
		Element root = xml.getDocumentElement();

		contact.setUid(Utils.getXmlElementString(root, "uid"));

		Element name = Utils.getXmlElement(root, "name");
		if (name != null)
		{
			// contact.setFullName(Utils.getXmlElementString(name,
			// "full-name"));
			String fullName = Utils.getXmlElementString(name, "full-name");
			if (fullName != null)
			{
				String[] names = fullName.split(" ");
				if (names.length == 2)
				{
					contact.setGivenName(names[0]);
					contact.setFamilyName(names[1]);
				}
			}
		}

		contact.setBirthday(Utils.getXmlElementString(root, "birthday"));

		contact.getContactMethods().clear();
		NodeList nl = Utils.getXmlElements(root, "phone");
		for (int i = 0; i < nl.getLength(); i++)
		{
			ContactMethod cm = new PhoneContact();
			cm.fromXml((Element) nl.item(i));
			contact.getContactMethods().add(cm);
		}
		nl = Utils.getXmlElements(root, "email");
		for (int i = 0; i < nl.getLength(); i++)
		{
			ContactMethod cm = new EmailContact();
			cm.fromXml((Element) nl.item(i));
			contact.getContactMethods().add(cm);
		}

		byte[] photo = getPhotoFromMessage(sync.getMessage(), xml);
		contact.setPhoto(photo);

		contact.setNote(Utils.getXmlElementString(root, "body"));

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

		Utils.setXmlElementValue(xml, root, "birthday", source.getBirthday());

		Utils.setXmlElementValue(xml, root, "body", source.getNotes());

		// TODO The method call below is not yet functional because the method
		// implementation is not yet complete
		storePhotoInMessage(sync.getMessage(), xml, source.getPhoto());

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
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

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
		return result;
	}

	private Contact getLocalItem(SyncContext sync) throws SyncException,
			MessagingException
	{
		if (sync.getLocalItem() != null) return (Contact) sync.getLocalItem();

		Contact c = localItemsCache.get(sync.getCacheEntry().getLocalId());
		if (c != null)
		{
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
			Multipart multipart = (Multipart) message.getContent();

			for (int i = 0, n = multipart.getCount(); i < n; i++)
			{
				Part part = multipart.getBodyPart(i);
				String disposition = part.getDisposition();

				if ((part.getFileName() != null)
						&& (part.getFileName().equals(photoFileName))
						&& (disposition != null)
						&& ((disposition.equals(Part.ATTACHMENT) || (disposition
								.equals(Part.INLINE))))) {

				return inputStreamToBytes(part.getInputStream()); }
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
	private void storePhotoInMessage(Message message, Document messageXml,
			byte[] photo)
	{
		Element root = messageXml.getDocumentElement();
		Utils.setXmlElementValue(messageXml, root, "picture", "kolab-picture.png");
		
		// TODO uncomment and test this method
		
		Utils.setXmlElementValue(messageXml, root, "picture",
				"kolab-picture.png");

		// TODO complete this method

		// delete existing photo if any

		// create new attachment for new photo
		// http://java.sun.com/developer/onlineTraining/JavaMail/contents.html#SendingAttachments explains how
/*
		try
		{
			if(message.getContent() instanceof MimeMultipart)
			{
				//TODO: this gives us a copy instead of a reference to the message content?
				MimeMultipart mmp = (MimeMultipart) message.getContent();
				
				//find picture attachment and remove it
				int removePartNo = -1;
				for(int i=0; i < mmp.getCount(); i++)
				{
					Part p = mmp.getBodyPart(i);
					//if ("kolab-picture.png".equals(p.getFileName()) &&
					//	p.getContentType().equals("image/png"))
					if ("kolab-picture.png".equals(p.getFileName()))
					{
						removePartNo = i;
					}
				}
				if(removePartNo > -1)
				{
					mmp.removeBodyPart(removePartNo);
				}
				
				//add picture as kolab-picture.png
				if(photo != null)
				{
					//TODO: which part type? IMAPBodyPart?
					BodyPart part = new MimeBodyPart();
					//TODO: type with name?
					DataSource source = new ByteArrayDataSource(photo, "image/png");
					part.setDataHandler(new DataHandler(source));
					part.setFileName("kolab-picture.png");
					
					mmp.addBodyPart(part);
				}
				
				Log.d("ConH:", "multipart complete?");
			}
		}
		catch (IOException ex)
		{
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
		catch (MessagingException ex)
		{
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}

		Log.d("ConH", "bla");
*/
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
