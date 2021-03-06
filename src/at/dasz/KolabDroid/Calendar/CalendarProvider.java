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

package at.dasz.KolabDroid.Calendar;

import java.util.Calendar;
import java.util.TimeZone;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import at.dasz.KolabDroid.R;
import at.dasz.KolabDroid.Utils;
import at.dasz.KolabDroid.Sync.SyncException;

public class CalendarProvider
{
	public static final String TAG = "KolabCalendarProvider";
	
	// don't make them public - it's better to do all database access jobs here
	public static Uri				CALENDAR_EVENTS_URI;
	public static Uri				CALENDAR_ALERT_URI;
	public static Uri				CALENDAR_REMINDER_URI;
	public static Uri				CALENDAR_CALENDARS_URI;

	private static final String		CALLER_IS_SYNCADAPTER	= "caller_is_syncadapter";

	public static final String		_ID						= "_id";

	public static final String[]	projection				= new String[] {
			_ID, "calendar_id", "title", "allDay", "dtstart", "dtend",
			"description", "eventLocation", "visibility", "hasAlarm", "rrule",
			"exdate"										};
	private ContentResolver			cr;

	private long					calendarID				= -1;

	private Context					ctx						= null;
	private Account					account					= null;

	public CalendarProvider(Context ctx, Account account)
	{
		this.cr = ctx.getContentResolver();
		this.ctx = ctx;
		this.account = account;

		if (Build.VERSION.SDK_INT <= 7) // android 2.1
		{
			CALENDAR_EVENTS_URI = Uri.parse("content://calendar/events");
			CALENDAR_ALERT_URI = Uri
					.parse("content://calendar/calendar_alerts");
			CALENDAR_REMINDER_URI = Uri.parse("content://calendar/reminders");

			CALENDAR_CALENDARS_URI = Uri.parse("content://calendar/calendars");
		}
		else // >= android 2.2
		{
			CALENDAR_EVENTS_URI = Uri
					.parse("content://com.android.calendar/events");
			CALENDAR_ALERT_URI = Uri
					.parse("content://com.android.calendar/calendar_alerts");
			CALENDAR_REMINDER_URI = Uri
					.parse("content://com.android.calendar/reminders");
			CALENDAR_CALENDARS_URI = Uri
					.parse("content://com.android.calendar/calendars");
		}

		CALENDAR_EVENTS_URI = addCallerIsSyncAdapterParameter(CALENDAR_EVENTS_URI);
		CALENDAR_ALERT_URI = addCallerIsSyncAdapterParameter(CALENDAR_ALERT_URI);
		CALENDAR_REMINDER_URI = addCallerIsSyncAdapterParameter(CALENDAR_REMINDER_URI);
		CALENDAR_CALENDARS_URI = addCallerIsSyncAdapterParameter(CALENDAR_CALENDARS_URI);
	}

	private static Uri addCallerIsSyncAdapterParameter(Uri uri)
	{
		return uri.buildUpon()
				.appendQueryParameter(CALLER_IS_SYNCADAPTER, "true").build();
	}

	public Cursor fetchAllLocalItems()
	{
		return cr.query(CalendarProvider.CALENDAR_EVENTS_URI,
				CalendarProvider.projection, "calendar_id=?",
				new String[] { String.valueOf(getCalendarID()) }, null);
	}

	public Cursor getAllLocalItemsCursor()
	{
		return cr.query(CalendarProvider.CALENDAR_EVENTS_URI,
				new String[] { CalendarProvider._ID }, "calendar_id=?",
				new String[] { String.valueOf(getCalendarID()) }, null);
	}

	public CalendarEntry loadCalendarEntry(int id, String uid)
			throws SyncException
	{
		if (id == 0) return null;
		Uri uri = ContentUris.withAppendedId(CALENDAR_EVENTS_URI, id);
		Cursor cur = cr.query(uri, projection, null, null, null);
		if (cur == null) throw new SyncException(Integer.toString(id),
				"cr.query returned null");
		try
		{
			if (cur.moveToFirst()) { return loadCalendarEntry(cur, uid); }
			return null;
		}
		finally
		{
			cur.close();
		}
	}

