package at.dasz.KolabDroid.Settings;

//We probably wont need this account manager
//once one installs the branch and adds the sync account, all contact with "null" accounts are changed
//to be KolabAndroidAccounts

import android.accounts.Account;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import at.dasz.KolabDroid.R;
import at.dasz.KolabDroid.ContactsContract.Contact;
import at.dasz.KolabDroid.ContactsContract.ContactDBHelper;
import at.dasz.KolabDroid.Sync.SyncException;

public class AccountManager extends Activity
{
	public static final String ACC_DELIMITER = "--";
	
	private ListView contactList = null;
	private String destAccountName = "";
	private String destAccountType = "";
	
	private String srcAccountName = "";
	private String srcAccountType = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
				
		setContentView(R.layout.account_manager);
	
		//Log.i("AccM:", "I am the AccountManager");
		
		Button doit = (Button) findViewById(R.id.AccmanagerButtonDoit);
		ButtonPressedListener bpl = new ButtonPressedListener(this);
		doit.setOnClickListener(bpl);
		
		Account[] accounts = android.accounts.AccountManager.get(this).getAccounts();
		Log.i("AccM", "Amount of accounts: " + accounts.length);
			
		String[] oldAccItems = new String[accounts.length + 1];
		//String[] oldAccItems = new String[accounts.length +2]; //for dummy add 2 :)
		oldAccItems[0] = "null" + ACC_DELIMITER +"null";
		//oldAccItems[1] = "Dummy"+ACC_DELIMITER+"DummyType"; //to play around with
		int i = 1; //int i = 2;
		for(Account acc: accounts)
		{
			oldAccItems[i] = acc.name + ACC_DELIMITER + acc.type;
			i++;
		}
		
