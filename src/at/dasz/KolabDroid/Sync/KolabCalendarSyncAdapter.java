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
import android.content.Context;
import android.text.format.Time;
import android.util.Log;
import at.dasz.KolabDroid.Calendar.SyncCalendarHandler;
import at.dasz.KolabDroid.Settings.Settings;

public class KolabCalendarSyncAdapter extends KolabAbstractSyncAdapter {
	
	public KolabCalendarSyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
	}

	@Override
	protected SyncHandler getHandler(Context context, Account account)
	{
		Log.i(TAG, "Creating Calendar Sync Handler");
		return new SyncCalendarHandler(context, account);
	}
	
	@Override
	protected Time getLastSyncTime(Settings s)
	{
		return s.getLastCalendarSyncTime();
	}
	
	@Override
	protected void setLastSyncTime(Settings s, Time t)
	{
		s.setLastCalendarSyncTime(t);		
	}
}
