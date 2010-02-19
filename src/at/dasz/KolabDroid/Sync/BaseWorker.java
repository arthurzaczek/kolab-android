package at.dasz.KolabDroid.Sync;

import android.content.Context;
import at.dasz.KolabDroid.R;

public abstract class BaseWorker extends Thread
{
	private final static String			SYNC_ROOT		= "SYNC";
	
	@Override
	public void start()
	{
		synchronized (SYNC_ROOT)
		{
			if(!isRunning)
			{
				isRunning = true;
				super.start();
			}
		}
	}

	private static boolean	isRunning	= false;
	private static boolean	isStopping	= false;

	public static boolean isRunning()
	{
		synchronized (SYNC_ROOT)
		{
			return isRunning;
		}
	}
	
	public static boolean isStopping()
	{
		synchronized (SYNC_ROOT)
		{
			return isStopping;
		}
	}
	
	protected abstract void runWorker();
	
	@Override
	public void run()
	{
		try
		{
			isStopping = false;
			runWorker();
		}
		finally
		{
			isRunning = false;
		}
	}
	
	public static void stopWorker()
	{
		synchronized (SYNC_ROOT)
		{
			isStopping = true;
		}
	}

	private static int	runningMessageResID	= 0;
	public static int getRunningMessageResID()
	{
		synchronized (SYNC_ROOT)
		{
			if(runningMessageResID == 0) return R.string.workerisrunning;
			return runningMessageResID;
		}
	}
	
	protected void setRunningMessage(int resid)
	{
		synchronized (SYNC_ROOT)
		{
			runningMessageResID = resid;
		}
	}

	protected Context	context;

	public BaseWorker(Context context)
	{
		this.context = context;
	}
}