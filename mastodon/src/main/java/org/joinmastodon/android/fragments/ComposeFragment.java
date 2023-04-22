package org.joinmastodon.android.fragments;

import static android.os.ext.SdkExtensions.getExtensionVersion;

import static org.joinmastodon.android.GlobalUserPreferences.recentLanguages;
import static org.joinmastodon.android.api.requests.statuses.CreateStatus.DRAFTS_AFTER_INSTANT;
import static org.joinmastodon.android.api.requests.statuses.CreateStatus.getDraftInstant;
import static org.joinmastodon.android.ui.utils.UiUtils.isPhotoPickerAvailable;
import static org.joinmastodon.android.utils.MastodonLanguage.allLanguages;
import static org.joinmastodon.android.utils.MastodonLanguage.defaultRecentLanguages;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.icu.text.BreakIterator;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.bottomSoftwareFoundation.bottom.Bottom;
import com.twitter.twittertext.TwitterTextEmojiRegex;

import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.MastodonErrorResponse;
import org.joinmastodon.android.api.ProgressListener;
import org.joinmastodon.android.api.requests.statuses.CreateStatus;
import org.joinmastodon.android.api.requests.statuses.DeleteStatus;
import org.joinmastodon.android.api.requests.statuses.EditStatus;
import org.joinmastodon.android.api.requests.statuses.GetAttachmentByID;
import org.joinmastodon.android.api.requests.statuses.UploadAttachment;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.ScheduledStatusCreatedEvent;
import org.joinmastodon.android.events.ScheduledStatusDeletedEvent;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.events.StatusCreatedEvent;
import org.joinmastodon.android.events.StatusUpdatedEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.EmojiCategory;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Mention;
import org.joinmastodon.android.model.Poll;
import org.joinmastodon.android.model.Preferences;
import org.joinmastodon.android.model.ScheduledStatus;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.ui.ComposeAutocompleteViewController;
import org.joinmastodon.android.ui.CustomEmojiPopupKeyboard;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.PopupKeyboard;
import org.joinmastodon.android.ui.drawables.SpoilerStripesDrawable;
import org.joinmastodon.android.ui.text.ComposeAutocompleteSpan;
import org.joinmastodon.android.ui.text.ComposeHashtagOrMentionSpan;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.utils.TransferSpeedTracker;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.ComposeEditText;
import org.joinmastodon.android.ui.views.ComposeMediaLayout;
import org.joinmastodon.android.ui.views.LinkedTextView;
import org.joinmastodon.android.ui.views.ReorderableLinearLayout;
import org.joinmastodon.android.ui.views.SizeListenerLinearLayout;
import org.joinmastodon.android.utils.MastodonLanguage;
import org.joinmastodon.android.utils.StatusTextEncoder;
import org.parceler.Parcel;
import org.parceler.Parcels;

