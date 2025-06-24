package com.qiscus.mychatui.util;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.ReplacementSpan;

public class TranslateYSpan extends ReplacementSpan {

    private final int offsetY;

    public TranslateYSpan(int offsetY) {
        this.offsetY = offsetY;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return (int) paint.measureText(text.subSequence(start, end).toString());
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end,
                     float x, int top, int y, int bottom, Paint paint) {
        canvas.save();
        canvas.translate(x, offsetY);
        canvas.drawText(text, start, end, 0, y, paint);
        canvas.restore();
    }
}
