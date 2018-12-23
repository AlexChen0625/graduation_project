package com.example.ibob0625.mysignin.FIreBase;

public class Photo {
    private double latitude;
    private double longitude;
    private String dataName;
    private String takedTime;

    public Photo(){}

    public Photo(String dataName, double la, double lo, String t){
        latitude = la;
        longitude = lo;
        this.dataName = dataName;
        this.takedTime = t;
    }

    public Photo(String dataName, String la, String lo, String t){
        latitude = Double.parseDouble(la);
        longitude = Double.parseDouble(lo);
        this.dataName = dataName;
        this.takedTime = t;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getFileName() { return dataName; }
    public String getTakedTime() { return takedTime; }
}

