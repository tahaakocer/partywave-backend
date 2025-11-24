package com.partywave.backend.service.redis;

/**
 * Result object for track operations.
 * Used by PlaybackRedisService to return operation results with status and details.
 */
public class TrackOperationResult {

    private final boolean success;
    private final String message;
    private final String playlistItemId;

    public TrackOperationResult(boolean success, String message, String playlistItemId) {
        this.success = success;
        this.message = message;
        this.playlistItemId = playlistItemId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getPlaylistItemId() {
        return playlistItemId;
    }

    @Override
    public String toString() {
        return (
            "TrackOperationResult{" +
            "success=" +
            success +
            ", message='" +
            message +
            '\'' +
            ", playlistItemId='" +
            playlistItemId +
            '\'' +
            '}'
        );
    }
}
