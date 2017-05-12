package edu.csulb.android.bluetoothmessenger;

public class Device {

    private String mName = null;
    private String mAddress = null;
    private boolean isSelected = false;

    public Device(String name, String address, boolean selected) {
        super();
        this.mName = name;
        this.mAddress = address;
        this.isSelected = selected;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String mAddress) {
        this.mAddress = mAddress;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