import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.OnBackPressedListener;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class ComposeFragment extends MastodonToolbarFragment implements OnBackPressedListener, ComposeEditText.SelectionListener{

	private static final int MEDIA_RESULT=717;
	private static final int IMAGE_DESCRIPTION_RESULT=363;
	private static final int SCHEDULED_STATUS_OPENED_RESULT=161;
	private static final int MAX_ATTACHMENTS=4;
	private static final String GLITCH_LOCAL_ONLY_SUFFIX = "👁";
	private static final Pattern GLITCH_LOCAL_ONLY_PATTERN = Pattern.compile("[\\s\\S]*" + GLITCH_LOCAL_ONLY_SUFFIX + "[\uFE00-\uFE0F]*");
	private static final String TAG="ComposeFragment";

	public static final Pattern MENTION_PATTERN=Pattern.compile("(^|[^\\/\\w])@(([a-z0-9_]+)@[a-z0-9\\.\\-]+[a-z0-9]+)", Pattern.CASE_INSENSITIVE);

	// from https://github.com/mastodon/mastodon-ios/blob/main/Mastodon/Helper/MastodonRegex.swift
	public static final Pattern AUTO_COMPLETE_PATTERN=Pattern.compile("(?<!\\w)(?:@([a-zA-Z0-9_]+)(@[a-zA-Z0-9_.-]+)?|#([^\\s.]+)|:([a-zA-Z0-9_]+))");
	public static final Pattern HIGHLIGHT_PATTERN=Pattern.compile("(?<!\\w)(?:@([a-zA-Z0-9_]+)(@[a-zA-Z0-9_.-]+)?|#([^\\s.]+))");

	@SuppressLint("NewApi") // this class actually exists on 6.0
	private final BreakIterator breakIterator=BreakIterator.getCharacterInstance();

	private SizeListenerLinearLayout contentView;
	private TextView selfName, selfUsername, selfExtraText, extraText;
	private ImageView selfAvatar;
	private Account self;
	private String instanceDomain;

	private ComposeEditText mainEditText;
	private TextView charCounter;
	private String accountID;
	private int charCount, charLimit, trimmedCharCount;

	private Button publishButton, languageButton, scheduleTimeBtn, draftsBtn;
	private PopupMenu languagePopup, visibilityPopup, draftOptionsPopup;
	private ImageButton mediaBtn, pollBtn, emojiBtn, spoilerBtn, visibilityBtn, scheduleDraftDismiss;
	private ImageView sensitiveIcon;
	private ComposeMediaLayout attachmentsView;
	private TextView replyText;
	private ReorderableLinearLayout pollOptionsView;
	private View pollWrap;
	private View addPollOptionBtn;
	private View sensitiveItem;
	private View pollAllowMultipleItem;
	private View scheduleDraftView;
	private ScrollView scrollView;
	private boolean initiallyScrolled = false;
	private TextView scheduleDraftText;
	private CheckBox pollAllowMultipleCheckbox;
	private TextView pollDurationView;
	private MenuItem draftMenuItem, undraftMenuItem, scheduleMenuItem, unscheduleMenuItem;

	private ArrayList<DraftPollOption> pollOptions=new ArrayList<>();

	private ArrayList<DraftMediaAttachment> attachments=new ArrayList<>();

	private List<EmojiCategory> customEmojis;
	private CustomEmojiPopupKeyboard emojiKeyboard;
	private Status replyTo;
	private Status quote;
	private String initialText;
	private String uuid;
	private int pollDuration=24*3600;
	private String pollDurationStr;
	private EditText spoilerEdit;
	private boolean hasSpoiler;
	private boolean sensitive;
	private Instant scheduledAt = null;
	private ProgressBar sendProgress;
	private ImageView sendError;
	private View sendingOverlay;
	private WindowManager wm;
	private StatusPrivacy statusVisibility=StatusPrivacy.PUBLIC;
	private boolean localOnly;
	private ComposeAutocompleteSpan currentAutocompleteSpan;
	private FrameLayout mainEditTextWrap;
	private ComposeAutocompleteViewController autocompleteViewController;
	private Instance instance;
	private boolean attachmentsErrorShowing;

	private Status editingStatus;
	private ScheduledStatus scheduledStatus;
	private boolean redraftStatus;
	private boolean pollChanged;
	private boolean creatingView;
	private boolean ignoreSelectionChanges=false;
	private Runnable updateUploadEtaRunnable;

	private String language, encoding;
	private MastodonLanguage.LanguageResolver languageResolver;

	private int navigationBarColorBefore;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		navigationBarColorBefore = getActivity().getWindow().getNavigationBarColor();
		getActivity().getWindow().setNavigationBarColor(UiUtils.getThemeColor(getActivity(), R.attr.colorBackgroundLightest));

		accountID=getArguments().getString("account");
		AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
		self=session.self;
		instanceDomain=session.domain;
		customEmojis=AccountSessionManager.getInstance().getCustomEmojis(instanceDomain);
		instance=AccountSessionManager.getInstance().getInstanceInfo(instanceDomain);
		languageResolver=new MastodonLanguage.LanguageResolver(instance);
		redraftStatus=getArguments().getBoolean("redraftStatus", false);
		if(getArguments().containsKey("editStatus"))
			editingStatus=Parcels.unwrap(getArguments().getParcelable("editStatus"));
		if(getArguments().containsKey("replyTo"))
			replyTo=Parcels.unwrap(getArguments().getParcelable("replyTo"));
		if(getArguments().containsKey("quote"))
			quote=Parcels.unwrap(getArguments().getParcelable("quote"));
		if(instance==null){
			Nav.finish(this);
			return;
		}
		if(customEmojis.isEmpty()){
			AccountSessionManager.getInstance().updateInstanceInfo(instanceDomain);
		}

		Bundle bundle = savedInstanceState != null ? savedInstanceState : getArguments();
		if (bundle.containsKey("scheduledStatus")) scheduledStatus=Parcels.unwrap(bundle.getParcelable("scheduledStatus"));
		if (bundle.containsKey("scheduledAt")) scheduledAt=(Instant) bundle.getSerializable("scheduledAt");

		if(instance.maxTootChars>0)
			charLimit=instance.maxTootChars;
		else if(instance.configuration!=null && instance.configuration.statuses!=null && instance.configuration.statuses.maxCharacters>0)
			charLimit=instance.configuration.statuses.maxCharacters;
		else
			charLimit=500;
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		for(DraftMediaAttachment att:attachments){
			if(att.isUploadingOrProcessing())
				att.cancelUpload();
		}
		if(updateUploadEtaRunnable!=null){
			UiUtils.removeCallbacks(updateUploadEtaRunnable);
			updateUploadEtaRunnable=null;
		}
		getActivity().getWindow().setNavigationBarColor(navigationBarColorBefore);
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setHasOptionsMenu(true);
		wm=activity.getSystemService(WindowManager.class);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		creatingView=true;
		emojiKeyboard=new CustomEmojiPopupKeyboard(getActivity(), customEmojis, instanceDomain);
		emojiKeyboard.setListener(this::onCustomEmojiClick);

		View view=inflater.inflate(R.layout.fragment_compose, container, false);

		if(GlobalUserPreferences.relocatePublishButton){
			publishButton=view.findViewById(R.id.publish);
//			publishButton.setText(editingStatus==null || redraftStatus ? R.string.publish : R.string.save);
			publishButton.setEllipsize(TextUtils.TruncateAt.END);
			publishButton.setOnClickListener(this::onPublishClick);
			publishButton.setSingleLine(true);
			publishButton.setVisibility(View.VISIBLE);

			draftsBtn=view.findViewById(R.id.drafts_btn);
			draftsBtn.setVisibility(View.VISIBLE);
		} else {
			charCounter=view.findViewById(R.id.char_counter);
			charCounter.setVisibility(View.VISIBLE);
			charCounter.setText(String.valueOf(charLimit));
		}

		mainEditText=view.findViewById(R.id.toot_text);
		mainEditTextWrap=view.findViewById(R.id.toot_text_wrap);
		scrollView=view.findViewById(R.id.scroll_view);

		selfName=view.findViewById(R.id.self_name);
		selfUsername=view.findViewById(R.id.self_username);
		selfAvatar=view.findViewById(R.id.self_avatar);
		selfExtraText=view.findViewById(R.id.self_extra_text);
		HtmlParser.setTextWithCustomEmoji(selfName, self.displayName, self.emojis);
		selfUsername.setText('@'+self.username+'@'+instanceDomain);
		ViewImageLoader.load(selfAvatar, null, new UrlImageLoaderRequest(self.avatar));
		ViewOutlineProvider roundCornersOutline=new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), V.dp(12));
			}
		};
		selfAvatar.setOutlineProvider(roundCornersOutline);
		selfAvatar.setClipToOutline(true);

		mediaBtn=view.findViewById(R.id.btn_media);
		pollBtn=view.findViewById(R.id.btn_poll);
		emojiBtn=view.findViewById(R.id.btn_emoji);
		spoilerBtn=view.findViewById(R.id.btn_spoiler);
		visibilityBtn=view.findViewById(R.id.btn_visibility);
		scheduleDraftView=view.findViewById(R.id.schedule_draft_view);
		scheduleDraftText=view.findViewById(R.id.schedule_draft_text);
		scheduleDraftDismiss=view.findViewById(R.id.schedule_draft_dismiss);
		scheduleTimeBtn=view.findViewById(R.id.scheduled_time_btn);
		sensitiveIcon=view.findViewById(R.id.sensitive_icon);
		sensitiveItem=view.findViewById(R.id.sensitive_item);
		replyText=view.findViewById(GlobalUserPreferences.replyLineAboveHeader ? R.id.reply_text : R.id.reply_text_below);
		view.findViewById(GlobalUserPreferences.replyLineAboveHeader ? R.id.reply_text_below : R.id.reply_text)
				.setVisibility(View.GONE);

		if (isPhotoPickerAvailable()) {
			PopupMenu attachPopup = new PopupMenu(getContext(), mediaBtn);
			attachPopup.inflate(R.menu.attach);
			attachPopup.setOnMenuItemClickListener(i -> {
				openFilePicker(i.getItemId() == R.id.media);
				return true;
			});
			UiUtils.enablePopupMenuIcons(getContext(), attachPopup);
			mediaBtn.setOnClickListener(v->attachPopup.show());
			mediaBtn.setOnTouchListener(attachPopup.getDragToOpenListener());
		} else {
			mediaBtn.setOnClickListener(v -> openFilePicker(false));
		}
		pollBtn.setOnClickListener(v->togglePoll());
		emojiBtn.setOnClickListener(v->emojiKeyboard.toggleKeyboardPopup(mainEditText));
		spoilerBtn.setOnClickListener(v->toggleSpoiler());

		localOnly = savedInstanceState != null ? savedInstanceState.getBoolean("localOnly") :
				editingStatus != null ? editingStatus.localOnly : replyTo != null && replyTo.localOnly;

		buildVisibilityPopup(visibilityBtn);
		visibilityBtn.setOnClickListener(v->visibilityPopup.show());
		visibilityBtn.setOnTouchListener(visibilityPopup.getDragToOpenListener());

		scheduleDraftDismiss.setOnClickListener(v->updateScheduledAt(null));
		scheduleTimeBtn.setOnClickListener(v->pickScheduledDateTime());

		sensitiveItem.setOnClickListener(v->toggleSensitive());
		emojiKeyboard.setOnIconChangedListener(new PopupKeyboard.OnIconChangeListener(){
			@Override
			public void onIconChanged(int icon){
				emojiBtn.setSelected(icon!=PopupKeyboard.ICON_HIDDEN);
			}
		});

		contentView=(SizeListenerLinearLayout) view;
		contentView.addView(emojiKeyboard.getView());
		emojiKeyboard.getView().setElevation(V.dp(2));

		attachmentsView=view.findViewById(R.id.attachments);
		pollOptionsView=view.findViewById(R.id.poll_options);
		pollWrap=view.findViewById(R.id.poll_wrap);
		addPollOptionBtn=view.findViewById(R.id.add_poll_option);
		pollAllowMultipleItem=view.findViewById(R.id.poll_allow_multiple);
		pollAllowMultipleCheckbox=view.findViewById(R.id.poll_allow_multiple_checkbox);
		pollAllowMultipleItem.setOnClickListener(v->this.togglePollAllowMultiple());

		addPollOptionBtn.setOnClickListener(v->{
			createDraftPollOption().edit.requestFocus();
			updatePollOptionHints();
		});
		pollOptionsView.setDragListener(this::onSwapPollOptions);
		pollDurationView=view.findViewById(R.id.poll_duration);
		pollDurationView.setOnClickListener(v->showPollDurationMenu());

		pollOptions.clear();
		if(savedInstanceState!=null && savedInstanceState.containsKey("pollOptions")){
			pollBtn.setSelected(true);
			mediaBtn.setEnabled(false);
			pollWrap.setVisibility(View.VISIBLE);
			updatePollAllowMultiple(savedInstanceState.getBoolean("pollAllowMultiple", false));
			for(String oldText:savedInstanceState.getStringArrayList("pollOptions")){
				DraftPollOption opt=createDraftPollOption();
				opt.edit.setText(oldText);
			}
			updatePollOptionHints();
			pollDurationView.setText(getString(R.string.compose_poll_duration, pollDurationStr));
		}else if(savedInstanceState==null && editingStatus!=null && editingStatus.poll!=null){
			pollBtn.setSelected(true);
			mediaBtn.setEnabled(false);
			pollWrap.setVisibility(View.VISIBLE);
			updatePollAllowMultiple(editingStatus.poll.multiple);
			for(Poll.Option eopt:editingStatus.poll.options){
				DraftPollOption opt=createDraftPollOption();
				opt.edit.setText(eopt.title);
			}
			pollDuration=scheduledStatus == null
					? (int)editingStatus.poll.expiresAt.minus(System.currentTimeMillis(), ChronoUnit.MILLIS).getEpochSecond()
					: Integer.parseInt(scheduledStatus.params.poll.expiresIn);
			pollDurationStr=UiUtils.formatTimeLeft(getActivity(), scheduledStatus == null
					? editingStatus.poll.expiresAt
					: Instant.now().plus(pollDuration, ChronoUnit.SECONDS));
			updatePollOptionHints();
			pollDurationView.setText(getString(R.string.compose_poll_duration, pollDurationStr));
		}else{
			pollDurationView.setText(getString(R.string.compose_poll_duration, pollDurationStr=getResources().getQuantityString(R.plurals.x_days, 1, 1)));
		}

		spoilerEdit=view.findViewById(R.id.content_warning);
		LayerDrawable spoilerBg=(LayerDrawable) spoilerEdit.getBackground().mutate();
		spoilerBg.setDrawableByLayerId(R.id.left_drawable, new SpoilerStripesDrawable());
		spoilerBg.setDrawableByLayerId(R.id.right_drawable, new SpoilerStripesDrawable());
		spoilerEdit.setBackground(spoilerBg);
		if((savedInstanceState!=null && savedInstanceState.getBoolean("hasSpoiler", false)) || hasSpoiler){
			hasSpoiler=true;
			spoilerEdit.setVisibility(View.VISIBLE);
			spoilerBtn.setSelected(true);
		}else if(editingStatus!=null && !TextUtils.isEmpty(editingStatus.spoilerText)){
			hasSpoiler=true;
			spoilerEdit.setVisibility(View.VISIBLE);
			spoilerEdit.setText(getArguments().getString("sourceSpoiler", editingStatus.spoilerText));
			spoilerBtn.setSelected(true);
		}

		sensitive = savedInstanceState==null && editingStatus != null ? editingStatus.sensitive
				: savedInstanceState!=null && savedInstanceState.getBoolean("sensitive", false);
		if (sensitive) {
			sensitiveItem.setVisibility(View.VISIBLE);
			sensitiveIcon.setSelected(true);
		}

		if(savedInstanceState!=null && savedInstanceState.containsKey("attachments")){
			ArrayList<Parcelable> serializedAttachments=savedInstanceState.getParcelableArrayList("attachments");
			for(Parcelable a:serializedAttachments){
				DraftMediaAttachment att=Parcels.unwrap(a);
				attachmentsView.addView(createMediaAttachmentView(att));
				attachments.add(att);
			}
			attachmentsView.setVisibility(View.VISIBLE);
		}else if(!attachments.isEmpty()){
			attachmentsView.setVisibility(View.VISIBLE);
			for(DraftMediaAttachment att:attachments){
				attachmentsView.addView(createMediaAttachmentView(att));
			}
		}

		if(editingStatus!=null && editingStatus.visibility!=null) {
			statusVisibility=editingStatus.visibility;
		} else {
			loadDefaultStatusVisibility(savedInstanceState);
		}

		updateVisibilityIcon();
		visibilityPopup.getMenu().findItem(switch(statusVisibility){
			case PUBLIC -> R.id.vis_public;
			case UNLISTED -> R.id.vis_unlisted;
			case PRIVATE -> R.id.vis_followers;
			case DIRECT -> R.id.vis_private;
			case LOCAL -> R.id.vis_local;
		}).setChecked(true);
		visibilityPopup.getMenu().findItem(R.id.local_only).setChecked(localOnly);

		autocompleteViewController=new ComposeAutocompleteViewController(getActivity(), accountID);
		autocompleteViewController.setCompletionSelectedListener(this::onAutocompleteOptionSelected);
		View autocompleteView=autocompleteViewController.getView();
		autocompleteView.setVisibility(View.GONE);
		mainEditTextWrap.addView(autocompleteView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(178), Gravity.TOP));

		creatingView=false;

		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		if(!pollOptions.isEmpty()){
			ArrayList<String> opts=new ArrayList<>();
			for(DraftPollOption opt:pollOptions){
				opts.add(opt.edit.getText().toString());
			}
			outState.putStringArrayList("pollOptions", opts);
			outState.putInt("pollDuration", pollDuration);
			outState.putString("pollDurationStr", pollDurationStr);
			outState.putBoolean("pollAllowMultiple", pollAllowMultipleItem.isSelected());
		}
		outState.putBoolean("sensitive", sensitive);
		outState.putBoolean("localOnly", localOnly);
		outState.putBoolean("hasSpoiler", hasSpoiler);
		outState.putString("language", language);
		if(!attachments.isEmpty()){
			ArrayList<Parcelable> serializedAttachments=new ArrayList<>(attachments.size());
			for(DraftMediaAttachment att:attachments){
				serializedAttachments.add(Parcels.wrap(att));
			}
			outState.putParcelableArrayList("attachments", serializedAttachments);
		}
		outState.putSerializable("visibility", statusVisibility);
		if (scheduledAt != null) outState.putSerializable("scheduledAt", scheduledAt);
		if (scheduledStatus != null) outState.putParcelable("scheduledStatus", Parcels.wrap(scheduledStatus));
	}

	@Override
	public void onResume(){
		super.onResume();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		contentView.setSizeListener(emojiKeyboard::onContentViewSizeChanged);
		InputMethodManager imm=getActivity().getSystemService(InputMethodManager.class);
		mainEditText.requestFocus();
		view.postDelayed(()->{
			imm.showSoftInput(mainEditText, 0);
		}, 100);

		mainEditText.setSelectionListener(this);
		mainEditText.addTextChangedListener(new TextWatcher(){
			private int lastChangeStart, lastChangeCount;

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count){
				if(s.length()==0)
					return;
				lastChangeStart=start;
				lastChangeCount=count;
			}

			@Override
			public void afterTextChanged(Editable s){
				if(s.length()==0)
					return;
				int start=lastChangeStart;
				int count=lastChangeCount;
				// offset one char back to catch an already typed '@' or '#' or ':'
				int realStart=start;
				start=Math.max(0, start-1);
				CharSequence changedText=s.subSequence(start, realStart+count);
				String raw=changedText.toString();
				Editable editable=(Editable) s;
				// 1. find mentions, hashtags, and emoji shortcodes in any freshly inserted text, and put spans over them
				if(raw.contains("@") || raw.contains("#") || raw.contains(":")){
					Matcher matcher=AUTO_COMPLETE_PATTERN.matcher(changedText);
					while(matcher.find()){
						if(editable.getSpans(start+matcher.start(), start+matcher.end(), ComposeAutocompleteSpan.class).length>0)
							continue;
						ComposeAutocompleteSpan span;
						if(TextUtils.isEmpty(matcher.group(4))){ // not an emoji
							span=new ComposeHashtagOrMentionSpan();
						}else{
							span=new ComposeAutocompleteSpan();
						}
						editable.setSpan(span, start+matcher.start(), start+matcher.end(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
					}
				}
				// 2. go over existing spans in the affected range, adjust end offsets and remove no longer valid spans
				ComposeAutocompleteSpan[] spans=editable.getSpans(realStart, realStart+count, ComposeAutocompleteSpan.class);
				for(ComposeAutocompleteSpan span:spans){
					int spanStart=editable.getSpanStart(span);
					int spanEnd=editable.getSpanEnd(span);
					if(spanStart==spanEnd){ // empty, remove
						editable.removeSpan(span);
						continue;
					}
					char firstChar=editable.charAt(spanStart);
					String spanText=s.subSequence(spanStart, spanEnd).toString();
					if(firstChar=='@' || firstChar=='#' || firstChar==':'){
						Matcher matcher=AUTO_COMPLETE_PATTERN.matcher(spanText);
						char prevChar=spanStart>0 ? editable.charAt(spanStart-1) : ' ';
						if(!matcher.find() || !Character.isWhitespace(prevChar)){ // invalid mention, remove
							editable.removeSpan(span);
							continue;
						}else if(matcher.end()+spanStart<spanEnd){ // mention with something at the end, move the end offset
							editable.setSpan(span, spanStart, spanStart+matcher.end(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
						}
					}else{
						editable.removeSpan(span);
					}
				}

				updateCharCounter();
			}
		});
		spoilerEdit.addTextChangedListener(new SimpleTextWatcher(e->updateCharCounter()));
		if(replyTo!=null || quote!=null){
			Status status = quote!=null ? quote : replyTo;
			View replyWrap = view.findViewById(R.id.reply_wrap);
			scrollView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
				int scrollHeight = scrollView.getHeight();
				if (replyWrap.getMinimumHeight() != scrollHeight) {
					replyWrap.setMinimumHeight(scrollHeight);
					if (!initiallyScrolled) {
						initiallyScrolled = true;
						scrollView.post(() -> {
							int bottom = scrollView.getChildAt(0).getBottom();
							int delta = bottom - (scrollView.getScrollY() + scrollView.getHeight());
							int space = GlobalUserPreferences.reduceMotion ? 0 : Math.min(V.dp(70), delta);
							scrollView.scrollBy(0, delta - space);
							if (!GlobalUserPreferences.reduceMotion) {
								scrollView.postDelayed(() -> scrollView.smoothScrollBy(0, space), 130);
							}
						});
					}
				}
			});
			View originalPost = view.findViewById(R.id.original_post);
			extraText = view.findViewById(R.id.extra_text);
			originalPost.setVisibility(View.VISIBLE);
			originalPost.setOnClickListener(v->{
				Bundle args=new Bundle();
				args.putString("account", accountID);
				args.putParcelable("status", Parcels.wrap(status));
				imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
				Nav.go(getActivity(), ThreadFragment.class, args);
			});

			ImageView avatar = view.findViewById(R.id.avatar);
			ViewImageLoader.load(avatar, null, new UrlImageLoaderRequest(status.account.avatar));
			ViewOutlineProvider roundCornersOutline=new ViewOutlineProvider(){
				@Override
				public void getOutline(View view, Outline outline){
					outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), V.dp(12));
				}
			};
			avatar.setOutlineProvider(roundCornersOutline);
			avatar.setClipToOutline(true);
			avatar.setOnClickListener(v->{
				Bundle args=new Bundle();
				args.putString("account", accountID);
				args.putParcelable("profileAccount", Parcels.wrap(status.account));
				imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
				Nav.go(getActivity(), ProfileFragment.class, args);
			});

			((TextView) view.findViewById(R.id.name)).setText(status.account.displayName);
			((TextView) view.findViewById(R.id.username)).setText(status.account.getDisplayUsername());
			view.findViewById(R.id.visibility).setVisibility(View.GONE);
			Drawable visibilityIcon = getActivity().getDrawable(switch(status.visibility){
				case PUBLIC -> R.drawable.ic_fluent_earth_20_regular;
				case UNLISTED -> R.drawable.ic_fluent_lock_open_20_regular;
				case PRIVATE -> R.drawable.ic_fluent_lock_closed_20_filled;
				case DIRECT -> R.drawable.ic_fluent_mention_20_regular;
				case LOCAL -> R.drawable.ic_fluent_eye_20_regular;
			});
			ImageView moreBtn = view.findViewById(R.id.more);
			moreBtn.setImageDrawable(visibilityIcon);
			moreBtn.setBackground(null);
			TextView timestamp = view.findViewById(R.id.timestamp);
			if (status.editedAt!=null) timestamp.setText(getString(R.string.edited_timestamp, UiUtils.formatRelativeTimestamp(getContext(), status.editedAt)));
			else if (status.createdAt!=null) timestamp.setText(UiUtils.formatRelativeTimestamp(getContext(), status.createdAt));
			else timestamp.setText("");
			if (status.spoilerText != null && !status.spoilerText.isBlank()) {
				view.findViewById(R.id.spoiler_header).setVisibility(View.VISIBLE);
				((TextView) view.findViewById(R.id.spoiler_title_inline)).setText(status.spoilerText);
			}

			SpannableStringBuilder content = HtmlParser.parse(status.content, status.emojis, status.mentions, status.tags, accountID);
			LinkedTextView text = view.findViewById(R.id.text);
			if (content.length() > 0) text.setText(content);
			else view.findViewById(R.id.display_item_text).setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(16)));

			replyText.setText(getString(quote!=null? R.string.sk_quoting_user : R.string.in_reply_to, status.account.displayName));
			int visibilityNameRes = switch (status.visibility) {
				case PUBLIC -> R.string.visibility_public;
				case UNLISTED -> R.string.sk_visibility_unlisted;
				case PRIVATE -> R.string.visibility_followers_only;
				case DIRECT -> R.string.visibility_private;
				case LOCAL -> R.string.sk_local_only;
			};
			replyText.setContentDescription(getString(R.string.in_reply_to, status.account.displayName) + ". " + getString(R.string.post_visibility) + ": " + getString(visibilityNameRes));
			replyText.setOnClickListener(v->{
				scrollView.smoothScrollTo(0, 0);
			});
			replyText.setOnClickListener(v->{
				scrollView.smoothScrollTo(0, 0);
			});


			ArrayList<String> mentions=new ArrayList<>();
			String ownID=AccountSessionManager.getInstance().getAccount(accountID).self.id;
			if(!status.account.id.equals(ownID))
				mentions.add('@'+status.account.acct);
			if(status.rebloggedBy != null && GlobalUserPreferences.mentionRebloggerAutomatically)
				mentions.add('@'+status.rebloggedBy.acct);
			for(Mention mention:status.mentions){
				if(mention.id.equals(ownID))
					continue;
				String m='@'+mention.acct;
				if(!mentions.contains(m))
					mentions.add(m);
			}
			initialText=mentions.isEmpty() ? "" : TextUtils.join(" ", mentions)+" ";
			if(savedInstanceState==null){
				mainEditText.setText(initialText);
				ignoreSelectionChanges=true;
				mainEditText.setSelection(mainEditText.length());
				ignoreSelectionChanges=false;
				if(!TextUtils.isEmpty(status.spoilerText)){
					hasSpoiler=true;
					spoilerEdit.setVisibility(View.VISIBLE);
					if(GlobalUserPreferences.prefixRepliesWithRe && !status.spoilerText.startsWith("re: ")){
						spoilerEdit.setText("re: " + status.spoilerText);
					}else{
						spoilerEdit.setText(status.spoilerText);
					}
					spoilerBtn.setSelected(true);
				}
				if (status.language != null && !status.language.isEmpty()) updateLanguage(status.language);
			}
		}else if (editingStatus==null || editingStatus.inReplyToId==null){
			// TODO: remove workaround after https://github.com/mastodon/mastodon-android/issues/341 gets fixed
			replyText.setVisibility(View.GONE);
		}
		if(savedInstanceState==null){
			if(editingStatus!=null){
				initialText=getArguments().getString("sourceText", "");
				mainEditText.setText(initialText);
				ignoreSelectionChanges=true;
				mainEditText.setSelection(mainEditText.length());
				ignoreSelectionChanges=false;
				updateLanguage(editingStatus.language);
				if(!editingStatus.mediaAttachments.isEmpty()){
					attachmentsView.setVisibility(View.VISIBLE);
					for(Attachment att:editingStatus.mediaAttachments){
						DraftMediaAttachment da=new DraftMediaAttachment();
						da.serverAttachment=att;
						da.description=att.description;
						da.uri=att.previewUrl!=null ? Uri.parse(att.previewUrl) : null;
						da.state=AttachmentUploadState.DONE;
						attachmentsView.addView(createMediaAttachmentView(da));
						attachments.add(da);
					}
					pollBtn.setEnabled(false);
				}
			}else{
				String prefilledText=getArguments().getString("prefilledText");
				if(!TextUtils.isEmpty(prefilledText)){
					mainEditText.setText(prefilledText);
					ignoreSelectionChanges=true;
					mainEditText.setSelection(mainEditText.length());
					ignoreSelectionChanges=false;
					initialText=prefilledText;
				}
				if (getArguments().containsKey("selectionStart") || getArguments().containsKey("selectionEnd")) {
					int selectionStart=getArguments().getInt("selectionStart", 0);
					int selectionEnd=getArguments().getInt("selectionEnd", selectionStart);
					mainEditText.setSelection(selectionStart, selectionEnd);
				}
				ArrayList<Uri> mediaUris=getArguments().getParcelableArrayList("mediaAttachments");
				if(mediaUris!=null && !mediaUris.isEmpty()){
					for(Uri uri:mediaUris){
						addMediaAttachment(uri, null);
					}
				}
			}
		}

		updateSensitive();
		updateHeaders();

		if(editingStatus!=null){
			updateCharCounter();
			visibilityBtn.setEnabled(redraftStatus);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		MenuItem item=menu.add(editingStatus==null ? R.string.publish : R.string.save);
		LinearLayout wrap=new LinearLayout(getActivity());
		getActivity().getLayoutInflater().inflate(R.layout.compose_action, wrap);
		item.setActionView(wrap);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		if(!GlobalUserPreferences.relocatePublishButton){
			publishButton = wrap.findViewById(R.id.publish_btn);
			publishButton.setOnClickListener(this::onPublishClick);
			publishButton.setVisibility(View.VISIBLE);

			draftsBtn = wrap.findViewById(R.id.drafts_btn);
			draftsBtn.setVisibility(View.VISIBLE);
		}else{
			charCounter = wrap.findViewById(R.id.char_counter);
			charCounter.setVisibility(View.VISIBLE);
			charCounter.setText(String.valueOf(charLimit));
		}

//		draftsBtn = wrap.findViewById(R.id.drafts_btn);
		draftOptionsPopup = new PopupMenu(getContext(), draftsBtn);
		draftOptionsPopup.inflate(R.menu.compose_more);
		draftMenuItem = draftOptionsPopup.getMenu().findItem(R.id.draft);
		undraftMenuItem = draftOptionsPopup.getMenu().findItem(R.id.undraft);
		scheduleMenuItem = draftOptionsPopup.getMenu().findItem(R.id.schedule);
		unscheduleMenuItem = draftOptionsPopup.getMenu().findItem(R.id.unschedule);
		draftOptionsPopup.setOnMenuItemClickListener(i->{
			int id = i.getItemId();
			if (id == R.id.draft) updateScheduledAt(getDraftInstant());
			else if (id == R.id.schedule) pickScheduledDateTime();
			else if (id == R.id.unschedule || id == R.id.undraft) updateScheduledAt(null);
			else navigateToUnsentPosts();
			return true;
		});
		UiUtils.enablePopupMenuIcons(getContext(), draftOptionsPopup);


		languageButton = wrap.findViewById(R.id.language_btn);
		sendProgress = wrap.findViewById(R.id.send_progress);
		sendError = wrap.findViewById(R.id.send_error);

		draftsBtn.setOnClickListener(v-> draftOptionsPopup.show());
		draftsBtn.setOnTouchListener(draftOptionsPopup.getDragToOpenListener());
		updateScheduledAt(scheduledAt != null ? scheduledAt : scheduledStatus != null ? scheduledStatus.scheduledAt : null);
		buildLanguageSelector(languageButton);

		if (editingStatus != null && scheduledStatus == null) {
			// editing an already published post
			draftsBtn.setVisibility(View.GONE);
		}
	}

	private void navigateToUnsentPosts() {
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putBoolean("hide_fab", true);
		InputMethodManager imm=getActivity().getSystemService(InputMethodManager.class);
		imm.hideSoftInputFromWindow(draftsBtn.getWindowToken(), 0);
		if (hasDraft()) {
			Nav.go(getActivity(), ScheduledStatusListFragment.class, args);
		} else {
			// result for the previous ScheduledStatusList
			setResult(true, null);
			// finishing fragment in "onFragmentResult"
			Nav.goForResult(getActivity(), ScheduledStatusListFragment.class, args, SCHEDULED_STATUS_OPENED_RESULT, this);
		}
	}

	private void updateLanguage(String lang) {
		updateLanguage(lang == null ? languageResolver.getDefault() : languageResolver.from(lang));
	}

	private void updateLanguage(MastodonLanguage loc) {
		updateLanguage(loc.getLanguage(), loc.getLanguageName(), loc.getDefaultName());
	}

	private void updateLanguage(String languageTag, String languageName, String defaultName) {
		language = languageTag;
		languageButton.setText(languageName);
		languageButton.setContentDescription(getActivity().getString(R.string.sk_post_language, defaultName));
	}

	@SuppressLint("ClickableViewAccessibility")
	private void buildLanguageSelector(Button btn) {
		languagePopup=new PopupMenu(getActivity(), languageButton);
		btn.setOnTouchListener(languagePopup.getDragToOpenListener());
		btn.setOnClickListener(v->languagePopup.show());

		Preferences prefs = AccountSessionManager.getInstance().getAccount(accountID).preferences;
		if (language != null) updateLanguage(language);
		else updateLanguage(prefs != null && prefs.postingDefaultLanguage != null && prefs.postingDefaultLanguage.length() > 0
				? languageResolver.from(prefs.postingDefaultLanguage)
				: languageResolver.getDefault());

		Menu languageMenu = languagePopup.getMenu();
		for (String recentLanguage : Optional.ofNullable(recentLanguages.get(accountID)).orElse(defaultRecentLanguages)) {
			if (recentLanguage.equals("bottom")) {
				addBottomLanguage(languageMenu);
			} else {
				MastodonLanguage l = languageResolver.from(recentLanguage);
				languageMenu.add(0, allLanguages.indexOf(l), Menu.NONE, getActivity().getString(R.string.sk_language_name, l.getDefaultName(), l.getLanguageName()));
			}
		}

		SubMenu allLanguagesMenu = languageMenu.addSubMenu(R.string.sk_available_languages);
		for (int i = 0; i < allLanguages.size(); i++) {
			MastodonLanguage l = allLanguages.get(i);
			allLanguagesMenu.add(0, i, Menu.NONE, getActivity().getString(R.string.sk_language_name, l.getDefaultName(), l.getLanguageName()));
		}

		if (GlobalUserPreferences.bottomEncoding) addBottomLanguage(allLanguagesMenu);

		btn.setOnLongClickListener(v->{
			btn.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
			if (!GlobalUserPreferences.bottomEncoding) addBottomLanguage(allLanguagesMenu);
			return false;
		});

		languagePopup.setOnMenuItemClickListener(i->{
			if (i.hasSubMenu()) return false;
			if (i.getItemId() == allLanguages.size()) {
				updateLanguage(language, "\uD83E\uDD7A\uD83D\uDC49\uD83D\uDC48", "bottom");
				encoding = "bottom";
			} else {
				updateLanguage(allLanguages.get(i.getItemId()));
				encoding = null;
			}
			return true;
		});
	}

	private void addBottomLanguage(Menu menu) {
		if (menu.findItem(allLanguages.size()) == null) {
			menu.add(0, allLanguages.size(), Menu.NONE, "bottom (\uD83E\uDD7A\uD83D\uDC49\uD83D\uDC48)");
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		emojiKeyboard.onConfigurationChanged();
	}

	@SuppressLint("NewApi")
	private void updateCharCounter(){
		CharSequence text=mainEditText.getText();

		String countableText=TwitterTextEmojiRegex.VALID_EMOJI_PATTERN.matcher(
				MENTION_PATTERN.matcher(
						HtmlParser.URL_PATTERN.matcher(text).replaceAll("$2xxxxxxxxxxxxxxxxxxxxxxx")
				).replaceAll("$1@$3")
		).replaceAll("x");
		charCount=0;
		breakIterator.setText(countableText);
		while(breakIterator.next()!=BreakIterator.DONE){
			charCount++;
		}

		if(hasSpoiler){
			charCount+=spoilerEdit.length();
		}
		if (localOnly && GlobalUserPreferences.accountsInGlitchMode.contains(accountID)) {
			charCount -= GLITCH_LOCAL_ONLY_SUFFIX.length();
		}
		charCounter.setText(String.valueOf(charLimit-charCount));
		trimmedCharCount=text.toString().trim().length();
		updatePublishButtonState();
	}

	private void resetPublishButtonText() {
		int publishText = editingStatus==null || redraftStatus ? R.string.publish : R.string.save;
		if(GlobalUserPreferences.relocatePublishButton){
			return;
		}
		if (publishText == R.string.publish && !GlobalUserPreferences.publishButtonText.isEmpty()) {
			publishButton.setText(GlobalUserPreferences.publishButtonText);
		} else {
			publishButton.setText(publishText);
		}
	}

	private void updatePublishButtonState(){
		uuid=null;
		int nonEmptyPollOptionsCount=0;
		for(DraftPollOption opt:pollOptions){
			if(opt.edit.length()>0)
				nonEmptyPollOptionsCount++;
		}
		if(publishButton==null)
			return;
		int nonDoneAttachmentCount=0;
		for(DraftMediaAttachment att:attachments){
			if(att.state!=AttachmentUploadState.DONE)
				nonDoneAttachmentCount++;
		}
		publishButton.setEnabled((trimmedCharCount>0 || !attachments.isEmpty()) && charCount<=charLimit && nonDoneAttachmentCount==0 && (pollOptions.isEmpty() || nonEmptyPollOptionsCount>1));
//		sendError.setVisibility(View.GONE);
	}

	private void onCustomEmojiClick(Emoji emoji){
		if(getActivity().getCurrentFocus() instanceof EditText edit){
			int start=edit.getSelectionStart();
			String prefix=start>0 && !Character.isWhitespace(edit.getText().charAt(start-1)) ? " :" : ":";
			edit.getText().replace(start, edit.getSelectionEnd(), prefix+emoji.shortcode+':');
		}
	}

	@Override
	protected void updateToolbar(){
		super.updateToolbar();
		getToolbar().setNavigationIcon(R.drawable.ic_fluent_dismiss_24_regular);
	}

	private void onPublishClick(View v){
		if (!attachments.isEmpty()
				&& statusVisibility != StatusPrivacy.DIRECT
				&& !attachments.stream().allMatch(attachment -> attachment.description != null && !attachment.description.isBlank())) {
			new M3AlertDialogBuilder(getActivity())
					.setTitle(R.string.mo_no_image_desc_title)
					.setMessage(R.string.mo_no_image_desc)
					.setNegativeButton(R.string.cancel, null)
					.setPositiveButton(R.string.publish, (dialog, i)-> publish())
					.show();
		} else {
			publish();
		}
	}

	private void publishErrorCallback(ErrorResponse error) {
		wm.removeView(sendingOverlay);
		sendingOverlay=null;
		sendProgress.setVisibility(View.GONE);
		sendError.setVisibility(View.VISIBLE);
		publishButton.setEnabled(true);
		if (error != null) error.showToast(getActivity());
	}

	private void createScheduledStatusFinish(ScheduledStatus result) {
		wm.removeView(sendingOverlay);
		sendingOverlay=null;
		Toast.makeText(getContext(), scheduledAt.isAfter(DRAFTS_AFTER_INSTANT) ?
				R.string.sk_draft_saved : R.string.sk_post_scheduled, Toast.LENGTH_SHORT).show();
		Nav.finish(ComposeFragment.this);
		E.post(new ScheduledStatusCreatedEvent(result, accountID));
	}

	private void maybeDeleteScheduledPost(Runnable callback) {
		if (scheduledStatus != null) {
			new DeleteStatus.Scheduled(scheduledStatus.id).setCallback(new Callback<>() {
				@Override
				public void onSuccess(Object o) {
					E.post(new ScheduledStatusDeletedEvent(scheduledStatus.id, accountID));
					callback.run();
				}

				@Override
				public void onError(ErrorResponse error) {
					publishErrorCallback(error);
				}
			}).exec(accountID);
		} else {
			callback.run();
		}
	}

	private void publish(){
		publish(false);
	}

	private void publish(boolean force){
		String text=mainEditText.getText().toString();
		CreateStatus.Request req=new CreateStatus.Request();
		if ("bottom".equals(encoding)) {
			text = new StatusTextEncoder(Bottom::encode).encode(text);
			req.spoilerText = "bottom-encoded emoji spam";
		}
		if (localOnly &&
				GlobalUserPreferences.accountsInGlitchMode.contains(accountID) &&
				!GLITCH_LOCAL_ONLY_PATTERN.matcher(text).matches()) {
			text += " " + GLITCH_LOCAL_ONLY_SUFFIX;
		}
		req.status=text;
		req.localOnly=localOnly;
		req.visibility=localOnly && instance.pleroma != null ? StatusPrivacy.LOCAL : statusVisibility;
		req.sensitive=sensitive;
		req.language=language;
		req.scheduledAt = scheduledAt;
		if(!attachments.isEmpty()){
			req.mediaIds=attachments.stream().map(a->a.serverAttachment.id).collect(Collectors.toList());
			Optional<DraftMediaAttachment> withoutAltText = attachments.stream().filter(a -> a.description == null || a.description.isBlank()).findFirst();
			boolean isDraft = scheduledAt != null && scheduledAt.isAfter(DRAFTS_AFTER_INSTANT);
			if (!force && !GlobalUserPreferences.disableAltTextReminder && !isDraft && withoutAltText.isPresent()) {
				new M3AlertDialogBuilder(getActivity())
						.setTitle(R.string.sk_alt_text_missing_title)
						.setMessage(R.string.sk_alt_text_missing)
						.setPositiveButton(R.string.add_alt_text, (d, w) -> editMediaDescription(withoutAltText.get()))
						.setNegativeButton(R.string.sk_publish_anyway, (d, w) -> publish(true))
						.show();
				return;
			}
		}
		// ask whether to publish now when editing an existing draft
		if (!force && editingStatus != null && scheduledAt != null && scheduledAt.isAfter(DRAFTS_AFTER_INSTANT)) {
			new M3AlertDialogBuilder(getActivity())
					.setTitle(R.string.sk_save_draft)
					.setMessage(R.string.sk_save_draft_message)
					.setPositiveButton(R.string.save, (d, w) -> publish(true))
					.setNegativeButton(R.string.publish, (d, w) -> {
						updateScheduledAt(null);
						publish();
					})
					.show();
			return;
		}
		if(replyTo!=null || (editingStatus != null && editingStatus.inReplyToId!=null)){
			req.inReplyToId=editingStatus!=null ? editingStatus.inReplyToId : replyTo.id;
		}
		if(!pollOptions.isEmpty()){
			req.poll=new CreateStatus.Request.Poll();
			req.poll.expiresIn=pollDuration;
			req.poll.multiple=pollAllowMultipleItem.isSelected();
			for(DraftPollOption opt:pollOptions)
				req.poll.options.add(opt.edit.getText().toString());
		}
		if(hasSpoiler && spoilerEdit.length()>0){
			req.spoilerText=spoilerEdit.getText().toString();
		}
		if(quote != null){
			req.quoteId=quote.id;
		}
		if(uuid==null)
			uuid=UUID.randomUUID().toString();

		sendingOverlay=new View(getActivity());
		WindowManager.LayoutParams overlayParams=new WindowManager.LayoutParams();
		overlayParams.type=WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
		overlayParams.flags=WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
		overlayParams.width=overlayParams.height=WindowManager.LayoutParams.MATCH_PARENT;
		overlayParams.format=PixelFormat.TRANSLUCENT;
		overlayParams.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED;
		overlayParams.token=mainEditText.getWindowToken();
		wm.addView(sendingOverlay, overlayParams);

		publishButton.setEnabled(false);
		sendProgress.setVisibility(View.VISIBLE);
		sendError.setVisibility(View.GONE);

		Callback<Status> resCallback=new Callback<>(){
			@Override
			public void onSuccess(Status result){
				maybeDeleteScheduledPost(() -> {
					wm.removeView(sendingOverlay);
					sendingOverlay=null;
					if(editingStatus==null){
						E.post(new StatusCreatedEvent(result, accountID));
						if(replyTo!=null){
							replyTo.repliesCount++;
							E.post(new StatusCountersUpdatedEvent(replyTo));
						}
					}else{
						E.post(new StatusUpdatedEvent(result));
					}
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !isStateSaved()) {
						Nav.finish(ComposeFragment.this);
					}
					if (getArguments().getBoolean("navigateToStatus", false)) {
						Bundle args=new Bundle();
						args.putString("account", accountID);
						args.putParcelable("status", Parcels.wrap(result));
						if(replyTo!=null) args.putParcelable("inReplyToAccount", Parcels.wrap(replyTo));
						Nav.go(getActivity(), ThreadFragment.class, args);
					}
				});
			}

			@Override
			public void onError(ErrorResponse error){
				publishErrorCallback(error);
			}
		};


		if(editingStatus!=null && !redraftStatus){
			new EditStatus(req, editingStatus.id)
					.setCallback(resCallback)
					.exec(accountID);
		}else if(req.scheduledAt == null){
			new CreateStatus(req, uuid)
					.setCallback(resCallback)
					.exec(accountID);
		}else if(req.scheduledAt.isAfter(Instant.now().plus(10, ChronoUnit.MINUTES))){
			// checking for 10 instead of 5 minutes (as per mastodon) because i really don't want
			// bugs to occur because the client's clock is wrong by a minute or two - the api
			// returns a status instead of a scheduled status if scheduled time is less than 5
			// minutes into the future and this is 1. unexpected for the user and 2. hard to handle
			new CreateStatus.Scheduled(req, uuid)
					.setCallback(new Callback<>() {
						@Override
						public void onSuccess(ScheduledStatus result) {
							maybeDeleteScheduledPost(() -> {
								createScheduledStatusFinish(result);
							});
						}

						@Override
						public void onError(ErrorResponse error) {
							publishErrorCallback(error);
						}
					}).exec(accountID);
		}else{
			new M3AlertDialogBuilder(getActivity())
					.setTitle(R.string.sk_scheduled_too_soon_title)
					.setMessage(R.string.sk_scheduled_too_soon)
					.setPositiveButton(R.string.ok, (a, b)->{})
					.show();
			publishErrorCallback(null);
			publishButton.setEnabled(false);
		}

		if (replyTo == null) {
			List<String> newRecentLanguages = new ArrayList<>(Optional.ofNullable(recentLanguages.get(accountID)).orElse(defaultRecentLanguages));
			newRecentLanguages.remove(language);
			newRecentLanguages.add(0, language);
			if (encoding != null) {
				newRecentLanguages.remove(encoding);
				newRecentLanguages.add(0, encoding);
			}
			if ("bottom".equals(encoding) && !GlobalUserPreferences.bottomEncoding) {
				GlobalUserPreferences.bottomEncoding = true;
				GlobalUserPreferences.save();
			}
			recentLanguages.put(accountID, newRecentLanguages.stream().limit(4).collect(Collectors.toList()));
			GlobalUserPreferences.save();
		}
	}

	private boolean hasDraft(){
		if(getArguments().getBoolean("hasDraft", false)) return true;
		if(editingStatus!=null){
			if(!mainEditText.getText().toString().equals(initialText))
				return true;
			List<String> existingMediaIDs=editingStatus.mediaAttachments.stream().map(a->a.id).collect(Collectors.toList());
			if(!existingMediaIDs.equals(attachments.stream().map(a->a.serverAttachment.id).collect(Collectors.toList())))
				return true;
			if(!statusVisibility.equals(editingStatus.visibility)) return true;
			if(scheduledStatus != null && !scheduledStatus.scheduledAt.equals(scheduledAt)) return true;
			return pollChanged;
		}
		boolean pollFieldsHaveContent=false;
		for(DraftPollOption opt:pollOptions)
			pollFieldsHaveContent|=opt.edit.length()>0;
		return (mainEditText.length()>0 && !mainEditText.getText().toString().equals(initialText)) || !attachments.isEmpty() || pollFieldsHaveContent;
	}

	@Override
	public boolean onBackPressed(){
		if(emojiKeyboard.isVisible()){
			emojiKeyboard.hide();
			return true;
		}
		if(hasDraft()){
			confirmDiscardDraftAndFinish();
			return true;
		}
		if(sendingOverlay!=null)
			return true;
		return false;
	}

	@Override
	public void onToolbarNavigationClick(){
		if(hasDraft()){
			confirmDiscardDraftAndFinish();
		}else{
			super.onToolbarNavigationClick();
		}
	}

	@Override
	public void onFragmentResult(int reqCode, boolean success, Bundle result){
		if(reqCode==IMAGE_DESCRIPTION_RESULT && success){
			Attachment updated=Parcels.unwrap(result.getParcelable("attachment"));
			for(DraftMediaAttachment att:attachments){
				if(att.serverAttachment.id.equals(updated.id)){
					att.serverAttachment=updated;
					att.description=updated.description;
					att.descriptionView.setText(att.description);
					break;
				}
			}
		} else if (reqCode == SCHEDULED_STATUS_OPENED_RESULT && success && getActivity() != null) {
			Nav.finish(this);
		}
	}

	private void confirmDiscardDraftAndFinish(){
		boolean attachmentsPending = attachments.stream().anyMatch(att -> att.state != AttachmentUploadState.DONE);
		if (attachmentsPending) new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.sk_unfinished_attachments)
				.setMessage(R.string.sk_unfinished_attachments_message)
				.setPositiveButton(R.string.edit, (d, w) -> {})
				.setNegativeButton(R.string.discard, (d, w) -> Nav.finish(this))
				.show();
		else new M3AlertDialogBuilder(getActivity())
				.setTitle(editingStatus != null ? R.string.sk_confirm_save_changes : R.string.sk_confirm_save_draft)
				.setPositiveButton(R.string.save, (d, w) -> {
					updateScheduledAt(scheduledAt == null ? getDraftInstant() : scheduledAt);
					publish();
				})
				.setNegativeButton(R.string.discard, (d, w) -> Nav.finish(this))
				.show();
	}


	/**
	 * Builds the correct intent for the device version to select media.
	 *
	 * <p>For Device version > T or R_SDK_v2, use the android platform photopicker via
	 * {@link MediaStore#ACTION_PICK_IMAGES}
	 *
	 * <p>For earlier versions use the built in docs ui via {@link Intent#ACTION_GET_CONTENT}
	 */
	private void openFilePicker(boolean photoPicker){
		Intent intent;
		boolean usePhotoPicker=photoPicker && isPhotoPickerAvailable();
		if(usePhotoPicker){
			intent=new Intent(MediaStore.ACTION_PICK_IMAGES);
			intent.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, MAX_ATTACHMENTS-getMediaAttachmentsCount());
		}else{
			intent=new Intent(Intent.ACTION_GET_CONTENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setType("*/*");
		}
		if(!usePhotoPicker && instance.configuration!=null &&
				instance.configuration.mediaAttachments!=null &&
				instance.configuration.mediaAttachments.supportedMimeTypes!=null &&
				!instance.configuration.mediaAttachments.supportedMimeTypes.isEmpty()){
			intent.putExtra(Intent.EXTRA_MIME_TYPES,
					instance.configuration.mediaAttachments.supportedMimeTypes.toArray(
							new String[0]));
		}else{
			if(!usePhotoPicker){
				// If photo picker is being used these are the default mimetypes.
				intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
			}
		}
		intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		startActivityForResult(intent, MEDIA_RESULT);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode==MEDIA_RESULT && resultCode==Activity.RESULT_OK){
			Uri single=data.getData();
			if(single!=null){
				addMediaAttachment(single, null);
			}else{
				ClipData clipData=data.getClipData();
				for(int i=0;i<clipData.getItemCount();i++){
					addMediaAttachment(clipData.getItemAt(i).getUri(), null);
				}
			}
		}
	}

	@SuppressLint("StringFormatInvalid")
	private boolean addMediaAttachment(Uri uri, String description){
		if(getMediaAttachmentsCount()==MAX_ATTACHMENTS){
			showMediaAttachmentError(getResources().getQuantityString(R.plurals.cant_add_more_than_x_attachments, MAX_ATTACHMENTS, MAX_ATTACHMENTS));
			return false;
		}
		String type=getActivity().getContentResolver().getType(uri);
		if(instance!=null && instance.configuration!=null && instance.configuration.mediaAttachments!=null){
			if(instance.configuration.mediaAttachments.supportedMimeTypes!=null && !instance.configuration.mediaAttachments.supportedMimeTypes.contains(type)){
				showMediaAttachmentError(getString(R.string.media_attachment_unsupported_type, UiUtils.getFileName(uri)));
				return false;
			}
			if(!type.startsWith("image/")){
				int sizeLimit=instance.configuration.mediaAttachments.videoSizeLimit;
				int size;
				try(Cursor cursor=MastodonApp.context.getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null)){
					cursor.moveToFirst();
					size=cursor.getInt(0);
				}catch(Exception x){
					Log.w("ComposeFragment", x);
					return false;
				}
				if(size>sizeLimit){
					float mb=sizeLimit/(float) (1024*1024);
					String sMb=String.format(Locale.getDefault(), mb%1f==0f ? "%.0f" : "%.2f", mb);
					showMediaAttachmentError(getString(R.string.media_attachment_too_big, UiUtils.getFileName(uri), sMb));
					return false;
				}
			}
		}
		pollBtn.setEnabled(false);
		DraftMediaAttachment draft=new DraftMediaAttachment();
		draft.uri=uri;
		draft.mimeType=type;
		draft.description=description;

		attachmentsView.addView(createMediaAttachmentView(draft));
		attachments.add(draft);
		attachmentsView.setVisibility(View.VISIBLE);
		draft.setOverlayVisible(true, false);

		if(!areThereAnyUploadingAttachments()){
			uploadNextQueuedAttachment();
		}
		updatePublishButtonState();
		updateSensitive();
		if(getMediaAttachmentsCount()==MAX_ATTACHMENTS)
			mediaBtn.setEnabled(false);
		return true;
	}

	private void showMediaAttachmentError(String text){
		if(!attachmentsErrorShowing){
			Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
			attachmentsErrorShowing=true;
			contentView.postDelayed(()->attachmentsErrorShowing=false, 2000);
		}
	}

	private View createMediaAttachmentView(DraftMediaAttachment draft){
		View thumb=getActivity().getLayoutInflater().inflate(R.layout.compose_media_thumb, attachmentsView, false);
		ImageView img=thumb.findViewById(R.id.thumb);
		if(draft.serverAttachment!=null){
			if(draft.serverAttachment.previewUrl!=null)
				ViewImageLoader.load(img, draft.serverAttachment.blurhashPlaceholder, new UrlImageLoaderRequest(draft.serverAttachment.previewUrl, V.dp(250), V.dp(250)));
		}else{
			if(draft.mimeType.startsWith("image/")){
				ViewImageLoader.load(img, null, new UrlImageLoaderRequest(draft.uri, V.dp(250), V.dp(250)));
			}else if(draft.mimeType.startsWith("video/")){
				loadVideoThumbIntoView(img, draft.uri);
			}
		}
		TextView fileName=thumb.findViewById(R.id.file_name);
		fileName.setText(UiUtils.getFileName(draft.serverAttachment!=null ? Uri.parse(draft.serverAttachment.url) : draft.uri));

		draft.view=thumb;
		draft.imageView=img;
		draft.progressBar=thumb.findViewById(R.id.progress);
		draft.infoBar=thumb.findViewById(R.id.info_bar);
		draft.overlay=thumb.findViewById(R.id.overlay);
		draft.descriptionView=thumb.findViewById(R.id.description);
		draft.uploadStateTitle=thumb.findViewById(R.id.state_title);
		draft.uploadStateText=thumb.findViewById(R.id.state_text);
		ImageButton btn=thumb.findViewById(R.id.remove_btn);
		btn.setTag(draft);
		btn.setOnClickListener(this::onRemoveMediaAttachmentClick);
		btn=thumb.findViewById(R.id.remove_btn2);
		btn.setTag(draft);
		btn.setOnClickListener(this::onRemoveMediaAttachmentClick);
		ImageButton retry=thumb.findViewById(R.id.retry_or_cancel_upload);
		retry.setTag(draft);
		retry.setOnClickListener(this::onRetryOrCancelMediaUploadClick);
		draft.retryButton=retry;
		draft.infoBar.setTag(draft);
		draft.infoBar.setOnClickListener(this::onEditMediaDescriptionClick);

		if(!TextUtils.isEmpty(draft.description))
			draft.descriptionView.setText(draft.description);

		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
			draft.overlay.setBackgroundColor(0xA6000000);
		}

		if(draft.state==AttachmentUploadState.UPLOADING || draft.state==AttachmentUploadState.PROCESSING || draft.state==AttachmentUploadState.QUEUED){
			draft.progressBar.setVisibility(View.GONE);
		}else if(draft.state==AttachmentUploadState.ERROR){
			draft.setOverlayVisible(true, false);
		}

		return thumb;
	}

	public void addFakeMediaAttachment(Uri uri, String description){
		pollBtn.setEnabled(false);
		DraftMediaAttachment draft=new DraftMediaAttachment();
		draft.uri=uri;
		draft.description=description;
		attachmentsView.addView(createMediaAttachmentView(draft));
		attachments.add(draft);
		attachmentsView.setVisibility(View.VISIBLE);
	}

	private void uploadMediaAttachment(DraftMediaAttachment attachment){
		if(areThereAnyUploadingAttachments()){
			throw new IllegalStateException("there is already an attachment being uploaded");
		}
		attachment.state=AttachmentUploadState.UPLOADING;
		attachment.progressBar.setVisibility(View.VISIBLE);
		ObjectAnimator rotationAnimator=ObjectAnimator.ofFloat(attachment.progressBar, View.ROTATION, 0f, 360f);
		rotationAnimator.setInterpolator(new LinearInterpolator());
		rotationAnimator.setDuration(1500);
		rotationAnimator.setRepeatCount(ObjectAnimator.INFINITE);
		rotationAnimator.start();
		attachment.progressBarAnimator=rotationAnimator;
		int maxSize=0;
		String contentType=getActivity().getContentResolver().getType(attachment.uri);
		if(contentType!=null && contentType.startsWith("image/")){
			maxSize=2_073_600; // TODO get this from instance configuration when it gets added there
		}
		attachment.uploadStateTitle.setText("");
		attachment.uploadStateText.setText("");
		attachment.progressBar.setProgress(0);
		attachment.speedTracker.reset();
		attachment.speedTracker.addSample(0);
		attachment.uploadRequest=(UploadAttachment) new UploadAttachment(attachment.uri, maxSize, attachment.description)
				.setProgressListener(new ProgressListener(){
					@Override
					public void onProgress(long transferred, long total){
						if(updateUploadEtaRunnable==null){
							// getting a NoSuchMethodError: No static method -$$Nest$mupdateUploadETAs(ComposeFragment;)V in class ComposeFragment
							// when using method reference out of nowhere after changing code elsewhere. no idea. programming is awful, actually
							// noinspection Convert2MethodRef
							UiUtils.runOnUiThread(updateUploadEtaRunnable=()->ComposeFragment.this.updateUploadETAs(), 50);
						}
						int progress=Math.round(transferred/(float)total*attachment.progressBar.getMax());
						if(Build.VERSION.SDK_INT>=24)
							attachment.progressBar.setProgress(progress, true);
						else
							attachment.progressBar.setProgress(progress);

						attachment.speedTracker.setTotalBytes(total);
						attachment.uploadStateTitle.setText(getString(R.string.file_upload_progress, UiUtils.formatFileSize(getActivity(), transferred, true), UiUtils.formatFileSize(getActivity(), total, true)));
						attachment.speedTracker.addSample(transferred);
					}
				})
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Attachment result){
						attachment.serverAttachment=result;
						if(TextUtils.isEmpty(result.url)){
							attachment.state=AttachmentUploadState.PROCESSING;
							attachment.processingPollingRunnable=()->pollForMediaAttachmentProcessing(attachment);
							if(getActivity()==null)
								return;
							attachment.uploadStateTitle.setText(R.string.upload_processing);
							attachment.uploadStateText.setText("");
							UiUtils.runOnUiThread(attachment.processingPollingRunnable, 1000);
							if(!areThereAnyUploadingAttachments())
								uploadNextQueuedAttachment();
						}else{
							finishMediaAttachmentUpload(attachment);
						}
					}

					@Override
					public void onError(ErrorResponse error){
						attachment.uploadRequest=null;
						attachment.progressBarAnimator=null;
						attachment.state=AttachmentUploadState.ERROR;
						attachment.uploadStateTitle.setText(R.string.upload_failed);
						if(error instanceof MastodonErrorResponse er){
							if(er.underlyingException instanceof SocketException || er.underlyingException instanceof UnknownHostException || er.underlyingException instanceof InterruptedIOException)
								attachment.uploadStateText.setText(R.string.upload_error_connection_lost);
							else
								attachment.uploadStateText.setText(er.error);
						}else{
							attachment.uploadStateText.setText("");
						}
						attachment.retryButton.setImageResource(R.drawable.ic_fluent_arrow_clockwise_24_filled);
						attachment.retryButton.setContentDescription(getString(R.string.retry_upload));

						rotationAnimator.cancel();
						V.setVisibilityAnimated(attachment.retryButton, View.VISIBLE);
						V.setVisibilityAnimated(attachment.progressBar, View.GONE);

						if(!areThereAnyUploadingAttachments())
							uploadNextQueuedAttachment();
					}
				})
				.exec(accountID);
	}

	private void onRemoveMediaAttachmentClick(View v){
		DraftMediaAttachment att=(DraftMediaAttachment) v.getTag();
		if(att.isUploadingOrProcessing())
			att.cancelUpload();
		attachments.remove(att);
		if(!areThereAnyUploadingAttachments())
			uploadNextQueuedAttachment();
		attachmentsView.removeView(att.view);
		if(getMediaAttachmentsCount()==0)
			attachmentsView.setVisibility(View.GONE);
		updatePublishButtonState();
		pollBtn.setEnabled(attachments.isEmpty());
		mediaBtn.setEnabled(true);
		updateSensitive();
	}

	private void onRetryOrCancelMediaUploadClick(View v){
		DraftMediaAttachment att=(DraftMediaAttachment) v.getTag();
		if(att.state==AttachmentUploadState.ERROR){
			att.retryButton.setImageResource(R.drawable.ic_fluent_dismiss_24_filled);
			att.retryButton.setContentDescription(getString(R.string.cancel));
			V.setVisibilityAnimated(att.progressBar, View.VISIBLE);
			att.state=AttachmentUploadState.QUEUED;
			if(!areThereAnyUploadingAttachments()){
				uploadNextQueuedAttachment();
			}
		}else{
			onRemoveMediaAttachmentClick(v);
		}
	}

	private void pollForMediaAttachmentProcessing(DraftMediaAttachment attachment){
		attachment.processingPollingRequest=(GetAttachmentByID) new GetAttachmentByID(attachment.serverAttachment.id)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Attachment result){
						attachment.processingPollingRequest=null;
						if(!TextUtils.isEmpty(result.url)){
							attachment.processingPollingRunnable=null;
							attachment.serverAttachment=result;
							finishMediaAttachmentUpload(attachment);
						}else if(getActivity()!=null){
							UiUtils.runOnUiThread(attachment.processingPollingRunnable, 1000);
						}
					}

					@Override
					public void onError(ErrorResponse error){
						attachment.processingPollingRequest=null;
						if(getActivity()!=null)
							UiUtils.runOnUiThread(attachment.processingPollingRunnable, 1000);
					}
				})
				.exec(accountID);
	}

	private void finishMediaAttachmentUpload(DraftMediaAttachment attachment){
		if(attachment.state!=AttachmentUploadState.PROCESSING && attachment.state!=AttachmentUploadState.UPLOADING)
			throw new IllegalStateException("Unexpected state "+attachment.state);
		attachment.uploadRequest=null;
		attachment.state=AttachmentUploadState.DONE;
		attachment.progressBar.setVisibility(View.GONE);
		if(!areThereAnyUploadingAttachments())
			uploadNextQueuedAttachment();
		updatePublishButtonState();

		if(attachment.progressBarAnimator!=null){
			attachment.progressBarAnimator.cancel();
			attachment.progressBarAnimator=null;
		}
		attachment.setOverlayVisible(false, true);
	}

	private void uploadNextQueuedAttachment(){
		for(DraftMediaAttachment att:attachments){
			if(att.state==AttachmentUploadState.QUEUED){
				uploadMediaAttachment(att);
				return;
			}
		}
	}

	private boolean areThereAnyUploadingAttachments(){
		for(DraftMediaAttachment att:attachments){
			if(att.state==AttachmentUploadState.UPLOADING)
				return true;
		}
		return false;
	}

	private void updateUploadETAs(){
		if(!areThereAnyUploadingAttachments()){
			UiUtils.removeCallbacks(updateUploadEtaRunnable);
			updateUploadEtaRunnable=null;
			return;
		}
		for(DraftMediaAttachment att:attachments){
			if(att.state==AttachmentUploadState.UPLOADING){
				long eta=att.speedTracker.updateAndGetETA();
//				Log.i(TAG, "onProgress: transfer speed "+UiUtils.formatFileSize(getActivity(), Math.round(att.speedTracker.getLastSpeed()), false)+" average "+UiUtils.formatFileSize(getActivity(), Math.round(att.speedTracker.getAverageSpeed()), false)+" eta "+eta);
				String time=String.format("%d:%02d", eta/60, eta%60);
				att.uploadStateText.setText(getString(R.string.file_upload_time_remaining, time));
			}
		}
		UiUtils.runOnUiThread(updateUploadEtaRunnable, 50);
	}

	private void onEditMediaDescriptionClick(View v){
		DraftMediaAttachment att=(DraftMediaAttachment) v.getTag();
		if(att.serverAttachment==null)
			return;
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putString("attachment", att.serverAttachment.id);
		args.putParcelable("uri", att.uri);
		args.putString("existingDescription", att.description);
		Nav.goForResult(getActivity(), ComposeImageDescriptionFragment.class, args, IMAGE_DESCRIPTION_RESULT, this);
	}

	private void togglePoll(){
		if(pollOptions.isEmpty()){
			pollBtn.setSelected(true);
			mediaBtn.setEnabled(false);
			pollWrap.setVisibility(View.VISIBLE);
			for(int i=0;i<2;i++)
				createDraftPollOption();
			updatePollOptionHints();
		}else{
			pollBtn.setSelected(false);
			mediaBtn.setEnabled(true);
			pollWrap.setVisibility(View.GONE);
			addPollOptionBtn.setVisibility(View.VISIBLE);
			pollOptionsView.removeAllViews();
			pollOptions.clear();
			pollDuration=24*3600;
		}
		updatePublishButtonState();
	}

	private DraftPollOption createDraftPollOption(){
		DraftPollOption option=new DraftPollOption();
		option.view=LayoutInflater.from(getActivity()).inflate(R.layout.compose_poll_option, pollOptionsView, false);
		option.edit=option.view.findViewById(R.id.edit);
		option.dragger=option.view.findViewById(R.id.dragger_thingy);
		ImageView icon = option.view.findViewById(R.id.icon);
		icon.setImageDrawable(getContext().getDrawable(pollAllowMultipleItem.isSelected() ?
				R.drawable.ic_poll_checkbox_regular_selector :
				R.drawable.ic_poll_option_button
		));

		option.dragger.setOnLongClickListener(v->{
			pollOptionsView.startDragging(option.view);
			return true;
		});
		option.edit.addTextChangedListener(new SimpleTextWatcher(e->{
			if(!creatingView)
				pollChanged=true;
			updatePublishButtonState();
		}));
		option.edit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(instance.configuration!=null && instance.configuration.polls!=null && instance.configuration.polls.maxCharactersPerOption>0 ? instance.configuration.polls.maxCharactersPerOption : 50)});

		pollOptionsView.addView(option.view);
		pollOptions.add(option);
		if(pollOptions.size()==(instance.configuration!=null && instance.configuration.polls!=null && instance.configuration.polls.maxOptions>0 ? instance.configuration.polls.maxOptions : 4))
			addPollOptionBtn.setVisibility(View.GONE);
		return option;
	}

	private void updatePollOptionHints(){
		int i=0;
		for(DraftPollOption option:pollOptions){
			option.edit.setHint(getString(R.string.poll_option_hint, ++i));
		}
	}

	private void onSwapPollOptions(int oldIndex, int newIndex){
		pollOptions.add(newIndex, pollOptions.remove(oldIndex));
		updatePollOptionHints();
		pollChanged=true;
	}

	private void showPollDurationMenu(){
		PopupMenu menu=new PopupMenu(getActivity(), pollDurationView);
		menu.getMenu().add(0, 1, 0, getResources().getQuantityString(R.plurals.x_minutes, 5, 5));
		menu.getMenu().add(0, 2, 0, getResources().getQuantityString(R.plurals.x_minutes, 30, 30));
		menu.getMenu().add(0, 3, 0, getResources().getQuantityString(R.plurals.x_hours, 1, 1));
		menu.getMenu().add(0, 4, 0, getResources().getQuantityString(R.plurals.x_hours, 6, 6));
		menu.getMenu().add(0, 5, 0, getResources().getQuantityString(R.plurals.x_hours, 12, 12));
		menu.getMenu().add(0, 6, 0, getResources().getQuantityString(R.plurals.x_days, 1, 1));
		menu.getMenu().add(0, 7, 0, getResources().getQuantityString(R.plurals.x_days, 3, 3));
		menu.getMenu().add(0, 8, 0, getResources().getQuantityString(R.plurals.x_days, 7, 7));
		menu.setOnMenuItemClickListener(item->{
			pollDuration=switch(item.getItemId()){
				case 1 -> 5*60;
				case 2 -> 30*60;
				case 3 -> 3600;
				case 4 -> 6*3600;
				case 5 -> 12*3600;
				case 6 -> 24*3600;
				case 7 -> 3*24*3600;
				case 8 -> 7*24*3600;
				default -> throw new IllegalStateException("Unexpected value: "+item.getItemId());
			};
			pollDurationView.setText(getString(R.string.compose_poll_duration, pollDurationStr=item.getTitle().toString()));
			pollChanged=true;
			return true;
		});
		menu.show();
	}

	private void toggleSpoiler(){
		hasSpoiler=!hasSpoiler;
		if(hasSpoiler){
			spoilerEdit.setVisibility(View.VISIBLE);
			spoilerBtn.setSelected(true);
			spoilerEdit.requestFocus();
		}else{
			spoilerEdit.setVisibility(View.GONE);
			spoilerEdit.setText("");
			spoilerBtn.setSelected(false);
			mainEditText.requestFocus();
			updateCharCounter();
			sensitiveIcon.setVisibility(getMediaAttachmentsCount() > 0 ? View.VISIBLE : View.GONE);
		}
		updateSensitive();
	}

	private void toggleSensitive() {
		sensitive=!sensitive;
		sensitiveIcon.setSelected(sensitive);
	}

	private void updateSensitive() {
		sensitiveItem.setVisibility(View.GONE);
		if (!attachments.isEmpty() && !hasSpoiler) sensitiveItem.setVisibility(View.VISIBLE);
		if (attachments.isEmpty()) sensitive = false;
	}

	private void pickScheduledDateTime() {
		LocalDateTime soon = LocalDateTime.now()
				.plus(15, ChronoUnit.MINUTES) // so 14:59 doesn't get rounded up to…
				.plus(1, ChronoUnit.HOURS) // …15:00, but rather 16:00
				.withMinute(0);
		new DatePickerDialog(getActivity(), (datePicker, year, arrayMonth, dayOfMonth) -> {
			new TimePickerDialog(getActivity(), (timePicker, hour, minute) -> {
				updateScheduledAt(LocalDateTime.of(year, arrayMonth + 1, dayOfMonth, hour, minute)
						.toInstant(OffsetDateTime.now().getOffset()));
			}, soon.getHour(), soon.getMinute(), DateFormat.is24HourFormat(getActivity())).show();
		}, soon.getYear(), soon.getMonthValue() - 1, soon.getDayOfMonth()).show();
	}

	private void updateScheduledAt(Instant scheduledAt) {
		this.scheduledAt = scheduledAt;
		updatePublishButtonState();
		scheduleDraftView.setVisibility(scheduledAt == null ? View.GONE : View.VISIBLE);
		draftMenuItem.setVisible(true);
		scheduleMenuItem.setVisible(true);
		undraftMenuItem.setVisible(false);
		unscheduleMenuItem.setVisible(false);
		if (scheduledAt != null) {
			DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault());
			if (scheduledAt.isAfter(DRAFTS_AFTER_INSTANT)) {
				draftMenuItem.setVisible(false);
				undraftMenuItem.setVisible(true);
				scheduleTimeBtn.setVisibility(View.GONE);
				scheduleDraftText.setText(R.string.sk_compose_draft);
				scheduleDraftText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_fluent_drafts_20_regular, 0, 0, 0);
				scheduleDraftDismiss.setContentDescription(getString(R.string.sk_compose_no_draft));
				draftsBtn.setCompoundDrawablesWithIntrinsicBounds(GlobalUserPreferences.relocatePublishButton ? R.drawable.ic_fluent_drafts_24_regular : R.drawable.ic_fluent_drafts_20_filled, 0, 0, 0);

				if(GlobalUserPreferences.relocatePublishButton){
					publishButton.setCompoundDrawablesWithIntrinsicBounds(scheduledStatus != null && scheduledStatus.scheduledAt.isAfter(DRAFTS_AFTER_INSTANT)
						? R.drawable.ic_fluent_save_24_selector : R.drawable.ic_fluent_drafts_24_selector, 0, 0, 0);
				}else{
					publishButton.setText(scheduledStatus != null && scheduledStatus.scheduledAt.isAfter(DRAFTS_AFTER_INSTANT)
							? R.string.save : R.string.sk_draft);
				}
			} else {
				scheduleMenuItem.setVisible(false);
				unscheduleMenuItem.setVisible(true);
				String at = scheduledAt.atZone(ZoneId.systemDefault()).format(formatter);
				scheduleTimeBtn.setVisibility(View.VISIBLE);
				scheduleTimeBtn.setText(at);
				scheduleDraftText.setText(R.string.sk_compose_scheduled);
				scheduleDraftText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
				scheduleDraftDismiss.setContentDescription(getString(R.string.sk_compose_no_schedule));
				draftsBtn.setCompoundDrawablesWithIntrinsicBounds(GlobalUserPreferences.relocatePublishButton ? R.drawable.ic_fluent_clock_24_filled : R.drawable.ic_fluent_clock_20_filled, 0, 0, 0);
				if(GlobalUserPreferences.relocatePublishButton)
				{
					publishButton.setCompoundDrawablesWithIntrinsicBounds(scheduledStatus != null && scheduledStatus.scheduledAt.isAfter(DRAFTS_AFTER_INSTANT)
							? R.drawable.ic_fluent_save_24_selector : R.drawable.ic_fluent_clock_24_selector, 0, 0, 0);
				}else{
					publishButton.setText(scheduledStatus != null && scheduledStatus.scheduledAt.equals(scheduledAt)
							? R.string.save : R.string.sk_schedule);
				}
			}
		} else {
			draftsBtn.setCompoundDrawablesWithIntrinsicBounds(GlobalUserPreferences.relocatePublishButton ? R.drawable.ic_fluent_clock_24_regular : R.drawable.ic_fluent_clock_20_regular, 0, 0, 0);
			if(GlobalUserPreferences.relocatePublishButton){
				publishButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_fluent_send_24_selector, 0, 0, 0);
			}
			resetPublishButtonText();
		}
	}

	private int getMediaAttachmentsCount(){
		return attachments.size();
	}

	private void updateHeaders() {
		UiUtils.setExtraTextInfo(getContext(), selfExtraText, statusVisibility, localOnly);
		if (replyTo != null) UiUtils.setExtraTextInfo(getContext(), extraText, replyTo.visibility, replyTo.localOnly);
	}

	private void buildVisibilityPopup(View v){
		visibilityPopup=new PopupMenu(getActivity(), v);
		visibilityPopup.inflate(R.menu.compose_visibility);
		Menu m=visibilityPopup.getMenu();
		MenuItem localOnlyItem = visibilityPopup.getMenu().findItem(R.id.local_only);
		boolean prefsSaysSupported = GlobalUserPreferences.accountsWithLocalOnlySupport.contains(accountID);
		if (instance.pleroma != null) {
			m.findItem(R.id.vis_local).setVisible(true);
		} else if (localOnly || prefsSaysSupported) {
			localOnlyItem.setVisible(true);
			localOnlyItem.setChecked(localOnly);
			Status status = editingStatus != null ? editingStatus : replyTo;
			if (!prefsSaysSupported) {
				GlobalUserPreferences.accountsWithLocalOnlySupport.add(accountID);
				if (GLITCH_LOCAL_ONLY_PATTERN.matcher(status.getStrippedText()).matches()) {
					GlobalUserPreferences.accountsInGlitchMode.add(accountID);
				}
				GlobalUserPreferences.save();
			}
		}
		UiUtils.enablePopupMenuIcons(getActivity(), visibilityPopup);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) m.setGroupDividerEnabled(true);
		visibilityPopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
			@Override
			public boolean onMenuItemClick(MenuItem item){
				int id=item.getItemId();
				if(id==R.id.vis_public){
					statusVisibility=StatusPrivacy.PUBLIC;
				}else if(id==R.id.vis_unlisted){
					statusVisibility=StatusPrivacy.UNLISTED;
				}else if(id==R.id.vis_followers){
					statusVisibility=StatusPrivacy.PRIVATE;
				}else if(id==R.id.vis_private){
					statusVisibility=StatusPrivacy.DIRECT;
				}else if(id==R.id.vis_local){
					statusVisibility=StatusPrivacy.LOCAL;
				}
				if (id == R.id.local_only) {
					localOnly = !item.isChecked();
					item.setChecked(localOnly);
				} else {
					item.setChecked(true);
				}
				updateVisibilityIcon();
				updateHeaders();
				return true;
			}
		});
	}

	private void loadDefaultStatusVisibility(Bundle savedInstanceState) {
		if(replyTo != null) {
			statusVisibility = (replyTo.visibility == StatusPrivacy.PUBLIC && GlobalUserPreferences.defaultToUnlistedReplies ? StatusPrivacy.UNLISTED : replyTo.visibility);
		}

		// A saved privacy setting from a previous compose session wins over the reply visibility
		if(savedInstanceState !=null){
			statusVisibility = (StatusPrivacy) savedInstanceState.getSerializable("visibility");
		}

		AccountSessionManager asm = AccountSessionManager.getInstance();
		Preferences prefs = asm.getAccount(accountID).preferences;
		if (prefs != null) {
			// Only override the reply visibility if our preference is more private
			// (and we're not replying to ourselves, or not at all)
			if (prefs.postingDefaultVisibility.isLessVisibleThan(statusVisibility) &&
					(replyTo == null || !asm.isSelf(accountID, replyTo.account))) {
				statusVisibility = prefs.postingDefaultVisibility;
			}
		}

		// A saved privacy setting from a previous compose session wins over all
		if(savedInstanceState !=null){
			statusVisibility = (StatusPrivacy) savedInstanceState.getSerializable("visibility");
		}
	}

	private void updateVisibilityIcon(){
		if(statusVisibility==null){ // TODO find out why this happens
			statusVisibility=StatusPrivacy.PUBLIC;
		}
		visibilityBtn.setImageResource(switch(statusVisibility){
			case PUBLIC -> R.drawable.ic_fluent_earth_24_regular;
			case UNLISTED -> R.drawable.ic_fluent_lock_open_24_regular;
			case PRIVATE -> R.drawable.ic_fluent_lock_closed_24_filled;
			case DIRECT -> R.drawable.ic_fluent_mention_24_regular;
			case LOCAL -> R.drawable.ic_fluent_eye_24_regular;
		});
	}

	private void togglePollAllowMultiple() {
		updatePollAllowMultiple(!pollAllowMultipleItem.isSelected());
	}

	private void updatePollAllowMultiple(boolean multiple){
		pollAllowMultipleItem.setSelected(multiple);
		pollAllowMultipleCheckbox.setChecked(multiple);
		ImageView btn = addPollOptionBtn.findViewById(R.id.add_poll_option_icon);
		btn.setImageDrawable(getContext().getDrawable(multiple ?
				R.drawable.ic_fluent_add_square_24_regular :
				R.drawable.ic_fluent_add_circle_24_regular
		));
		for (DraftPollOption opt:pollOptions) {
			ImageView icon = opt.view.findViewById(R.id.icon);
			icon.setImageDrawable(getContext().getDrawable(multiple ?
					R.drawable.ic_poll_checkbox_regular_selector :
					R.drawable.ic_poll_option_button
			));
		}
	}

	@Override
	public void onSelectionChanged(int start, int end){
		if(ignoreSelectionChanges)
			return;
		if(start==end && mainEditText.length()>0){
			ComposeAutocompleteSpan[] spans=mainEditText.getText().getSpans(start, end, ComposeAutocompleteSpan.class);
			if(spans.length>0){
				assert spans.length==1;
				ComposeAutocompleteSpan span=spans[0];
				if(currentAutocompleteSpan==null && end==mainEditText.getText().getSpanEnd(span)){
					startAutocomplete(span);
				}else if(currentAutocompleteSpan!=null){
					Editable e=mainEditText.getText();
					String spanText=e.toString().substring(e.getSpanStart(span), e.getSpanEnd(span));
					autocompleteViewController.setText(spanText);
				}

				View autocompleteView=autocompleteViewController.getView();
				Layout layout=mainEditText.getLayout();
				int line=layout.getLineForOffset(start);
				int offsetY=layout.getLineBottom(line);
				FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams) autocompleteView.getLayoutParams();
				if(lp.topMargin!=offsetY){
					lp.topMargin=offsetY;
					mainEditTextWrap.requestLayout();
				}
				int offsetX=Math.round(layout.getPrimaryHorizontal(start))+mainEditText.getPaddingLeft();
				autocompleteViewController.setArrowOffset(offsetX);
			}else if(currentAutocompleteSpan!=null){
				finishAutocomplete();
			}
		}else if(currentAutocompleteSpan!=null){
			finishAutocomplete();
		}
	}

	@Override
	public String[] onGetAllowedMediaMimeTypes(){
		if(instance!=null && instance.configuration!=null && instance.configuration.mediaAttachments!=null && instance.configuration.mediaAttachments.supportedMimeTypes!=null)
			return instance.configuration.mediaAttachments.supportedMimeTypes.toArray(new String[0]);
		return new String[]{"image/jpeg", "image/gif", "image/png", "video/mp4"};
	}

	@Override
	public boolean onAddMediaAttachmentFromEditText(Uri uri, String description){
		return addMediaAttachment(uri, description);
	}

	private void startAutocomplete(ComposeAutocompleteSpan span){
		currentAutocompleteSpan=span;
		Editable e=mainEditText.getText();
		String spanText=e.toString().substring(e.getSpanStart(span), e.getSpanEnd(span));
		autocompleteViewController.setText(spanText);
		View autocompleteView=autocompleteViewController.getView();
		autocompleteView.setVisibility(View.VISIBLE);
	}

	private void finishAutocomplete(){
		if(currentAutocompleteSpan==null)
			return;
		autocompleteViewController.setText(null);
		currentAutocompleteSpan=null;
		autocompleteViewController.getView().setVisibility(View.GONE);
	}

	private void onAutocompleteOptionSelected(String text){
		Editable e=mainEditText.getText();
		int start=e.getSpanStart(currentAutocompleteSpan);
		int end=e.getSpanEnd(currentAutocompleteSpan);
		e.replace(start, end, text+" ");
		mainEditText.setSelection(start+text.length()+1);
		finishAutocomplete();
	}

	private void loadVideoThumbIntoView(ImageView target, Uri uri){
		MastodonAPIController.runInBackground(()->{
			Context context=getActivity();
			if(context==null)
				return;
			try{
				MediaMetadataRetriever mmr=new MediaMetadataRetriever();
				mmr.setDataSource(context, uri);
				Bitmap frame=mmr.getFrameAtTime(3_000_000);
				mmr.release();
				int size=Math.max(frame.getWidth(), frame.getHeight());
				int maxSize=V.dp(250);
				if(size>maxSize){
					float factor=maxSize/(float)size;
					frame=Bitmap.createScaledBitmap(frame, Math.round(frame.getWidth()*factor), Math.round(frame.getHeight()*factor), true);
				}
				Bitmap finalFrame=frame;
				target.post(()->target.setImageBitmap(finalFrame));
			}catch(Exception x){
				Log.w(TAG, "loadVideoThumbIntoView: error getting video frame", x);
			}
		});
	}
	private void editMediaDescription(DraftMediaAttachment att) {
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putString("attachment", att.serverAttachment.id);
		args.putParcelable("uri", att.uri);
		args.putString("existingDescription", att.description);
		Nav.goForResult(getActivity(), ComposeImageDescriptionFragment.class, args, IMAGE_DESCRIPTION_RESULT, this);
	}

	@Override
	public CharSequence getTitle(){
		return getString(R.string.new_post);
	}

	@Override
	public boolean wantsLightStatusBar(){
		return !UiUtils.isDarkTheme();
	}

	@Override
	public boolean wantsLightNavigationBar(){
		return !UiUtils.isDarkTheme();
	}

	@Parcel
	static class DraftMediaAttachment{
		public Attachment serverAttachment;
		public Uri uri;
		public transient UploadAttachment uploadRequest;
		public transient GetAttachmentByID processingPollingRequest;
		public String description;
		public String mimeType;
		public AttachmentUploadState state=AttachmentUploadState.QUEUED;

		public transient View view;
		public transient ProgressBar progressBar;
		public transient TextView descriptionView;
		public transient View overlay;
		public transient View infoBar;
		public transient ImageButton retryButton;
		public transient ObjectAnimator progressBarAnimator;
		public transient Runnable processingPollingRunnable;
		public transient ImageView imageView;
		public transient TextView uploadStateTitle, uploadStateText;
		public transient TransferSpeedTracker speedTracker=new TransferSpeedTracker();

		public void cancelUpload(){
			switch(state){
				case UPLOADING -> {
					if(uploadRequest!=null){
						uploadRequest.cancel();
						uploadRequest=null;
					}
				}
				case PROCESSING -> {
					if(processingPollingRunnable!=null){
						UiUtils.removeCallbacks(processingPollingRunnable);
						processingPollingRunnable=null;
					}
					if(processingPollingRequest!=null){
						processingPollingRequest.cancel();
						processingPollingRequest=null;
					}
				}
				default -> throw new IllegalStateException("Unexpected state "+state);
			}
		}

		public boolean isUploadingOrProcessing(){
			return state==AttachmentUploadState.UPLOADING || state==AttachmentUploadState.PROCESSING;
		}

		public void setOverlayVisible(boolean visible, boolean animated){
			if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
				if(visible){
					imageView.setRenderEffect(RenderEffect.createBlurEffect(V.dp(16), V.dp(16), Shader.TileMode.REPEAT));
				}else{
					imageView.setRenderEffect(null);
				}
			}
			int infoBarVis=visible ? View.GONE : View.VISIBLE;
			int overlayVis=visible ? View.VISIBLE : View.GONE;
			if(animated){
				V.setVisibilityAnimated(infoBar, infoBarVis);
				V.setVisibilityAnimated(overlay, overlayVis);
			}else{
				infoBar.setVisibility(infoBarVis);
				overlay.setVisibility(overlayVis);
			}
		}
	}

	enum AttachmentUploadState{
		QUEUED,
		UPLOADING,
		PROCESSING,
		ERROR,
		DONE
	}

	private static class DraftPollOption{
		public EditText edit;
		public View view;
		public View dragger;
	}
}
