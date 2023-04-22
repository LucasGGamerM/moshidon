package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.markers.SaveMarkers;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.AllNotificationsSeenEvent;
import org.joinmastodon.android.events.PollUpdatedEvent;
import org.joinmastodon.android.events.RemoveAccountPostsEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.CacheablePaginatedResponse;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.Filter;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.PaginatedResponse;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.AccountCardStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.HeaderStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.TextStatusDisplayItem;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.DiscoverInfoBannerHelper;
import org.joinmastodon.android.ui.utils.InsetStatusItemDecoration;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.SimpleCallback;

public class NotificationsListFragment extends BaseStatusListFragment<Notification>{
	private boolean onlyMentions;
	private boolean onlyPosts;
	private String maxID;
	private final DiscoverInfoBannerHelper bannerHelper = new DiscoverInfoBannerHelper(DiscoverInfoBannerHelper.BannerType.POST_NOTIFICATIONS);

	@Override
	protected boolean withComposeButton() {
		return true;
	}

	@Override
	public String getDomain() {
		return super.getDomain() + "/notifications";
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
		onlyMentions=getArguments().getBoolean("onlyMentions", false);
		onlyPosts=getArguments().getBoolean("onlyPosts", false);
	}

	@Override
	public void onRefresh() {
		super.onRefresh();
		if (getParentFragment() instanceof NotificationsFragment notificationsFragment) {
			notificationsFragment.refreshFollowRequestsBadge();
		}
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(Notification n){
		Account reportTarget = n.report == null ? null : n.report.targetAccount == null ? null :
				n.report.targetAccount;
		Emoji emoji = new Emoji();
		if(n.emojiUrl!=null){
			emoji.shortcode=n.emoji.substring(1,n.emoji.length()-1);
			emoji.url=n.emojiUrl;
			emoji.staticUrl=n.emojiUrl;
			emoji.visibleInPicker=false;
		}
		String extraText=switch(n.type){
			case FOLLOW -> getString(R.string.user_followed_you);
			case FOLLOW_REQUEST -> getString(R.string.user_sent_follow_request);
			case MENTION, STATUS -> null;
			case REBLOG -> getString(R.string.notification_boosted);
			case FAVORITE -> getString(R.string.user_favorited);
			case POLL -> getString(R.string.poll_ended);
			case UPDATE -> getString(R.string.sk_post_edited);
			case SIGN_UP -> getString(R.string.sk_signed_up);
			case REPORT -> getString(R.string.sk_reported);
			case EMOJI_REACTION -> getString(R.string.sk_reacted, n.emoji);
		};
		HeaderStatusDisplayItem titleItem=extraText!=null ? new HeaderStatusDisplayItem(n.id, n.account, n.createdAt, this, accountID, n.status, n.emojiUrl!=null ? HtmlParser.parseCustomEmoji(extraText, Collections.singletonList(emoji)) : extraText, n, null) : null;
		if(n.status!=null){
			ArrayList<StatusDisplayItem> items=StatusDisplayItem.buildItems(this, n.status, accountID, n, knownAccounts, titleItem!=null, titleItem==null, n, false, Filter.FilterContext.NOTIFICATIONS);
			if(titleItem!=null)
				items.add(0, titleItem);
			return items;
		}else if(titleItem!=null){
			AccountCardStatusDisplayItem card=new AccountCardStatusDisplayItem(n.id, this,
					reportTarget != null ? reportTarget : n.account, n);
			TextStatusDisplayItem text = n.report != null && !TextUtils.isEmpty(n.report.comment) ?
					new TextStatusDisplayItem(n.id, n.report.comment, this,
							Status.ofFake(n.id, n.report.comment, n.createdAt), true) :
					null;
			return text == null ? Arrays.asList(titleItem, card) : Arrays.asList(titleItem, text, card);
		}else{
			return Collections.emptyList();
		}
	}

	@Override
	protected void addAccountToKnown(Notification s){
		if(!knownAccounts.containsKey(s.account.id))
			knownAccounts.put(s.account.id, s.account);
		if(s.status!=null && !knownAccounts.containsKey(s.status.account.id))
			knownAccounts.put(s.status.account.id, s.status.account);
		if(s.status!=null && s.status.reblog!=null && !knownAccounts.containsKey(s.status.reblog.account.id))
			knownAccounts.put(s.status.reblog.account.id, s.status.reblog.account);
	}

	@Override
	protected void doLoadData(int offset, int count){
		AccountSessionManager.getInstance()
				.getAccount(accountID).getCacheController()
				.getNotifications(offset>0 ? maxID : null, count, onlyMentions, onlyPosts, refreshing, new SimpleCallback<>(this){
					@Override
					public void onSuccess(CacheablePaginatedResponse<List<Notification>> result){
						if (getActivity() == null) return;
						if(refreshing)
							relationships.clear();
						onDataLoaded(result.items.stream().filter(n->n.type!=null).collect(Collectors.toList()), !result.items.isEmpty());
						Set<String> needRelationships=result.items.stream()
								.filter(ntf->ntf.status==null && !relationships.containsKey(ntf.account.id))
								.map(ntf->ntf.account.id)
								.collect(Collectors.toSet());
						loadRelationships(needRelationships);
						maxID=result.maxID;

						if(offset==0 && !result.items.isEmpty() && !result.isFromCache()){
							E.post(new AllNotificationsSeenEvent());
							new SaveMarkers(null, result.items.get(0).id).exec(accountID);
							AccountSessionManager.getInstance().getAccount(accountID).markers
									.notifications.lastReadId = result.items.get(0).id;
							AccountSessionManager.getInstance().writeAccountsFile();
						}
					}
				});
	}

	@Override
	protected void onRelationshipsLoaded(){
		if(getActivity()==null)
			return;
		for(int i=0;i<list.getChildCount();i++){
			RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
			if(holder instanceof AccountCardStatusDisplayItem.Holder accountHolder)
				accountHolder.rebind();
		}
	}

	@Override
	protected void onShown(){
		super.onShown();
		if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading){
			refreshing=true;
			loadData();
		}

	}

