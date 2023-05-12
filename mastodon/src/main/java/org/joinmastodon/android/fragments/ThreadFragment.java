package org.joinmastodon.android.fragments;

import android.os.Bundle;
import android.view.View;

import org.joinmastodon.android.DomainManager;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.mastodon.requests.statuses.GetStatusContext;
import org.joinmastodon.android.api.mastodon.session.session.AccountSession;
import org.joinmastodon.android.api.mastodon.session.session.AccountSessionManager;
import org.joinmastodon.android.events.StatusCreatedEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Filter;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusContext;
import org.joinmastodon.android.ui.displayitems.ExtendedFooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.FooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.TextStatusDisplayItem;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.utils.StatusFilterPredicate;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import me.grishka.appkit.api.SimpleCallback;

public class ThreadFragment extends StatusListFragment implements DomainDisplay{
	protected Status mainStatus;

	@Override
	public String getDomain() {
		return mainStatus.url;
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		mainStatus=Parcels.unwrap(getArguments().getParcelable("status"));
		Account inReplyToAccount=Parcels.unwrap(getArguments().getParcelable("inReplyToAccount"));
		if(inReplyToAccount!=null)
			knownAccounts.put(inReplyToAccount.id, inReplyToAccount);
		data.add(mainStatus);
		onAppendItems(Collections.singletonList(mainStatus));
		setTitle(HtmlParser.parseCustomEmoji(getString(R.string.post_from_user, mainStatus.account.displayName), mainStatus.account.emojis));

		DomainManager.getInstance().setCurrentDomain(getDomain());
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(Status s){
		List<StatusDisplayItem> items=super.buildDisplayItems(s);
		if(s.id.equals(mainStatus.id)){
			for(StatusDisplayItem item:items){
				if(item instanceof TextStatusDisplayItem text)
					text.textSelectable=true;
				else if(item instanceof FooterStatusDisplayItem footer)
					footer.hideCounts=true;
			}
			items.add(new ExtendedFooterStatusDisplayItem(s.id, this, s.getContentStatus()));
		}
		return items;
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetStatusContext(mainStatus.id)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(StatusContext result){
						if (getActivity() == null) return;
						if(refreshing){
							data.clear();
							displayItems.clear();
							data.add(mainStatus);
							onAppendItems(Collections.singletonList(mainStatus));
						}
						AccountSession account=AccountSessionManager.getInstance().getAccount(accountID);
						Instance instance=AccountSessionManager.getInstance().getInstanceInfo(account.domain);
						if(instance.pleroma != null){
							List<String> threadIds=new ArrayList<>();
							threadIds.add(mainStatus.id);
							for(Status s:result.descendants){
								if(threadIds.contains(s.inReplyToId)){
									threadIds.add(s.id);
								}
							}
							threadIds.add(mainStatus.inReplyToId);
							for(int i=result.ancestors.size()-1; i >= 0; i--){
								Status s=result.ancestors.get(i);
								if(s.inReplyToId != null && threadIds.contains(s.id)){
									threadIds.add(s.inReplyToId);
								}
							}

							result.ancestors=result.ancestors.stream().filter(s -> threadIds.contains(s.id)).collect(Collectors.toList());
							result.descendants=getDescendantsOrdered(mainStatus.id,
									result.descendants.stream()
											.filter(s -> threadIds.contains(s.id))
											.collect(Collectors.toList()));
						}
						result.descendants=filterStatuses(result.descendants);
						result.ancestors=filterStatuses(result.ancestors);
						if(footerProgress!=null)
							footerProgress.setVisibility(View.GONE);
						data.addAll(result.descendants);
						int prevCount=displayItems.size();
						onAppendItems(result.descendants);
						int count=displayItems.size();
						if(!refreshing)
							adapter.notifyItemRangeInserted(prevCount, count-prevCount);
						prependItems(result.ancestors, !refreshing);
						dataLoaded();
						if(refreshing){
							refreshDone();
							adapter.notifyDataSetChanged();
						}
						list.scrollToPosition(displayItems.size()-count);
					}
				})
				.exec(accountID);
	}

	private List<Status> getDescendantsOrdered(String id, List<Status> statuses){
		List<Status> out=new ArrayList<>();
		for(Status s:getDirectDescendants(id, statuses)){
			out.add(s);
			getDirectDescendants(s.id, statuses).forEach(d ->{
				out.add(d);
				out.addAll(getDescendantsOrdered(d.id, statuses));
			});
		}
		return out;
	}

	private List<Status> getDirectDescendants(String id, List<Status> statuses){
		return statuses.stream()
				.filter(s -> s.inReplyToId.equals(id))
				.collect(Collectors.toList());
	}

	private List<Status> filterStatuses(List<Status> statuses){
		StatusFilterPredicate statusFilterPredicate=new StatusFilterPredicate(accountID,Filter.FilterContext.THREAD);
		return statuses.stream()
				.filter(statusFilterPredicate)
				.collect(Collectors.toList());
	}

	@Override
	protected void onShown(){
		super.onShown();
		if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading){
			dataLoading=true;
			doLoadData();
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		UiUtils.loadCustomEmojiInTextView(toolbarTitleView);
		showContent();
		if(!loaded)
			footerProgress.setVisibility(View.VISIBLE);
	}

	protected void onStatusCreated(StatusCreatedEvent ev){
		if(ev.status.inReplyToId!=null && getStatusByID(ev.status.inReplyToId)!=null){
			onAppendItems(Collections.singletonList(ev.status));
		}
	}

	@Override
	public boolean isItemEnabled(String id){
		return !id.equals(mainStatus.id);
	}

	@Override
	public boolean wantsLightStatusBar(){
		return !UiUtils.isDarkTheme();
	}

	@Override
	public boolean wantsLightNavigationBar(){
		return !UiUtils.isDarkTheme();
	}
}
