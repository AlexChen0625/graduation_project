package com.example.ibob0625.mysignin.FIreBase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

import com.google.firebase.database.Exclude;
import com.google.gson.Gson;

public class flightData{
    private String userID;
    private long StartTime;
    private long EndTime;
    private long spendTime;

    private int photoNumber;
    private ArrayList<Photo> photoList;
    private ArrayList<String[]> flyingPath;
    private String videoFileName;
    private String jsonFileName;
    private String flightName;

    public flightData(){}

    public flightData(String uid){
        userID = uid;
        GregorianCalendar now = new GregorianCalendar();
        StartTime = now.getTimeInMillis();

        photoNumber = 0;
        photoList = new ArrayList<Photo>();
        flyingPath = new ArrayList<String[]>();
        videoFileName = "";
    }

    public void recordPath(String latitude, String longitude){
        String[] coordinate = {latitude, longitude};
        flyingPath.add(coordinate);
    }

    public void setPhotoList(Photo p){
        photoList.add(p);
        photoNumber++;
    }

    public void setFlightName(String fn){ flightName = fn; }

    public void setVideoFileName(String vfn){
        videoFileName = vfn;
    }

    public void endSetting(String fileDir){
        GregorianCalendar now = new GregorianCalendar();
        EndTime = now.getTimeInMillis();
        spendTime = EndTime - StartTime;

        jsonFileName = userID + "_" + StartTime + "_flight.json";
        Gson gson = new Gson();
        File f = new File(fileDir,  jsonFileName);
        try(FileWriter writer = new FileWriter(fileDir + "/" + jsonFileName)){
            gson.toJson(this, writer);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public ArrayList<String> getPhotoFileNameList(){
        ArrayList<String> s = new ArrayList<String>();
        for(int count = 0; count < photoNumber; count++)
            s.add(photoList.get(count).getFileName());
        return s;
    }

    @Exclude
    public long getStartTimeWithMillSec(){
        return StartTime;
    }

    public String getStartTime(){
        Date d = new Date(StartTime);
        String time = new SimpleDateFormat("EEE yyyy/MM/dd a HH:mm:ss").format(d);
        return time;
    }
    public String getEndTime(){
        Date d = new Date(EndTime);
        String time = new SimpleDateFormat("EEE yyyy/MM/dd a HH:mm:ss").format(d);
        return time;
    }
    public String getUserID() { return userID; }
    public String getSpendTime(){
        Date d = new Date(spendTime);
        String time = new SimpleDateFormat("mm:ss").format(d);
        return time;
    }
    public int getPhotoNumber(){ return photoNumber; }
    @Exclude
    public ArrayList<Photo> getPhotoList(){ return photoList; }
    public ArrayList<String> getFlyingPath(){
        ArrayList<String> sList = new ArrayList<>();
        for(String[] s : flyingPath)
            sList.add(s[0] + "," + s[1]);
        return sList;
    }
    @Exclude
    public String getVideoFileName(){ return videoFileName; }
    @Exclude
    public String getJsonFileName() { return jsonFileName; }
    public String getFlightName() { return flightName; }

}
