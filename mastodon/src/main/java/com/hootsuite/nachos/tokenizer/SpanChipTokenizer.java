package com.hootsuite.nachos.tokenizer;

import android.content.Context;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.Pair;

import com.hootsuite.nachos.ChipConfiguration;
import com.hootsuite.nachos.chip.Chip;
import com.hootsuite.nachos.chip.ChipCreator;
import com.hootsuite.nachos.chip.ChipSpan;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A default implementation of {@link ChipTokenizer}.
 *     This implementation does the following:
 *     <ul>
 *         <li>Surrounds each token with a space and the Unit Separator ASCII control character (31) - See the diagram below
 *              <ul>
 *                  <li>The spaces are included so that android keyboards can distinguish the chips as different words and provide accurate
 *                  autocorrect suggestions</li>
 *              </ul>
 *         </li>
 *         <li>Replaces each token with a {@link ChipSpan} containing the same text, once the token terminates</li>
 *         <li>Uses the values passed to {@link #applyConfiguration(Editable, ChipConfiguration)} to configure any ChipSpans that get created</li>
 *     </ul>
 * Each terminated token will therefore look like the following (this is what will be returned from {@link #terminateToken(CharSequence, Object)}):
 * <pre>
 *  -----------------------------------------------------------
 *  | SpannableString                                         |
 *  |   ----------------------------------------------------  |
 *  |   | ChipSpan                                         |  |
 *  |   |                                                  |  |
 *  |   |  space   separator    text    separator   space  |  |
 *  |   |                                                  |  |
 *  |   ----------------------------------------------------  |
 *  -----------------------------------------------------------
 * </pre>
 *
 * @see ChipSpan
 */
public class SpanChipTokenizer<C extends Chip> implements ChipTokenizer {

    /**
     * The character used to separate chips internally is the US (Unit Separator) ASCII control character.
     * This character is used because it's untypable so we have complete control over when chips are created.
     */
    public static final char CHIP_SPAN_SEPARATOR = 31;
    public static final char AUTOCORRECT_SEPARATOR = ' ';

    private Context mContext;

    @Nullable
    private ChipConfiguration mChipConfiguration;
    @NonNull
    private ChipCreator<C> mChipCreator;
    @NonNull
    private Class<C> mChipClass;

    private Comparator<Pair<Integer, Integer>> mReverseTokenIndexesSorter = new Comparator<Pair<Integer, Integer>>() {
        @Override
        public int compare(Pair<Integer, Integer> lhs, Pair<Integer, Integer> rhs) {
            return rhs.first - lhs.first;
        }
    };

    public SpanChipTokenizer(Context context, @NonNull ChipCreator<C> chipCreator, @NonNull Class<C> chipClass) {
        mContext = context;
        mChipCreator = chipCreator;
        mChipClass = chipClass;
    }

    @Override
    public void applyConfiguration(Editable text, ChipConfiguration chipConfiguration) {
        mChipConfiguration = chipConfiguration;

        for (C chip : findAllChips(0, text.length(), text)) {
            // Recreate the chips with the new configuration
            int chipStart = findChipStart(chip, text);
            deleteChip(chip, text);
            text.insert(chipStart, terminateToken(mChipCreator.createChip(mContext, chip)));
        }
    }

    @Override
    public int findTokenStart(CharSequence text, int cursor) {
        int i = cursor;

        // Work backwards until we find a CHIP_SPAN_SEPARATOR
        while (i > 0 && text.charAt(i - 1) != CHIP_SPAN_SEPARATOR) {
            i--;
        }
        // Work forwards to skip over any extra whitespace at the beginning of the token
        while (i > 0 && i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        return i;
    }

    @Override
    public int findTokenEnd(CharSequence text, int cursor) {
        int i = cursor;
        int len = text.length();

        // Work forwards till we find a CHIP_SPAN_SEPARATOR
        while (i < len) {
            if (text.charAt(i) == CHIP_SPAN_SEPARATOR) {
                return (i - 1); // subtract one because the CHIP_SPAN_SEPARATOR will be preceded by a space
            } else {
                i++;
            }
        }
        return len;
    }

    @NonNull
    @Override
    public List<Pair<Integer, Integer>> findAllTokens(CharSequence text) {
        List<Pair<Integer, Integer>> unterminatedTokens = new ArrayList<>();

        boolean insideChip = false;
        // Iterate backwards through the text (to avoid messing up indexes)
        for (int index = text.length() - 1; index >= 0; index--) {
            char theCharacter = text.charAt(index);

            // Every time we hit a CHIP_SPAN_SEPARATOR character we switch from being inside to outside
            // or outside to inside a chip
            // This check must happen before the whitespace check because CHIP_SPAN_SEPARATOR is considered a whitespace character
            if (theCharacter == CHIP_SPAN_SEPARATOR) {
                insideChip = !insideChip;
                continue;
            }

            // Completely skip over whitespace
            if (Character.isWhitespace(theCharacter)) {
                continue;
            }

            // If we're ever outside a chip, see if the text we're in is a viable token for chipification
            if (!insideChip) {
                int tokenStart = findTokenStart(text, index);
                int tokenEnd = findTokenEnd(text, index);

                // Can only actually be chipified if there's at least one character between them
                if (tokenEnd - tokenStart >= 1) {
                    unterminatedTokens.add(new Pair<>(tokenStart, tokenEnd));
                    index = tokenStart;
                }
            }
        }
        return unterminatedTokens;
    }

    @Override
    public CharSequence terminateToken(CharSequence text, @Nullable Object data) {
        // Remove leading/trailing whitespace
        CharSequence trimmedText = text.toString().trim();
        return terminateToken(mChipCreator.createChip(mContext, trimmedText, data));
    }

    private CharSequence terminateToken(C chip) {
        // Surround the text with CHIP_SPAN_SEPARATOR and spaces
        // The spaces allow autocorrect to correctly identify words
        String chipSeparator = Character.toString(CHIP_SPAN_SEPARATOR);
        String autoCorrectSeparator = Character.toString(AUTOCORRECT_SEPARATOR);
        CharSequence textWithSeparator = autoCorrectSeparator + chipSeparator + chip.getText() + chipSeparator + autoCorrectSeparator;

        // Build the container object to house the ChipSpan and space
        SpannableString spannableString = new SpannableString(textWithSeparator);

        // Attach the ChipSpan
        if (mChipConfiguration != null) {
            mChipCreator.configureChip(chip, mChipConfiguration);
        }
        spannableString.setSpan(chip, 0, textWithSeparator.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannableString;
    }

    @Override
    public void terminateAllTokens(Editable text) {
        List<Pair<Integer, Integer>> unterminatedTokens = findAllTokens(text);
        // Sort in reverse order (so index changes don't affect anything)
        Collections.sort(unterminatedTokens, mReverseTokenIndexesSorter);
        for (Pair<Integer, Integer> indexes : unterminatedTokens) {
            int start = indexes.first;
            int end = indexes.second;
            CharSequence textToChip = text.subSequence(start, end);
            CharSequence chippedText = terminateToken(textToChip, null);
            text.replace(start, end, chippedText);
        }
    }

    @Override
    public int findChipStart(Chip chip, Spanned text) {
        return text.getSpanStart(chip);
    }

    @Override
    public int findChipEnd(Chip chip, Spanned text) {
        return text.getSpanEnd(chip);
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public C[] findAllChips(int start, int end, Spanned text) {
        C[] spansArray = text.getSpans(start, end, mChipClass);
        return (spansArray != null) ? spansArray : (C[]) Array.newInstance(mChipClass, 0);
    }

    @Override
    public void revertChipToToken(Chip chip, Editable text) {
        int chipStart = findChipStart(chip, text);
        int chipEnd = findChipEnd(chip, text);
        text.removeSpan(chip);
        text.replace(chipStart, chipEnd, chip.getText());
    }

    @Override
    public void deleteChip(Chip chip, Editable text) {
        int chipStart = findChipStart(chip, text);
        int chipEnd = findChipEnd(chip, text);
        text.removeSpan(chip);
        // On the emulator for some reason the text automatically gets deleted and chipStart and chipEnd end up both being -1, so in that case we
        // don't need to call text.delete(...)
        if (chipStart != chipEnd) {
            text.delete(chipStart, chipEnd);
        }
    }

    @Override
    public void deleteChipAndPadding(Chip chip, Editable text) {
        // This implementation does not add any extra padding outside of the span so we can just delete the chip normally
        deleteChip(chip, text);
    }
}
