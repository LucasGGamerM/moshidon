package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.mastodon.requests.lists.GetList;
import org.joinmastodon.android.api.mastodon.requests.lists.UpdateList;
import org.joinmastodon.android.api.mastodon.requests.timelines.GetListTimeline;
import org.joinmastodon.android.events.ListDeletedEvent;
import org.joinmastodon.android.events.ListUpdatedCreatedEvent;
import org.joinmastodon.android.model.Filter;
import org.joinmastodon.android.model.ListTimeline;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.TimelineDefinition;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.ListTimelineEditor;
import org.joinmastodon.android.utils.StatusFilterPredicate;

import java.util.List;
import java.util.stream.Collectors;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.V;


public class ListTimelineFragment extends PinnableStatusListFragment {
    private String listID;
    private String listTitle;
    @Nullable
    private ListTimeline.RepliesPolicy repliesPolicy;

    @Override
    protected boolean wantsComposeButton() {
        return true;
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        Bundle args = getArguments();
        listID = args.getString("listID");
        listTitle = args.getString("listTitle");
        repliesPolicy = ListTimeline.RepliesPolicy.values()[args.getInt("repliesPolicy", 0)];

        setTitle(listTitle);
        setHasOptionsMenu(true);

        new GetList(listID).setCallback(new Callback<>() {
            @Override
            public void onSuccess(ListTimeline listTimeline) {
                if (getActivity() == null) return;
                // TODO: save updated info
                if (!listTimeline.title.equals(listTitle)) setTitle(listTimeline.title);
                if (listTimeline.repliesPolicy != null && !listTimeline.repliesPolicy.equals(repliesPolicy)) {
                    repliesPolicy = listTimeline.repliesPolicy;
                }
            }

            @Override
            public void onError(ErrorResponse error) {
                error.showToast(getContext());
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.list, menu);
        super.onCreateOptionsMenu(menu, inflater);
        UiUtils.enableOptionsMenuIcons(getContext(), menu, R.id.pin);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (super.onOptionsItemSelected(item)) return true;
        if (item.getItemId() == R.id.edit) {
            ListTimelineEditor editor = new ListTimelineEditor(getContext());
            editor.applyList(listTitle, repliesPolicy);
            new M3AlertDialogBuilder(getActivity())
                    .setTitle(R.string.sk_edit_list_title)
                    .setIcon(R.drawable.ic_fluent_people_28_regular)
                    .setView(editor)
                    .setPositiveButton(R.string.save, (d, which) -> {
                        String newTitle = editor.getTitle().trim();
                        setTitle(newTitle);
                        new UpdateList(listID, newTitle, editor.getRepliesPolicy()).setCallback(new Callback<>() {
                            @Override
                            public void onSuccess(ListTimeline list) {
                                if (getActivity() == null) return;
                                setTitle(list.title);
                                listTitle = list.title;
                                repliesPolicy = list.repliesPolicy;
                                E.post(new ListUpdatedCreatedEvent(listID, listTitle, repliesPolicy));
                            }

                            @Override
                            public void onError(ErrorResponse error) {
                                setTitle(listTitle);
                                error.showToast(getContext());
                            }
                        }).exec(accountID);
                    })
                    .setNegativeButton(R.string.cancel, (d, which) -> {})
                    .show();
        } else if (item.getItemId() == R.id.delete) {
            UiUtils.confirmDeleteList(getActivity(), accountID, listID, listTitle, () -> {
                E.post(new ListDeletedEvent(listID));
                Nav.finish(this);
            });
        }
        return true;
    }

    @Override
    protected TimelineDefinition makeTimelineDefinition() {
        return TimelineDefinition.ofList(listID, listTitle);
    }

    @Override
    protected void doLoadData(int offset, int count) {
        currentRequest=new GetListTimeline(listID, offset==0 ? null : getMaxID(), null, count, null)
                .setCallback(new SimpleCallback<>(this) {
                    @Override
                    public void onSuccess(List<Status> result) {
                        if (getActivity() == null) return;
                        result=result.stream().filter(new StatusFilterPredicate(accountID, Filter.FilterContext.HOME)).collect(Collectors.toList());
                        onDataLoaded(result, !result.isEmpty());
                    }
                })
               .exec(accountID);
    }

    @Override
    protected void onShown() {
        super.onShown();
        if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading)
            loadData();
    }

    @Override
    public void onFabClick(View v){
        Bundle args=new Bundle();
        args.putString("account", accountID);
        Nav.go(getActivity(), ComposeFragment.class, args);
    }

    @Override
    protected void onSetFabBottomInset(int inset) {
        ((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin=V.dp(24)+inset;
    }
}
