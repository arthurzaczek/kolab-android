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
import at.dasz.KolabDroid.ContactsContract.SyncContactsHandler;
import at.dasz.KolabDroid.Settings.Settings;

public class KolabContactSyncAdapter extends KolabAbstractSyncAdapter {

	public KolabContactSyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
	}

	@Override
	protected SyncHandler getHandler(Context context, Account account)
	{
		Log.i(TAG, "Creating Contact Sync Handler");
		return new SyncContactsHandler(context, account);
	}
	
	@Override
	protected Time getLastSyncTime(Settings s)
	{
		return s.getLastContactSyncTime();
	}
	
	@Override
	protected void setLastSyncTime(Settings s, Time t)
	{
		s.setLastContactSyncTime(t);		
	}
}
