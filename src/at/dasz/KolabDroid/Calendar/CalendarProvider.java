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

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.CalendarAlerts;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import at.dasz.KolabDroid.R;
import at.dasz.KolabDroid.Utils;
import at.dasz.KolabDroid.Sync.SyncException;

public class CalendarProvider
{
	public static final String		TAG					= "KolabCalendarProvider";

	// don't make them public - it's better to do all database access jobs here
	public static Uri				CALENDAR_EVENTS_URI;
	public static Uri				CALENDAR_ALERT_URI;
	public static Uri				CALENDAR_REMINDER_URI;
	public static Uri				CALENDAR_CALENDARS_URI;

	public static final String[]	eventsProjection	= new String[] {
			Events._ID, Events.CALENDAR_ID, Events.TITLE, Events.ALL_DAY,
			Events.DTSTART, Events.DTEND, Events.DESCRIPTION,
			Events.EVENT_LOCATION, Events.VISIBLE, Events.HAS_ALARM,
			Events.RRULE, Events.EXDATE				};

	private ContentResolver			cr;

	private long					calendarID			= -1;

	private Context					ctx					= null;
	private Account					account				= null;

	public CalendarProvider(Context ctx, Account account)
	{
		this.cr = ctx.getContentResolver();
		this.ctx = ctx;
		this.account = account;

		CALENDAR_EVENTS_URI = addCallerIsSyncAdapterParameter(Events.CONTENT_URI);
		CALENDAR_ALERT_URI = addCallerIsSyncAdapterParameter(CalendarAlerts.CONTENT_URI);
		CALENDAR_REMINDER_URI = addCallerIsSyncAdapterParameter(Reminders.CONTENT_URI);
		CALENDAR_CALENDARS_URI = addCallerIsSyncAdapterParameter(Calendars.CONTENT_URI);
	}

