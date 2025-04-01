package com.hootsuite.nachos.chip;

import android.content.Context;

import com.hootsuite.nachos.ChipConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Interface to allow the creation and configuration of chips
 *
 * @param <C> The type of {@link Chip} that the implementation will create/configure
 */
public interface ChipCreator<C extends Chip> {

    /**
     * Creates a chip from the given context and text. Use this method when creating a brand new chip from a piece of text.
     *
     * @param context the {@link Context} to use to initialize the chip
     * @param text    the text the Chip should represent
     * @param data    the data to associate with the Chip, or null to associate no data
     * @return the created chip
     */
    C createChip(@NonNull Context context, @NonNull CharSequence text, @Nullable Object data);

    /**
     * Creates a chip from the given context and existing chip. Use this method when recreating a chip from an existing one.
     *
     * @param context      the {@link Context} to use to initialize the chip
     * @param existingChip the chip that the created chip should be based on
     * @return the created chip
     */
    C createChip(@NonNull Context context, @NonNull C existingChip);

    /**
     * Applies the given {@link ChipConfiguration} to the given {@link Chip}. Use this method to customize the appearance/behavior of a chip before
     * adding it to the text.
     *
     * @param chip              the chip to configure
     * @param chipConfiguration the configuration to apply to the chip
     */
    void configureChip(@NonNull C chip, @NonNull ChipConfiguration chipConfiguration);
}
