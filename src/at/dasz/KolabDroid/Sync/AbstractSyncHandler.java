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

package at.dasz.KolabDroid.Sync;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Flags.Flag;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import android.accounts.Account;
import android.content.Context;
import android.text.format.Time;
import android.util.Log;
import at.dasz.KolabDroid.Utils;
import at.dasz.KolabDroid.Settings.Settings;

/**
 * This class contains common code for all local data stores and defines the
 * store-specific functionality that has to be implemented by the store.
 */
public abstract class AbstractSyncHandler implements SyncHandler
{
	private static final String	TAG	= "sync";

	protected AbstractSyncHandler(Context context, Account account)
	{
		this.context = context;
		this.account = account;
		status = new StatusEntry();
		Time t = new Time();
		t.setToNow();
		status.setTime(t);
		
		settings = new Settings(context);
		diagLog = settings.getDiagLog();
	}

	protected StatusEntry	status;
	protected Context		context;
	protected Account		account;

	protected Settings		settings;
	protected boolean 		diagLog = false;

	protected abstract String getMimeType();

	/**
	 * Create the XML in kolab format to describe the specified item.
	 * 
	 * @param sync
	 * @return String containing the XML describing the item
	 * @throws ParserConfigurationException
	 * @throws SyncException
	 * @throws MessagingException
	 */
	protected abstract String createNewXml(SyncContext sync)
			throws ParserConfigurationException, SyncException,
			MessagingException;

	/**
	 * Create a human readable description of the specified item to be displayed
	 * as text/plain part in the mail.
	 * 
	 * @param sync
	 * @return
	 * @throws SyncException
	 * @throws MessagingException
	 */
	protected abstract String getMessageBodyText(SyncContext sync)
			throws SyncException, MessagingException;

	/**
	 * Create a short human readable string describing an item for log messages.
	 * 
	 * @param sync
	 * @return
	 * @throws MessagingException
	 */
	public abstract String getItemText(SyncContext sync)
			throws MessagingException;

	/**
	 * Update the local item from the specified XML Document.
	 * 
	 * @param sync
	 * @param xml
	 * @throws SyncException
	 */
	protected abstract void updateLocalItemFromServer(SyncContext sync,
			Document xml) throws SyncException;

	/**
	 * Update the specified XML Document from the local item.
	 * 
	 * @param sync
	 * @param xml
	 * @throws SyncException
	 * @throws MessagingException
	 */
	protected abstract void updateServerItemFromLocal(SyncContext sync,
			Document xml) throws SyncException, MessagingException;

	/**
	 * Delete the local item with the specified ID.
	 * 
	 * @param localId
	 * @throws SyncException
	 */
	public abstract void deleteLocalItem(int localId) throws SyncException;

	public StatusEntry getStatus()
	{
		return status;
	}

	public void createLocalItemFromServer(Session session, Folder folder,
			SyncContext sync) throws MessagingException,
			ParserConfigurationException, IOException, SyncException
	{
		Log.d("sync", "Downloading item ...");
		try
		{
			InputStream xmlinput = extractXml(sync.getMessage());
			if (xmlinput == null) throw new SyncException(getItemText(sync),
					"Unable to find XML Document");
			Document doc = Utils.getDocument(xmlinput);
			updateLocalItemFromServer(sync, doc);
			updateCacheEntryFromMessage(sync, doc);
		}
		catch (SAXException ex)
		{
			throw new SyncException(getItemText(sync),
					"Unable to parse XML Document", ex);
		}
	}

	public void updateLocalItemFromServer(SyncContext sync)
			throws MessagingException, ParserConfigurationException,
			IOException, SyncException
	{
		if (hasLocalItem(sync))
		{
			Log.d("sync", "Updating without conflict check: "
					+ sync.getCacheEntry().getLocalId());
			try
			{
				InputStream xmlinput = extractXml(sync.getMessage());
				if (xmlinput == null) throw new SyncException(
						getItemText(sync), "Unable to find XML Document");
				Document doc = Utils.getDocument(xmlinput);
				updateLocalItemFromServer(sync, doc);
				updateCacheEntryFromMessage(sync, doc);
			}
			catch (SAXException ex)
			{
				throw new SyncException(getItemText(sync),
						"Unable to parse XML Document", ex);
			}
		}
	}

