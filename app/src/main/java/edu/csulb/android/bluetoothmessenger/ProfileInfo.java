package edu.csulb.android.bluetoothmessenger;

/**
 * Created by nisar on 09/05/2017.
 */

public class ProfileInfo {

    String Username = null;
    String macAddress = null;

    public ProfileInfo(String username, String macAddress) {
        Username = username;
        this.macAddress = macAddress;
    }

    public String getUsername() {
        return Username;
    }

    public void setUsername(String username) {
        Username = username;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
}
