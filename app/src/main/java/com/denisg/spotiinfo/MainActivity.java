package com.denisg.spotiinfo;

import com.squareup.picasso.Picasso;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PagingCursorbased;
import se.michaelthelin.spotify.model_objects.specification.PlayHistory;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.requests.data.personalization.simplified.GetUsersTopArtistsRequest;
import se.michaelthelin.spotify.requests.data.personalization.simplified.GetUsersTopTracksRequest;


import jp.wasabeef.picasso.transformations.RoundedCornersTransformation;
import se.michaelthelin.spotify.requests.data.player.GetCurrentUsersRecentlyPlayedTracksRequest;
import se.michaelthelin.spotify.requests.data.tracks.GetTrackRequest;


public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = ApiConfig.CLIENT_ID;
    private static final String CLIENT_SECRET = ApiConfig.CLIENT_SECRET;
    private static final String REDIRECT_URI = ApiConfig.REDIRECT_URI;
    private boolean logged;
    boolean[] Loaded = new boolean[4];
    private String accessToken = "none";
    private int control=1;

    List<Artist> ShortTermArtists = new ArrayList<>(),
            MediumTermArtists = new ArrayList<>(),
            LongTermArtists = new ArrayList<>();

    List<Track> ShortTermTracks = new ArrayList<>(),
            MediumTermTracks = new ArrayList<>(),
            LongTermTracks = new ArrayList<>();

    PlayHistory[] playHistoryItems= new PlayHistory[0];
    Track[] trackArray= new Track[0];

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
                    Log.e("test","STARTED");
                    LinearLayout auth = findViewById(R.id.authpanel);
                    LinearLayout logoff = findViewById(R.id.mainpanel);
                    logoff.setVisibility(View.GONE);
                    auth.setVisibility(View.GONE);

                    new LoadTop().execute();
                    new MainPageTop().execute();
                    //new TopArtistsTask().execute();
                    //new TopTracksTask().execute();
                    new startloading().execute();
                    Log.e("test","FINISHED");

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
            Arrays.fill(Loaded, false);
            LinearLayout logoff = findViewById(R.id.mainpanel);
            FrameLayout loading = findViewById(R.id.loadingpanel);
            logoff.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);
            logged = false;
        }
    }

    public class startloading extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.e("test","STARTED2");


        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.e("test","DOINBACKGROUNDSTARTED2");

            while(loaded()==false) {
                try {
                    Thread.sleep(200); // Sleep for 200 milliseconds (0.2 seconds)
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
                return null;
        }

        boolean loaded()
        {
            for(int i=0; i< Loaded.length;i++)
            {
                if(Loaded[i]==false)
                    return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            FrameLayout loading = findViewById(R.id.loadingpanel);
            LinearLayout logoff = findViewById(R.id.mainpanel);
            logoff.setVisibility(View.VISIBLE);
            loading.setVisibility(View.GONE);
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

    private class LoadTop extends AsyncTask<Void, Void, Void> {


        void loadRecentTracks(){
            SpotifyApi spotifyApi = new SpotifyApi.Builder()
                    .setAccessToken(accessToken)
                    .build();

            try {
                // Make a request to get the user's recently played tracks
                GetCurrentUsersRecentlyPlayedTracksRequest recentlyPlayedTracksRequest = spotifyApi.getCurrentUsersRecentlyPlayedTracks()
                        .limit(40)
                        .build();

                PagingCursorbased<PlayHistory> playHistoryPaging = recentlyPlayedTracksRequest.execute();
                PlayHistory[] localplayHistoryItems = playHistoryPaging.getItems();
                System.out.println("LENGTH: " + localplayHistoryItems.length);

                playHistoryItems = new PlayHistory[localplayHistoryItems.length];
                System.arraycopy(localplayHistoryItems, 0, playHistoryItems, 0, localplayHistoryItems.length);


                for (PlayHistory playHistory : playHistoryItems) {
                    System.out.println("Track: " + playHistory.getTrack().getName());
                    System.out.println("Artist: " + playHistory.getTrack().getArtists()[0].getName());
                    System.out.println("Played at: " + playHistory.getPlayedAt());
                    System.out.println();
                }
            } catch (SpotifyWebApiException | IOException | ParseException e) {
                e.printStackTrace();
            }

            trackArray = new Track[playHistoryItems.length];


            for (int i = 0; i < playHistoryItems.length; i++) {
                PlayHistory playHistory = playHistoryItems[i];
                TrackSimplified trackSimplified = playHistory.getTrack();
                String trackId = trackSimplified.getId();

                GetTrackRequest getTrackRequest = spotifyApi.getTrack(trackId).build();

                try {
                    Track track = getTrackRequest.execute();
                    trackArray[i] = track;
                } catch (SpotifyWebApiException | IOException | ParseException e) {
                    e.printStackTrace();
                    // Handle the exception appropriately.
                }
            }




        }

        void loadTracks(String term)
        {
            SpotifyApi spotifyApi = new SpotifyApi.Builder()
                    .setAccessToken(accessToken)
                    .build();

            GetUsersTopTracksRequest getTopTracksRequest = spotifyApi.getUsersTopTracks()
                    .time_range(term)
                    .limit(50)
                    .build();

            try {
                Paging<Track> trackPaging = getTopTracksRequest.execute();
                List<Track> tracks = Arrays.asList(trackPaging.getItems());
                System.out.println("Total " + term + " tracks listened to: " + tracks.size());
                if(term.equals("short_term"))
                    ShortTermTracks.addAll(tracks);
                else if(term.equals("medium_term"))
                    MediumTermTracks.addAll(tracks);
                else if(term.equals("long_term"))
                    LongTermTracks.addAll(tracks);
            } catch (SpotifyWebApiException | IOException | ParseException e) {
                e.printStackTrace();
            }
        }

        void loadArtists(String term)
        {
            SpotifyApi spotifyApi = new SpotifyApi.Builder()
                    .setAccessToken(accessToken)
                    .build();

            GetUsersTopArtistsRequest getTopArtistsRequest = spotifyApi.getUsersTopArtists()
                    .time_range(term)
                    .limit(50)
                    .build();

            try {
                Paging<Artist> trackPaging = getTopArtistsRequest.execute();
                List<Artist> artists = Arrays.asList(trackPaging.getItems());
                System.out.println("Total " + term + " artists listened to: " + artists.size());
                if(term.equals("short_term"))
                    ShortTermArtists.addAll(artists);
                else if(term.equals("medium_term"))
                    MediumTermArtists.addAll(artists);
                else if(term.equals("long_term"))
                    LongTermArtists.addAll(artists);
            } catch (SpotifyWebApiException | IOException | ParseException e) {
                e.printStackTrace();
            }
        }

        protected Void doInBackground(Void... voids) {
            Log.e("test","DOINBACKGROUNDSTARTED2");
            loadArtists("short_term");
            loadArtists("medium_term");
            loadArtists("long_term");
            loadTracks("short_term");
            loadTracks("medium_term");
            loadTracks("long_term");
            loadRecentTracks();
            Loaded[0]=true;
            Loaded[1]=true;
            return null;
        }
    }



    private class MainPageTop extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.e("test","STARTED3");

        }
        @Override
        protected Void doInBackground(Void... voids) {
            while(Loaded[0]==false || Loaded[1]==false) {
                try {
                    Thread.sleep(200); // Sleep for 200 milliseconds (0.2 seconds)
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            //ARTISTS
            if (ShortTermArtists != null) {
                HorizontalScrollView scrollView = findViewById(R.id.scrollViewArtists);
                LinearLayout scrollViewContent = findViewById(R.id.scrollViewContentArtists);

                int i=1;
                for (Artist artist : ShortTermArtists) {
                    if (i >= 11)
                        break;
                    else {
                        LinearLayout artistLayout = new LinearLayout(MainActivity.this);
                        artistLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1));
                        artistLayout.setOrientation(LinearLayout.VERTICAL);

                        ImageView artistImage = new ImageView(MainActivity.this);
                        artistImage.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, // Set image width to match parent
                                300));
                        if (artist.getImages() != null && artist.getImages().length > 0) {
                            String imageUrl = artist.getImages()[0].getUrl();

                            Picasso.get()
                                    .load(imageUrl)
                                    .transform(new RoundedCornersTransformation(700, 10))
                                    .into(artistImage);


                        }

                        TextView artistName = new TextView(MainActivity.this);
                        artistName.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, // Set width to match parent
                                LinearLayout.LayoutParams.WRAP_CONTENT));
                        artistName.setText("\n" + String.valueOf(i) + "." + artist.getName());
                        artistName.setGravity(Gravity.CENTER);
                        artistName.setTextColor(Color.WHITE);
                        artistLayout.addView(artistImage);
                        artistLayout.addView(artistName);

                        i = i + 1;
                        scrollViewContent.addView(artistLayout);
                    }
                }

                Loaded[2]=true;
            } else {
                System.out.println("Error fetching top artists");
            }


            //TRACKS
            if (ShortTermTracks != null) {
                HorizontalScrollView scrollView = findViewById(R.id.scrollViewTracks);
                LinearLayout scrollViewContent = findViewById(R.id.scrollViewContentTracks);

                int i = 1;
                for (Track track : ShortTermTracks) {
                    if (i >= 11)
                        break;
                    else {
                        LinearLayout artistLayout = new LinearLayout(MainActivity.this);
                        artistLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1));
                        artistLayout.setOrientation(LinearLayout.VERTICAL);

                        ImageView artistImage = new ImageView(MainActivity.this);
                        artistImage.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, // Set image width to match parent
                                300));

                        AlbumSimplified album = track.getAlbum();
                        if (album.getImages() != null && album.getImages().length > 0) {
                            String imageUrl = album.getImages()[0].getUrl();

                            Picasso.get()
                                    .load(imageUrl)
                                    .transform(new RoundedCornersTransformation(100, 10))
                                    .into(artistImage);
                        }

                        TextView artistName = new TextView(MainActivity.this);
                        artistName.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, // Set width to match parent
                                LinearLayout.LayoutParams.WRAP_CONTENT));
                        artistName.setText("\n" + String.valueOf(i) + "." + track.getName());
                        artistName.setMaxLines(2);
                        artistName.setEllipsize(TextUtils.TruncateAt.END);
                        artistName.setGravity(Gravity.CENTER);
                        artistName.setTextColor(Color.WHITE);
                        artistLayout.addView(artistImage);
                        artistLayout.addView(artistName);

                        i = i + 1;
                        scrollViewContent.addView(artistLayout);
                    }
                }
                Loaded[3] = true;
            } else {
                System.out.println("Error fetching top artists");
            }

            //RECENT TRACKS

            ImageView lasttrackimg = findViewById(R.id.LastSongImage);
            TextView lasttracktitle = findViewById(R.id.LastSongTitle);
            TextView lasttrackartist = findViewById(R.id.LastSongArtist);

            Track lasttrack=trackArray[0];
            lasttracktitle.setText(lasttrack.getName());
            lasttrackartist.setText(lasttrack.getArtists()[0].getName());

            AlbumSimplified album = lasttrack.getAlbum();
            if (album.getImages() != null && album.getImages().length > 0) {
                String imageUrl = album.getImages()[0].getUrl();

                Picasso.get()
                        .load(imageUrl)
                        .transform(new RoundedCornersTransformation(100, 10))
                        .into(lasttrackimg);
            }




/*
System.out.println("Track: " + track.getName());
                    System.out.println("Artist: " + track.getArtists()[0].getName());
                    System.out.println("Played at: " + playHistory.getPlayedAt());
                    System.out.println();
 */

        }
    }


    public void SpotiAuth(View view)
    {
        SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setClientId(CLIENT_ID)
                .setRedirectUri(URI.create(REDIRECT_URI))
                .build();

        AuthorizationCodeUriRequest authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
                .scope("user-read-private,user-read-email,user-top-read,user-read-recently-played")
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