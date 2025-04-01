package org.joinmastodon.android.api.requests.statuses;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;

public class AddStatusReaction extends MastodonAPIRequest<Status> {
	public AddStatusReaction(String id, String emoji) {
		super(HttpMethod.POST, "/statuses/" + id + "/react/" + emoji, Status.class);
		setRequestBody(new Object());
	}
}
