package org.joinmastodon.android.api.mastodon.requests.instance;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Instance;

public class GetInstance extends MastodonAPIRequest<Instance>{
	public GetInstance(){
		super(HttpMethod.GET, "/instance", Instance.class);
	}

	public static class V2 extends MastodonAPIRequest<Instance.V2>{
		public V2(){
			super(HttpMethod.GET, "/instance", Instance.V2.class);
		}

		@Override
		protected String getPathPrefix() {
			return "/api/v2";
		}
	}
}
