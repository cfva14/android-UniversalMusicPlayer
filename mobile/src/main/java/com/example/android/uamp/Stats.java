package com.example.android.uamp;

import io.realm.RealmObject;

/**
 * Created on 9/28/17.
 */
public class Stats extends RealmObject {

    private long listeners;
    private long plays;

    public long getListeners() {
        return listeners;
    }

    public void setListeners(long listeners) {
        this.listeners = listeners;
    }

    public long getPlays() {
        return plays;
    }

    public void setPlays(long plays) {
        this.plays = plays;
    }
}
