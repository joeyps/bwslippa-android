package joeyp.bwslippa;

import org.apache.http.impl.client.DefaultHttpClient;

import com.google.android.gms.common.AccountPicker;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class Welcome extends Activity {
	DefaultHttpClient http_client = new DefaultHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        
        Button btnLogin = (Button) findViewById(R.id.button_login);
        btnLogin.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(Welcome.this, SignInActivity.class);
				startActivity(intent);
				finish();
//				Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[]{"com.google"},
//				         false, null, null, null, null);
//				startActivity(intent);
//				 startActivityForResult(intent, SOME_REQUEST_CODE);
			}
		});
        
        String accountName = BWSlippa.getPrimaryAccountName(this);
        if(accountName != null) {
        	Intent intent = new Intent();
			intent.setClass(this, BWSlippa.class);
			startActivity(intent);
			finish();
        }
        
    }

}
