package com.hootsuite.nachos.chip;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;

import com.hootsuite.nachos.ChipConfiguration;

import androidx.annotation.NonNull;

public class ChipSpanChipCreator implements ChipCreator<ChipSpan> {

    @Override
    public ChipSpan createChip(@NonNull Context context, @NonNull CharSequence text, Object data) {
        return new ChipSpan(context, text, null, data);
    }

    @Override
    public ChipSpan createChip(@NonNull Context context, @NonNull ChipSpan existingChip) {
        return new ChipSpan(context, existingChip);
    }

    @Override
    public void configureChip(@NonNull ChipSpan chip, @NonNull ChipConfiguration chipConfiguration) {
        int chipHorizontalSpacing = chipConfiguration.getChipHorizontalSpacing();
        ColorStateList chipBackground = chipConfiguration.getChipBackground();
        int chipCornerRadius = chipConfiguration.getChipCornerRadius();
        int chipTextColor = chipConfiguration.getChipTextColor();
        int chipTextSize = chipConfiguration.getChipTextSize();
        int chipHeight = chipConfiguration.getChipHeight();
        int chipVerticalSpacing = chipConfiguration.getChipVerticalSpacing();
        int maxAvailableWidth = chipConfiguration.getMaxAvailableWidth();

        if (chipHorizontalSpacing != -1) {
            chip.setLeftMargin(chipHorizontalSpacing / 2);
            chip.setRightMargin(chipHorizontalSpacing / 2);
        }
        if (chipBackground != null) {
            chip.setBackgroundColor(chipBackground);
        }
        if (chipCornerRadius != -1) {
            chip.setCornerRadius(chipCornerRadius);
        }
        if (chipTextColor != Color.TRANSPARENT) {
            chip.setTextColor(chipTextColor);
        }
        if (chipTextSize != -1) {
            chip.setTextSize(chipTextSize);
        }
        if (chipHeight != -1) {
            chip.setChipHeight(chipHeight);
        }
        if (chipVerticalSpacing != -1) {
            chip.setChipVerticalSpacing(chipVerticalSpacing);
        }
        if (maxAvailableWidth != -1) {
            chip.setMaxAvailableWidth(maxAvailableWidth);
        }
    }
}
