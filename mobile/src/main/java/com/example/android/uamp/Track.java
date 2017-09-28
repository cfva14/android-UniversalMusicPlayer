package com.example.android.uamp;

import com.example.android.uamp.utils.RealmHelper;

import io.realm.RealmObject;

/**
 * Created on 9/28/17.
 */
public class Track extends RealmObject implements RealmHelper.RealmHelpable {

    private String albumId;
    private String albumImageUrl;
    private String albumName;
    private String artistId;
    private String artistImageUrl;
    private String artistName;
    private long duration;
    private String id;
    private String source;
    private String title;
    private int trackNumber;
    private Stats stats;
    private String genre;

    public Track() {}

    public Track(String albumId, String albumImageUrl, String albumName, String artistId, String artistImageUrl, String artistName, long duration, String id, String source, String title, int trackNumber, String genre, Stats stats) {
        this.albumId = albumId;
        this.albumImageUrl = albumImageUrl;
        this.albumName = albumName;
        this.artistId = artistId;
        this.artistImageUrl = artistImageUrl;
        this.artistName = artistName;
        this.duration = duration;
        this.id = id;
        this.source = source;
        this.title = title;
        this.trackNumber = trackNumber;
        this.stats = stats;
        this.genre = genre;
    }

    public String getPrimaryKeyName() {
        return "id";
    }

    public String getAlbumId() {
        return albumId;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    public String getAlbumImageUrl() {
        return albumImageUrl;
    }

    public void setAlbumImageUrl(String albumImageUrl) {
        this.albumImageUrl = albumImageUrl;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getArtistId() {
        return artistId;
    }

    public void setArtistId(String artistId) {
        this.artistId = artistId;
    }

    public String getArtistImageUrl() {
        return artistImageUrl;
    }

    public void setArtistImageUrl(String artistImageUrl) {
        this.artistImageUrl = artistImageUrl;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(int trackNumber) {
        this.trackNumber = trackNumber;
    }

    public Stats getStats() {
        return stats;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }
}
