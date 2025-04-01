package com.hootsuite.nachos.tokenizer;

import android.text.Editable;
import android.text.Spanned;
import android.util.Pair;

import com.hootsuite.nachos.ChipConfiguration;
import com.hootsuite.nachos.chip.Chip;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An extension of {@link android.widget.MultiAutoCompleteTextView.Tokenizer Tokenizer} that provides extra support
 * for chipification.
 * <p>
 *     In the context of this interface, a token is considered to be plain (non-chipped) text. Once a token is terminated it becomes or contains a chip.
 * </p>
 * <p>
 *     The CharSequences passed to the ChipTokenizer methods may contain both chipped text
 *     and plain text so the tokenizer must have some method of distinguishing between the two (e.g. using a delimeter character.
 *     The {@link #terminateToken(CharSequence, Object)} method is where a chip can be formed and returned to replace the plain text.
 *     Whatever class the implementation deems to represent a chip, must implement the {@link Chip} interface.
 * </p>
 *
 * @see SpanChipTokenizer
 */
public interface ChipTokenizer {

    /**
     * Configures this ChipTokenizer to produce chips with the provided attributes. For each of these attributes, {@code -1} or {@code null} may be
     * passed to indicate that the attribute may be ignored.
     * <p>
     *     This will also apply the provided {@link ChipConfiguration} to any existing chips in the provided text.
     * </p>
     *
     * @param text              the text in which to search for existing chips to apply the configuration to
     * @param chipConfiguration a {@link ChipConfiguration} containing customizations for the chips produced by this class
     */
    void applyConfiguration(Editable text, ChipConfiguration chipConfiguration);

    /**
     * Returns the start of the token that ends at offset
     * <code>cursor</code> within <code>text</code>.
     */
    int findTokenStart(CharSequence text, int cursor);

    /**
     * Returns the end of the token (minus trailing punctuation)
     * that begins at offset <code>cursor</code> within <code>text</code>.
     */
    int findTokenEnd(CharSequence text, int cursor);

    /**
     * Searches through {@code text} for any tokens.
     *
     * @param text the text in which to search for un-terminated tokens
     * @return a list of {@link Pair}s of the form (startIndex, endIndex) containing the locations of all
     * unterminated tokens
     */
    @NonNull
    List<Pair<Integer, Integer>> findAllTokens(CharSequence text);

    /**
     * Returns <code>text</code>, modified, if necessary, to ensure that
     * it ends with a token terminator (for example a space or comma).
     */
    CharSequence terminateToken(CharSequence text, @Nullable Object data);

    /**
     * Terminates (converts from token into chip) all unterminated tokens in the provided text.
     * This method CAN alter the provided text.
     *
     * @param text the text in which to terminate all tokens
     */
    void terminateAllTokens(Editable text);

    /**
     * Finds the index of the first character in {@code text} that is a part of {@code chip}
     *
     * @param chip the chip whose start should be found
     * @param text the text in which to search for the start of {@code chip}
     * @return the start index of the chip
     */
    int findChipStart(Chip chip, Spanned text);

    /**
     * Finds the index of the character after the last character in {@code text} that is a part of {@code chip}
     *
     * @param chip the chip whose end should be found
     * @param text the text in which to search for the end of {@code chip}
     * @return the end index of the chip
     */
    int findChipEnd(Chip chip, Spanned text);

    /**
     * Searches through {@code text} for any chips
     *
     * @param start index to start looking for terminated tokens (inclusive)
     * @param end   index to end looking for terminated tokens (exclusive)
     * @param text  the text in which to search for terminated tokens
     * @return a list of objects implementing the {@link Chip} interface to represent the terminated tokens
     */
    @NonNull
    Chip[] findAllChips(int start, int end, Spanned text);

    /**
     * Effectively does the opposite of {@link #terminateToken(CharSequence, Object)} by reverting the provided chip back into a token.
     * This method CAN alter the provided text.
     *
     * @param chip the chip to revert into a token
     * @param text the text in which the chip resides
     */
    void revertChipToToken(Chip chip, Editable text);

    /**
     * Removes a chip and any text it encompasses from {@code text}. This method CAN alter the provided text.
     *
     * @param chip the chip to remove
     * @param text the text to remove the chip from
     */
    void deleteChip(Chip chip, Editable text);

    /**
     * Removes a chip, any text it encompasses AND any padding text (such as spaces) that may have been inserted when the chip was created in
     * {@link #terminateToken(CharSequence, Object)} or after. This method CAN alter the provided text.
     *
     * @param chip the chip to remove
     * @param text the text to remove the chip and padding from
     */
    void deleteChipAndPadding(Chip chip, Editable text);
}
