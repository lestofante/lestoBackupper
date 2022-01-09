package com.example.lestobackupper

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import com.example.lestobackupper.databinding.ActivityMainBinding
import java.io.File
import androidx.documentfile.provider.DocumentFile

import android.content.Intent
import android.net.Uri
import android.system.Os.socket
import java.net.MulticastSocket
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.MulticastLock
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.TimeoutException


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        val thread = Thread {

            val MCAST_ADDR = "255.255.255.255";
            val GROUP = InetAddress.getByName(MCAST_ADDR);
            val socket = java.net.DatagramSocket(4446)
            socket.broadcast = true

            var expected = "LestoBackupper0".toByteArray()
            var buf = ByteArray(expected.size)
            val packet = java.net.DatagramPacket(buf, buf.size)
            var received = false;
            val requestBuf = "LestoBackupper?".toByteArray()
            val request = java.net.DatagramPacket(requestBuf, requestBuf.size, GROUP, 4445)
            socket.soTimeout = 2000;   // set the timeout in milliseconds.
            while (!received) {
                socket.send(request)
                try {
                    while (true) {
                        // we are gonna read UDP broadcast until no answer come back for 2 seconds
                        socket.receive(packet)
                        val message = packet.data
                        Log.d("Autodiscovery", "found broadcast: " + String(message))
                        received = Arrays.equals(message, expected)
                        if (received) {
                            val senderIP = packet.address.hostAddress
                            Log.d("Autodiscovery", "found server: $senderIP")
                        }
                    }
                }catch(e: SocketTimeoutException){
                    Log.d("Autodiscovery", "server answer timeout")
                }
            }

            socket.close()
        }
        thread.start()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == RESULT_OK) {
            when(requestCode){
                 42 -> {
                    val treeUri: Uri = resultData!!.data!!

                    Log.w("onActivityResult", "treeUri: $treeUri");

                    // request persistent permission
                    val contentResolver = applicationContext.contentResolver
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    // Check for the freshest data.
                    contentResolver.takePersistableUriPermission(treeUri, takeFlags)

                    // save the path
                    val pref = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    val editor: SharedPreferences.Editor = pref.edit()
                    editor.putString("path", treeUri.toString())
                    editor.commit()
                }
                else -> {
                    Log.e("onActivityResult", "unknown requestCode: " + requestCode + " " + resultData!!.data);
                }
            }

        }
    }
}