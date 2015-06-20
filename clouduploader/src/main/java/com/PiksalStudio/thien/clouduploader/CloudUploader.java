package com.PiksalStudio.thien.clouduploader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.plus.Plus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Thien on 6/8/2015.
 * Upload to google drive, dropbox
 */
public class CloudUploader {
    //--------------------------Variable declaration------------------------------
    //init
    Context context; //activity context
    String APP_FOLDER_NAME;
    Activity activity;

    //LogTag
    String DROPBOX_LOG_TAG = "Dropbox";
    String GOOGLEDRIVE_LOG_TAG = "Google Drive";

    //Prepare for Upload
    InputStream inputStream;
    String File_Name;//name with extension
     Long File_length;


    //success flag
    boolean error_Dropbox = false;
    boolean google_drive = false;

    //frefs
    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    //dropbox api
    DropboxAPI<AndroidAuthSession> Dropbox_mApi = null;
    String Dropbox_token=null;
    private String Dropbox_AppId=null;
    private String Dropbox_AppSecret=null;

    //Handler
    private Handler dropbox_mHandler=null;
    private Handler googledriver_mHandle=null;

    //google drive
    GoogleApiClient mGoogleApiClient = null;
    GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener= null;
    GoogleApiClient.ConnectionCallbacks connectionCallbacks=null;
    final int GOOGLE_DRIVE_LOGIN_REQUEST_CODE = 100;



    //-------------------------------

    public CloudUploader( Activity activity, String APP_FOLDER_NAME, String Dropbox_app_id, String Dropbox_app_secret) {

        this.APP_FOLDER_NAME = APP_FOLDER_NAME;
        this.activity = activity;
        this.context = this.activity.getBaseContext();
        this.Dropbox_AppId = Dropbox_app_id;
        this.Dropbox_AppSecret = Dropbox_app_secret;

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        editor = prefs.edit();
    }

