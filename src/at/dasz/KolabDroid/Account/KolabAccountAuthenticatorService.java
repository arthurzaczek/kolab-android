package at.dasz.KolabDroid.Account;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;
import at.dasz.KolabDroid.Main;
import at.dasz.KolabDroid.R;
import at.dasz.KolabDroid.Provider.LocalCacheProvider;

public class KolabAccountAuthenticatorService extends Service
{
	private static AbstractAccountAuthenticator	authenticator;

	@Override
	public IBinder onBind(Intent intent)
	{
		IBinder ret = null;
		if (intent.getAction().equals(
				android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT)) ret = getAuthenticator()
				.getIBinder();
		return ret;

	}

	public AbstractAccountAuthenticator getAuthenticator()
	{
		if (authenticator == null) authenticator = new AccountAuthenticatorImpl(
				this);

		return authenticator;
	}

	private static class AccountAuthenticatorImpl extends
			AbstractAccountAuthenticator
	{
		private Context	context;

		public AccountAuthenticatorImpl(Context context)
		{
			super(context);
			this.context = context;
		}

		/**
		 * {@inheritDoc} Check if a KolabAccount already exists. If this is the
		 * case, we display an error since only one KolabAccount is supported
		 * yet. If no KolabAccount exists, we create one.
		 */
		@Override
		public Bundle addAccount(AccountAuthenticatorResponse response,
				String accountType, String authTokenType,
				String[] requiredFeatures, Bundle options)
				throws NetworkErrorException
		{
			Bundle reply = new Bundle();
			Account kolabAccount = getKolabDroidAccount(context);

			if (kolabAccount != null) handler.sendEmptyMessage(0);
			else createKolabDroidAccount(context);

			return reply;
		}

		private Handler	handler	= new Handler() {
									@Override
									public void handleMessage(
											android.os.Message msg)
									{
										if (msg.what == 0) Toast
												.makeText(
														context,
														"KolabAccount already exists.\nOnly one account is supported.",
														5).show();
									};
								};

		/**
		 * Ensures that the Kolab SyncAccount exists and creates it if it
		 * doesn't.
		 */
		private void createKolabDroidAccount(Context context)
		{
			Log.i(KolabAccountAuthenticatorService.class.getSimpleName(),
					"Creating Kolab Account.");
			AccountManager am = AccountManager.get(context);
			final String syncAccountName = context.getResources().getString(
					R.string.SYNC_ACCOUNT_NAME);
			final String syncAccountType = context.getResources().getString(
					R.string.SYNC_ACCOUNT_TYPE);

			Account kolabAccount = new Account(syncAccountName, syncAccountType);
			am.addAccountExplicitly(kolabAccount, null, null);
			ContentResolver.setSyncAutomatically(kolabAccount,
					ContactsContract.AUTHORITY, false); // False, because the
														// user needs to check
														// settings first
			if (Build.VERSION.SDK_INT <= 7) // android 2.1
			{
				ContentResolver.setSyncAutomatically(kolabAccount, "calendar",
						false);// False, because the user needs to check
								// settings first
			}
			else
			// >= android 2.2
			{
				ContentResolver.setSyncAutomatically(kolabAccount,
						"com.android.calendar", false); // False, because the
														// user needs to check
														// settings first
			}
			
			// Clear local cache, creating a new Account means
			// that an old account has been deleted
			// deleting an old account means, that every contact
			// and calendar item also will be delete
			LocalCacheProvider.resetContacts(context);
			LocalCacheProvider.resetCalendar(context);
		}

		private Account getKolabDroidAccount(Context context)
		{
			AccountManager am = AccountManager.get(context);
			final String syncAccountType = context.getResources().getString(
					R.string.SYNC_ACCOUNT_TYPE);
			Account[] accounts = am.getAccountsByType(syncAccountType);

			if (accounts.length > 1) Log.w(
					KolabAccountAuthenticatorService.class.getSimpleName(),
					"More than one Kolab-Account exists.");

			if (accounts.length > 0) return accounts[0];
			else return null;
		}

		@Override
		public Bundle confirmCredentials(AccountAuthenticatorResponse response,
				Account account, Bundle options)
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Bundle editProperties(AccountAuthenticatorResponse response,
				String accountType)
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Bundle getAuthToken(AccountAuthenticatorResponse response,
				Account account, String authTokenType, Bundle options)
				throws NetworkErrorException
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getAuthTokenLabel(String authTokenType)
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Bundle hasFeatures(AccountAuthenticatorResponse response,
				Account account, String[] features)
				throws NetworkErrorException
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Bundle updateCredentials(AccountAuthenticatorResponse response,
				Account account, String authTokenType, Bundle options)
		{
			// TODO Auto-generated method stub
			return null;
		}
	}

}
