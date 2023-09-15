package com.bluestone.scienceexplorer.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bluestone.scenceexplorer.R
import com.bluestone.scenceexplorer.databinding.ServerErrorDialogBinding
import com.bluestone.scienceexplorer.network.ServerErrorResponse

class ServerErrorDialog(private val data: ServerErrorResponse, private val onDismissCall:( () -> Unit)? = null): DialogFragment() {
    private lateinit var dlg: Dialog
    private lateinit var binding: ServerErrorDialogBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val builder = AlertDialog.Builder(requireActivity())
        binding = ServerErrorDialogBinding.inflate(inflater)
        isCancelable = true
        dlg = builder.create()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.txtErrorText.text = data.error_text
        binding.txtErrorDate.text = data.date_time
        binding.txtErrorCode.text = data.error_code.toString()
        binding.txtUserAdvise?.text = getString(R.string.error_server_connection)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissCall?.let {
            it()
        }
    }
}