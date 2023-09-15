package com.bluestone.scienceexplorer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bluestone.scenceexplorer.R
import com.bluestone.scenceexplorer.databinding.MenuFragmentBinding
import com.bluestone.scienceexplorer.adapters.MenuFragmentAdapter
import com.bluestone.scienceexplorer.adapters.SimpleItemTouchHelperCallback
import com.bluestone.scienceexplorer.constants.TAG
import com.bluestone.scienceexplorer.database.Cache
import com.bluestone.scienceexplorer.database.SimpleChannelObserver
import com.bluestone.scienceexplorer.dataclasses.ChannelSelectionMenuDescriptor
import com.bluestone.scienceexplorer.dataclasses.DirectoryrecordsItem
import com.bluestone.scienceexplorer.dataclasses.FetchResponse
import com.bluestone.scienceexplorer.dataclasses.SelectedChannel
import com.bluestone.scienceexplorer.dialogs.HelpDialog
import com.bluestone.scienceexplorer.dialogs.ServerErrorDialog
import com.bluestone.scienceexplorer.network.NetworkMonitorProcessor
import com.bluestone.scienceexplorer.network.ServerErrorResponse
import com.bluestone.scienceexplorer.uitilities.PreferenceManager
import com.bluestone.scienceexplorer.uitilities.cleanupName
import com.bluestone.scienceexplorer.uitilities.clearTitle
import com.bluestone.scienceexplorer.uitilities.computeThumbMetrics
import com.bluestone.scienceexplorer.uitilities.updateTitle
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import timber.log.Timber

