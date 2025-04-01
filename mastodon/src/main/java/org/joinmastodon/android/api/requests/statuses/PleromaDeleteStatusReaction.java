package org.joinmastodon.android.api.requests.statuses;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;

public class PleromaDeleteStatusReaction extends MastodonAPIRequest<Status> {
    public PleromaDeleteStatusReaction(String id, String emoji) {
        super(HttpMethod.DELETE, "/pleroma/statuses/" + id + "/reactions/" + emoji, Status.class);
    }
}
