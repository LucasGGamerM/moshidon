package org.joinmastodon.android.fragments;

import org.joinmastodon.android.api.mastodon.session.AccountSession;
import org.joinmastodon.android.api.mastodon.session.AccountSessionManager;

public interface DomainDisplay {

	default String getDomain(){
		AccountSession session = AccountSessionManager.getInstance().getLastActiveAccount();
		if (session != null)
			return session.domain;
		else
			return "";
	}
}
