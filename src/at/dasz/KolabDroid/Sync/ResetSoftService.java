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
import android.content.Intent;
import android.util.Log;
import at.dasz.KolabDroid.Main;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class ResetSoftService extends WakefulIntentService
{
	public ResetSoftService()
	{
		super("ResetSoftService");
	}

	public static void startReset(Context context, int what)
	{
		if(!BaseWorker.isRunning())
		{
			Log.i("Service", "starting service");
			WakefulIntentService.acquireStaticLock(context);
			context.startService(new Intent(context, ResetSoftService.class).putExtra("what", what));	
					
		}
		else
		{
			Log.i("Service", "another service is already running");			
		}
	}

	@Override
	protected void doWakefulWork(Intent intent)
	{
		int what = 0;
		
		if(intent.getExtras() != null)
			what = intent.getExtras().getInt("what");
		
		Log.i("Service", "starting soft reset");
		try
		{
			ResetSoftWorker r = new ResetSoftWorker(this, what);
			r.start();
			Log.i("Service", "soft reset finished");
		}
		catch (Exception ex)
		{
			Log.i("Service", "soft reset failed: " + ex.toString());
		}
	}
}
