package com.example.eventcountdownwidget.utils;

import android.content.Context; // Keep Context import if other methods might need it later
import android.graphics.Color;
// Removed unused imports: android.util.TypedValue, java.util.Random

public class ColorUtil {

    /**
     * Determines if a color is perceived as dark based on its luminance.
     * Uses the standard relative luminance formula.
     *
     * @param color The ARGB color integer.
     * @return true if the color is considered dark, false otherwise.
     */
    public static boolean isDarkColor(int color) {
        // Calculate luminance using the standard formula W3C formula
        // (https://www.w3.org/TR/WCAG20-TECHS/G17.html#G17-tests)
        double r = Color.red(color) / 255.0;
        double g = Color.green(color) / 255.0;
        double b = Color.blue(color) / 255.0;

        // Apply gamma correction before calculating luminance
        r = (r <= 0.03928) ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
        g = (g <= 0.03928) ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
        b = (b <= 0.03928) ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);

        double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;

        // Colors with luminance below 0.5 are generally considered dark
        return luminance < 0.5;
    }

    /**
     * Returns an appropriate text color (either white or black) for good contrast
     * against the given background color.
     *
     * @param backgroundColor The ARGB background color integer.
     * @return Color.WHITE if the background is dark, Color.BLACK otherwise.
     */
    public static int getTextColorForBackground(int backgroundColor) {
        // Return white text for dark backgrounds, black text for light backgrounds
        return isDarkColor(backgroundColor) ? Color.WHITE : Color.BLACK;
    }

    /*
     * Removed getRandomMaterialColor(Context context) method.
     * Widget background color generation is handled directly in the
     * configuration activities (WidgetConfigActivity, SimpleListWidgetConfigActivity)
     * using a predefined array and the generateUniqueRandomColor logic,
     * which is more suitable for generating a specific, persistent color
     * for the widget instance itself, rather than relying on potentially
     * unavailable theme attributes in the widget update context.
     */
    /*
    public static int getRandomMaterialColor(Context context) {
        int[] colorIds = new int[]{
                com.google.android.material.R.attr.colorPrimary,
                com.google.android.material.R.attr.colorSecondary,
                com.google.android.material.R.attr.colorTertiary
        };

        int selectedColorAttr = colorIds[new Random().nextInt(colorIds.length)];
        TypedValue typedValue = new TypedValue();
        // Using context.getTheme() might not always work reliably outside an Activity
        // context, especially for dynamic colors in widget updates.
        context.getTheme().resolveAttribute(selectedColorAttr, typedValue, true);

        return typedValue.data;
    }
    */
}