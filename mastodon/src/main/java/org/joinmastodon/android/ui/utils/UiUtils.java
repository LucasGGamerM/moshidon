package org.joinmastodon.android.ui.utils;

import static android.view.Menu.NONE;
import static org.joinmastodon.android.GlobalUserPreferences.theme;
import static org.joinmastodon.android.GlobalUserPreferences.trueBlackTheme;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.ext.SdkExtensions;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.StatusInteractionController;
import org.joinmastodon.android.api.mastodon.requests.accounts.GetAccountByHandle;
import org.joinmastodon.android.api.mastodon.requests.accounts.SetAccountBlocked;
import org.joinmastodon.android.api.mastodon.requests.accounts.SetAccountFollowed;
import org.joinmastodon.android.api.mastodon.requests.accounts.SetAccountMuted;
import org.joinmastodon.android.api.mastodon.requests.accounts.SetDomainBlocked;
import org.joinmastodon.android.api.mastodon.requests.accounts.AuthorizeFollowRequest;
import org.joinmastodon.android.api.mastodon.requests.accounts.RejectFollowRequest;
import org.joinmastodon.android.api.mastodon.requests.lists.DeleteList;
import org.joinmastodon.android.api.mastodon.requests.notifications.DismissNotification;
import org.joinmastodon.android.api.mastodon.requests.search.GetSearchResults;
import org.joinmastodon.android.api.mastodon.requests.statuses.CreateStatus;
import org.joinmastodon.android.api.mastodon.requests.statuses.DeleteStatus;
import org.joinmastodon.android.api.mastodon.requests.statuses.GetStatusByID;
import org.joinmastodon.android.api.mastodon.requests.statuses.SetStatusPinned;
import org.joinmastodon.android.api.mastodon.session.AccountSession;
import org.joinmastodon.android.api.mastodon.session.AccountSessionManager;
import org.joinmastodon.android.events.ScheduledStatusDeletedEvent;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.events.FollowRequestHandledEvent;
import org.joinmastodon.android.events.NotificationDeletedEvent;
import org.joinmastodon.android.events.RemoveAccountPostsEvent;
import org.joinmastodon.android.events.StatusDeletedEvent;
import org.joinmastodon.android.events.StatusUnpinnedEvent;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.fragments.HashtagTimelineFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.fragments.ThreadFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.ScheduledStatus;
import org.joinmastodon.android.model.SearchResults;
import org.joinmastodon.android.model.Searchable;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.text.CustomEmojiSpan;
import org.parceler.Parcels;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;
import okhttp3.MediaType;

