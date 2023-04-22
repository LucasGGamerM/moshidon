package org.joinmastodon.android.ui.displayitems;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountRelationships;
import org.joinmastodon.android.api.requests.announcements.DismissAnnouncement;
import org.joinmastodon.android.api.requests.statuses.CreateStatus;
import org.joinmastodon.android.api.requests.statuses.GetStatusSourceText;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.fragments.ListTimelinesFragment;
import org.joinmastodon.android.fragments.NotificationsListFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.fragments.ThreadFragment;
import org.joinmastodon.android.fragments.report.ReportReasonChoiceFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Announcement;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.ScheduledStatus;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class HeaderStatusDisplayItem extends StatusDisplayItem{
	private Account user;
	private Instant createdAt;
	private ImageLoaderRequest avaRequest;
	private String accountID;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
	private SpannableStringBuilder parsedName;
	public final Status status;
	private boolean hasVisibilityToggle;
	boolean needBottomPadding;
	private CharSequence extraText;
	private Notification notification;
	private ScheduledStatus scheduledStatus;
	private Announcement announcement;
	private Consumer<String> consumeReadAnnouncement;

	public HeaderStatusDisplayItem(String parentID, Account user, Instant createdAt, BaseStatusListFragment parentFragment, String accountID, Status status, CharSequence extraText, Notification notification, ScheduledStatus scheduledStatus){
		super(parentID, parentFragment);
		user=scheduledStatus != null ? AccountSessionManager.getInstance().getAccount(accountID).self : user;
		this.user=user;
		this.createdAt=createdAt;
		avaRequest=new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? user.avatar : user.avatarStatic, V.dp(50), V.dp(50));
		this.accountID=accountID;
		parsedName=new SpannableStringBuilder(user.displayName);
		this.status=status;
		this.notification=notification;
		this.scheduledStatus=scheduledStatus;
		HtmlParser.parseCustomEmoji(parsedName, user.emojis);
		emojiHelper.setText(parsedName);
		if(status!=null){
			hasVisibilityToggle=status.sensitive || !TextUtils.isEmpty(status.spoilerText);
			if(!hasVisibilityToggle && !status.mediaAttachments.isEmpty()){
				for(Attachment att:status.mediaAttachments){
					if(att.type!=Attachment.Type.AUDIO){
						hasVisibilityToggle=true;
						break;
					}
				}
			}
		}
		this.extraText=extraText;
		emojiHelper.addText(extraText);
	}

	public static HeaderStatusDisplayItem fromAnnouncement(Announcement a, Status fakeStatus, Account instanceUser, BaseStatusListFragment parentFragment, String accountID, Consumer<String> consumeReadID) {
		HeaderStatusDisplayItem item = new HeaderStatusDisplayItem(a.id, instanceUser, a.startsAt, parentFragment, accountID, fakeStatus, null, null, null);
		item.announcement = a;
		item.consumeReadAnnouncement = consumeReadID;
		return item;
	}

	@Override
	public Type getType(){
		return Type.HEADER;
	}

	@Override
	public int getImageCount(){
		return 1+emojiHelper.getImageCount();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		if(index>0){
			return emojiHelper.getImageRequest(index-1);
		}
		return avaRequest;
	}

	public static class Holder extends StatusDisplayItem.Holder<HeaderStatusDisplayItem> implements ImageLoaderViewHolder{
		private final TextView name, username, timestamp, extraText, separator;
		private final View collapseBtn;
		private final ImageView avatar, more, visibility, deleteNotification, unreadIndicator, collapseBtnIcon,botIcon;
		private final PopupMenu optionsMenu;
		private Relationship relationship;
		private APIRequest<?> currentRelationshipRequest;

		private static final ViewOutlineProvider roundCornersOutline=new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), V.dp(12));
			}
		};

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_header, parent);
			name=findViewById(R.id.name);
			username=findViewById(R.id.username);
			separator=findViewById(R.id.separator);
			timestamp=findViewById(R.id.timestamp);
			avatar=findViewById(R.id.avatar);
			more=findViewById(R.id.more);
			visibility=findViewById(R.id.visibility);
			deleteNotification=findViewById(R.id.delete_notification);
			unreadIndicator=findViewById(R.id.unread_indicator);
			collapseBtn=findViewById(R.id.collapse_btn);
			collapseBtnIcon=findViewById(R.id.collapse_btn_icon);
			botIcon=findViewById(R.id.bot_icon);
			extraText=findViewById(R.id.extra_text);
			avatar.setOnClickListener(this::onAvaClick);
			avatar.setOutlineProvider(roundCornersOutline);
			avatar.setClipToOutline(true);
			more.setOnClickListener(this::onMoreClick);
			visibility.setOnClickListener(v->item.parentFragment.onVisibilityIconClick(this));
			deleteNotification.setOnClickListener(v->UiUtils.confirmDeleteNotification(activity, item.parentFragment.getAccountID(), item.notification, ()->{
				if (item.parentFragment instanceof NotificationsListFragment fragment) {
					fragment.removeNotification(item.notification);
				}
			}));
			collapseBtn.setOnClickListener(l -> item.parentFragment.onToggleExpanded(item.status, getItemID()));

			optionsMenu=new PopupMenu(activity, more);

			optionsMenu.inflate(R.menu.post);

			optionsMenu.setOnMenuItemClickListener(menuItem->{
				Account account=item.user;
				int id=menuItem.getItemId();

				if(id==R.id.edit || id==R.id.delete_and_redraft) {
					final Bundle args=new Bundle();
					args.putString("account", item.parentFragment.getAccountID());
					args.putParcelable("editStatus", Parcels.wrap(item.status));
					boolean redraft = id==R.id.delete_and_redraft;
					if (redraft) {
						args.putBoolean("redraftStatus", true);
						if (item.parentFragment instanceof ThreadFragment thread && !thread.isItemEnabled(item.status.id)) {
							// ("enabled" = clickable; opened status is not clickable)
							// request navigation to the re-drafted status if status is currently opened
							args.putBoolean("navigateToStatus", true);
						}
					}
					if(!redraft && TextUtils.isEmpty(item.status.content) && TextUtils.isEmpty(item.status.spoilerText)){
						Nav.go(item.parentFragment.getActivity(), ComposeFragment.class, args);
					}else if(item.scheduledStatus!=null){
						args.putString("sourceText", item.status.text);
						args.putString("sourceSpoiler", item.status.spoilerText);
						args.putBoolean("redraftStatus", true);
						args.putParcelable("scheduledStatus", Parcels.wrap(item.scheduledStatus));
						Nav.go(item.parentFragment.getActivity(), ComposeFragment.class, args);
					}else{
						new GetStatusSourceText(item.status.id)
								.setCallback(new Callback<>(){
									@Override
									public void onSuccess(GetStatusSourceText.Response result){
										args.putString("sourceText", result.text);
										args.putString("sourceSpoiler", result.spoilerText);
										if (redraft) {
											UiUtils.confirmDeletePost(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), item.status, s->{
												Nav.go(item.parentFragment.getActivity(), ComposeFragment.class, args);
											}, true);
										} else {
											Nav.go(item.parentFragment.getActivity(), ComposeFragment.class, args);
										}
									}

									@Override
									public void onError(ErrorResponse error){
										error.showToast(item.parentFragment.getActivity());
									}
								})
								.wrapProgress(item.parentFragment.getActivity(), R.string.loading, true)
								.exec(item.parentFragment.getAccountID());
					}
				}else if(id==R.id.delete){
					if (item.scheduledStatus != null) {
						UiUtils.confirmDeleteScheduledPost(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), item.scheduledStatus, ()->{});
					} else {
						UiUtils.confirmDeletePost(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), item.status, s->{});
					}
				}else if(id==R.id.pin || id==R.id.unpin) {
					UiUtils.confirmPinPost(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), item.status, !item.status.pinned, s->{});
				}else if(id==R.id.mute){
					UiUtils.confirmToggleMuteUser(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), account, relationship!=null && relationship.muting, r->{});
				}else if(id==R.id.block){
					UiUtils.confirmToggleBlockUser(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), account, relationship!=null && relationship.blocking, r->{});
				}else if(id==R.id.report){
					Bundle args=new Bundle();
					args.putString("account", item.parentFragment.getAccountID());
					args.putParcelable("status", Parcels.wrap(item.status));
					args.putParcelable("reportAccount", Parcels.wrap(item.status.account));
					Nav.go(item.parentFragment.getActivity(), ReportReasonChoiceFragment.class, args);
				}else if(id==R.id.open_in_browser) {
					UiUtils.launchWebBrowser(activity, item.status.url);
				}else if(id==R.id.copy_link){
					UiUtils.copyText(parent, item.status.url);
				}else if(id==R.id.follow){
					if(relationship==null)
						return true;
					ProgressDialog progress=new ProgressDialog(activity);
					progress.setCancelable(false);
					progress.setMessage(activity.getString(R.string.loading));
					UiUtils.performAccountAction(activity, account, item.parentFragment.getAccountID(), relationship, null, visible->{
						if(visible)
							progress.show();
						else
							progress.dismiss();
					}, rel->{
						relationship=rel;
						Toast.makeText(activity, activity.getString(rel.following ? R.string.followed_user : rel.requested ? R.string.following_user_requested : R.string.unfollowed_user, account.getDisplayUsername()), Toast.LENGTH_SHORT).show();
					});
				}else if(id==R.id.block_domain){
					UiUtils.confirmToggleBlockDomain(activity, item.parentFragment.getAccountID(), account.getDomain(), relationship!=null && relationship.domainBlocking, ()->{});
				}else if(id==R.id.bookmark){
					AccountSessionManager.getInstance().getAccount(item.accountID).getStatusInteractionController().setBookmarked(item.status, !item.status.bookmarked);
				}else if(id==R.id.manage_user_lists){
					final Bundle args=new Bundle();
					args.putString("account", item.parentFragment.getAccountID());
					args.putString("profileAccount", account.id);
					args.putString("profileDisplayUsername", account.getDisplayUsername());
					Nav.go(item.parentFragment.getActivity(), ListTimelinesFragment.class, args);
				}

				if(!item.status.filterRevealed){
					this.itemView.setVisibility(View.GONE);
					ViewGroup.LayoutParams params = this.itemView.getLayoutParams();
					params.height = 0;
					params.width = 0;
					this.itemView.setLayoutParams(params);
//					item.parentFragment.notifyItemsChanged(this.getAbsoluteAdapterPosition());
				}
				return true;
			});
			UiUtils.enablePopupMenuIcons(activity, optionsMenu);

		}

