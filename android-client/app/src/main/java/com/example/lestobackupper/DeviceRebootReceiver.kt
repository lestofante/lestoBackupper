package com.example.lestobackupper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DeviceRebootReceiver : BroadcastReceiver() {
    override fun onReceive(context : Context?, intent : Intent?) {
        Log.w("DeviceRebootReceiver", "DeviceRebootReceiver STARTED")
        if (context != null) {
            runFilesystemSweep(context)
        }else{
            Log.e(this.javaClass.name, "NULL context")
        }
    }
}