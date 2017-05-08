package edu.csulb.android.bluetoothmessenger;

import android.graphics.Bitmap;

import java.io.File;

/**
 * Created by nisarg on 5/08/2017
 */

public class MessageInstance {
    public boolean send;
    public String message;
    public Bitmap imageBitmap;
    public File audioFile;

    public MessageInstance(boolean side, String message) {
        this.send = side;
        this.message = message;
        imageBitmap = null;
        audioFile = null;
    }

    public MessageInstance(boolean side, Bitmap imageBitmap) {
        this.send = side;
        this.message = null;
        this.imageBitmap = imageBitmap;
        audioFile = null;
    }

    public MessageInstance(boolean side, File audioFile) {
        this.send = side;
        this.message = null;
        this.imageBitmap = null;
        this.audioFile = audioFile;
    }
}
