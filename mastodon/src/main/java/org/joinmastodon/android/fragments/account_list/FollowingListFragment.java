package org.joinmastodon.android.fragments.account_list;

import android.os.Bundle;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.mastodon.requests.HeaderPaginationRequest;
import org.joinmastodon.android.api.mastodon.requests.accounts.GetAccountFollowing;
import org.joinmastodon.android.model.Account;

public class FollowingListFragment extends AccountRelatedAccountListFragment{

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		targetAccount = account;
		setSubtitle(getResources().getQuantityString(R.plurals.x_following, (int)(account.followingCount%1000), account.followingCount));
	}

	@Override
	public HeaderPaginationRequest<Account> onCreateRequest(String maxID, int count){
		return new GetAccountFollowing(account.id, maxID, count);
	}

	@Override
	public HeaderPaginationRequest<Account> onCreateRemoteRequest(String id, String maxID, int count){
		return new GetAccountFollowing(id, maxID, count);
	}
}
