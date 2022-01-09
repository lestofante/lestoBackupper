package com.example.lestobackupper

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class FileUploader(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {

    private fun upload(server: String, uri:Uri): kotlin.Result<Unit>{
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

                    val out = java.io.DataOutputStream(client.outputStream)
                    // print my id
                    val id = "ciao".toByteArray()
                    out.writeInt(id.size)
                    out.write(id)

                    // print my base path
                    val basePath = "base".toByteArray()
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

                    var bytesCopied: Long = 0
                    val buffer = ByteArray(bufferSize)
                    var bytes = inputStream.read(buffer)
                    while (bytes >= 0) {
                        out.write(buffer, 0, bytes)
                        checkSumSha256.update(buffer, 0, bytes)
                        bytesCopied += bytes
                        bytes = inputStream.read(buffer)
                    }

                    // get calculated checksum locally
                    val hashCalculated = checkSumSha256.digest()

                    // get remote checksum
                    val hashReceived = ByteArray(32)
                    java.io.DataInputStream(client.getInputStream()).readFully(hashReceived)

                    // check the checksum validity, send answer back and cleanup
                    val result = java.util.Arrays.equals(hashReceived, hashCalculated)

                    out.writeBoolean(result)
                    if (result) {
                        //here we can eventually delete or move the original file
                        return kotlin.Result.success(Unit)
                    }else{
                        Log.d("uploader", "\nhashReceived " + hashReceived.contentToString() + "\nhashCalculated " + hashCalculated.contentToString() + "\nbytesCopied $bytesCopied File size: $fileSize")
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
        val fileURI = inputData.getString("file_path")!!
        val server = inputData.getString("server")!!

        Log.d("FileUploader", "start upload for $fileURI on $server");

        //actually upload file
        if (upload(server, Uri.parse(fileURI)).isFailure)
            return Result.failure()

        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}
