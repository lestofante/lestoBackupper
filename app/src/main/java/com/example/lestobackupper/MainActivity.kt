package com.example.lestobackupper

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.example.lestobackupper.databinding.ActivityMainBinding
import java.io.File
import androidx.documentfile.provider.DocumentFile

import android.content.Intent
import android.net.Uri
import androidx.core.net.toFile
import androidx.fragment.app.FragmentActivity


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

        val pref = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = pref.edit()
        editor.putString("serverIP", "string value")
        editor.commit()

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
            val treeUri: Uri? = resultData!!.data
            val pickedDir = DocumentFile.fromTreeUri(this, treeUri!!)

            val pref = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = pref.edit()
            editor.putString("path", File(resultData!!.data!!.path).absolutePath)
            editor.commit()

            // List all existing files inside picked directory
            for (file in pickedDir!!.listFiles()) {
                Log.d("lol", "Found file " + file.name + " with size " + file.length())
            }

        }
    }
}