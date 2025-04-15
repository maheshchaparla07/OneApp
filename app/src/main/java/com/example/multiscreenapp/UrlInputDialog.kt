package com.example.multiscreenapp

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment

class UrlInputDialog : DialogFragment() {
    interface OnUrlAddedListener {
        fun onUrlAdded(url: String, name: String)
    }

    interface QRScanListener {
        fun onScanRequested()
    }

    private var listener: OnUrlAddedListener? = null
    private var qrListener: QRScanListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Try to get listeners from parent fragment first, then activity
        listener = parentFragment as? OnUrlAddedListener ?: activity as? OnUrlAddedListener
        qrListener = parentFragment as? QRScanListener ?: activity as? QRScanListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_url_input, null)
        val urlEditText = view.findViewById<EditText>(R.id.urlEditText)
        val nameEditText = view.findViewById<EditText>(R.id.nameEditText)
        val scanQrButton = view.findViewById<Button>(R.id.scanQrButton)
        val submitButton = view.findViewById<Button>(R.id.submitButton)

        // Pre-fill URL if available
        arguments?.getString("prefilledUrl")?.let {
            urlEditText.setText(it)
        }

        scanQrButton.setOnClickListener {
            qrListener?.onScanRequested()
            dismiss()
        }

        submitButton.setOnClickListener {
            val url = urlEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()

            if (url.isNotEmpty()) {
                listener?.onUrlAdded(
                    if (url.startsWith("http")) url else "https://$url",
                    if (name.isNotEmpty()) name else "Web App"
                )
                dismiss()
            } else {
                urlEditText.error = "Please enter a URL"
            }
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("Add Web App")
            .create()
    }

    companion object {
        fun newInstance(prefilledUrl: String = ""): UrlInputDialog {
            return UrlInputDialog().apply {
                arguments = Bundle().apply {
                    putString("prefilledUrl", prefilledUrl)
                }
            }
        }
    }
}