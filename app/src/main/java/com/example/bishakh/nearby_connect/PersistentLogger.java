package com.example.bishakh.nearby_connect;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
 * Created by bishakh on 11/7/17.
 */

public class PersistentLogger {
    File logfile = null;
    FileWriter fw = null;
    public PersistentLogger() throws IOException {
        logfile = Environment.getExternalStoragePublicDirectory("nearby_connect_log_" + (new Date()).toString() + ".txt");
        fw = new FileWriter(logfile);
    }

    public void write(String msg){
        Log.d("nearby_connect", msg);
        Date now = new Date();
        String log_msg = now.toString() + ", " + msg + "\n";
        try {
            fw.write(log_msg);
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
