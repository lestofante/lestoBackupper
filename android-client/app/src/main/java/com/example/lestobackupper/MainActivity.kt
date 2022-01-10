package com.example.lestobackupper

import android.R.attr
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
import android.R.attr.data
import android.app.Activity


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

        FolderListener.scheduleJob(baseContext)
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
                43 -> {
                    val contents = resultData!!.getStringExtra("SCAN_RESULT")
                    Log.d("onActivityResult", "scanned QR was: $contents")
                }
                else -> {
                    Log.e("onActivityResult", "unknown requestCode: " + requestCode + " " + resultData!!.data);
                }
            }

        }
    }
}