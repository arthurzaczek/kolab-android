package at.dasz.KolabDroid.ContactsContract;

import android.provider.ContactsContract.Data;

/**
 * Constants for a query to get contact data for a given rawContactId
 */
public final class DataQuery
{

	private DataQuery()
	{
	}

	public static final String[]	PROJECTION				= new String[] {
																	Data._ID,
																	Data.MIMETYPE,
																	Data.DATA1,
																	Data.DATA2,
																	Data.DATA3,
																	Data.DATA15};

	public static final int			COLUMN_ID				= 0;
	public static final int			COLUMN_MIMETYPE			= 1;
	public static final int			COLUMN_DATA1			= 2;
	public static final int			COLUMN_DATA2			= 3;
	public static final int			COLUMN_DATA3			= 4;
	public static final int			COLUMN_DATA15			= 5;
	
	public static final int			COLUMN_FULL_NAME		= COLUMN_DATA1;
	public static final int			COLUMN_GIVEN_NAME		= COLUMN_DATA2;
	public static final int			COLUMN_FAMILY_NAME		= COLUMN_DATA3;
	
	public static final int			COLUMN_EVENT_START_DATE = COLUMN_DATA1;
	public static final int			COLUMN_PHOTO			= COLUMN_DATA15;
	public static final int			COLUMN_NOTE				= COLUMN_DATA1;

	public static final int			COLUMN_PHONE_NUMBER		= COLUMN_DATA1;
	public static final int			COLUMN_PHONE_TYPE		= COLUMN_DATA2;

	public static final int			COLUMN_EMAIL_ADDRESS	= COLUMN_DATA1;
	public static final int			COLUMN_EMAIL_TYPE		= COLUMN_DATA2;

	public static final int			COLUMN_POSTAL_TYPE		= COLUMN_DATA2;

	public static final String		SELECTION				= Data.RAW_CONTACT_ID
																	+ "=?";
}