/*
 *  Copyright 2010 Tobias Langner <tolangner@gmail.com>; All rights reserved.
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

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import at.dasz.KolabDroid.ContactsContract.SyncContactsHandler;
import at.dasz.KolabDroid.Settings.Settings;

public class KolabContactSyncAdapter extends AbstractThreadedSyncAdapter {
	private Context context;
	
	public KolabContactSyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		this.context = context;
	}

	private static final String TAG = "ContactsSyncAdapterService";
	
	

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
		Log.i(TAG, ">>>>>>>>>>>>>>>>>>>>>>> performSync called!");
		
		Settings s = new Settings(this.context);
		Time supposedSyncTime = s.getLastContactSyncTime();
		supposedSyncTime.minute += 15; // Avoid sync loops
		supposedSyncTime.normalize(false);
		
		Time currentTime = new Time();
		currentTime.set(System.currentTimeMillis());
		
		if (Time.compare(supposedSyncTime, currentTime) < 0) {
			SyncContactsHandler handler = new SyncContactsHandler(context, account);
			SyncWorker syncWorker = new SyncWorker(this.context, account, handler);
			syncWorker.runWorker();
			
			StatusEntry status = SyncWorker.getStatus();
			
			syncResult.stats.numEntries = status.getItems();

			syncResult.stats.numDeletes = status.getLocalDeleted() + status.getRemoteDeleted();
			syncResult.stats.numInserts = status.getLocalNew() + status.getRemoteNew();
			syncResult.stats.numUpdates = status.getLocalChanged() + status.getRemoteChanged();
			
			s.edit();
			s.setLastContactSyncTime(currentTime);
			s.save();
		} else {
			Log.i(TAG, "Sync skipped, next sync: " + supposedSyncTime.format3339(false));
		}
		
		Log.i(TAG, "syncResult.hasError() = " + syncResult.hasError());

		Log.i(TAG, "<<<<<<<<<<<<<<<<<<<<<<< performSync finished!");
	}
}
