package at.dasz.KolabDroid.Settings;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import at.dasz.KolabDroid.R;

public class AccountManager extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
				
		setContentView(R.layout.account_manager);
		
		TextView debug = (TextView) findViewById(R.id.AccountMDebugTxt);
		debug.setText("TODO: I will help the user to clean up the contact mess we created with our previous releases :)");
		
		Log.i("AccM:", "I am the AccountManager");
	}
	
}