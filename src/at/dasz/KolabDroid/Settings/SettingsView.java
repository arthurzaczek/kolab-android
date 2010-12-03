/*
 * Copyright 2010 Arthur Zaczek <arthur@dasz.at>, dasz.at OG; All rights reserved.
 * Copyright 2010 David Schmitt <david@dasz.at>, dasz.at OG; All rights reserved.
 * Copyright 2010 Sönke Schwardt-Krummrich <soenke@schwardtnet.de>; All rights reserved.
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
 *  
 *  This code contains some code snippets of the k9mail project that is licensed 
 *  under Apache License 2.0: http://k9mail.googlecode.com/svn/k9mail/trunk (SVN Rev 2337)
 *  Thanks to the k9mail project for their great work!
 */

package at.dasz.KolabDroid.Settings;

import java.net.ConnectException;
import java.net.UnknownHostException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.view.View;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;
import at.dasz.KolabDroid.R;
import at.dasz.KolabDroid.Imap.ImapClient;
import at.dasz.KolabDroid.Imap.TrustManagerFactory;

import javax.mail.AuthenticationFailedException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.MessagingException;
import javax.net.ssl.SSLException;

public class SettingsView extends Activity implements Runnable {
	public static final String EDIT_SETTINGS_ACTION = "at.dasz.KolabDroid.Settings.action.EDIT_TITLE";

	private EditText txtHost;
	private EditText txtPort;
	private CheckBox cbUseSSL;
	private EditText txtUsername;
	private EditText txtPassword;
	private EditText txtFolderContact;
	private EditText txtFolderCalendar;
	private CheckBox cbCreateRemoteHash;
	private CheckBox cbMergeContactsByName;
	private Spinner spAccount;
		
	private Handler mHandler = new Handler();
	private ProgressDialog mProgDialog = null;
	private Settings pref;
	private boolean isInitializing = true;
	private boolean mDestroyed = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        
        txtHost = (EditText)findViewById(R.id.edithost);
        txtPort = (EditText)findViewById(R.id.editport);
        cbUseSSL = (CheckBox)findViewById(R.id.usessl);
        txtUsername = (EditText)findViewById(R.id.editusername);
        txtPassword = (EditText)findViewById(R.id.editpassword);
        txtFolderContact = (EditText)findViewById(R.id.editfoldercontact);
        txtFolderCalendar = (EditText)findViewById(R.id.editfoldercalendar);
        cbCreateRemoteHash = (CheckBox)findViewById(R.id.createRemoteHash);
        cbMergeContactsByName = (CheckBox)findViewById(R.id.mergeContactsByName);
        spAccount = (Spinner)findViewById(R.id.selectAccount);

        pref = new Settings(this);
        
        cbUseSSL.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isInitializing) return;
				if(isChecked && "143".equals(txtPort.getText().toString())) {
					txtPort.setText("993");
				}
				else if(!isChecked && "993".equals(txtPort.getText().toString())) {
					txtPort.setText("143");
				}
			}
		});
			
		isInitializing = true;
		
		txtHost.setText(pref.getHost());
		txtPort.setText(Integer.toString(pref.getPort()));
		cbUseSSL.setChecked(pref.getUseSSL());
		txtUsername.setText(pref.getUsername());
		txtPassword.setText(pref.getPassword());
		txtFolderContact.setText(pref.getContactsFolder());
		txtFolderCalendar.setText(pref.getCalendarFolder());
		cbCreateRemoteHash.setChecked(pref.getCreateRemoteHash());
		cbMergeContactsByName.setChecked(pref.getMergeContactsByName());
		
		//TODO: adjust account spinner to show configured account
		//setFirstAccount();
		
		isInitializing = false;
	}

	@Override
	protected void onPause() {
        pref.edit();
		pref.setHost(txtHost.getText().toString());
		pref.setPort(Integer.parseInt(txtPort.getText().toString()));
		pref.setUseSSL(cbUseSSL.isChecked());
		pref.setUsername(txtUsername.getText().toString());
		pref.setPassword(txtPassword.getText().toString());
		pref.setContactsFolder(txtFolderContact.getText().toString());
		pref.setCalendarFolder(txtFolderCalendar.getText().toString());
		pref.setCreateRemoteHash(cbCreateRemoteHash.isChecked());
		pref.setMergeContactsByName(cbMergeContactsByName.isChecked());
		
		//TODO: adjust account spinner to show configured account
		// No one uses the account manager in this trunk version
		// version is also compatible to 1.6
		//setFirstAccount();
		
		pref.save();

		super.onPause();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		mDestroyed = true;
	}
	