	protected void updateCacheEntryFromMessage(SyncContext sync, Document doc)
			throws MessagingException
	{
		Log.d("ConH", "This is updateCacheEntryFromMessage");

		CacheEntry c = sync.getCacheEntry();
		Message m = sync.getMessage();
		Date dt = Utils.getMailDate(m);
		c.setRemoteChangedDate(dt);
		c.setRemoteId(m.getSubject());
		c.setRemoteSize(m.getSize());
		try
		{
			if (doc == null)
			{
				// set correct remote hash
				InputStream is = extractXml(m);
				if (is != null)
				{
					doc = Utils.getDocument(is);
				}
			}

			if (doc != null)
			{
				String docText = Utils.getXml(doc.getDocumentElement());

				// Log.v("ASH DocDebug:", docText);

				byte[] remoteHash = Utils.sha1Hash(docText);
				c.setRemoteHash(remoteHash);
			}
		}
		catch (Exception ex)
		{
			Log.e("EE", ex.toString());
		}

		Log.d("ASH", "Updated Cacheentry to: " + c);

		getLocalCacheProvider().saveEntry(c);
	}

	public void createServerItemFromLocal(Session session, Folder targetFolder,
			SyncContext sync, int localId) throws MessagingException,
			ParserConfigurationException, SyncException
	{
		Log.d("sync", "Uploading: localID #" + localId);

		// initialize cache entry with values that should go
		// into the new server item
		CacheEntry entry = new CacheEntry();
		entry.setLocalId(localId);
		sync.setCacheEntry(entry);

		Log.d("ASH", "Created new Cacheentry with localID: " + localId);

		Log.d("ASH", "Writing XML and uploading IMAP message");

		String xml = createNewXml(sync);
		Message m = wrapXmlInMessage(session, sync, xml);
		targetFolder.appendMessages(new Message[] { m });
		m.saveChanges();
		sync.setMessage(m);
		// TODO: Improve that
		updateCacheEntryFromMessage(sync, null);
	}

	public void updateServerItemFromLocal(Session session, Folder targetFolder,
			SyncContext sync) throws MessagingException, IOException,
			SyncException, ParserConfigurationException
	{
		Log.d("sync", "Update item on Server: #"
				+ sync.getCacheEntry().getLocalId());

		InputStream xmlinput = extractXml(sync.getMessage());
		if (xmlinput == null) throw new SyncException(getItemText(sync),
				"Unable to find XML Document");
		try
		{
			// Parse XML
			Document doc = Utils.getDocument(xmlinput);

			// Update
			updateServerItemFromLocal(sync, doc);

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

			updateCacheEntryFromMessage(sync, doc);
		}
		catch (SAXException ex)
		{
			throw new SyncException(getItemText(sync),
					"Unable to extract XML Document", ex);
		}
		finally
		{
			if (xmlinput != null) xmlinput.close();
		}
	}

	public void deleteLocalItem(SyncContext sync) throws SyncException
	{
		Log.d("sync", "Deleting locally: "
				+ sync.getCacheEntry().getId());
		deleteLocalItem(sync.getCacheEntry().getLocalId());
		getLocalCacheProvider().deleteEntry(sync.getCacheEntry());
	}

	public void deleteServerItem(SyncContext sync) throws MessagingException,
			SyncException
	{
		Log.d("sync", "Deleting from server: " + sync.getMessage().getSubject());
		sync.getMessage().setFlag(Flag.DELETED, true);
		getLocalCacheProvider().deleteEntry(sync.getCacheEntry());
	}

	protected InputStream extractXml(Message message)
			throws MessagingException, IOException
	{
		if(diagLog) Log.d(TAG, "extractXml");
		final Object content = message.getContent();
		if (content instanceof Multipart)
		{
			final Multipart multipart = (Multipart) content;
			if(diagLog) Log.d(TAG, "content is a Multipart containing " + multipart.getCount() + " items");
			for (int idx = 0; idx < multipart.getCount(); idx++)
			{
				final BodyPart p = multipart.getBodyPart(idx);
				if(diagLog) Log.d(TAG, "  " + idx + ": " + p.getContentType());
				if (p.isMimeType(getMimeType())) 
				{ 
					if(diagLog) Log.d(TAG, "  -> found");
					return p.getInputStream(); 
				}
			}
		} 
		else if(diagLog) 
		{
			if(content != null)
			{
				Log.d(TAG, "  message.getContent() cannot be handeled: " + content.getClass().getName());
			}
			else 
			{
				Log.d(TAG, "  message.getContent() returned null");
			}
		}
		if(diagLog) Log.d(TAG, "no XML found");
		return null;
	}

