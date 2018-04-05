package ca.carleton.gcrc.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;

/**
 * Created by thomaseaton on 2018-04-03.
 */

public class AtlasPictureSingleton {
    private static AtlasPictureSingleton singleton = new AtlasPictureSingleton();

    private AtlasPictureSingleton() {}

    public static AtlasPictureSingleton getInstance() {
        return singleton;
    }

    public Bitmap getUserInitialsBitmap(String initials, int imageSize, Context context) {
        if (initials != null && !initials.isEmpty()) {
            Bitmap initialsBitmap = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(initialsBitmap);
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setFlags(Paint.ANTI_ALIAS_FLAG);

            canvas.drawCircle(initialsBitmap.getWidth() / 2F, initialsBitmap.getHeight() / 2F,
                    initialsBitmap.getWidth() / 2F, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

            paint.setColor(Color.WHITE);
            canvas.drawPaint(paint);

            paint.setColor(Color.WHITE);
            paint.setTextSize(imageSize / 2.5F);

            Rect textBounds = new Rect();
            paint.getTextBounds(initials, 0, 1, textBounds);
            float textOffsetX = (imageSize - paint.measureText(initials)) / 2F;
            float textOffsetY = (imageSize - textBounds.height()) - (imageSize * 0.05F);
            canvas.drawText(initials,
                    textOffsetX,
                    textOffsetY,
                    paint);

            return initialsBitmap;
        }

        return null;
    }

}
