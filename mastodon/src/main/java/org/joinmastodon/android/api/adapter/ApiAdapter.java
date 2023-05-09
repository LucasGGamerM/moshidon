package org.joinmastodon.android.api.adapter;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.requests.statuses.GetStatusByID;
import org.joinmastodon.android.api.requests.timelines.GetHomeTimeline;
import org.joinmastodon.android.model.Status;

import java.util.List;

public class ApiAdapter {
    public final ServerType serverType;

    public ApiAdapter(ServerType serverType){
        this.serverType = serverType;
    }

    public MastodonAPIRequest<?> getPostById(String id){
        switch (serverType){
            case MASTODON -> {
                return new GetStatusByID(id);
            }
            case MISSKEY -> {
                return null;
            }
        }
        return null;
    }

    public MastodonAPIRequest<List<Status>> getHomeTimeline(String maxID, String minID, int limit, String sinceID){
        switch (serverType){
            case MASTODON -> {
                return new GetHomeTimeline(maxID, minID, limit, sinceID);
            }
            case MISSKEY -> {
                return null;
            }
        }
        return null;
    }



    public enum ServerType {
        MASTODON,
        MISSKEY,
    }
}
