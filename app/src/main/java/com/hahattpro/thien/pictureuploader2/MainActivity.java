package com.hahattpro.thien.pictureuploader2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import com.PiksalStudio.thien.clouduploader.CloudUploader;
import com.hahattpro.thien.pictureuploader2.StaticField.AppIDandSecret;
import com.hahattpro.thien.pictureuploader2.StaticField.Dir;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;


public class MainActivity extends ActionBarActivity {

    //LOGtag
    private String CAMERA_LOG_TAG ="CAMERA";
    final String GOOGLEDRIVE_LOG_TAG ="Google Drive";
    final String DROPBOX_LOG_TAG ="Dropbox";


    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    Uri pic_uri = null;
    //view
    ImageView imageView;
    Button buttonSelect;
    Button buttonUpload;
    Button buttonCamera;
    TextView textStatus;
    // flag
    boolean error_Dropbox = false;
    private String LOG_TAG = MainActivity.class.getSimpleName();
    private String Dropbox_token = null;

    private int SELECT_PICTURE = 1;//select picture request code
    private int CAPTURE_IMAGE= 2;//
    private int CURRENT_REQUEST=0;
    final int GOOGLE_DRIVE_LOGIN_REQUEST_CODE = 100;


    //handler
    Handler dropbox_handler = null ;
    Handler google_drive_handler = null;

    //DIR of image
    String IMAGE_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
    int IMAGE_NUM=0;

    //Prepare to upload
    InputStream inputStream = null;
    String File_Name=null;//name with extension
    Long File_length=null;
    String FOLDER = Dir.PICTURE_DIR;


    CloudUploader cloudUploader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Log.i(LOG_TAG,"Keep calm and meow on");

        imageView = (ImageView) findViewById(R.id.imageview1);
        buttonSelect = (Button) findViewById(R.id.button_select);
        buttonUpload = (Button) findViewById(R.id.button_upload);
        buttonCamera = (Button) findViewById(R.id.button_camera);


        textStatus = (TextView) findViewById(R.id.textStatus);
        buttonUpload.setEnabled(false);

        //init prefs which is used to store access token
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = prefs.edit();

        //handle
        dropbox_handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.arg1==1) {
                    Log.i("DROPBOX HANDLER", "Upload ok");
                    //do something when upload finished
                }
                if (msg.arg1==-1) {
                    Log.i("DROPBOX HANDLER", "Upload failed");
                    //do something when upload failed
                }
            }
        };
        google_drive_handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.arg1==1) {
                    Log.i("GOOGLE DRIVE HANDLER", "Upload ok");
                    //do something when upload finished
                }
                if (msg.arg1==-1) {
                    Log.i("GOOGLE DRIVE", "Upload failed");
                    //do something when upload failed
                }
            }
        };


        cloudUploader = new CloudUploader(MainActivity.this,Dir.PICTURE_DIR, AppIDandSecret.AppID_Dropbox,AppIDandSecret.Secret_Dropbox);

        buttonSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectPic();
            }
        });

        buttonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //new LoginDropboxAndUpload().execute();


                cloudUploader.UploadFileDropbox(File_Name, inputStream, File_length,dropbox_handler);

                //new UploadGoogleDrive().execute();
                cloudUploader.UploadFileGoogleDrive(File_Name,inputStream,File_length,google_drive_handler);


            }
        });

        buttonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
            }
        });

    }


    //open chooser to chose picture
    private void selectPic() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
    }

    private void takePhoto(){
        Intent intent = new Intent();
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);


        File image ;
        image = new File(IMAGE_DIR+"/"+"image"+IMAGE_NUM+".JPG");
        while (image.exists())
        {
            IMAGE_NUM++;
            image = new File(IMAGE_DIR+"/"+"image"+IMAGE_NUM+".JPG");
        }

         pic_uri = Uri.fromFile(image);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, pic_uri);

        startActivityForResult(intent, CAPTURE_IMAGE);

    }

    //go to activity where you will login, get access token
    private void GoToAccountManager() {
        /*Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);*/

        cloudUploader.StartLoginActivity();
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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
        if (id == R.id.action_account_manager) {
            GoToAccountManager();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_PICTURE) {
            //show picture you  selected
            if (data != null) try {
                pic_uri = data.getData();
                imageView.setImageURI(pic_uri);
                if (pic_uri != null)
                    buttonUpload.setEnabled(true);
                CURRENT_REQUEST = SELECT_PICTURE;
                InputStream is = getContentResolver().openInputStream(pic_uri);
                Log.i(LOG_TAG, "File name = " + getFileName(pic_uri));
                Log.i(LOG_TAG, "File size = " + getFileSize(pic_uri));


                //TODO: pass FileName, FileLength,inputStream to upload method
                File_Name = getFileName(pic_uri);
                File_length = getFileSize(pic_uri);
                inputStream = is;
                FOLDER = Dir.PICTURE_DIR;

            }
            catch (Exception e){e.printStackTrace();}
        }

        if (requestCode == CAPTURE_IMAGE) {
            if (resultCode == RESULT_OK) try {
                // Image captured and saved to fileUri specified in the Intent
                Log.i(CAMERA_LOG_TAG,"CAMERA Path = "+ pic_uri.getPath());
                imageView.setImageURI(pic_uri);
                if (pic_uri != null)
                    buttonUpload.setEnabled(true);
                CURRENT_REQUEST = CAPTURE_IMAGE;
                File myFile = new File(pic_uri.getPath());
                InputStream is = new FileInputStream(myFile);

                //TODO: pass FileName, FileLength,inputStream to upload method
                File_Name = myFile.getName();
                File_length = myFile.length();
                inputStream = is;

            } catch (Exception e){e.printStackTrace();}
            else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture
            } else {
                // Image capture failed, advise user
            }
        }

    }

    ///////>>>>>>HELPER FUNCTION<<<<<<///////////////////

    //get file name from uri
    private String getFileName(Uri contentURI) {
        //https://developer.android.com/training/secure-file-sharing/retrieve-info.html
        Uri returnUri = pic_uri;
        Cursor returnCursor =
                getContentResolver().query(returnUri, null, null, null, null);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        String file_name = returnCursor.getString(nameIndex);
        returnCursor.close();
        return file_name;
    }

    //get file size from uri
    private long getFileSize(Uri contentURI) {
        //https://developer.android.com/training/secure-file-sharing/retrieve-info.html
        Uri returnUri = pic_uri;
        Cursor returnCursor =
                getContentResolver().query(returnUri, null, null, null, null);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        long file_size = returnCursor.getLong(sizeIndex);
        returnCursor.close();
        return file_size;
    }


}

//TODOdone: FIX upload task so it take inputstream, file name, file length, FOLDER   to upload
//TODO: move to new class
//TODO: Separate into new class