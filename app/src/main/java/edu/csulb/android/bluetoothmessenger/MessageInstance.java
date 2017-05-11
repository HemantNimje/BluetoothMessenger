package edu.csulb.android.bluetoothmessenger;

import android.graphics.Bitmap;

import java.io.File;

/**
 * Created by nisarg on 5/08/2017
 */

public class MessageInstance {
    public static final int DATA_IMAGE = 2;
    public static final int DATA_IMAGE_64 = 4;
    public static final int DATA_AUDIO = 3;
    public static final int DATA_TEXT = 1;

    public boolean send;
    public String message;
    public Bitmap imageBitmap;
    public File audioFile;
    public String macAddress;
    public String userName;
    public int dataType;
    public Object data;

    public MessageInstance() {
        message = null;
        imageBitmap = null;
        audioFile = null;
    }

    private MessageInstance(MessageInstanceBuilder builder) {
        this.userName = builder.userName;
        this.macAddress = builder.macAddress;
        this.audioFile = builder.audioFile;
        this.imageBitmap = builder.imageBitmap;
        this.message = builder.message;
        this.send = builder.send;
        this.dataType = builder.dataType;
        this.data = builder.data;
    }

    public String getMessage() {
        return message;
    }

    public Bitmap getImage() {
        return imageBitmap;
    }

    public File getAudio() {
        return audioFile;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getUserName() {
        return userName;
    }

    public boolean getSide() {
        return send;
    }

    public int getDataType() {
        return dataType;
    }

    public Object getData() {
        return data;
    }


    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
    public void setData(Object data) {
        this.data = data;
    }
    public void setDataType(int type) {
        this.dataType = type;
    }

    public MessageInstance(boolean side, String message, String macAddress) {
        this.send = side;
        this.message = message;
        macAddress = macAddress;
        imageBitmap = null;
        audioFile = null;
    }

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

    public class MessageInstanceBuilder {
        private boolean send;
        private String message;
        private Bitmap imageBitmap;
        private File audioFile;
        private String macAddress;
        private String userName;
        private int dataType;
        private Object data;

        public MessageInstanceBuilder() {
            this.message = null;
            this.imageBitmap = null;
            this.audioFile = null;
            this.macAddress = null;
            this.userName = null;
            this.data = null;
        }

        public MessageInstanceBuilder macAddress(String macAddress) {
            this.macAddress = macAddress;
            return this;
        }

        public MessageInstanceBuilder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public MessageInstanceBuilder message(String message) {
            this.message = message;
            return this;
        }

        public MessageInstanceBuilder dataType(int type) {
            this.dataType = type;
            return this;
        }

        public MessageInstanceBuilder data(Object msgData) {
            this.data = msgData;
            return this;
        }

    }
}
