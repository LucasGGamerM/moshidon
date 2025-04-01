package com.hootsuite.nachos.terminator;

import android.text.Editable;

import com.hootsuite.nachos.tokenizer.ChipTokenizer;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * This interface is used to handle the management of characters that should trigger the creation of chips in a text view.
 *
 * @see ChipTokenizer
 */
public interface ChipTerminatorHandler {

    /**
     * When a chip terminator character is encountered in newly inserted text, all tokens in the whole text view will be chipified
     */
    int BEHAVIOR_CHIPIFY_ALL = 0;

    /**
     * When a chip terminator character is encountered in newly inserted text, only the current token (that in which the chip terminator character
     * was found) will be chipified. This token may extend beyond where the chip terminator character was located.
     */
    int BEHAVIOR_CHIPIFY_CURRENT_TOKEN = 1;

    /**
     * When a chip terminator character is encountered in newly inserted text, only the text from the previous chip up until the chip terminator
     * character will be chipified. This may not be an entire token.
     */
    int BEHAVIOR_CHIPIFY_TO_TERMINATOR = 2;

    /**
     * Constant for use with {@link #setPasteBehavior(int)}. Use this if a paste should behave the same as a standard text input (the chip temrinators
     * will all behave according to their pre-determined behavior set through {@link #addChipTerminator(char, int)} or {@link #setChipTerminators(Map)}).
     */
    int PASTE_BEHAVIOR_USE_DEFAULT = -1;

    /**
     * Sets all the characters that will be marked as chip terminators. This will replace any previously set chip terminators.
     *
     * @param chipTerminators a map of characters to be marked as chip terminators to behaviors that describe how to respond to the characters, or null
     *                        to remove all chip terminators
     */
    void setChipTerminators(@Nullable Map<Character, Integer> chipTerminators);

    /**
     * Adds a character as a chip terminator. When the provided character is encountered in entered text, the nearby text will be chipified according
     * to the behavior provided here.
     *     {@code behavior} Must be one of:
     *     <ul>
     *         <li>{@link #BEHAVIOR_CHIPIFY_ALL}</li>
     *         <li>{@link #BEHAVIOR_CHIPIFY_CURRENT_TOKEN}</li>
     *         <li>{@link #BEHAVIOR_CHIPIFY_TO_TERMINATOR}</li>
     *     </ul>
     *
     * @param character the character to mark as a chip terminator
     * @param behavior  the behavior describing how to respond to the chip terminator
     */
    void addChipTerminator(char character, int behavior);

    /**
     * Customizes the way paste events are handled.
     *     If one of:
     *     <ul>
     *         <li>{@link #BEHAVIOR_CHIPIFY_ALL}</li>
     *         <li>{@link #BEHAVIOR_CHIPIFY_CURRENT_TOKEN}</li>
     *         <li>{@link #BEHAVIOR_CHIPIFY_TO_TERMINATOR}</li>
     *     </ul>
     *     is passed, all chip terminators will be handled with that behavior when a paste event occurs.
     *     If {@link #PASTE_BEHAVIOR_USE_DEFAULT} is passed, whatever behavior is configured for a particular chip terminator
     *     (through {@link #setChipTerminators(Map)} or {@link #addChipTerminator(char, int)} will be used for that chip terminator
     *
     * @param pasteBehavior the behavior to use on a paste event
     */
    void setPasteBehavior(int pasteBehavior);

    /**
     * Parses the provided text looking for characters marked as chip terminators through {@link #addChipTerminator(char, int)} and {@link #setChipTerminators(Map)}.
     *     The provided {@link Editable} will be modified if chip terminators are encountered.
     *
     * @param tokenizer    the {@link ChipTokenizer} to use to identify and chipify tokens in the text
     * @param text         the text in which to search for chip terminators tokens to be chipped
     * @param start        the index at which to begin looking for chip terminators (inclusive)
     * @param end          the index at which to end looking for chip terminators (exclusive)
     * @param isPasteEvent true if this handling is for a paste event in which case the behavior set in {@link #setPasteBehavior(int)} will be used,
     *                     otherwise false
     * @return an non-negative integer indicating the index where the cursor (selection) should be placed once the handling is complete,
     *         or a negative integer indicating that the cursor should not be moved.
     */
    int findAndHandleChipTerminators(@NonNull ChipTokenizer tokenizer, @NonNull Editable text, int start, int end, boolean isPasteEvent);
}
