package org.joinmastodon.android.api.mastodon.requests.statuses;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;

public class DeleteStatus extends MastodonAPIRequest<Status>{
	public DeleteStatus(String id){
		super(HttpMethod.DELETE, "/statuses/"+id, Status.class);
	}

	public static class Scheduled extends MastodonAPIRequest<Object> {
		public Scheduled(String id) {
			super(HttpMethod.DELETE, "/scheduled_statuses/"+id, Object.class);
		}
	}
}