//		public void setFilteredShown(){
//			this.itemView.setVisibility(View.VISIBLE);
//			params = this.itemView.getLayoutParams();
//			params.height = 0;
//			params.width = 0;
//			this.itemView.setLayoutParams(params);
//		}

		private void populateAccountsMenu(Menu menu) {
			List<AccountSession> sessions=AccountSessionManager.getInstance().getLoggedInAccounts();
			sessions.stream().filter(s -> !s.getID().equals(item.accountID)).forEach(s -> {
				String username = "@"+s.self.username+"@"+s.domain;
				menu.add(username).setOnMenuItemClickListener(c->{
					UiUtils.openURL(item.parentFragment.getActivity(), s.getID(), item.status.url, false);
					return true;
				});
			});
		}

		@Override
		public void onBind(HeaderStatusDisplayItem item){
			name.setText(item.parsedName);
			username.setText('@'+item.user.acct);
			botIcon.setVisibility(item.user.bot ? View.VISIBLE : View.GONE);
			botIcon.setColorFilter(username.getCurrentTextColor());
			separator.setVisibility(View.VISIBLE);

			if (item.scheduledStatus!=null)
				if (item.scheduledStatus.scheduledAt.isAfter(CreateStatus.DRAFTS_AFTER_INSTANT)) {
					timestamp.setText(R.string.sk_draft);
				} else {
					DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault());
					timestamp.setText(item.scheduledStatus.scheduledAt.atZone(ZoneId.systemDefault()).format(formatter));
				}
			else if ((!item.inset || item.status==null || item.status.editedAt==null) && item.createdAt != null)
				timestamp.setText(UiUtils.formatRelativeTimestamp(itemView.getContext(), item.createdAt));
			else if (item.status != null && item.status.editedAt != null)
				timestamp.setText(item.parentFragment.getString(R.string.edited_timestamp, UiUtils.formatRelativeTimestamp(itemView.getContext(), item.status.editedAt)));
			else {
				separator.setVisibility(View.GONE);
				timestamp.setText("");
			}
			visibility.setVisibility(item.hasVisibilityToggle && !item.inset ? View.VISIBLE : View.GONE);
			deleteNotification.setVisibility(GlobalUserPreferences.enableDeleteNotifications && item.notification!=null && !item.inset ? View.VISIBLE : View.GONE);
			if(item.hasVisibilityToggle){
				visibility.setImageResource(item.status.spoilerRevealed ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
				visibility.setContentDescription(item.parentFragment.getString(item.status.spoilerRevealed ? R.string.hide_content : R.string.reveal_content));
				if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
					visibility.setTooltipText(visibility.getContentDescription());
				}
			}
			itemView.setPadding(itemView.getPaddingLeft(), itemView.getPaddingTop(), itemView.getPaddingRight(), item.needBottomPadding ? V.dp(16) : 0);
			if(TextUtils.isEmpty(item.extraText)){
				if (item.status != null) {
					UiUtils.setExtraTextInfo(item.parentFragment.getContext(), extraText, item.status.visibility, item.status.localOnly);
				}
			}else{
				extraText.setVisibility(View.VISIBLE);
				extraText.setText(item.extraText);
			}
			more.setVisibility(item.inset || (item.notification != null && item.notification.report != null)
					? View.GONE : View.VISIBLE);
			avatar.setClickable(!item.inset);
			avatar.setContentDescription(item.parentFragment.getString(R.string.avatar_description, item.user.acct));
			if(currentRelationshipRequest!=null){
				currentRelationshipRequest.cancel();
			}
			relationship=null;

			String desc;
			if (item.announcement != null) {
				if (unreadIndicator.getVisibility() == View.GONE) {
					more.setAlpha(0f);
					unreadIndicator.setAlpha(0f);
					unreadIndicator.setVisibility(View.VISIBLE);
				}
				float alpha = item.announcement.read ? 0 : 1;
				more.setImageResource(R.drawable.ic_fluent_checkmark_20_filled);
				desc = item.parentFragment.getString(R.string.sk_mark_as_read);
				more.animate().alpha(alpha);
				unreadIndicator.animate().alpha(alpha);
				more.setEnabled(!item.announcement.read);
				more.setOnClickListener(v -> {
					if (item.announcement.read) return;
					new DismissAnnouncement(item.announcement.id).setCallback(new Callback<>() {
						@Override
						public void onSuccess(Object o) {
							item.consumeReadAnnouncement.accept(item.announcement.id);
							item.announcement.read = true;
							if (item.parentFragment.getActivity() == null) return;
							rebind();
						}

						@Override
						public void onError(ErrorResponse error) {
							error.showToast(item.parentFragment.getActivity());
						}
					}).exec(item.accountID);
				});
			} else {
				more.setImageResource(R.drawable.ic_fluent_more_vertical_20_filled);
				desc = item.parentFragment.getString(R.string.more_options);
				more.setOnClickListener(this::onMoreClick);
			}

			more.setContentDescription(desc);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) more.setTooltipText(desc);

			if (item.status == null || !item.status.textExpandable) {
				collapseBtn.setVisibility(View.GONE);
			} else {
				String collapseText = item.parentFragment.getString(item.status.textExpanded ? R.string.sk_collapse : R.string.sk_expand);
				collapseBtn.setVisibility(item.status.textExpandable ? View.VISIBLE : View.GONE);
				collapseBtn.setContentDescription(collapseText);
				if (GlobalUserPreferences.reduceMotion) collapseBtnIcon.setScaleY(item.status.textExpanded ? -1 : 1);
				else collapseBtnIcon.animate().scaleY(item.status.textExpanded ? -1 : 1).start();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) collapseBtn.setTooltipText(collapseText);
			}
		}

		@Override
		public void setImage(int index, Drawable drawable){
			if(index>0){
				item.emojiHelper.setImageDrawable(index-1, drawable);
				name.invalidate();
			}else{
				avatar.setImageDrawable(drawable);
			}
			if(drawable instanceof Animatable)
				((Animatable) drawable).start();
		}

		@Override
		public void clearImage(int index){
			setImage(index, null);
		}

		private void onAvaClick(View v){
			if (item.announcement != null) {
				UiUtils.openURL(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), item.user.url);
				return;
			}
			Bundle args=new Bundle();
			if(item.status != null && item.status.reloadWhenClicked){
				UiUtils.lookupAccount(v.getContext(), item.status.account, item.accountID, null, account -> {
					args.putString("account", item.accountID);
					args.putParcelable("profileAccount", Parcels.wrap(account));
					Nav.go(item.parentFragment.getActivity(), ProfileFragment.class, args);
				});
				return;
			}
			args.putString("account", item.accountID);
			args.putParcelable("profileAccount", Parcels.wrap(item.user));
			Nav.go(item.parentFragment.getActivity(), ProfileFragment.class, args);
		}

		private void onMoreClick(View v){
			updateOptionsMenu();
			optionsMenu.show();
			if(relationship==null && currentRelationshipRequest==null){
				currentRelationshipRequest=new GetAccountRelationships(Collections.singletonList(item.user.id))
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(List<Relationship> result){
								if(!result.isEmpty()){
									relationship=result.get(0);
									updateOptionsMenu();
								}
								currentRelationshipRequest=null;
							}

							@Override
							public void onError(ErrorResponse error){
								currentRelationshipRequest=null;
							}
						})
						.exec(item.parentFragment.getAccountID());
			}
		}

		private void updateOptionsMenu(){
			if (item.parentFragment.getActivity() == null) return;
			if (item.announcement != null) return;
			boolean hasMultipleAccounts = AccountSessionManager.getInstance().getLoggedInAccounts().size() > 1;
			Menu menu=optionsMenu.getMenu();

			MenuItem openWithAccounts = menu.findItem(R.id.open_with_account);
			SubMenu accountsMenu = openWithAccounts != null ? openWithAccounts.getSubMenu() : null;
			if (hasMultipleAccounts && accountsMenu != null) {
				openWithAccounts.setVisible(true);
				accountsMenu.clear();
				UiUtils.populateAccountsMenu(item.accountID, accountsMenu, s-> UiUtils.openURL(
						item.parentFragment.getActivity(), s.getID(), item.status.url, false
				));
			} else if (openWithAccounts != null) {
				openWithAccounts.setVisible(false);
			}

			Account account=item.user;
			String username = account.getShortUsername();
			boolean isOwnPost=AccountSessionManager.getInstance().isSelf(item.parentFragment.getAccountID(), account);
			boolean isPostScheduled=item.scheduledStatus!=null;
			menu.findItem(R.id.open_with_account).setVisible(!isPostScheduled && hasMultipleAccounts);
			menu.findItem(R.id.edit).setVisible(item.status!=null && isOwnPost);
			menu.findItem(R.id.delete).setVisible(item.status!=null && isOwnPost);
			menu.findItem(R.id.delete_and_redraft).setVisible(!isPostScheduled && item.status!=null && isOwnPost);
			menu.findItem(R.id.pin).setVisible(!isPostScheduled && item.status!=null && isOwnPost && !item.status.pinned);
			menu.findItem(R.id.unpin).setVisible(!isPostScheduled && item.status!=null && isOwnPost && item.status.pinned);
			menu.findItem(R.id.open_in_browser).setVisible(!isPostScheduled && item.status!=null);
			menu.findItem(R.id.copy_link).setVisible(!isPostScheduled && item.status!=null);
			MenuItem blockDomain=menu.findItem(R.id.block_domain);
			MenuItem mute=menu.findItem(R.id.mute);
			MenuItem block=menu.findItem(R.id.block);
			MenuItem report=menu.findItem(R.id.report);
			MenuItem follow=menu.findItem(R.id.follow);
			MenuItem manageUserLists = menu.findItem(R.id.manage_user_lists);
			MenuItem bookmark=menu.findItem(R.id.bookmark);
			bookmark.setVisible(false);
			/* disabled in megalodon: add/remove bookmark is already available through status footer
			if(item.status!=null){
				bookmark.setVisible(true);
				bookmark.setTitle(item.status.bookmarked ? R.string.remove_bookmark : R.string.add_bookmark);
			}else{
				bookmark.setVisible(false);
			}
			*/
			if(isPostScheduled || isOwnPost){
				mute.setVisible(false);
				block.setVisible(false);
				report.setVisible(false);
				follow.setVisible(false);
				blockDomain.setVisible(false);
				manageUserLists.setVisible(false);
			}else{
				mute.setVisible(true);
				// hiding when following to keep menu item count equal (trading it for user lists)
				block.setVisible(relationship == null || !relationship.following);
				report.setVisible(true);
				follow.setVisible(relationship==null || relationship.following || (!relationship.blocking && !relationship.blockedBy && !relationship.domainBlocking && !relationship.muting));
				mute.setTitle(item.parentFragment.getString(relationship!=null && relationship.muting ? R.string.unmute_user : R.string.mute_user, username));
				mute.setIcon(relationship!=null && relationship.muting ? R.drawable.ic_fluent_speaker_0_24_regular : R.drawable.ic_fluent_speaker_off_24_regular);
				UiUtils.insetPopupMenuIcon(item.parentFragment.getContext(), mute);
				block.setTitle(item.parentFragment.getString(relationship!=null && relationship.blocking ? R.string.unblock_user : R.string.block_user, username));
				report.setTitle(item.parentFragment.getString(R.string.report_user, username));
				// disabled in megalodon. domain blocks from a post clutters the context menu and looks out of place
//				if(!account.isLocal()){
//					blockDomain.setVisible(true);
//					blockDomain.setTitle(item.parentFragment.getString(relationship!=null && relationship.domainBlocking ? R.string.unblock_domain : R.string.block_domain, account.getDomain()));
//				}else{
					blockDomain.setVisible(false);
//				}
				boolean following = relationship!=null && relationship.following;
				follow.setTitle(item.parentFragment.getString(following ? R.string.unfollow_user : R.string.follow_user, username));
				follow.setIcon(following ? R.drawable.ic_fluent_person_delete_24_regular : R.drawable.ic_fluent_person_add_24_regular);
				manageUserLists.setVisible(relationship != null && relationship.following);
				manageUserLists.setTitle(item.parentFragment.getString(R.string.sk_lists_with_user, username));
				UiUtils.insetPopupMenuIcon(item.parentFragment.getContext(), follow);
			}

			workaroundChangingMenuItemWidths(menu, username);
		}

		// ugliest piece of code you'll see in a while: i measure the menu items' text widths to
		// determine the biggest one, because it's probably not being displayed at first
		// (before the relationship loaded). i take the largest one's size and add a space to the
		// last item ("open in browser") until it takes up as much space as the largest item.
		// goal: no more ugly ellipsis after the relationship loads in when opening the context menu
		// of a post
		private void workaroundChangingMenuItemWidths(Menu menu, String username) {
			String openInBrowserText = item.parentFragment.getString(R.string.open_in_browser);
			if (relationship == null) {
				float largestWidth = 0;
				Paint paint = new Paint();
				paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
				String[] otherStrings = new String[] {
						item.parentFragment.getString(R.string.unfollow_user, username),
						item.parentFragment.getString(R.string.unblock_user, username),
						item.parentFragment.getString(R.string.unmute_user, username),
						item.parentFragment.getString(R.string.sk_lists_with_user, username),
				};
				for (int i = 0; i < menu.size(); i++) {
					MenuItem item = menu.getItem(i);
					if (item.getItemId() == R.id.open_in_browser || !item.isVisible()) continue;
					float width = paint.measureText(menu.getItem(i).getTitle().toString());
					if (width > largestWidth) largestWidth = width;
				}
				for (String str : otherStrings) {
					float width = paint.measureText(str);
					if (width > largestWidth) largestWidth = width;
				}
				float textWidth = paint.measureText(openInBrowserText);
				float missingWidth = Math.max(0, largestWidth - textWidth);
				float singleSpaceWidth = paint.measureText(" ");
				int howManySpaces = (int) Math.ceil(missingWidth / singleSpaceWidth);
				String enlargedText = openInBrowserText + " ".repeat(howManySpaces);
				menu.findItem(R.id.open_in_browser).setTitle(enlargedText);
			} else {
				menu.findItem(R.id.open_in_browser).setTitle(openInBrowserText);
			}
		}
	}
}
