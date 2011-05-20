package at.dasz.KolabDroid.Sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import at.dasz.KolabDroid.Settings.Settings;

public abstract class KolabAbstractSyncAdapter extends
		AbstractThreadedSyncAdapter
{
	protected Context				context;

	protected static final String		TAG					= "KolabSyncAdapter";
	protected final static boolean	DBG_IGNORE_DELAY	= false;

	public KolabAbstractSyncAdapter(Context context, boolean autoInitialize)
	{
		super(context, autoInitialize);
		this.context = context;
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult)
	{
		Log.i(TAG, ">>>>>>>>>>>>>>>>>>>>>>> performSync called!");

		Settings s = new Settings(this.context);
		Time supposedSyncTime = getLastSyncTime(s);
		boolean force = extras.getBoolean("force", false);
		int interval;
		if (DBG_IGNORE_DELAY || force)
		{
			interval = 0;
		}
		else
		{
			interval = s.getMinSynIntervalMinutes();
		}
		supposedSyncTime.minute += interval; // Avoid sync loops
		supposedSyncTime.normalize(false);

		Time currentTime = new Time();
		currentTime.set(System.currentTimeMillis());

		if (Time.compare(supposedSyncTime, currentTime) < 0)
		{
			SyncHandler handler = getHandler(context, account);
			SyncWorker syncWorker = new SyncWorker(this.context, account,
					handler);
			syncWorker.runWorker();

			StatusEntry status = SyncWorker.getStatus();

			syncResult.stats.numEntries = status.getItems();

			syncResult.stats.numDeletes = status.getLocalDeleted()
					+ status.getRemoteDeleted();
			syncResult.stats.numInserts = status.getLocalNew()
					+ status.getRemoteNew();
			syncResult.stats.numUpdates = status.getLocalChanged()
					+ status.getRemoteChanged();

			s.edit();
			setLastSyncTime(s, currentTime);
			s.save();
		}
		else
		{
			Log.i(TAG,
					"Sync skipped, next sync: "
							+ supposedSyncTime.format3339(false));
		}
		
		Log.i(TAG, "syncResult.hasError() = " + syncResult.hasError());
		Log.i(TAG, "<<<<<<<<<<<<<<<<<<<<<<< performSync finished!");
	}

	protected abstract SyncHandler getHandler(Context context, Account account);

	protected abstract Time getLastSyncTime(Settings s);

	protected abstract void setLastSyncTime(Settings s, Time t);
}