	public CalendarEntry loadCalendarEntry(Cursor cur, String uid)
	{
		CalendarEntry e = new CalendarEntry();
		e.setId(cur.getInt(0));
		e.setUid(uid);
		e.setCalendar_id(cur.getInt(1));
		e.setTitle(cur.getString(2));
		boolean allDay = cur.getInt(3) != 0;
		e.setAllDay(allDay);
		
		long startMillis = cur.getLong(4);
		long endMillis = cur.getLong(5);

		// If the event is all-day, read the times in UTC timezone
		Time start = new Time();
		if(allDay)
		{
			start.timezone = Time.TIMEZONE_UTC;
			start.set(startMillis);
			start.normalize(true);
		} else {
			start.set(startMillis);
		}		
		e.setDtstart(start);

		Time end = new Time();
		
		// Tobias, 20/09/2011:
		// somehow the end date sometimes seems to be set to 0 for recurring events within the
		// Android database. For now, I just set the end date to the start date + 1 hour if we have
		// this case and we're not having an all day event.
		//
		// TODO We should try to figure out why the end date is set to 0.
		if (endMillis == 0 && !allDay) {
			end.set(start);
			end.hour += 1;
			end.normalize(true);
		} else
		{
			if(allDay) {
                end.timezone = Time.TIMEZONE_UTC;
                end.set(endMillis);
                end.normalize(true);
			} else {
			end.set(endMillis);
			}
		}
		e.setDtend(end);

		e.setDescription(cur.getString(6));
		e.setEventLocation(cur.getString(7));
		e.setVisibility(cur.getInt(8));
		e.setHasAlarm(cur.getInt(9));

		if (e.getHasAlarm() == 1)
		{
			// for now, just use the reminder that is the farthest away from the
			// appointment
			// Cursor alertCur = cr.query(CALENDAR_ALERT_URI, null,
			// "event_id=?", new String[]{Integer.toString(e.getId())},
			// "minutes DESC");

			// use reminder which include snoozed events (i.e. moving the alarm
			// towards the future)
			Cursor alertCur = cr.query(CALENDAR_REMINDER_URI, null,
					"event_id=?", new String[] { Integer.toString(e.getId()) },
					"minutes DESC");

			if (alertCur != null && alertCur.moveToFirst())
			{
				int colIdx = alertCur.getColumnIndex("minutes");
				e.setReminderTime(alertCur.getInt(colIdx));
			}
		}

		e.setrRule(cur.getString(10));

		return e;
	}

	public void delete(CalendarEntry e)
	{
		delete(e.getId());
	}

	public void delete(int id)
	{
		if (id == 0) return;
		Uri uri = ContentUris.withAppendedId(CALENDAR_EVENTS_URI, id);
		cr.delete(uri, null, null);
		// don't delete matching alerts; trigger will do this for us
	}

	public void save(CalendarEntry e) throws SyncException
	{
		// some useful information:
		// http://www.google.com/codesearch/p?hl=en&sa=N&cd=3&ct=rc#uX1GffpyOZk/core/java/android/provider/Calendar.java

		final ContentValues values = new ContentValues();
		if (e == null)
		{
			Log.e(TAG,
					"e == null ; cannot save calendar entry");
			return;
		}
		if (e.getDtstart() == null)
		{
			Log.e(TAG,
					"e.getDtstart() == null ; cannot save calendar entry with id="
							+ e.getCalendar_id());
			return;
		}
		if (e.getDtend() == null)
		{
			Log.e(TAG,
					"e.getDtend() == null ; cannot save calendar entry with id="
							+ e.getCalendar_id());
			return;
		}
		final long start = e.getDtstart().toMillis(true);
		final long end = e.getDtend().toMillis(true);
		final boolean allDay = e.getAllDay();

		String duration;
		if (allDay)
		{
			final long days = (end - start + DateUtils.DAY_IN_MILLIS - 1)
					/ DateUtils.DAY_IN_MILLIS;
			duration = "P" + days + "D";
		}
		else
		{
			final long seconds = (end - start) / DateUtils.SECOND_IN_MILLIS;
			duration = "P" + seconds + "S";
		}

		values.put("_sync_account", account.name);
		values.put("_sync_account_type", account.type);
		values.put("_sync_dirty", 0);
		values.put("_sync_time", System.currentTimeMillis());

		values.put("calendar_id", e.getCalendar_id());
		String timezone;
		if (allDay) {
			timezone = Time.TIMEZONE_UTC;
		} else {
			timezone = TimeZone.getDefault().getID();
		}
		values.put("eventTimezone", timezone);

		values.put("title", e.getTitle());
		values.put("allDay", allDay ? 1 : 0);
		values.put("dtstart", start);
		values.put("dtend", end);
		values.put("duration", duration);
		values.put("description", e.getDescription());
		values.put("eventLocation", e.getEventLocation());
		values.put("visibility", e.getVisibility());
		values.put("hasAlarm", e.getHasAlarm());
		values.put("rrule", e.getrRule());
		values.put("exdate", e.getexDate());

		if (e.getId() == 0)
		{
			Uri newUri = cr.insert(CALENDAR_EVENTS_URI, values);
			if (newUri == null) throw new SyncException(e.getTitle(),
					"Unable to create new Calender Entry, provider returned null uri.");
			e.setId((int) ContentUris.parseId(newUri));
		}
		else
		{
			Uri uri = ContentUris
					.withAppendedId(CALENDAR_EVENTS_URI, e.getId());
			cr.update(uri, values, null, null);
		}

		if (e.getHasAlarm() == 1)
		{
			// delete existing alerts to replace them with the ones from the
			// synchronisation
			cr.delete(CALENDAR_ALERT_URI, "event_id=?",
					new String[] { Integer.toString(e.getId()) });

			// remove reminder entry
			cr.delete(CALENDAR_REMINDER_URI, "event_id=?",
					new String[] { Integer.toString(e.getId()) });

			// create reminder
			ContentValues reminderValues = new ContentValues();
			reminderValues.put("event_id", e.getId());
			reminderValues.put("method", 1);
			reminderValues.put("minutes", e.getReminderTime());

			cr.insert(CALENDAR_REMINDER_URI, reminderValues);

			// create alert
			ContentValues alertValues = new ContentValues();

			alertValues.put("event_id", e.getId());
			alertValues.put("begin", start);
			alertValues.put("end", end);
			alertValues.put("alarmTime", (start - e.getReminderTime() * 60000));

			alertValues.put("state", 0);
			// SCHEDULED = 0;
			// FIRED = 1;
			// DISMISSED = 2;

			alertValues.put("minutes", e.getReminderTime());

			// we need those values to prevent the exception mentioned below
			// from occuring
			Calendar cal = Calendar.getInstance();
			long now = cal.getTimeInMillis();
			alertValues.put("creationTime", now);
			alertValues.put("receivedTime", now);
			alertValues.put("notifyTime", now);

			// TODO: sometimes throws an SQLiteConstraintException error code
			// 19; constraint failed
			// this happens although the query seems correct (tested in
			// sqliteman against calendar.db)
			// if it happens it seems half of the data are inserted into
			// calendarAlerts, strange
			cr.insert(CALENDAR_ALERT_URI, alertValues);

		}
	}

