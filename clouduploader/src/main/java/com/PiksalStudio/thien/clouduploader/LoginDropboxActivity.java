package com.PiksalStudio.thien.clouduploader;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;


public class LoginDropboxActivity extends Activity {

    //Dropbox appid, Dropbox appsecret
    String Dropbox_AppId=null;
    String Dropbox_AppSecret=null;
    DropboxAPI<AndroidAuthSession> Dropbox_mApi=null;
    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    Button buttonLoginDropbox;
    private String LOG_TAG = LoginDropboxActivity.class.getSimpleName();
    private String Dropbox_token = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_dropbox);
        //init prefs which is used to store access token
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = prefs.edit();

        //set dropbox appid, app secret from intent
        Intent intent = getIntent();
        Dropbox_AppId = intent.getExtras().getString(getResources().getString(R.string.extra_dropbox_app_id_request));
        Dropbox_AppSecret = intent.getExtras().getString(getResources().getString(R.string.extra_dropbox_app_secret_request));

        //TODO :fix here login dropbox activity
            new LoginDropbox().execute();


    }

    protected void onResume() {
        super.onResume();
        if (Dropbox_mApi!=null)
            if (Dropbox_mApi.getSession().authenticationSuccessful()) {
                try {
                    // Required to complete auth, sets the access token on the session
                    Dropbox_mApi.getSession().finishAuthentication();
                    String ACCESS_TOKEN = Dropbox_mApi.getSession().getOAuth2AccessToken();
                    editor.remove(getResources().getString(R.string.prefs_dropbox_token));
                    editor.commit();
                    editor.putString(getResources().getString(R.string.prefs_dropbox_token),ACCESS_TOKEN);//put access token into prefs
                    editor.commit();//commit or it will not save
                    //accessToken should be save somewhere
                    //TODO: accessToken ?
                    Log.i("DbAuthLog", "Login successful");
                    finish();
                } catch (IllegalStateException e) {
                    Log.i("DbAuthLog", "Error authenticating", e);
                }
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
                Dropbox_mApi.getSession().startOAuth2Authentication(LoginDropboxActivity.this);
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
            Log.i("DbAuthLog", "is link " + Dropbox_mApi.getSession().isLinked() );


        }
    }

}
