package com.hootsuite.nachos.validator;

import com.hootsuite.nachos.tokenizer.ChipTokenizer;

import androidx.annotation.NonNull;

/**
 * Interface used to ensure that a given CharSequence complies to a particular format.
 */
public interface NachoValidator {

    /**
     * Validates the specified text.
     *
     * @return true If the text currently in the text editor is valid.
     * @see #fixText(ChipTokenizer, CharSequence)
     */
    boolean isValid(@NonNull ChipTokenizer chipTokenizer, CharSequence text);

    /**
     * Corrects the specified text to make it valid.
     *
     * @param invalidText A string that doesn't pass validation: isValid(invalidText)
     *                    returns false
     * @return A string based on invalidText such as invoking isValid() on it returns true.
     * @see #isValid(ChipTokenizer, CharSequence)
     */
    CharSequence fixText(@NonNull ChipTokenizer chipTokenizer, CharSequence invalidText);
}