public class UiUtils {
	private static Handler mainHandler = new Handler(Looper.getMainLooper());
	private static final DateTimeFormatter DATE_FORMATTER_SHORT_WITH_YEAR = DateTimeFormatter.ofPattern("d MMM uuuu"), DATE_FORMATTER_SHORT = DateTimeFormatter.ofPattern("d MMM");
	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT);
	public static int MAX_WIDTH;

	private UiUtils(){}

	public static void loadMaxWidth(Context ctx) {
		if (MAX_WIDTH == 0) MAX_WIDTH = (int) ctx.getResources().getDimension(R.dimen.layout_max_width);
	}

	public static void launchWebBrowser(Context context, String url) {
		try {
			if (GlobalUserPreferences.useCustomTabs) {
				new CustomTabsIntent.Builder()
						.setShowTitle(true)
						.build()
						.launchUrl(context, Uri.parse(url));
			}else{
				context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
			}
		}catch(ActivityNotFoundException x){
			Toast.makeText(context, R.string.no_app_to_handle_action, Toast.LENGTH_SHORT).show();
		}
	}

	public static String formatRelativeTimestamp(Context context, Instant instant){
		long t=instant.toEpochMilli();
		long now=System.currentTimeMillis();
		long diff=now-t;
		if(diff<1000L){
			return context.getString(R.string.time_now);
		}else if(diff<60_000L){
			return context.getString(R.string.time_seconds, diff/1000L);
		}else if(diff<3600_000L){
			return context.getString(R.string.time_minutes, diff/60_000L);
		}else if(diff<3600_000L*24L){
			return context.getString(R.string.time_hours, diff/3600_000L);
		}else{
			int days=(int)(diff/(3600_000L*24L));
			if(days>30){
				ZonedDateTime dt=instant.atZone(ZoneId.systemDefault());
				if(dt.getYear()==ZonedDateTime.now().getYear()){
					return DATE_FORMATTER_SHORT.format(dt);
				}else{
					return DATE_FORMATTER_SHORT_WITH_YEAR.format(dt);
				}
			}
			return context.getString(R.string.time_days, days);
		}
	}

	public static int alphaBlendColors(int color1, int color2, float alpha) {
		float alpha0 = 1f - alpha;
		int r = Math.round(((color1 >> 16) & 0xFF) * alpha0 + ((color2 >> 16) & 0xFF) * alpha);
		int g = Math.round(((color1 >> 8) & 0xFF) * alpha0 + ((color2 >> 8) & 0xFF) * alpha);
		int b = Math.round((color1 & 0xFF) * alpha0 + (color2 & 0xFF) * alpha);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	public static String formatRelativeTimestampAsMinutesAgo(Context context, Instant instant){
		long t=instant.toEpochMilli();
		long now=System.currentTimeMillis();
		long diff=now-t;
		if(diff<1000L){
			return context.getString(R.string.time_just_now);
		}else if(diff<60_000L){
			int secs=(int)(diff/1000L);
			return context.getResources().getQuantityString(R.plurals.x_seconds_ago, secs, secs);
		}else if(diff<3600_000L){
			int mins=(int)(diff/60_000L);
			return context.getResources().getQuantityString(R.plurals.x_minutes_ago, mins, mins);
		}else{
			return DATE_TIME_FORMATTER.format(instant.atZone(ZoneId.systemDefault()));
		}
	}

	public static String formatTimeLeft(Context context, Instant instant){
		long t=instant.toEpochMilli();
		long now=System.currentTimeMillis();
		long diff=t-now;
		if(diff<60_000L){
			int secs=(int)(diff/1000L);
			return context.getResources().getQuantityString(R.plurals.x_seconds_left, secs, secs);
		}else if(diff<3600_000L){
			int mins=(int)(diff/60_000L);
			return context.getResources().getQuantityString(R.plurals.x_minutes_left, mins, mins);
		}else if(diff<3600_000L*24L){
			int hours=(int)(diff/3600_000L);
			return context.getResources().getQuantityString(R.plurals.x_hours_left, hours, hours);
		}else{
			int days=(int)(diff/(3600_000L*24L));
			return context.getResources().getQuantityString(R.plurals.x_days_left, days, days);
		}
	}

	@SuppressLint("DefaultLocale")
	public static String abbreviateNumber(int n){
		if(n<1000){
			return String.format("%,d", n);
		}else if(n<1_000_000){
			float a=n/1000f;
			return a>99f ? String.format("%,dK", (int)Math.floor(a)) : String.format("%,.1fK", a);
		}else{
			float a=n/1_000_000f;
			return a>99f ? String.format("%,dM", (int)Math.floor(a)) : String.format("%,.1fM", n/1_000_000f);
		}
	}

	@SuppressLint("DefaultLocale")
	public static String abbreviateNumber(long n){
		if(n<1_000_000_000L)
			return abbreviateNumber((int)n);

		double a=n/1_000_000_000.0;
		return a>99f ? String.format("%,dB", (int)Math.floor(a)) : String.format("%,.1fB", n/1_000_000_000.0);
	}

	/**
	 * Android 6.0 has a bug where start and end compound drawables don't get tinted.
	 * This works around it by setting the tint colors directly to the drawables.
	 * @param textView
	 */
	public static void fixCompoundDrawableTintOnAndroid6(TextView textView){
		Drawable[] drawables=textView.getCompoundDrawablesRelative();
		for(int i=0;i<drawables.length;i++){
			if(drawables[i]!=null){
				Drawable tinted=drawables[i].mutate();
				tinted.setTintList(textView.getTextColors());
				drawables[i]=tinted;
			}
		}
		textView.setCompoundDrawablesRelative(drawables[0], drawables[1], drawables[2], drawables[3]);
	}

	public static void runOnUiThread(Runnable runnable){
		mainHandler.post(runnable);
	}

	public static void runOnUiThread(Runnable runnable, long delay){
		mainHandler.postDelayed(runnable, delay);
	}

	public static void removeCallbacks(Runnable runnable){
		mainHandler.removeCallbacks(runnable);
	}

	/** Linear interpolation between {@code startValue} and {@code endValue} by {@code fraction}. */
	public static int lerp(int startValue, int endValue, float fraction) {
		return startValue + Math.round(fraction * (endValue - startValue));
	}

	public static String getFileName(Uri uri){
		if(uri.getScheme().equals("content")){
			try(Cursor cursor=MastodonApp.context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)){
				cursor.moveToFirst();
				String name=cursor.getString(0);
				if(name!=null)
					return name;
			}catch(Throwable ignore){}
		}
		return uri.getLastPathSegment();
	}

	public static String formatFileSize(Context context, long size, boolean atLeastKB){
		if(size<1024 && !atLeastKB){
			return context.getString(R.string.file_size_bytes, size);
		}else if(size<1024*1024){
			return context.getString(R.string.file_size_kb, size/1024.0);
		}else if(size<1024*1024*1024){
			return context.getString(R.string.file_size_mb, size/(1024.0*1024.0));
		}else{
			return context.getString(R.string.file_size_gb, size/(1024.0*1024.0*1024.0));
		}
	}

	public static MediaType getFileMediaType(File file){
		String name=file.getName();
		return MediaType.parse(MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(name.lastIndexOf('.')+1)));
	}

	public static void loadCustomEmojiInTextView(TextView view){
		CharSequence _text=view.getText();
		if(!(_text instanceof Spanned))
			return;
		Spanned text=(Spanned)_text;
		CustomEmojiSpan[] spans=text.getSpans(0, text.length(), CustomEmojiSpan.class);
		if(spans.length==0)
			return;
		int emojiSize=V.dp(20);
		Map<Emoji, List<CustomEmojiSpan>> spansByEmoji=Arrays.stream(spans).collect(Collectors.groupingBy(s->s.emoji));
		for(Map.Entry<Emoji, List<CustomEmojiSpan>> emoji:spansByEmoji.entrySet()){
			ViewImageLoader.load(new ViewImageLoader.Target(){
				@Override
				public void setImageDrawable(Drawable d){
					if(d==null)
						return;
					for(CustomEmojiSpan span:emoji.getValue()){
						span.setDrawable(d);
					}
					view.invalidate();
				}

				@Override
				public View getView(){
					return view;
				}
			}, null, new UrlImageLoaderRequest(emoji.getKey().url, emojiSize, emojiSize), null, false, true);
		}
	}

	public static int getThemeColor(Context context, @AttrRes int attr) {
		if (context == null) return 0xff00ff00;
		TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
		int color = ta.getColor(0, 0xff00ff00);
		ta.recycle();
		return color;
	}

	public static int getThemeColorRes(Context context, @AttrRes int attr) {
		if (context == null) return 0xff00ff00;
		TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
		int color = ta.getResourceId(0, R.color.black);
		ta.recycle();
		return color;
	}

	public static void openProfileByID(Context context, String selfID, String id) {
		Bundle args = new Bundle();
		args.putString("account", selfID);
		args.putString("profileAccountID", id);
		Nav.go((Activity)context, ProfileFragment.class, args);
	}

	public static void openHashtagTimeline(Context context, String accountID, String hashtag, @Nullable Boolean following){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putString("hashtag", hashtag);
		if (following != null) args.putBoolean("following", following);
		Nav.go((Activity)context, HashtagTimelineFragment.class, args);
	}

	public static void showConfirmationAlert(Context context, @StringRes int title, @StringRes int message, @StringRes int confirmButton, Runnable onConfirmed){
		showConfirmationAlert(context, title, message, confirmButton, 0, onConfirmed);
	}

	public static void showConfirmationAlert(Context context, @StringRes int title, @StringRes int message, @StringRes int confirmButton, @DrawableRes int icon, Runnable onConfirmed){
		showConfirmationAlert(context, context.getString(title), context.getString(message), context.getString(confirmButton), icon, onConfirmed);
	}

	public static void showConfirmationAlert(Context context, CharSequence title, CharSequence message, CharSequence confirmButton, int icon, Runnable onConfirmed){
		new M3AlertDialogBuilder(context)
				.setTitle(title)
				.setMessage(message)
				.setPositiveButton(confirmButton, (dlg, i)->onConfirmed.run())
				.setNegativeButton(R.string.cancel, null)
				.setIcon(icon)
				.show();
	}

	public static void confirmToggleBlockUser(Activity activity, String accountID, Account account, boolean currentlyBlocked, Consumer<Relationship> resultCallback){
		showConfirmationAlert(activity, activity.getString(currentlyBlocked ? R.string.confirm_unblock_title : R.string.confirm_block_title),
				activity.getString(currentlyBlocked ? R.string.confirm_unblock : R.string.confirm_block, account.displayName),
				activity.getString(currentlyBlocked ? R.string.do_unblock : R.string.do_block),
				R.drawable.ic_fluent_person_prohibited_28_regular,
				()->{
					new SetAccountBlocked(account.id, !currentlyBlocked)
							.setCallback(new Callback<>(){
								@Override
								public void onSuccess(Relationship result){
									if (activity == null) return;
									resultCallback.accept(result);
									if(!currentlyBlocked){
										E.post(new RemoveAccountPostsEvent(accountID, account.id, false));
									}
								}

								@Override
								public void onError(ErrorResponse error){
									error.showToast(activity);
								}
							})
							.wrapProgress(activity, R.string.loading, false)
							.exec(accountID);
				});
	}

	public static void confirmSoftBlockUser(Activity activity, String accountID, Account account, Consumer<Relationship> resultCallback){
		showConfirmationAlert(activity,
				activity.getString(R.string.sk_remove_follower),
				activity.getString(R.string.sk_remove_follower_confirm, account.displayName),
				activity.getString(R.string.sk_do_remove_follower),
				R.drawable.ic_fluent_person_delete_24_regular,
				() -> new SetAccountBlocked(account.id, true).setCallback(new Callback<>() {
						@Override
						public void onSuccess(Relationship relationship) {
							new SetAccountBlocked(account.id, false).setCallback(new Callback<>() {
								@Override
								public void onSuccess(Relationship relationship) {
									if (activity == null) return;
									Toast.makeText(activity, R.string.sk_remove_follower_success, Toast.LENGTH_SHORT).show();
									resultCallback.accept(relationship);
								}

								@Override
								public void onError(ErrorResponse error) {
									error.showToast(activity);
									resultCallback.accept(relationship);
								}
							}).exec(accountID);
						}

						@Override
						public void onError(ErrorResponse error) {
							error.showToast(activity);
						}
					}).exec(accountID)
		);
	}

	public static void confirmToggleBlockDomain(Activity activity, String accountID, String domain, boolean currentlyBlocked, Runnable resultCallback){
		showConfirmationAlert(activity, activity.getString(currentlyBlocked ? R.string.confirm_unblock_domain_title : R.string.confirm_block_domain_title),
				activity.getString(currentlyBlocked ? R.string.confirm_unblock : R.string.confirm_block, domain),
				activity.getString(currentlyBlocked ? R.string.do_unblock : R.string.do_block),
				R.drawable.ic_fluent_shield_28_regular,
				()->{
					new SetDomainBlocked(domain, !currentlyBlocked)
							.setCallback(new Callback<>(){
								@Override
								public void onSuccess(Object result){
									resultCallback.run();
								}

								@Override
								public void onError(ErrorResponse error){
									error.showToast(activity);
								}
							})
							.wrapProgress(activity, R.string.loading, false)
							.exec(accountID);
				});
	}

	public static void confirmToggleMuteUser(Activity activity, String accountID, Account account, boolean currentlyMuted, Consumer<Relationship> resultCallback){
		View menu = LayoutInflater.from(activity).inflate(R.layout.item_mute_duration, null);
		Button button = menu.findViewById(R.id.button);

		AtomicReference<Duration> muteDuration = new AtomicReference<>(Duration.ZERO);

		PopupMenu popupMenu=new PopupMenu(activity, button, Gravity.CENTER_HORIZONTAL);
		popupMenu.inflate(R.menu.mute_duration);
		popupMenu.setOnMenuItemClickListener(item -> {
			int id = item.getItemId();
			if (id == R.id.duration_indefinite)
				muteDuration.set(Duration.ZERO);
			else if (id == R.id.duration_minutes_5) {
				muteDuration.set(Duration.ofMinutes(5));
			}else if (id == R.id.duration_minutes_30) {
				muteDuration.set(Duration.ofMinutes(30));
			}else if (id == R.id.duration_hours_1) {
				muteDuration.set(Duration.ofHours(1));
			}else if (id == R.id.duration_hours_6) {
				muteDuration.set(Duration.ofHours(6));
			}else if (id == R.id.duration_days_1) {
				muteDuration.set(Duration.ofDays(1));
			}else if (id == R.id.duration_days_3) {
				muteDuration.set(Duration.ofDays(3));
			}else if (id == R.id.duration_days_7) {
				muteDuration.set(Duration.ofDays(7));
			}
			button.setText(item.getTitle());
			return true;
		});
		button.setOnTouchListener(popupMenu.getDragToOpenListener());
		button.setOnClickListener(v->popupMenu.show());
		button.setText(popupMenu.getMenu().getItem(0).getTitle());

		new M3AlertDialogBuilder(activity)
				.setTitle(activity.getString(currentlyMuted ? R.string.confirm_unmute_title : R.string.confirm_mute_title))
				.setMessage(activity.getString(currentlyMuted ? R.string.confirm_unmute : R.string.confirm_mute, account.displayName))
				.setView(currentlyMuted ? null : menu)
				.setPositiveButton(activity.getString(currentlyMuted ? R.string.do_unmute : R.string.do_mute), (dlg, i)-> {
					new SetAccountMuted(account.id, !currentlyMuted, muteDuration.get().getSeconds())
							.setCallback(new Callback<>(){
								@Override
								public void onSuccess(Relationship result){
									resultCallback.accept(result);
									if(!currentlyMuted){
										E.post(new RemoveAccountPostsEvent(accountID, account.id, false));
									}
								}

								@Override
								public void onError(ErrorResponse error){
									error.showToast(activity);
								}
							})
							.wrapProgress(activity, R.string.loading, false)
							.exec(accountID);
				})
				.setNegativeButton(R.string.cancel, null)
				.setIcon(currentlyMuted ? R.drawable.ic_fluent_speaker_0_28_regular : R.drawable.ic_fluent_speaker_off_28_regular)
				.show();
	}
	public static void confirmDeletePost(Activity activity, String accountID, Status status, Consumer<Status> resultCallback){
		confirmDeletePost(activity, accountID, status, resultCallback, false);
	}

	public static void confirmDeletePost(Activity activity, String accountID, Status status, Consumer<Status> resultCallback, boolean forRedraft){
		showConfirmationAlert(activity,
				forRedraft ? R.string.sk_confirm_delete_and_redraft_title : R.string.confirm_delete_title,
				forRedraft ? R.string.sk_confirm_delete_and_redraft : R.string.confirm_delete,
				forRedraft ? R.string.sk_delete_and_redraft : R.string.delete,
				forRedraft ? R.drawable.ic_fluent_arrow_clockwise_28_regular : R.drawable.ic_fluent_delete_28_regular,
				() -> new DeleteStatus(status.id)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Status result){
								resultCallback.accept(result);
								AccountSessionManager.getInstance().getAccount(accountID).getCacheController().deleteStatus(status.id);
								E.post(new StatusDeletedEvent(status.id, accountID));
							}

							@Override
							public void onError(ErrorResponse error){
								error.showToast(activity);
							}
						})
						.wrapProgress(activity, R.string.deleting, false)
						.exec(accountID)
		);
	}

	public static void confirmDeleteScheduledPost(Activity activity, String accountID, ScheduledStatus status, Runnable resultCallback){
		boolean isDraft = status.scheduledAt.isAfter(CreateStatus.DRAFTS_AFTER_INSTANT);
		showConfirmationAlert(activity,
				isDraft ? R.string.sk_confirm_delete_draft_title : R.string.sk_confirm_delete_scheduled_post_title,
				isDraft ? R.string.sk_confirm_delete_draft : R.string.sk_confirm_delete_scheduled_post,
				R.string.delete,
				R.drawable.ic_fluent_delete_28_regular,
				() -> new DeleteStatus.Scheduled(status.id)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Object o){
								resultCallback.run();
								E.post(new ScheduledStatusDeletedEvent(status.id, accountID));
							}

							@Override
							public void onError(ErrorResponse error){
								error.showToast(activity);
							}
						})
						.wrapProgress(activity, R.string.deleting, false)
						.exec(accountID)
		);
	}

	public static void confirmPinPost(Activity activity, String accountID, Status status, boolean pinned, Consumer<Status> resultCallback){
		showConfirmationAlert(activity,
				pinned ? R.string.sk_confirm_pin_post_title : R.string.sk_confirm_unpin_post_title,
				pinned ? R.string.sk_confirm_pin_post : R.string.sk_confirm_unpin_post,
				pinned ? R.string.sk_pin_post : R.string.sk_unpin_post,
				pinned ? R.drawable.ic_fluent_pin_28_regular : R.drawable.ic_fluent_pin_off_28_regular,
				()->{
					new SetStatusPinned(status.id, pinned)
							.setCallback(new Callback<>() {
								@Override
								public void onSuccess(Status result) {
									resultCallback.accept(result);
									E.post(new StatusCountersUpdatedEvent(result));
									if (!result.pinned)
										E.post(new StatusUnpinnedEvent(status.id, accountID));
								}

								@Override
								public void onError(ErrorResponse error) {
									error.showToast(activity);
								}
							})
							.wrapProgress(activity, pinned ? R.string.sk_pinning : R.string.sk_unpinning, false)
							.exec(accountID);
				}
		);
	}

	public static void confirmDeleteNotification(Activity activity, String accountID, Notification notification, Runnable callback) {
		showConfirmationAlert(activity,
				notification == null ? R.string.sk_clear_all_notifications : R.string.sk_delete_notification,
				notification == null ? R.string.sk_clear_all_notifications_confirm : R.string.sk_delete_notification_confirm,
				notification == null ? R.string.sk_clear_all_notifications_confirm_action : R.string.sk_delete_notification_confirm_action,
				notification == null ? R.drawable.ic_fluent_mail_inbox_dismiss_28_regular : R.drawable.ic_fluent_delete_28_regular,
				() -> new DismissNotification(notification != null ? notification.id : null).setCallback(new Callback<>() {
					@Override
					public void onSuccess(Object o) {
						callback.run();
					}

					@Override
					public void onError(ErrorResponse error) {
						error.showToast(activity);
					}
				}).exec(accountID)
		);
	}

	public static void confirmDeleteList(Activity activity, String accountID, String listID, String listTitle, Runnable callback) {
		showConfirmationAlert(activity,
				activity.getString(R.string.sk_delete_list),
				activity.getString(R.string.sk_delete_list_confirm, listTitle),
				activity.getString(R.string.delete),
				R.drawable.ic_fluent_delete_28_regular,
				() -> new DeleteList(listID).setCallback(new Callback<>() {
							@Override
							public void onSuccess(Object o) {
								callback.run();
							}

							@Override
							public void onError(ErrorResponse error) {
								error.showToast(activity);
							}
						})
						.wrapProgress(activity, R.string.deleting, false)
						.exec(accountID));
	}

	public static void setRelationshipToActionButton(Relationship relationship, Button button){
		setRelationshipToActionButton(relationship, button, false);
	}

	public static void setRelationshipToActionButton(Relationship relationship, Button button, boolean keepText){
		CharSequence textBefore = keepText ? button.getText() : null;
		boolean secondaryStyle;
		if(relationship.blocking){
			button.setText(R.string.button_blocked);
			secondaryStyle=true;
		}else if(relationship.blockedBy){
			button.setText(R.string.button_follow);
			secondaryStyle=false;
		}else if(relationship.requested){
			button.setText(R.string.button_follow_pending);
			secondaryStyle=true;
		}else if(!relationship.following){
			button.setText(relationship.followedBy ? R.string.follow_back : R.string.button_follow);
			secondaryStyle=false;
		}else{
			button.setText(R.string.button_following);
			secondaryStyle=true;
		}

		if (keepText) button.setText(textBefore);

		button.setEnabled(!relationship.blockedBy);
		int attr=secondaryStyle ? R.attr.secondaryButtonStyle : android.R.attr.buttonStyle;
		TypedArray ta=button.getContext().obtainStyledAttributes(new int[]{attr});
		int styleRes=ta.getResourceId(0, 0);
		ta.recycle();
		ta=button.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.background});
		button.setBackground(ta.getDrawable(0));
		ta.recycle();
		ta=button.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.textColor});
		if(relationship.blocking)
			button.setTextColor(button.getResources().getColorStateList(R.color.error_600));
		else
			button.setTextColor(ta.getColorStateList(0));
		ta.recycle();
	}

	public static void performToggleAccountNotifications(Activity activity, Account account, String accountID, Relationship relationship, Button button, Consumer<Boolean> progressCallback, Consumer<Relationship> resultCallback) {
		progressCallback.accept(true);
		new SetAccountFollowed(account.id, true, relationship.showingReblogs, !relationship.notifying)
				.setCallback(new Callback<>() {
					@Override
					public void onSuccess(Relationship result) {
						resultCallback.accept(result);
						progressCallback.accept(false);
						Toast.makeText(activity, activity.getString(result.notifying ? R.string.sk_user_post_notifications_on : R.string.sk_user_post_notifications_off, '@'+account.username), Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onError(ErrorResponse error) {
						progressCallback.accept(false);
						error.showToast(activity);
					}
				}).exec(accountID);
	}

	public static void setRelationshipToActionButtonM3(Relationship relationship, Button button){
		boolean secondaryStyle;
		if(relationship.blocking){
			button.setText(R.string.button_blocked);
			secondaryStyle=true;
		}else if(relationship.blockedBy){
			button.setText(R.string.button_follow);
			secondaryStyle=false;
		}else if(relationship.requested){
			button.setText(R.string.button_follow_pending);
			secondaryStyle=true;
		}else if(!relationship.following){
			button.setText(relationship.followedBy ? R.string.follow_back : R.string.button_follow);
			secondaryStyle=false;
		}else{
			button.setText(R.string.button_following);
			secondaryStyle=true;
		}

		button.setEnabled(!relationship.blockedBy);
		int styleRes=secondaryStyle ? R.style.Widget_Mastodon_M3_Button_Tonal : R.style.Widget_Mastodon_M3_Button_Filled;
		TypedArray ta=button.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.background});
		button.setBackground(ta.getDrawable(0));
		ta.recycle();
		ta=button.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.textColor});
		button.setTextColor(ta.getColorStateList(0));
		ta.recycle();
	}

	public static void performAccountAction(Activity activity, Account account, String accountID, Relationship relationship, Button button, Consumer<Boolean> progressCallback, Consumer<Relationship> resultCallback) {
		if(relationship == null){
			UiUtils.lookupAccount(button.getContext(), account, accountID, null, account1 -> {
				if(account1 == null){
					return;
				}
				progressCallback.accept(true);
				new SetAccountFollowed(account1.id, true, true, false)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Relationship result){
								resultCallback.accept(result);
								progressCallback.accept(false);
								if(!result.following && !result.requested){
									E.post(new RemoveAccountPostsEvent(accountID, account.id, true));
								}
							}

							@Override
							public void onError(ErrorResponse error){
								error.showToast(activity);
								progressCallback.accept(false);
							}
						})
						.exec(accountID);
			});
			return;
		}
		if (relationship.blocking) {
			confirmToggleBlockUser(activity, accountID, account, true, resultCallback);
		}else if(relationship.muting){
			confirmToggleMuteUser(activity, accountID, account, true, resultCallback);
		}else{
			progressCallback.accept(true);
			new SetAccountFollowed(account.id, !relationship.following && !relationship.requested, true, false)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Relationship result){
							resultCallback.accept(result);
							progressCallback.accept(false);
							if(!result.following && !result.requested){
								E.post(new RemoveAccountPostsEvent(accountID, account.id, true));
							}
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(activity);
							progressCallback.accept(false);
						}
					})
					.exec(accountID);
		}
	}


	public static void handleFollowRequest(Activity activity, Account account, String accountID, @Nullable String notificationID, boolean accepted, Relationship relationship, Consumer<Relationship> resultCallback) {
		if (accepted) {
			new AuthorizeFollowRequest(account.id).setCallback(new Callback<>() {
				@Override
				public void onSuccess(Relationship rel) {
					E.post(new FollowRequestHandledEvent(accountID, true, account, rel));
					resultCallback.accept(rel);
				}

				@Override
				public void onError(ErrorResponse error) {
					resultCallback.accept(relationship);
					error.showToast(activity);
				}
			}).exec(accountID);
		} else {
			new RejectFollowRequest(account.id).setCallback(new Callback<>() {
				@Override
				public void onSuccess(Relationship rel) {
					E.post(new FollowRequestHandledEvent(accountID, false, account, rel));
					if (notificationID != null) E.post(new NotificationDeletedEvent(notificationID));
					resultCallback.accept(rel);
				}

				@Override
				public void onError(ErrorResponse error) {
					resultCallback.accept(relationship);
					error.showToast(activity);
				}
			}).exec(accountID);
		}
	}

	public static <T> void updateList(List<T> oldList, List<T> newList, RecyclerView list, RecyclerView.Adapter<?> adapter, BiPredicate<T, T> areItemsSame){
		// Save topmost item position and offset because for some reason RecyclerView would scroll the list to weird places when you insert items at the top
		int topItem, topItemOffset;
		if(list.getChildCount()==0){
			topItem=topItemOffset=0;
		}else{
			View child=list.getChildAt(0);
			topItem=list.getChildAdapterPosition(child);
			topItemOffset=child.getTop();
		}
		DiffUtil.calculateDiff(new DiffUtil.Callback(){
			@Override
			public int getOldListSize(){
				return oldList.size();
			}

			@Override
			public int getNewListSize(){
				return newList.size();
			}

			@Override
			public boolean areItemsTheSame(int oldItemPosition, int newItemPosition){
				return areItemsSame.test(oldList.get(oldItemPosition), newList.get(newItemPosition));
			}

			@Override
			public boolean areContentsTheSame(int oldItemPosition, int newItemPosition){
				return true;
			}
		}).dispatchUpdatesTo(adapter);
		list.scrollToPosition(topItem);
		list.scrollBy(0, topItemOffset);
	}

	public static Bitmap getBitmapFromDrawable(Drawable d){
		if(d instanceof BitmapDrawable)
			return ((BitmapDrawable) d).getBitmap();
		Bitmap bitmap=Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
		d.draw(new Canvas(bitmap));
		return bitmap;
	}

	public static void insetPopupMenuIcon(Context context, MenuItem item) {
		ColorStateList iconTint=ColorStateList.valueOf(UiUtils.getThemeColor(context, android.R.attr.textColorSecondary));
		insetPopupMenuIcon(item, iconTint);
	}
	public static void insetPopupMenuIcon(MenuItem item, ColorStateList iconTint) {
		Drawable icon=item.getIcon().mutate();
		if(Build.VERSION.SDK_INT>=26) item.setIconTintList(iconTint);
		else icon.setTintList(iconTint);
		icon=new InsetDrawable(icon, V.dp(8), 0, V.dp(8), 0);
		item.setIcon(icon);
		SpannableStringBuilder ssb=new SpannableStringBuilder(item.getTitle());
		item.setTitle(ssb);
	}

	public static void resetPopupItemTint(MenuItem item) {
		if(Build.VERSION.SDK_INT>=26) {
			item.setIconTintList(null);
		} else {
			Drawable icon=item.getIcon().mutate();
			icon.setTintList(null);
			item.setIcon(icon);
		}
	}

	public static void enableOptionsMenuIcons(Context context, Menu menu, @IdRes int... asAction) {
		if(menu.getClass().getSimpleName().equals("MenuBuilder")){
			try {
				Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
				m.setAccessible(true);
				m.invoke(menu, true);
				enableMenuIcons(context, menu, asAction);
			}
			catch(Exception ignored){}
		}
	}

	public static void enableMenuIcons(Context context, Menu m, @IdRes int... exclude) {
		ColorStateList iconTint=ColorStateList.valueOf(UiUtils.getThemeColor(context, android.R.attr.textColorSecondary));
		for(int i=0;i<m.size();i++){
			MenuItem item=m.getItem(i);
			SubMenu subMenu = item.getSubMenu();
			if (subMenu != null) enableMenuIcons(context, subMenu, exclude);
			if (item.getIcon() == null || Arrays.stream(exclude).anyMatch(id -> id == item.getItemId())) continue;
			insetPopupMenuIcon(item, iconTint);
		}
	}

	public static void enablePopupMenuIcons(Context context, PopupMenu menu){
		Menu m=menu.getMenu();
		if(Build.VERSION.SDK_INT>=29){
			menu.setForceShowIcon(true);
		}else{
			try{
				Method setOptionalIconsVisible=m.getClass().getDeclaredMethod("setOptionalIconsVisible", boolean.class);
				setOptionalIconsVisible.setAccessible(true);
				setOptionalIconsVisible.invoke(m, true);
			}catch(Exception ignore){}
		}
		enableMenuIcons(context, m);
	}

	public static void setUserPreferredTheme(Context context){
		context.setTheme(switch (theme) {
			case LIGHT -> R.style.Theme_Mastodon_Light;
			case DARK -> trueBlackTheme ? R.style.Theme_Mastodon_Dark_TrueBlack : R.style.Theme_Mastodon_Dark;
			default -> trueBlackTheme ? R.style.Theme_Mastodon_AutoLightDark_TrueBlack : R.style.Theme_Mastodon_AutoLightDark;
		});

		ColorPalette palette = ColorPalette.palettes.get(GlobalUserPreferences.color);
		if (palette != null) palette.apply(context);
	}
	public static boolean isDarkTheme(){
		if(theme==GlobalUserPreferences.ThemePreference.AUTO)
			return (MastodonApp.context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
		return theme==GlobalUserPreferences.ThemePreference.DARK;
	}

	// https://mastodon.foo.bar/@User
	// https://mastodon.foo.bar/@User/43456787654678
	// https://pleroma.foo.bar/users/User
	// https://pleroma.foo.bar/users/9qTHT2ANWUdXzENqC0
	// https://pleroma.foo.bar/notice/9sBHWIlwwGZi5QGlHc
	// https://pleroma.foo.bar/objects/d4643c42-3ae0-4b73-b8b0-c725f5819207
	// https://friendica.foo.bar/profile/user
	// https://friendica.foo.bar/display/d4643c42-3ae0-4b73-b8b0-c725f5819207
	// https://misskey.foo.bar/notes/83w6r388br (always lowercase)
	// https://pixelfed.social/p/connyduck/391263492998670833
	// https://pixelfed.social/connyduck
	// https://gts.foo.bar/@goblin/statuses/01GH9XANCJ0TA8Y95VE9H3Y0Q2
	// https://gts.foo.bar/@goblin
	// https://foo.microblog.pub/o/5b64045effd24f48a27d7059f6cb38f5
	//
	// COPIED FROM https://github.com/tuskyapp/Tusky/blob/develop/app/src/main/java/com/keylesspalace/tusky/util/LinkHelper.kt
	public static boolean looksLikeMastodonUrl(String urlString) {
		if(urlString == null)
			return false;
		URI uri;
		try {
			uri = new URI(urlString);
		} catch (URISyntaxException e) {
			return false;
		}

		if (uri.getQuery() != null || uri.getFragment() != null || uri.getPath() == null) return false;

		String it = uri.getPath();
		return it.matches("^/@[^/]+$") ||
				it.matches("^/@[^/]+/\\d+$") ||
				it.matches("^/users/\\w+$") ||
				it.matches("^/notice/[a-zA-Z0-9]+$") ||
				it.matches("^/objects/[-a-f0-9]+$") ||
				it.matches("^/notes/[a-z0-9]+$") ||
				it.matches("^/display/[-a-f0-9]+$") ||
				it.matches("^/profile/\\w+$") ||
				it.matches("^/p/\\w+/\\d+$") ||
				it.matches("^/\\w+$") ||
				it.matches("^/@[^/]+/statuses/[a-zA-Z0-9]+$") ||
				it.matches("^/users/[^/]+/statuses/[a-zA-Z0-9]+$") ||
				it.matches("^/o/[a-f0-9]+$");
	}

	public static String getInstanceName(String accountID) {
		AccountSession session = AccountSessionManager.getInstance().getAccount(accountID);
		Instance instance = AccountSessionManager.getInstance().getInstanceInfo(session.domain);
		return instance != null && !instance.title.isBlank() ? instance.title : session.domain;
	}

	public static void pickAccount(Context context, String exceptFor, @StringRes int titleRes, @DrawableRes int iconRes, Consumer<AccountSession> sessionConsumer, Consumer<AlertDialog.Builder> transformDialog) {
		List<AccountSession> sessions=AccountSessionManager.getInstance().getLoggedInAccounts()
				.stream().filter(s->!s.getID().equals(exceptFor)).collect(Collectors.toList());

		AlertDialog.Builder builder = new M3AlertDialogBuilder(context)
				.setItems(
						sessions.stream().map(AccountSession::getFullUsername).toArray(String[]::new),
						(dialog, which) -> sessionConsumer.accept(sessions.get(which))
				)
				.setTitle(titleRes == 0 ? R.string.choose_account : titleRes)
				.setIcon(iconRes);
		if (transformDialog != null) transformDialog.accept(builder);
		builder.show();
	}

	public static void restartApp() {
		Intent intent = Intent.makeRestartActivityTask(MastodonApp.context.getPackageManager().getLaunchIntentForPackage(MastodonApp.context.getPackageName()).getComponent());
		MastodonApp.context.startActivity(intent);
		Runtime.getRuntime().exit(0);
	}

	public static MenuItem makeBackItem(Menu m) {
		MenuItem back = m.add(0, R.id.menu_back, NONE, R.string.back);
		back.setIcon(R.drawable.ic_fluent_arrow_left_24_regular);
		return back;
	}

	public static boolean setExtraTextInfo(Context ctx, TextView extraText, StatusPrivacy visibility, boolean localOnly) {
		List<String> extraParts = new ArrayList<>();
		if (localOnly || (visibility != null && visibility.equals(StatusPrivacy.LOCAL)))
			extraParts.add(ctx.getString(R.string.sk_inline_local_only));
		if (visibility != null && visibility.equals(StatusPrivacy.DIRECT))
			extraParts.add(ctx.getString(R.string.sk_inline_direct));
		if (!extraParts.isEmpty()) {
			String sep = ctx.getString(R.string.sk_separator);
			extraText.setText(String.join(" " + sep + " ", extraParts));
			extraText.setVisibility(View.VISIBLE);
			return true;
		} else {
			extraText.setVisibility(View.GONE);
			return false;
		}
	}

	@FunctionalInterface
	public interface InteractionPerformer {
		void interact(StatusInteractionController ic, Status status, Consumer<Status> resultConsumer);
	}

	public static void pickInteractAs(Context context, String accountID, Status sourceStatus, Predicate<Status> checkInteracted, InteractionPerformer interactionPerformer, @StringRes int interactAsRes, @StringRes int interactedAsAccountRes, @StringRes int alreadyInteractedRes, @DrawableRes int iconRes) {
		pickAccount(context, accountID, interactAsRes, iconRes, session -> {
			lookupStatus(context, sourceStatus, session.getID(), accountID, status -> {
				if (status == null) return;

				if (checkInteracted.test(status)) {
					Toast.makeText(context, alreadyInteractedRes, Toast.LENGTH_SHORT).show();
					return;
				}

				StatusInteractionController ic = AccountSessionManager.getInstance().getAccount(session.getID()).getRemoteStatusInteractionController();
				interactionPerformer.interact(ic, status, s -> {
					if (checkInteracted.test(s)) {
						Toast.makeText(context, context.getString(interactedAsAccountRes, session.getFullUsername()), Toast.LENGTH_SHORT).show();
					}
				});
			});
		}, null);
	}

	public static void lookupStatus(Context context, Status queryStatus, String targetAccountID, @Nullable String sourceAccountID, Consumer<Status> resultConsumer) {
		lookup(context, queryStatus, targetAccountID, sourceAccountID, GetSearchResults.Type.STATUSES, resultConsumer, results ->
			!results.statuses.isEmpty() ? Optional.of(results.statuses.get(0)) : Optional.empty()
		);
	}

	public static void lookupAccount(Context context, Account queryAccount, String targetAccountID, @Nullable String sourceAccountID, Consumer<Account> resultConsumer) {
		lookup(context, queryAccount, targetAccountID, sourceAccountID, GetSearchResults.Type.ACCOUNTS, resultConsumer, results ->
				!results.accounts.isEmpty() ? Optional.of(results.accounts.get(0)) : Optional.empty()
		);
	}

	public static void lookupRemoteStatus(Context context, Status queryStatus, String targetAccountID, @Nullable String sourceAccountID, Consumer<Status> resultConsumer) {
		remoteLookup(context, queryStatus, targetAccountID, sourceAccountID, GetSearchResults.Type.STATUSES, resultConsumer, results ->
				!results.statuses.isEmpty() ? Optional.of(results.statuses.get(0)) : Optional.empty()
		);
	}

	public static void lookupRemoteAccount(Context context, Account queryAccount, String targetAccountID, @Nullable String sourceAccountID, Consumer<Account> resultConsumer) {
		remoteLookup(context, queryAccount, targetAccountID, sourceAccountID, GetSearchResults.Type.ACCOUNTS, resultConsumer, results ->
				!results.accounts.isEmpty() ? Optional.of(results.accounts.get(0)) : Optional.empty()
		);
	}

	public static <T extends Searchable> void lookup(Context context, T query, String targetAccountID, @Nullable String sourceAccountID, @Nullable GetSearchResults.Type type, Consumer<T> resultConsumer, Function<SearchResults, Optional<T>> extractResult) {
		if (sourceAccountID != null && targetAccountID.startsWith(sourceAccountID.substring(0, sourceAccountID.indexOf('_')))) {
			resultConsumer.accept(query);
			return;
		}

		new GetSearchResults(query.getQuery(), type, true).setCallback(new Callback<>() {
					@Override
					public void onSuccess(SearchResults results) {
						Optional<T> result = extractResult.apply(results);
						if (result.isPresent()) resultConsumer.accept(result.get());
						else {
							Toast.makeText(context, R.string.sk_resource_not_found, Toast.LENGTH_SHORT).show();
							resultConsumer.accept(null);
						}
					}

					@Override
					public void onError(ErrorResponse error) {
						error.showToast(context);
					}
				})
				.wrapProgress((Activity)context, R.string.loading, true,
						d -> transformDialogForLookup(context, targetAccountID, null, d))
				.exec(targetAccountID);
	}

	public static <T extends Searchable> void remoteLookup(Context context, T query, String targetAccountID, @Nullable String sourceAccountID, @Nullable GetSearchResults.Type type, Consumer<T> resultConsumer, Function<SearchResults, Optional<T>> extractResult) {
		if (sourceAccountID != null && targetAccountID.startsWith(sourceAccountID.substring(0, sourceAccountID.indexOf('_')))) {
			resultConsumer.accept(query);
			return;
		}

		Pattern pattern = Pattern.compile("(?<=\\/\\/)([^\\/]+)(?=\\/@)");
		Matcher matcher = pattern.matcher(query.getQuery());
		String domain = null;
		if(matcher.find()){
			domain = matcher.group(1);
		}

//		if(domain == null){
//			return;
//		}

		Pattern patternForQuery = Pattern.compile("https?:\\/\\/[^\\/]+\\/@(\\w+)");
		Matcher matcherForQuery = patternForQuery.matcher(query.getQuery());
		String trimmedQuery = null;

		if(matcherForQuery.find()){
			trimmedQuery = matcherForQuery.group(1);
		}

//		if(trimmedQuery == null){
//			return;
//		}

		if(query instanceof Account){
			domain = ((Account) query).getDomain();
			trimmedQuery = ((Account) query).username;
		}

		String finalDomain = domain;

		if(query instanceof Account){
			new GetAccountByHandle(((Account) query).acct)
					.setCallback(new Callback<Account>() {
						@Override
						public void onSuccess(Account result) {
							if(result != null){
								resultConsumer.accept((T) result);
							} else {
								Toast.makeText(context, R.string.sk_resource_not_found, Toast.LENGTH_SHORT).show();
								resultConsumer.accept(null);
							}
						}

						@Override
						public void onError(ErrorResponse error) {
							error.showToast(context);
							resultConsumer.accept(null);
						}
					})
					.wrapProgress((Activity)context, R.string.loading, true,
							d -> transformDialogForLookup(context, targetAccountID, null, d, finalDomain))
					.execNoAuth(domain);
			return;
		}
		new GetSearchResults(trimmedQuery, type, false).setCallback(new Callback<>() {
					@Override
					public void onSuccess(SearchResults results) {
						Optional<T> result = extractResult.apply(results);
						if (result.isPresent()) resultConsumer.accept(result.get());
						else {
							Toast.makeText(context, R.string.sk_resource_not_found, Toast.LENGTH_SHORT).show();
							resultConsumer.accept(null);
						}
					}

					@Override
					public void onError(ErrorResponse error) {
						error.showToast(context);
						resultConsumer.accept(null);
					}
				})
				.wrapProgress((Activity)context, R.string.loading, true,
						d -> transformDialogForLookup(context, targetAccountID, null, d, finalDomain))
				.execNoAuth(domain);
	}

	public static void openURL(Context context, String accountID, String url) {
		openURL(context, accountID, url, true);
	}

	private static void transformDialogForLookup(Context context, String accountID, @Nullable String url, ProgressDialog dialog){
		transformDialogForLookup(context, accountID, url, dialog, null);
	}

	private static void transformDialogForLookup(Context context, String accountID, @Nullable String url, ProgressDialog dialog, @Nullable String instanceName) {
		if (accountID != null) {
			dialog.setTitle(context.getString(R.string.sk_loading_resource_on_instance_title, instanceName != null ? instanceName : getInstanceName(accountID)));
		} else {
			dialog.setTitle(R.string.sk_loading_fediverse_resource_title);
		}
		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.cancel), (d, which) -> d.cancel());
		if (url != null) {
			dialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.open_in_browser), (d, which) -> {
				d.cancel();
				launchWebBrowser(context, url);
			});
		}
	}

	public static void openURL(Context context, String accountID, String url, boolean launchBrowser){
		Uri uri=Uri.parse(url);
		List<String> path=uri.getPathSegments();
		if(accountID!=null && "https".equals(uri.getScheme())){
			if(path.size()==2 && path.get(0).matches("^@[a-zA-Z0-9_]+$") && path.get(1).matches("^[0-9]+$") && AccountSessionManager.getInstance().getAccount(accountID).domain.equalsIgnoreCase(uri.getAuthority())){
				new GetStatusByID(path.get(1))
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Status result){
								Bundle args=new Bundle();
								args.putString("account", accountID);
								args.putParcelable("status", Parcels.wrap(result));
								Nav.go((Activity) context, ThreadFragment.class, args);
							}

							@Override
							public void onError(ErrorResponse error){
								error.showToast(context);
								if (launchBrowser) launchWebBrowser(context, url);
							}
						})
						.wrapProgress((Activity)context, R.string.loading, true,
								d -> transformDialogForLookup(context, accountID, url, d))
						.exec(accountID);
				return;
			} else if (looksLikeMastodonUrl(url)) {
				new GetSearchResults(url, null, true)
						.setCallback(new Callback<>() {
							@Override
							public void onSuccess(SearchResults results) {
								Bundle args=new Bundle();
								args.putString("account", accountID);
								if (!results.statuses.isEmpty()) {
									args.putParcelable("status", Parcels.wrap(results.statuses.get(0)));
									Nav.go((Activity) context, ThreadFragment.class, args);
								} else if (!results.accounts.isEmpty()) {
									args.putParcelable("profileAccount", Parcels.wrap(results.accounts.get(0)));
									Nav.go((Activity) context, ProfileFragment.class, args);
								} else {
									if (launchBrowser) launchWebBrowser(context, url);
									else Toast.makeText(context, R.string.sk_resource_not_found, Toast.LENGTH_SHORT).show();
								}
							}

							@Override
							public void onError(ErrorResponse error) {
								error.showToast(context);
								if (launchBrowser) launchWebBrowser(context, url);
							}
						})
						.wrapProgress((Activity)context, R.string.loading, true,
								d -> transformDialogForLookup(context, accountID, url, d))
						.exec(accountID);
				return;
			}
		}
		launchWebBrowser(context, url);
	}

	public static void copyText(View v, String text) {
		Context context = v.getContext();
		context.getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText(null, text));
		if(Build.VERSION.SDK_INT<Build.VERSION_CODES.TIRAMISU || UiUtils.isMIUI()){ // Android 13+ SystemUI shows its own thing when you put things into the clipboard
			Toast.makeText(context, R.string.text_copied, Toast.LENGTH_SHORT).show();
		}
		v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
	}

	private static String getSystemProperty(String key){
		try{
			Class<?> props=Class.forName("android.os.SystemProperties");
			Method get=props.getMethod("get", String.class);
			return (String)get.invoke(null, key);
		}catch(Exception ignore){}
		return null;
	}

	public static boolean isMIUI(){
		return !TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.code"));
	}

	public static boolean isEMUI() {
		return !TextUtils.isEmpty(getSystemProperty("ro.build.version.emui"));
	}

	public static boolean pickAccountForCompose(Activity activity, String accountID, String prefilledText) {
		Bundle args = new Bundle();
		if (prefilledText != null) args.putString("prefilledText", prefilledText);
		return pickAccountForCompose(activity, accountID, args);
	}

	public static boolean pickAccountForCompose(Activity activity, String accountID){
		return pickAccountForCompose(activity, accountID, (String) null);
	}

	public static boolean pickAccountForCompose(Activity activity, String accountID, Bundle args){
		if (AccountSessionManager.getInstance().getLoggedInAccounts().size() > 1) {
			UiUtils.pickAccount(activity, accountID, 0, 0, session -> {
				args.putString("account", session.getID());
				Nav.go(activity, ComposeFragment.class, args);
			}, null);
			return true;
		} else {
			return false;
		}
	}

