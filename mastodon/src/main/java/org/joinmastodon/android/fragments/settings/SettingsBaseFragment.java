package org.joinmastodon.android.fragments.settings;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.DomainManager;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.DomainDisplay;
import org.joinmastodon.android.fragments.MastodonToolbarFragment;
import org.joinmastodon.android.model.PushNotification;
import org.joinmastodon.android.model.PushSubscription;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.updater.GithubSelfUpdater;

import java.util.ArrayList;
import java.util.function.Consumer;

import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public abstract class SettingsBaseFragment extends MastodonToolbarFragment implements DomainDisplay {
	protected View view;
	private UsableRecyclerView list;

	private ImageView themeTransitionWindowView;

	private SettingsBaseFragment.NotificationPolicyItem notificationPolicyItem;

	private PushSubscription pushSubscription;
	private ArrayList<Item> items=new ArrayList<>();
	private String accountID;

	private boolean needUpdateNotificationSettings;

	public abstract void addItems(ArrayList<Item> items);

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			setRetainInstance(true);
		setTitle(R.string.settings);

		accountID=getArguments().getString("account");
		AccountSession session = AccountSessionManager.getInstance().getAccount(accountID);
		DomainManager.getInstance().setCurrentDomain(session.domain + "/settings");

		addItems(items);
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		list=new UsableRecyclerView(getActivity());
		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		list.setAdapter(new SettingsAdapter());
		list.setBackgroundColor(UiUtils.getThemeColor(getActivity(), android.R.attr.colorBackground));
		list.setPadding(0, V.dp(16), 0, V.dp(12));
		list.setClipToPadding(false);
		list.addItemDecoration(new RecyclerView.ItemDecoration(){
			@Override
			public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				// Add 32dp gaps between sections
				RecyclerView.ViewHolder holder=parent.getChildViewHolder(view);
				if((holder instanceof HeaderViewHolder || holder instanceof FooterViewHolder) && holder.getAbsoluteAdapterPosition()>1)
					outRect.top=V.dp(32);
			}
		});
		return list;
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=29 && insets.getTappableElementInsets().bottom==0){
			list.setPadding(0, V.dp(16), 0, V.dp(12)+insets.getSystemWindowInsetBottom());
			insets=insets.inset(0, 0, 0, insets.getSystemWindowInsetBottom());
		}
		super.onApplyWindowInsets(insets);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		this.view = view;
	}


	private static abstract class Item{
		public abstract int getViewType();
	}

	private class HeaderItem extends Item{
		private String text;

		public HeaderItem(@StringRes int text){
			this.text=getString(text);
		}

		public HeaderItem(String text){
			this.text=text;
		}

		@Override
		public int getViewType(){
			return Type.HEADER.ordinal();
		}
	}

	private class SwitchItem extends Item{
		private String text;
		private int icon;
		private boolean checked;
		private Consumer<SwitchItem> onChanged;
		private boolean enabled=true;

		public SwitchItem(@StringRes int text, @DrawableRes int icon, boolean checked, Consumer<SwitchItem> onChanged){
			this.text=getString(text);
			this.icon=icon;
			this.checked=checked;
			this.onChanged=onChanged;
		}

		public SwitchItem(@StringRes int text, @DrawableRes int icon, boolean checked, Consumer<SwitchItem> onChanged, boolean enabled){
			this.text=getString(text);
			this.icon=icon;
			this.checked=checked;
			this.onChanged=onChanged;
			this.enabled=enabled;
		}

		@Override
		public int getViewType(){
			return Type.SWITCH.ordinal();
		}
	}

	private class UpdateItem extends SettingsBaseFragment.Item {

		@Override
		public int getViewType(){
			return Type.UPDATER.ordinal();
		}
	}

	private static class ThemeItem extends SettingsBaseFragment.Item {

		@Override
		public int getViewType(){
			return Type.THEME.ordinal();
		}
	}

	private static class NotificationPolicyItem extends SettingsBaseFragment.Item {

		@Override
		public int getViewType(){
			return Type.NOTIFICATION_POLICY.ordinal();
		}
	}


	public class ButtonItem extends Item{
		private int text;
		private int icon;
		private Consumer<Button> buttonConsumer;

		public ButtonItem(@StringRes int text, @DrawableRes int icon, Consumer<Button> buttonConsumer) {
			this.text = text;
			this.icon = icon;
			this.buttonConsumer = buttonConsumer;
		}

		@Override
		public int getViewType(){
			return Type.BUTTON.ordinal();
		}
	}

	private class SmallTextItem extends Item {
		private String text;

		public SmallTextItem(String text) {
			this.text = text;
		}

		@Override
		public int getViewType() {
			return Type.SMALL_TEXT.ordinal();
		}
	}

	private class TextItem extends Item{
		private String text;
		private String secondaryText;
		private Runnable onClick;
		private boolean loading;
		private int icon;

		public TextItem(@StringRes int text, Runnable onClick) {
			this(text, null, onClick, false, 0);
		}

		public TextItem(@StringRes int text, Runnable onClick, @DrawableRes int icon) {
			this(text, null, onClick, false, icon);
		}

		public TextItem(@StringRes int text, String secondaryText, Runnable onClick, @DrawableRes int icon) {
			this(text, secondaryText, onClick, false, icon);
		}

		public TextItem(@StringRes int text, String secondaryText, Runnable onClick, boolean loading, @DrawableRes int icon){
			this.text=getString(text);
			this.onClick=onClick;
			this.loading=loading;
			this.icon=icon;
			this.secondaryText = secondaryText;
		}

		public TextItem(String text, Runnable onClick){
			this.text=text;
			this.onClick=onClick;
		}

		public TextItem(String text, Runnable onClick, @DrawableRes int icon){
			this.text=text;
			this.onClick=onClick;
			this.icon=icon;
		}

		@Override
		public int getViewType(){
			return Type.TEXT.ordinal();
		}
	}

	private class FooterItem extends Item{
		private String text;
		private Runnable onClick;

		public FooterItem(String text, Runnable onClick){
			this.text=text;
			this.onClick=onClick;
		}

		@Override
		public int getViewType(){
			return Type.FOOTER.ordinal();
		}
	}

	public enum Type{
		HEADER,
		SWITCH,
		THEME,
		TEXT,
		NOTIFICATION_POLICY,
		FOOTER,
		BUTTON,
		SMALL_TEXT,
		UPDATER
	}


	private class SettingsAdapter extends RecyclerView.Adapter<BindableViewHolder<Item>>{

		@NonNull
		@Override
		public BindableViewHolder<Item> onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			//noinspection unchecked
			return (BindableViewHolder<Item>) switch(Type.values()[viewType]){
				case HEADER -> new HeaderViewHolder();
				case SWITCH -> new SwitchViewHolder();
				case THEME -> new ThemeViewHolder();
				case TEXT -> new TextViewHolder();
				case NOTIFICATION_POLICY -> new NotificationPolicyViewHolder();
				case FOOTER -> new FooterViewHolder();
				case BUTTON -> new ButtonViewHolder();
				case SMALL_TEXT -> new SmallTextViewHolder();
				case UPDATER -> new UpdateViewHolder();
			};
		}

		@Override
		public void onBindViewHolder(@NonNull BindableViewHolder<Item> holder, int position){
			holder.bind(items.get(position));
		}

		@Override
		public int getItemCount(){
			return items.size();
		}

		@Override
		public int getItemViewType(int position){
			return items.get(position).getViewType();
		}
	}

	private class HeaderViewHolder extends BindableViewHolder<HeaderItem>{
		private final TextView text;
		public HeaderViewHolder(){
			super(getActivity(), R.layout.item_settings_header, list);
			text=(TextView) itemView;
		}

		@Override
		public void onBind(HeaderItem item){
			text.setText(item.text);
		}
	}

	private void onThemePreferenceClick(GlobalUserPreferences.ThemePreference theme){
		GlobalUserPreferences.theme=theme;
		GlobalUserPreferences.save();
		restartActivityToApplyNewTheme();
	}

	private void restartActivityToApplyNewTheme(){
		// Calling activity.recreate() causes a black screen for like half a second.
		// So, let's take a screenshot and overlay it on top to create the illusion of a smoother transition.
		// As a bonus, we can fade it out to make it even smoother.
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
			View activityDecorView=getActivity().getWindow().getDecorView();
			Bitmap bitmap=Bitmap.createBitmap(activityDecorView.getWidth(), activityDecorView.getHeight(), Bitmap.Config.ARGB_8888);
			activityDecorView.draw(new Canvas(bitmap));
			themeTransitionWindowView=new ImageView(MastodonApp.context);
			themeTransitionWindowView.setImageBitmap(bitmap);
			WindowManager.LayoutParams lp=new WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_APPLICATION);
			lp.flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
					WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
			lp.systemUiVisibility=View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
			lp.systemUiVisibility|=(activityDecorView.getWindowSystemUiVisibility() & (View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR));
			lp.width=lp.height=WindowManager.LayoutParams.MATCH_PARENT;
			lp.token=getActivity().getWindow().getAttributes().token;
			lp.windowAnimations=R.style.window_fade_out;
			MastodonApp.context.getSystemService(WindowManager.class).addView(themeTransitionWindowView, lp);
		}
		getActivity().recreate();
	}

	private class NotificationPolicyViewHolder extends BindableViewHolder<NotificationPolicyItem>{
		private final Button button;
		private final PopupMenu popupMenu;

		@SuppressLint("ClickableViewAccessibility")
		public NotificationPolicyViewHolder(){
			super(getActivity(), R.layout.item_settings_notification_policy, list);
			button=findViewById(R.id.button);
			popupMenu=new PopupMenu(getActivity(), button, Gravity.CENTER_HORIZONTAL);
			popupMenu.inflate(R.menu.notification_policy);
			popupMenu.setOnMenuItemClickListener(item->{
				PushSubscription.Policy policy;
				int id=item.getItemId();
				if(id==R.id.notify_anyone)
					policy=PushSubscription.Policy.ALL;
				else if(id==R.id.notify_followed)
					policy=PushSubscription.Policy.FOLLOWED;
				else if(id==R.id.notify_follower)
					policy=PushSubscription.Policy.FOLLOWER;
				else if(id==R.id.notify_none)
					policy=PushSubscription.Policy.NONE;
				else
					return false;
				onNotificationsPolicyChanged(policy);
				return true;
			});
			UiUtils.enablePopupMenuIcons(getActivity(), popupMenu);
			button.setOnTouchListener(popupMenu.getDragToOpenListener());
			button.setOnClickListener(v->popupMenu.show());
		}

		@Override
		public void onBind(SettingsBaseFragment.NotificationPolicyItem item){
			button.setText(switch(getPushSubscription().policy){
				case ALL -> R.string.notify_anyone;
				case FOLLOWED -> R.string.notify_followed;
				case FOLLOWER -> R.string.notify_follower;
				case NONE -> R.string.notify_none;
			});
		}
	}

	private void onNotificationsPolicyChanged(PushSubscription.Policy policy){
		PushSubscription subscription=getPushSubscription();
		PushSubscription.Policy prevPolicy=subscription.policy;
		if(prevPolicy==policy)
			return;
		subscription.policy=policy;
		int index=items.indexOf(notificationPolicyItem);
		RecyclerView.ViewHolder policyHolder=list.findViewHolderForAdapterPosition(index);
		if(policyHolder!=null){
			((SettingsBaseFragment.NotificationPolicyViewHolder)policyHolder).rebind();
		}else{
			list.getAdapter().notifyItemChanged(index);
		}
		if((prevPolicy==PushSubscription.Policy.NONE)!=(policy==PushSubscription.Policy.NONE)){
			boolean newState=policy!=PushSubscription.Policy.NONE;
			for(PushNotification.Type value : PushNotification.Type.values()){
				onNotificationsChanged(value, newState);
			}
			index++;
			while(items.get(index) instanceof SettingsBaseFragment.SwitchItem si){
				si.enabled=si.checked=newState;
				RecyclerView.ViewHolder holder=list.findViewHolderForAdapterPosition(index);
				if(holder!=null)
					((BindableViewHolder<?>)holder).rebind();
				else
					list.getAdapter().notifyItemChanged(index);
				index++;
			}
		}
		needUpdateNotificationSettings=true;
	}

	private void onNotificationsChanged(PushNotification.Type type, boolean enabled){
		PushSubscription subscription=getPushSubscription();
		switch(type){
			case FAVORITE -> subscription.alerts.favourite=enabled;
			case FOLLOW -> subscription.alerts.follow=enabled;
			case REBLOG -> subscription.alerts.reblog=enabled;
			case MENTION -> subscription.alerts.mention=enabled;
			case POLL -> subscription.alerts.poll=enabled;
			case STATUS -> subscription.alerts.status=enabled;
			case UPDATE -> subscription.alerts.update=enabled;
		}
		needUpdateNotificationSettings=true;
	}

	private PushSubscription getPushSubscription(){
		if(pushSubscription!=null)
			return pushSubscription;
		AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
		if(session.pushSubscription==null){
			pushSubscription=new PushSubscription();
			pushSubscription.alerts=PushSubscription.Alerts.ofAll();
		}else{
			pushSubscription=session.pushSubscription.clone();
		}
		return pushSubscription;
	}

	private class SwitchViewHolder extends BindableViewHolder<SwitchItem> implements UsableRecyclerView.DisableableClickable{
		private final TextView text;
		private final ImageView icon;
		private final Switch checkbox;

		public SwitchViewHolder(){
			super(getActivity(), R.layout.item_settings_switch, list);
			text=findViewById(R.id.text);
			icon=findViewById(R.id.icon);
			checkbox=findViewById(R.id.checkbox);
		}

		@Override
		public void onBind(SwitchItem item){
			text.setText(item.text);
			if (item.icon == 0) {
				icon.setVisibility(View.GONE);
			} else {
				icon.setVisibility(View.VISIBLE);
				icon.setImageResource(item.icon);
			}
			checkbox.setChecked(item.checked && item.enabled);
			checkbox.setEnabled(item.enabled);
		}

		@Override
		public void onClick(){
			item.checked=!item.checked;
			checkbox.setChecked(item.checked);
			item.onChanged.accept(item);
		}

		@Override
		public boolean isEnabled(){
			return item.enabled;
		}
	}

	private class ThemeViewHolder extends BindableViewHolder<ThemeItem>{
		private ThemeViewHolder.SubitemHolder autoHolder, lightHolder, darkHolder;

		public ThemeViewHolder(){
			super(getActivity(), R.layout.item_settings_theme, list);
			autoHolder=new ThemeViewHolder.SubitemHolder(findViewById(R.id.theme_auto));
			lightHolder=new ThemeViewHolder.SubitemHolder(findViewById(R.id.theme_light));
			darkHolder=new ThemeViewHolder.SubitemHolder(findViewById(R.id.theme_dark));
		}

		@Override
		public void onBind(SettingsBaseFragment.ThemeItem item){
			bindSubitems();
		}

		public void bindSubitems(){
			autoHolder.bind(R.string.theme_auto, GlobalUserPreferences.trueBlackTheme ? R.drawable.theme_auto_trueblack : R.drawable.theme_auto, GlobalUserPreferences.theme==GlobalUserPreferences.ThemePreference.AUTO);
			lightHolder.bind(R.string.theme_light, R.drawable.theme_light, GlobalUserPreferences.theme==GlobalUserPreferences.ThemePreference.LIGHT);
			darkHolder.bind(R.string.theme_dark, GlobalUserPreferences.trueBlackTheme ? R.drawable.theme_dark_trueblack : R.drawable.theme_dark, GlobalUserPreferences.theme==GlobalUserPreferences.ThemePreference.DARK);
		}

		private void onSubitemClick(View v){
			GlobalUserPreferences.ThemePreference pref;
			if(v.getId()==R.id.theme_auto)
				pref=GlobalUserPreferences.ThemePreference.AUTO;
			else if(v.getId()==R.id.theme_light)
				pref=GlobalUserPreferences.ThemePreference.LIGHT;
			else if(v.getId()==R.id.theme_dark)
				pref=GlobalUserPreferences.ThemePreference.DARK;
			else
				return;
			onThemePreferenceClick(pref);
		}

		private class SubitemHolder{
			public TextView text;
			public ImageView icon;
			public RadioButton checkbox;

			public SubitemHolder(View view){
				text=view.findViewById(R.id.text);
				icon=view.findViewById(R.id.icon);
				checkbox=view.findViewById(R.id.checkbox);
				view.setOnClickListener(ThemeViewHolder.this::onSubitemClick);

				icon.setClipToOutline(true);
				icon.setOutlineProvider(OutlineProviders.roundedRect(4));
			}

			public void bind(int text, int icon, boolean checked){
				this.text.setText(text);
				this.icon.setImageResource(icon);
				checkbox.setChecked(checked);
			}

			public void setChecked(boolean checked){
				checkbox.setChecked(checked);
			}
		}
	}

	private class ButtonViewHolder extends BindableViewHolder<ButtonItem>{
		private final Button button;
		private final ImageView icon;
		private final TextView text;

		@SuppressLint("ClickableViewAccessibility")
		public ButtonViewHolder(){
			super(getActivity(), R.layout.item_settings_button, list);
			text=findViewById(R.id.text);
			icon=findViewById(R.id.icon);
			button=findViewById(R.id.button);
		}

		@Override
		public void onBind(ButtonItem item){
			text.setText(item.text);
			icon.setImageResource(item.icon);
			item.buttonConsumer.accept(button);
		}
	}

	private class TextViewHolder extends BindableViewHolder<TextItem> implements UsableRecyclerView.Clickable{
		private final TextView text, secondaryText;
		private final ProgressBar progress;
		private final ImageView icon;

		public TextViewHolder(){
			super(getActivity(), R.layout.item_settings_text, list);
			text = itemView.findViewById(R.id.text);
			secondaryText = itemView.findViewById(R.id.secondary_text);
			progress = itemView.findViewById(R.id.progress);
			icon = itemView.findViewById(R.id.icon);
		}

		@Override
		public void onBind(TextItem item){
			icon.setVisibility(item.icon != 0 ? View.VISIBLE : View.GONE);
			secondaryText.setVisibility(item.secondaryText != null ? View.VISIBLE : View.GONE);

			text.setText(item.text);
			progress.animate().alpha(item.loading ? 1 : 0);
			icon.setImageResource(item.icon);
			secondaryText.setText(item.secondaryText);
		}

		@Override
		public void onClick(){
			item.onClick.run();
		}
	}

	private class SmallTextViewHolder extends BindableViewHolder<SmallTextItem> {
		private final TextView text;

		public SmallTextViewHolder(){
			super(getActivity(), R.layout.item_settings_text, list);
			text = itemView.findViewById(R.id.text);
			text.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
			text.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
			text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
			text.setPaddingRelative(text.getPaddingStart(), 0, text.getPaddingEnd(), text.getPaddingBottom());
		}

		@Override
		public void onBind(SmallTextItem item){
			text.setText(item.text);
		}
	}

	private class FooterViewHolder extends BindableViewHolder<FooterItem> implements UsableRecyclerView.Clickable{
		private final TextView text;
		public FooterViewHolder(){
			super(getActivity(), R.layout.item_settings_footer, list);
			text=(TextView) itemView;
		}

		@Override
		public void onBind(FooterItem item){
			text.setText(item.text);
		}

		@Override
		public void onClick(){
			item.onClick.run();
		}
	}

	private class UpdateViewHolder extends BindableViewHolder<UpdateItem>{

		private final TextView text, changelog;
		private final Button button;
		private final ImageButton cancelBtn;
		private final ProgressBar progress;

		private ObjectAnimator rotationAnimator;
		private Runnable progressUpdater=this::updateProgress;

		public UpdateViewHolder(){
			super(getActivity(), R.layout.item_settings_update, list);
			text=findViewById(R.id.text);
			changelog=findViewById(R.id.changelog);
			button=findViewById(R.id.button);
			cancelBtn=findViewById(R.id.cancel_btn);
			progress=findViewById(R.id.progress);
			button.setOnClickListener(v->{
				GithubSelfUpdater updater=GithubSelfUpdater.getInstance();
				switch(updater.getState()){
					case UPDATE_AVAILABLE -> updater.downloadUpdate();
					case DOWNLOADED -> updater.installUpdate(getActivity());
				}
			});
			cancelBtn.setOnClickListener(v->GithubSelfUpdater.getInstance().cancelDownload());
			rotationAnimator=ObjectAnimator.ofFloat(progress, View.ROTATION, 0f, 360f);
			rotationAnimator.setInterpolator(new LinearInterpolator());
			rotationAnimator.setDuration(1500);
			rotationAnimator.setRepeatCount(ObjectAnimator.INFINITE);
		}

		@Override
		public void onBind(SettingsBaseFragment.UpdateItem item){
			GithubSelfUpdater updater=GithubSelfUpdater.getInstance();
			GithubSelfUpdater.UpdateState state=updater.getState();
			if (state == GithubSelfUpdater.UpdateState.CHECKING) return;
			GithubSelfUpdater.UpdateInfo info=updater.getUpdateInfo();
			if(state!=GithubSelfUpdater.UpdateState.DOWNLOADED){
				text.setText(getString(R.string.mo_update_available, info.version));
				button.setText(getString(R.string.download_update, UiUtils.formatFileSize(getActivity(), info.size, false)));
			}else{
				text.setText(getString(R.string.mo_update_ready, info.version));
				button.setText(R.string.install_update);
			}
			if(state==GithubSelfUpdater.UpdateState.DOWNLOADING){
				rotationAnimator.start();
				button.setVisibility(View.INVISIBLE);
				cancelBtn.setVisibility(View.VISIBLE);
				progress.setVisibility(View.VISIBLE);
				updateProgress();
			}else{
				rotationAnimator.cancel();
				button.setVisibility(View.VISIBLE);
				cancelBtn.setVisibility(View.GONE);
				progress.setVisibility(View.GONE);
				progress.removeCallbacks(progressUpdater);
			}
			changelog.setText(info.changelog);
//			changelog.setText(getString(R.string.sk_changelog, info.changelog));
		}

		private void updateProgress(){
			GithubSelfUpdater updater=GithubSelfUpdater.getInstance();
			if(updater.getState()!=GithubSelfUpdater.UpdateState.DOWNLOADING)
				return;
			int value=Math.round(progress.getMax()*updater.getDownloadProgress());
			if(Build.VERSION.SDK_INT>=24)
				progress.setProgress(value, true);
			else
				progress.setProgress(value);
			progress.postDelayed(progressUpdater, 1000);
		}
	}
}
