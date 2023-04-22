package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.HashtagTimelineFragment;
import org.joinmastodon.android.fragments.HomeTabFragment;
import org.joinmastodon.android.fragments.ListTimelineFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.fragments.ThreadFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.DisplayItemsParent;
import org.joinmastodon.android.model.Filter;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.Poll;
import org.joinmastodon.android.model.ScheduledStatus;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.PhotoLayoutHelper;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.utils.StatusFilterPredicate;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import me.grishka.appkit.Nav;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.views.UsableRecyclerView;

public abstract class StatusDisplayItem{
	public final String parentID;
	public final BaseStatusListFragment parentFragment;
	public boolean inset;
	public int index;

	public StatusDisplayItem(String parentID, BaseStatusListFragment parentFragment){
		this.parentID=parentID;
		this.parentFragment=parentFragment;
	}

	public abstract Type getType();

	public int getImageCount(){
		return 0;
	}

	public ImageLoaderRequest getImageRequest(int index){
		return null;
	}

	public static BindableViewHolder<? extends StatusDisplayItem> createViewHolder(Type type, Activity activity, ViewGroup parent){
		return switch(type){
			case HEADER -> new HeaderStatusDisplayItem.Holder(activity, parent);
			case REBLOG_OR_REPLY_LINE -> new ReblogOrReplyLineStatusDisplayItem.Holder(activity, parent);
			case TEXT -> new TextStatusDisplayItem.Holder(activity, parent);
			case AUDIO -> new AudioStatusDisplayItem.Holder(activity, parent);
			case POLL_OPTION -> new PollOptionStatusDisplayItem.Holder(activity, parent);
			case POLL_FOOTER -> new PollFooterStatusDisplayItem.Holder(activity, parent);
			case CARD -> new LinkCardStatusDisplayItem.Holder(activity, parent);
			case FOOTER -> new FooterStatusDisplayItem.Holder(activity, parent);
			case ACCOUNT_CARD -> new AccountCardStatusDisplayItem.Holder(activity, parent);
			case ACCOUNT -> new AccountStatusDisplayItem.Holder(activity, parent);
			case HASHTAG -> new HashtagStatusDisplayItem.Holder(activity, parent);
			case GAP -> new GapStatusDisplayItem.Holder(activity, parent);
			case EXTENDED_FOOTER -> new ExtendedFooterStatusDisplayItem.Holder(activity, parent);
			case MEDIA_GRID -> new MediaGridStatusDisplayItem.Holder(activity, parent);
			case WARNING -> new WarningFilteredStatusDisplayItem.Holder(activity, parent);
		};
	}

	public static ArrayList<StatusDisplayItem> buildItems(BaseStatusListFragment<?> fragment, Status status, String accountID, DisplayItemsParent parentObject, Map<String, Account> knownAccounts, boolean inset, boolean addFooter, Notification notification){
		return buildItems(fragment, status, accountID, parentObject, knownAccounts, inset, addFooter, notification, false, Filter.FilterContext.HOME);
	}

	public static ArrayList<StatusDisplayItem> buildItems(BaseStatusListFragment<?> fragment, Status status, String accountID, DisplayItemsParent parentObject, Map<String, Account> knownAccounts, boolean inset, boolean addFooter, Notification notification, Filter.FilterContext filterContext){
		return buildItems(fragment, status, accountID, parentObject, knownAccounts, inset, addFooter, notification, false, filterContext);
	}

	public static ArrayList<StatusDisplayItem> buildItems(BaseStatusListFragment<?> fragment, Status status, String accountID, DisplayItemsParent parentObject, Map<String, Account> knownAccounts, boolean inset, boolean addFooter, Notification notification, boolean disableTranslate){
		return buildItems(fragment, status, accountID, parentObject, knownAccounts, inset, addFooter, notification, disableTranslate, Filter.FilterContext.HOME);
	}

