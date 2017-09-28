package com.example.android.uamp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.security.KeyStoreException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;

/**
 * Created on 9/28/17.
 */
public class RealmHelper<T extends RealmObject & RealmHelper.RealmHelpable> {

    private static final String TAG = RealmHelper.class.getSimpleName();

    private static final String REALM_ENCRYPTED_KEY = "REALM_ENCRYPTED_KEY";
    private static final int DB_VERSION = 0;
    private static final AtomicBoolean realmInitialized = new AtomicBoolean(false);

    private Class<T> type;

    public RealmHelper(Class<T> type) {
        this.type = type;
    }

    public static void initRealm(Context context) throws KeyStoreException {


        /** Sets up Realm for the app **/
        Realm.init(context);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .schemaVersion(DB_VERSION)
                .build();
        Realm.setDefaultConfiguration(config);

        realmInitialized.set(true);
    }

    public static boolean isRealmInitialized() {
        return realmInitialized.get();
    }

    public T getFirstRecord() {
        Realm realm = Realm.getDefaultInstance();
        try {
            RealmResults<T> result = realm.where(type).findAll();
            if (!result.isEmpty()) {
                return result.first();
            }
        } finally {
            realm.close();
        }

        return null;
    }

    public T getUnmanagedFirstRecord() {
        Realm realm = Realm.getDefaultInstance();
        try {
            RealmResults<T> result = realm.where(type).findAll();
            if (!result.isEmpty()) {
                return realm.copyFromRealm(result.first());
            }
        } finally {
            realm.close();
        }

        return null;
    }

    public T getUnmanagedLastRecord() {
        Realm realm = Realm.getDefaultInstance();
        try {
            RealmResults<T> result = realm.where(type).findAll();
            if (!result.isEmpty()) {
                return realm.copyFromRealm(result.last());
            }
        } finally {
            realm.close();
        }

        return null;
    }

    public T getRecord(String columnName, String valueThatEquals) {
        Realm realm = Realm.getDefaultInstance();
        try {
            RealmResults<T> result = realm.where(type).equalTo(columnName, valueThatEquals).findAll();
            if (!result.isEmpty()) {
                return result.first();
            }
        } finally {
            realm.close();
        }

        return null;
    }

    public T getRecord(String columnName, int valueThatEquals) {
        Realm realm = Realm.getDefaultInstance();
        try {
            RealmResults<T> result = realm.where(type).equalTo(columnName, valueThatEquals).findAll();
            if (!result.isEmpty()) {
                return result.first();
            }
        } finally {
            realm.close();
        }

        return null;
    }

    public T getRecord(String columnName, long valueThatEquals) {
        Realm realm = Realm.getDefaultInstance();
        try {
            RealmResults<T> result = realm.where(type).equalTo(columnName, valueThatEquals).findAll();
            if (!result.isEmpty()) {
                return result.first();
            }
        } finally {
            realm.close();
        }

        return null;
    }

    public T getUnmanagedRecord(String columnName, int valueThatEquals) {
        Realm realm = Realm.getDefaultInstance();
        try {
            RealmResults<T> result = realm.where(type).equalTo(columnName, valueThatEquals).findAll();
            if (!result.isEmpty()) {
                return realm.copyFromRealm(result.first());
            }
        } finally {
            realm.close();
        }

        return null;
    }

    public T getUnmanagedRecord(String columnName, String valueThatEquals) {
        Realm realm = Realm.getDefaultInstance();
        try {
            RealmResults<T> result = realm.where(type).equalTo(columnName, valueThatEquals).findAll();
            if (!result.isEmpty()) {
                return realm.copyFromRealm(result.first());
            }
        } finally {
            realm.close();
        }

        return null;
    }

    public T getUnmanagedRecord(String columnName, long valueThatEquals) {
        Realm realm = Realm.getDefaultInstance();
        try {
            RealmResults<T> result = realm.where(type).equalTo(columnName, valueThatEquals).findAll();
            if (!result.isEmpty()) {
                return realm.copyFromRealm(result.first());
            }
        } finally {
            realm.close();
        }

        return null;
    }

