package com.example.lestobackupper

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat.getExternalFilesDirs
import androidx.documentfile.provider.DocumentFile
import androidx.work.*
import java.io.File

fun upload(context: Context): ListenableWorker.Result {

    val pref = context?.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val path = pref.getString("path", null)
    val serverIP = pref.getString("serverIP", null)

    Log.w("upload", "sweeping 1 "+path + " "+getExternalFilesDirs(context, null))

    if (path == null || serverIP == null){
        Log.w("upload", "invalid path or server ip, stopping sweep")
        return ListenableWorker.Result.failure()
    }

    Log.w("upload", "sweeping 2 "+path)

    val pickedDir = DocumentFile.fromTreeUri(context, Uri.parse(path)!!)
    for (it in pickedDir!!.listFiles()) {
       // Log.d("lol", "Found file " + file.name + " with size " + file.length())
    //}
    //File(path).walkTopDown().forEach {

        Log.w("upload", "uploading "+it)/*
        val data = Data.Builder()
        data.putString("file_path", it.absolutePath)
        data.putString("server", serverIP)
        val fileUploader: OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<FileUploader>()
                .setInputData(data.build())
                .build()
        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(serverIP+" "+it.absolutePath, ExistingWorkPolicy.KEEP, fileUploader)*/
    }

    return ListenableWorker.Result.success()
}

class FilesystemSweep(private val context: Context, workerParams: WorkerParameters):
    Worker(context, workerParams) {
    override fun doWork(): Result {
        return upload(context)
    }
}