	@Override
	public void onItemClick(String id){
		Notification n=getNotificationByID(id);
		if(n.status!=null){
			Status status=n.status;
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("status", Parcels.wrap(status));
			if(status.inReplyToAccountId!=null && knownAccounts.containsKey(status.inReplyToAccountId))
				args.putParcelable("inReplyToAccount", Parcels.wrap(knownAccounts.get(status.inReplyToAccountId)));
			Nav.go(getActivity(), ThreadFragment.class, args);
		}else if(n.report != null){
			String domain = AccountSessionManager.getInstance().getAccount(accountID).domain;
			UiUtils.launchWebBrowser(getActivity(), "https://"+domain+"/admin/reports/"+n.report.id);
		}else{
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("profileAccount", Parcels.wrap(n.account));
			Nav.go(getActivity(), ProfileFragment.class, args);
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		list.addItemDecoration(new InsetStatusItemDecoration(this));
		if (getParentFragment() instanceof NotificationsFragment) fab.setVisibility(View.GONE);
		if (onlyPosts) bannerHelper.maybeAddBanner(contentWrap);
	}

	private Notification getNotificationByID(String id){
		for(Notification n:data){
			if(n.id.equals(id))
				return n;
		}
		return null;
	}

	@Subscribe
	public void onPollUpdated(PollUpdatedEvent ev){
		if(!ev.accountID.equals(accountID))
			return;
		for(Notification ntf:data){
			if(ntf.status==null)
				continue;
			Status contentStatus=ntf.status.getContentStatus();
			if(contentStatus.poll!=null && contentStatus.poll.id.equals(ev.poll.id)){
				updatePoll(ntf.id, ntf.status, ev.poll);
			}
		}
	}

	@Subscribe
	public void onRemoveAccountPostsEvent(RemoveAccountPostsEvent ev){
		if(!ev.accountID.equals(accountID) || ev.isUnfollow)
			return;
		List<Notification> toRemove=Stream.concat(data.stream(), preloadedData.stream())
				.filter(n->n.account!=null && n.account.id.equals(ev.postsByAccountID))
				.collect(Collectors.toList());
		for(Notification n:toRemove){
			removeNotification(n);
		}
	}

	public void removeNotification(Notification n){
		data.remove(n);
		preloadedData.remove(n);
		int index=-1;
		for(int i=0;i<displayItems.size();i++){
			if(n.id.equals(displayItems.get(i).parentID)){
				index=i;
				break;
			}
		}
		if(index==-1)
			return;
		int lastIndex;
		for(lastIndex=index;lastIndex<displayItems.size();lastIndex++){
			if(!displayItems.get(lastIndex).parentID.equals(n.id))
				break;
		}
		displayItems.subList(index, lastIndex).clear();
		adapter.notifyItemRangeRemoved(index, lastIndex-index);
	}
}
