package com.PiksalStudio.thien.clouduploader;

import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;


public class LoginActivity extends ActionBarActivity {
    DropboxAPI<AndroidAuthSession> Dropbox_mApi=null;
    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    Button buttonLoginDropbox;
    private String LOG_TAG = LoginDropbox.class.getSimpleName();
    private String Dropbox_token = null;

    final String GOOGLEDRIVE_LOG_TAG ="Google Drive";
    final int GOOGLE_DRIVE_LOGIN_REQUEST_CODE = 100;
    GoogleApiClient mGoogleApiClient;
    GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener;
    GoogleApiClient.ConnectionCallbacks connectionCallbacks;
    Button buttonLoginGoogleDrive;

    //Dropbox appid, Dropbox appsecret
    String Dropbox_AppId=null;
    String Dropbox_AppSecret=null;

    //OneDrive
    String OneDrive_ClientID = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        buttonLoginDropbox = (Button) findViewById(R.id.button_login_dropbox);
        buttonLoginGoogleDrive = (Button) findViewById(R.id.button_login_GoogleDrive);
        //init prefs which is used to store access token
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = prefs.edit();

        //set dropbox appid, app secret from intent
        Intent intent = getIntent();
        Dropbox_AppId = intent.getExtras().getString(getResources().getString(R.string.extra_dropbox_app_id_request));
        Dropbox_AppSecret = intent.getExtras().getString(getResources().getString(R.string.extra_dropbox_app_secret_request));

        //get token
        Dropbox_token = prefs.getString(getResources().getString(R.string.prefs_dropbox_token), null);

        buttonLoginDropbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Dropbox_mApi!=null &&Dropbox_mApi.getSession().isLinked())
                {
                    Dropbox_mApi.getSession().unlink();//logout
                    //remove token from prefs
                    editor.remove(getResources().getString(R.string.prefs_dropbox_token));
                    editor.commit();//remember to commit
                    UpdateUI();
                }
                    else
                    new LoginDropbox().execute();//open browser, ask user to login dropbox, and ask for access permission
            }
        });

        buttonLoginGoogleDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginGoogleDrive();
            }
        });

    }



    protected void onResume() {
        super.onResume();
        UpdateUI();
        //auto login if have access token
        if (Dropbox_token!= null) {
            Log.i(LOG_TAG,"start login");
            new LoginDropbox().execute();
        }

        if (Dropbox_mApi!=null)
        if (Dropbox_mApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                Dropbox_mApi.getSession().finishAuthentication();
                String ACCESS_TOKEN = Dropbox_mApi.getSession().getOAuth2AccessToken();
                editor.putString(getResources().getString(R.string.prefs_dropbox_token),ACCESS_TOKEN);//put access token into prefs
                editor.commit();//commit or it will not save
                //accessToken should be save somewhere
                //TODO: accessToken ?
                Log.i("DbAuthLog", "Login successful");
                UpdateUI();
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLE_DRIVE_LOGIN_REQUEST_CODE)
            if (resultCode == RESULT_OK) {
                mGoogleApiClient.connect();
            }
    }






    ///////>>>>>>HELPER FUNCTION<<<<<<///////////////////

    //build AndroidAuthSession
    private AndroidAuthSession buildSession()
    {
        // APP_KEY and APP_SECRET goes here
        AppKeyPair appKeyPair= new AppKeyPair(Dropbox_AppId,Dropbox_AppId);
        AndroidAuthSession session = new AndroidAuthSession(appKeyPair,Dropbox_token);

        return session;
    }

    //show status of account button
    private void UpdateUI() {
        if (Dropbox_mApi != null && Dropbox_mApi.getSession().isLinked())
            buttonLoginDropbox.setText(getResources().getText(R.string.unlink_dropbox));
        else
            buttonLoginDropbox.setText(getResources().getText(R.string.login_dropbox));

    }

    //open browser, login, ask for permission
    private class LoginDropbox extends AsyncTask<Void,Void,Void>
    {
        @Override
        protected Void doInBackground(Void... params) {
            Log.i("DbAuthLog", "start authenticating");
            Dropbox_token=prefs.getString(getResources().getString(R.string.prefs_dropbox_token),null);

            // bind APP_KEY and APP_SECRET with session
            AndroidAuthSession session = buildSession();
            Dropbox_mApi = new DropboxAPI<AndroidAuthSession>(session);

            if (Dropbox_token!=null)
                Dropbox_mApi.getSession().setOAuth2AccessToken(Dropbox_token);

            if (Dropbox_token==null)
            Dropbox_mApi.getSession().startOAuth2Authentication(LoginActivity.this);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (Dropbox_mApi.getSession().authenticationSuccessful()) {
                try {
                    Dropbox_mApi.getSession().finishAuthentication();

                } catch (IllegalStateException e) {
                    Log.i("DbAuthLog", "Error authenticating", e);
                }
            }
            Log.i("DbAuthLog","Done async task");
            Log.i("DbAuthLog","is link "+Dropbox_mApi.getSession().isLinked() );
            UpdateUI();//work here
        }
    }

    ////////Google Drive Login//////////////
    private void LoginGoogleDrive(){
        onConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(ConnectionResult connectionResult) {
                Log.i(GOOGLEDRIVE_LOG_TAG,"onConnectionFailed");
                if (connectionResult.hasResolution()) {
                    try {
                        //For first login when user choose account then ask for permission
                        //must call onActivityResult
                        connectionResult.startResolutionForResult(LoginActivity.this, GOOGLE_DRIVE_LOGIN_REQUEST_CODE);
                    } catch (IntentSender.SendIntentException e) {
                        // Unable to resolve, message user appropriately
                        Log.i(GOOGLEDRIVE_LOG_TAG,"something wrong");
                        e.printStackTrace();
                    }
                } else {
                    GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), LoginActivity.this, 0).show();
                }
            }
        };

        connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                Log.i(GOOGLEDRIVE_LOG_TAG,"onConnected call back");
            }

            @Override
            public void onConnectionSuspended(int i) {
                Log.i(GOOGLEDRIVE_LOG_TAG,"onConnectionSuspended call back");
            }
        };

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addScope(Drive.SCOPE_APPFOLDER)
                .addConnectionCallbacks(connectionCallbacks)
                .addOnConnectionFailedListener(onConnectionFailedListener)
                .build();
        mGoogleApiClient.connect();


    }


}
