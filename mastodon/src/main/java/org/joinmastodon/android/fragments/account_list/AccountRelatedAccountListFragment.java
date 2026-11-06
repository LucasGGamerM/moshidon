package org.joinmastodon.android.fragments.account_list;

import android.net.Uri;
import android.os.Bundle;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Account;
import org.parceler.Parcels;

public abstract class AccountRelatedAccountListFragment extends PaginatedAccountListFragment{
	protected Account account;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		account=Parcels.unwrap(getArguments().getParcelable("targetAccount"));
		setTitle("@"+account.acct);
		// MOSHIDON: distinguish "no followers/following" from "the user hid their collections".
		// The API returns an empty list in both cases; hide_collections tells us which one it is.
		if(account.hideCollections)
			emptyText=getString(R.string.account_hides_collections);
	}

	// MOSHIDON:
	@Override
	public Uri getWebUri(Uri.Builder base) {
		return base.path(isInstanceAkkoma()
				? "/users/" + account.id
				: '@' + account.acct).build();
	}
}