    /*
    * start Login activity
    * Allow user to login
    * */
    public void StartLoginActivity()
    {
        Intent intent = new Intent(context,LoginActivity.class);
        intent.putExtra(context.getResources().getString(R.string.extra_dropbox_app_id_request),Dropbox_AppId);
        intent.putExtra(context.getResources().getString(R.string.extra_dropbox_app_secret_request),Dropbox_AppSecret);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /*
    * LoginDropbox will open device web browser, ask user to login, ask for permission, and store dropbox_token
    * After login successfully with LoginDropbox ( use  public boolean Dropbox_isLogin() to check)
    *   you can upload thing to dropbox
    * */
    public void LoginDropbox()
    {
        Intent intent = new Intent(context,LoginDropboxActivity.class);
        intent.putExtra(context.getResources().getString(R.string.extra_dropbox_app_id_request),Dropbox_AppId);
        intent.putExtra(context.getResources().getString(R.string.extra_dropbox_app_secret_request),Dropbox_AppSecret);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }


    /*Change default account which is use for Google Drive
    * In activity, must implement
        @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Google_API_request_code)
            if (resultCode==RESULT_OK) {
                mGoogleApiClient.connect();
                Log.i("google","result ok");
            }
    }
    */
    public GoogleApiClient SelectGoogleAccount(int Google_API_request_code){

        if (mGoogleApiClient!=null&&mGoogleApiClient.isConnected())
            mGoogleApiClient.clearDefaultAccountAndReconnect();
        else
        LoginGoogleDrive(Google_API_request_code);
        return mGoogleApiClient;
    }


    /*
    * Check if dropbox is logined
    * When this function return true, you can use  public void UploadFileDropbox(String Name, InputStream is, Long Length, Handler handler)
    * */
    public boolean Dropbox_isLogin(){
        //TODO: test Dropbox_isLogin
        Log.d(DROPBOX_LOG_TAG, "start_isLogin");
        String tmp_DP_TOKEN = prefs.getString(context.getResources().getString(R.string.prefs_dropbox_token) ,null);
        Log.d(DROPBOX_LOG_TAG,"isLogin token key ="+tmp_DP_TOKEN);
        //if no dp_token then dropbox is not login
        if (tmp_DP_TOKEN==null) {
            return false;
        }
        //check if dropbox token is valid
        AndroidAuthSession session = buildSession();
        Dropbox_mApi = new DropboxAPI<AndroidAuthSession>(session);
        Dropbox_mApi.getSession().setOAuth2AccessToken(tmp_DP_TOKEN);
        if (Dropbox_mApi.getSession().isLinked()) {
            Log.d(DROPBOX_LOG_TAG,"end_isLogin, return true");
            return true;
        }
        Log.d(DROPBOX_LOG_TAG, "end_isLogin return false");
        return false;
    }



    public void UploadFileDropbox(String Name, InputStream is, Long Length, Handler handler){
            this.File_Name = Name;
            this.File_length = Length;
            this.inputStream = is;
            this.dropbox_mHandler = handler;
        new LoginDropboxAndUpload().execute();
    }

    public void UploadFileGoogleDrive(String Name, InputStream is, Long Length, Handler handler ){
        this.File_Name = Name;
        this.File_length = Length;
        this.inputStream = is;
        this.googledriver_mHandle = handler;
        new UploadGoogleDrive().execute();
    }


    //----------------DROP BOX--------------------------

    //build AndroidAuthSession
    private AndroidAuthSession buildSession() {
        // APP_KEY and APP_SECRET goes here
        AppKeyPair appKeyPair = new AppKeyPair(Dropbox_AppId,Dropbox_AppSecret );

        AndroidAuthSession session = new AndroidAuthSession(appKeyPair, Dropbox_token);

        return session;
    }



    //set token to Dropbox_mApi
    private class LoginDropboxAndUpload extends AsyncTask<Void, Void, Void> {



        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            error_Dropbox=false;

        }

        @Override
        protected Void doInBackground(Void... params) {
            //get token
            Dropbox_token = prefs.getString(context.getResources().getString(R.string.prefs_dropbox_token), null);
            // bind APP_KEY and APP_SECRET with session and Access token
            AndroidAuthSession session = buildSession();
            Dropbox_mApi = new DropboxAPI<AndroidAuthSession>(session);

            if (Dropbox_token != null)
                Dropbox_mApi.getSession().setOAuth2AccessToken(Dropbox_token);

            if (Dropbox_token == null) {
                //dropbox not link because token is null
                // Login at Login Activity
                error_Dropbox = true;
                Log.e(DROPBOX_LOG_TAG, "Dropbox is not linked");
            }
            if (Dropbox_mApi.getSession().isLinked())
                Log.i(DROPBOX_LOG_TAG, "Dropbox is Link");
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (Dropbox_mApi.getSession().isLinked())
                new UploadPicture_Dropbox().execute();

            if (error_Dropbox) {
                String tmp = "Dropbox Upload Error. ";
                if (Dropbox_token == null) {
                    tmp = tmp + "Dropbox is not linked";
                }
                dropbox_failed_handle();
                Log.e(DROPBOX_LOG_TAG,tmp);
            }

        }
    }

