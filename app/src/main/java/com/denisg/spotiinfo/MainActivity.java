package com.denisg.spotiinfo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.apache.hc.core5.http.ParseException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.requests.data.personalization.simplified.GetUsersTopArtistsRequest;

public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "cda313e2c22d41fca0fd9d6a5d918190";
    private static final String REDIRECT_URI = "spotiinfo://callback";
    private boolean logged;
    private String accessToken = "none";

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

    private void start() {
        File persistentDir = getApplicationContext().getFilesDir();
        File tokenFile = new File(persistentDir, "token_details.json");

        if (tokenFile.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(tokenFile));
                StringBuilder jsonString = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonString.append(line);
                }
                reader.close();

                JSONObject tokenDetails = new JSONObject(jsonString.toString());
                accessToken = tokenDetails.getString("access_token");
                Button logoff = findViewById(R.id.logout_button);
                logoff.setText("Log Out");
                Button auth = findViewById(R.id.authorize_button);
                auth.setVisibility(View.GONE);
                logged = true;

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        } else {
            Button auth = findViewById(R.id.authorize_button);
            auth.setText("Login with Spotify");
            Button logoff = findViewById(R.id.logout_button);
            logoff.setVisibility(View.GONE);
            logged = false;
        }
    }

    public void TopArtists(View view)
    {
        new TopArtistsTask().execute();
    }

    private class TopArtistsTask extends AsyncTask<Void, Void, List<Artist>> {

        @Override
        protected List<Artist> doInBackground(Void... voids) {
            SpotifyApi spotifyApi = new SpotifyApi.Builder()
                    .setAccessToken(accessToken)
                    .build();
            GetUsersTopArtistsRequest getTopArtistsRequest = spotifyApi.getUsersTopArtists()
                    .time_range("short_term")
                    .limit(10)
                    .build();

            try {
                Paging<Artist> artistPaging = getTopArtistsRequest.execute();
                List<Artist> artists = Arrays.asList(artistPaging.getItems());
                return artists;
            } catch (SpotifyWebApiException | IOException | ParseException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Artist> artists) {
            if (artists != null) {
                for (Artist artist : artists) {
                    System.out.println("Artist Name: " + artist.getName());
                }
            } else {
                System.out.println("Error fetching top artists");
            }
        }
    }



    public void SpotiAuth(View view)
    {
        SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setClientId(CLIENT_ID)
                .setRedirectUri(URI.create(REDIRECT_URI))
                .build();

        AuthorizationCodeUriRequest authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
                .scope("user-read-private,user-read-email,user-top-read")
                .show_dialog(true)
                .build();

        URI authorizationUri = authorizationCodeUriRequest.execute();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizationUri.toString()));
        startActivity(intent);
    }

    public void SpotiLogOff(View view)
    {
        File persistentDir = getApplicationContext().getFilesDir();

        String fileName = "token_details.json";
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