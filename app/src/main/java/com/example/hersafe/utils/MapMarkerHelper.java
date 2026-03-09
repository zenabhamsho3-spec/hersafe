package com.example.hersafe.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.io.File;

/**
 * Helper utility to create a circular profile photo map marker.
 */
public class MapMarkerHelper {

    /**
     * Creates a circular BitmapDescriptor from a profile photo path.
     * If no photo is found, generates a default pink circle with an icon.
     */
    public static BitmapDescriptor createProfileMarker(Context context, String photoPath) {
        // Try to load the profile photo
        if (photoPath != null && !photoPath.isEmpty()) {
            File photoFile = new File(photoPath);
            if (photoFile.exists()) {
                Bitmap original = BitmapFactory.decodeFile(photoPath);
                if (original != null) {
                    return BitmapDescriptorFactory.fromBitmap(makeCircularBitmap(original));
                }
            }
        }

        // Fallback: Default Pink Avatar with Initials (or the app icon)
        return createDefaultPinkMarker(context);
    }

    private static Bitmap makeCircularBitmap(Bitmap original) {
        int size = 120; // px size on the map
        Bitmap output = Bitmap.createBitmap(size + 8, size + 8, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // White border ring
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f + 4, size / 2f + 4, size / 2f + 4, borderPaint);

        // Pink/purple shadow ring
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(0xFFE91E63); // pink
        canvas.drawCircle(size / 2f + 4, size / 2f + 4, size / 2f + 2, shadowPaint);

        // Circular clip
        Paint clipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawCircle(size / 2f + 4, size / 2f + 4, size / 2f, clipPaint);

        clipPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        // Scale the image to fit the circle
        Bitmap scaled = Bitmap.createScaledBitmap(original, size, size, true);
        canvas.drawBitmap(scaled, 4, 4, clipPaint);

        return output;
    }

    private static BitmapDescriptor createDefaultPinkMarker(Context context) {
        int size = 100;
        Bitmap output = Bitmap.createBitmap(size + 8, size + 8, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // White outer ring
        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f + 4, size / 2f + 4, size / 2f + 4, border);

        // Pink circle fill
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(0xFFE91E63);
        canvas.drawCircle(size / 2f + 4, size / 2f + 4, size / 2f, fill);

        // Pink lock icon (text stand-in)
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("📍", size / 2f + 4, size / 2f + 14 + 4, textPaint);

        return BitmapDescriptorFactory.fromBitmap(output);
    }
}