	protected Message wrapXmlInMessage(Session session, SyncContext sync,
			String xml) throws MessagingException, SyncException
	{
		Message result = new MimeMessage(session);
		result.setSubject(sync.getCacheEntry().getRemoteId());
		result.setSentDate(sync.getCacheEntry().getRemoteChangedDate());
		result.setFrom(new InternetAddress("kolab-android@dasz.at"));
		result.setRecipient(RecipientType.TO, new InternetAddress(
				"kolab-android@dasz.at"));
		result.setHeader("User-Agent", "kolab-android 0.1");
		result.setHeader("X-Kolab-Type", getMimeType());
		MimeMultipart mp = new MimeMultipart();
		MimeBodyPart txt = new MimeBodyPart();
		txt.setText(getMessageBodyText(sync), "utf-8");
		mp.addBodyPart(txt);

		BodyPart messageBodyPart = new MimeBodyPart();
		DataSource source;
		try
		{
			source = new ByteArrayDataSource(xml.getBytes("UTF-8"),
					getMimeType());
		}
		catch (UnsupportedEncodingException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		messageBodyPart.setDataHandler(new DataHandler(source));
		messageBodyPart.setFileName("kolab.xml");
		mp.addBodyPart(messageBodyPart);

		// append picture attachment
		if (sync.getNewMessageContent() != null)
		{
			Multipart smp = sync.getNewMessageContent();

			// find picture attachment and add it to new message
			BodyPart photoPart = null;
			for (int i = 0; i < smp.getCount(); i++)
			{
				BodyPart p = smp.getBodyPart(i);
				String disposition = p.getDisposition();

				if ((p.getFileName() != null)
						&& "kolab-picture.png".equals(p.getFileName())
						&& (disposition.equals(Part.ATTACHMENT)))
				{
					photoPart = p;
				}
			}
			if (photoPart != null)
			{
				mp.addBodyPart(photoPart);
			}
		}

		result.setContent(mp);

		// avoid later change in timestamp when the SEEN flag would be updated
		result.setFlag(Flags.Flag.SEEN, true);

		return result;
	}

	public boolean isSameRemoteHash(CacheEntry entry, Message message)
			throws MessagingException, IOException
	{
		Date dt = null;
		if (message != null)
		{
			dt = Utils.getMailDate(message);
		}

		InputStream is = extractXml(message);
		boolean remoteHashIsSame = false;

		if (is != null)
		{
			Document doc = null;
			try
			{
				doc = Utils.getDocument(is);
				String docText = Utils.getXml(doc.getDocumentElement());

				// Log.v("DocDebug isSame:", docText);

				byte[] remoteHash = Utils.sha1Hash(docText);
				byte[] localHash = entry.getRemoteHash();

				Log.d("ASH",
						"Compare Remotehashes: entry: "
								+ Utils.getBytesAsHexString(localHash)
								+ " message: "
								+ Utils.getBytesAsHexString(remoteHash));
				if (Arrays.equals(remoteHash, localHash))
				{
					remoteHashIsSame = true;
				}
				else
				{
					remoteHashIsSame = false;
				}
			}
			catch (Exception ex)
			{
				Log.e("EE", ex.toString());
			}
		}
		else
		{
			Log.d("ASH", "Cannot extract XML from message in isSameRemoteHash");
		}

		boolean result = entry != null && message != null
				&& entry.getRemoteChangedDate().equals(dt)
				&& entry.getRemoteId().equals(message.getSubject())
				&& remoteHashIsSame;

		if (!result)
		{
			if (entry == null) Log.d("syncisSame", "entry == null");
			if (message == null) Log.d("syncisSame", "message == null");
			if (entry != null && message != null)
			{
				if (!entry.getRemoteChangedDate().equals(dt))
				{
					Log.d("syncisSame",
							"getRemoteChangedDate="
									+ entry.getRemoteChangedDate()
									+ ", getReceived/SentDate=" + dt);
				}
				if (!entry.getRemoteId().equals(message.getSubject()))
				{
					Log.d("syncisSame", "getRemoteId=" + entry.getRemoteId()
							+ ", getSubject=" + message.getSubject());
				}
			}
		}

		return result;
	}

	public boolean isSame(CacheEntry entry, Message message)
			throws MessagingException
	{
		Date dt = null;
		if (message != null)
		{
			dt = Utils.getMailDate(message);
		}

		boolean result = entry != null && message != null
				&& entry.getRemoteChangedDate().equals(dt)
				&& entry.getRemoteId().equals(message.getSubject());

		if (!result)
		{
			if (entry == null) Log.d("syncisSame", "entry == null");
			if (message == null) Log.d("syncisSame", "message == null");
			if (entry != null && message != null)
			{
				if (!entry.getRemoteChangedDate().equals(dt))
				{
					Log.d("syncisSame",
							"getRemoteChangedDate="
									+ entry.getRemoteChangedDate()
									+ ", getReceived/SentDate=" + dt);
				}
				if (!entry.getRemoteId().equals(message.getSubject()))
				{
					Log.d("syncisSame", "getRemoteId=" + entry.getRemoteId()
							+ ", getSubject=" + message.getSubject());
				}
			}
		}

		return result;
	}
}
