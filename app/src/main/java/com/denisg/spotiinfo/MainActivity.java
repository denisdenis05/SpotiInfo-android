package com.denisg.spotiinfo;

import com.denisg.spotiinfo.TokenManager;
import com.squareup.picasso.Picasso;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.apache.hc.core5.http.ParseException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.requests.data.personalization.simplified.GetUsersTopArtistsRequest;
import se.michaelthelin.spotify.requests.data.personalization.simplified.GetUsersTopTracksRequest;

public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "cda313e2c22d41fca0fd9d6a5d918190";
    private static final String CLIENT_SECRET = "7535b2784fe34a0ebce5764a770bc5a2";
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

                long expiresIn = tokenDetails.getLong("expires_in");
                long tokenAcquiredTime = tokenDetails.getLong("token_acquired_time"); // Save the token acquisition time when you first acquire the token

                long currentTimeMillis = System.currentTimeMillis();
                long expirationTimeMillis = tokenAcquiredTime + (expiresIn * 1000); // Convert expiresIn seconds to milliseconds

                if (currentTimeMillis < expirationTimeMillis) {
                    // Token is valid
                    Button logoff = findViewById(R.id.logout_button);
                    logoff.setText("Log Out");
                    Button auth = findViewById(R.id.authorize_button);
                    auth.setVisibility(View.GONE);
                    logged = true;
                }
                else {
                    Log.e("ceva","TOKEN EXPIRAT");
                    String refreshToken = tokenDetails.getString("refresh_token");
                    //refreshAccessToken(refreshToken);
                    //temp
                    File pd = getApplicationContext().getFilesDir();

                    String fileName = "token_details.json";
                    try {
                        File uriFile = new File(pd, fileName);

                        if (uriFile.exists())
                            uriFile.delete();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    //temp
                }
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


    private void refreshAccessToken(String refreshToken) {
    SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .build();

    AuthorizationCodeRefreshRequest refreshRequest = spotifyApi.authorizationCodeRefresh()
            .refresh_token(refreshToken)
            .build();

    try {
        AuthorizationCodeCredentials refreshedCredentials = refreshRequest.execute();

        String newAccessToken = refreshedCredentials.getAccessToken();
        int newExpiresIn = refreshedCredentials.getExpiresIn();

        // Update the saved token details with the new access token and expiration time
        TokenManager.saveTokenDetailsToFile(this,newAccessToken, refreshToken, newExpiresIn);
        accessToken = newAccessToken;


    } catch (IOException | SpotifyWebApiException | ParseException e) {
        e.printStackTrace();
    }
}

    public void TopArtists(View view)
    {
        new TopArtistsTask().execute();
    }
    public void TopTracks(View view)
    {
        new TopTracksTask().execute();
    }

    private class TopArtistsTask extends AsyncTask<Void, Void, List<Artist>> {

        @Override
        protected List<Artist> doInBackground(Void... voids) {
            SpotifyApi spotifyApi = new SpotifyApi.Builder()
                    .setAccessToken(accessToken)
                    .build();
            int limit = 50; // Start with a reasonable limit
            int currentPage = 1;
            List<Artist> allArtists = new ArrayList<>();

            do {
                GetUsersTopArtistsRequest getTopArtistsRequest = spotifyApi.getUsersTopArtists()
                        .time_range("long_term")
                        .limit(limit)
                        .offset((currentPage - 1) * limit) // Adjust offset based on current page
                        .build();

                try {
                    Paging<Artist> artistPaging = getTopArtistsRequest.execute();
                    List<Artist> artists = Arrays.asList(artistPaging.getItems());

                    if (artists.isEmpty()) {
                        break; // No more artists to retrieve
                    }

                    allArtists.addAll(artists);
                    currentPage++;
                } catch (SpotifyWebApiException | IOException | ParseException e) {
                    e.printStackTrace();
                    // Continue the loop whatsoever
                }
            } while (true);

            System.out.println("Total artists listened to: " + allArtists.size());
            return allArtists;

        }

        @Override
        protected void onPostExecute(List<Artist> artists) {
            if (artists != null) {
                HorizontalScrollView scrollView = findViewById(R.id.scrollViewArtists);
                LinearLayout scrollViewContent = findViewById(R.id.scrollViewContentArtists);

                for (Artist artist : artists) {
                    LinearLayout artistLayout = new LinearLayout(MainActivity.this);
                    artistLayout.setLayoutParams(new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1));
                    artistLayout.setOrientation(LinearLayout.VERTICAL);

                    ImageView artistImage = new ImageView(MainActivity.this);
                    artistImage.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, // Set image width to match parent
                            200));
                    if (artist.getImages() != null && artist.getImages().length > 0) {
                        String imageUrl = artist.getImages()[0].getUrl(); // Get the first image URL
                        // Use a library like Picasso or Glide to load the image into the ImageView
                        Picasso.get().load(imageUrl).into(artistImage);
                    }

                    TextView artistName = new TextView(MainActivity.this);
                    artistName.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, // Set width to match parent
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    artistName.setText(artist.getName());
                    artistName.setGravity(Gravity.CENTER);

                    artistLayout.addView(artistImage);
                    artistLayout.addView(artistName);

                    scrollViewContent.addView(artistLayout);

                }
            } else {
                System.out.println("Error fetching top artists");
            }
        }
    }
    private class TopTracksTask extends AsyncTask<Void, Void, List<Track>> {

        @Override
        protected List<Track> doInBackground(Void... voids) {
            SpotifyApi spotifyApi = new SpotifyApi.Builder()
                    .setAccessToken(accessToken)
                    .build();

            GetUsersTopTracksRequest getTopTracksRequest = spotifyApi.getUsersTopTracks()
                    .time_range("short_term") // You can change the time range
                    .limit(10) // You can change the number of tracks
                    .build();

            try {
                Paging<Track> trackPaging = getTopTracksRequest.execute();
                List<Track> tracks = Arrays.asList(trackPaging.getItems());
                return tracks;
            } catch (SpotifyWebApiException | IOException | ParseException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Track> tracks) {
            if (tracks != null) {
                for (Track track : tracks) {
                    System.out.println("Track Name: " + track.getName());
                }
            } else {
                System.out.println("Error fetching top tracks");
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