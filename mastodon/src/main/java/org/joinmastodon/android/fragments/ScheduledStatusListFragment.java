package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.mastodon.requests.statuses.CreateStatus;
import org.joinmastodon.android.api.mastodon.requests.statuses.GetScheduledStatuses;
import org.joinmastodon.android.events.ScheduledStatusCreatedEvent;
import org.joinmastodon.android.events.ScheduledStatusDeletedEvent;
import org.joinmastodon.android.model.Filter;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.model.ScheduledStatus;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.Collections;
import java.util.List;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.SimpleCallback;

public class ScheduledStatusListFragment extends BaseStatusListFragment<ScheduledStatus> {
	private String nextMaxID;
	private static final int SCHEDULED_STATUS_LIST_OPENED = 161;

	@Override
	protected boolean wantsComposeButton() {
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		E.register(this);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		E.unregister(this);
	}


	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setTitle(R.string.sk_unsent_posts);
		loadData();
	}

	@Override
	public void onFabClick(View v) {
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putSerializable("scheduledAt", CreateStatus.getDraftInstant());
		Nav.go(getActivity(), ComposeFragment.class, args);
	}

	@Override
	public boolean onFabLongClick(View v) {
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putSerializable("scheduledAt", CreateStatus.getDraftInstant());
		return UiUtils.pickAccountForCompose(getActivity(), accountID, args);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (getArguments().getBoolean("hide_fab", false)) fab.setVisibility(View.GONE);
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(ScheduledStatus s) {
		return StatusDisplayItem.buildItems(this, s.toStatus(), accountID, s, knownAccounts, false, false, null, true, Filter.FilterContext.HOME);
	}

	@Override
	protected void addAccountToKnown(ScheduledStatus s) {}

	@Override
	public void onItemClick(String id) {
		final Bundle args=new Bundle();
		args.putString("account", accountID);
		ScheduledStatus scheduledStatus = getStatusByID(id);
		Status status = scheduledStatus.toStatus();
		args.putParcelable("scheduledStatus", Parcels.wrap(scheduledStatus));
		args.putParcelable("editStatus", Parcels.wrap(status));
		args.putString("sourceText", status.text);
		args.putString("sourceSpoiler", status.spoilerText);
		args.putBoolean("redraftStatus", true);
		setResult(true, null);

		// closing this scheduled status list if another status list is opened from compose fragment
		Nav.goForResult(getActivity(), ComposeFragment.class, args, SCHEDULED_STATUS_LIST_OPENED, this);
	}

	@Override
	public void onFragmentResult(int reqCode, boolean success, Bundle result) {
		if (reqCode == SCHEDULED_STATUS_LIST_OPENED && success && getActivity() != null) {
			Nav.finish(this);
		}
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetScheduledStatuses(offset==0 ? null : nextMaxID, count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(HeaderPaginationList<ScheduledStatus> result){
						if(result.nextPageUri!=null)
							nextMaxID=result.nextPageUri.getQueryParameter("max_id");
						else
							nextMaxID=null;
						if (getActivity() == null) return;
						onDataLoaded(result, nextMaxID!=null);
					}
				})
				.exec(accountID);
	}

	// copied from StatusListFragment.java
	@Subscribe
	public void onScheduledStatusDeleted(ScheduledStatusDeletedEvent ev){
		if(!ev.accountID.equals(accountID)) return;
		ScheduledStatus status=getStatusByID(ev.id);
		if(status==null) return;
		removeStatus(status);
	}

	// copied from StatusListFragment.java
	@Subscribe
	public void onScheduledStatusCreated(ScheduledStatusCreatedEvent ev){
		if(!ev.accountID.equals(accountID))	return;
		prependItems(Collections.singletonList(ev.scheduledStatus), true);
		scrollToTop();
	}

	// copied from StatusListFragment.java
	protected void removeStatus(ScheduledStatus status){
		data.remove(status);
		preloadedData.remove(status);
		int index=-1;
		for(int i=0;i<displayItems.size();i++){
			if(status.id.equals(displayItems.get(i).parentID)){
				index=i;
				break;
			}
		}
		if(index==-1)
			return;
		int lastIndex;
		for(lastIndex=index;lastIndex<displayItems.size();lastIndex++){
			if(!displayItems.get(lastIndex).parentID.equals(status.id))
				break;
		}
		displayItems.subList(index, lastIndex).clear();
		adapter.notifyItemRangeRemoved(index, lastIndex-index);
	}

	// copied from StatusListFragment.java
	protected ScheduledStatus getStatusByID(String id){
		for(ScheduledStatus s:data){
			if(s.id.equals(id)){
				return s;
			}
		}
		for(ScheduledStatus s:preloadedData){
			if(s.id.equals(id)){
				return s;
			}
		}
		return null;
	}
}
