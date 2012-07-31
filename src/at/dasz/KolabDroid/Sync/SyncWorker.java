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
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.util.HashSet;
import java.util.Set;

import javax.activation.CommandInfo;
import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.MailcapCommandMap;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Flags.Flag;
import javax.xml.parsers.ParserConfigurationException;

import android.accounts.Account;
import org.acra.ErrorReporter;

import android.content.Context;
import android.util.Log;
import at.dasz.KolabDroid.R;
import at.dasz.KolabDroid.StatusHandler;
import at.dasz.KolabDroid.Imap.DchFactory;
import at.dasz.KolabDroid.Imap.ImapClient;
import at.dasz.KolabDroid.Imap.TrustManagerFactory;
import at.dasz.KolabDroid.Provider.LocalCacheProvider;
import at.dasz.KolabDroid.Provider.StatusProvider;
import at.dasz.KolabDroid.Settings.Settings;

/**
 * The background worker that implements the main synchronization algorithm.
 */
public class SyncWorker
{
	private static final String TAG = "sync";
	
	// Not final to avoid warnings
	private static boolean	DBG_LOCAL_CHANGED	= false;
	private static boolean	DBG_REMOTE_CHANGED	= false;

	protected Context		context;
	protected Account		account;
	protected SyncHandler	handler;
	
	protected boolean diagLog = false;

	public SyncWorker(Context context, Account account, SyncHandler handler)
	{
		this.context = context;
		this.account = account;
		this.handler = handler;
	}

	private static StatusEntry	status;

	public static StatusEntry getStatus()
	{
		return status;
	}

	public void runWorker()
	{
		initJavaMail();

		StatusProvider statProvider = new StatusProvider(context);
		status = handler.getStatus();
		try
		{
			if(!handler.shouldProcess()) 
			{
				status.setFatalErrorMsg("Sync Handler reported invalid setup");
				return;
			}
			StatusHandler.writeStatus(R.string.startsync);

			Settings settings = new Settings(this.context);
			sync(settings, handler);
			StatusHandler.writeStatus(R.string.syncfinished);
		}
		//Fixes issue #36
		catch (MessagingException mex)
		{
			Exception e = mex.getNextException();
			if(e != null && e instanceof ConnectException)
			{
				StatusHandler.writeStatus("Connection to server rejected");
				status.setFatalErrorMsg("Connection to server rejected");
			}
			//Fixes issue #36 and prints a "nice" error message in status
			else if(e != null && e instanceof UnknownHostException)
			{
				StatusHandler.writeStatus("Could not resolve hostname of server");
				status.setFatalErrorMsg("Could not resolve hostname of server");
			}
			else
			{
				StatusHandler.writeStatus("Error: " + mex.getMessage());
				status.setFatalErrorMsg(mex.toString());
			}
		}
		// Database error - either wrong database or database not ready
		catch(android.database.sqlite.SQLiteException sqlex) {
			StatusHandler.writeStatus("Error: " + sqlex.getMessage());
			status.setFatalErrorMsg(sqlex.toString());
		}
		catch (Exception ex)
		{
			final String errorFormat = this.context.getResources().getString(
					R.string.sync_error_format);

			// Report
			ErrorReporter.getInstance().handleException(ex);
			
			status.setFatalErrorMsg(ex.toString());
			StatusHandler
					.writeStatus(String.format(errorFormat, ex.getMessage()));
			Log.e(TAG, ex.toString());
		}
		finally
		{
			try
			{
				statProvider.saveStatusEntry(status);
				statProvider.close();
				StatusHandler.notifySyncFinished();
			}
			catch (Exception ex)
			{
				// don't fail here
				Log.e(TAG, ex.toString());
			}
		}
	}

	private void initJavaMail()
	{
//		// http://blog.hpxn.net/2009/12/02/tomcat-java-6-and-javamail-cant-load-dch/
//		// from: http://stackoverflow.com/questions/1969667/send-a-mail-from-java5-and-java6/1969983#1969983
//		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		if (!DataHandler.hasDataContentHandlerFactory())
		{
			DataHandler.setDataContentHandlerFactory(new DchFactory());
		}
	}

