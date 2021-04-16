package edu.byu.cs.tweeter.view.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * Contains utility methods for working with Android images.
 */
public class ImageUtils {

    /**
     * Creates a drawable from the bytes read from an image file.
     *
     * @param bytes the bytes.
     * @return the drawable.
     */
    public static Drawable drawableFromByteArray(byte [] bytes) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        return new BitmapDrawable(Resources.getSystem(), bitmap);
    }
}