    public int getNextPrimaryKey(T data) {
        Realm realm = Realm.getDefaultInstance();
        try {
            Number max = realm.where(type).max(data.getPrimaryKeyName());
            if (max != null) {
                return max.intValue() + 1;
            }
        } finally {
            realm.close();
        }

        return 0;
    }

    /**
     * This method may cause app to crash on emulators
     *
     * @param data
     */
    public void storeData(T data) {
        Realm realm = Realm.getDefaultInstance();
        try {
            realm.beginTransaction();
            if (hasPrimaryKey(data)) {
                realm.copyToRealmOrUpdate(data);
            } else {
                realm.copyToRealm(data);
            }
            realm.commitTransaction();
        } finally {
            realm.close();
        }
    }

    /**
     * This method may cause app to crash on emulators
     *
     * @param data
     */
    public void storeData(List<T> data) {
        Realm realm = Realm.getDefaultInstance();
        try {
            for (T member : data) {
                realm.beginTransaction();
                if (hasPrimaryKey(member)) {
                    realm.copyToRealmOrUpdate(member);
                } else {
                    realm.copyToRealm(member);
                }
                realm.commitTransaction();
            }
        } finally {
            realm.close();
        }
    }

    private boolean hasPrimaryKey(T data) {
        return TextUtils.isEmpty(data.getPrimaryKeyName()) || data.getPrimaryKeyName().trim().length() == 0;
    }

    public RealmResults<T> getData() {
        Realm realm = Realm.getDefaultInstance();
        try {
            return realm.where(type).findAll();
        } finally {
            realm.close();
        }
    }

    public List<T> getUnmanagedData() {
        Realm realm = Realm.getDefaultInstance();
        try {
            return realm.copyFromRealm(realm.where(type).findAll());
        } finally {
            realm.close();
        }
    }

    public void clearData() {
        Realm realm = Realm.getDefaultInstance();
        try {
            realm.beginTransaction();
            RealmResults<T> result = realm.where(type).findAll();
            result.deleteAllFromRealm();
            realm.commitTransaction();
        } finally {
            realm.close();
        }
    }

    public static void cascadeDelete(Object objectToDelete) {
        if (objectToDelete != null) {
            if (objectToDelete instanceof CascadeDeletable) {
                LogHelper.d(TAG, "Delete CascadeDeletable: " + objectToDelete.getClass().getSimpleName());
                ((CascadeDeletable) objectToDelete).cascadeDelete();
            } else if (objectToDelete instanceof List) {
                List listToDelete = (List) objectToDelete;
                if (!listToDelete.isEmpty()) {
                    if (listToDelete.get(0) instanceof CascadeDeletable) {
                        LogHelper.d(TAG, "Delete list of CascadeDeletable: " + objectToDelete.getClass().getSimpleName());
                        for (Object o : listToDelete) {
                            ((CascadeDeletable) o).cascadeDelete();
                        }
                    } else if (objectToDelete instanceof RealmList) {
                        LogHelper.d(TAG, "Delete RealmList: " + objectToDelete.getClass().getSimpleName());
                        Realm realm = Realm.getDefaultInstance();
                        try {
                            realm.beginTransaction();
                            ((RealmList) objectToDelete).deleteAllFromRealm();
                            realm.commitTransaction();
                        } finally {
                            realm.close();
                        }
                    }
                }
            } else if (objectToDelete instanceof RealmObject) {
                LogHelper.d(TAG, "Delete RealmObject: " + objectToDelete.getClass().getSimpleName());
                Realm realm = Realm.getDefaultInstance();
                try {
                    realm.beginTransaction();
                    ((RealmObject) objectToDelete).deleteFromRealm();
                    realm.commitTransaction();
                } finally {
                    realm.close();
                }
            }
        }
    }

    public static void delete(RealmObject objToDelete) {
        Realm realm = Realm.getDefaultInstance();
        try {
            realm.beginTransaction();
            objToDelete.deleteFromRealm();
            realm.commitTransaction();
        } finally {
            realm.close();
        }
    }

    public interface CascadeDeletable {
        void cascadeDelete();
    }

    public interface RealmHelpable {
        String getPrimaryKeyName();
    }
}