	private void dumpAllCalendars()
	{
		Log.d(TAG,
				"name - displayName - _sync_account - _sync_account_type");
		Cursor cur = cr.query(CalendarProvider.CALENDAR_CALENDARS_URI,
				new String[] { "name", "displayName", "_sync_account",
						"_sync_account_type" }, null, null, null);
		if(cur == null) return;
		try
		{
			while (cur.moveToNext())
			{
				Log.d(TAG,
						cur.getString(0) + " - " + cur.getString(1) + " - "
								+ cur.getString(2) + " - " + cur.getString(3));
			}
		}
		finally
		{
			cur.close();
		}
	}

	public void deleteOurCalendar(String accountName, String accountType)
	{
		Log.i(TAG, "Deleting our KolabDroid calendar(s)");
		dumpAllCalendars();
		if (getCalendarID() > 0)
		{
			Uri delUri = ContentUris.withAppendedId(
					CalendarProvider.CALENDAR_CALENDARS_URI, getCalendarID());
			cr.delete(delUri, null, null);
		}
		// make sure we clean up ALL of our calendars
		cr.delete(CalendarProvider.CALENDAR_CALENDARS_URI,
				"_sync_account=? and _sync_account_type=?", new String[] {
						accountName, accountType });
	}

	// TODO: we only support one calendar for now
	public void setOrCreateKolabCalendar()
	{
		dumpAllCalendars();

		String accountName = "";

		if (account == null) // called by reset button
		{
			accountName = ctx.getString(R.string.SYNC_ACCOUNT_NAME);
		}
		else
		{
			accountName = account.name;
		}

		String selection = "_sync_account=? and _sync_account_type=?";

		Cursor cur = cr.query(CalendarProvider.CALENDAR_CALENDARS_URI, null,
				selection, new String[] { accountName, Utils.SYNC_ACCOUNT_TYPE }, null);

		if (cur == null)
		{
			Log.e(TAG, "Cannot query calendars");
			return;
		}

		ContentValues cvs = new ContentValues();
		if (!cur.moveToFirst())
		{
			Log.i(TAG, "Creating new KolabDroid calendar");
			// create one
			cvs.put("_sync_account", accountName);
			cvs.put("_sync_account_type", Utils.SYNC_ACCOUNT_TYPE);
			cvs.put("name", accountName);
			cvs.put("displayName", accountName);
			cvs.put("selected", 1);
			cvs.put("sync_events", 1);
			cvs.put("access_level", 700);

			// TODO: Arthur: Do we need that? I don't think so
			//cvs.put("url", "http://www.test.de"); // TODO what to put here? -> reported as invalid by Android >= 3.0.1
			cvs.put("color", -14069085); // TODO: how are colors represented?
			cvs.put("timezone", "Europe/Berlin"); // TODO: where to get timezone
													// for
													// calendar from?
			cvs.put("ownerAccount", "kolab-android@dasz.at"); // TODO: which
																// owner?
																// use same as
																// for
																// contacts

			Uri newUri = cr.insert(CALENDAR_CALENDARS_URI, cvs);
			if (newUri == null)
			{
				Log.e("CalProvider",
						"Unable to create new Calender, provider returned null uri.");
				return;
			}
			calendarID = ContentUris.parseId(newUri);
		}
		else
		{
			int idx = cur.getColumnIndex("_id");
			calendarID = cur.getLong(idx);
			Log.i(TAG, "Using KolabDroid calendar = " + calendarID);

			// Do not update the calendar
			// This ends in a sync loop
		}

		cur.close();
	}

	public long getCalendarID()
	{
		return calendarID;
	}
}
