package org.joinmastodon.android.api.mastodon.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Account;

public class GetAccountByHandle extends MastodonAPIRequest<Account>{
    public GetAccountByHandle(String acct){
        super(HttpMethod.GET, "/accounts/lookup", Account.class);
        addQueryParameter("acct", acct);
    }
}
