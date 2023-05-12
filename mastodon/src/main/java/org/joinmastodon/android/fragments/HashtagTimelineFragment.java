package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.joinmastodon.android.DomainManager;
import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.mastodon.requests.tags.GetHashtag;
import org.joinmastodon.android.api.mastodon.requests.tags.SetHashtagFollowed;
import org.joinmastodon.android.api.mastodon.requests.timelines.GetHashtagTimeline;
import org.joinmastodon.android.events.HashtagUpdatedEvent;
import org.joinmastodon.android.model.Filter;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.TimelineDefinition;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.utils.StatusFilterPredicate;

import java.util.List;
import java.util.stream.Collectors;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.V;

public class HashtagTimelineFragment extends PinnableStatusListFragment {
	private String hashtag;
	private boolean following;
	private MenuItem followButton;

	@Override
	protected boolean wantsComposeButton() {
		return true;
	}

	@Override
	public String getDomain() {
		return super.getDomain() + "/tags/" + hashtag;
	}


	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		updateTitle(getArguments().getString("hashtag"));
		following=getArguments().getBoolean("following", false);
		setHasOptionsMenu(true);
		DomainManager.getInstance().setCurrentDomain(getDomain());
	}

	private void updateTitle(String hashtagName) {
		hashtag = hashtagName;
		setTitle('#'+hashtag);
	}

	private void updateFollowingState(boolean newFollowing) {
		this.following = newFollowing;
		followButton.setTitle(getString(newFollowing ? R.string.unfollow_user : R.string.follow_user, "#" + hashtag));
		followButton.setIcon(newFollowing ? R.drawable.ic_fluent_person_delete_24_filled : R.drawable.ic_fluent_person_add_24_regular);
		E.post(new HashtagUpdatedEvent(hashtag, following));
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.hashtag_timeline, menu);
		super.onCreateOptionsMenu(menu, inflater);
		followButton = menu.findItem(R.id.follow_hashtag);
		updateFollowingState(following);

		new GetHashtag(hashtag).setCallback(new Callback<>() {
			@Override
			public void onSuccess(Hashtag hashtag) {
				if (getActivity() == null) return;
				updateTitle(hashtag.name);
				updateFollowingState(hashtag.following);
			}

			@Override
			public void onError(ErrorResponse error) {
				error.showToast(getActivity());
			}
		}).exec(accountID);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (super.onOptionsItemSelected(item)) return true;
		if (item.getItemId() == R.id.follow_hashtag) {
			updateFollowingState(!following);
			getToolbar().performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
			new SetHashtagFollowed(hashtag, following).setCallback(new Callback<>() {
				@Override
				public void onSuccess(Hashtag i) {
					if (getActivity() == null) return;
					if (i.following == following) Toast.makeText(getActivity(), getString(i.following ? R.string.followed_user : R.string.unfollowed_user, "#" + i.name), Toast.LENGTH_SHORT).show();
					updateFollowingState(i.following);
				}

				@Override
				public void onError(ErrorResponse error) {
					error.showToast(getActivity());
					updateFollowingState(!following);
				}
			}).exec(accountID);
			return true;
		}
		return false;
	}

	@Override
	protected TimelineDefinition makeTimelineDefinition() {
		return TimelineDefinition.ofHashtag(hashtag);
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetHashtagTimeline(hashtag, offset==0 ? null : getMaxID(), null, count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Status> result){
						if (getActivity() == null) return;
						result=result.stream().filter(new StatusFilterPredicate(accountID, Filter.FilterContext.PUBLIC)).collect(Collectors.toList());
						onDataLoaded(result, !result.isEmpty());
					}
				})
				.exec(accountID);
	}

	@Override
	protected void onShown(){
		super.onShown();
		if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading)
			loadData();
	}

	@Override
	public boolean onFabLongClick(View v) {
		return UiUtils.pickAccountForCompose(getActivity(), accountID, '#'+hashtag+' ');
	}

	@Override
	public void onFabClick(View v){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putString("prefilledText", '#'+hashtag+' ');
		Nav.go(getActivity(), ComposeFragment.class, args);
	}

	@Override
	protected void onSetFabBottomInset(int inset){
		((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin=V.dp(24)+inset;
	}
}
