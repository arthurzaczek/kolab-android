package at.dasz.KolabDroid.ContactsContract;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import at.dasz.KolabDroid.R;
import at.dasz.KolabDroid.Sync.SyncException;

public class EditContactActivity extends Activity
{
	private Contact mContact = null;	
	private ImageButton photoBtn = null;
	
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
			}
			catch (SyncException ex)
			{
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
		
		EditText birthday = (EditText) findViewById(R.id.EditBirthday);
		birthday.setText(mContact.getBirthday());
		
//		DatePicker bday = (DatePicker) findViewById(R.id.EditBirthdayPicker);		
//		if(!"".equals(mContact.getBirthday()))
//		{
//			bday.updateDate(year, monthOfYear, dayOfMonth)
//		}
		
		photoBtn = (ImageButton) findViewById(R.id.EditPhotoButton);
		
		if(mContact.getPhoto() != null)
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(mContact.getPhoto());		
			Drawable d = Drawable.createFromStream(bais, "picture");
			
			photoBtn.setBackgroundDrawable(d);
		}
		
		PhotoButtonOnClickListener pbocl = new PhotoButtonOnClickListener();		
		photoBtn.setOnClickListener(pbocl);
		
		Button remPhoto = (Button) findViewById(R.id.EditRemovePhotoButton);
		remPhoto.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v)
			{
				mContact.setPhoto(null);
				photoBtn.setBackgroundDrawable(null);
			}
		});
		
		EditText notes = (EditText) findViewById(R.id.EditNotes);
		notes.setText(mContact.getNotes());
		
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
		EditText firstName = (EditText) findViewById(R.id.EditFirstName);
		mContact.setGivenName(firstName.getText().toString());
		
		EditText lastName = (EditText) findViewById(R.id.EditLastName);
		mContact.setFamilyName(lastName.getText().toString());
		
		EditText birthday = (EditText) findViewById(R.id.EditBirthday);
		mContact.setBirthday(birthday.getText().toString());
		
		//DatePicker bday = (DatePicker) findViewById(R.id.EditBirthdayPicker);		
		//bday.updateDate(year, monthOfYear, dayOfMonth)
		
		EditText notes = (EditText) findViewById(R.id.EditNotes);
		mContact.setNote(notes.getText().toString());
		
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
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (resultCode == RESULT_OK) {
	        if (requestCode == 1) {
	            Uri selectedImageUri = data.getData();
	            //Log.i("ECA:", "Selected pic uri:" + selectedImageUri);
	            
	            String[] filePathColumn = {MediaStore.Images.Media.DATA};

	            Cursor cursor = getContentResolver().query(selectedImageUri, filePathColumn, null, null, null);
	            cursor.moveToFirst();

	            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
	            String filePath = cursor.getString(columnIndex);
	            cursor.close();

	            byte[] fileData = null;
	            
	            File imageFile = new File(filePath);
	            try
	            {
		            FileInputStream fis = new FileInputStream(imageFile);
		            
		            fileData = new byte[(int) imageFile.length()];
		            fis.read(fileData);
		            fis.close();
	            }
	            catch(Exception e)
	            {
	            	Log.e("ECA:", e.toString());
	            }
	            
	            if(fileData != null)
	            {
		            ByteArrayInputStream bais = new ByteArrayInputStream(fileData);		
					Drawable d = Drawable.createFromStream(bais, "picture");
					
					photoBtn.setBackgroundDrawable(d);
		            mContact.setPhoto(fileData);
	            }
	            //Log.i("ECA:", "saved pic");
	            
	        }
	    }
	}
	
	public class PhotoButtonOnClickListener implements View.OnClickListener
	{		
		public PhotoButtonOnClickListener()
		{
		}

		public void onClick(View v)
		{		
			Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);			
			//startActivityForResult(i, 1);
			startActivityForResult(Intent.createChooser(i, "Select Picture"), 1);
		}		
	}
	
}
