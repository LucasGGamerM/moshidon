package org.joinmastodon.android.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import org.joinmastodon.android.DomainManager;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountByID;
import org.joinmastodon.android.api.requests.accounts.GetAccountRelationships;
import org.joinmastodon.android.api.requests.accounts.GetAccountStatuses;
import org.joinmastodon.android.api.requests.accounts.GetOwnAccount;
import org.joinmastodon.android.api.requests.accounts.SetAccountFollowed;
import org.joinmastodon.android.api.requests.accounts.SetPrivateNote;
import org.joinmastodon.android.api.requests.accounts.UpdateAccountCredentials;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.account_list.FollowerListFragment;
import org.joinmastodon.android.fragments.account_list.FollowingListFragment;
import org.joinmastodon.android.fragments.report.ReportReasonChoiceFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AccountField;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.ui.BetterItemAnimator;
import org.joinmastodon.android.ui.SimpleViewHolder;
import org.joinmastodon.android.ui.SingleImagePhotoViewerListener;
import org.joinmastodon.android.ui.drawables.CoverOverlayGradientDrawable;
import org.joinmastodon.android.ui.photoviewer.PhotoViewer;
import org.joinmastodon.android.ui.tabs.TabLayout;
import org.joinmastodon.android.ui.tabs.TabLayoutMediator;
import org.joinmastodon.android.ui.text.CustomEmojiSpan;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.CoverImageView;
import org.joinmastodon.android.ui.views.LinkedTextView;
import org.joinmastodon.android.ui.views.NestedRecyclerScrollView;
import org.joinmastodon.android.ui.views.ProgressBarButton;
import org.parceler.Parcels;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.fragments.LoaderFragment;
import me.grishka.appkit.fragments.OnBackPressedListener;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.ListImageLoaderWrapper;
import me.grishka.appkit.imageloader.RecyclerViewDelegate;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class ProfileFragment extends LoaderFragment implements OnBackPressedListener, ScrollableToTop{
	private static final int AVATAR_RESULT=722;
	private static final int COVER_RESULT=343;

	private ImageView avatar;
	private CoverImageView cover;
	private View avatarBorder, nameWrap;
	private TextView name, username, bio, followersCount, followersLabel, followingCount, followingLabel, postsCount, postsLabel;
	private ProgressBarButton actionButton, notifyButton;
	private ViewPager2 pager;
	private NestedRecyclerScrollView scrollView;
	private AccountTimelineFragment postsFragment, postsWithRepliesFragment, pinnedPostsFragment, mediaFragment;
//	private ProfileAboutFragment aboutFragment;
	private TabLayout tabbar;
	private SwipeRefreshLayout refreshLayout;
	private CoverOverlayGradientDrawable coverGradient=new CoverOverlayGradientDrawable();
	private float titleTransY;
	private View postsBtn, followersBtn, followingBtn, profileCounters;
	private EditText nameEdit, bioEdit;
	private ProgressBar actionProgress, notifyProgress;
	private FrameLayout[] tabViews;
	private TabLayoutMediator tabLayoutMediator;
	private TextView followsYouView;
	private ViewGroup rolesView;

	public FrameLayout noteWrap;
	public EditText noteEdit;
	private String note;
	private Account account;
	private String accountID;
	private Relationship relationship;
	private int statusBarHeight;
	private boolean isOwnProfile;
	private ArrayList<AccountField> fields=new ArrayList<>();

	private boolean isInEditMode;
	private Uri editNewAvatar, editNewCover;
	private String profileAccountID;
	private boolean refreshing;
	private ImageButton fab;
	private WindowInsets childInsets;
	private PhotoViewer currentPhotoViewer;
	private boolean editModeLoading;
	protected int scrollDiff = 0;

	private static final int MAX_FIELDS=4;

	// from ProfileAboutFragment
	public UsableRecyclerView list;
	private List<AccountField> metadataListData=Collections.emptyList();
	private MetadataAdapter adapter;
	private ItemTouchHelper dragHelper=new ItemTouchHelper(new ReorderCallback());
	private RecyclerView.ViewHolder draggedViewHolder;
	private ListImageLoaderWrapper imgLoader;

	public ProfileFragment(){
		super(R.layout.loader_fragment_overlay_toolbar);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			setRetainInstance(true);

		accountID=getArguments().getString("account");
		if(getArguments().containsKey("profileAccount")){
			account=Parcels.unwrap(getArguments().getParcelable("profileAccount"));
			profileAccountID=account.id;
			isOwnProfile=AccountSessionManager.getInstance().isSelf(accountID, account);
			loaded=true;
			if(!isOwnProfile)
				loadRelationship();
		}else{
			profileAccountID=getArguments().getString("profileAccountID");
			if(!getArguments().getBoolean("noAutoLoad", false))
				loadData();
		}
	}

	private String getPrefilledText() {
		return account == null || AccountSessionManager.getInstance().isSelf(accountID, account)
				? null : '@'+account.acct+' ';
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setHasOptionsMenu(true);
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		if (!hidden) {
			DomainManager.getInstance().setCurrentDomain(account.url);
		}
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View content=inflater.inflate(R.layout.fragment_profile, container, false);

		avatar=content.findViewById(R.id.avatar);
		cover=content.findViewById(R.id.cover);
		avatarBorder=content.findViewById(R.id.avatar_border);
		name=content.findViewById(R.id.name);
		nameWrap=content.findViewById(R.id.name_wrap);
		username=content.findViewById(R.id.username);
		bio=content.findViewById(R.id.bio);
		profileCounters=content.findViewById(R.id.profile_counters);
		followersCount=content.findViewById(R.id.followers_count);
		followersLabel=content.findViewById(R.id.followers_label);
		followersBtn=content.findViewById(R.id.followers_btn);
		followingCount=content.findViewById(R.id.following_count);
		followingLabel=content.findViewById(R.id.following_label);
		followingBtn=content.findViewById(R.id.following_btn);

		postsCount=content.findViewById(R.id.posts_count);
		postsLabel=content.findViewById(R.id.posts_label);
		postsBtn=content.findViewById(R.id.posts_btn);
		actionButton=content.findViewById(R.id.profile_action_btn);
		notifyButton=content.findViewById(R.id.notify_btn);
		pager=content.findViewById(R.id.pager);
		scrollView=content.findViewById(R.id.scroller);
		tabbar=content.findViewById(R.id.tabbar);
		refreshLayout=content.findViewById(R.id.refresh_layout);
		nameEdit=content.findViewById(R.id.name_edit);
		bioEdit=content.findViewById(R.id.bio_edit);
		actionProgress=content.findViewById(R.id.action_progress);
		notifyProgress=content.findViewById(R.id.notify_progress);
		fab=content.findViewById(R.id.fab);
		followsYouView=content.findViewById(R.id.follows_you);
		list=content.findViewById(R.id.metadata);
		rolesView=content.findViewById(R.id.roles);

		noteEdit = content.findViewById(R.id.note_edit);
		noteWrap = content.findViewById(R.id.note_edit_wrap);
		Button noteEditConfirm = content.findViewById(R.id.note_edit_confirm);

		avatar.setOutlineProvider(new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), V.dp(25));
			}
		});
		avatar.setClipToOutline(true);

		noteEdit.setOnFocusChangeListener((v, hasFocus) -> {
			if (hasFocus) {
				fab.setVisibility(View.INVISIBLE);
				TranslateAnimation animate = new TranslateAnimation(
						0,
						0,
						0,
						fab.getHeight() * 2);
				animate.setDuration(300);
				animate.setFillAfter(true);
				fab.startAnimation(animate);

				noteEditConfirm.setVisibility(View.VISIBLE);
				noteEditConfirm.animate()
						.alpha(1.0f)
						.setDuration(700);
			} else {
				fab.setVisibility(View.VISIBLE);
				TranslateAnimation animate = new TranslateAnimation(
						0,
						0,
						fab.getHeight() * 2,
						0);
				animate.setDuration(300);
				animate.setFillAfter(true);
				fab.startAnimation(animate);

				noteEditConfirm.animate()
						.alpha(0.0f)
						.setDuration(700);
				noteEditConfirm.setVisibility(View.INVISIBLE);
			}
		});

		noteEditConfirm.setOnClickListener((v -> {
			if (!noteEdit.getText().toString().trim().equals(note)) {
				savePrivateNote();
			}
			InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(this.getView().getRootView().getWindowToken(), 0);
			noteEdit.clearFocus();
		}));

		FrameLayout sizeWrapper=new FrameLayout(getActivity()){
			@Override
			protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
				Toolbar toolbar=getToolbar();
				pager.getLayoutParams().height=MeasureSpec.getSize(heightMeasureSpec)-getPaddingTop()-getPaddingBottom()-toolbar.getLayoutParams().height-statusBarHeight-V.dp(38);
				coverGradient.setTopPadding(statusBarHeight+toolbar.getLayoutParams().height);
				super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			}
		};

		tabViews=new FrameLayout[4];
		for(int i=0;i<tabViews.length;i++){
			FrameLayout tabView=new FrameLayout(getActivity());
			tabView.setId(switch(i){
				case 0 -> R.id.profile_posts;
				case 1 -> R.id.profile_posts_with_replies;
				case 2 -> R.id.profile_pinned_posts;
				case 3 -> R.id.profile_media;
				case 4 -> R.id.profile_about;
				default -> throw new IllegalStateException("Unexpected value: "+i);
			});
			tabView.setVisibility(View.GONE);
			sizeWrapper.addView(tabView); // needed so the fragment manager will have somewhere to restore the tab fragment
			tabViews[i]=tabView;
		}

		UiUtils.reduceSwipeSensitivity(pager);
		pager.setOffscreenPageLimit(4);
		pager.setUserInputEnabled(!GlobalUserPreferences.disableSwipe);
		pager.setAdapter(new ProfilePagerAdapter());
		pager.getLayoutParams().height=getResources().getDisplayMetrics().heightPixels;

		scrollView.setScrollableChildSupplier(this::getScrollableRecyclerView);

		sizeWrapper.addView(content);

		tabbar.setTabTextColors(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary), UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
		tabbar.setTabTextSize(V.dp(16));
		tabLayoutMediator=new TabLayoutMediator(tabbar, pager, new TabLayoutMediator.TabConfigurationStrategy(){
			@Override
			public void onConfigureTab(@NonNull TabLayout.Tab tab, int position){
				tab.setText(switch(position){
					case 0 -> R.string.posts;
					case 1 -> R.string.posts_and_replies;
					case 2 -> R.string.sk_pinned_posts;
					case 3 -> R.string.media;
					case 4 -> R.string.profile_about;
					default -> throw new IllegalStateException();
				});
			}
		});

		cover.setForeground(coverGradient);
		cover.setOutlineProvider(new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setEmpty();
			}
		});

		actionButton.setOnClickListener(this::onActionButtonClick);
		actionButton.setOnLongClickListener(this::onActionButtonLongClick);
		notifyButton.setOnClickListener(this::onNotifyButtonClick);
		avatar.setOnClickListener(this::onAvatarClick);
		cover.setOnClickListener(this::onCoverClick);
		refreshLayout.setOnRefreshListener(this);
		fab.setOnClickListener(this::onFabClick);
		fab.setOnLongClickListener(v->UiUtils.pickAccountForCompose(getActivity(), accountID, getPrefilledText()));

		if(loaded){
			bindHeaderView();
			dataLoaded();
			tabLayoutMediator.attach();
		}else{
			fab.setVisibility(View.GONE);
		}

		followersBtn.setOnClickListener(this::onFollowersOrFollowingClick);
		followingBtn.setOnClickListener(this::onFollowersOrFollowingClick);

		username.setOnLongClickListener(v->{
			String usernameString=account.acct;
			if(!usernameString.contains("@")){
				usernameString+="@"+AccountSessionManager.getInstance().getAccount(accountID).domain;
			}
			UiUtils.copyText(username, '@'+usernameString);
			return true;
		});

		// from ProfileAboutFragment
		list.setItemAnimator(new BetterItemAnimator());
		list.setDrawSelectorOnTop(true);
		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		imgLoader=new ListImageLoaderWrapper(getActivity(), list, new RecyclerViewDelegate(list), null);
		list.setAdapter(adapter=new MetadataAdapter());
		list.setClipToPadding(false);

		return sizeWrapper;
	}

	public void setNote(String note){
		this.note=note;
		noteWrap.setVisibility(View.VISIBLE);
		noteEdit.setVisibility(View.VISIBLE);
		noteEdit.setText(note);
	}

	private void savePrivateNote(){
		new SetPrivateNote(profileAccountID, noteEdit.getText().toString()).setCallback(new Callback<>() {
			@Override
			public void onSuccess(Relationship result) {}

			@Override
			public void onError(ErrorResponse result) {
				Toast.makeText(getActivity(), getString(R.string.mo_personal_note_update_failed), Toast.LENGTH_LONG).show();
			}
		}).exec(accountID);
	}

	@Override
	protected void doLoadData(){
		currentRequest=new GetAccountByID(profileAccountID)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(Account result){
						if (getActivity() == null) return;
						account=result;
						isOwnProfile=AccountSessionManager.getInstance().isSelf(accountID, account);
						bindHeaderView();
						dataLoaded();
						if(!tabLayoutMediator.isAttached())
							tabLayoutMediator.attach();
						if(!isOwnProfile)
							loadRelationship();
						else
							AccountSessionManager.getInstance().updateAccountInfo(accountID, account);
						if(refreshing){
							refreshing=false;
							refreshLayout.setRefreshing(false);
							if(postsFragment.loaded)
								postsFragment.onRefresh();
							if(postsWithRepliesFragment.loaded)
								postsWithRepliesFragment.onRefresh();
							if(pinnedPostsFragment.loaded)
								pinnedPostsFragment.onRefresh();
							if(mediaFragment.loaded)
								mediaFragment.onRefresh();
						}
						V.setVisibilityAnimated(fab, View.VISIBLE);
					}
				})
				.exec(accountID);
	}

	@Override
	public void onRefresh(){
		if(isInEditMode){
			refreshing=false;
			refreshLayout.setRefreshing(false);
			return;
		}
		if(refreshing)
			return;
		refreshing=true;
		doLoadData();
	}

	@Override
	public void dataLoaded(){
		if(getActivity()==null)
			return;
		if(postsFragment==null){
			postsFragment=AccountTimelineFragment.newInstance(accountID, account, GetAccountStatuses.Filter.DEFAULT, true);
			postsWithRepliesFragment=AccountTimelineFragment.newInstance(accountID, account, GetAccountStatuses.Filter.INCLUDE_REPLIES, false);
			pinnedPostsFragment=AccountTimelineFragment.newInstance(accountID, account, GetAccountStatuses.Filter.PINNED, false);
			mediaFragment=AccountTimelineFragment.newInstance(accountID, account, GetAccountStatuses.Filter.MEDIA, false);
//			aboutFragment=new ProfileAboutFragment();
			setFields(fields);
		}
		pager.getAdapter().notifyDataSetChanged();
		super.dataLoaded();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		updateToolbar();
		// To avoid the callback triggering on first layout with position=0 before anything is instantiated
		pager.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
			@Override
			public boolean onPreDraw(){
				pager.getViewTreeObserver().removeOnPreDrawListener(this);
				pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){
					@Override
					public void onPageSelected(int position){
						if(position==0)
							return;
						Fragment _page=getFragmentForPage(position);
						if(_page instanceof BaseRecyclerFragment<?> page){
							if(!page.loaded && !page.isDataLoading())
								page.loadData();
						}
					}

					@Override
					public void onPageScrollStateChanged(int state){
						refreshLayout.setEnabled(state!=ViewPager2.SCROLL_STATE_DRAGGING);
					}
				});
				return true;
			}
		});

		scrollView.setOnScrollChangeListener(this::onScrollChanged);
		titleTransY=getToolbar().getLayoutParams().height;
		if(toolbarTitleView!=null){
			toolbarTitleView.setTranslationY(titleTransY);
			toolbarSubtitleView.setTranslationY(titleTransY);
		}
		RecyclerFragment.setRefreshLayoutColors(refreshLayout);
	}

	@Override
	public void onDestroyView(){
		super.onDestroyView();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		updateToolbar();
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		statusBarHeight=insets.getSystemWindowInsetTop();
		if(contentView!=null){
			((ViewGroup.MarginLayoutParams) getToolbar().getLayoutParams()).topMargin=statusBarHeight;
			refreshLayout.setProgressViewEndTarget(true, statusBarHeight+refreshLayout.getProgressCircleDiameter()+V.dp(24));
			if(Build.VERSION.SDK_INT>=29 && insets.getTappableElementInsets().bottom==0){
				int insetBottom=insets.getSystemWindowInsetBottom();
				childInsets=insets.inset(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
				((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin=V.dp(24)+insetBottom;
				applyChildWindowInsets();
				insets=insets.inset(0, 0, 0, insetBottom);
			}else{
				((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin=V.dp(24);
			}
		}
		super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()));
	}

	private void applyChildWindowInsets(){
		if(postsFragment!=null && postsFragment.isAdded() && childInsets!=null){
			postsFragment.onApplyWindowInsets(childInsets);
			postsWithRepliesFragment.onApplyWindowInsets(childInsets);
			pinnedPostsFragment.onApplyWindowInsets(childInsets);
			mediaFragment.onApplyWindowInsets(childInsets);
		}
	}

	private void bindHeaderView(){
		setTitle(account.displayName);
		setSubtitle(getResources().getQuantityString(R.plurals.x_posts, (int)(account.statusesCount%1000), account.statusesCount));
		ViewImageLoader.load(avatar, null, new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? account.avatar : account.avatarStatic, V.dp(100), V.dp(100)));
		ViewImageLoader.load(cover, null, new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? account.header : account.headerStatic, 1000, 1000));
		SpannableStringBuilder ssb=new SpannableStringBuilder(account.displayName);
		HtmlParser.parseCustomEmoji(ssb, account.emojis);
		name.setText(ssb);
		setTitle(ssb);

		if (account.roles != null && !account.roles.isEmpty()) {
			rolesView.setVisibility(View.VISIBLE);
			rolesView.removeAllViews();
			name.setPadding(0, 0, V.dp(12), 0);
			for (Account.Role role : account.roles) {
				TextView roleText = new TextView(getActivity(), null, 0, R.style.role_label);
				roleText.setText(role.name);
				if (!TextUtils.isEmpty(role.color) && role.color.startsWith("#")) try {
					GradientDrawable bg = (GradientDrawable) roleText.getBackground().mutate();
					bg.setStroke(V.dp(2), Color.parseColor(role.color));
				} catch (Exception ignored) {}
				rolesView.addView(roleText);
			}
		}

		boolean isSelf=AccountSessionManager.getInstance().isSelf(accountID, account);


		if(account.locked){
			ssb=new SpannableStringBuilder("@");
			ssb.append(account.acct);
			if(isSelf){
				ssb.append('@');
				ssb.append(AccountSessionManager.getInstance().getAccount(accountID).domain);
			}
			ssb.append(" ");
			Drawable lock=username.getResources().getDrawable(R.drawable.ic_lock, getActivity().getTheme()).mutate();
			lock.setBounds(0, 0, lock.getIntrinsicWidth(), lock.getIntrinsicHeight());
			lock.setTint(username.getCurrentTextColor());
			ssb.append(getString(R.string.manually_approves_followers), new ImageSpan(lock, ImageSpan.ALIGN_BASELINE), 0);
			username.setText(ssb);
		}else if(account.bot){
			ssb=new SpannableStringBuilder("@");
			ssb.append(account.acct);
			if(isSelf){
				ssb.append('@');
				ssb.append(AccountSessionManager.getInstance().getAccount(accountID).domain);
			}
			ssb.append(" ");
			Drawable botIcon=username.getResources().getDrawable(R.drawable.ic_bot, getActivity().getTheme()).mutate();
			botIcon.setBounds(0, 0, botIcon.getIntrinsicWidth(), botIcon.getIntrinsicHeight());
			botIcon.setTint(username.getCurrentTextColor());
			ssb.append(getString(R.string.manually_approves_followers), new ImageSpan(botIcon, ImageSpan.ALIGN_BASELINE), 0);
			username.setText(ssb);
		}else{
			// noinspection SetTextI18n
			username.setText('@'+account.acct+(isSelf ? ('@'+AccountSessionManager.getInstance().getAccount(accountID).domain) : ""));
		}
		CharSequence parsedBio=HtmlParser.parse(account.note, account.emojis, Collections.emptyList(), Collections.emptyList(), accountID);
		if(TextUtils.isEmpty(parsedBio)){
			bio.setVisibility(View.GONE);
		}else{
			bio.setVisibility(View.VISIBLE);
			bio.setText(parsedBio);
		}


		followersCount.setText(UiUtils.abbreviateNumber(account.followersCount));
		followingCount.setText(UiUtils.abbreviateNumber(account.followingCount));
		postsCount.setText(UiUtils.abbreviateNumber(account.statusesCount));
		followersLabel.setText(getResources().getQuantityString(R.plurals.followers, (int)Math.min(999, account.followersCount)));
		followingLabel.setText(getResources().getQuantityString(R.plurals.following, (int)Math.min(999, account.followingCount)));
		postsLabel.setText(getResources().getQuantityString(R.plurals.posts, (int)Math.min(999, account.statusesCount)));

		UiUtils.loadCustomEmojiInTextView(name);
		UiUtils.loadCustomEmojiInTextView(bio);

		notifyButton.setVisibility(View.GONE);
		if(AccountSessionManager.getInstance().isSelf(accountID, account)){
			actionButton.setText(R.string.edit_profile);
		}else{
			actionButton.setVisibility(View.GONE);
		}

		fields.clear();

		if (account.createdAt != null) {
			AccountField joined=new AccountField();
			joined.parsedName=joined.name=getString(R.string.profile_joined);
			joined.parsedValue=joined.value=DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(LocalDateTime.ofInstant(account.createdAt, ZoneId.systemDefault()));
			fields.add(joined);
		}

		for(AccountField field:account.fields){
			field.parsedValue=ssb=HtmlParser.parse(field.value, account.emojis, Collections.emptyList(), Collections.emptyList(), accountID);
			field.valueEmojis=ssb.getSpans(0, ssb.length(), CustomEmojiSpan.class);
			ssb=new SpannableStringBuilder(field.name);
			HtmlParser.parseCustomEmoji(ssb, account.emojis);
			field.parsedName=ssb;
			field.nameEmojis=ssb.getSpans(0, ssb.length(), CustomEmojiSpan.class);
			field.emojiRequests=new ArrayList<>(field.nameEmojis.length+field.valueEmojis.length);
			for(CustomEmojiSpan span:field.nameEmojis){
				field.emojiRequests.add(span.createImageLoaderRequest());
			}
			for(CustomEmojiSpan span:field.valueEmojis){
				field.emojiRequests.add(span.createImageLoaderRequest());
			}
			fields.add(field);
		}

		setFields(fields);
	}

	private void updateToolbar(){
		getToolbar().setBackgroundColor(0);
		if(toolbarTitleView!=null){
			toolbarTitleView.setTranslationY(titleTransY);
			toolbarSubtitleView.setTranslationY(titleTransY);
		}
		getToolbar().setOnClickListener(v->scrollToTop());
		getToolbar().setNavigationContentDescription(R.string.back);
	}

	@Override
	public boolean wantsLightStatusBar(){
		return false;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		if(isOwnProfile && isInEditMode){
			Button cancelButton=new Button(getActivity(), null, 0, R.style.Widget_Mastodon_Button_Secondary_LightOnDark);
			cancelButton.setText(R.string.cancel);
			cancelButton.setOnClickListener(v->exitEditMode());
			FrameLayout wrap=new FrameLayout(getActivity());
			wrap.addView(cancelButton, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP|Gravity.LEFT));
			wrap.setPadding(V.dp(16), V.dp(4), V.dp(16), V.dp(8));
			wrap.setClipToPadding(false);
			MenuItem item=menu.add(R.string.cancel);
			item.setActionView(wrap);
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			return;
		}
		if(relationship==null && !isOwnProfile)
			return;
		inflater.inflate(isOwnProfile ? R.menu.profile_own : R.menu.profile, menu);
		if(isOwnProfile){
			UiUtils.enableOptionsMenuIcons(getActivity(), menu, R.id.scheduled, R.id.bookmarks, R.id.favorites);
		}else{
			UiUtils.enableOptionsMenuIcons(getActivity(), menu, R.id.bookmarks, R.id.followed_hashtags, R.id.favorites, R.id.scheduled);
		}
		boolean hasMultipleAccounts = AccountSessionManager.getInstance().getLoggedInAccounts().size() > 1;
		MenuItem openWithAccounts = menu.findItem(R.id.open_with_account);
		openWithAccounts.setVisible(hasMultipleAccounts);
		SubMenu accountsMenu = openWithAccounts.getSubMenu();
		if (hasMultipleAccounts) {
			accountsMenu.clear();
			UiUtils.populateAccountsMenu(accountID, accountsMenu, s-> UiUtils.openURL(
					getActivity(), s.getID(), account.url, false
			));
		}
		menu.findItem(R.id.share).setTitle(getString(R.string.share_user, account.getShortUsername()));
		if(isOwnProfile)
			return;

		MenuItem mute = menu.findItem(R.id.mute);
		mute.setTitle(getString(relationship.muting ? R.string.unmute_user : R.string.mute_user, account.getShortUsername()));
		mute.setIcon(relationship.muting ? R.drawable.ic_fluent_speaker_0_24_regular : R.drawable.ic_fluent_speaker_off_24_regular);
		UiUtils.insetPopupMenuIcon(getContext(), mute);

		menu.findItem(R.id.block).setTitle(getString(relationship.blocking ? R.string.unblock_user : R.string.block_user, account.getShortUsername()));
		menu.findItem(R.id.report).setTitle(getString(R.string.report_user, account.getShortUsername()));
		menu.findItem(R.id.manage_user_lists).setVisible(relationship.following);
		menu.findItem(R.id.soft_block).setVisible(relationship.followedBy && !relationship.following);
		if(relationship.following) {
			MenuItem hideBoosts = menu.findItem(R.id.hide_boosts);
			hideBoosts.setTitle(getString(relationship.showingReblogs ? R.string.hide_boosts_from_user : R.string.show_boosts_from_user, account.getShortUsername()));
			hideBoosts.setIcon(relationship.showingReblogs ? R.drawable.ic_fluent_arrow_repeat_all_off_24_regular : R.drawable.ic_fluent_arrow_repeat_all_24_regular);
			UiUtils.insetPopupMenuIcon(getContext(), hideBoosts);
			menu.findItem(R.id.manage_user_lists).setTitle(getString(R.string.sk_lists_with_user, account.getShortUsername()));
		}else {
			menu.findItem(R.id.hide_boosts).setVisible(false);
		}
		if(!account.isLocal())
			menu.findItem(R.id.block_domain).setTitle(getString(relationship.domainBlocking ? R.string.unblock_domain : R.string.block_domain, account.getDomain()));
		else
			menu.findItem(R.id.block_domain).setVisible(false);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		int id=item.getItemId();
		if(id==R.id.share) {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, account.url);
			startActivity(Intent.createChooser(intent, item.getTitle()));
		}else if(id==R.id.mute){
			confirmToggleMuted();
		}else if(id==R.id.block){
			confirmToggleBlocked();
		}else if(id==R.id.soft_block){
			confirmSoftBlockUser();
		}else if(id==R.id.report){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("reportAccount", Parcels.wrap(account));
			Nav.go(getActivity(), ReportReasonChoiceFragment.class, args);
		}else if(id==R.id.open_in_browser){
			UiUtils.launchWebBrowser(getActivity(), account.url);
		}else if(id==R.id.block_domain){
			UiUtils.confirmToggleBlockDomain(getActivity(), accountID, account.getDomain(), relationship.domainBlocking, ()->{
				relationship.domainBlocking=!relationship.domainBlocking;
				updateRelationship();
			});
		}else if(id==R.id.hide_boosts){
			new SetAccountFollowed(account.id, true, !relationship.showingReblogs, relationship.notifying)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Relationship result){
							updateRelationship(result);
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(getActivity());
						}
					})
					.wrapProgress(getActivity(), R.string.loading, false)
					.exec(accountID);
		}else if(id==R.id.bookmarks){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), BookmarkedStatusListFragment.class, args);
		}else if(id==R.id.favorites){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), FavoritedStatusListFragment.class, args);
		}else if(id==R.id.manage_user_lists){
			final Bundle args=new Bundle();
			args.putString("account", accountID);
			if (!isOwnProfile) {
				args.putString("profileAccount", profileAccountID);
				args.putString("profileDisplayUsername", account.getDisplayUsername());
			}
			Nav.go(getActivity(), ListTimelinesFragment.class, args);
		}else if(id==R.id.followed_hashtags){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), FollowedHashtagsFragment.class, args);
		}else if(id==R.id.scheduled){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), ScheduledStatusListFragment.class, args);
		}
		return true;
	}

	@Override
	protected int getToolbarResource(){
		return R.layout.profile_toolbar;
	}

	private void loadRelationship(){
		new GetAccountRelationships(Collections.singletonList(account.id))
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Relationship> result){
						if(!result.isEmpty()){
							relationship=result.get(0);
							updateRelationship();
						}
					}

					@Override
					public void onError(ErrorResponse error){

					}
				})
				.exec(accountID);
	}

	private void updateRelationship(){
		if (getActivity() == null) return;
		invalidateOptionsMenu();
		actionButton.setVisibility(View.VISIBLE);
		notifyButton.setVisibility(relationship.following ? View.VISIBLE : View.GONE);
		UiUtils.setRelationshipToActionButton(relationship, actionButton);
		UiUtils.setRelationshipToActionButton(relationship, notifyButton, true);
		actionProgress.setIndeterminateTintList(actionButton.getTextColors());
		notifyProgress.setIndeterminateTintList(notifyButton.getTextColors());
		followsYouView.setVisibility(relationship.followedBy ? View.VISIBLE : View.GONE);
		notifyButton.setSelected(relationship.notifying);
		if (!isOwnProfile) {
			setNote(relationship.note);
//			aboutFragment.setNote(relationship.note, accountID, profileAccountID);
		}
		notifyButton.setContentDescription(getString(relationship.notifying ? R.string.sk_user_post_notifications_on : R.string.sk_user_post_notifications_off, '@'+account.username));
		DomainManager.getInstance().setCurrentDomain(account.url);
	}

	public ImageButton getFab() {
		return fab;
	}

	private void onScrollChanged(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY){
		int topBarsH=getToolbar().getHeight()+statusBarHeight;
		if(scrollY>avatarBorder.getTop()-topBarsH){
			float avaAlpha=Math.max(1f-((scrollY-(avatarBorder.getTop()-topBarsH))/(float)V.dp(38)), 0f);
			avatarBorder.setAlpha(avaAlpha);
		}else{
			avatarBorder.setAlpha(1f);
		}
		if(scrollY>cover.getHeight()-topBarsH){
			cover.setTranslationY(scrollY-(cover.getHeight()-topBarsH));
			cover.setTranslationZ(V.dp(10));
			cover.setTransform(cover.getHeight()/2f-topBarsH/2f, 1f);
		}else{
			cover.setTranslationY(0f);
			cover.setTranslationZ(0f);
			cover.setTransform(scrollY/2f, 1f);
		}
		coverGradient.setTopOffset(scrollY);
		cover.invalidate();
		titleTransY=getToolbar().getHeight();
		if(scrollY>nameWrap.getTop()-topBarsH){
			titleTransY=Math.max(0f, titleTransY-(scrollY-(nameWrap.getTop()-topBarsH)));
		}
		if(toolbarTitleView!=null){
			toolbarTitleView.setTranslationY(titleTransY);
			toolbarSubtitleView.setTranslationY(titleTransY);
		}
		if(currentPhotoViewer!=null){
			currentPhotoViewer.offsetView(0, oldScrollY-scrollY);
		}

		if(GlobalUserPreferences.enableFabAutoHide){
			int dy = scrollY - oldScrollY;

			if (dy > 0 && fab.getVisibility() == View.VISIBLE) {
				TranslateAnimation animate = new TranslateAnimation(
						0,
						0,
						0,
						fab.getHeight() * 2);
				animate.setDuration(300);
				fab.startAnimation(animate);
				fab.setVisibility(View.INVISIBLE);
				scrollDiff = 0;
			} else if (dy < 0 && fab.getVisibility() != View.VISIBLE) {
				if (scrollDiff > 400) {
					fab.setVisibility(View.VISIBLE);
					TranslateAnimation animate = new TranslateAnimation(
							0,
							0,
							fab.getHeight() * 2,
							0);
					animate.setDuration(300);
					fab.startAnimation(animate);
					scrollDiff = 0;
				} else {
					scrollDiff += Math.abs(dy);
				}
			}
		}
	}

	private Fragment getFragmentForPage(int page){
		return switch(page){
			case 0 -> postsFragment;
			case 1 -> postsWithRepliesFragment;
			case 2 -> pinnedPostsFragment;
			case 3 -> mediaFragment;
//			case 4 -> aboutFragment;
			default -> throw new IllegalStateException();
		};
	}

	private RecyclerView getScrollableRecyclerView(){
		return getFragmentForPage(pager.getCurrentItem()).getView().findViewById(R.id.list);
	}

	private void onActionButtonClick(View v){
		if(isOwnProfile){
			if(!isInEditMode)
				loadAccountInfoAndEnterEditMode();
			else
				saveAndExitEditMode();
		}else{
			UiUtils.performAccountAction(getActivity(), account, accountID, relationship, actionButton, this::setActionProgressVisible, this::updateRelationship);
		}
	}

	private boolean onActionButtonLongClick(View v) {
		if (isOwnProfile || AccountSessionManager.getInstance().getLoggedInAccounts().size() < 2) return false;
		UiUtils.pickAccount(getActivity(), accountID, R.string.sk_follow_as, R.drawable.ic_fluent_person_add_28_regular, session -> {
			UiUtils.lookupAccount(getActivity(), account, session.getID(), accountID, acc -> {
				if (acc == null) return;
				new SetAccountFollowed(acc.id, true, true).setCallback(new Callback<>() {
					@Override
					public void onSuccess(Relationship relationship) {
						Toast.makeText(
								getActivity(),
								getString(R.string.sk_followed_as, session.self.getShortUsername()),
								Toast.LENGTH_SHORT
						).show();
					}

					@Override
					public void onError(ErrorResponse error) {
						error.showToast(getActivity());
					}
				}).exec(session.getID());
			});
		}, null);
		return true;
	}

	private void setActionProgressVisible(boolean visible){
		actionButton.setTextVisible(!visible);
		actionProgress.setVisibility(visible ? View.VISIBLE : View.GONE);
		actionButton.setClickable(!visible);
	}

	private void setNotifyProgressVisible(boolean visible){
		notifyButton.setTextVisible(!visible);
		notifyProgress.setVisibility(visible ? View.VISIBLE : View.GONE);
		notifyButton.setClickable(!visible);
	}

	private void loadAccountInfoAndEnterEditMode(){
		if(editModeLoading)
			return;
		editModeLoading=true;
		setActionProgressVisible(true);
		new GetOwnAccount()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						editModeLoading=false;
						if (getActivity() == null) return;
						enterEditMode(result);
						setActionProgressVisible(false);
					}

					@Override
					public void onError(ErrorResponse error){
						editModeLoading=false;
						if (getActivity() == null) return;
						error.showToast(getActivity());
						setActionProgressVisible(false);
					}
				})
				.exec(accountID);
	}

	private void enterEditMode(Account account){
		if(isInEditMode)
			throw new IllegalStateException();
		isInEditMode=true;
		invalidateOptionsMenu();
		pager.setUserInputEnabled(false);
		actionButton.setText(R.string.done);
		ArrayList<Animator> animators=new ArrayList<>();
		Drawable overlay=getResources().getDrawable(R.drawable.edit_avatar_overlay, getActivity().getTheme()).mutate();
		avatar.setForeground(overlay);
		animators.add(ObjectAnimator.ofInt(overlay, "alpha", 0, 255));

		nameWrap.setVisibility(View.GONE);
		nameEdit.setVisibility(View.VISIBLE);
		nameEdit.setText(account.displayName);
		RelativeLayout.LayoutParams lp=(RelativeLayout.LayoutParams) username.getLayoutParams();
		lp.addRule(RelativeLayout.BELOW, R.id.name_edit);
		username.getParent().requestLayout();
		animators.add(ObjectAnimator.ofFloat(nameEdit, View.ALPHA, 0f, 1f));

		bioEdit.setVisibility(View.VISIBLE);
		bioEdit.setText(account.source.note);
		animators.add(ObjectAnimator.ofFloat(bioEdit, View.ALPHA, 0f, 1f));
		animators.add(ObjectAnimator.ofFloat(bio, View.ALPHA, 0f));
		profileCounters.setVisibility(View.GONE);
		pager.setVisibility(View.GONE);
		tabbar.setVisibility(View.GONE);

		AnimatorSet set=new AnimatorSet();
		set.playTogether(animators);
		set.setDuration(300);
		set.setInterpolator(CubicBezierInterpolator.DEFAULT);
		set.start();

//		aboutFragment.enterEditMode(account.source.fields);

		V.setVisibilityAnimated(fab, View.GONE);
		metadataListData=account.source.fields;
		adapter.notifyDataSetChanged();
		dragHelper.attachToRecyclerView(list);
	}

	private void exitEditMode(){
		if(!isInEditMode)
			throw new IllegalStateException();
		isInEditMode=false;

		invalidateOptionsMenu();
		ArrayList<Animator> animators=new ArrayList<>();
		actionButton.setText(R.string.edit_profile);
		animators.add(ObjectAnimator.ofInt(avatar.getForeground(), "alpha", 0));
		animators.add(ObjectAnimator.ofFloat(nameEdit, View.ALPHA, 0f));
		animators.add(ObjectAnimator.ofFloat(bioEdit, View.ALPHA, 0f));
		animators.add(ObjectAnimator.ofFloat(bio, View.ALPHA, 1f));
		profileCounters.setVisibility(View.VISIBLE);
		pager.setVisibility(View.VISIBLE);
		tabbar.setVisibility(View.VISIBLE);
		V.setVisibilityAnimated(nameWrap, View.VISIBLE);

		AnimatorSet set=new AnimatorSet();
		set.playTogether(animators);
		set.setDuration(200);
		set.setInterpolator(CubicBezierInterpolator.DEFAULT);
		set.addListener(new AnimatorListenerAdapter(){
			@Override
			public void onAnimationEnd(Animator animation){
				pager.setUserInputEnabled(true);
				nameEdit.setVisibility(View.GONE);
				bioEdit.setVisibility(View.GONE);
				RelativeLayout.LayoutParams lp=(RelativeLayout.LayoutParams) username.getLayoutParams();
				lp.addRule(RelativeLayout.BELOW, R.id.name_wrap);
				username.getParent().requestLayout();
				avatar.setForeground(null);
				scrollToTop();
			}
		});
		set.start();

		InputMethodManager imm=getActivity().getSystemService(InputMethodManager.class);
		imm.hideSoftInputFromWindow(content.getWindowToken(), 0);
		V.setVisibilityAnimated(fab, View.VISIBLE);
		bindHeaderView();
	}

	private void saveAndExitEditMode(){
		if(!isInEditMode)
			throw new IllegalStateException();
		setActionProgressVisible(true);
		new UpdateAccountCredentials(nameEdit.getText().toString(), bioEdit.getText().toString(), editNewAvatar, editNewCover, metadataListData)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						account=result;
						AccountSessionManager.getInstance().updateAccountInfo(accountID, account);
						if (getActivity() == null) return;
						exitEditMode();
						setActionProgressVisible(false);
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
						setActionProgressVisible(false);
					}
				})
				.exec(accountID);
	}

	private void confirmToggleMuted(){
		UiUtils.confirmToggleMuteUser(getActivity(), accountID, account, relationship.muting, this::updateRelationship);
	}

	private void confirmToggleBlocked(){
		UiUtils.confirmToggleBlockUser(getActivity(), accountID, account, relationship.blocking, this::updateRelationship);
	}

	private void confirmSoftBlockUser(){
		UiUtils.confirmSoftBlockUser(getActivity(), accountID, account, this::updateRelationship);
	}

	private void updateRelationship(Relationship r){
		relationship=r;
		updateRelationship();
	}

	@Override
	public boolean onBackPressed(){
		if(noteEdit.hasFocus()) {
			savePrivateNote();
		}
		if(isInEditMode){
			exitEditMode();
			return true;
		}
		return false;
	}

	private List<Attachment> createFakeAttachments(String url, Drawable drawable){
		Attachment att=new Attachment();
		att.type=Attachment.Type.IMAGE;
		att.url=url;
		att.meta=new Attachment.Metadata();
		att.meta.width=drawable.getIntrinsicWidth();
		att.meta.height=drawable.getIntrinsicHeight();
		return Collections.singletonList(att);
	}

	private void onNotifyButtonClick(View v) {
		UiUtils.performToggleAccountNotifications(getActivity(), account, accountID, relationship, actionButton, this::setNotifyProgressVisible, this::updateRelationship);
	}

	private void onAvatarClick(View v){
		if(isInEditMode){
			startImagePicker(AVATAR_RESULT);
		}else{
			Drawable ava=avatar.getDrawable();
			if(ava==null)
				return;
			int radius=V.dp(25);
			currentPhotoViewer=new PhotoViewer(getActivity(), createFakeAttachments(account.avatar, ava), 0,
					new SingleImagePhotoViewerListener(avatar, avatarBorder, new int[]{radius, radius, radius, radius}, this, ()->currentPhotoViewer=null, ()->ava, null, null));
		}
	}

	private void onCoverClick(View v){
		if(isInEditMode){
			startImagePicker(COVER_RESULT);
		}else{
			Drawable drawable=cover.getDrawable();
			if(drawable==null || drawable instanceof ColorDrawable)
				return;
			currentPhotoViewer=new PhotoViewer(getActivity(), createFakeAttachments(account.header, drawable), 0,
					new SingleImagePhotoViewerListener(cover, cover, null, this, ()->currentPhotoViewer=null, ()->drawable, ()->avatarBorder.setTranslationZ(2), ()->avatarBorder.setTranslationZ(0)));
		}
	}

	private void onFabClick(View v){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		if(getPrefilledText() != null) args.putString("prefilledText", getPrefilledText());
		Nav.go(getActivity(), ComposeFragment.class, args);
	}

	private void startImagePicker(int requestCode){
		Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		startActivityForResult(intent, requestCode);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(resultCode==Activity.RESULT_OK){
			if(requestCode==AVATAR_RESULT){
				editNewAvatar=data.getData();
				ViewImageLoader.load(avatar, null, new UrlImageLoaderRequest(editNewAvatar, V.dp(100), V.dp(100)));
			}else if(requestCode==COVER_RESULT){
				editNewCover=data.getData();
				ViewImageLoader.load(cover, null, new UrlImageLoaderRequest(editNewCover, V.dp(1000), V.dp(1000)));
			}
		}
	}

	@Override
	public void scrollToTop(){
		getScrollableRecyclerView().scrollToPosition(0);
		scrollView.smoothScrollTo(0, 0);
	}

	@Override
	public boolean isScrolledToTop() {
		return list.getChildAt(0).getTop() == 0;
	}

	private void onFollowersOrFollowingClick(View v){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("targetAccount", Parcels.wrap(account));
		Class<? extends Fragment> cls;
		if(v.getId()==R.id.followers_btn)
			cls=FollowerListFragment.class;
		else if(v.getId()==R.id.following_btn)
			cls=FollowingListFragment.class;
		else
			return;
		Nav.go(getActivity(), cls, args);
	}

	private class ProfilePagerAdapter extends RecyclerView.Adapter<SimpleViewHolder>{
		@NonNull
		@Override
		public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			FrameLayout view=tabViews[viewType];
			if (view.getParent() != null) ((ViewGroup)view.getParent()).removeView(view);
			view.setVisibility(View.VISIBLE);
			view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			return new SimpleViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position){
			Fragment fragment=getFragmentForPage(position);
			if(!fragment.isAdded()){
				getChildFragmentManager().beginTransaction().add(holder.itemView.getId(), fragment).commit();
				holder.itemView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
					@Override
					public boolean onPreDraw(){
						if(fragment.isAdded()){
							holder.itemView.getViewTreeObserver().removeOnPreDrawListener(this);
							applyChildWindowInsets();
						}
						return true;
					}
				});
			}
		}

		@Override
		public int getItemCount(){
			return loaded ? tabViews.length : 0;
		}

		@Override
		public int getItemViewType(int position){
			return position;
		}
	}

	// from ProfileAboutFragment
	public void setFields(ArrayList<AccountField> fields){
		metadataListData=fields;
		if (isInEditMode) {
			isInEditMode=false;
			dragHelper.attachToRecyclerView(null);
		}
		if (adapter != null) adapter.notifyDataSetChanged();
	}

	private class MetadataAdapter extends UsableRecyclerView.Adapter<BaseViewHolder> implements ImageLoaderRecyclerAdapter {
		public MetadataAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return switch(viewType){
				case 0 -> new AboutViewHolder();
				case 1 -> new EditableAboutViewHolder();
				case 2 -> new AddRowViewHolder();
				default -> throw new IllegalStateException("Unexpected value: "+viewType);
			};
		}

		@Override
		public void onBindViewHolder(BaseViewHolder holder, int position){
			if(position<metadataListData.size()){
				holder.bind(metadataListData.get(position));
			}else{
				holder.bind(null);
			}
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getItemCount(){
			if(isInEditMode){
				int size=metadataListData.size();
				if(size<MAX_FIELDS)
					size++;
				return size;
			}
			return metadataListData.size();
		}

		@Override
		public int getItemViewType(int position){
			if(isInEditMode){
				return position==metadataListData.size() ? 2 : 1;
			}
			return 0;
		}

		@Override
		public int getImageCountForItem(int position){
			return isInEditMode || metadataListData.get(position).emojiRequests==null
					? 0 : metadataListData.get(position).emojiRequests.size();
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			return metadataListData.get(position).emojiRequests.get(image);
		}
	}

	private abstract class BaseViewHolder extends BindableViewHolder<AccountField> {
		public BaseViewHolder(int layout){
			super(getActivity(), layout, list);
		}
	}

	private class AboutViewHolder extends BaseViewHolder implements ImageLoaderViewHolder {
		private TextView title;
		private LinkedTextView value;

		public AboutViewHolder(){
			super(R.layout.item_profile_about);
			title=findViewById(R.id.title);
			value=findViewById(R.id.value);
		}

		@Override
		public void onBind(AccountField item){
			title.setText(item.parsedName);
			value.setText(item.parsedValue);
			if(item.verifiedAt!=null){
				int textColor=UiUtils.isDarkTheme() ? 0xFF89bb9c : 0xFF5b8e63;
				value.setTextColor(textColor);
				value.setLinkTextColor(textColor);
				Drawable check=getResources().getDrawable(R.drawable.ic_fluent_checkmark_24_regular, getActivity().getTheme()).mutate();
				check.setTint(textColor);
				value.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, check, null);
			}else{
				value.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
				value.setLinkTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.colorAccent));
				value.setCompoundDrawables(null, null, null, null);
			}
		}

		@Override
		public void setImage(int index, Drawable image){
			CustomEmojiSpan span=index>=item.nameEmojis.length ? item.valueEmojis[index-item.nameEmojis.length] : item.nameEmojis[index];
			span.setDrawable(image);
			title.invalidate();
			value.invalidate();
		}

		@Override
		public void clearImage(int index){
			setImage(index, null);
		}
	}

	private class EditableAboutViewHolder extends BaseViewHolder {
		private EditText title;
		private boolean titleHasFocus, valueHasFocus;
		private EditText value;

		public EditableAboutViewHolder(){
			super(R.layout.item_profile_about_editable);
			title=findViewById(R.id.title);
			value=findViewById(R.id.value);
			findViewById(R.id.dragger_thingy).setOnLongClickListener(v->{
				dragHelper.startDrag(this);
				return true;
			});
			title.addTextChangedListener(new SimpleTextWatcher(e->item.name=e.toString()));
			title.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					titleHasFocus = hasFocus;
				}
			});
			value.addTextChangedListener(new SimpleTextWatcher(e->item.value=e.toString()));
			value.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					valueHasFocus = hasFocus;
				}
			});
			findViewById(R.id.remove_row_btn).setOnClickListener(this::onRemoveRowClick);
		}

		@Override
		public void onBind(AccountField item){
			title.setText(item.name);
			value.setText(item.value);
		}

		private void onRemoveRowClick(View v){
			if(titleHasFocus || valueHasFocus){
				return;
			}

			int pos=getAbsoluteAdapterPosition();
			metadataListData.remove(pos);
			adapter.notifyItemRemoved(pos);
			for(int i=0;i<list.getChildCount();i++){
				BaseViewHolder vh=(BaseViewHolder) list.getChildViewHolder(list.getChildAt(i));
				vh.rebind();
			}
		}
	}

	private class AddRowViewHolder extends BaseViewHolder implements UsableRecyclerView.Clickable{
		public AddRowViewHolder(){
			super(R.layout.item_profile_about_add_row);
		}

		@Override
		public void onClick(){
			metadataListData.add(new AccountField());
			if(metadataListData.size()==MAX_FIELDS){ // replace this row with new row
				adapter.notifyItemChanged(metadataListData.size()-1);
			}else{
				adapter.notifyItemInserted(metadataListData.size()-1);
				rebind();
			}
		}

		@Override
		public void onBind(AccountField item) {}
	}

	private class ReorderCallback extends ItemTouchHelper.SimpleCallback{
		public ReorderCallback(){
			super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
		}

		@Override
		public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target){
			if(target instanceof AddRowViewHolder)
				return false;
			int fromPosition=viewHolder.getAbsoluteAdapterPosition();
			int toPosition=target.getAbsoluteAdapterPosition();
			if (fromPosition<toPosition) {
				for (int i=fromPosition;i<toPosition;i++) {
					Collections.swap(metadataListData, i, i+1);
				}
			} else {
				for (int i=fromPosition;i>toPosition;i--) {
					Collections.swap(metadataListData, i, i-1);
				}
			}
			adapter.notifyItemMoved(fromPosition, toPosition);
			((BindableViewHolder)viewHolder).rebind();
			((BindableViewHolder)target).rebind();
			return true;
		}

		@Override
		public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction){

		}

		@Override
		public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState){
			super.onSelectedChanged(viewHolder, actionState);
			if(actionState==ItemTouchHelper.ACTION_STATE_DRAG){
				viewHolder.itemView.setTag(me.grishka.appkit.R.id.item_touch_helper_previous_elevation, viewHolder.itemView.getElevation()); // prevents the default behavior of changing elevation in onDraw()
				viewHolder.itemView.animate().translationZ(V.dp(1)).setDuration(200).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
				draggedViewHolder=viewHolder;
			}
		}

		@Override
		public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder){
			super.clearView(recyclerView, viewHolder);
			viewHolder.itemView.animate().translationZ(0).setDuration(100).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
			draggedViewHolder=null;
		}

		@Override
		public boolean isLongPressDragEnabled(){
			return false;
		}
	}
}
