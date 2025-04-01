package org.joinmastodon.android.api.requests.statuses;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.requests.HeaderPaginationRequest;
import org.joinmastodon.android.model.ScheduledStatus;

public class GetScheduledStatuses extends HeaderPaginationRequest<ScheduledStatus>{
	public GetScheduledStatuses(String maxID, int limit){
		super(HttpMethod.GET, "/scheduled_statuses", new TypeToken<>(){});
		if(maxID!=null)
			addQueryParameter("max_id", maxID);
		if(limit>0)
			addQueryParameter("limit", limit+"");
	}
}