class MenuFragment : Fragment() {
    private lateinit var binding: MenuFragmentBinding
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var preferenceManager: PreferenceManager
    private val networkMonitorProcessor: NetworkMonitorProcessor by lazy { initializeNetworkMonitor() }
    private lateinit var adapter: MenuFragmentAdapter
    private lateinit var menuHost: MenuHost
    private lateinit var menuProvider: MenuProvider
    private val channelActionChannel = Channel<Boolean>(Channel.CONFLATED)
    private val menuActions = mutableMapOf<Int, ChannelSelectionMenuDescriptor>()
    private val helpDlg=HelpDialog()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager = PreferenceManager(requireContext())
        menuHost = requireActivity()
        initializeNetworkMonitor()
        monitorVideoSelection()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MenuFragmentBinding.inflate(inflater, container, false)
        clearTitle(lifecycleScope, requireActivity() as AppCompatActivity)
        setupPullDownToRefresh()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val networkAvailable = networkMonitorProcessor.isNetworkAvailable()
        toggleNetworkError(networkAvailable)
        setupAdapter()
        val response = Cache.fetchAvailableVideoList(Cache.getUniqueClientID())
        monitorDataFetch(populateAdapter(response))
        setupMenu()
        populateMenu()
    }

    private fun initializeNetworkMonitor(): NetworkMonitorProcessor {
        val networkLost = { toggleNetworkError(false) }
        val networkUnavailable = { toggleNetworkError(false) }
        val networkAvailable = { toggleNetworkError(true) }
        return NetworkMonitorProcessor(
            requireActivity() as AppCompatActivity,
            networkLost,
            networkUnavailable,
            networkAvailable
        )
    }

    private fun setupMenu() {
        menuProvider = object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
                menu.clear()
                menuActions.forEach { (_, value) ->
                    val item = menu.add(
                        Menu.NONE,
                        value.menuId,
                        Menu.NONE,
                        value.menuDescription
                    )
                    item.isEnabled = !value.isDisabled
                    item.isVisible = !value.isDisabled
                }
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
                menu.clear()
                menuActions.forEach { (_, value) ->
                    menu.add(
                        Menu.NONE,
                        value.menuId,
                        Menu.NONE,
                        value.menuDescription
                    )
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return executeMenu(menuItem.itemId)
            }
        }
        menuHost.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.STARTED)
    }

    private fun executeMenu(menuId: Int): Boolean {
        return if (menuActions.containsKey(menuId)) {
            menuActions[menuId]?.let { menuDescriptor ->
                (menuDescriptor.callback)(menuDescriptor.clientID)
            }
            true
        } else
            false
    }

    private fun updateMenuActions() {
        menuActions.clear()
        var menuIndex = 100
        menuActions[menuIndex] = ChannelSelectionMenuDescriptor(
            menuIndex,
            !preferenceManager.getIsChannelListModified(),
            getString(R.string.menu_name_restore_deleted_channels),
            Cache.getUniqueClientID(),
        ) { clientID ->
            val response = Cache.restoreDeletedChannels(clientID)
            monitorDataFetch(populateAdapter(response))
            preferenceManager.putIsChannelListModified(false)
        }
        menuIndex += 10
        menuActions[menuIndex] = ChannelSelectionMenuDescriptor(
            menuIndex,
            false,
            getString(R.string.menu_name_help),
            Cache.getUniqueClientID(),
        ) {
            helpDlg.show(parentFragmentManager, "help_dlg")
        }
    }

    private fun populateMenu() {
        updateMenuActions()
        var menuIndex: Int
        lifecycleScope.launch {
            channelActionChannel.receiveAsFlow().onEach {
                menuActions.clear()
                menuIndex = 0
                menuActions[menuIndex] = ChannelSelectionMenuDescriptor(
                    menuIndex,
                    false,
                    getString(R.string.menu_name_restore_deleted_channels),
                    Cache.getUniqueClientID(),
                ) { clientID ->
                    val response = Cache.restoreDeletedChannels(clientID)
                    monitorDataFetch(populateAdapter(response))
                    preferenceManager.putIsChannelListModified(false)
                    updateMenuActions()
                }
                menuIndex += 10
                menuActions[menuIndex] = ChannelSelectionMenuDescriptor(
                    menuIndex,
                    false,
                    getString(R.string.menu_name_help),
                    Cache.getUniqueClientID(),
                ) {
                    helpDlg.show(parentFragmentManager, "help_dlg")
                }
            }.collect()
        }
    }

    private fun monitorDataFetch(emitter: Channel<SimpleChannelObserver>) {
        lifecycleScope.launch {
            while (select {
                    emitter.onReceive {
                        when (it) {
                            SimpleChannelObserver.eSTARTED -> {
                                showProgressBar(true)
                                binding.menueFragmentSwipeRefreshLayout.isRefreshing = true
                                true
                            }

                            SimpleChannelObserver.eINPROGRESS -> {
                                true
                            }

                            SimpleChannelObserver.eFINISHED_SUCCESS -> {
                                binding.recyclerMenuProgressBar.visibility = View.GONE
                                binding.menueFragmentSwipeRefreshLayout.isRefreshing = false
                                false
                            }

                            SimpleChannelObserver.eFINISHED_FAILURE -> {
                                showProgressBar(false)
                                binding.menueFragmentSwipeRefreshLayout.isRefreshing = false
                                false
                            }
                        }
                    }
                }) {/*no body*/
            }
        }
    }

    private fun setupPullDownToRefresh() {
        if (binding.menueFragmentSwipeRefreshLayout.isRefreshing) {
            binding.menueFragmentSwipeRefreshLayout.setOnRefreshListener {
                binding.menueFragmentSwipeRefreshLayout.isRefreshing = false
            }
            return
        }
        binding.menueFragmentSwipeRefreshLayout.setOnRefreshListener {
            val response = Cache.fetchAvailableVideoList(Cache.getUniqueClientID())
            monitorDataFetch(populateAdapter(response))
        }
    }

    private fun toggleNetworkError(networkAvailable: Boolean) {
        if (!this::binding.isInitialized)
            return
        if (!networkAvailable) {
            binding.menueFragmentSwipeRefreshLayout.visibility = View.GONE
            binding.txtNetworkErrorMenu.text = getString(R.string.error_device_offline)
            binding.txtNetworkErrorMenu.visibility = View.VISIBLE
        } else {
            binding.menueFragmentSwipeRefreshLayout.visibility = View.VISIBLE
            binding.txtNetworkErrorMenu.visibility = View.GONE
            binding.txtNetworkErrorMenu.text = ""
        }
    }

    private fun setupAdapter() {
        adapter = MenuFragmentAdapter(binding.recyclerMenu)
        binding.recyclerMenu.setHasFixedSize(true)
        binding.recyclerMenu.adapter = adapter
        layoutManager = GridLayoutManager(
            requireContext(),
            computeThumbMetrics(resources).numberOfRows,
            RecyclerView.VERTICAL,
            false
        )
        val callback: ItemTouchHelper.Callback =
            SimpleItemTouchHelperCallback(
                activity as AppCompatActivity, adapter, channelActionChannel
            )
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.recyclerMenu)
        binding.recyclerMenu.layoutManager = layoutManager
    }

    private fun populateAdapter(response: FetchResponse<DirectoryrecordsItem, ServerErrorResponse>): Channel<SimpleChannelObserver> {
        val observer = Channel<SimpleChannelObserver>(
            onBufferOverflow = BufferOverflow.SUSPEND,
            capacity = Channel.CONFLATED
        )
        lifecycleScope.launch {
            while (select {
                    response.onFirstEmission.onReceive {
                        adapter.clear()
                        observer.trySend(SimpleChannelObserver.eSTARTED)
                        true
                    }
                    response.onSuccess.onReceive { directoryRecord ->
                        observer.trySend(SimpleChannelObserver.eINPROGRESS)
                        val adapterItem = SelectedChannel(
                            directoryRecord.client,
                            directoryRecord.channel_id,
                            cleanupName(directoryRecord.name),
                            directoryRecord.latest_video
                        )
                        adapter.update(adapterItem)
                        true
                    }
                    /*TODO:This code forces the app to exit on error. Exit on error should only happen during a bad share attempt. In
                        all other cases the error should be propagated to the caller and retrieved via call to translateServerError().
                        In the case that the client id is invalid, populateAdapter() should call the server with the default client id.
                        It is guaranteed to be valid. All other cased must be addressed separately.
                     */
                    response.onFailure.onReceive {
                        observer.trySend(SimpleChannelObserver.eFINISHED_FAILURE)
                        val dlg = ServerErrorDialog(it) { activity?.finishAndRemoveTask() }
                        dlg.show(parentFragmentManager, "__ServerErrorDialog__")
                        false
                    }
                    response.onComplete.onReceive {
                        observer.trySend(SimpleChannelObserver.eFINISHED_SUCCESS)
                        updateTitle(
                            requireActivity() as AppCompatActivity,
                            getString(R.string.menu_fragment_title),
                            it
                        )
                        false
                    }
                }) {/*no body*/
            }
        }
        return observer
    }

    private fun monitorVideoSelection() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                MenuFragmentAdapter.selectedChannel.receiveAsFlow().onEach { selectedChannel ->
                    val action = MenuFragmentDirections.actionMenuFragmentToSelectionFragment(
                        selectedChannel
                    )
                    Timber.tag(TAG).d("Received selection: %d", action.actionId)
                    findNavController().navigate(action)
                }.collect()
            }
        }
    }

    private fun showProgressBar(show: Boolean) {
        if (show)
            binding.recyclerMenuProgressBar.visibility = View.VISIBLE
        else
            binding.recyclerMenuProgressBar.visibility = View.GONE
    }
}