	private void sync(Settings settings, SyncHandler handler)
			throws MessagingException, IOException,
			ParserConfigurationException, SyncException, CertificateException
	{
		diagLog = settings.getDiagLog();
		Store server = null;
		Folder sourceFolder = null;
		try
		{
			StatusHandler.writeStatus(R.string.fetching_local_items);
			handler.fetchAllLocalItems();

			StatusHandler.writeStatus(R.string.connect_server);

			if (settings.getUseSSL())
			{
				Log.v(TAG, "loading local keystore");
				TrustManagerFactory.loadLocalKeystore(context);
			}

			Session session = ImapClient.getDefaultImapSession(
					settings.getPort(), settings.getUseSSL());
			server = ImapClient.openServer(session, settings.getHost(),
					settings.getUsername(), settings.getPassword());

			StatusHandler.writeStatus(R.string.fetching_messages);

			// Numbers in comments and messages reference Gargan's Algorithm and
			// the wiki

			// 1. retrieve list of all imap message headers
			sourceFolder = server.getFolder(handler.getDefaultFolderName());
			sourceFolder.open(Folder.READ_WRITE);
			Message[] msgs = sourceFolder.getMessages();
			FetchProfile fp = new FetchProfile();
			fp.add(FetchProfile.Item.CONTENT_INFO);
			fp.add(FetchProfile.Item.FLAGS);
			fp.add(FetchProfile.Item.ENVELOPE);
			sourceFolder.fetch(msgs, fp);

			final LocalCacheProvider cache = handler.getLocalCacheProvider();
			final Set<Integer> processedEntries = new HashSet<Integer>(
					(int) (msgs.length * 1.2));
			final String processMessageFormat = this.context.getResources()
					.getString(R.string.processing_message_format);
			final StatusEntry status = handler.getStatus();
			final boolean useRemoteHash = settings.getCreateRemoteHash();
			Log.d(TAG, "Using remote hash = " + useRemoteHash);

			Log.i("sync", "1. Syncing IMAP Messages");
			for (Message m : msgs)
			{
				if (m.getFlags().contains(Flag.DELETED))
				{
					if(diagLog) Log.d(TAG, "Found deleted message, continue");
					continue;
				}

				SyncContext sync = new SyncContext();
				try
				{
					sync.setMessage(m);

					StatusHandler.writeStatus(String.format(processMessageFormat, status.incrementItems(), msgs.length));

					// 2. check message headers for changes
					String subject = sync.getMessage().getSubject();
					if(diagLog) Log.i(TAG, "2. Checking message " + subject);
					
					if(subject == null || "".equals(subject))
					{
						Log.w(TAG, "2. Message does NOT have a subject => will ignore it");
						continue;
					}

					// 5. fetch local cache entry
					sync.setCacheEntry(cache.getEntryFromRemoteId(subject));
					final CacheEntry cacheEntry = sync.getCacheEntry(); 

					if (cacheEntry == null)
					{
						Log.i(TAG, "6. found no local entry => save");
						handler.createLocalItemFromServer(session, sourceFolder, sync);
						status.incrementLocalNew();
						if (sync.getCacheEntry() == null)
						{
							Log.w(TAG, "createLocalItemFromServer returned a null object! See Logfile for parsing errors");
						}

					}
					else
					{
						if (processedEntries.contains(cacheEntry.getLocalId()))
						{
							Log.w(TAG,
							 	"7. already processed from server: skipping");
							continue;
						}
						if(diagLog) Log.d("sync", "7. compare data to figure out what happened");

						boolean cacheIsSame = false;
						if (useRemoteHash)
						{
							cacheIsSame = handler.isSameRemoteHash(cacheEntry, sync.getMessage());
						}
						else
						{
							cacheIsSame = handler.isSame(cacheEntry, sync.getMessage());
						}

						if (cacheIsSame && !DBG_REMOTE_CHANGED)
						{
							if(diagLog) Log.d(TAG, "7.a/d cur=localdb (cache is same)");
							if (handler.hasLocalItem(sync))
							{
								if(diagLog) Log.d(TAG, "7.a check for local changes and upload them");
								if (handler.hasLocalChanges(sync) || DBG_LOCAL_CHANGED)
								{
									Log.i(TAG, "7.a local changes found: updating ServerItem from Local");
									handler.updateServerItemFromLocal(session, sourceFolder, sync);
									status.incrementRemoteChanged();
								}
								else
								{
									if(diagLog) Log.d(TAG, "7.a NO local changes found => doing nothing");
									handler.markAsSynced(sync);
								}
							}
							else
							{
								Log.i(TAG, "7.d entry missing => delete on server");
								handler.deleteServerItem(sync);
								status.incrementRemoteDeleted();
							}
						}
						else
						{
							if(diagLog) Log.d(TAG, "7.b/c check for local changes and \"resolve\" the conflict");
							if (handler.hasLocalChanges(sync))
							{
								Log.i(TAG, "7.c local changes found: conflicting, updating local item from server");
								status.incrementConflicted();
							}
							else
							{
								Log.i(TAG, "7.b no local changes found: updating local item from server");
							}
							handler.updateLocalItemFromServer(sync);
							status.incrementLocalChanged();
						}
					}
				}
				catch (SyncException ex)
				{
					Log.e(TAG, ex.toString());
					status.incrementErrors();
				}
				catch(MessagingException mex) 
				{
					Log.e(TAG, mex.toString());
					status.incrementErrors();					
				}
				catch(IOException ioex)
				{
					Log.e(TAG, ioex.toString());
					status.incrementErrors();					
				}
				if (sync.getCacheEntry() != null)
				{
					if(diagLog) Log.d(TAG, "8. remember message as processed (item id=" + sync.getCacheEntry().getLocalId() + ")");
					processedEntries.add(sync.getCacheEntry().getLocalId());
				}
			}

			// 9. for all unprocessed local items
			// 9.a upload/delete
			Log.i(TAG, "9. process unprocessed local items");

			Set<Integer> localIDs = handler.getAllLocalItemsIDs();
			if (localIDs == null) throw new SyncException("getAllLocalItems", "cr.query returned null");
			int currentLocalItemNo = 1;
			int itemsCount = localIDs.size();
			try
			{
				final String processItemFormat = this.context.getResources()
						.getString(R.string.processing_item_format);

				for (int localId : localIDs)
				{
					if(diagLog) Log.i(TAG, "9. processing local#" + localId);

					StatusHandler.writeStatus(String.format(processItemFormat,
							currentLocalItemNo++, itemsCount));

					if (processedEntries.contains(localId))
					{
						if(diagLog) Log.d("sync", "9.a already processed from server: skipping");
						continue;
					}

					SyncContext sync = new SyncContext();
					sync.setCacheEntry(cache.getEntryFromLocalId(localId));
					if (sync.getCacheEntry() != null)
					{
						Log.i(TAG, "9.b found in local cache: deleting localy");
						handler.deleteLocalItem(sync);
						status.incrementLocalDeleted();
						status.incrementItems();
						processedEntries.add(localId);
					}
					else
					{
						Log.i(TAG, "9.c NOT found in local cache: creating on server");
						handler.createServerItemFromLocal(session, sourceFolder, sync, localId);
						status.incrementRemoteNew();
						status.incrementItems();
						processedEntries.add(localId);
					}
				}
			}
			catch (SyncException ex)
			{
				Log.e(TAG, ex.toString());
				status.incrementErrors();
			}
			catch(MessagingException mex) 
			{
				Log.e("sync", mex.toString());
				status.incrementErrors();					
			}
		}
		finally
		{
			handler.finalizeSync();
			Log.i(TAG, "** sync finished");
			if (sourceFolder != null) {
				try {
					sourceFolder.close(true);
				}
				catch(IllegalStateException ex) {
					// don't care if folder is already closed
				}
			}
			if (server != null) server.close();
		}
	}
}
