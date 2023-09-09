package com.denisg.spotiinfo;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import org.apache.hc.core5.http.ParseException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;

public class AuthorizationActivity extends AppCompatActivity {

    private static final String CLIENT_ID = ApiConfig.CLIENT_ID;
    private static final String CLIENT_SECRET = ApiConfig.CLIENT_SECRET;
    private static final String REDIRECT_URI = ApiConfig.REDIRECT_URI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        Uri callbackUri = getIntent().getData();

        if (callbackUri != null && "spotiinfo".equals(callbackUri.getScheme()) && !(String.valueOf(callbackUri).equals(REDIRECT_URI+"/?error=access_denied"))) {
            String authorizationCode = callbackUri.getQueryParameter("code");
            Log.e("YourTag", authorizationCode);

            if (authorizationCode != null) {
                exchangeAuthorizationCodeForToken(authorizationCode);
            }
        }

    }

    private void exchangeAuthorizationCodeForToken(String authorizationCode) {
        new TokenExchangeTask().execute(authorizationCode);
    }
        private class TokenExchangeTask extends AsyncTask<String, Void, Void> {
            @Override
            protected Void doInBackground(String... params) {
                String authorizationCode = params[0];

                SpotifyApi spotifyApi = new SpotifyApi.Builder()
                        .setClientId(CLIENT_ID)
                        .setClientSecret(CLIENT_SECRET)
                        .setRedirectUri(URI.create(REDIRECT_URI))
                        .build();

                AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(authorizationCode).build();
                try {
                    AuthorizationCodeCredentials credentials = authorizationCodeRequest.execute();

                    String accessToken = credentials.getAccessToken();
                    String refreshToken = credentials.getRefreshToken();
                    int expiresIn = credentials.getExpiresIn();
                    Log.e("testtoken1", accessToken);

                    TokenManager.saveTokenDetailsToFile(getApplicationContext(),accessToken, refreshToken, expiresIn);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finish();
                Intent intent = new Intent(AuthorizationActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return null;
            }


        }


}