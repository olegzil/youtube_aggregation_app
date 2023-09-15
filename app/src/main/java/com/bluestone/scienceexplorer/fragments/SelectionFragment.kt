package com.bluestone.scienceexplorer.fragments

import android.content.Intent
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bluestone.scenceexplorer.R
import com.bluestone.scenceexplorer.databinding.SelectionFragmentBinding
import com.bluestone.scienceexplorer.MainActivity
import com.bluestone.scienceexplorer.YoutubeActivity
import com.bluestone.scienceexplorer.adapters.VideoThumbnailAdapter
import com.bluestone.scienceexplorer.constants.INTENT_KEY_YOUTUBE_DATA
import com.bluestone.scienceexplorer.constants.TAG
import com.bluestone.scienceexplorer.database.Cache
import com.bluestone.scienceexplorer.database.SimpleChannelObserver
import com.bluestone.scienceexplorer.dataclasses.DirectoryrecordsItem
import com.bluestone.scienceexplorer.dataclasses.FetchResponse
import com.bluestone.scienceexplorer.dataclasses.MenuFetchResult
import com.bluestone.scienceexplorer.dataclasses.SelectedChannel
import com.bluestone.scienceexplorer.dataclasses.VideoSelectionMenuDescriptor
import com.bluestone.scienceexplorer.dataclasses.VideoThumbnailItem
import com.bluestone.scienceexplorer.network.NetworkMonitorProcessor
import com.bluestone.scienceexplorer.network.ServerErrorResponse
import com.bluestone.scienceexplorer.network.ServerResponse
import com.bluestone.scienceexplorer.uitilities.PreferenceManager
import com.bluestone.scienceexplorer.uitilities.checkGooglePlayServices
import com.bluestone.scienceexplorer.uitilities.cleanupName
import com.bluestone.scienceexplorer.uitilities.computeThumbMetrics
import com.bluestone.scienceexplorer.uitilities.parcelable
import com.bluestone.scienceexplorer.uitilities.updateTitle
import com.bluestone.scienceexplorer.viewmodels.VideoFragmentViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import timber.log.Timber


class SelectionFragment : Fragment() {
    private var _binding: SelectionFragmentBinding? = null
    private lateinit var adapter: VideoThumbnailAdapter
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var viewModel: VideoFragmentViewModel
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager
    private val networkMonitorProcessor: NetworkMonitorProcessor by lazy { initializeNetworkMonitor() }
    private lateinit var menuHost: MenuHost
    private lateinit var menuProvider: MenuProvider
    private val menuActions = mutableMapOf<Int, VideoSelectionMenuDescriptor>()
    private val _lastSelected = MutableSharedFlow<SelectedChannel>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val _lastCount =
        MutableSharedFlow<Long>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val lastSelected: SharedFlow<SelectedChannel> = _lastSelected
    private val lastCount: SharedFlow<Long> = _lastCount
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menuHost = requireActivity()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SelectionFragmentBinding.inflate(inflater, container, false)
        val v: VideoFragmentViewModel by viewModels()
        viewModel = v
        initializeNetworkMonitor()
        setupAdapter()