//	public static String getVisibilityText(Status status) {
//		return MastodonApp.context.getString(switch (status.visibility) {
//			case PUBLIC -> R.string.visibility_public;
//			case UNLISTED -> R.string.sk_visibility_unlisted;
//			case PRIVATE -> R.string.visibility_followers_only;
//			case DIRECT -> R.string.visibility_private;;
//		});
//	}

	// https://github.com/tuskyapp/Tusky/pull/3148
	public static void reduceSwipeSensitivity(ViewPager2 pager) {
		try {
			Field recyclerViewField = ViewPager2.class.getDeclaredField("mRecyclerView");
			recyclerViewField.setAccessible(true);
			RecyclerView recyclerView = (RecyclerView) recyclerViewField.get(pager);
			Field touchSlopField = RecyclerView.class.getDeclaredField("mTouchSlop");
			touchSlopField.setAccessible(true);
			int touchSlop = touchSlopField.getInt(recyclerView);
			touchSlopField.set(recyclerView, touchSlop * 3);
		} catch (Exception ex) {
			Log.e("reduceSwipeSensitivity", Log.getStackTraceString(ex));
		}
	}

	public static View makeOverflowActionView(Context ctx) {
		// container needs tooltip, content description
		LinearLayout container = new LinearLayout(ctx, null, 0, R.style.Widget_Mastodon_ActionButton_Overflow) {
			@Override
			public CharSequence getAccessibilityClassName() {
				return Button.class.getName();
			}
		};
		// image needs, well, the image, and the paddings
		ImageView image = new ImageView(ctx, null, 0, R.style.Widget_Mastodon_ActionButton_Overflow);

		image.setDuplicateParentStateEnabled(true);
		image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
		image.setClickable(false);
		image.setFocusable(false);
		image.setEnabled(false);

		// problem: as per overflow action button defaults, the padding on left and right is unequal
		// so (however the native overflow button manages this), the ripple background is off-center

		// workaround: set both paddings to the smaller, left one…
		int end = image.getPaddingEnd();
		int start = image.getPaddingStart();
		int paddingDiff = end - start; // what's missing to the long padding
		image.setPaddingRelative(start, image.getPaddingTop(), start, image.getPaddingBottom());

		// …and add the missing padding to the right on the container
		container.setPaddingRelative(0, 0, paddingDiff, 0);
		container.setBackground(null);
		container.setClickable(true);
		container.setFocusable(true);

		container.addView(image);

		// fucking finally
		return container;
	}

	/**
	 * Check to see if Android platform photopicker is available on the device\
	 *
	 * @return whether the device supports photopicker intents.
	 */
	@SuppressLint("NewApi")
	public static boolean isPhotoPickerAvailable(){
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
			return true;
		}else if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
			return SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R)>=2;
		}else
			return false;
	}

	@SuppressLint("InlinedApi")
	public static Intent getMediaPickerIntent(String[] mimeTypes, int maxCount){
		Intent intent;
		if(isPhotoPickerAvailable()){
			intent=new Intent(MediaStore.ACTION_PICK_IMAGES);
			if(maxCount>1)
				intent.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, maxCount);
		}else{
			intent=new Intent(Intent.ACTION_GET_CONTENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
		}
		if(mimeTypes.length>1){
			intent.setType("*/*");
			intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
		}else if(mimeTypes.length==1){
			intent.setType(mimeTypes[0]);
		}else{
			intent.setType("*/*");
		}
		if(maxCount>1)
			intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		return intent;
	}

	public static void populateAccountsMenu(String excludeAccountID, Menu menu, Consumer<AccountSession> onClick) {
		List<AccountSession> sessions=AccountSessionManager.getInstance().getLoggedInAccounts();
		sessions.stream().filter(s -> !s.getID().equals(excludeAccountID)).forEach(s -> {
			String username = "@"+s.self.username+"@"+s.domain;
			menu.add(username).setOnMenuItemClickListener((c) -> {
				onClick.accept(s);
				return true;
			});
		});
	}

	/**
	 * Wraps a View.OnClickListener to filter multiple clicks in succession.
	 * Useful for buttons that perform some action that changes their state asynchronously.
	 * @param l
	 * @return
	 */
	public static View.OnClickListener rateLimitedClickListener(View.OnClickListener l){
		return new View.OnClickListener(){
			private long lastClickTime;

			@Override
			public void onClick(View v){
				if(SystemClock.uptimeMillis()-lastClickTime>500L){
					lastClickTime=SystemClock.uptimeMillis();
					l.onClick(v);
				}
			}
		};
	}
}
