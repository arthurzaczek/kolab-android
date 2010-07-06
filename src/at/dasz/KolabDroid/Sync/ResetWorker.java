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
import at.dasz.KolabDroid.R;
import at.dasz.KolabDroid.StatusHandler;
import at.dasz.KolabDroid.Calendar.SyncCalendarHandler;
import at.dasz.KolabDroid.Contacts.SyncContactsHandler;
import at.dasz.KolabDroid.Provider.LocalCacheProvider;

public class ResetWorker extends BaseWorker
{
	public ResetWorker(Context context)
	{
		super(context);
	}

	@Override
	protected void runWorker()
	{
		setRunningMessage(R.string.resetisrunning);
		try
		{
			StatusHandler.writeStatus(R.string.resetting_start);

			final String deleteMessageFormat = this.context.getResources()
					.getString(R.string.delete_message_format);
			int currentItemNo = 0;

			LocalCacheProvider.resetDatabase(context);

			currentItemNo = resetContacts(deleteMessageFormat, currentItemNo);
			currentItemNo = resetCalendar(deleteMessageFormat, currentItemNo);

			StatusHandler.writeStatus(R.string.reseting_finished);
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

	private int resetCalendar(final String deleteMessageFormat,
			int currentItemNo)
	{
		SyncCalendarHandler calendar = new SyncCalendarHandler(context);
		for (int id : calendar.getAllLocalItemsIDs())
		{
			calendar.deleteLocalItem(id);
			if (++currentItemNo % 10 == 0)
			{
				StatusHandler.writeStatus(String.format(deleteMessageFormat,
						currentItemNo));
			}
		}
		return currentItemNo;
	}

	private int resetContacts(final String deleteMessageFormat,
			int currentItemNo)
	{
		SyncContactsHandler contacts = new SyncContactsHandler(context);
		for (int id : contacts.getAllLocalItemsIDs())
		{
			contacts.deleteLocalItem(id);
			if (++currentItemNo % 10 == 0)
			{
				StatusHandler.writeStatus(String.format(deleteMessageFormat,
						currentItemNo));
			}
		}

		return currentItemNo;
	}
}