	public static ArrayList<StatusDisplayItem> buildItems(BaseStatusListFragment<?> fragment, Status status, String accountID, DisplayItemsParent parentObject, Map<String, Account> knownAccounts, boolean inset, boolean addFooter, Notification notification, boolean disableTranslate, Filter.FilterContext filterContext){
		String parentID=parentObject.getID();
		ArrayList<StatusDisplayItem> items=new ArrayList<>();

		Status statusForContent=status.getContentStatus();
		Bundle args=new Bundle();
		args.putString("account", accountID);
		ScheduledStatus scheduledStatus = parentObject instanceof ScheduledStatus ? (ScheduledStatus) parentObject : null;

		List<Filter> filters = AccountSessionManager.getInstance().getAccount(accountID).wordFilters.stream()
			.filter(f -> f.context.contains(filterContext)).collect(Collectors.toList());
		StatusFilterPredicate filterPredicate = new StatusFilterPredicate(filters);

		if(statusForContent != null && !statusForContent.filterRevealed){
			statusForContent.filterRevealed = filterPredicate.testWithWarning(status);
		}

		ReblogOrReplyLineStatusDisplayItem replyLine = null;
		boolean threadReply = statusForContent.inReplyToAccountId != null &&
				statusForContent.inReplyToAccountId.equals(statusForContent.account.id);

		if(statusForContent.inReplyToAccountId!=null && !(threadReply && fragment instanceof ThreadFragment)){
			Account account = knownAccounts.get(statusForContent.inReplyToAccountId);
			String text = threadReply ? fragment.getString(R.string.sk_show_thread)
					: account == null ? fragment.getString(R.string.sk_in_reply)
					: GlobalUserPreferences.compactReblogReplyLine && status.reblog != null ? account.displayName
					: fragment.getString(R.string.in_reply_to, account.displayName);
			String fullText = threadReply ? fragment.getString(R.string.sk_show_thread)
					: account == null ? fragment.getString(R.string.sk_in_reply)
					: fragment.getString(R.string.in_reply_to, account.displayName);
			replyLine = new ReblogOrReplyLineStatusDisplayItem(
					parentID, fragment, text, account == null ? List.of() : account.emojis,
					R.drawable.ic_fluent_arrow_reply_20_filled, null, null, fullText
			);
		}

		if(status.reblog!=null){
			boolean isOwnPost = AccountSessionManager.getInstance().isSelf(fragment.getAccountID(), status.account);
			statusForContent.rebloggedBy = status.account;
			String fullText = fragment.getString(R.string.user_boosted, status.account.displayName);
			String text = GlobalUserPreferences.compactReblogReplyLine && replyLine != null ? status.account.displayName : fullText;
			items.add(new ReblogOrReplyLineStatusDisplayItem(parentID, fragment, text, status.account.emojis, R.drawable.ic_fluent_arrow_repeat_all_20_filled, isOwnPost ? status.visibility : null, i->{
				args.putParcelable("profileAccount", Parcels.wrap(status.account));
				Nav.go(fragment.getActivity(), ProfileFragment.class, args);
			}, fullText));
		} else if (!(status.tags.isEmpty() ||
				fragment instanceof HashtagTimelineFragment ||
				fragment instanceof ListTimelineFragment
		) && fragment.getParentFragment() instanceof HomeTabFragment home) {
			home.getHashtags().stream()
					.filter(followed -> status.tags.stream()
							.anyMatch(hashtag -> followed.name.equalsIgnoreCase(hashtag.name)))
					.findAny()
					// post contains a hashtag the user is following
					.ifPresent(hashtag -> items.add(new ReblogOrReplyLineStatusDisplayItem(
							parentID, fragment, hashtag.name, List.of(),
							R.drawable.ic_fluent_number_symbol_20_filled, null,
							i -> {
								args.putString("hashtag", hashtag.name);
								Nav.go(fragment.getActivity(), HashtagTimelineFragment.class, args);
							}
					)));
		}

		if (replyLine != null && GlobalUserPreferences.replyLineAboveHeader) {
			Optional<ReblogOrReplyLineStatusDisplayItem> primaryLine = items.stream()
					.filter(i -> i instanceof ReblogOrReplyLineStatusDisplayItem)
					.map(ReblogOrReplyLineStatusDisplayItem.class::cast)
					.findFirst();

			if (primaryLine.isPresent() && GlobalUserPreferences.compactReblogReplyLine) {
				primaryLine.get().extra = replyLine;
			} else {
				items.add(replyLine);
			}
		}

		HeaderStatusDisplayItem header;
		items.add(header=new HeaderStatusDisplayItem(parentID, statusForContent.account, statusForContent.createdAt, fragment, accountID, statusForContent, null, notification, scheduledStatus));

		if (replyLine != null && !GlobalUserPreferences.replyLineAboveHeader) {
			replyLine.belowHeader = true;
			items.add(replyLine);
		}

		if(!TextUtils.isEmpty(statusForContent.content))
			items.add(new TextStatusDisplayItem(parentID, HtmlParser.parse(statusForContent.content, statusForContent.emojis, statusForContent.mentions, statusForContent.tags, accountID), fragment, statusForContent, disableTranslate));
		else if (!GlobalUserPreferences.replyLineAboveHeader && replyLine != null)
			replyLine.needBottomPadding=true;
		else
			header.needBottomPadding=true;
		List<Attachment> imageAttachments=statusForContent.mediaAttachments.stream().filter(att->att.type.isImage()).collect(Collectors.toList());
		if(!imageAttachments.isEmpty()){
			PhotoLayoutHelper.TiledLayoutResult layout=PhotoLayoutHelper.processThumbs(imageAttachments);
			items.add(new MediaGridStatusDisplayItem(parentID, fragment, layout, imageAttachments, statusForContent));
		}
		for(Attachment att:statusForContent.mediaAttachments){
			if(att.type==Attachment.Type.AUDIO){
				items.add(new AudioStatusDisplayItem(parentID, fragment, statusForContent, att));
			}
		}
		if(statusForContent.poll!=null){
			buildPollItems(parentID, fragment, statusForContent.poll, items, statusForContent);
		}
		if(statusForContent.card!=null && statusForContent.mediaAttachments.isEmpty() && TextUtils.isEmpty(statusForContent.spoilerText)){
			items.add(new LinkCardStatusDisplayItem(parentID, fragment, statusForContent));
		}
		if(addFooter){
			items.add(new FooterStatusDisplayItem(parentID, fragment, statusForContent, accountID));
			if(status.hasGapAfter && !(fragment instanceof ThreadFragment)){
				items.add(new GapStatusDisplayItem(parentID, fragment));
			}
		}

		int i=1;
		for(StatusDisplayItem item:items){
			item.inset=inset;
			item.index=i++;
		}

		if (!statusForContent.filterRevealed) {
			return new ArrayList<>(List.of(
					new WarningFilteredStatusDisplayItem(parentID, fragment, statusForContent, items)
			));
		}

		return items;
	}

