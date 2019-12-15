package com.example.androidgameapp;

import android.util.AndroidException;
import android.util.AndroidRuntimeException;

public class AndroidBusinessApiException extends AndroidRuntimeException {

    public AndroidBusinessApiException(String message) {
        super(message);
    }
}
