package com.example.lestobackupper

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class FileUploader(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {

    private fun upload(id: String, server: String, baseFileURI: String, uri: Uri): kotlin.Result<Unit>{
        Log.d("FileUploader", "opening file: $uri")
        try {
            val query = applicationContext.contentResolver.query(uri, null, null, null, null)!!
            //val nameIndex = query.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = query.getColumnIndex(OpenableColumns.SIZE)
            query.moveToFirst()
            val fileSize = query.getLong(sizeIndex)
            query.close()

            val inputStream = applicationContext.contentResolver.openInputStream(uri)
            inputStream?.let { inputStream ->
                //open a socket with the server
                java.net.Socket(server, 9999).use {
                    val client = it
                    val input = java.io.DataInputStream(client.getInputStream())

                    val out = java.io.DataOutputStream(client.outputStream)
                    // print my id
                    val id = id.toByteArray()
                    out.writeInt(id.size)
                    out.write(id)

                    // print my base path
                    val basePath = baseFileURI.toByteArray()
                    out.writeInt(basePath.size)
                    out.write(basePath)

                    //then print the path
                    val path = uri.toString().toByteArray();
                    out.writeInt(path.size)
                    out.write(path)

                    //then print the file size, followed by the file itself
                    out.writeLong(fileSize)

                    // send the file while calculating the checksum
                    val checkSumSha256 = java.security.MessageDigest.getInstance("SHA-256")
                    val bufferSize = client.receiveBufferSize;
                    val buffer = ByteArray(bufferSize)

                    run{
                        val start = System.currentTimeMillis()
                        var bytes = inputStream.read(buffer)
                        while (bytes >= 0) {
                            checkSumSha256.update(buffer, 0, bytes)
                            bytes = inputStream.read(buffer)
                        }
                        val end = System.currentTimeMillis()
                        Log.d(
                            "upload",
                            "hashing B: " + fileSize + " duration S " + (end / 1000.0 - start / 1000.0)
                        )
                    }
                    // get calculated checksum locally, than print it
                    val hashCalculated = checkSumSha256.digest()
                    out.write(hashCalculated)

                    // get remote status
                    val uploadRequested = input.readBoolean()

                    var result = false
                    if (uploadRequested) {
                        // rewind to the beginning of the file
                        val inputStream = applicationContext.contentResolver.openInputStream(uri)!!

                        val start = System.currentTimeMillis()
                        var bytes = inputStream.read(buffer)
                        var bytesCopied: Long = 0
                        while (bytes >= 0) {
                            out.write(buffer, 0, bytes)
                            bytesCopied += bytes
                            bytes = inputStream.read(buffer)
                        }
                        val end = System.currentTimeMillis()
                        Log.d(
                            "upload",
                            "uploading B: " + fileSize + " duration S " + (end / 1000.0 - start / 1000.0)
                        )

                        // get remote status
                        if (input.readBoolean()) {
                            result = true
                        }
                    } else {
                        // remote already have the file, ignore upload
                        result = true
                    }

                    if (result) {
                        //here we can eventually delete or move the original file
                        return kotlin.Result.success(Unit)
                    }
                }

            }
        }catch (e: Exception){
            e.printStackTrace();
            return kotlin.Result.failure(Exception("fail"))
        }
        return kotlin.Result.failure(Exception("fail2"))
    }

    override fun doWork(): Result {
        val id = inputData.getString("id")!!
        val baseFileURI = inputData.getString("base_file_path")!!
        val fileURI = inputData.getString("file_path")!!
        val server = inputData.getString("server")!!

        Log.d("FileUploader", "start upload for $fileURI on $server");

        //actually upload file
        if (upload(id, server, baseFileURI, Uri.parse(fileURI)).isFailure)
            return Result.failure()

        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}
