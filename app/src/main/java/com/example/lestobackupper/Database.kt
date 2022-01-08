package com.example.lestobackupper

import android.content.Context
import android.util.Log
import androidx.room.*
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.nio.file.Path
import java.util.concurrent.TimeUnit

enum class Status{
    UPLOADING, CHECKING,  UPLOADED
}

@Entity
data class FileUploadStatus(
    @PrimaryKey val fullPath: Path,
    @ColumnInfo(name = "status") val status: Status,
)

@Dao
interface FileDao {
    @Query("SELECT * FROM fileuploadstatus")
    fun getAll(): List<FileUploadStatus>

    @Insert
    fun insertAll(vararg file: FileUploadStatus)

    @Delete
    fun delete(file: FileUploadStatus)

    @Insert
    fun tryInsert(f: FileUploadStatus): Result<Nothing> {
        return Result.failure(Exception("AlreadyExist"))
    }

    @Update
    fun setUploaded(f: FileUploadStatus): Result<Nothing> {
        return Result.failure(Exception("Not Found"))
    }

    @Update
    fun setVerified(f: FileUploadStatus): Result<Nothing> {
        return Result.failure(Exception("Not Found"))
    }
}

@Database(entities = [FileUploadStatus::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
}

fun runFilesystemSweep(context : Context){
    Log.w("runFilesystemSweep", "runFilesystemSweep ")
    val fileSystemListener: PeriodicWorkRequest =
        PeriodicWorkRequestBuilder<FilesystemSweep>(1, TimeUnit.MINUTES)
            .setInitialDelay(0, TimeUnit.MILLISECONDS)
            .build()

    WorkManager
        .getInstance(context)
        .enqueueUniquePeriodicWork("fileSystemListener", ExistingPeriodicWorkPolicy.KEEP, fileSystemListener)
}