package com.example.lestobackupper

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.lestobackupper.databinding.FragmentFirstBinding

import android.content.Intent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.DialogInterface
import android.content.SharedPreferences
import android.net.Uri
import android.widget.EditText
import android.util.Log
import java.lang.Exception

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment(), MyRecyclerViewAdapter.ItemClickListener {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pref = context?.getSharedPreferences("settings", Context.MODE_PRIVATE)
        var serverIP = pref!!.getString("serverIP", null)!!

        val serverIPText = requireView().findViewById<EditText>(R.id.editTextServerIP)

        serverIPText.setText(serverIP)

        binding.buttonFirst.setOnClickListener {
            serverIP = serverIPText.text.toString()
            Log.d("", "setting serverIP to $serverIP")
            val editor: SharedPreferences.Editor = pref.edit()
            editor.putString("serverIP", serverIP)
            editor.putString("id", "cacca")
            editor.commit()

            val uploader = FileSystemUploader(requireContext(), serverIP);
            uploader.upload(requireContext())
        }

        binding.buttonAddPath.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            requireActivity().startActivityForResult(intent, 42)
        }

        binding.addServer.setOnClickListener{
            try {
                val intent = Intent("com.google.zxing.client.android.SCAN")
                intent.putExtra("SCAN_MODE", "QR_CODE_MODE") // "PRODUCT_MODE for bar codes
                requireActivity().startActivityForResult(intent, 43)
            } catch (e: Exception) {
                val marketUri: Uri = Uri.parse("market://details?id=com.google.zxing.client.android")
                val marketIntent = Intent(Intent.ACTION_VIEW, marketUri)
                startActivity(marketIntent)
            }
        }

        updateRv();

    }

    private fun updateRv(){
        val pref = context?.getSharedPreferences("settings", Context.MODE_PRIVATE)
        var path = pref!!.getString("path", "")!!
        val pathList = path.split(",")

        // set up the RecyclerView
        val recyclerView: RecyclerView = requireView().findViewById(R.id.rvPathList)!!
        recyclerView.layoutManager = LinearLayoutManager(context)
        val adapter = MyRecyclerViewAdapter(context, pathList)
        adapter.setClickListener(this)
        recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onItemClick(view: View?, position: Int) {
        val dialogClickListener =
            DialogInterface.OnClickListener { _, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                    }
                    DialogInterface.BUTTON_NEGATIVE -> {
                    }
                }
            }
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setMessage("Do you want to remove?").setPositiveButton("Yes", dialogClickListener)
            .setNegativeButton("No", dialogClickListener).show()
    }
}