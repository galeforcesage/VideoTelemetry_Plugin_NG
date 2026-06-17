package com.sagetv.ng.telemetry;

public final class ServerIntegrationNotes {
    private ServerIntegrationNotes() {
    }

    // Plugin event names exposed through SageTVPluginRegistry and SageTVEventListener.
    public static final String EVENT_PLAYBACK_STARTED = "PlaybackStarted";
    public static final String EVENT_PLAYBACK_STOPPED = "PlaybackStopped";
    public static final String EVENT_PLAYBACK_FINISHED = "PlaybackFinished";
    public static final String EVENT_MEDIA_FILE_REMOVED = "MediaFileRemoved";

    // UI hook and API names observed in SageTV-mine for playback/caption integration planning.
    public static final String UI_HOOK_MEDIA_PLAYER_FILE_LOAD_COMPLETE = "MediaPlayerFileLoadComplete";
    public static final String UI_HOOK_FILE_PLAYBACK_FINISHED = "FilePlaybackFinished";
    public static final String API_GET_CC_STATE = "GetMediaPlayerClosedCaptionState";
    public static final String API_GET_CC_STATE_LABEL = "GetMediaPlayerClosedCaptionStateLabel";

    // Shared-core candidate (not implemented in this plugin): HEVC 10-bit capability discovery.
    public static final String SHARED_CORE_CAPABILITY_HEVC_10BIT = "hevc_10bit_capability";
}
