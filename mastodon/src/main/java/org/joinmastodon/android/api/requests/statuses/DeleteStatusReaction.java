package org.joinmastodon.android.api.requests.statuses;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;

public class DeleteStatusReaction extends MastodonAPIRequest<Status> {
	public DeleteStatusReaction(String id, String emoji) {
		super(HttpMethod.POST, "/statuses/" + id + "/unreact/" + emoji, Status.class);
		setRequestBody(new Object());
	}
}
