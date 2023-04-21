package org.joinmastodon.android.fragments.settings;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.DomainManager;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.DomainDisplay;
import org.joinmastodon.android.fragments.MastodonToolbarFragment;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.ArrayList;
import java.util.function.Consumer;

import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public abstract class SettingsBaseFragment extends MastodonToolbarFragment implements DomainDisplay {
	protected View view;
	private UsableRecyclerView list;
	private ArrayList<Item> items=new ArrayList<>();
	private String accountID;

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
		TEXT,
		FOOTER,
		BUTTON,
		SMALL_TEXT,
	}


	private class SettingsAdapter extends RecyclerView.Adapter<BindableViewHolder<Item>>{

		@NonNull
		@Override
		public BindableViewHolder<Item> onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			//noinspection unchecked
			return (BindableViewHolder<Item>) switch(Type.values()[viewType]){
				case HEADER -> new HeaderViewHolder();
				case SWITCH -> new SwitchViewHolder();
				case TEXT -> new TextViewHolder();
				case FOOTER -> new FooterViewHolder();
				case BUTTON -> new ButtonViewHolder();
				case SMALL_TEXT -> new SmallTextViewHolder();
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
}
