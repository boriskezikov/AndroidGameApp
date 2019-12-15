package com.example.androidgameapp;

import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class NetworkFailsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(this, "Connection lost", Toast.LENGTH_LONG).show();
        SystemClock.sleep(10000);
    }
}
