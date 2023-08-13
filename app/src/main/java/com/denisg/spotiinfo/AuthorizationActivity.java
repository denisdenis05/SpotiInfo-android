package com.denisg.spotiinfo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class AuthorizationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri callbackUri = getIntent().getData();
        saveUriToFile(callbackUri);
        finish();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void saveUriToFile(Uri uri) {
        try {
            // Get the persistent data directory for your app
            File persistentDir = getApplicationContext().getFilesDir();

            // Create a file to save the URI (you can change the file name as needed)
            File uriFile = new File(persistentDir, "callback_uri.file");

            // Write the URI to the file
            FileWriter writer = new FileWriter(uriFile);
            writer.write(uri.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}


