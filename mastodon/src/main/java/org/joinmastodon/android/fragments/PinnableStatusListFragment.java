package org.joinmastodon.android.fragments;

import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountLocalPreferences;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.TimelineDefinition;

import java.util.ArrayList;
import java.util.List;

public abstract class PinnableStatusListFragment extends StatusListFragment {
    protected List<TimelineDefinition> timelines;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        timelines=new ArrayList<>(AccountSessionManager.get(accountID).getLocalPreferences().timelines);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        updatePinButton(menu.findItem(R.id.pin));
    }

    protected boolean isPinned() {
        return timelines.contains(makeTimelineDefinition());
    }

    protected void updatePinButton(MenuItem pin) {
        boolean pinned = isPinned();
        pin.setIcon(pinned ?
                R.drawable.ic_fluent_pin_24_filled :
                R.drawable.ic_fluent_pin_24_regular);
        pin.setTitle(pinned ? R.string.sk_unpin_timeline : R.string.sk_pin_timeline);
    }

    protected abstract TimelineDefinition makeTimelineDefinition();

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.pin) {
            togglePin(item);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void togglePin(MenuItem pin) {
        onPinnedUpdated(true);
        getToolbar().performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
        TimelineDefinition def = makeTimelineDefinition();
        boolean pinned = isPinned();
        if (pinned) timelines.remove(def);
        else timelines.add(def);
        Toast.makeText(getContext(), pinned ? R.string.sk_unpinned_timeline : R.string.sk_pinned_timeline, Toast.LENGTH_SHORT).show();
		AccountLocalPreferences prefs=AccountSessionManager.get(accountID).getLocalPreferences();
		prefs.timelines=new ArrayList<>(timelines);
        prefs.save();
        updatePinButton(pin);
    }

    public void onPinnedUpdated(boolean pinned) {}
}
