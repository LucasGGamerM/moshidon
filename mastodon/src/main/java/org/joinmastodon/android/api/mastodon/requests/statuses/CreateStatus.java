package org.joinmastodon.android.api.mastodon.requests.statuses;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.ScheduledStatus;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CreateStatus extends MastodonAPIRequest<Status>{
	public static final Instant DRAFTS_AFTER_INSTANT = Instant.ofEpochMilli(253370764799999L) /* end of 9998 */;
	private static final float draftFactor = 31536000000f /* one year */ / 253370764799999f /* end of 9998 */;

	public static Instant getDraftInstant() {
		// returns an instant between 9999-01-01 00:00:00 and 9999-12-31 23:59:59
		// yes, this is a weird implementation for something that hardly matters
		return DRAFTS_AFTER_INSTANT.plusMillis(1 + (long) (System.currentTimeMillis() * draftFactor));
	}

	public CreateStatus(CreateStatus.Request req, String uuid){
		super(HttpMethod.POST, "/statuses", Status.class);
		setRequestBody(req);
		addHeader("Idempotency-Key", uuid);
	}

	public static class Scheduled extends MastodonAPIRequest<ScheduledStatus>{
		public Scheduled(CreateStatus.Request req, String uuid){
			super(HttpMethod.POST, "/statuses", ScheduledStatus.class);
			setRequestBody(req);
			addHeader("Idempotency-Key", uuid);
		}
	}

	public static class Request{
		public String status;
		public List<String> mediaIds;
		public Poll poll;
		public String inReplyToId;
		public boolean sensitive;
		public boolean localOnly;
		public String spoilerText;
		public StatusPrivacy visibility;
		public Instant scheduledAt;
		public String language;

		public String quoteId;

		public static class Poll{
			public ArrayList<String> options=new ArrayList<>();
			public int expiresIn;
			public boolean multiple;
			public boolean hideTotals;
		}
	}
}
