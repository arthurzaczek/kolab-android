package at.dasz.KolabDroid.Calendar;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

/*
 * This provider does nothing but cheating android
 * if(Build.VERSION.SDK_INT <= 7) // android 2.1
 *			CALENDAR_URI	= Uri.parse("content://calendar/events");
 * if(Build.VERSION.SDK_INT == 8) //android 2.2
 *			CALENDAR_URI	= Uri.parse("content://com.android.calendar/events");
 *
 * With this fake provider we can add our own calender sync authority
 */
public class FakeCalendarContentProvider extends ContentProvider
{

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onCreate()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs)
	{
		// TODO Auto-generated method stub
		return 0;
	}

}
