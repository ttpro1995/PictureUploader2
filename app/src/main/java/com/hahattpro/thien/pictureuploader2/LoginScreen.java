package com.hahattpro.thien.pictureuploader2;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.api.GoogleApiClient;
import com.hahattpro.thien.pictureuploader2.StaticField.AppIDandSecret;
import com.piksalstudio.thien.clouduploader.CloudUploader;
import com.piksalstudio.thien.clouduploader.LoginActivity;


public class LoginScreen extends Activity {

    Button loginDropbox;
    Button selectGoogleDrive;
    CloudUploader cloudUploader = null;
    GoogleApiClient mGoogleApiClient = null;
    int GOOGLE_API_REQUEST_CODE= 1234;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);
        cloudUploader = new CloudUploader(LoginScreen.this,null, AppIDandSecret.AppID_Dropbox,AppIDandSecret.Secret_Dropbox);

        loginDropbox = (Button) findViewById(R.id.button_login_dropbox);
        loginDropbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               cloudUploader.LoginDropbox();
            }
        });

        selectGoogleDrive = (Button) findViewById(R.id.button_select_account_google_drive);
        selectGoogleDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               mGoogleApiClient = cloudUploader.SelectGoogleAccount(GOOGLE_API_REQUEST_CODE);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login_screen, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLE_API_REQUEST_CODE)
            if (resultCode == RESULT_OK)
                mGoogleApiClient.connect();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
