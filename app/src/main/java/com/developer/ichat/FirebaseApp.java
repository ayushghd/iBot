package com.developer.ichat;

/**
 * Created by AJAY KUMAR on 3/24/2018.
 */

import com.google.firebase.database.FirebaseDatabase;


public class FirebaseApp extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}