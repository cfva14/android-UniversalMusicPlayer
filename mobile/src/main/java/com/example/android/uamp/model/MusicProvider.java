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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.MediaIDHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM;
import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_ARTIST;
import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE;
import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_ROOT;
import static com.example.android.uamp.utils.MediaIDHelper.createMediaID;

/**
 * Simple data provider for music tracks. The actual metadata source is delegated to a
 * MusicProviderSource defined by a constructor argument of this class.
 */
public class MusicProvider {

    private static final String TAG = LogHelper.makeLogTag(MusicProvider.class);

    private MusicProviderSource mSource;

    // Categorized caches for music track data:
    private ConcurrentMap<String, List<MediaMetadataCompat>> mMusicListByArtist;
    private ConcurrentMap<String, List<MediaMetadataCompat>> mMusicListByAlbum;
    private ConcurrentMap<String, List<MediaMetadataCompat>> mMusicListByGenre;
    private ConcurrentMap<String, List<String>> mAlbumListByArtist;
    private final ConcurrentMap<String, MutableMediaMetadata> mMusicListById;

    private final Set<String> mFavoriteTracks;

    enum State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }

    private volatile State mCurrentState = State.NON_INITIALIZED;

    public interface Callback {
        void onMusicCatalogReady(boolean success);
    }

    public MusicProvider() {
        this(new RemoteJSONSource());
    }
    public MusicProvider(MusicProviderSource source) {
        mSource = source;
        mMusicListByArtist = new ConcurrentHashMap<>();
        mMusicListByAlbum = new ConcurrentHashMap<>();
        mAlbumListByArtist = new ConcurrentHashMap<>();
        mMusicListByGenre = new ConcurrentHashMap<>();
        mMusicListById = new ConcurrentHashMap<>();
        mFavoriteTracks = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    }

    /**
     * Get an iterator over the list of artists
     *
     * @return artists
     */
    public Iterable<String> getArtists() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        return mMusicListByArtist.keySet();
    }

    /**
     * Get an iterator over the list of artists
     *
     * @return artists
     */
    public Iterable<String> getGenres() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        return mMusicListByGenre.keySet();
    }

    /**
     * Get an iterator over the list of artists
     *
     * @return genres
     */
    public Iterable<String> getAlbumsByArtist(String artist) {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        return mAlbumListByArtist.get(artist);
    }

    /**
     * Get an iterator over a shuffled collection of all songs
     */
    public Iterable<MediaMetadataCompat> getShuffledMusic() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        List<MediaMetadataCompat> shuffled = new ArrayList<>(mMusicListById.size());
        for (MutableMediaMetadata mutableMetadata: mMusicListById.values()) {
            shuffled.add(mutableMetadata.metadata);
        }
        Collections.shuffle(shuffled);
        return shuffled;
    }

    /**
     * Get music tracks of the given artist
     *
     */
    public Iterable<MediaMetadataCompat> getMusicsByArtist(String artist) {
        if (mCurrentState != State.INITIALIZED || !mMusicListByArtist.containsKey(artist)) {
            return Collections.emptyList();
        }
        return mMusicListByArtist.get(artist);
    }

    /**
     * Get music tracks of the given album
     *
     */
    public List<MediaMetadataCompat> getMusicsByAlbum(String album) {
        if (mCurrentState != State.INITIALIZED || !mMusicListByAlbum.containsKey(album)) {
            return Collections.emptyList();
        }
        return mMusicListByAlbum.get(album);
    }

    /**
     * Get music tracks of the given genre
     *
     */
    public List<MediaMetadataCompat> getMusicsByGenre(String genre) {
        if (mCurrentState != State.INITIALIZED || !mMusicListByGenre.containsKey(genre)) {
            return Collections.emptyList();
        }
        return mMusicListByGenre.get(genre);
    }

    /**
     * Very basic implementation of a search that filter music tracks with title containing
     * the given query.
     *
     */
    public Iterable<MediaMetadataCompat> searchMusicBySongTitle(String query) {
        return searchMusic(MediaMetadataCompat.METADATA_KEY_TITLE, query);
    }

    /**
     * Very basic implementation of a search that filter music tracks with album containing
     * the given query.
     *
     */
    public Iterable<MediaMetadataCompat> searchMusicByAlbum(String query) {
        return searchMusic(MediaMetadataCompat.METADATA_KEY_ALBUM, query);
    }

    /**
     * Very basic implementation of a search that filter music tracks with artist containing
     * the given query.
     *
     */
    public Iterable<MediaMetadataCompat> searchMusicByArtist(String query) {
        return searchMusic(MediaMetadataCompat.METADATA_KEY_ARTIST, query);
    }

    Iterable<MediaMetadataCompat> searchMusic(String metadataField, String query) {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        ArrayList<MediaMetadataCompat> result = new ArrayList<>();
        query = query.toLowerCase(Locale.US);
        for (MutableMediaMetadata track : mMusicListById.values()) {
            if (track.metadata.getString(metadataField).toLowerCase(Locale.US)
                .contains(query)) {
                result.add(track.metadata);
            }
        }
        return result;
    }


    /**
     * Return the MediaMetadataCompat for the given musicID.
     *
     * @param musicId The unique, non-hierarchical music ID.
     */
    public MediaMetadataCompat getMusic(String musicId) {
        return mMusicListById.containsKey(musicId) ? mMusicListById.get(musicId).metadata : null;
    }

    public synchronized void updateMusicArt(String musicId, Bitmap albumArt, Bitmap icon) {
        MediaMetadataCompat metadata = getMusic(musicId);
        metadata = new MediaMetadataCompat.Builder(metadata)

                // set high resolution bitmap in METADATA_KEY_ALBUM_ART. This is used, for
                // example, on the lockscreen background when the media session is active.
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)

                // set small version of the album art in the DISPLAY_ICON. This is used on
                // the MediaDescription and thus it should be small to be serialized if
                // necessary
                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, icon)

                .build();

        MutableMediaMetadata mutableMetadata = mMusicListById.get(musicId);
        if (mutableMetadata == null) {
            throw new IllegalStateException("Unexpected error: Inconsistent data structures in " +
                    "MusicProvider");
        }

        mutableMetadata.metadata = metadata;
    }

    public void setFavorite(String musicId, boolean favorite) {
        if (favorite) {
            mFavoriteTracks.add(musicId);
        } else {
            mFavoriteTracks.remove(musicId);
        }
    }

    public boolean isInitialized() {
        return mCurrentState == State.INITIALIZED;
    }

    public boolean isFavorite(String musicId) {
        return mFavoriteTracks.contains(musicId);
    }

    /**
     * Get the list of music tracks from a server and caches the track information
     * for future reference, keying tracks by musicId and grouping by artist.
     */
    public void retrieveMediaAsync(final Callback callback) {
        LogHelper.d(TAG, "retrieveMediaAsync called");
        if (mCurrentState == State.INITIALIZED) {
            if (callback != null) {
                // Nothing to do, execute callback immediately
                callback.onMusicCatalogReady(true);
            }
            return;
        }

        // Asynchronously load the music catalog in a separate thread
        new AsyncTask<Void, Void, State>() {
            @Override
            protected State doInBackground(Void... params) {
                retrieveMedia();
                return mCurrentState;
            }

            @Override
            protected void onPostExecute(State current) {
                if (callback != null) {
                    callback.onMusicCatalogReady(current == State.INITIALIZED);
                }
            }
        }.execute();
    }

    private synchronized void buildListsByArtist() {
        ConcurrentMap<String, List<MediaMetadataCompat>> newMusicListByArtist = new ConcurrentHashMap<>();

        for (MutableMediaMetadata m : mMusicListById.values()) {
            String artist = m.metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
            List<MediaMetadataCompat> list = newMusicListByArtist.get(artist);
            if (list == null) {
                list = new ArrayList<>();
                newMusicListByArtist.put(artist, list);
            }
            list.add(m.metadata);
        }
        mMusicListByArtist = newMusicListByArtist;
    }

    private synchronized void buildListsByAlbum() {
        ConcurrentMap<String, List<MediaMetadataCompat>> newMusicListByAlbum = new ConcurrentHashMap<>();

        for (MutableMediaMetadata m : mMusicListById.values()) {
            String album = m.metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
            List<MediaMetadataCompat> list = newMusicListByAlbum.get(album);
            if (list == null) {
                list = new ArrayList<>();
                newMusicListByAlbum.put(album, list);
            }
            list.add(m.metadata);
        }
        mMusicListByAlbum = newMusicListByAlbum;
    }

    private synchronized void buildListsByGenre() {
        ConcurrentMap<String, List<MediaMetadataCompat>> newMusicListByGenre = new ConcurrentHashMap<>();

        for (MutableMediaMetadata m : mMusicListById.values()) {
            String genre = m.metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE);
            List<MediaMetadataCompat> list = newMusicListByGenre.get(genre);
            if (list == null) {
                list = new ArrayList<>();
                newMusicListByGenre.put(genre, list);
            }
            list.add(m.metadata);
        }
        mMusicListByGenre = newMusicListByGenre;
    }

    private synchronized void buildAlbumListByArtist() {
        ConcurrentMap<String, List<String>> newAlbumListByArtist = new ConcurrentHashMap<>();
        for (MutableMediaMetadata m : mMusicListById.values()) {
            String artist = m.metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
            List<String> list = newAlbumListByArtist.get(artist);
            if (list == null) {
                list = new ArrayList<>();
                newAlbumListByArtist.put(artist, list);
            }
            if (!list.contains(m.metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM))) {
                list.add(m.metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM));
            }
        }
        mAlbumListByArtist = newAlbumListByArtist;
    }

    private synchronized void retrieveMedia() {
        try {
            if (mCurrentState == State.NON_INITIALIZED) {
                mCurrentState = State.INITIALIZING;

                Iterator<MediaMetadataCompat> tracks = mSource.iterator();
                while (tracks.hasNext()) {
                    MediaMetadataCompat item = tracks.next();
                    String musicId = item.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
                    mMusicListById.put(musicId, new MutableMediaMetadata(musicId, item));
                }
                buildListsByArtist();
                buildListsByAlbum();
                buildAlbumListByArtist();
                buildListsByGenre();
                mCurrentState = State.INITIALIZED;
            }
        } finally {
            if (mCurrentState != State.INITIALIZED) {
                // Something bad happened, so we reset state to NON_INITIALIZED to allow
                // retries (eg if the network connection is temporary unavailable)
                mCurrentState = State.NON_INITIALIZED;
            }
        }
    }


    public List<MediaBrowserCompat.MediaItem> getChildren(String mediaId, Resources resources) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        if (!MediaIDHelper.isBrowseable(mediaId)) {
            return mediaItems;
        }

        if (MEDIA_ID_ROOT.equals(mediaId)) {
            mediaItems.add(createBrowsableMediaItemForRoot(MEDIA_ID_MUSICS_BY_ARTIST, resources));
            mediaItems.add(createBrowsableMediaItemForRoot(MEDIA_ID_MUSICS_BY_GENRE, resources));

        } else if (MEDIA_ID_MUSICS_BY_ARTIST.equals(mediaId)) {
            for (String artist : getArtists()) {
                mediaItems.add(createBrowsableMediaItemForArtist(artist, resources));
            }

        } else if (MEDIA_ID_MUSICS_BY_GENRE.equals(mediaId)) {
            for (String genre : getGenres()) {
                mediaItems.add(createBrowsableMediaItemForGenre(genre, resources));
            }

        } else if (mediaId.startsWith(MEDIA_ID_MUSICS_BY_ARTIST) && mediaId.endsWith(MEDIA_ID_MUSICS_BY_ALBUM)) {
            String[] split = mediaId.split("/");
            String artist = split[1];
            for (String album : getAlbumsByArtist(artist)) {
                mediaItems.add(createBrowsableAlbumMediaItemForArtist(artist, album, resources));
            }

        } else if (mediaId.startsWith(MEDIA_ID_MUSICS_BY_ARTIST) && mediaId.contains(MEDIA_ID_MUSICS_BY_ARTIST) && mediaId.contains(MEDIA_ID_MUSICS_BY_ALBUM)) {
            String album = MediaIDHelper.getHierarchy(mediaId)[3];
            for (MediaMetadataCompat metadata : getMusicsByAlbum(album)) {
                mediaItems.add(createMediaItem(metadata));
            }

        } else if (mediaId.startsWith(MEDIA_ID_MUSICS_BY_GENRE)) {
            String genre = MediaIDHelper.getHierarchy(mediaId)[1];
            for (MediaMetadataCompat metadata : getMusicsByGenre(genre)) {
                mediaItems.add(createMediaItem(metadata));
            }

        } else {
            LogHelper.w(TAG, "Skipping unmatched mediaId: ", mediaId);
        }
        return mediaItems;
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForRoot(String mediaId, Resources resources) {
        String title = resources.getString(R.string.browse_unknown);
        String subTitle = resources.getString(R.string.browse_genre_subtitle);;
        switch (mediaId) {
            case MEDIA_ID_MUSICS_BY_ARTIST:
                title = resources.getString(R.string.browse_artists);
                subTitle = resources.getString(R.string.browse_artist_subtitle);
                break;
            case MEDIA_ID_MUSICS_BY_GENRE:
                title = resources.getString(R.string.browse_genres);
                subTitle = resources.getString(R.string.browse_genre_subtitle);
                break;
        }
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(mediaId)
                .setTitle(title)
                .setSubtitle(subTitle)
                .setIconUri(Uri.parse("android.resource://" + "com.example.android.uamp/drawable/ic_by_genre"))
                .build();
        return new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForArtist(String artist, Resources resources) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(createMediaID(null, MEDIA_ID_MUSICS_BY_ARTIST, artist, MEDIA_ID_MUSICS_BY_ALBUM))
                .setTitle(artist)
                .setSubtitle(resources.getString(
                        R.string.browse_musics_by_genre_subtitle, artist))
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForGenre(String genre, Resources resources) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(createMediaID(null, MEDIA_ID_MUSICS_BY_GENRE, genre))
                .setTitle(genre)
                .setSubtitle(resources.getString(
                        R.string.browse_musics_by_genre_subtitle, genre))
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createBrowsableAlbumMediaItemForArtist(String artist, String album, Resources resources) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(createMediaID(null, MEDIA_ID_MUSICS_BY_ARTIST, artist, MEDIA_ID_MUSICS_BY_ALBUM, album))
                .setTitle(album)
                .setSubtitle(resources.getString(
                        R.string.browse_musics_by_genre_subtitle, album))
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createMediaItem(MediaMetadataCompat metadata) {
        // Since mediaMetadata fields are immutable, we need to create a copy, so we
        // can set a hierarchy-aware mediaID. We will need to know the media hierarchy
        // when we get a onPlayFromMusicID call, so we can create the proper queue based
        // on where the music was selected from (by artist, by genre, random, etc)
        String artist = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
        String hierarchyAwareMediaID = MediaIDHelper.createMediaID(
                metadata.getDescription().getMediaId(), MEDIA_ID_MUSICS_BY_ARTIST, artist);
        MediaMetadataCompat copy = new MediaMetadataCompat.Builder(metadata)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
                .build();
        return new MediaBrowserCompat.MediaItem(copy.getDescription(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);

    }

}
