package org.joinmastodon.android.api.mastodon.requests.statuses;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.TranslatedStatus;

public class TranslateStatus extends MastodonAPIRequest<TranslatedStatus> {
    public TranslateStatus(String id) {
        super(HttpMethod.POST, "/statuses/"+id+"/translate", TranslatedStatus.class);
        setRequestBody(new Object());
    }
}