		Spinner accSpinner = (Spinner) findViewById(R.id.AccManagerAccSpinner);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, oldAccItems);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		accSpinner.setAdapter(adapter);
		
		OnItemSelectedListener spinnerListener = new AccountSelectedListener(this, "source");
		accSpinner.setOnItemSelectedListener(spinnerListener);
		
		String[] newAccItems = new String[accounts.length];
		//String[] newAccItems = new String[accounts.length+1]; //for dummy
		//newAccItems[0] = "Dummy"+ACC_DELIMITER+"DummyType";
		i = 0;
		for(Account acc: accounts)
		{
			newAccItems[i] = acc.name + ACC_DELIMITER + acc.type;
			i++;
		}
		
		Spinner newAccSpinner = (Spinner) findViewById(R.id.AccManagerSpinnerNewAccount);
		ArrayAdapter<String> newAccAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, newAccItems);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		newAccSpinner.setAdapter(newAccAdapter);
		
		OnItemSelectedListener newSpinnerListener = new AccountSelectedListener(this, "dest");
		newAccSpinner.setOnItemSelectedListener(newSpinnerListener);
	
		contactList = (ListView) findViewById(R.id.AccManagerContactList);
		
		//Set option as Multiple Choice. So that user can able to select more the one option from list
		contactList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		
		//uncheck all
		for(i=0; i<contactList.getCount(); i++)
		{
			contactList.setItemChecked(i, false);
		}
		
		CheckBox cBoxAll = (CheckBox) findViewById(R.id.AccManagerCheckBoxAll);
		
		cBoxAll.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if (buttonView.isChecked())
				{
					for(int i=0; i<contactList.getCount(); i++)
					{
						contactList.setItemChecked(i, true);
					}
				} 
				else 
				{
					for(int i=0; i<contactList.getCount(); i++)
					{
						contactList.setItemChecked(i, false);
					}
				}
			} 
		});
		
		//Log.i("AccM:", "created");		
	}
	
	public String getDestAccountName()
	{
		return destAccountName;
	}

	public String getDestAccountType()
	{
		return destAccountType;
	}
	
	public void updateContactList()
	{
		Log.i("AccM:", "Will filter list with: " + srcAccountName + " of type " + srcAccountType);
		
		ContentResolver cr = getContentResolver();
			
		String where = ContactsContract.RawContacts.ACCOUNT_NAME + "=?";
			
		Cursor dbContacts = null;
		
		if("null".equals(srcAccountName))
		{
			dbContacts = cr.query(ContactsContract.RawContacts.CONTENT_URI, null, where,
				new String[]{""}, null);
		}
		else
		{				
			dbContacts = cr.query(ContactsContract.RawContacts.CONTENT_URI, null, where,
					new String[]{srcAccountName}, null);
		}
		
		int count = dbContacts.getCount();
		
		Contact[] cs = new Contact[count];
		
		if(dbContacts != null && dbContacts.moveToFirst())
		{
			int rawIDCol = dbContacts.getColumnIndex(ContactsContract.RawContacts._ID);
			
			int i=0;
			do
			{
				int id = dbContacts.getInt(rawIDCol);
				try
				{
					cs[i] = ContactDBHelper.getContactByRawID(id, cr);
				}
				catch (SyncException ex)
				{
					// TODO Auto-generated catch block
					ex.printStackTrace();
				}
				
				i++;
			}
			while(dbContacts.moveToNext());
		}		
		contactList.setAdapter(new ArrayAdapter<Contact>(this, android.R.layout.simple_list_item_multiple_choice, cs));
		
		return;
		
	}
	
	public void setDestAccount(String acc)
	{
		String[] tokens = acc.split(AccountManager.ACC_DELIMITER);
		if(tokens.length == 2)
		{
			this.destAccountName = tokens[0];
			this.destAccountType = tokens[1];
		}
	}
	
	public void setSrcAccount(String acc)
	{
		String[] tokens = acc.split(AccountManager.ACC_DELIMITER);
		if(tokens.length == 2)
		{
			this.srcAccountName = tokens[0];
			this.srcAccountType = tokens[1];
		}
	}
	
	public ListView getContactList()
	{
		return this.contactList;
	}
	
	public class ButtonPressedListener implements View.OnClickListener
	{
		private AccountManager accM = null;
		private ContentResolver cr = getContentResolver();
		
		public ButtonPressedListener(AccountManager accM)
		{
			this.accM = accM;
		}

		public void onClick(View v)
		{
			Log.i("AccM:", "Button do it pressed");
			ListView contactList = accM.getContactList();
			
			String destAccName = accM.getDestAccountName();
			String destAccType = accM.getDestAccountType();
			
			int checkedContacts = 0;
			int updatedContacts = 0;
			
			for(int i=0; i<contactList.getCount(); i++)
			{
				if(contactList.isItemChecked(i))
				{
					checkedContacts++;
					
					Contact contactChecked = (Contact) contactList.getItemAtPosition(i);
					Log.i("AccM:", "Need to convert entry: " + contactChecked + " to Account: " + destAccName + "of type: " + destAccType);
					
					String selection = ContactsContract.RawContacts._ID + "=?";										
					
					ContentValues cvs = new ContentValues();
					cvs.put(ContactsContract.RawContacts.ACCOUNT_NAME, destAccName);
					cvs.put(ContactsContract.RawContacts.ACCOUNT_TYPE, destAccType);
					
					int rowsUpdated = cr.update(ContactsContract.RawContacts.CONTENT_URI, cvs, selection, new String[] {String.valueOf(contactChecked.getId())});
					if(rowsUpdated == 1)
						updatedContacts++;					
				}
			}
			
			this.accM.updateContactList();
			Toast.makeText(getBaseContext(), updatedContacts + " of "+ checkedContacts+" converted", Toast.LENGTH_LONG).show();
		}
		
	}
	
	public class AccountSelectedListener implements OnItemSelectedListener
	{
		private AccountManager accM = null;
		private String sourceDestType = "source";
		
		public AccountSelectedListener(AccountManager accM, String type)
		{
			this.accM = accM;
			this.sourceDestType = type;
		}
		
		public void onItemSelected(AdapterView<?> element, View arg1, int pos, long arg3)
		{
			Log.i("AccM:", "on selected SOURCE with item at pos: " + pos);
			if(element instanceof Spinner)
			{
				Spinner accSpin = (Spinner) element;
				String str = (String) accSpin.getItemAtPosition(pos);
				//Log.i("AccM:", "spinner");
				if("source".equals(sourceDestType))
				{
					this.accM.setSrcAccount(str);
					this.accM.updateContactList();
				}
				else
				{
					this.accM.setDestAccount(str);
				}
			}
		}

		public void onNothingSelected(AdapterView<?> arg0)
		{}
		
	}
	
}