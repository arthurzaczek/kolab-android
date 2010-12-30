package at.dasz.KolabDroid.ContactsContract;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
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
import android.widget.Toast;
import at.dasz.KolabDroid.R;
import at.dasz.KolabDroid.Sync.SyncException;

public class EditContactActivity extends Activity
{
	private Contact mContact = null;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
				
		setContentView(R.layout.edit_contact);
		
		Intent intent = getIntent();
		
		Uri uri = Uri.parse(intent.getDataString());		
		Log.i("ECA:", "Edit uri: "+ uri);
		
		if(uri.toString().endsWith("contacts")) //new contact
		{						
			mContact = new Contact();
		}
		else
		{	
			try
			{
				long contactID = ContentUris.parseId(uri);
				mContact = ContactDBHelper.getContactByRawID(contactID, getContentResolver());			
				//Log.i("ECA:", "Got contact");
			}
			catch (SyncException ex)
			{
				// TODO Auto-generated catch block
				ex.printStackTrace();
			}
		}		
		
		EditText firstName = (EditText) findViewById(R.id.EditFirstName);
		firstName.setText(mContact.getGivenName());
		
		EditText lastName = (EditText) findViewById(R.id.EditLastName);
		lastName.setText(mContact.getFamilyName());
		
		EditText phoneHome = (EditText) findViewById(R.id.EditPhoneHome);
		EditText phoneMobile = (EditText) findViewById(R.id.EditPhoneMobile);
		EditText phoneWork = (EditText) findViewById(R.id.EditPhoneWork);
		
		EditText emailHome = (EditText) findViewById(R.id.EditEmailHome);
		
		for (ContactMethod cm : mContact.getContactMethods())
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

	
	@Override
	public void onBackPressed()
	{
		//Log.i("ECA:", "on back");
//		if(mContact == null) //we save a new contact
//			mContact = new Contact();
		
		EditText firstName = (EditText) findViewById(R.id.EditFirstName);
		mContact.setGivenName(firstName.getText().toString());
		
		EditText lastName = (EditText) findViewById(R.id.EditLastName);
		mContact.setFamilyName(lastName.getText().toString());
		
		mContact.clearContactMethods();
		
		EditText phoneHome = (EditText) findViewById(R.id.EditPhoneHome);
		if(! "".equals(phoneHome.getText().toString()))
		{
			PhoneContact pc = new PhoneContact();
			pc.setType(ContactsContract.CommonDataKinds.Phone.TYPE_HOME);
			pc.setData(phoneHome.getText().toString());
			
			mContact.addContactMethod(pc);
		}
		
		EditText phoneMobile = (EditText) findViewById(R.id.EditPhoneMobile);
		if(! "".equals(phoneMobile.getText().toString()))
		{
			PhoneContact pc = new PhoneContact();
			pc.setType(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
			pc.setData(phoneMobile.getText().toString());
			
			mContact.addContactMethod(pc);
		}
		
		EditText phoneWork = (EditText) findViewById(R.id.EditPhoneWork);
		if(! "".equals(phoneWork.getText().toString()))
		{
			PhoneContact pc = new PhoneContact();
			pc.setType(ContactsContract.CommonDataKinds.Phone.TYPE_WORK);
			pc.setData(phoneWork.getText().toString());
			
			mContact.addContactMethod(pc);
		}
		
		EditText emailHome = (EditText) findViewById(R.id.EditEmailHome);
		if(! "".equals(emailHome.getText().toString()))
		{
			EmailContact ec = new EmailContact();
			ec.setType(ContactsContract.CommonDataKinds.Email.TYPE_HOME);
			ec.setData(emailHome.getText().toString());
			
			mContact.addContactMethod(ec);
		}
		
		try
		{
			ContactDBHelper.saveContact(mContact, this);
			
			//Toast with successfully saved
			Toast notice = Toast.makeText(this, R.string.editor_save_success, Toast.LENGTH_LONG);
			notice.show();
			
		}
		catch (SyncException ex)
		{
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
		
		super.onBackPressed();
	}
	
}
