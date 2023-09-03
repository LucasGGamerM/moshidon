package org.joinmastodon.android.fragments;

import static android.view.Menu.NONE;
import static com.hootsuite.nachos.terminator.ChipTerminatorHandler.BEHAVIOR_CHIPIFY_ALL;
import static org.joinmastodon.android.ui.utils.UiUtils.makeBackItem;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.hootsuite.nachos.NachoTextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.lists.GetLists;
import org.joinmastodon.android.api.requests.tags.GetFollowedHashtags;
import org.joinmastodon.android.api.session.AccountLocalPreferences;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.model.ListTimeline;
import org.joinmastodon.android.model.TimelineDefinition;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.views.UsableRecyclerView;

public class EditTimelinesFragment extends MastodonRecyclerFragment<TimelineDefinition> implements ScrollableToTop {
    private String accountID;
    private TimelinesAdapter adapter;
    private final ItemTouchHelper itemTouchHelper;
    private Menu optionsMenu;
    private boolean updated;
    private final Map<MenuItem, TimelineDefinition> timelineByMenuItem = new HashMap<>();
    private final List<ListTimeline> listTimelines = new ArrayList<>();
    private final List<Hashtag> hashtags = new ArrayList<>();
    private MenuItem addHashtagItem;

    public EditTimelinesFragment() {
        super(10);
        ItemTouchHelper.SimpleCallback itemTouchCallback = new ItemTouchHelperCallback() ;
        itemTouchHelper = new ItemTouchHelper(itemTouchCallback);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setTitle(R.string.sk_timelines);
        accountID = getArguments().getString("account");

        new GetLists().setCallback(new Callback<>() {
            @Override
            public void onSuccess(List<ListTimeline> result) {
                listTimelines.addAll(result);
                updateOptionsMenu();
            }

            @Override
            public void onError(ErrorResponse error) {
                error.showToast(getContext());
            }
        }).exec(accountID);

        new GetFollowedHashtags().setCallback(new Callback<>() {
            @Override
            public void onSuccess(HeaderPaginationList<Hashtag> result) {
                hashtags.addAll(result);
                updateOptionsMenu();
            }

            @Override
            public void onError(ErrorResponse error) {
                error.showToast(getContext());
            }
        }).exec(accountID);
    }

