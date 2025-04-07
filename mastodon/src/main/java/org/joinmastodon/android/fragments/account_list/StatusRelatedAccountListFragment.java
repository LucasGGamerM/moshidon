package org.joinmastodon.android.fragments.account_list;

import android.net.Uri;
import android.os.Bundle;

import org.joinmastodon.android.model.Status;
import org.parceler.Parcels;

public abstract class StatusRelatedAccountListFragment extends PaginatedAccountListFragment{
	protected Status status;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		status=Parcels.unwrap(getArguments().getParcelable("status"));
	}

	// MOSHIDON:
	@Override
	public Uri getWebUri(Uri.Builder base) {
		return base
				.encodedPath(isInstanceAkkoma()
						? "/notice/" + status.id
						: '@' + status.account.acct + '/' + status.id)
				.build();
	}
}
