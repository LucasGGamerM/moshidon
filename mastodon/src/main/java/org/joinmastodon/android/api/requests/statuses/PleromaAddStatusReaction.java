package org.joinmastodon.android.api.requests.statuses;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;

public class PleromaAddStatusReaction extends MastodonAPIRequest<Status> {
    public PleromaAddStatusReaction(String id, String emoji) {
        super(HttpMethod.PUT, "/pleroma/statuses/" + id + "/reactions/" + emoji, Status.class);
		setRequestBody(new Object());
    }
}
