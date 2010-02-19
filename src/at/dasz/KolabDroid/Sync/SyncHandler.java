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

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.database.Cursor;
import at.dasz.KolabDroid.Provider.LocalCacheProvider;

public interface SyncHandler
{
	public StatusEntry getStatus();
	
	public abstract Cursor getAllLocalItemsCursor();

	public abstract int getIdColumnIndex(Cursor c);

	public abstract String getDefaultFolderName();

	public abstract LocalCacheProvider getLocalCacheProvider();

	public abstract boolean hasLocalItem(SyncContext sync);

	/**
	 * Returns true if there are local changes with respect to the specified
	 * cache entry.
	 * 
	 * @param entry
	 * @return
	 */
	public abstract boolean hasLocalChanges(SyncContext sync);

	/**
	 * Create a local item and cache entry from the message.
	 * 
	 * @param message
	 * @return
	 * @throws MessagingException
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public abstract void createLocalItemFromServer(SyncContext sync)
			throws MessagingException, ParserConfigurationException, SAXException, IOException;

	/**
	 * Upload the item specified by the cache entry to the targetFolder.
	 * 
	 * @param session
	 * @param targetFolder
	 * @param localId
	 * @return
	 * @throws MessagingException
	 * @throws ParserConfigurationException 
	 */
	public abstract void createServerItemFromLocal(Session session,
			Folder targetFolder, SyncContext sync, int localId) throws MessagingException, ParserConfigurationException;

	/**
	 * Update the local item and cache entry with the data from the server.
	 * 
	 * @param entry
	 * @param message
	 * @return
	 * @throws MessagingException
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public abstract void updateLocalItemFromServer(SyncContext sync) throws MessagingException, ParserConfigurationException, SAXException, IOException;

	/**
	 * Update the message and cache entry with the local data.
	 * 
	 * @param entry
	 * @param message
	 * @return
	 * @throws MessagingException
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public abstract void updateServerItemFromLocal(Session session,
			Folder targetFolder,SyncContext sync) throws MessagingException, IOException, SAXException;

	/**
	 * Delete the local item and cache entry.
	 * 
	 * @param entry
	 */
	public abstract void deleteLocalItem(SyncContext sync);

	/**
	 * Delete the message and cache entry.
	 * 
	 * @param entry
	 * @param message
	 * @throws MessagingException
	 */
	public abstract void deleteServerItem(SyncContext sync)
			throws MessagingException;
}
