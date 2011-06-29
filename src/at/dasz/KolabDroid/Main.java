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
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
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
import at.dasz.KolabDroid.Sync.StatusEntry;

public class Main extends Activity implements MainActivity
{

	private StatusListAdapter	statusAdapter	= null;
	private TextView			status			= null;
	private TextView			version			= null;
	private Account				account			= null;

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

		version = (TextView) findViewById(R.id.version);
		version.setText(String.format(getString(R.string.version_format),
				Utils.getVersionNumber(this)));

		status = (TextView) findViewById(R.id.status);
		StatusHandler.load(this);
		StatusHandler.writeStatus("");

		statusAdapter = new StatusListAdapter(this);

		ExpandableListView statusListView = (ExpandableListView) findViewById(R.id.statusList);
		statusListView.setAdapter(statusAdapter);
		statusListView.setClickable(true);
		statusListView.setOnChildClickListener(new OnChildClickListener() {
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id)
			{
				return onChildClickStatusList(parent, v, groupPosition,
						childPosition, id);
			}
		});

		bindStatus();
		account = (Account) getIntent().getParcelableExtra("account");
		if (account != null)
		{
			Log.i("Main", "Account = " + account.name);
		}
		else
		{
			Log.i("Main", "Account = null");
		}
	}

	public boolean onChildClickStatusList(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id)
	{
		final StatusEntry e = (StatusEntry) statusAdapter
				.getGroup(groupPosition);
		if (e.hasFatalError())
		{
			StringBuilder msg = new StringBuilder();
			msg.append("Should the error be reported?\n\n");
			msg.append(e.getFatalErrorMsg());
			NotificationDialog.showYesNo(this, msg.toString(),
					new OnClickListener() {
						public void onClick(DialogInterface dialog, int which)
						{
							org.acra.ErrorReporter.getInstance().addCustomData(
									"Status.FatalError", e.getFatalErrorMsg());
							org.acra.ErrorReporter.getInstance()
									.handleSilentException(null);
							Toast.makeText(getApplicationContext(),
									R.string.crash_dialog_ok_toast,
									Toast.LENGTH_SHORT).show();
						}
					}, NotificationDialog.closeDlg);
		}
		else
		{
			Toast.makeText(this, "No errors to report", Toast.LENGTH_SHORT)
					.show();
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

	public final static int	MENU_REFRESH				= 3;
	public final static int	MENU_CLEAR_LOG				= 6;
	public final static int	MENU_DIAG_DUMP				= 9;

	/* Creates the menu items */
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, MENU_REFRESH, 0, R.string.refreshstatus);
		menu.add(0, MENU_CLEAR_LOG, 0, R.string.clearlog);

		menu.add(0, MENU_DIAG_DUMP, 0, R.string.diag_dump);

		return true;
	}

	/* Handles item selections */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case MENU_REFRESH:
			bindStatus();
			return true;
		case MENU_CLEAR_LOG:
			StatusProvider statProvider = new StatusProvider(Main.this);
			statProvider.clearAllEntries();
			bindStatus();
			return true;
		case MENU_DIAG_DUMP:
			diagDump();
			return true;
		}
		return false;
	}

	private void diagDump()
	{
		DiagDump.writeDump(this);
	}

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