//	private void setFirstAccount()
//	{
//		 // Get account data from system
//		
//        Account[] accounts = AccountManager.get(this).getAccounts();
//        
//        if(accounts.length >0)
//        {
//        	pref.setAccountName(accounts[0].name);
//        	pref.setAccountType(accounts[0].type);
//        }
//        else
//        {
//        	pref.setAccountName("");
//        	pref.setAccountType("");
//        }
//	}
	
	public void onClick(View v) {
        try
        {
            switch (v.getId())
            {
                case R.id.BtnTestSettings:
                	onClickCheckSettings();
                    break;
            }
        }
        catch (Exception e)
        {
        	showMsgDialog(R.string.checkSettingsTestFailedUnknownExceptionTitle, R.string.checkSettingsTestFailedUnknownException);
            Log.e("SettingsView", "unknown exception in onClick():", e);
        }
    }

	private void showMsgDialog(final int msgResIdTitle, final int msgResIdMsg, final Object... args)
    {
        mHandler.post(new Runnable()
        {
            public void run()
            {
                if (mDestroyed)
                {
                    return;
                }
                mProgDialog.setIndeterminate(false);
                new AlertDialog.Builder(SettingsView.this)
                .setTitle(getString(msgResIdTitle))
                .setMessage(getString(msgResIdMsg, args))
                .setCancelable(true)
                .setNeutralButton(getString(R.string.checkSettingsMsgDialogButton), null)
                .show();
            }
        });
    }

	
    public void run() {
    	Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		Store server = null;
		String hostname = txtHost.getText().toString();
		String port = txtPort.getText().toString();
		String username = txtUsername.getText().toString();
		try
		{
			TrustManagerFactory.loadLocalKeystore(getApplicationContext());
			Session session = ImapClient.getDefaultImapSession(
					Integer.parseInt(port),
					cbUseSSL.isChecked());
			server = ImapClient.openServer(session,
					hostname,
					username,
					txtPassword.getText().toString());
			Log.d("SettingsView", "Authentication with user " + username + " at " + hostname + ':' + port + " has been successful.");
			showMsgDialog(R.string.checkSettingsTestSuccessfulTitle,
					R.string.checkSettingsTestSuccessful);
		}
		catch (final MessagingException e) {
			Exception ne = e.getNextException();
			if (ne instanceof SSLException) {
				showAcceptKeyDialog(e);
				
			} else if (ne instanceof UnknownHostException) {
				showMsgDialog(R.string.checkSettingsTestFailedUnknownHostExceptionTitle, 
						R.string.checkSettingsTestFailedUnknownHostException, hostname);
				Log.e("SettingsView", "Host is unresolved: " + hostname + ':' + port, e);
				
			} else if (ne instanceof ConnectException) {
				showMsgDialog(R.string.checkSettingsTestFailedConnectExceptionTitle, 
						R.string.checkSettingsTestFailedConnectException, hostname);
				Log.e("SettingsView", "Failed to connecto to host: " + hostname + ':' + port, e);
				
			} else if (e instanceof AuthenticationFailedException) {
				showMsgDialog(R.string.checkSettingsTestFailedAuthenticationFailedExceptionTitle, 
						R.string.checkSettingsTestFailedAuthenticationFailedException, hostname);
				Log.e("SettingsView", "Authentication failed with user " + username + " at " + hostname + ':' + port, e);
				
			} else {
				showMsgDialog(R.string.checkSettingsTestFailedUnknownExceptionTitle, 
						R.string.checkSettingsTestFailedUnknownException, hostname);
				Log.e("SettingsView", "Unknown messaging exception while connecting to " + hostname + ':' + port + " with user " + username, e);
			}
		}
		catch(Exception e) {
			showMsgDialog(R.string.checkSettingsTestFailedUnknownExceptionTitle, 
					R.string.checkSettingsTestFailedUnknownException, hostname);
			Log.e("SettingsView", "Unknown exception while connecting to " + hostname + ':' + port + " with user " + username, e);
		}
		try {
			if (server != null) server.close();
		}
		catch (final MessagingException e) {
			showMsgDialog(R.string.checkSettingsTestFailedUnknownExceptionTitle, 
					R.string.checkSettingsTestFailedUnknownException, hostname);
			Log.e("SettingsView", "Unknown exception while closing connection to " + hostname + ':' + port, e);
		}
		mProgDialog.dismiss();
	}
    
    private void showAcceptKeyDialog(final Object... args) {
        mHandler.post(new Runnable()
        {
            public void run()
            {
                if (mDestroyed)
                {
                    return;
                }

                final java.security.cert.X509Certificate[] chain = TrustManagerFactory.getLastUsedChain();
                String exMessage = getString(R.string.checkSettingsTestFailedInvalidCertificateDefaultErrorMsg);

                Exception ex = ((Exception)args[0]);
                Log.v("showAcceptKeyDialog", "Got following exception: ", ex);
                if (ex != null)
                {
                	if (ex.getCause() != null)
                	{
                		if (ex.getCause().getCause() != null)
                		{
                			exMessage = ex.getCause().getCause().getMessage();

                		}
                		else
                		{
                			exMessage = ex.getCause().getMessage();
                		}
                	}
                	else
                	{
                		exMessage = ex.getMessage();
                	}
                }

                StringBuffer chainInfo = new StringBuffer(100);
                if (chain != null) {
                	for (int i = 0; i < chain.length; i++)
                	{
                		// display certificate chain information
                        chainInfo.append("Certificate chain[" + i + "]:\n");
                        chainInfo.append("Subject: " + chain[i].getSubjectDN().toString() + "\n");
                        chainInfo.append("Issuer: " + chain[i].getIssuerDN().toString() + "\n");
                	}
                } else {
                	Log.e("SettingsView", "internal error: chain is null");
                	chainInfo.append("Internal error: chain is null");
                }

                new AlertDialog.Builder(SettingsView.this)
                .setTitle(getString(R.string.checkSettingsTestFailedInvalidCertificateTitle))
                .setMessage(getString(R.string.checkSettingsTestFailedInvalidCertificateMessage) + 
                			exMessage + ":\n" + chainInfo.toString() )
                .setCancelable(true)
                .setPositiveButton(
                		getString(R.string.checkSettingsTestFailedInvalidCertificateBtnAccept),
                		new DialogInterface.OnClickListener()
                		{
                			public void onClick(DialogInterface dialog, int which)
                			{
                				try
                				{
            						Log.d("SettingsView", "user accepted certificate");
                					TrustManagerFactory.addCertificateChainToKeystore(getApplicationContext(), chain);
            						Log.d("SettingsView", "certificate saved to keystore");
                					showMsgDialog(R.string.checkSettingsCertificateChainSavedTitle,
                							R.string.checkSettingsCertificateChainSaved);
                					// username and password have not been checked yet ==> restart test
                					onClickCheckSettings();
                				}
								catch (java.security.cert.CertificateException e)
                				{
                					Log.e("SettingsView", "Adding certificate chain to local keystore failed: ", e);
                					showMsgDialog(R.string.checkSettingsTestFailedKeystoreErrorTitle, 
                							R.string.checkSettingsTestFailedKeystoreError);
                				}
                			}
                		})
                		.setNegativeButton(
                				getString(R.string.checkSettingsTestFailedInvalidCertificateBtnReject),
                				new DialogInterface.OnClickListener()
                				{
                					public void onClick(DialogInterface dialog, int which)
                					{
                						Log.d("SettingsView", "User declined certificate chain");
                    					showMsgDialog(R.string.checkSettingsCertificateChainDeclinedTitle,
                    							R.string.checkSettingsCertificateChainDeclined);
                					}
                				})
                				.show();
            }
        });
    }

    private void onClickCheckSettings() {
    	String title = getResources().getString(R.string.checkSettingsProgressDialogTitle);
    	String msg = getResources().getString(R.string.checkSettingsProgressDialogMessage);
    	mProgDialog = ProgressDialog.show(this, 
    			title, 
    			msg, 
    			true,   // no time limit 
    			false); // user cannot cancel
    	new Thread(this).start();
    }
        
}
