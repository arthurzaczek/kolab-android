package at.dasz.KolabDroid.Settings;

import android.accounts.Account;
import android.app.Activity;
import android.os.Bundle;
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
import at.dasz.KolabDroid.R;

public class AccountManager extends Activity
{
	private ListView contactList = null;
	//private CheckBox cBoxAll
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
				
		setContentView(R.layout.account_manager);
	
		Log.i("AccM:", "I am the AccountManager");
		
		Button doit = (Button) findViewById(R.id.AccmanagerButtonDoit);
		ButtonPressedListener bpl = new ButtonPressedListener(this);
		doit.setOnClickListener(bpl);
		
		Account[] accounts = android.accounts.AccountManager.get(this).getAccounts();
		Log.i("AccM", "Amount of accounts: " + accounts.length);
			
		String[] oldAccItems = new String[accounts.length +1];
		oldAccItems[0] = "null";
		int i = 1;
		for(Account acc: accounts)
		{
			oldAccItems[i] = acc.name;
			i++;
		}
		
		Spinner accSpinner = (Spinner) findViewById(R.id.AccManagerAccSpinner);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, oldAccItems);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		accSpinner.setAdapter(adapter);
		
		OnItemSelectedListener spinnerListener = new AccountSelectedListener(this);
		accSpinner.setOnItemSelectedListener(spinnerListener);
		
		
		String[] newAccItems = new String[accounts.length];
		i = 0;
		for(Account acc: accounts)
		{
			newAccItems[i] = acc.name;
			i++;
		}
		
		Spinner newAccSpinner = (Spinner) findViewById(R.id.AccManagerSpinnerNewAccount);
		ArrayAdapter<String> newAccAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, newAccItems);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		newAccSpinner.setAdapter(newAccAdapter);
		
		
		//TODO: make this a list of raw contacts from contacts2.db
		
		String lv_items[] = { "Android", "iPhone", "BlackBerry" };

		
		contactList = (ListView) findViewById(R.id.AccManagerContactList);
		
		//Set option as Multiple Choice. So that user can able to select more the one option from list
		contactList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, lv_items));
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
				if (buttonView.isChecked()) { 
//					Toast.makeText(getBaseContext(), "Checked", 
//					Toast.LENGTH_SHORT).show();
					
					for(int i=0; i<contactList.getCount(); i++)
					{
						contactList.setItemChecked(i, true);
					}
				} 
				else 
				{ 
//					Toast.makeText(getBaseContext(), "UnChecked", 
//					Toast.LENGTH_SHORT).show();
					for(int i=0; i<contactList.getCount(); i++)
					{
						contactList.setItemChecked(i, false);
					}
				}
			} 
		});
		
		
		
	}
	
	public void updateContactList(String filter)
	{
		Log.i("AccM:", "Will filter list with: " + filter);
		
		if(filter == null) return;
		
		if("null".equals(filter))
		{
			return; //for testing => later we have contacts with no account here
		}
		else
		{
			String lv_items[] = {"eins", "zwei", "drei"};
			contactList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, lv_items));
		}
	}
	
	public ListView getContactList()
	{
		return this.contactList;
	}
	
	public class ButtonPressedListener implements View.OnClickListener
	{
		private AccountManager accM = null;
		
		public ButtonPressedListener(AccountManager accM)
		{
			this.accM = accM;
		}

		public void onClick(View v)
		{
			// TODO Auto-generated method stub
			Log.i("AccM:", "Button do it pressed");
			ListView contactList = accM.getContactList();
			
			for(int i=0; i<contactList.getCount(); i++)
			{
				if(contactList.isItemChecked(i))
				{
					String contactChecked = (String) contactList.getItemAtPosition(i);
					Log.i("AccM:", "Need to convert entry: " + contactChecked);
				}
			}
			
		}
		
	}
	
	public class AccountSelectedListener implements OnItemSelectedListener
	{
		private AccountManager accM = null;
		
		public AccountSelectedListener(AccountManager accM)
		{
			this.accM = accM;
		}
		
		public void onItemSelected(AdapterView<?> element, View arg1, int pos,
				long arg3)
		{
			Log.i("AccM:", "on selected with item at pos: " + pos);
			if(element instanceof Spinner)
			{
				Spinner accSpin = (Spinner) element;
				String str = (String) accSpin.getItemAtPosition(pos);
				//Log.i("AccM:", "spinner");
				this.accM.updateContactList(str);
				
			}
			
		}

		public void onNothingSelected(AdapterView<?> arg0)
		{
			// TODO Auto-generated method stub
			
		}
		
	}
	
}