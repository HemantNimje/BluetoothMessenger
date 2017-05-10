package edu.csulb.android.bluetoothmessenger;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class UserInfo implements Parcelable {
    String name;
    String macAddress;

    UserInfo(String name, String macAddress) {
        this.name = name;
        this.macAddress = macAddress;
    }

    private UserInfo(Parcel in) {
        name = in.readString();
        macAddress = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(macAddress);
    }

    public static final Parcelable.Creator<UserInfo> CREATOR = new Parcelable.Creator<UserInfo>() {
        public UserInfo createFromParcel(Parcel in) {
            return new UserInfo(in);
        }

        public UserInfo[] newArray(int size) {
            return new UserInfo[size];

        }
    };

    static List<UserInfo> getUsersInfo(String users) {
        String usersSplitUp[] = users.split("[\\r?\\n]+");

        List<UserInfo> usersInfo = new ArrayList<>();

        for (int i = 0; i < usersSplitUp.length; i+= 2) {
            String name = usersSplitUp[i];
            String userMacAddress = usersSplitUp[i+1];
            usersInfo.add(new UserInfo(name, userMacAddress));
        }

        return usersInfo;
    }

    public String getName(){
        return name;
    }
    public String getMacAddress(){
        return macAddress;
    }
}
