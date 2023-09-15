package com.bluestone.scienceexplorer.constants
const val TAG = "_SCIENCE_"
const val LOCAL_IP = "http://10.0.0.3:8083/"
const val GOOGLE_IP ="http://35.208.173.235:8080/"
const val ACTIVE_IP = GOOGLE_IP
const val YOUTUBE_BASE_LINK_URL = "https://youtu.be/"
const val YOUTUBE_BASE_URL = ACTIVE_IP
const val YOUTUBE_PLAYLIST_BASE_URL = "https://youtube.com/"
const val YOUTUBE_VIDEO_BASE_URL = "https://www.youtube.com/watch?v="
const val ACTIVE_CHANNEL_KEY = "UC7_gcs09iThXybpVgjHZ_7g"
const val ServerRequestTimeOut = 60L * 10L //in seconds
const val SERVER_REQUEST_CLIENT_DIRECTORY = "fetchclientdirectory" //Requests a list of channel ids and associated data for specific client
const val SERVER_RESTORE_DEFAULT_CHANNELS = "restoredefaultclient"
const val SERVER_REQUEST_CHANNEL_VIDEOS = "fetchchannelvideos"
const val SERVER_ADD_VIDEO_LINK = "addvideolink"
const val SERVER_LINK_STATUS = "linkstatus"
const val SERVER_FETCH_VIDEO_DATA = "fetch_video_data"
const val SERVER_UPDATE_CHANNEL = "updatechannel"
const val SERVER_DELETE_CHANNEL = "deletechannel"
const val INTENT_KEY_YOUTUBE_DATA = "SelectedChannel"
const val INTENT_KEY_VIDEO_LINK = "videolink"
const val INTENT_KEY_VIDEO_PLAYLIST = "playlist"
const val SHARED_LINK_URL = "https://img.youtube.com/vi/%s/default.jpg"
const val SHARED_PLAYLIST_URL = "https://youtube.com/playlist?list=%s&playnext=1"
/** The player view cannot be smaller than 110 pixels high.  */
const val PLAYER_VIEW_MINIMUM_HEIGHT_DP = 110f
const val MAX_NUMBER_OF_ROWS_WANTED = 4
const val INTER_IMAGE_PADDING_DP = 5
// YouTube thumbnails have a 16 / 9 aspect ratio
const val THUMBNAIL_ASPECT_RATIO = 16 / 9.0
const val SWIPE_DIRECTION_RIGHT = 32
const val SWIPE_DIRECTION_LEFT = 16
const val LINK_INVALID = 0xFFAB00
const val LINK_EXISTS = 0xFFAB01
const val LINK_NOT_FOUND = 0xFFAB05
const val LINK_REQUEST_UNKNOWN_ERROR = 0xFFAB03

