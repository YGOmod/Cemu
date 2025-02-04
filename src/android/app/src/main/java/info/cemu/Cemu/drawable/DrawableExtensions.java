package info.cemu.Cemu.drawable;

import android.content.res.Resources;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;

import java.util.Objects;

public class DrawableExtensions {
    private static final ColorMatrix INVERTED_COLOR_MATRIX = new ColorMatrix(
            new float[]{
                    -1, 0, 0, 0, 255,
                    0, -1, 0, 0, 255,
                    0, 0, -1, 0, 255,
                    0, 0, 0, 1, 0
            }
    );

    public static Drawable getInvertedDrawable(Drawable drawable, Resources resources) {
        var newDrawable = Objects.requireNonNull(drawable.getConstantState())
                .newDrawable(resources);
        return applyInvertedColorTransform(newDrawable);
    }

    public static Drawable applyInvertedColorTransform(Drawable drawable) {
        drawable = drawable.mutate();
        drawable.setColorFilter(new ColorMatrixColorFilter(INVERTED_COLOR_MATRIX));
        return drawable;
    }
}
