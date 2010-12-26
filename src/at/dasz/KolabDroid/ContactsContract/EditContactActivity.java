package at.dasz.KolabDroid.ContactsContract;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import at.dasz.KolabDroid.R;
import at.dasz.KolabDroid.Sync.SyncException;

public class EditContactActivity extends Activity
{	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
				
		setContentView(R.layout.edit_contact);
		
		Intent intent = getIntent();
		
		Uri uri = Uri.parse(intent.getDataString());		
		Log.i("ECA:", "Edit uri: "+ uri);
		
		if(uri.toString().endsWith("contacts"))
		{
			//TODO: proide empty form instead of fetching contact from db
			
			TextView txtV = (TextView) findViewById(R.id.TextInfo);
			txtV.setText("TODO: show empty form for new contact");
			return;
		}
		
		Contact c = null;
		
		try
		{
			c = getContactByRawURI(uri);			
			//Log.i("ECA:", "Got contact");
		}
		catch (SyncException ex)
		{
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
		
		if(c == null) return; //TODO: what if db is broken somehow?
		
		EditText firstName = (EditText) findViewById(R.id.EditFirstName);
		firstName.setText(c.getGivenName());
		
		EditText lastName = (EditText) findViewById(R.id.EditLastName);
		lastName.setText(c.getFamilyName());
		
		EditText phoneHome = (EditText) findViewById(R.id.EditPhoneHome);
		phoneHome.setText("");
		EditText phoneMobile = (EditText) findViewById(R.id.EditPhoneMobile);
		phoneMobile.setText("");
		EditText phoneWork = (EditText) findViewById(R.id.EditPhoneWork);
		phoneWork.setText("");
		
		EditText emailHome = (EditText) findViewById(R.id.EditEmailHome);
		emailHome.setText("");
		
		for (ContactMethod cm : c.getContactMethods())
		{
			if(cm instanceof EmailContact)
			{
				switch (cm.getType())
				{
					case ContactsContract.CommonDataKinds.Email.TYPE_HOME:
						emailHome.setText(cm.getData());
						break;			
	
					default:
						break;
				}
			}
			else if(cm instanceof PhoneContact)
			{
				switch (cm.getType())
				{
					case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
						phoneHome.setText(cm.getData());
						break;
					case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
						phoneWork.setText(cm.getData());
						break;
					case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
						phoneMobile.setText(cm.getData());
						break;
		
					default:
						break;
				}
			}
		}
		
	}
	
	//TODO: this method is pretty much copy&paste from loadContact, maybe we can put them together somehow
	private Contact getContactByRawURI(Uri uri) throws SyncException
	{
		ContentResolver cr = getContentResolver();
		Cursor queryCursor = null;
		try
		{
			//TODO: hack to get Raw ID
			String tmp = uri.toString();
			String[] a = tmp.split("/");
			int idx = a.length -1;
			int id = Integer.parseInt(a[idx]);

			String where = ContactsContract.Data.RAW_CONTACT_ID + "=?";
			
			String[] projection = new String[] {
					Contacts.Data.MIMETYPE,
					StructuredName.GIVEN_NAME,
					StructuredName.FAMILY_NAME,
					Phone.NUMBER,
					Phone.TYPE,
					Email.DATA,
					Event.START_DATE,
					Photo.PHOTO,
					Note.NOTE
			};
			
			queryCursor = cr.query(ContactsContract.Data.CONTENT_URI, projection,
					where, new String[] { Integer.toString(id) }, null);

			if (queryCursor == null) throw new SyncException("",
					"cr.query returned null");
			if (!queryCursor.moveToFirst()) return null;

			Contact result = new Contact();
			result.setId(id);

			int idxMimeType = queryCursor.getColumnIndex(ContactsContract.Contacts.Data.MIMETYPE);
			String mimeType;
			
			do
			{
				mimeType = queryCursor.getString(idxMimeType);
				if (mimeType
						.equals(StructuredName.CONTENT_ITEM_TYPE))
				{
					int idxFirst = queryCursor
							.getColumnIndex(StructuredName.GIVEN_NAME);
					int idxLast = queryCursor
							.getColumnIndex(StructuredName.FAMILY_NAME);

					result.setGivenName(queryCursor.getString(idxFirst));
					result.setFamilyName(queryCursor.getString(idxLast));
				}
				else if (mimeType.equals(Phone.CONTENT_ITEM_TYPE))
				{
					int numberIdx = queryCursor.getColumnIndex(Phone.NUMBER);
					int typeIdx = queryCursor.getColumnIndex(Phone.TYPE);
					PhoneContact pc = new PhoneContact();
					pc.setData(queryCursor.getString(numberIdx));
					pc.setType(queryCursor.getInt(typeIdx));
					result.getContactMethods().add(pc);

				}
				else if (mimeType.equals(Email.CONTENT_ITEM_TYPE))
				{
					int dataIdx = queryCursor.getColumnIndex(Email.DATA);
					// int typeIdx =
					// emailCursor.getColumnIndex(CommonDataKinds.Email.TYPE);
					EmailContact pc = new EmailContact();
					pc.setData(queryCursor.getString(dataIdx));
					// pc.setType(emailCursor.getInt(typeIdx));
					result.getContactMethods().add(pc);

				}
				else if (mimeType.equals(Event.CONTENT_ITEM_TYPE))
				{
					int dateIdx = queryCursor.getColumnIndex(Event.START_DATE);
					String bday = queryCursor.getString(dateIdx);
					result.setBirthday(bday);

				}
				else if (mimeType.equals(Photo.CONTENT_ITEM_TYPE))
				{
					int colIdx = queryCursor.getColumnIndex(Photo.PHOTO);
					byte[] photo = queryCursor.getBlob(colIdx);
					result.setPhoto(photo);

				}
				else if (mimeType.equals(Note.CONTENT_ITEM_TYPE))
				{
					int colIdx = queryCursor.getColumnIndex(Note.NOTE);
					String note = queryCursor.getString(colIdx);
					result.setNote(note);
				}
			} while (queryCursor.moveToNext());

			return result;
		}
		finally
		{
			if (queryCursor != null) queryCursor.close();
		}
	}
}
