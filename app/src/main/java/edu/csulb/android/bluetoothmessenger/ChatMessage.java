package edu.csulb.android.bluetoothmessenger;

import android.graphics.Bitmap;

import java.io.File;

/**
 * Created by vaibhavjain on 4/17/2017
 */

public class ChatMessage {
    public boolean send;
    public String message;
    public Bitmap imageBitmap;
    public File audioFile;

    public ChatMessage(boolean side, String message) {
        this.send = side;
        this.message = message;
        imageBitmap = null;
        audioFile = null;
    }

    public ChatMessage(boolean side, Bitmap imageBitmap) {
        this.send = side;
        this.message = null;
        this.imageBitmap = imageBitmap;
        audioFile = null;
    }

    public ChatMessage(boolean side, File audioFile) {
        this.send = side;
        this.message = null;
        this.imageBitmap = null;
        this.audioFile = audioFile;
    }
}
