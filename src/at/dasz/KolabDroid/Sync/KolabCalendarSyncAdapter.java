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
import at.dasz.KolabDroid.Calendar.SyncCalendarHandler;
import at.dasz.KolabDroid.Settings.Settings;

public class KolabCalendarSyncAdapter extends AbstractThreadedSyncAdapter {
	private Context context;
	
	public KolabCalendarSyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		this.context = context;
	}

	private static final String TAG = "CalendarSyncAdapter";	

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
		Log.i("SYNC", "performSync called!");
		
		Settings s = new Settings(this.context);
		Time supposedSyncTime = s.getLastCalendarSyncTime();
		supposedSyncTime.hour += 6;
		supposedSyncTime.normalize(false);
		
		Time currentTime = new Time();
		currentTime.set(System.currentTimeMillis());
		
		if (true || Time.compare(supposedSyncTime, currentTime) < 0) {
			SyncCalendarHandler handler = new SyncCalendarHandler(context, account);
			SyncWorker syncWorker = new SyncWorker(this.context, account, handler);
			syncWorker.runWorker();
			
			s.edit();
			s.setLastCalendarSyncTime(currentTime);
			s.save();
		} else {
			Log.i(TAG, "Sync skipped, next sync: " + supposedSyncTime.format3339(false));
		}
	}

}
