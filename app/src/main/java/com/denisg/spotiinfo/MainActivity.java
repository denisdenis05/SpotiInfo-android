package com.denisg.spotiinfo;


import com.squareup.picasso.Picasso;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.WindowCompat;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
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
import java.util.Collections;
import java.util.Date;
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
    private String accessToken = "none", refreshToken="none";
    private int toptype=0;

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

    void init()
    {
        FrameLayout loadingpanel = findViewById(R.id.loadingpanel);
        ForceHideEverythingExcept(loadingpanel);

        //init blank layouts
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenHeight = displayMetrics.heightPixels;
        int desiredHeight = (int) (0.08 * screenHeight);


        LinearLayout existingLinearLayout1 = findViewById(R.id.blanklayout1);
        LinearLayout.LayoutParams layoutParams1 = (LinearLayout.LayoutParams) existingLinearLayout1.getLayoutParams();
        layoutParams1.height = desiredHeight;

        existingLinearLayout1.setLayoutParams(layoutParams1);

        LinearLayout existingLinearLayout2 = findViewById(R.id.blanklayout2);
        LinearLayout.LayoutParams layoutParams2 = (LinearLayout.LayoutParams) existingLinearLayout2.getLayoutParams();
        layoutParams2.height = desiredHeight;

        existingLinearLayout2.setLayoutParams(layoutParams2);
    //init blank layouts finish
    }

    private void start() {
        File persistentDir = getApplicationContext().getFilesDir();
        File tokenFile = new File(persistentDir, "token_details.json");

        init();

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
                refreshToken = tokenDetails.getString("refresh_token");

                long expiresIn = tokenDetails.getLong("expires_in");
                long tokenAcquiredTime = tokenDetails.getLong("token_acquired_time"); // Save the token acquisition time when you first acquire the token

                long currentTimeMillis = System.currentTimeMillis();
                long expirationTimeMillis = tokenAcquiredTime + (expiresIn * 1000); // Convert expiresIn seconds to milliseconds

                if (currentTimeMillis < expirationTimeMillis) {
                    // Token is valid

                    Log.e("test","STARTED");

                    FrameLayout loadingpanel = findViewById(R.id.loadingpanel);
                    HideEverythingExcept(loadingpanel);

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
                    //new refreshAccessToken().execute();
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
            LinearLayout authpanel = findViewById(R.id.authpanel);
            HideEverythingExcept(authpanel);
            logged = false;
            final Button authorizeb = findViewById(R.id.authorize_button);
            Animations.fadeInAndBounce(authorizeb);

        }
    }

    public class refreshAccessToken extends AsyncTask<Void, Void, Void> {


        @Override
        protected Void doInBackground(Void... voids) {
            SpotifyApi spotifyApi = new SpotifyApi.Builder()
                    .setClientId(CLIENT_ID)
                    .setClientSecret(CLIENT_SECRET)
                    .setRefreshToken(refreshToken) // Use the stored refresh token
                    .build();

            AuthorizationCodeRefreshRequest refreshRequest = spotifyApi.authorizationCodeRefresh()
                    .build();

            AuthorizationCodeCredentials refreshedCredentials = null;
            try {
                refreshedCredentials = refreshRequest.execute();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SpotifyWebApiException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            String newAccessToken = refreshedCredentials.getAccessToken();
            String newRefreshToken = refreshedCredentials.getRefreshToken(); // It might not always change
            int newExpiresIn = refreshedCredentials.getExpiresIn();
            Log.e("testtoken2", newAccessToken);

            // Update the saved token details with the new access token and expiration time
            TokenManager.saveTokenDetailsToFile(getApplicationContext(), newAccessToken, newRefreshToken, newExpiresIn);
            finish();
            Intent intent = new Intent(MainActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

            return null;
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
            LinearLayout logoff = findViewById(R.id.mainpanel);
            HideEverythingExcept(logoff);
        }
    }


    void HideEverythingExcept(View view)
    {
        LinearLayout auth = findViewById(R.id.authpanel);
        LinearLayout mainpanel = findViewById(R.id.mainpanel);
        LinearLayout tops = findViewById(R.id.tops);
        LinearLayout lasttracks = findViewById(R.id.lasttracks);
        FrameLayout loading = findViewById(R.id.loadingpanel);
        Animations.FadeInOutViews(view, auth, mainpanel, tops,lasttracks, loading);
    }

    void ForceHideEverythingExcept(View view)
    {
        LinearLayout auth = findViewById(R.id.authpanel);
        LinearLayout mainpanel = findViewById(R.id.mainpanel);
        LinearLayout tops = findViewById(R.id.tops);
        LinearLayout lasttracks = findViewById(R.id.lasttracks);
        FrameLayout loading = findViewById(R.id.loadingpanel);
        Animations.ForceHideViews(view, auth, mainpanel, tops,lasttracks, loading);
    }


    private class LoadTop extends AsyncTask<Void, Void, Void> {


        void loadRecentTracks(){
            SpotifyApi spotifyApi = new SpotifyApi.Builder()
                    .setAccessToken(accessToken)
                    .build();



            int limit = 50;
            Date before = new Date();
            List<PlayHistory> playHistoryItemsTest = new ArrayList<>();

            try {
                while (true) {
                    GetCurrentUsersRecentlyPlayedTracksRequest recentlyPlayedTracksRequest = spotifyApi
                            .getCurrentUsersRecentlyPlayedTracks()
                            .limit(limit)
                            .before(before) // Set the "before" parameter for pagination
                            .build();

                    PagingCursorbased<PlayHistory> playHistoryPaging = recentlyPlayedTracksRequest.execute();
                    PlayHistory[] pagePlayHistoryItems  = playHistoryPaging.getItems();

                    if (pagePlayHistoryItems.length == 0) {
                        // No more tracks to retrieve, break out of the loop
                        break;
                    }

                    // Iterate through playHistoryItems to gather artist data
                    // ...

                    // Update the "before" parameter for the next request

                    Collections.addAll(playHistoryItemsTest, pagePlayHistoryItems);
                    before = pagePlayHistoryItems [pagePlayHistoryItems .length - 1].getPlayedAt();
                }
            } catch (SpotifyWebApiException | IOException | ParseException e) {
                e.printStackTrace();
            }

            Log.e("CUM ADICA", String.valueOf(playHistoryItemsTest.size()));








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
            try {
                loadRecentTracks();
            } catch (Exception e) {
                e.printStackTrace();
            }
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

                        float desiredHeightPercentage = 0.10f;
                        int parentHeight = getResources().getDisplayMetrics().heightPixels;
                        int desiredHeight = (int) (parentHeight * desiredHeightPercentage);
                        int desiredWidth=2*desiredHeight;


                        artistImage.setLayoutParams(new LinearLayout.LayoutParams(
                                desiredWidth, // Set image width to match parent
                                desiredHeight));
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

                        float desiredHeightPercentage = 0.10f;
                        int parentHeight = getResources().getDisplayMetrics().heightPixels;
                        int desiredHeight = (int) (parentHeight * desiredHeightPercentage);

                        int desiredWidth=2*desiredHeight;

                        artistImage.setLayoutParams(new LinearLayout.LayoutParams(
                                desiredWidth, // Set image width to match parent
                                desiredHeight));

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

    public void MoreTop(View view) {
        int viewId = view.getId();

        if (viewId == R.id.MoreArtistsButton) {
            toptype=1;
            Button SwitchToButton = findViewById(R.id.SwitchToButton);
            SwitchToButton.setText("Switch to tracks");
        }
        else if (viewId == R.id.MoreTracksButton) {
            toptype=4;
            Button SwitchToButton = findViewById(R.id.SwitchToButton);
            SwitchToButton.setText("Switch to artists");
        }
        new TopPageTop().execute();
    }

    public void MoreTracks(View view)
    {
        int viewId = view.getId();

        if (viewId == R.id.MoreRecentTracksButton)
            new LastTracksPageTop().execute();
    }

    public void MainPanel(View view)
    {
        LinearLayout mainpanel = findViewById(R.id.mainpanel);
        LinearLayout scrollViewContent = findViewById(R.id.TopPanelLayout);
        scrollViewContent.removeAllViews();
        HideEverythingExcept(mainpanel);
    }

    public void TopButtonsFunction(View view) {
        int viewId = view.getId();
        FrameLayout loading = findViewById(R.id.loadingpanel);
        HideEverythingExcept(loading);

        if (viewId == R.id.SwitchToButton) {
            if(toptype<4)
            {
                toptype=toptype+3;
                Button SwitchToButton = findViewById(R.id.SwitchToButton);
                SwitchToButton.setText("Switch to artists");
            }
            else
            {
                toptype=toptype-3;
                Button SwitchToButton = findViewById(R.id.SwitchToButton);
                SwitchToButton.setText("Switch to tracks");
            }
            if(toptype==1) {
                TextView TopTextTitle = findViewById(R.id.TopTextTitle);
                TopTextTitle.setText("Top artists\nlast 4 weeks");
            }
            else if(toptype==2) {
                TextView TopTextTitle = findViewById(R.id.TopTextTitle);
                TopTextTitle.setText("Top artists\nlast 6 months");
            }else if(toptype==3) {
                TextView TopTextTitle = findViewById(R.id.TopTextTitle);
                TopTextTitle.setText("Top artists\nlifetime");
            }
            if(toptype==4) {
                TextView TopTextTitle = findViewById(R.id.TopTextTitle);
                TopTextTitle.setText("Top tracks\nlast 4 weeks");
            }
            else if(toptype==5) {
                TextView TopTextTitle = findViewById(R.id.TopTextTitle);
                TopTextTitle.setText("Top tracks\nlast 6 months");
            }
            else if(toptype==6) {
                TextView TopTextTitle = findViewById(R.id.TopTextTitle);
                TopTextTitle.setText("Top tracks\nlifetime");
            }

        }
        else if (viewId == R.id.weeksbutton) {
            if(toptype<4)
                toptype=1;
            else
                toptype=4;
        }
        else if (viewId == R.id.monthsbutton) {
            if(toptype<4)
                toptype=2;
            else
                toptype=5;
        }
        else if (viewId == R.id.lifetimebutton) {
            if(toptype<4)
                toptype=3;
            else
                toptype=6;
        }
        new TopPageTop().execute();
    }

    private class TopPageTop extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.e("test","STARTED3");

        }
        @Override
        protected Void doInBackground(Void... voids) {
            return null;
        }


        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            if(toptype>=1 && toptype<=3) {
                //TOP ARTISTS
                List<Artist> ArtistsList = new ArrayList<>();
                if(toptype==1) {
                    ArtistsList.addAll(ShortTermArtists);
                    TextView TopTextTitle = findViewById(R.id.TopTextTitle);
                    TopTextTitle.setText("Top artists\nlast 4 weeks");

                }
                else if(toptype==2) {
                    ArtistsList.addAll(MediumTermArtists);
                    TextView TopTextTitle = findViewById(R.id.TopTextTitle);
                    TopTextTitle.setText("Top artists\nlast 6 months");
                }else if(toptype==3) {
                    ArtistsList.addAll(LongTermArtists);
                    TextView TopTextTitle = findViewById(R.id.TopTextTitle);
                    TopTextTitle.setText("Top artists\nlifetime");
                }
                if (ArtistsList != null) {
                    Log.e("test","ARTISTS!!!!!!!");

                    LinearLayout scrollViewContent = findViewById(R.id.TopPanelLayout);
                    scrollViewContent.removeAllViews();

                    int i = 1;
                    for (Artist artist : ArtistsList) {
                        if (i >= ArtistsList.size())
                            break;
                        else {
                            Log.e("test",artist.getName());
                                LinearLayout artistLayout = new LinearLayout(MainActivity.this);
                                artistLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                ));
                                artistLayout.setOrientation(LinearLayout.HORIZONTAL);

                                ImageView artistImage = new ImageView(MainActivity.this);

                            float desiredHeightPercentage = 0.10f;
                            int parentHeight = getResources().getDisplayMetrics().heightPixels;
                            int desiredHeight = (int) (parentHeight * desiredHeightPercentage);
                            int desiredWidth=2*desiredHeight;


                            artistImage.setLayoutParams(new LinearLayout.LayoutParams(
                                    desiredWidth,
                                    desiredHeight
                                ));

                                if (artist.getImages() != null && artist.getImages().length > 0) {
                                    String imageUrl = artist.getImages()[0].getUrl();
                                    Picasso.get()
                                            .load(imageUrl)
                                            .transform(new RoundedCornersTransformation(700, 10))
                                            .into(artistImage);
                                }

                                TextView artistInfo = new TextView(MainActivity.this);
                                artistInfo.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                ));

                                artistInfo.setText("\n"+i + ". " + artist.getName());
                                artistInfo.setGravity(Gravity.CENTER);
                                artistInfo.setTextColor(Color.parseColor("#280057"));
                                Typeface customTypeface = ResourcesCompat.getFont(MainActivity.this, R.font.font_family);
                                artistInfo.setTypeface(customTypeface);
                            int rightMarginPx = 10;
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );

                            layoutParams.setMargins(0, 0, rightMarginPx, 0);

                            artistInfo.setLayoutParams(layoutParams);

                            artistInfo.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);


                                artistLayout.addView(artistImage);
                                artistLayout.addView(artistInfo);

                                scrollViewContent.addView(artistLayout);

                                i++;
                        }
                    }
                    //one blank item for readability
                    View blankView = new View(MainActivity.this);
                    blankView.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 100));

                    scrollViewContent.addView(blankView);

                } else {
                    System.out.println("Error fetching top artists");
                }
            }

            else if(toptype>=4 && toptype<=6) {
                // TOP TRACKS

                List<Track> TracksList = new ArrayList<>();
                if(toptype==4) {
                    TracksList.addAll(ShortTermTracks);
                    TextView TopTextTitle = findViewById(R.id.TopTextTitle);
                    TopTextTitle.setText("Top tracks\nlast 4 weeks");
                }
                else if(toptype==5) {
                    TracksList.addAll(MediumTermTracks);
                    TextView TopTextTitle = findViewById(R.id.TopTextTitle);
                    TopTextTitle.setText("Top tracks\nlast 6 months");
                }
                else if(toptype==6) {
                    TracksList.addAll(LongTermTracks);
                    TextView TopTextTitle = findViewById(R.id.TopTextTitle);
                    TopTextTitle.setText("Top tracks\nlifetime");
                }
                if (TracksList != null) {
                    LinearLayout scrollViewContent = findViewById(R.id.TopPanelLayout);
                    scrollViewContent.removeAllViews();
                    int i = 1;
                    for (Track track : TracksList) {
                        if (i >= TracksList.size())
                            break;
                        else {
                            Log.e("test",track.getName());
                            LinearLayout artistLayout = new LinearLayout(MainActivity.this);
                            artistLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            ));
                            artistLayout.setOrientation(LinearLayout.HORIZONTAL);

                            ImageView artistImage = new ImageView(MainActivity.this);

                            float desiredHeightPercentage = 0.10f;
                            int parentHeight = getResources().getDisplayMetrics().heightPixels;
                            int desiredHeight = (int) (parentHeight * desiredHeightPercentage);
                            int desiredWidth=2*desiredHeight;


                            artistImage.setLayoutParams(new LinearLayout.LayoutParams(
                                    desiredWidth,
                                    desiredHeight
                            ));


                            AlbumSimplified album = track.getAlbum();
                            if (album.getImages() != null && album.getImages().length > 0) {
                                String imageUrl = album.getImages()[0].getUrl();

                                Picasso.get()
                                        .load(imageUrl)
                                        .transform(new RoundedCornersTransformation(100, 10))
                                        .into(artistImage);
                            }


                            TextView artistInfo = new TextView(MainActivity.this);
                            artistInfo.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            ));

                            artistInfo.setText("\n"+i + ". " + track.getName()+"\n"+track.getArtists()[0].getName());
                            artistInfo.setGravity(Gravity.CENTER);
                            artistInfo.setTextColor(Color.parseColor("#280057"));
                            Typeface customTypeface = ResourcesCompat.getFont(MainActivity.this, R.font.font_family);
                            artistInfo.setTypeface(customTypeface);
                            int rightMarginPx = 10;
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );

                            layoutParams.setMargins(0, 0, rightMarginPx, 0);

                            artistInfo.setLayoutParams(layoutParams);

                            artistInfo.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);


                            artistLayout.addView(artistImage);
                            artistLayout.addView(artistInfo);

                            scrollViewContent.addView(artistLayout);

                            i++;
                        }
                    }
                    View blankView = new View(MainActivity.this);
                    blankView.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 100));

                    scrollViewContent.addView(blankView);
                } else {
                    System.out.println("Error fetching top artists");
                }
            }

            LinearLayout tops = findViewById(R.id.tops);
            HideEverythingExcept(tops);

        }
    }




    private class LastTracksPageTop extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.e("test","STARTED3");

        }
        @Override
        protected Void doInBackground(Void... voids) {
            return null;
        }


        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);


                    LinearLayout scrollViewContent = findViewById(R.id.LastTracksPanelLayout);
                    scrollViewContent.removeAllViews();
                    int i = 1;
                    for (Track track : trackArray) {
                        if (i >= trackArray.length)
                            break;
                        else {

                            Log.e("test",track.getName());
                            LinearLayout artistLayout = new LinearLayout(MainActivity.this);
                            artistLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            ));
                            artistLayout.setOrientation(LinearLayout.HORIZONTAL);

                            ImageView artistImage = new ImageView(MainActivity.this);

                            float desiredHeightPercentage = 0.10f;
                            int parentHeight = getResources().getDisplayMetrics().heightPixels;
                            int desiredHeight = (int) (parentHeight * desiredHeightPercentage);
                            int desiredWidth=2*desiredHeight;


                            artistImage.setLayoutParams(new LinearLayout.LayoutParams(
                                    desiredWidth,
                                    desiredHeight
                            ));


                            AlbumSimplified album = track.getAlbum();
                            if (album.getImages() != null && album.getImages().length > 0) {
                                String imageUrl = album.getImages()[0].getUrl();

                                Picasso.get()
                                        .load(imageUrl)
                                        .transform(new RoundedCornersTransformation(100, 10))
                                        .into(artistImage);
                            }


                            TextView artistInfo = new TextView(MainActivity.this);
                            artistInfo.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            ));

                            artistInfo.setText("\n"+ track.getName()+"\n"+track.getArtists()[0].getName());
                            artistInfo.setGravity(Gravity.CENTER);
                            artistInfo.setTextColor(Color.parseColor("#280057"));
                            Typeface customTypeface = ResourcesCompat.getFont(MainActivity.this, R.font.font_family);
                            artistInfo.setTypeface(customTypeface);
                            int rightMarginPx = 10;
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );

                            layoutParams.setMargins(0, 0, rightMarginPx, 0);

                            artistInfo.setLayoutParams(layoutParams);

                            artistInfo.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);


                            artistLayout.addView(artistImage);
                            artistLayout.addView(artistInfo);

                            scrollViewContent.addView(artistLayout);

                            i++;
                        }
                    }
                    View blankView = new View(MainActivity.this);
                    blankView.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 100));

                    scrollViewContent.addView(blankView);

            LinearLayout lasttracks = findViewById(R.id.lasttracks);;
            HideEverythingExcept(lasttracks);

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