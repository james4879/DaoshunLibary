package com.daoshun.lib.util;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * 图片处理工具
 */
public class BitmapUtils {

    /**
     * 图片缩放
     * 
     * @param bitmap
     *            位图
     * @param scale
     *            缩放比例
     */
    public static final Bitmap scaleBitmap(Bitmap bitmap, float scale) {
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        return redrawBitmap(bitmap, 0, 0, bitmapWidth, bitmapHeight, 0, scale);
    }

    /**
     * 图片旋转
     * 
     * @param bitmap
     *            位图
     * @param degrees
     *            旋转角度
     */
    public static final Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        return redrawBitmap(bitmap, 0, 0, bitmapWidth, bitmapHeight, degrees, 1);
    }

    /**
     * 图片重绘
     * 
     * @param bitmap
     *            位图
     * @param cropX
     *            切图X位置
     * @param cropY
     *            切图Y位置
     * @param cropWidth
     *            切图宽度
     * @param cropHeight
     *            切图高度
     */
    public static final Bitmap cropBitmap(Bitmap bitmap, int cropX, int cropY, int cropWidth,
            int cropHeight) {
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight);
        bitmap.recycle();
        return resizedBitmap;
    }

    /**
     * 图片重绘
     * 
     * @param bitmap
     *            位图
     * @param cropX
     *            切图X位置
     * @param cropY
     *            切图Y位置
     * @param cropWidth
     *            切图宽度
     * @param cropHeight
     *            切图高度
     * @param degrees
     *            旋转角度
     * @param scale
     *            缩放比例
     */
    public static final Bitmap redrawBitmap(Bitmap bitmap, int cropX, int cropY, int cropWidth,
            int cropHeight, int degrees, float scale) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        matrix.postScale(scale, scale);
        Bitmap resizedBitmap =
                Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight, matrix, false);
        bitmap.recycle();
        return resizedBitmap;
    }

    /**
     * 图片圆角
     * 
     * @param bitmap
     *            位图
     * @param round
     *            圆角弧度
     */
    public static Bitmap roundCorner(Bitmap bitmap, int round) {
        Bitmap output =
                Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawRoundRect(rectF, round, round, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    public static Bitmap getBitmapFromFile(File dst, int width, int height) {
        if (null != dst && dst.exists()) {
            BitmapFactory.Options opts = null;
            if (width > 0 && height > 0) {
                opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(dst.getPath(), opts);
                final int minSideLength = Math.min(width, height);
                opts.inSampleSize = computeSampleSize(opts, minSideLength, width * height);
                opts.inJustDecodeBounds = false;
                opts.inInputShareable = true;
                opts.inPurgeable = true;
            }
            return BitmapFactory.decodeFile(dst.getPath(), opts);
        }
        return null;
    }

    private static int computeSampleSize(BitmapFactory.Options options, int minSideLength,
            int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);

        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }

        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength,
            int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;

        int lowerBound =
                (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound =
                (minSideLength == -1) ? 128 : (int) Math.min(Math.floor(w / minSideLength),
                        Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound;
        }

        if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
            return 1;
        } else if (minSideLength == -1) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }
}
