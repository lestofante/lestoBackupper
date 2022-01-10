package com.example.lestobackupper

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.work.*

class FileSystemUploader(private val context: Context, private val serverIP: String){
    private fun uploadFile(client_id: String, baseDir: DocumentFile, document: DocumentFile) {

        Log.d("FileSystemUploader", "uploading " + document.uri);

        val data = Data.Builder()
        data.putString("id", client_id)
        data.putString("server", serverIP)
        data.putString("base_file_path", baseDir.uri.toString())
        data.putString("file_path", document.uri.toString())

        val fileUploader: OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<FileUploader>()
                .setInputData(data.build())
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniqueWork("$serverIP $document", ExistingWorkPolicy.KEEP, fileUploader)
    }

    private fun listFilesRecursive(id: String, baseDir: DocumentFile, pickedDir: DocumentFile) {
        for (it in pickedDir.listFiles()) {
            if (it.isDirectory) {
                listFilesRecursive(id, baseDir, it)
            }else {
                uploadFile(id, baseDir, it)
            }
        }
    }

    fun upload(context: Context): ListenableWorker.Result {
        val pref = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val id = pref.getString("id", null)
        val uriStr = pref.getString("path", null)
        val serverIP = pref.getString("serverIP", null)

        Log.d("upload", "sweeping 1 $uriStr")

        if (id == null || uriStr == null || serverIP == null){
            Log.d("upload", "invalid path or server ip, stopping sweep")
            return ListenableWorker.Result.failure()
        }

        val uri = Uri.parse(uriStr)!!
        Log.d("upload", "sweeping 2 $uri")

        val pickedDir = DocumentFile.fromTreeUri(context, uri)!!
        listFilesRecursive(id, pickedDir, pickedDir)

        return ListenableWorker.Result.success()
    }
}

class FilesystemSweep(private val context: Context, workerParams: WorkerParameters, private val serverIP: String
):
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val uploader = FileSystemUploader(context, serverIP);
        return uploader.upload(context)
    }
}
