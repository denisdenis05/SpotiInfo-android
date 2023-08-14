package com.denisg.spotiinfo;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

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

    private static final String CLIENT_ID = "cda313e2c22d41fca0fd9d6a5d918190";
    private static final String CLIENT_SECRET = "7535b2784fe34a0ebce5764a770bc5a2";
    private static final String REDIRECT_URI = "spotiinfo://callback";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri callbackUri = getIntent().getData();

        if (callbackUri != null && "spotiinfo".equals(callbackUri.getScheme()) && !(String.valueOf(callbackUri).equals("spotiinfo://callback/?error=access_denied"))) {
            String authorizationCode = callbackUri.getQueryParameter("code");
            Log.e("YourTag", authorizationCode);

            if (authorizationCode != null) {
                exchangeAuthorizationCodeForToken(authorizationCode);
            }
        }
        finish();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
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

                    TokenManager.saveTokenDetailsToFile(getApplicationContext(),accessToken, refreshToken, expiresIn);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }


        }


}