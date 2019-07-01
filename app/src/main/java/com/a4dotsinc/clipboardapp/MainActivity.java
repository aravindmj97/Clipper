package com.a4dotsinc.clipboardapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.a4dotsinc.clipboardapp.services.ClipboardMonitorService;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        ContextCompat.startForegroundService(this, new Intent(this, ClipboardMonitorService.class).putExtra("text", "Monitoring"));
        finish();

    }
}
