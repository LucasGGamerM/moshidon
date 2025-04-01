package com.hootsuite.nachos.chip;

import androidx.annotation.Nullable;

public interface Chip {

    /**
     * @return the text represented by this Chip
     */
    CharSequence getText();

    /**
     * @return the data associated with this Chip or null if no data is associated with it
     */
    @Nullable
    Object getData();

    /**
     * @return the width of the Chip or -1 if the Chip hasn't been given the chance to calculate its width
     */
    int getWidth();

    /**
     * Sets the UI state.
     *
     * @param stateSet one of the state constants in {@link android.view.View}
     */
    void setState(int[] stateSet);
}
