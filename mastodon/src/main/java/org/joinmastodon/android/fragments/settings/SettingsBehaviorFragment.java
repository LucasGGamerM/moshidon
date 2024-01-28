package org.joinmastodon.android.fragments.settings;

import android.os.Bundle;

import androidx.annotation.StringRes;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountLocalPreferences;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.HasAccountID;
import org.joinmastodon.android.model.Preferences;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.viewcontrollers.ComposeLanguageAlertViewController;
import org.joinmastodon.android.utils.MastodonLanguage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SettingsBehaviorFragment extends BaseSettingsFragment<Void> implements HasAccountID{
	private ListItem<Void> languageItem;
	private CheckableListItem<Void> altTextItem, playGifsItem, customTabsItem, confirmUnfollowItem, confirmBoostItem, confirmDeleteItem;
	private MastodonLanguage postLanguage;
	private ComposeLanguageAlertViewController.SelectedOption newPostLanguage;

	// MEGALODON
	private MastodonLanguage.LanguageResolver languageResolver;
	private ListItem<Void> prefixRepliesItem, replyVisibilityItem;
	private CheckableListItem<Void> forwardReportsItem, remoteLoadingItem, showBoostsItem, showRepliesItem, loadNewPostsItem, seeNewPostsBtnItem, overlayMediaItem;

	// MOSHIDON
    private CheckableListItem<Void> mentionRebloggerAutomaticallyItem, hapticFeedbackItem, showPostsWithoutAltItem, disableRightToLeftCharacterItem;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.settings_behavior);

		AccountSession s=AccountSessionManager.get(accountID);
		AccountLocalPreferences lp=getLocalPrefs();
		languageResolver = s.getInstance().map(MastodonLanguage.LanguageResolver::new).orElse(null);
		postLanguage=s.preferences==null || s.preferences.postingDefaultLanguage==null ? null :
				languageResolver.from(s.preferences.postingDefaultLanguage).orElse(null);

		List<ListItem<Void>> items = new ArrayList<>(List.of(
				languageItem=new ListItem<>(getString(R.string.default_post_language), postLanguage!=null ? postLanguage.getDisplayName(getContext()) : null, R.drawable.ic_fluent_local_language_24_regular, this::onDefaultLanguageClick),
				altTextItem=new CheckableListItem<>(R.string.settings_alt_text_reminders, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.altTextReminders, R.drawable.ic_fluent_image_alt_text_24_regular, i->toggleCheckableItem(altTextItem)),
				showPostsWithoutAltItem=new CheckableListItem<>(R.string.mo_settings_show_posts_without_alt, R.string.mo_settings_show_posts_without_alt_summary, CheckableListItem.Style.SWITCH, GlobalUserPreferences.showPostsWithoutAlt, R.drawable.ic_fluent_eye_tracking_on_24_regular, i->toggleCheckableItem(showPostsWithoutAltItem)),
				playGifsItem=new CheckableListItem<>(R.string.settings_gif, R.string.mo_setting_play_gif_summary, CheckableListItem.Style.SWITCH, GlobalUserPreferences.playGifs, R.drawable.ic_fluent_gif_24_regular, i->toggleCheckableItem(playGifsItem)),
				overlayMediaItem=new CheckableListItem<>(R.string.sk_settings_continues_playback, R.string.sk_settings_continues_playback_summary, CheckableListItem.Style.SWITCH, GlobalUserPreferences.overlayMedia, R.drawable.ic_fluent_play_circle_hint_24_regular, i->toggleCheckableItem(overlayMediaItem)),
				customTabsItem=new CheckableListItem<>(R.string.settings_custom_tabs, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.useCustomTabs, R.drawable.ic_fluent_link_24_regular, i->toggleCheckableItem(customTabsItem)),
				confirmUnfollowItem=new CheckableListItem<>(R.string.settings_confirm_unfollow, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.confirmUnfollow, R.drawable.ic_fluent_person_delete_24_regular, i->toggleCheckableItem(confirmUnfollowItem)),
				confirmBoostItem=new CheckableListItem<>(R.string.settings_confirm_boost, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.confirmBoost, R.drawable.ic_fluent_arrow_repeat_all_24_regular, i->toggleCheckableItem(confirmBoostItem)),
				confirmDeleteItem=new CheckableListItem<>(R.string.settings_confirm_delete_post, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.confirmDeletePost, R.drawable.ic_fluent_delete_24_regular, i->toggleCheckableItem(confirmDeleteItem)),
				prefixRepliesItem=new ListItem<>(R.string.sk_settings_prefix_reply_cw_with_re, getPrefixWithRepliesString(), R.drawable.ic_fluent_arrow_reply_24_regular, this::onPrefixRepliesClick),
				forwardReportsItem=new CheckableListItem<>(R.string.sk_settings_forward_report_default, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.forwardReportDefault, R.drawable.ic_fluent_arrow_forward_24_regular, i->toggleCheckableItem(forwardReportsItem)),
				loadNewPostsItem=new CheckableListItem<>(R.string.sk_settings_load_new_posts, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.loadNewPosts, R.drawable.ic_fluent_arrow_sync_24_regular, i->onLoadNewPostsClick()),
				seeNewPostsBtnItem=new CheckableListItem<>(R.string.sk_settings_see_new_posts_button, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.showNewPostsButton, R.drawable.ic_fluent_arrow_up_24_regular, i->toggleCheckableItem(seeNewPostsBtnItem)),
				remoteLoadingItem=new CheckableListItem<>(R.string.sk_settings_allow_remote_loading, R.string.sk_settings_allow_remote_loading_explanation, CheckableListItem.Style.SWITCH, GlobalUserPreferences.allowRemoteLoading, R.drawable.ic_fluent_communication_24_regular, i->toggleCheckableItem(remoteLoadingItem)),
				mentionRebloggerAutomaticallyItem=new CheckableListItem<>(R.string.mo_mention_reblogger_automatically, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.mentionRebloggerAutomatically, R.drawable.ic_fluent_comment_mention_24_regular, i->toggleCheckableItem(mentionRebloggerAutomaticallyItem)),
				hapticFeedbackItem=new CheckableListItem<>(R.string.mo_haptic_feedback, R.string.mo_setting_haptic_feedback_summary, CheckableListItem.Style.SWITCH, GlobalUserPreferences.hapticFeedback, R.drawable.ic_fluent_phone_vibrate_24_regular, i->toggleCheckableItem(hapticFeedbackItem), true),
				disableRightToLeftCharacterItem=new CheckableListItem<>(R.string.mo_settings_disable_right_to_left_character, R.string.mo_setting_disable_right_to_left_character_summary, CheckableListItem.Style.SWITCH, GlobalUserPreferences.disableRightToLeftCharacter, R.drawable.ic_fluent_arrow_forward_24_regular, i->toggleCheckableItem(disableRightToLeftCharacterItem)),
				showBoostsItem=new CheckableListItem<>(R.string.sk_settings_show_boosts, 0, CheckableListItem.Style.SWITCH, lp.showBoosts, R.drawable.ic_fluent_arrow_repeat_all_24_regular, i->toggleCheckableItem(showBoostsItem)),
				showRepliesItem=new CheckableListItem<>(R.string.sk_settings_show_replies, 0, CheckableListItem.Style.SWITCH, lp.showReplies, R.drawable.ic_fluent_arrow_reply_24_regular, i->toggleCheckableItem(showRepliesItem))
		));

		if(isInstanceAkkoma()) items.add(
				replyVisibilityItem=new ListItem<>(R.string.sk_settings_reply_visibility, getReplyVisibilityString(), R.drawable.ic_fluent_chat_24_regular, this::onReplyVisibilityClick)
		);

		loadNewPostsItem.checkedChangeListener=checked->onLoadNewPostsClick();
		seeNewPostsBtnItem.isEnabled=loadNewPostsItem.checked;

		onDataLoaded(items);
	}

	private @StringRes int getPrefixWithRepliesString(){
		return switch(GlobalUserPreferences.prefixReplies){
			case NEVER -> R.string.sk_settings_prefix_replies_never;
			case ALWAYS -> R.string.sk_settings_prefix_replies_always;
			case TO_OTHERS -> R.string.sk_settings_prefix_replies_to_others;
		};
	}

	private @StringRes int getReplyVisibilityString(){
		AccountLocalPreferences lp=getLocalPrefs();
		if (lp.timelineReplyVisibility==null) return R.string.sk_settings_reply_visibility_all;
		return switch(lp.timelineReplyVisibility){
			case "following" -> R.string.sk_settings_reply_visibility_following;
			case "self" -> R.string.sk_settings_reply_visibility_self;
			default -> R.string.sk_settings_reply_visibility_all;
		};
	}

	@Override
	protected void doLoadData(int offset, int count){}

	private void onDefaultLanguageClick(ListItem<?> item){
		if (languageResolver == null) return;
		ComposeLanguageAlertViewController vc=new ComposeLanguageAlertViewController(getActivity(), null, new ComposeLanguageAlertViewController.SelectedOption(postLanguage), null, languageResolver);
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.default_post_language)
				.setView(vc.getView())
				.setPositiveButton(R.string.ok, (dlg, which)->{
					ComposeLanguageAlertViewController.SelectedOption opt=vc.getSelectedOption();
					if(!opt.language.equals(postLanguage)){
						newPostLanguage=opt;
						postLanguage=newPostLanguage.language;
						languageItem.subtitle=newPostLanguage.language.getDefaultName();
						rebindItem(languageItem);
					}
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void onPrefixRepliesClick(ListItem<?> item){
		int selected=GlobalUserPreferences.prefixReplies.ordinal();
		int[] newSelected={selected};
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.sk_settings_prefix_reply_cw_with_re)
				.setSingleChoiceItems((String[]) IntStream.of(R.string.sk_settings_prefix_replies_never, R.string.sk_settings_prefix_replies_always, R.string.sk_settings_prefix_replies_to_others).mapToObj(this::getString).toArray(String[]::new),
						selected, (dlg, which)->newSelected[0]=which)
				.setPositiveButton(R.string.ok, (dlg, which)->{
					GlobalUserPreferences.prefixReplies=GlobalUserPreferences.PrefixRepliesMode.values()[newSelected[0]];
					prefixRepliesItem.subtitleRes=getPrefixWithRepliesString();
					rebindItem(prefixRepliesItem);
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void onReplyVisibilityClick(ListItem<?> item){
		AccountLocalPreferences lp=getLocalPrefs();
		int selected=lp.timelineReplyVisibility==null ? 2 : switch(lp.timelineReplyVisibility){
			case "following" -> 0;
			case "self" -> 1;
			default -> 2;
		};
		int[] newSelected={selected};
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.sk_settings_prefix_reply_cw_with_re)
				.setSingleChoiceItems((String[]) IntStream.of(R.string.sk_settings_reply_visibility_following, R.string.sk_settings_reply_visibility_self, R.string.sk_settings_reply_visibility_all).mapToObj(this::getString).toArray(String[]::new),
						selected, (dlg, which)->newSelected[0]=which)
				.setPositiveButton(R.string.ok, (dlg, which)->{
					lp.timelineReplyVisibility=switch(newSelected[0]){
						case 0 -> "following";
						case 1 -> "self";
						default -> null;
					};
					replyVisibilityItem.subtitleRes=getReplyVisibilityString();
					rebindItem(replyVisibilityItem);
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void onLoadNewPostsClick(){
		toggleCheckableItem(loadNewPostsItem);
		seeNewPostsBtnItem.checked=loadNewPostsItem.checked;
		seeNewPostsBtnItem.isEnabled=loadNewPostsItem.checked;
		rebindItem(seeNewPostsBtnItem);
	}

	@Override
	protected void onHidden(){
		super.onHidden();
		GlobalUserPreferences.playGifs=playGifsItem.checked;
		GlobalUserPreferences.overlayMedia=overlayMediaItem.checked;
		GlobalUserPreferences.useCustomTabs=customTabsItem.checked;
		GlobalUserPreferences.altTextReminders=altTextItem.checked;
		GlobalUserPreferences.confirmUnfollow=confirmUnfollowItem.checked;
		GlobalUserPreferences.confirmBoost=confirmBoostItem.checked;
		GlobalUserPreferences.confirmDeletePost=confirmDeleteItem.checked;
		GlobalUserPreferences.forwardReportDefault=forwardReportsItem.checked;
		GlobalUserPreferences.loadNewPosts=loadNewPostsItem.checked;
		GlobalUserPreferences.showNewPostsButton=seeNewPostsBtnItem.checked;
		GlobalUserPreferences.allowRemoteLoading=remoteLoadingItem.checked;
		GlobalUserPreferences.mentionRebloggerAutomatically=mentionRebloggerAutomaticallyItem.checked;
		GlobalUserPreferences.hapticFeedback=hapticFeedbackItem.checked;
		GlobalUserPreferences.showPostsWithoutAlt=showPostsWithoutAltItem.checked;
		GlobalUserPreferences.disableRightToLeftCharacter=disableRightToLeftCharacterItem.checked;
		GlobalUserPreferences.save();
		AccountLocalPreferences lp=getLocalPrefs();
		boolean restartPlease=lp.showBoosts!=showBoostsItem.checked
				|| lp.showReplies!=showRepliesItem.checked;
		lp.showBoosts=showBoostsItem.checked;
		lp.showReplies=showRepliesItem.checked;
		lp.save();
		if(newPostLanguage!=null){
			AccountSession s=AccountSessionManager.get(accountID);
			if(s.preferences==null)
				s.preferences=new Preferences();
			s.preferences.postingDefaultLanguage=newPostLanguage.language.getLanguage();
			s.savePreferencesLater();
		}
		if(restartPlease) getActivity().recreate();
	}

	@Override
	public String getAccountID(){
		return accountID;
	}
}
