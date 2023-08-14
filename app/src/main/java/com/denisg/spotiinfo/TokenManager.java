package com.denisg.spotiinfo;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;


public class TokenManager extends AppCompatActivity {


    public static void saveTokenDetailsToFile(Context context, String accessToken, String refreshToken, int expiresIn) {
        try {
            File persistentDir = context.getApplicationContext().getFilesDir();

            File tokenFile = new File(persistentDir, "token_details.json");

            JSONObject tokenDetails = new JSONObject();
            tokenDetails.put("access_token", accessToken);
            tokenDetails.put("refresh_token", refreshToken);
            tokenDetails.put("expires_in", expiresIn);
            tokenDetails.put("token_acquired_time", System.currentTimeMillis());

            FileWriter writer = new FileWriter(tokenFile);
            writer.write(tokenDetails.toString());
            writer.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

}
