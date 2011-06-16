package at.dasz.KolabDroid;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SyncAdapterType;
import android.database.Cursor;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.widget.Toast;
import at.dasz.KolabDroid.Calendar.CalendarProvider;

public class DiagDump
{
	public static void writeDump(Context ctx)
	{
		String state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(state))
		{
			Toast.makeText(ctx, "External Media not mounted", Toast.LENGTH_LONG)
					.show();
			return;
		}

		File f = new File(Environment.getExternalStorageDirectory(),
				"KolabDiagDump.txt");
		FileWriter w = null;
		try
		{
			w = new FileWriter(f, false);

			w.write("Kolab Droid Diagnostic Dump\n");
			w.write("===========================\n");
			w.write("Version: " + Utils.getVersionNumber(ctx) + "\n\n");

			dumpSyncAdapterTypes(ctx, w);
			w.write("\n\n");

			dumpAccounts(ctx, w);
			w.write("\n\n");

			dumpContacts(ctx, w);
			w.write("\n\n");

			dumpCalendars(ctx, w);
			w.write("\n\n");

			dumpCalendarEntries(ctx, w);
			w.write("\n\n");

			w.flush();

			Toast.makeText(ctx, "Dump written to " + f.getAbsolutePath(),
					Toast.LENGTH_LONG).show();
		}
		catch (Exception ex)
		{
			Toast.makeText(ctx, ex.getMessage(), Toast.LENGTH_LONG).show();
		}
		finally
		{
			if (w != null)
			{
				try
				{
					w.close();
				}
				catch (IOException ex)
				{
				}
			}
		}
	}

	private static void dumpCalendars(Context ctx, FileWriter w)
			throws IOException
	{
		w.write("Calendars: \n");

		final CalendarProvider calProvider = new CalendarProvider(ctx, null);
		calProvider.setOrCreateKolabCalendar();
		final long calID = calProvider.getCalendarID();
		w.write(String.format("Our Calendar ID = %d\n", calID));

		final ContentResolver cr = ctx.getContentResolver();

		Cursor cur = cr.query(CalendarProvider.CALENDAR_CALENDARS_URI,
				new String[] { "_id", "name", "displayName", "_sync_account",
						"_sync_account_type", "selected", "sync_events" }, null, null, null);
		if (cur == null) return;
		try
		{
			while (cur.moveToNext())
			{
				w.write(String
						.format("ID = %d, name = %s; displayName = %s; _sync_account = %s; _sync_account_type = %s, selected = %d, sync_events = %d\n",
								cur.getInt(0), cur.getString(1),
								cur.getString(2), cur.getString(3),
								cur.getString(4),
								cur.getInt(5), cur.getInt(6)));
			}
		}
		finally
		{
			cur.close();
		}
	}

	private static void dumpCalendarEntries(Context ctx, FileWriter w)
			throws IOException
	{
		w.write("First 10 calendar entries: \n");

		final CalendarProvider calProvider = new CalendarProvider(ctx, null);
		calProvider.setOrCreateKolabCalendar();

		final long calID = calProvider.getCalendarID();
		w.write(String.format("Our Calendar ID = %d\n", calID));

		final ContentResolver cr = ctx.getContentResolver();
		final String[] projection = new String[] { "_id", "_sync_account",
				"_sync_account_type" };

		final Cursor cur = cr.query(CalendarProvider.CALENDAR_EVENTS_URI,
				projection, "calendar_id=" + calID, null, null);
		if (cur == null) return;
		try
		{
			int counter = 0;
			while (cur.moveToNext())
			{
				w.write(String.format(
						"ID = %d, _sync_account = %s; _sync_account_type = %s\n",
						cur.getInt(0),
						cur.getString(1), cur.getString(2)));
				if (counter++ >= 10) break;
			}
		}
		finally
		{
			cur.close();
		}
	}

	private static void dumpContacts(Context ctx, FileWriter w)
			throws IOException
	{
		final ContentResolver cr = ctx.getContentResolver();
		w.write("First 10 contacts: \n");

		// only return those from our account
		final String where = ContactsContract.RawContacts.ACCOUNT_NAME
				+ "=? and " + ContactsContract.RawContacts.ACCOUNT_TYPE + "=?";

		final String[] projection = new String[] { RawContacts._ID,
				RawContacts.ACCOUNT_NAME, RawContacts.ACCOUNT_TYPE,
				RawContacts.DELETED, RawContacts.CONTACT_ID, };

		final String[] contactProjection = new String[] { Contacts._ID,
				Contacts.HAS_PHONE_NUMBER, Contacts.IN_VISIBLE_GROUP,
				Contacts.STARRED, };

		// TODO: maybe we should use the account.name and account.type instead
		// of string
		final Cursor cursor = cr.query(
				ContactsContract.RawContacts.CONTENT_URI, projection, where,
				new String[] { ctx.getString(R.string.SYNC_ACCOUNT_NAME),
						Utils.SYNC_ACCOUNT_TYPE }, null);

		int counter = 0;
		while (cursor.moveToNext())
		{
			try
			{
				w.write(String
						.format("ID = %d; ACCOUNT_NAME = %s; ACCOUNT_TYPE = %s; deleted = %s\n",
								cursor.getInt(0), cursor.getString(1),
								cursor.getString(2), cursor.getString(3)));
				final int id = cursor.getInt(4);
				final Cursor contactCursor = cr.query(
						ContentUris.withAppendedId(Contacts.CONTENT_URI, id),
						contactProjection, null, null, null);
				if (contactCursor.moveToFirst())
				{
					w.write(String
							.format("ID = %d; HAS_PHONE_NUMBER = %s; IN_VISIBLE_GROUP = %s; STARRED = %s\n\n",
									contactCursor.getInt(0),
									contactCursor.getString(1),
									contactCursor.getString(2),
									contactCursor.getString(3)));
				}
				else
				{
					w.write("Unable to find coresponding contact row\n\n");
				}
			}
			catch (Exception ex)
			{
				w.write("Error reading contact: " + ex.getMessage() + "\n\n");
			}

			if (counter++ >= 10) break;
		}
	}

	private static void dumpSyncAdapterTypes(Context ctx, FileWriter w)
			throws IOException
	{
		w.write("SyncAdapterTypes: \n");

		final SyncAdapterType[] syncs = ContentResolver.getSyncAdapterTypes();
		for (SyncAdapterType sync : syncs)
		{
			w.write(String
					.format("accountType = %s; authority = %s; supportsUploading = %b; isUserVisible = %b\n",
							sync.accountType, sync.authority, sync.supportsUploading(),
							sync.isUserVisible()));
		}
	}

	private static void dumpAccounts(Context ctx, FileWriter w)
			throws IOException
	{
		w.write("Accounts: \n");
		final Account[] accounts = android.accounts.AccountManager.get(ctx)
				.getAccounts();

		for (Account a : accounts)
		{
			w.write(String.format("Name = %s; Type = %s\n", a.name, a.type));
		}
	}
}