        preferenceManager = PreferenceManager(requireContext())
        setupPullDownToRefresh()
        monitorVideoSelection()
        monitorTitleUpdate()
        monitorMenuBackArrow()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val networkAvailable = networkMonitorProcessor.isNetworkAvailable()
        toggleNetworkError(networkAvailable)
        if (networkAvailable)
            arguments?.parcelable<SelectedChannel>("SelectedChannel")?.let { selectedChannel ->
                with(selectedChannel) {
                    preferenceManager.putTitle(channelTitle)
                    preferenceManager.putChannelID(channelID)
                    retrieveAvailableVideoTitles(clientID, channelTitle)
                }
                scrollToLastPosition()
            }
    }

    private fun monitorMenuUpdates() {
        lifecycleScope.launch {
            populateMenu().receiveAsFlow().onEach {
                view?.let {
                    setupMenu()
                }
            }.collect()
        }
    }

    private fun toggleNetworkError(networkAvailable: Boolean) {
        if (_binding == null)
            return
        if (!networkAvailable) {
            binding.menueFragmentSwipeRefreshLayout.visibility = View.GONE
            binding.frameSelectionNetworkError.visibility = View.VISIBLE
            binding.txtNetworkError.text = getString(R.string.error_device_offline)
        } else {
            binding.menueFragmentSwipeRefreshLayout.visibility = View.VISIBLE
            binding.frameSelectionNetworkError.visibility = View.GONE
            binding.txtNetworkError.text = ""
        }
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

    private fun monitorTitleUpdate() {
        lifecycleScope.launch {
            lastSelected.combine(lastCount) { selected, count ->
                updateTitle(requireActivity() as AppCompatActivity, selected.channelTitle, count)
                preferenceManager.putTitle(selected.channelTitle)
                preferenceManager.putVideoCount(count)
            }.collect()
        }
    }

    private fun monitorVideoSelection() {
        lifecycleScope.launch {
            VideoThumbnailAdapter.selectedVideo.receiveAsFlow().onEach {
                if (checkGooglePlayServices(requireActivity())) {
                    preferenceManager.putRecyclerPosition(it.first)
                    val intent = Intent(activity, YoutubeActivity::class.java)
                    intent.putExtra(INTENT_KEY_YOUTUBE_DATA, it.second)
                    startActivity(intent)
                }
            }.collect()
        }
    }

    private fun monitorMenuBackArrow() {
        lifecycleScope.launch {
            MainActivity.homeNaviation.receiveAsFlow().onEach {
                findNavController().navigateUp()
            }.collect()
        }
    }

    private suspend fun monitorDataFetch(
        channelID: String,
        dataSource: FetchResponse<VideoThumbnailItem, ServerErrorResponse>
    ): Channel<SimpleChannelObserver> {
        val observer = Channel<SimpleChannelObserver>(capacity = Channel.CONFLATED)
        var started = false
        while (select {
                dataSource.onComplete.onReceive {
                    observer.trySend(SimpleChannelObserver.eFINISHED_SUCCESS)
                    Timber.tag(TAG).i("Received $it  items")
                    preferenceManager.putChannelID(channelID)
                    _lastCount.tryEmit(it)
                    false
                }
                dataSource.onFailure.onReceive {
                    observer.trySend(SimpleChannelObserver.eFINISHED_FAILURE)
                    Timber.tag(TAG).i("Error:  $it")
                    val channelTitle = preferenceManager.getTitle()
                    val message =
                        resources.getString(R.string.channel_unavailable).format(channelTitle)
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    false
                }
                dataSource.onSuccess.onReceive {
                    if (!started) {
                        observer.trySend(SimpleChannelObserver.eSTARTED)
                        started = true
                    }
                    true
                }
            }) {/*empty body*/
        }
        return observer
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun scrollToLastPosition() {
        var pos = preferenceManager.getRecyclerPosition()
        if (pos < 0)
            pos = 0
        val layoutManager = binding.recyclerView.layoutManager
        layoutManager?.postOnAnimation {
            binding.recyclerView.smoothScrollToPosition(pos)
        }
    }

    private fun setupAdapter() {
        adapter = VideoThumbnailAdapter()
        binding.recyclerView.adapter = adapter
        layoutManager = GridLayoutManager(
            requireContext(),
            computeThumbMetrics(resources).numberOfRows,
            RecyclerView.VERTICAL,
            false
        )
        binding.recyclerView.layoutManager = layoutManager
    }

    private fun setupPullDownToRefresh() {
        if (binding.menueFragmentSwipeRefreshLayout.isRefreshing) {
            binding.menueFragmentSwipeRefreshLayout.setOnRefreshListener {
                binding.menueFragmentSwipeRefreshLayout.isRefreshing = false
            }
            return
        }
        binding.menueFragmentSwipeRefreshLayout.setOnRefreshListener {
            val channelID = preferenceManager.getChannelID()
            val clientID = Cache.getUniqueClientID()
            forceFetchChannelData(clientID, channelID)
        }
    }

    private fun forceFetchChannelData(clientID: String, channelID: String) {
        val response = Cache.forceFetchAvailableVideo(clientID, channelID)
        lifecycleScope.launch {
            adapter.clear()
            val retVal = populateAdapter(response)
            monitorDataFetch(channelID, retVal).receiveAsFlow().onEach {
                when (it) {
                    SimpleChannelObserver.eSTARTED -> {
                        showProgressBar(true)
                    }

                    SimpleChannelObserver.eFINISHED_SUCCESS,
                    SimpleChannelObserver.eFINISHED_FAILURE -> {
                        binding.menueFragmentSwipeRefreshLayout.isRefreshing = false
                        showProgressBar(false)
                    }

                    else -> {
                        //No-op
                    }
                }
            }.collect()
        }
    }

    private fun fetchChannelData(
        clientID: String,
        channelID: String,
    ) {
        adapter.clear()
        val response = Cache.fetchAllVideo(clientID, channelID)
        lifecycleScope.launch {
            adapter.clear()
            val retVal = populateAdapter(response)
            monitorDataFetch(channelID, retVal).receiveAsFlow().onEach {
                when (it) {
                    SimpleChannelObserver.eSTARTED -> {
                        showProgressBar(true)
                    }

                    SimpleChannelObserver.eFINISHED_SUCCESS,
                    SimpleChannelObserver.eFINISHED_FAILURE -> {
                        binding.menueFragmentSwipeRefreshLayout.isRefreshing = false
                        showProgressBar(false)
                    }

                    else -> {
                        //No-op
                    }
                }
            }.collect()

        }
    }

    private suspend fun populateAdapter(result: FetchResponse<VideoThumbnailItem, ServerErrorResponse>): FetchResponse<VideoThumbnailItem, ServerErrorResponse> {
        val retVal = FetchResponse<VideoThumbnailItem, ServerErrorResponse>()
        while (select {
                result.onSuccess.onReceive { videoItem ->
                    videoItem.title = cleanupName(videoItem.title)
                    adapter.update(videoItem)
                    retVal.onSuccess.trySend(videoItem)
                    true
                }
                result.onComplete.onReceive { count ->
                    Timber.tag(TAG).i("Received $count items")
                    retVal.onComplete.trySend(count)
                    false
                }
                result.onFailure.onReceive { error ->
                    val message: String = "error: %s\ncode: %d\ntime: %s".format(
                        error.error_text,
                        error.error_code,
                        error.date_time
                    )
                    binding.txtErrorMessage.text = message
                    binding.txtErrorMessage.visibility = View.VISIBLE
                    retVal.onFailure.trySend(error)
                    false
                }
            }) {/*empty body*/
        }
        return retVal
    }

    private fun setupMenu() {
        menuProvider = object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
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
        binding.txtErrorMessage.visibility = View.GONE
        return if (menuActions.containsKey(menuId)) {
            menuActions[menuId]?.let { menuDescriptor ->
                (menuDescriptor.callback)(menuDescriptor.channelID)
            }
            true
        } else
            false
    }

    private suspend fun populateMenu(): ReceiveChannel<MenuFetchResult> {
        val retVal = Channel<MenuFetchResult>(capacity = Channel.CONFLATED)
        menuActions.clear()
        val response = Cache.fetchAvailableVideoList(Cache.getUniqueClientID())
        lifecycleScope.launch {
            var menuIndex = 100
            while (select {
                    response.onSuccess.onReceive { channelDirectory ->
                        val workName = channelDirectory.name.split("_")
                        val name =
                            workName.joinToString(separator = " ") { word -> word.replaceFirstChar { it.uppercase() } }
                        menuActions[menuIndex] = VideoSelectionMenuDescriptor(
                            menuIndex,
                            name,
                            channelDirectory.client,
                            channelDirectory.channel_id
                        ) { channelId ->
                            showProgressBar(true)
                            fetchChannelData(channelDirectory.client, channelId)
                            _lastSelected.tryEmit(
                                SelectedChannel(
                                    channelDirectory.client,
                                    channelId,
                                    name
                                )
                            )
                        }
                        menuIndex += 10
                        true
                    }
                    response.onComplete.onReceive {
                        retVal.trySend(MenuFetchResult(success = true, menuActions))
                        false
                    }
                    response.onFailure.onReceive {
                        retVal.trySend(
                            MenuFetchResult(
                                false,
                                menuActions,
                                ServerResponse(error = it)
                            )
                        )
                        false
                    }
                }) {/*empty body*/
            }
        }
        return retVal
    }

    private fun retrieveAvailableVideoTitles(clientID: String, channelTitle: String) {
        binding.txtErrorMessage.visibility = View.GONE
        lifecycleScope.launch {
            showProgressBar(true)
            val fetchResult =
                Cache.fetchAvailableVideoList(Cache.getUniqueClientID())
            val directoryList = mutableListOf<DirectoryrecordsItem>()
            while (select {
                    fetchResult.onSuccess.onReceive {
                        directoryList.add(it)
                        true
                    }
                    fetchResult.onFailure.onReceive { error ->
                        binding.txtErrorMessage.text = error.error_text
                        showProgressBar(false)
                        false
                    }
                    fetchResult.onComplete.onReceive {
                        val channelID = preferenceManager.getChannelID()
                        val result = Cache.fetchAllVideo(clientID, channelID)
                        val retVal = populateAdapter(result)
                        monitorDataFetch(channelID, retVal).receiveAsFlow().onEach {
                            when (it) {
                                SimpleChannelObserver.eSTARTED -> {
                                    showProgressBar(true)
                                }

                                SimpleChannelObserver.eFINISHED_SUCCESS,
                                SimpleChannelObserver.eFINISHED_FAILURE -> {
                                    binding.menueFragmentSwipeRefreshLayout.isRefreshing = false
                                    showProgressBar(false)
                                }

                                else -> {
                                    //No-op
                                }
                            }
                        }.collect()
                        _lastSelected.tryEmit(SelectedChannel(clientID, channelID, channelTitle))
                        monitorMenuUpdates()
                        false
                    }
                }) {/*empty body*/
            }
        }
    }
    private fun showProgressBar(show: Boolean) {
        if (show) {
            binding.selectionFragmentProgressBar.visibility = View.VISIBLE
            binding.menueFragmentSwipeRefreshLayout.isRefreshing = true
        } else {
            binding.selectionFragmentProgressBar.visibility = View.GONE
            binding.menueFragmentSwipeRefreshLayout.isRefreshing = false
        }
    }
}