    @Override
    protected void onShown(){
        super.onShown();
        if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading) loadData();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        itemTouchHelper.attachToRecyclerView(list);
        refreshLayout.setEnabled(false);
        list.addItemDecoration(new DividerItemDecoration(getActivity(), R.attr.colorM3OutlineVariant, 0.5f, 56, 16));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.optionsMenu = menu;
        updateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_back) {
            updateOptionsMenu();
            optionsMenu.performIdentifierAction(R.id.menu_add_timeline, 0);
            return true;
        }
        TimelineDefinition tl = timelineByMenuItem.get(item);
        if (tl != null) {
            addTimeline(tl);
        } else if (item == addHashtagItem) {
            makeTimelineEditor(null, (hashtag) -> {
                if (hashtag != null) addTimeline(hashtag);
            }, null);
        }
        return true;
    }

    private void addTimeline(TimelineDefinition tl) {
        data.add(tl.copy());
        adapter.notifyItemInserted(data.size());
        saveTimelines();
        updateOptionsMenu();
    }

    private void addTimelineToOptions(TimelineDefinition tl, Menu menu) {
        if (data.contains(tl)) return;
        MenuItem item = addOptionsItem(menu, tl.getTitle(getContext()), tl.getIcon().iconRes);
        timelineByMenuItem.put(item, tl);
    }

    private MenuItem addOptionsItem(Menu menu, String name, @DrawableRes int icon) {
        MenuItem item = menu.add(0, View.generateViewId(), Menu.NONE, name);
        item.setIcon(icon);
        return item;
    }

    private void updateOptionsMenu() {
        if (getActivity() == null) return;
        optionsMenu.clear();
        timelineByMenuItem.clear();

        SubMenu menu = optionsMenu.addSubMenu(0, R.id.menu_add_timeline, NONE, R.string.sk_timelines_add);
        menu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.getItem().setIcon(R.drawable.ic_fluent_add_24_regular);

        SubMenu timelinesMenu = menu.addSubMenu(R.string.sk_timeline);
        timelinesMenu.getItem().setIcon(R.drawable.ic_fluent_timeline_24_regular);
        SubMenu listsMenu = menu.addSubMenu(R.string.sk_list);
        listsMenu.getItem().setIcon(R.drawable.ic_fluent_people_24_regular);
        SubMenu hashtagsMenu = menu.addSubMenu(R.string.sk_hashtag);
        hashtagsMenu.getItem().setIcon(R.drawable.ic_fluent_number_symbol_24_regular);

        makeBackItem(timelinesMenu);
        makeBackItem(listsMenu);
        makeBackItem(hashtagsMenu);

        TimelineDefinition.getAllTimelines(accountID).stream().forEach(tl -> addTimelineToOptions(tl, timelinesMenu));
        listTimelines.stream().map(TimelineDefinition::ofList).forEach(tl -> addTimelineToOptions(tl, listsMenu));
        addHashtagItem = addOptionsItem(hashtagsMenu, getContext().getString(R.string.sk_timelines_add), R.drawable.ic_fluent_add_24_regular);
        hashtags.stream().map(TimelineDefinition::ofHashtag).forEach(tl -> addTimelineToOptions(tl, hashtagsMenu));

        timelinesMenu.getItem().setVisible(timelinesMenu.size() > 0);
        listsMenu.getItem().setVisible(listsMenu.size() > 0);
        hashtagsMenu.getItem().setVisible(hashtagsMenu.size() > 0);

        UiUtils.enableOptionsMenuIcons(getContext(), optionsMenu, R.id.menu_add_timeline);
    }

    private void saveTimelines() {
        updated=true;
		AccountLocalPreferences prefs=AccountSessionManager.get(accountID).getLocalPreferences();
		if(data.isEmpty()) data.add(TimelineDefinition.HOME_TIMELINE);
		prefs.timelines=data;
		prefs.save();
	}

    private void removeTimeline(int position) {
        data.remove(position);
        adapter.notifyItemRemoved(position);
        saveTimelines();
        updateOptionsMenu();
    }

    @Override
    protected void doLoadData(int offset, int count){
        onDataLoaded(AccountSessionManager.get(accountID).getLocalPreferences().timelines);
        updateOptionsMenu();
    }

    @Override
    protected RecyclerView.Adapter<TimelineViewHolder> getAdapter() {
        return adapter = new TimelinesAdapter();
    }

    @Override
    public void scrollToTop() {
        smoothScrollRecyclerViewToTop(list);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (updated) UiUtils.restartApp();
    }

    private boolean setTagListContent(NachoTextView editText, @Nullable List<String> tags) {
        if (tags == null || tags.isEmpty()) return false;
        editText.setText(String.join(",", tags));
        editText.chipifyAllUnterminatedTokens();
        return true;
    }

    private NachoTextView prepareChipTextView(NachoTextView nacho) {
        nacho.addChipTerminator(',', BEHAVIOR_CHIPIFY_ALL);
        nacho.addChipTerminator('\n', BEHAVIOR_CHIPIFY_ALL);
        nacho.addChipTerminator(' ', BEHAVIOR_CHIPIFY_ALL);
        nacho.addChipTerminator(';', BEHAVIOR_CHIPIFY_ALL);
        nacho.enableEditChipOnTouch(true, true);
        nacho.setOnFocusChangeListener((v, hasFocus) -> nacho.chipifyAllUnterminatedTokens());
        return nacho;
    }

    @SuppressLint("ClickableViewAccessibility")
    protected void makeTimelineEditor(@Nullable TimelineDefinition item, Consumer<TimelineDefinition> onSave, Runnable onRemove) {
        Context ctx = getContext();
        View view = getActivity().getLayoutInflater().inflate(R.layout.edit_timeline, list, false);

		View divider = view.findViewById(R.id.divider);
		Button advancedBtn = view.findViewById(R.id.advanced);
        EditText editText = view.findViewById(R.id.input);
        if (item != null) editText.setText(item.getCustomTitle());
        editText.setHint(item != null ? item.getDefaultTitle(ctx) : ctx.getString(R.string.sk_hashtag));

        LinearLayout tagWrap = view.findViewById(R.id.tag_wrap);
        boolean advancedOptionsAvailable = item == null || item.getType() == TimelineDefinition.TimelineType.HASHTAG;
        advancedBtn.setVisibility(advancedOptionsAvailable ? View.VISIBLE : View.GONE);
        advancedBtn.setOnClickListener(l -> {
            advancedBtn.setSelected(!advancedBtn.isSelected());
			advancedBtn.setText(advancedBtn.isSelected() ? R.string.sk_advanced_options_hide : R.string.sk_advanced_options_show);
			divider.setVisibility(advancedBtn.isSelected() ? View.VISIBLE : View.GONE);
            tagWrap.setVisibility(advancedBtn.isSelected() ? View.VISIBLE : View.GONE);
			UiUtils.beginLayoutTransition((ViewGroup) view);
        });

        Switch localOnlySwitch = view.findViewById(R.id.local_only_switch);
        view.findViewById(R.id.local_only)
                .setOnClickListener(l -> localOnlySwitch.setChecked(!localOnlySwitch.isChecked()));

        EditText tagMain = view.findViewById(R.id.tag_main);
        NachoTextView tagsAny = prepareChipTextView(view.findViewById(R.id.tags_any));
        NachoTextView tagsAll = prepareChipTextView(view.findViewById(R.id.tags_all));
        NachoTextView tagsNone = prepareChipTextView(view.findViewById(R.id.tags_none));
        if (item != null) {
            tagMain.setText(item.getHashtagName());
			boolean hasAdvanced = !TextUtils.isEmpty(item.getCustomTitle()) && !Objects.equals(item.getHashtagName(), item.getCustomTitle());
			hasAdvanced = setTagListContent(tagsAny, item.getHashtagAny()) || hasAdvanced;
			hasAdvanced = setTagListContent(tagsAll, item.getHashtagAll()) || hasAdvanced;
            hasAdvanced = setTagListContent(tagsNone, item.getHashtagNone()) || hasAdvanced;
            if (item.isHashtagLocalOnly()) {
                localOnlySwitch.setChecked(true);
                hasAdvanced = true;
            }
            if (hasAdvanced) {
                advancedBtn.setSelected(true);
                advancedBtn.setText(R.string.sk_advanced_options_hide);
				tagWrap.setVisibility(View.VISIBLE);
				divider.setVisibility(View.VISIBLE);
            }
        }

        ImageButton btn = view.findViewById(R.id.button);
        PopupMenu popup = new PopupMenu(ctx, btn);
        TimelineDefinition.Icon currentIcon = item != null ? item.getIcon() : TimelineDefinition.Icon.HASHTAG;
        btn.setImageResource(currentIcon.iconRes);
        btn.setTag(currentIcon.ordinal());
        btn.setContentDescription(ctx.getString(currentIcon.nameRes));
        btn.setOnTouchListener(popup.getDragToOpenListener());
        btn.setOnClickListener(l -> popup.show());

        Menu menu = popup.getMenu();
        TimelineDefinition.Icon defaultIcon = item != null ? item.getDefaultIcon() : TimelineDefinition.Icon.HASHTAG;
        menu.add(0, currentIcon.ordinal(), NONE, currentIcon.nameRes).setIcon(currentIcon.iconRes);
        if (!currentIcon.equals(defaultIcon)) {
            menu.add(0, defaultIcon.ordinal(), NONE, defaultIcon.nameRes).setIcon(defaultIcon.iconRes);
        }
        for (TimelineDefinition.Icon icon : TimelineDefinition.Icon.values()) {
            if (icon.hidden || icon.ordinal() == (int) btn.getTag()) continue;
            menu.add(0, icon.ordinal(), NONE, icon.nameRes).setIcon(icon.iconRes);
        }
        UiUtils.enablePopupMenuIcons(ctx, popup);

        popup.setOnMenuItemClickListener(menuItem -> {
            TimelineDefinition.Icon icon = TimelineDefinition.Icon.values()[menuItem.getItemId()];
            btn.setImageResource(icon.iconRes);
            btn.setTag(menuItem.getItemId());
            btn.setContentDescription(ctx.getString(icon.nameRes));
            return true;
        });

        AlertDialog.Builder builder = new M3AlertDialogBuilder(ctx)
                .setTitle(item == null ? R.string.sk_add_timeline : R.string.sk_edit_timeline)
                .setView(view)
                .setPositiveButton(R.string.save, (d, which) -> {
                    tagsAny.chipifyAllUnterminatedTokens();
                    tagsAll.chipifyAllUnterminatedTokens();
                    tagsNone.chipifyAllUnterminatedTokens();
                    String name = editText.getText().toString().trim();
                    String mainHashtag = tagMain.getText().toString().trim();
                    if (TextUtils.isEmpty(mainHashtag)) {
                        mainHashtag = name;
                        name = null;
                    }
                    if (TextUtils.isEmpty(mainHashtag)) {
                        Toast.makeText(ctx, R.string.sk_add_timeline_tag_error_empty, Toast.LENGTH_SHORT).show();
                        onSave.accept(null);
                        return;
                    }

                    TimelineDefinition tl = item != null ? item : TimelineDefinition.ofHashtag(name);
                    TimelineDefinition.Icon icon = TimelineDefinition.Icon.values()[(int) btn.getTag()];
                    tl.setIcon(icon);
                    tl.setTitle(name);
                    tl.setTagOptions(
                            mainHashtag,
                            tagsAny.getChipValues(),
                            tagsAll.getChipValues(),
                            tagsNone.getChipValues(),
                            localOnlySwitch.isChecked()
                    );
                    onSave.accept(tl);
                })
                .setNegativeButton(R.string.cancel, (d, which) -> {});

        if (onRemove != null) builder.setNeutralButton(R.string.sk_remove, (d, which) -> onRemove.run());

        builder.show();
        btn.requestFocus();
    }

    private class TimelinesAdapter extends RecyclerView.Adapter<TimelineViewHolder>{
        @NonNull
        @Override
        public TimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
            return new TimelineViewHolder();
        }

        @Override
        public void onBindViewHolder(@NonNull TimelineViewHolder holder, int position) {
            holder.bind(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private class TimelineViewHolder extends BindableViewHolder<TimelineDefinition> implements UsableRecyclerView.Clickable{
        private final TextView title;
        private final ImageView dragger;

        public TimelineViewHolder(){
            super(getActivity(), R.layout.item_text, list);
            title=findViewById(R.id.title);
            dragger=findViewById(R.id.dragger_thingy);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onBind(TimelineDefinition item) {
            title.setText(item.getTitle(getContext()));
            title.setCompoundDrawablesRelativeWithIntrinsicBounds(itemView.getContext().getDrawable(item.getIcon().iconRes), null, null, null);
            dragger.setVisibility(View.VISIBLE);
            dragger.setOnTouchListener((View v, MotionEvent event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(this);
                    return true;
                }
                return false;
            });
        }

        private void onSave(TimelineDefinition tl) {
            saveTimelines();
            rebind();
        }

        private void onRemove() {
            removeTimeline(getAbsoluteAdapterPosition());
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onClick() {
            makeTimelineEditor(item, this::onSave, this::onRemove);
        }
    }

    private class ItemTouchHelperCallback extends ItemTouchHelper.SimpleCallback {
        public ItemTouchHelperCallback() {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getAbsoluteAdapterPosition();
            int toPosition = target.getAbsoluteAdapterPosition();
            if (Math.max(fromPosition, toPosition) >= data.size() || Math.min(fromPosition, toPosition) < 0) {
                return false;
            } else {
                Collections.swap(data, fromPosition, toPosition);
                adapter.notifyItemMoved(fromPosition, toPosition);
                saveTimelines();
                return true;
            }
        }

        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                viewHolder.itemView.animate().alpha(0.65f);
            }
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            viewHolder.itemView.animate().alpha(1f);
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAbsoluteAdapterPosition();
            removeTimeline(position);
        }
    }
}
