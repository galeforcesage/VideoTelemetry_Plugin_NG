package com.sagetv.ng.telemetry;

public final class SageNgHookTargets {
    // Observed in SageTV-mine Catbert hook table.
    public static final String HOOK_MEDIA_PLAYER_FILE_LOAD_COMPLETE = "MediaPlayerFileLoadComplete";
    public static final String HOOK_FILE_PLAYBACK_FINISHED = "FilePlaybackFinished";

    // Observed in SageTV-mine API surfaces for caption state.
    public static final String API_GET_CC_STATE = "GetMediaPlayerClosedCaptionState";
    public static final String API_GET_CC_STATE_LABEL = "GetMediaPlayerClosedCaptionStateLabel";

    private SageNgHookTargets() {
    }
}
