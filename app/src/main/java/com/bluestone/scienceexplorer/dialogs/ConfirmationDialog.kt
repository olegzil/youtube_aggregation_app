package com.bluestone.scienceexplorer.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.bluestone.scenceexplorer.databinding.ConfirmationDialogBinding
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class ConfirmationDialog(private val activity: AppCompatActivity): DialogFragment() {
    enum class Response {
        eCANCEL,
        eOK,
        eDISMISS
    }
    lateinit var title: String
    lateinit var message: String
    private lateinit var dlg: Dialog
    private lateinit var binding: ConfirmationDialogBinding
    private val response: Channel<Response> = Channel(Channel.CONFLATED)
    fun populate(title:String, message: String): ReceiveChannel<Response> {
        this.title = title
        this.message = message
        return response
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ConfirmationDialogBinding.inflate(inflater)
        binding.dlgConfirmTitle.text = title
        binding.dlgConfirmMessage.text = message
        isCancelable = true

        setup()
        return binding.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        response.trySend(Response.eDISMISS)
        super.onDismiss(dialog)
    }
    private fun setup() {
        binding.cancelBtn.setOnClickListener {
            response.trySend(Response.eCANCEL)
            dismiss()
        }
        binding.okBtn.setOnClickListener {
            response.trySend(Response.eOK)
            dismiss()
        }
    }
}