	private Uri addCallerIsSyncAdapterParameter(Uri uri)
	{
		return uri
				.buildUpon()
				.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER,
						"true")
				.appendQueryParameter(Calendars.ACCOUNT_NAME,
						ctx.getString(R.string.SYNC_ACCOUNT_NAME))
				.appendQueryParameter(Calendars.ACCOUNT_TYPE,
						Utils.SYNC_ACCOUNT_TYPE).build();
	}

	public Cursor fetchAllLocalItems()
	{
		return cr.query(CALENDAR_EVENTS_URI, CalendarProvider.eventsProjection,
				Events.CALENDAR_ID + "=?",
				new String[] { String.valueOf(getCalendarID()) }, null);
	}

	public Cursor getAllLocalItemsCursor()
	{
		return cr.query(CALENDAR_EVENTS_URI, new String[] { Events._ID },
				Events.CALENDAR_ID + "=?",
				new String[] { String.valueOf(getCalendarID()) }, null);
	}

	public CalendarEntry loadCalendarEntry(int id, String uid)
			throws SyncException
	{
		if (id == 0) return null;
		Uri uri = ContentUris.withAppendedId(CALENDAR_EVENTS_URI, id);
		Cursor cur = cr.query(uri, eventsProjection, null, null, null);
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
		e.setAllDay(cur.getInt(3) != 0);

		Time start = new Time();
		start.set(cur.getLong(4));
		e.setDtstart(start);

		Time end = new Time();

		// Tobias, 20/09/2011:
		// somehow the end date sometimes seems to be set to 0 for recurring
		// events within the
		// Android database. For now, I just set the end date to the start date
		// + 1 hour if we have
		// this case and we're not having an all day event.
		//
		// TODO We should try to figure out why the end date is set to 0.
		if (cur.getLong(5) == 0 && !e.getAllDay())
		{
			end.set(start);
			end.hour += 1;
			end.normalize(true);
		}
		else end.set(cur.getLong(5));
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
					Reminders.EVENT_ID + "=?",
					new String[] { Integer.toString(e.getId()) },
					Reminders.MINUTES + " DESC");

			if (alertCur != null && alertCur.moveToFirst())
			{
				int colIdx = alertCur.getColumnIndex(Reminders.MINUTES);
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

		ContentValues values = new ContentValues();
		if (e == null)
		{
			Log.e(TAG, "e == null ; cannot save calendar entry");
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
		long start = e.getDtstart().toMillis(true);
		long end = e.getDtend().toMillis(true);

		String duration;
		if (e.getAllDay())
		{
			long days = (end - start + DateUtils.DAY_IN_MILLIS - 1)
					/ DateUtils.DAY_IN_MILLIS;
			duration = "P" + days + "D";
		}
		else
		{
			long seconds = (end - start) / DateUtils.SECOND_IN_MILLIS;
			duration = "P" + seconds + "S";
		}

		// Not needed for ICS
		// values.put(Events.ACCOUNT_NAME, account.name);
		// values.put(Events.ACCOUNT_TYPE, account.type);
		values.put(Events.DIRTY, 0);

		// TODO: doesn't exist anymore
		// values.put("_sync_time", System.currentTimeMillis());

		// values.put("eventTimezone", "UTC"); //TODO: put eventTimezone here:
		// UTC from kolab? Arthur: yes, see comment in writeXml

		values.put(Events.CALENDAR_ID, e.getCalendar_id());
		values.put(Events.TITLE, e.getTitle());
		values.put(Events.ALL_DAY, e.getAllDay() ? 1 : 0);
		values.put(Events.DTSTART, start);
		values.put(Events.DTEND, end);
		values.put(Events.DURATION, duration);
		values.put(Events.DESCRIPTION, e.getDescription());
		values.put(Events.EVENT_LOCATION, e.getEventLocation());
		// TODO: not allowed by CalendarContract
		// values.put(Events.VISIBLE, e.getVisibility());
		values.put(Events.HAS_ALARM, e.getHasAlarm());
		values.put(Events.RRULE, e.getrRule());
		values.put(Events.EXDATE, e.getexDate());

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
			cr.delete(CALENDAR_ALERT_URI, CalendarAlerts.EVENT_ID + "=?",
					new String[] { Integer.toString(e.getId()) });

			// remove reminder entry
			cr.delete(CALENDAR_REMINDER_URI, Reminders.EVENT_ID + "=?",
					new String[] { Integer.toString(e.getId()) });

			// create reminder
			ContentValues reminderValues = new ContentValues();
			reminderValues.put(Reminders.EVENT_ID, e.getId());
			reminderValues.put(Reminders.METHOD, Reminders.METHOD_ALERT);
			reminderValues.put(Reminders.MINUTES, e.getReminderTime());

			cr.insert(CALENDAR_REMINDER_URI, reminderValues);

			// create alert
			ContentValues alertValues = new ContentValues();

			alertValues.put(CalendarAlerts.EVENT_ID, e.getId());
			alertValues.put(CalendarAlerts.BEGIN, start);
			alertValues.put(CalendarAlerts.END, end);
			alertValues.put(CalendarAlerts.ALARM_TIME,
					(start - e.getReminderTime() * 60000));

			alertValues.put(CalendarAlerts.STATE,
					CalendarAlerts.STATE_SCHEDULED);

			alertValues.put(CalendarAlerts.MINUTES, e.getReminderTime());

			// we need those values to prevent the exception mentioned below
			// from occurring
			Calendar cal = Calendar.getInstance();
			long now = cal.getTimeInMillis();
			// TODO: this should be UTC. See
			// http://stackoverflow.com/a/230383/4918 for transformation code
			// example
			alertValues.put(CalendarAlerts.CREATION_TIME, now);
			alertValues.put(CalendarAlerts.RECEIVED_TIME, now);
			alertValues.put(CalendarAlerts.NOTIFY_TIME, now);

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
		Log.d(TAG, "name - displayName - _sync_account - _sync_account_type");
		Cursor cur = cr.query(CalendarProvider.CALENDAR_CALENDARS_URI,
				new String[] { Calendars.NAME, Calendars.CALENDAR_DISPLAY_NAME,
						Calendars.ACCOUNT_NAME, Calendars.ACCOUNT_TYPE }, null,
				null, null);
		if (cur == null) return;
		try
		{
			while (cur.moveToNext())
			{
				Log.d(TAG, cur.getString(0) + " - " + cur.getString(1) + " - "
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
				Calendars.ACCOUNT_NAME + "=? and " + Calendars.ACCOUNT_TYPE
						+ "=?", new String[] { accountName, accountType });
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

		String selection = Calendars.ACCOUNT_NAME + "=? and "
				+ Calendars.ACCOUNT_TYPE + "=?";

		Cursor cur = cr.query(CalendarProvider.CALENDAR_CALENDARS_URI, null,
				selection,
				new String[] { accountName, Utils.SYNC_ACCOUNT_TYPE }, null);

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
			cvs.put(Calendars.ACCOUNT_NAME, accountName);
			cvs.put(Calendars.ACCOUNT_TYPE, Utils.SYNC_ACCOUNT_TYPE);
			cvs.put(Calendars.NAME, accountName);

			// TODO: should be user@imap host?
			cvs.put(Calendars.CALENDAR_DISPLAY_NAME, accountName);
			// TODO: missing from ICS
			// cvs.put("selected", 1);
			cvs.put(Calendars.SYNC_EVENTS, 1);
			cvs.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER);

			// TODO: Arthur: Do we need that? I don't think so
			// cvs.put("url", "http://www.test.de"); // TODO what to put here?
			// -> reported as invalid by Android >= 3.0.1

			// TODO: how are colors represented?
			cvs.put(Calendars.CALENDAR_COLOR, -14069085);

			// TODO: where to get timezone for calendar from?
			cvs.put(Calendars.CALENDAR_TIME_ZONE, "Europe/Berlin");

			// TODO: which owner? use same as for contacts
			cvs.put(Calendars.OWNER_ACCOUNT, "kolab-android@dasz.at");

			Uri newUri = cr.insert(CALENDAR_CALENDARS_URI, cvs);
			if (newUri == null)
			{
				Log.e(TAG,
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