	public static void buildPollItems(String parentID, BaseStatusListFragment fragment, Poll poll, List<StatusDisplayItem> items, Status status){
		for(Poll.Option opt:poll.options){
			items.add(new PollOptionStatusDisplayItem(parentID, poll, opt, fragment, status));
		}
		items.add(new PollFooterStatusDisplayItem(parentID, fragment, poll, status));
	}

	public enum Type{
		HEADER,
		REBLOG_OR_REPLY_LINE,
		TEXT,
		AUDIO,
		POLL_OPTION,
		POLL_FOOTER,
		CARD,
		FOOTER,
		ACCOUNT_CARD,
		ACCOUNT,
		HASHTAG,
		GAP,
		WARNING,
		EXTENDED_FOOTER,
		MEDIA_GRID
	}

	public static abstract class Holder<T extends StatusDisplayItem> extends BindableViewHolder<T> implements UsableRecyclerView.DisableableClickable{
		public Holder(View itemView){
			super(itemView);
		}

		public Holder(Context context, int layout, ViewGroup parent){
			super(context, layout, parent);
		}

		public String getItemID(){
			return item.parentID;
		}

		@Override
		public void onClick(){
			item.parentFragment.onItemClick(item.parentID);
		}

		@Override
		public boolean isEnabled(){
			return item.parentFragment.isItemEnabled(item.parentID);
		}
	}
}
