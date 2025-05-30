package org.joinmastodon.android.api.requests.oauth;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Application;

public class CreateOAuthApp extends MastodonAPIRequest<Application>{
	public CreateOAuthApp(){
		super(HttpMethod.POST, "/apps", Application.class);
		setRequestBody(new Request());
	}

	private static class Request{
		public String clientName="Moshidon";
		public String redirectUris=AccountSessionManager.REDIRECT_URI;
		public String scopes=AccountSessionManager.SCOPE;
		public String website="https://github.com/LucasGGamerM/moshidon";
	}
}
