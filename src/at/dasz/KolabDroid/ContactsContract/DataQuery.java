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
																	Data.DATA4,
																	Data.DATA7,
																	Data.DATA8,
																	Data.DATA9,
																	Data.DATA10,
																	Data.DATA15};

	public static final int			COLUMN_ID				= 0;
	public static final int			COLUMN_MIMETYPE			= 1;
	public static final int			COLUMN_DATA1			= 2;
	public static final int			COLUMN_DATA2			= 3;
	public static final int			COLUMN_DATA3			= 4;
	public static final int			COLUMN_DATA4			= 5;
	public static final int			COLUMN_DATA7			= 6;
	public static final int			COLUMN_DATA8			= 7;
	public static final int			COLUMN_DATA9			= 8;
	public static final int			COLUMN_DATA10			= 9;
	public static final int			COLUMN_DATA15			= 10;
	
	public static final int			COLUMN_FULL_NAME		= COLUMN_DATA1;
	public static final int			COLUMN_GIVEN_NAME		= COLUMN_DATA2;
	public static final int			COLUMN_FAMILY_NAME		= COLUMN_DATA3;
	
	public static final int			COLUMN_EVENT_START_DATE = COLUMN_DATA1;
	public static final int			COLUMN_PHOTO			= COLUMN_DATA15;
	public static final int			COLUMN_NOTE				= COLUMN_DATA1;
	public static final int			COLUMN_WEBPAGE			= COLUMN_DATA1;
	public static final int			COLUMN_ORGANIZATION		= COLUMN_DATA1;

	public static final int			COLUMN_PHONE_NUMBER		= COLUMN_DATA1;
	public static final int			COLUMN_PHONE_TYPE		= COLUMN_DATA2;

	public static final int			COLUMN_EMAIL_ADDRESS	= COLUMN_DATA1;
	public static final int			COLUMN_EMAIL_TYPE		= COLUMN_DATA2;

	public static final int			COLUMN_ADDRESS_TYPE		= COLUMN_DATA2;
	public static final int			COLUMN_ADDRESS_STREET	= COLUMN_DATA4;
	public static final int			COLUMN_ADDRESS_CITY		= COLUMN_DATA7;
	public static final int			COLUMN_ADDRESS_REGION	= COLUMN_DATA8;
	public static final int			COLUMN_ADDRESS_POSTALCODE	= COLUMN_DATA9;
	public static final int			COLUMN_ADDRESS_COUNTRY	= COLUMN_DATA10;

	public static final String		SELECTION				= Data.RAW_CONTACT_ID
																	+ "=?";
}