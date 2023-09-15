package com.bluestone.scienceexplorer.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bluestone.scenceexplorer.R
import com.bluestone.scenceexplorer.databinding.HelpDialogBinding

class HelpDialog: DialogFragment()  {
    private lateinit var dlg: Dialog
    private lateinit var binding: HelpDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val builder = AlertDialog.Builder(requireActivity())
        dlg = builder.create()
        binding = HelpDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.txtHelpMsg.text = resources.getString(R.string.main_screen_help)
        binding.btnOk.setOnClickListener {
            dismiss()
        }
    }
}