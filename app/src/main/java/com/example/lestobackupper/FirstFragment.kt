package com.example.lestobackupper

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.lestobackupper.databinding.FragmentFirstBinding
import android.os.Environment
import android.os.Environment.getExternalStorageDirectory
import android.util.Log
import android.widget.TextView
import java.io.File
import androidx.core.app.ActivityCompat.startActivityForResult

import android.content.Intent





/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

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

        binding.buttonFirst.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, 42)

            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            Log.w("main", "first fragment click")


        }

        val textView = getView()?.findViewById<TextView>(R.id.textview_path)
        val pref = context?.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val path = pref!!.getString("path", null)
        textView?.text = path

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}