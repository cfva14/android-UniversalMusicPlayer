/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.uamp.model;

import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import com.example.android.uamp.Track;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.RealmHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Utility class to get a list of MusicTrack's based on a server-side JSON
 * configuration.
 */
public class RemoteJSONSource implements MusicProviderSource {

    private static final String TAG = LogHelper.makeLogTag(RemoteJSONSource.class);

    //protected static final String CATALOG_URL = "https://musicapp-54d43.firebaseio.com/track.json?orderBy=\"$key\"&startAt=\"-KtoRvHuW9ryTCfewG1r\"&limitToFirst=2";
    protected static final String CATALOG_URL = "https://musicapp-54d43.firebaseio.com/track.json";

    private static final String JSON_ID = "id";
    private static final String JSON_TITLE = "title";
    private static final String JSON_GENRE = "genre";
    private static final String JSON_ALBUM = "albumName";
    private static final String JSON_ALBUM_IMAGE = "albumImageUrl";
    private static final String JSON_ALBUM_ID = "albumId";
    private static final String JSON_ARTIST = "artistName";
    private static final String JSON_ARTIST_IMAGE = "artistImageUrl";
    private static final String JSON_ARTIST_ID = "artistId";
    private static final String JSON_SOURCE = "source";
    private static final String JSON_TRACK_NUMBER = "trackNumber";
    private static final String JSON_DURATION = "duration";

    @Override
    public Iterator<MediaMetadataCompat> iterator() {
        RealmHelper<Track> realmHelper = new RealmHelper<>(Track.class);
        Track track = realmHelper.getUnmanagedLastRecord();
        String url;
        if (track != null) {
            url = "https://musicapp-54d43.firebaseio.com/track.json?orderBy=\"$key\"&startAt=\"" + track.getId() + "\"";
        } else {
            url = CATALOG_URL;
        }
        try {
            JSONObject jsonObj = fetchJSONFromUrl(url);
            Log.e(TAG, jsonObj.toString().getBytes().length / 1000000f + "MB");
            ArrayList<MediaMetadataCompat> tracks = new ArrayList<>();
            if (jsonObj != null) {
                Iterator<String> keys = jsonObj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject innerObject = jsonObj.getJSONObject(key);
                    saveToRealm(innerObject);
                }

                RealmHelper<Track> storedTracks = new RealmHelper<>(Track.class);
                for(Track storedTrack : storedTracks.getUnmanagedData()) {
                    tracks.add(buildFromRealm(storedTrack));
                }
            }
            return tracks.iterator();
        } catch (JSONException e) {
            LogHelper.e(TAG, e, "Could not retrieve music list");
            throw new RuntimeException("Could not retrieve music list", e);
        }
    }

    private void saveToRealm(JSONObject json) throws JSONException {
        String id = json.getString(JSON_ID);
        String title = json.getString(JSON_TITLE);
        String genre = "Unknown";
        if (!json.isNull(JSON_GENRE)) {
            genre = json.getString(JSON_GENRE);
        }
        String album = json.getString(JSON_ALBUM);
        String albumImageUrl = json.getString(JSON_ALBUM_IMAGE);
        String albumId = json.getString(JSON_ALBUM_ID);
        String artist = json.getString(JSON_ARTIST);
        String artistImageUrl = "Unknown";
        if (!json.isNull(JSON_ARTIST_IMAGE)) {
            json.getString(JSON_ARTIST_IMAGE);
        }
        String artistId = json.getString(JSON_ARTIST_ID);
        String source = json.getString(JSON_SOURCE);
        int trackNumber = json.getInt(JSON_TRACK_NUMBER);
        int duration = json.getInt(JSON_DURATION) * 1000; // ms

        Track track = new Track(albumId, albumImageUrl, album, artistId, artistImageUrl, artist, duration, id, source, title, trackNumber, genre, null);
        RealmHelper<Track> realmHelper = new RealmHelper<>(Track.class);
        realmHelper.storeData(track);
    }

    private MediaMetadataCompat buildFromRealm(Track track) throws JSONException {
        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, track.getId())
                .putString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE, track.getSource())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.getAlbumName())
                .putString(MusicProviderSource.CUSTOM_METADATA_ALBUM_ID, track.getAlbumId())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.getArtistName())
                .putString(MusicProviderSource.CUSTOM_METADATA_ARTIST_ID, track.getArtistId())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.getDuration())
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, track.getGenre())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, track.getAlbumImageUrl())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.getTitle())
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, track.getTrackNumber())
                .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 10)
                .build();
    }

    /**
     * Download a JSON file from a server, parse the content and return the JSON
     * object.
     *
     * @return result JSONObject containing the parsed representation.
     */
    private JSONObject fetchJSONFromUrl(String urlString) throws JSONException {
        BufferedReader reader = null;
        try {
            URLConnection urlConnection = new URL(urlString).openConnection();
            reader = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream(), "iso-8859-1"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return new JSONObject(sb.toString());
        } catch (JSONException e) {
            throw e;
        } catch (Exception e) {
            LogHelper.e(TAG, "Failed to parse the json for media list", e);
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}
