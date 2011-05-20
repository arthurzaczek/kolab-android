/*
 * Copyright 2010 Arthur Zaczek <arthur@dasz.at>, dasz.at OG; All rights reserved.
 * Copyright 2010 David Schmitt <david@dasz.at>, dasz.at OG; All rights reserved.
 *
 *  This file is part of Kolab Sync for Android.

 *  Kolab Sync for Android is free software: you can redistribute it
 *  and/or modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation, either version 3 of
 *  the License, or (at your option) any later version.

 *  Kolab Sync for Android is distributed in the hope that it will be
 *  useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 *  of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.

 *  You should have received a copy of the GNU General Public License
 *  along with Kolab Sync for Android.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package at.dasz.KolabDroid;

import javax.activation.DataHandler;

import android.accounts.Account;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SyncAdapterType;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;
import android.widget.Toast;
import at.dasz.KolabDroid.Imap.DchFactory;
import at.dasz.KolabDroid.Provider.StatusProvider;
import at.dasz.KolabDroid.Sync.BaseWorker;
import at.dasz.KolabDroid.Sync.ResetService;
import at.dasz.KolabDroid.Sync.ResetSoftService;
import at.dasz.KolabDroid.Sync.StatusEntry;

public class Main extends Activity implements MainActivity {
	
	private StatusListAdapter statusAdapter = null;
	private TextView status = null;
	private Account account = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		if (!DataHandler.hasDataContentHandlerFactory())
		{
			DataHandler.setDataContentHandlerFactory(new DchFactory());
		}
		
		setContentView(R.layout.main);
		
		status = (TextView) findViewById(R.id.status);
		StatusHandler.load(this);
		StatusHandler.writeStatus("");
		
		statusAdapter = new StatusListAdapter(this);
		
		ExpandableListView statusListView = (ExpandableListView)findViewById(R.id.statusList);
		statusListView.setAdapter(statusAdapter);
		statusListView.setClickable(true);
		statusListView.setOnChildClickListener(new OnChildClickListener() {
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id)
			{
				return onChildClickStatusList(parent, v, groupPosition, childPosition, id);
			}
		});
		
		bindStatus();		
		account = (Account) getIntent().getParcelableExtra("account");
		if (account != null) {
			Log.i("Main", "Account = " + account.name);
		} else {
			Log.i("Main", "Account = null");
		}
		
		// Some testings
		final SyncAdapterType[] syncs = ContentResolver.getSyncAdapterTypes();
		for (SyncAdapterType sync : syncs) {
			Log.d("Main", "Sync Adapter: " + sync.accountType + ", upload=" + sync.supportsUploading() + ", visible=" + sync.isUserVisible());
		}
	}
	
	public boolean onChildClickStatusList(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id)
	{
		final StatusEntry e = (StatusEntry)statusAdapter.getGroup(groupPosition);
		if(e.hasFatalError()) {
			StringBuilder msg = new StringBuilder();
			msg.append("Should the error be reported?\n\n");
			msg.append(e.getFatalErrorMsg());
			NotificationDialog.showYesNo(this, msg.toString(), new OnClickListener() {
				public void onClick(DialogInterface dialog, int which)
				{
					org.acra.ErrorReporter.getInstance().addCustomData("Status.FatalError", e.getFatalErrorMsg());
					org.acra.ErrorReporter.getInstance().handleSilentException(null);
					Toast.makeText(getApplicationContext(), R.string.crash_dialog_ok_toast, Toast.LENGTH_SHORT).show();
				}
			}, NotificationDialog.closeDlg);
		} else {
			Toast.makeText(this, "No errors to report", Toast.LENGTH_SHORT).show();
		}
		return true;
	}

    @Override
    protected void onResume()
    {
    	status = (TextView) findViewById(R.id.status);
		StatusHandler.load(this);
    	super.onResume();
    }
    
    @Override
    protected void onPause()
    {
    	StatusHandler.unload();
    	super.onPause();
    }
    
    public final static int MENU_RESET_CALENDAR = 1;
    public final static int MENU_RESET_CONTACTS = 2;
    public final static int MENU_REFRESH = 3;
    public final static int MENU_CLEAR_LOG = 6;
    public final static int MENU_RESET_CALENDAR_SOFT = 7;
    public final static int MENU_RESET_CONTACTS_SOFT = 8;
    
    /* Creates the menu items */
    public boolean onCreateOptionsMenu(Menu menu) {
        //menu.add(0, MENU_SETTINGS, 0, R.string.settings);

    	menu.add(0, MENU_RESET_CALENDAR, 0, R.string.ResetCalendar);
    	menu.add(0, MENU_RESET_CALENDAR_SOFT, 0, R.string.ResetCalendarSoft);
    	
    	menu.add(0, MENU_RESET_CONTACTS, 0, R.string.ResetContacts);
    	menu.add(0, MENU_RESET_CONTACTS_SOFT, 0, R.string.ResetContactsSoft);    	
    	
    	menu.add(0, MENU_CLEAR_LOG, 0, R.string.clearlog);
    	menu.add(0, MENU_REFRESH, 0, R.string.refreshstatus);
    	        
        //menu.add(0, MENU_RESET, 0, R.string.reset);
        
        return true;
    }

    /* Handles item selections */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_RESET_CALENDAR:
        	resetData(MENU_RESET_CALENDAR);
        	return true;
        case MENU_RESET_CONTACTS:
        	resetData(MENU_RESET_CONTACTS);
        	return true;
        case MENU_RESET_CALENDAR_SOFT:
    		resetDataSoft(MENU_RESET_CALENDAR_SOFT);
    		bindStatus();
            return true;
        case MENU_RESET_CONTACTS_SOFT:
    		resetDataSoft(MENU_RESET_CONTACTS_SOFT);
    		bindStatus();
            return true;
        case MENU_REFRESH:
        	bindStatus();
            return true;
        case MENU_CLEAR_LOG:
        	StatusProvider statProvider = new StatusProvider(Main.this);
        	statProvider.clearAllEntries();
        	bindStatus();
            return true;
        }
        return false;
    }

	private void resetData(int what)
	{	
		if(BaseWorker.isRunning()) {
			NotificationDialog.show(this, BaseWorker.getRunningMessageResID());
		} else {
			
			switch (what)
			{
				case MENU_RESET_CALENDAR:
					NotificationDialog.showYesNo(this, R.string.really_reset_calendar, 
							dlgResetCalendarListener, NotificationDialog.closeDlg);
				break;
				
				case MENU_RESET_CONTACTS:
					NotificationDialog.showYesNo(this, R.string.really_reset_contacts, 
							dlgResetContactsListener, NotificationDialog.closeDlg);
				break;

				default:
					break;
			}			
			
		}
	}
	
	private void resetDataSoft(int what)
	{
		if(BaseWorker.isRunning()) {
			NotificationDialog.show(this, BaseWorker.getRunningMessageResID());
		} else {
			
			switch (what)
			{
				case MENU_RESET_CALENDAR_SOFT:
					NotificationDialog.showYesNo(this, R.string.really_reset_calendar_soft, 
							dlgResetCalendarSoftListener, NotificationDialog.closeDlg);
					break;
					
				case MENU_RESET_CONTACTS_SOFT:
					NotificationDialog.showYesNo(this, R.string.really_reset_contacts_soft, 
							dlgResetContactsSoftListener, NotificationDialog.closeDlg);
					break;

				default:
					break;
			}
			
		}
	}

    private final DialogInterface.OnClickListener dlgResetCalendarListener = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			ResetService.startReset(Main.this, MENU_RESET_CALENDAR);
			dialog.cancel();
		}
    };
    
    private final DialogInterface.OnClickListener dlgResetContactsListener = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			ResetService.startReset(Main.this, MENU_RESET_CONTACTS);
			dialog.cancel();
		}
    };
    
    private final DialogInterface.OnClickListener dlgResetCalendarSoftListener = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			ResetSoftService.startReset(Main.this, MENU_RESET_CALENDAR_SOFT);
			dialog.cancel();
		}
    };
    
    private final DialogInterface.OnClickListener dlgResetContactsSoftListener = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			ResetSoftService.startReset(Main.this, MENU_RESET_CONTACTS_SOFT);
			dialog.cancel();
		}
    };

	public void setStatusText(int resid)
	{
		status.setText(resid);
	}

	public void setStatusText(String text)
	{
		status.setText(text);		
	}
	
	public void bindStatus()
	{
		statusAdapter.refresh();
	}
}
