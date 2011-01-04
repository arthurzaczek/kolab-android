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

import android.content.Context;
//import android.database.Cursor;
import at.dasz.KolabDroid.Main;
import at.dasz.KolabDroid.R;
import at.dasz.KolabDroid.StatusHandler;
//import at.dasz.KolabDroid.Calendar.SyncCalendarHandler;
//import at.dasz.KolabDroid.ContactsContract.SyncContactsHandler;
import at.dasz.KolabDroid.Provider.LocalCacheProvider;

public class ResetSoftWorker extends BaseWorker
{
	private int reset_what = 0;
	
	public ResetSoftWorker(Context context, int what)
	{
		super(context);
		reset_what = what;
	}

	@Override
	protected void runWorker()
	{
		setRunningMessage(R.string.resetisrunning);
		try
		{
			StatusHandler.writeStatus(R.string.resetting_soft_start);

			switch (reset_what)
			{
				case Main.MENU_RESET_CONTACTS_SOFT:
					LocalCacheProvider.resetContacts(context);
					break;
					
				case Main.MENU_RESET_CALENDAR_SOFT:
					LocalCacheProvider.resetCalendar(context);
					break;
	
				default:
					break;
			}
			
			//LocalCacheProvider.resetDatabase(context);

			StatusHandler.writeStatus(R.string.resetting_soft_finished);
		}
		catch (Exception ex)
		{
			final String errorFormat = this.context.getResources().getString(
					R.string.reset_error_format);

			StatusHandler
					.writeStatus(String.format(errorFormat, ex.toString()));

			ex.printStackTrace();
		}
	}
	
}
