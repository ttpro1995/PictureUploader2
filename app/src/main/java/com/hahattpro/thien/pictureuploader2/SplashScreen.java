package com.hahattpro.thien.pictureuploader2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

//Use it as laucher activity, because MainActivity not working probably when use google drive. (just feel it)
public class SplashScreen extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        Intent intent = new Intent(SplashScreen.this,MainActivity.class);
        startActivity(intent);
    }


}