    //Upload select picture (which is shown on image view to dropbox
    private class UploadPicture_Dropbox extends AsyncTask<Void, Long, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i(DROPBOX_LOG_TAG,"Start uploading");
        }

        @Override
        protected Void doInBackground(Void... voids) {



            /////////////////////////
            try {

                CopyInputStream copyInputStream = new CopyInputStream(inputStream);//init copyInputStream
                inputStream = copyInputStream.getCopy();//restore source inputstream

                Log.d(DROPBOX_LOG_TAG, "File Name = " + File_Name);
                Log.d(DROPBOX_LOG_TAG, "File length = " + File_length);



                InputStream tmpis = copyInputStream.getCopy();//create tmp inputstream
                Dropbox_mApi.putFile(File_Name // path in drop box
                        , tmpis //input stream
                        , File_length //file.length()
                        ,null
                        ,null);


            }
            catch (DropboxException e){ e.printStackTrace();
                    error_Dropbox = true;

            }
            return null;
        }



        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!error_Dropbox ) {
                Log.i(DROPBOX_LOG_TAG, "Dropbox Upload Complete");
                dropbox_ok_handle();
            }
            else {
               dropbox_failed_handle();

            }


        }
    }

    private void dropbox_failed_handle(){
        Message message = new Message();
        message.arg1 = -1;//error
        dropbox_mHandler.sendMessage(message);
        dropbox_mHandler=null;
    }

    private void dropbox_ok_handle(){
        Message message = new Message();
        message.arg1 = 1;//upload success
        dropbox_mHandler.sendMessage(message);
        dropbox_mHandler=null;
    }

    //-------------------------------------------

    //-------GOOGLE DRIVE ---------------
    ////////////////google drive ////////////


    private class UploadGoogleDrive extends AsyncTask<Void,Void,Void> {

        DriveId mFolderDriveId = null;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Void doInBackground(Void... params) {
            LoginGoogleDrive(GOOGLE_DRIVE_LOGIN_REQUEST_CODE);//start connecting to google api
            try {//wait for it
                while (mGoogleApiClient==null||!mGoogleApiClient.isConnected())
                    Thread.sleep(100);
            }
            catch (InterruptedException e){}


            CopyInputStream copyInputStream = new CopyInputStream(inputStream);
            inputStream = copyInputStream.getCopy();//restore source inputstream

            InputStream tmpis = copyInputStream.getCopy();
            SaveFileToDrive(File_Name,tmpis,APP_FOLDER_NAME);

            return null;
        }



        /*
        * Input: title, inputstream
        * Upload to google drive
        * */
        private void SaveFileToDrive(final String title, InputStream is, String FolderName)
        {
            // Start by creating a new contents, and setting a callback.
            Log.i(GOOGLEDRIVE_LOG_TAG, "Creating new contents.");



            final InputStream inputStream = is;

            //Get DriveID of old Drive folder is created
            String tmp_driveid = prefs.getString(context.getResources().getString(R.string.prefs_googledrive_folder),null);
            if (tmp_driveid==null)
                mFolderDriveId =  null;
            else
                mFolderDriveId = DriveId.decodeFromString(tmp_driveid);

            //Create PictureUploader  Folder if there are no PictureUploader folder
            if (mFolderDriveId == null){
                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                        .setTitle(FolderName).build();
                Drive.DriveApi.getRootFolder(mGoogleApiClient).createFolder(
                        mGoogleApiClient, changeSet).setResultCallback(callback);}

            try {//wait for creating new folder
                while (mFolderDriveId == null)
                    Thread.sleep(1000);
            }
            catch (InterruptedException e){e.printStackTrace();}



            Drive.DriveApi.newDriveContents(mGoogleApiClient)
                    .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {

                        @Override
                        public void onResult(DriveApi.DriveContentsResult result) {
                            // If the operation was not successful, we cannot do anything
                            // and must
                            // fail.
                            if (!result.getStatus().isSuccess()) {
                                Log.i(GOOGLEDRIVE_LOG_TAG, "Failed to create new contents.");



                                return;
                            }
                            // Otherwise, we can write our data to the new contents.
                            Log.i(GOOGLEDRIVE_LOG_TAG, "New contents created.");
                            // Get an output stream for the contents.
                            OutputStream outputStream = result.getDriveContents().getOutputStream();
                            // Write the bitmap data from it.


                            try {
                                //Copy from input stream to output stream
                                org.apache.commons.io.IOUtils.copy(inputStream, outputStream);
                            } catch (IOException e1) {
                                Log.i(GOOGLEDRIVE_LOG_TAG, "Unable to write file contents.");
                            }
                            // Create the initial metadata - MIME type and title.
                            // Note that the user will be able to change the title later.

                            // Create an intent for the file chooser, and start it.
                            /*
                            IntentSender intentSender = Drive.DriveApi
                                    .newCreateFileActivityBuilder()
                                    .setInitialMetadata(metadataChangeSet)
                                    .setInitialDriveContents(result.getDriveContents())
                                    .build(mGoogleApiClient);
                            try {
                                startIntentSenderForResult(
                                        intentSender, 0, null, 0, 0, 0);
                            } catch (IntentSender.SendIntentException e) {
                                Log.i(GOOGLEDRIVE_LOG_TAG, "Failed to launch file chooser.");
                            }
                            */


                            //Create a file at root folder
                            MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                    .setMimeType("image/jpeg").setTitle(title).build();
                            DriveFolder root = Drive.DriveApi.getFolder(mGoogleApiClient,mFolderDriveId);
                            root.createFile(mGoogleApiClient, metadataChangeSet, result.getDriveContents()).setResultCallback(fileCallback);

                        }
                    });
        }

        //create new file
        final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
                ResultCallback<DriveFolder.DriveFileResult>() {
                    @Override
                    public void onResult(DriveFolder.DriveFileResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.e(GOOGLEDRIVE_LOG_TAG,"Error while trying to create the file");
                            google_drive_failed_handle();
                            return;
                        }
                        Log.i(GOOGLEDRIVE_LOG_TAG,"Created a file  "
                                + result.getDriveFile().getDriveId());
                        google_drive_ok_handle();//google drive upload ok

                    }
                };

        //Create new folder
        final ResultCallback<DriveFolder.DriveFolderResult> callback = new ResultCallback<DriveFolder.DriveFolderResult>() {
            @Override
            public void onResult(DriveFolder.DriveFolderResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.e(GOOGLEDRIVE_LOG_TAG,"Error while trying to create the folder");
                   google_drive_failed_handle();
                    return;
                }
                Log.i(GOOGLEDRIVE_LOG_TAG, "Created a folder: " + result.getDriveFolder().getDriveId());

                //store new drive id into prefs
                mFolderDriveId = result.getDriveFolder().getDriveId();
                editor.clear();
                editor.putString(context.getResources().getString(R.string.prefs_googledrive_folder), result.getDriveFolder().getDriveId().encodeToString());
                editor.commit();
            }
        };
    }

    //Connect to google api (maybe it is run its own thread ?)
    private void LoginGoogleDrive(@Nullable final int  Google_api_request_code) {

        //It is auto-gen constructor
        onConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(ConnectionResult connectionResult) {
                Log.i(GOOGLEDRIVE_LOG_TAG, "onConnectionFailed");
                if (connectionResult.hasResolution()) {
                    try {
                        //Must implement onActivityResult
                        //google api will ask for user permission in first time
                        connectionResult.startResolutionForResult(activity, Google_api_request_code);
                    } catch (IntentSender.SendIntentException e) {
                        // Unable to resolve, message user appropriately
                        Log.i(GOOGLEDRIVE_LOG_TAG, "something wrong");
                        google_drive_failed_handle();//failed
                        e.printStackTrace();
                    }
                } else {
                    GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(),activity, 0).show();
                }
            }
        };

        //It is auto-gen constructor
        connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                Log.i(GOOGLEDRIVE_LOG_TAG, "onConnected call back");
            }

            @Override
            public void onConnectionSuspended(int i) {
                Log.i(GOOGLEDRIVE_LOG_TAG, "onConnectionSuspended call back");
            }
        };

        //link google account (will appear account chooser)
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addScope(Drive.SCOPE_APPFOLDER)
                .addConnectionCallbacks(connectionCallbacks)
                .addOnConnectionFailedListener(onConnectionFailedListener)
                .build();

        mGoogleApiClient.connect();
    }

    private void google_drive_failed_handle()
    {
        Message failed_message = new Message();
        failed_message.arg1=-1;

        if (googledriver_mHandle!=null) {
            googledriver_mHandle.sendMessage(failed_message);
            googledriver_mHandle = null;
        }
    }
    private void google_drive_ok_handle()
    {
        Message message = new Message();
        message.arg1=1;
        if (googledriver_mHandle!=null)
        {
            googledriver_mHandle.sendMessage(message);
            googledriver_mHandle=null;
        }
    }
    //----------------//

}
