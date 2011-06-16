package at.dasz.KolabDroid.ContactsContract;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import at.dasz.KolabDroid.R;
import at.dasz.KolabDroid.Sync.SyncException;

public class EditContactActivity extends Activity
{
	static final int DATE_DIALOG_ID = 0;
	
	private Contact			mContact;

	private ImageButton		photoBtn;

	private EditText		firstName;
	private EditText		lastName;
	private EditText		phoneHome;
	private EditText		phoneMobile;
	private EditText		phoneWork;
	private EditText		email1;
	private EditText		email2;
	private EditText		email3;
	private TextView		birthday;
	private EditText		notes;

	private PhoneContact	pcHome;
	private PhoneContact	pcMobile;
	private PhoneContact	pcWork;

	private EmailContact	ec1;
	private EmailContact	ec2;
	private EmailContact	ec3;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.edit_contact);
		initControls();

		loadContact();
		bindTo();
	}
	
	public void onChangeDateClicked(View v)
	{
		showDialog(DATE_DIALOG_ID);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
	    switch (id) {
	    case DATE_DIALOG_ID:
	        return new DatePickerDialog(this,
	                    mDateSetListener,
	                    0, 0, 0);
	    }
	    return null;
	}
	
	private DatePickerDialog.OnDateSetListener mDateSetListener =
        new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, 
                                  int monthOfYear, int dayOfMonth) {
                String date = String.format("%04d-%02d-%02d", year, monthOfYear+1, dayOfMonth);
                birthday.setText(date);
                mContact.setBirthday(date);
            }
        };

	private void bindTo()
	{
		firstName.setText(mContact.getGivenName());
		lastName.setText(mContact.getFamilyName());
		birthday.setText(mContact.getBirthday());
		notes.setText(mContact.getNotes());

		if (mContact.getPhoto() != null)
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(
					mContact.getPhoto());
			Drawable d = Drawable.createFromStream(bais, "picture");
			photoBtn.setBackgroundDrawable(d);
		}

		int emailCounter = 1;
		for (ContactMethod cm : mContact.getContactMethods())
		{
			if (cm instanceof EmailContact)
			{
				switch (emailCounter)
				{
				case 1:
					email1.setText(cm.getData());
					ec1 = (EmailContact) cm;
					break;
				case 2:
					email2.setText(cm.getData());
					ec2 = (EmailContact) cm;
					break;
				case 3:
					email3.setText(cm.getData());
					ec3 = (EmailContact) cm;
					break;

				default:
					// Sorry
					break;
				}
			}
			else if (cm instanceof PhoneContact)
			{
				switch (cm.getType())
				{
				case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
					phoneHome.setText(cm.getData());
					pcHome = (PhoneContact) cm;
					break;
				case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
					phoneWork.setText(cm.getData());
					pcWork = (PhoneContact) cm;
					break;
				case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
					phoneMobile.setText(cm.getData());
					pcMobile = (PhoneContact) cm;
					break;

				default:
					break;
				}
			}
		}
	}

	private void initControls()
	{
		firstName = (EditText) findViewById(R.id.EditFirstName);
		lastName = (EditText) findViewById(R.id.EditLastName);

		phoneHome = (EditText) findViewById(R.id.EditPhoneHome);
		phoneMobile = (EditText) findViewById(R.id.EditPhoneMobile);
		phoneWork = (EditText) findViewById(R.id.EditPhoneWork);

		email1 = (EditText) findViewById(R.id.EditEmail1);
		email2 = (EditText) findViewById(R.id.EditEmail2);
		email3 = (EditText) findViewById(R.id.EditEmail3);

		birthday = (TextView) findViewById(R.id.dateDisplay);
		notes = (EditText) findViewById(R.id.EditNotes);

		photoBtn = (ImageButton) findViewById(R.id.EditPhotoButton);
	}

	public void onPhotoBtnClicked(View v)
	{
		Intent i = new Intent(Intent.ACTION_PICK,
				android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
		startActivityForResult(Intent.createChooser(i, "Select Picture"), 1);
	}

	public void onRemPhotoClicked(View v)
	{
		mContact.setPhoto(null);
		photoBtn.setBackgroundDrawable(null);
	}

	private void loadContact()
	{
		Intent intent = getIntent();

		Uri uri = Uri.parse(intent.getDataString());
		Log.i("ECA:", "Edit uri: " + uri);

		if (uri.toString().endsWith("contacts")) // new contact
		{
			mContact = new Contact();
		}
		else
		{
			long contactID = ContentUris.parseId(uri);
			try
			{
				mContact = ContactDBHelper.getContactByRawID(contactID,
						getContentResolver());
			}
			catch (SyncException ex)
			{
				this.finish();
			}
		}
	}

	public void onSaveClicked(View view) throws SyncException
	{
		bindFrom();
		saveContact();
		this.finish();
	}

	public void onCancelClicked(View view)
	{
		this.finish();
	}

	private void saveContact() throws SyncException
	{
		ContactDBHelper.saveContact(mContact, this);

		// Toast with successfully saved
		Toast.makeText(this, R.string.editor_save_success, Toast.LENGTH_LONG)
				.show();
	}

	private void bindFrom()
	{
		mContact.setGivenName(firstName.getText().toString());
		mContact.setFamilyName(lastName.getText().toString());

		mContact.setNote(notes.getText().toString());

		pcHome = binFromPhone(pcHome, phoneHome.getText().toString(),
				Phone.TYPE_HOME);
		pcMobile = binFromPhone(pcMobile, phoneMobile.getText().toString(),
				Phone.TYPE_MOBILE);
		pcWork = binFromPhone(pcWork, phoneWork.getText().toString(),
				Phone.TYPE_WORK);

		ec1 = binFromEmail(ec1, email1.getText().toString());
		ec2 = binFromEmail(ec2, email1.getText().toString());
		ec3 = binFromEmail(ec3, email1.getText().toString());
	}

	private PhoneContact binFromPhone(PhoneContact pc, String txt, int type)
	{
		if (!TextUtils.isEmpty(txt))
		{
			if (pc == null)
			{
				pc = new PhoneContact();
				mContact.addContactMethod(pc);
			}
			pc.setType(type);
			pc.setData(txt);
		}
		else
		{
			mContact.removeContactMethod(pc);
			pc = null;
		}
		return pc;
	}

	private EmailContact binFromEmail(EmailContact ec, String txt)
	{
		if (!TextUtils.isEmpty(txt))
		{
			if (ec == null)
			{
				ec = new EmailContact();
				mContact.addContactMethod(ec);
			}
			ec.setType(Email.TYPE_OTHER);
			ec.setData(txt);
		}
		else
		{
			mContact.removeContactMethod(ec);
			ec = null;
		}
		return ec;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK)
		{
			if (requestCode == 1)
			{
				Uri selectedImageUri = data.getData();
				// Log.i("ECA:", "Selected pic uri:" + selectedImageUri);

				String[] filePathColumn = { MediaStore.Images.Media.DATA };

				Cursor cursor = getContentResolver().query(selectedImageUri,
						filePathColumn, null, null, null);
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
				catch (Exception e)
				{
					Log.e("ECA:", e.toString());
				}

				if (fileData != null)
				{
					ByteArrayInputStream bais = new ByteArrayInputStream(
							fileData);
					Drawable d = Drawable.createFromStream(bais, "picture");

					photoBtn.setBackgroundDrawable(d);
					mContact.setPhoto(fileData);
				}
				// Log.i("ECA:", "saved pic");

			}
		}
	}
}
