package com.PiksalStudio.thien.clouduploader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
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


    //google drive
    GoogleApiClient mGoogleApiClient;
    GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener;
    GoogleApiClient.ConnectionCallbacks connectionCallbacks;
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

    public void UploadFileDropbox(String Name, InputStream is, Long Length ){
            this.File_Name = Name;
            this.File_length = Length;
            this.inputStream = is;

        new LoginDropboxAndUpload().execute();
    }

    public void UploadFileGoogleDrive(String Name, InputStream is, Long Length ){
        this.File_Name = Name;
        this.File_length = Length;
        this.inputStream = is;

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

                Log.e(DROPBOX_LOG_TAG,tmp);
            }

        }
    }

    //Upload select picture (which is shown on image view to dropbox
    private class UploadPicture_Dropbox extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i(DROPBOX_LOG_TAG,"Start uploading");
        }

        @Override
        protected Void doInBackground(Void... voids) {

           /* if (CURRENT_REQUEST == SELECT_PICTURE)
            try {
                //File file = new File(getRealPathFromURI(pic_uri));
                InputStream is = getContentResolver().openInputStream(pic_uri);

                Log.i(LOG_TAG, "File name = " + getFileName(pic_uri));
                Log.i(LOG_TAG, "File size = " + getFileSize(pic_uri));

                //upload to dropbox
                Dropbox_mApi.putFile(Dir.PICTURE_DIR + getFileName(pic_uri)
                        , is
                        , getFileSize(pic_uri)
                        , null
                        , null);
            } catch (FileNotFoundException e) {
                Log.e(LOG_TAG, "FILE_NOT_FOUND");
                error_Dropbox = true;
            } catch (DropboxException e) {
                Log.e(LOG_TAG, "DROPBOX_ERROR");
                error_Dropbox = true;
            } catch (NullPointerException e) {
                Log.e(LOG_TAG, "no picture is selected");
                e.printStackTrace();
                error_Dropbox = true;
            }


            if (CURRENT_REQUEST == CAPTURE_IMAGE)
                try{
                    File myFile = new File(pic_uri.getPath());
                    InputStream is = new FileInputStream(myFile);
                    Dropbox_mApi.putFile(Dir.PICTURE_DIR +myFile.getName()
                            , is
                            , myFile.length()
                            , null
                            , null);
                }
                catch (FileNotFoundException er){er.printStackTrace();}
                catch (DropboxException er){ er.printStackTrace();}
*/
            /////////////////////////
            try {

                CopyInputStream copyInputStream = new CopyInputStream(inputStream);//init copyInputStream
                inputStream = copyInputStream.getCopy();//restore source inputstream

                Log.d(DROPBOX_LOG_TAG,"File Name = "+File_Name);
                Log.d(DROPBOX_LOG_TAG, "File length = " + File_length);


                InputStream tmpis = copyInputStream.getCopy();//create tmp inputstream
                Dropbox_mApi.putFile(File_Name // path in drop box
                        , tmpis //input stream
                        , File_length //file.length()
                        , null
                        , null);


            }
            catch (DropboxException e){ e.printStackTrace();
                error_Dropbox = true;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.i(DROPBOX_LOG_TAG, "Dropbox Upload Complete");




        }
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
            LoginGoogleDrive();//start connecting to google api
            try {//wait for it
                while (mGoogleApiClient==null||!mGoogleApiClient.isConnected())
                    Thread.sleep(100);
            }
            catch (InterruptedException e){}




/*
            if (CURRENT_REQUEST == SELECT_PICTURE)
                try {
                    //File file = new File(getRealPathFromURI(pic_uri));
                    InputStream is = getContentResolver().openInputStream(pic_uri);

                    Log.i(LOG_TAG, "File name = " + getFileName(pic_uri));
                    Log.i(LOG_TAG, "File size = " + getFileSize(pic_uri));

                    SaveFileToDrive(getFileName(pic_uri),is);

                } catch (FileNotFoundException e) {e.printStackTrace();}
                catch (Exception e) {e.printStackTrace();}

            if (CURRENT_REQUEST == CAPTURE_IMAGE)
                try{
                    File myFile = new File(pic_uri.getPath());
                    InputStream is = new FileInputStream(myFile);
                    SaveFileToDrive(myFile.getName(),is);
                }
                catch (FileNotFoundException er){er.printStackTrace();}
                catch (Exception e){e.printStackTrace();}*/

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
                            return;
                        }
                        Log.i(GOOGLEDRIVE_LOG_TAG,"Created a file  "
                                + result.getDriveFile().getDriveId());
                    }
                };

        //Create new folder
        final ResultCallback<DriveFolder.DriveFolderResult> callback = new ResultCallback<DriveFolder.DriveFolderResult>() {
            @Override
            public void onResult(DriveFolder.DriveFolderResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.e(GOOGLEDRIVE_LOG_TAG,"Error while trying to create the folder");
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
    private void LoginGoogleDrive() {

        //It is auto-gen constructor
        onConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(ConnectionResult connectionResult) {
                Log.i(GOOGLEDRIVE_LOG_TAG, "onConnectionFailed");
                if (connectionResult.hasResolution()) {
                    try {
                        connectionResult.startResolutionForResult(activity, GOOGLE_DRIVE_LOGIN_REQUEST_CODE);
                    } catch (IntentSender.SendIntentException e) {
                        // Unable to resolve, message user appropriately
                        Log.i(GOOGLEDRIVE_LOG_TAG, "something wrong");
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


    //----------------//

}
