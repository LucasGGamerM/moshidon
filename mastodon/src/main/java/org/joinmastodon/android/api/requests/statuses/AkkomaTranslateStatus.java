package org.joinmastodon.android.api.requests.statuses;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.AkkomaTranslation;

public class AkkomaTranslateStatus extends MastodonAPIRequest<AkkomaTranslation>{
	public AkkomaTranslateStatus(String id, String lang){
		super(HttpMethod.GET, "/statuses/"+id+"/translations/"+lang.toLowerCase(), AkkomaTranslation.class);
	}
}
