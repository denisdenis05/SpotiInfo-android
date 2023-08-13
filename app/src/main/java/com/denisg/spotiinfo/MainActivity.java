package com.denisg.spotiinfo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;

public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "cda313e2c22d41fca0fd9d6a5d918190";
    private static final String REDIRECT_URI = "spotiinfo://callback";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        start();
    }

    private void start()
    {
        String uriString="none";
        File persistentDir = getApplicationContext().getFilesDir();
        File uriFile = new File(persistentDir, "callback_uri.file");
        try {
            if (uriFile.exists()) {
                // Read the URI from the file
                BufferedReader reader = new BufferedReader(new FileReader(uriFile));
                uriString = reader.readLine();
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(uriString.equals("none"))
        {
            Button auth = findViewById(R.id.authorize_button);
            auth.setText("Login with Spotify");
            Button logoff = findViewById(R.id.logout_button);
            logoff.setVisibility(View.GONE);

        }
        else
        {
            Button logoff = findViewById(R.id.logout_button);
            logoff.setText("Log Out");
            Button auth = findViewById(R.id.authorize_button);
            auth.setVisibility(View.GONE);
        }
    }




    public void SpotiAuth(View view)
    {
        SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setClientId(CLIENT_ID)
                .setRedirectUri(URI.create(REDIRECT_URI))
                .build();

        AuthorizationCodeUriRequest authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
                .scope("user-read-playback-state,user-modify-playback-state")
                .show_dialog(true)
                .build();

        URI authorizationUri = authorizationCodeUriRequest.execute();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizationUri.toString()));
        startActivity(intent);
    }

    public void SpotiLogOff(View view)
    {
        File persistentDir = getApplicationContext().getFilesDir();

        String fileName = "callback_uri.file";
        try {
            File uriFile = new File(persistentDir, fileName);

            if (uriFile.exists())
                uriFile.delete();

        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}