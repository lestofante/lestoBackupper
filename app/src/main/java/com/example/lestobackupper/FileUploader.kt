package com.example.lestobackupper

import android.content.Context
import androidx.room.Room
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.lang.Exception
import java.nio.file.Path
import java.nio.file.Paths

class FileUploader(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {

    private fun upload(server: String, file:Path): kotlin.Result<Nothing>{
        return kotlin.Result.failure(Exception("lol"))
    }

    override fun doWork(): Result {
/*
        // get the whole list of files
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "processDB"
        ).build()

        val fileTable = db.fileDao()
*/
        val filePath = inputData.getString("file_path")!!
        val server = inputData.getString("server")!!
/*
        val f = FileUploadStatus(Paths.get(filePath), Status.UPLOADING)
        if (fileTable.tryInsert(f).isFailure){
            return Result.failure()
        }
*/
        //actually upload file
        if (upload(server, Paths.get(filePath)).isFailure)
            return Result.failure()
/*
        // set file as uploaded
        if (fileTable.setUploaded(f).isFailure)
            return Result.failure()
*/
        //calculate file checksum
        val checksum = "asd"

        //download file checksum
        val uploadedChecksum = "dsa"

        if (checksum != uploadedChecksum)
            return Result.failure()
/*
        if (fileTable.setVerified(f).isFailure)
            return Result.failure()
*/
        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}
