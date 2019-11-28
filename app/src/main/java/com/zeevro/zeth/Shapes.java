package com.zeevro.zeth;

import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.RectF;
import android.util.Log;

class Shapes {
    private static final int FILL1_STRIPES = 4;
    private static final int FILL3_STRIPES = 2;
    private static float[] points = {0.17f, 0.06f, 0.1f, 0.4f, 0.3f, 0.73f, 0.0f, 0.87f, 0.2f, 0.98f};
    private static float[] shadeHeight = {1008981770, 1063339950, 1038174126, 1060152279};
    private static float[] vector = {-0.15f, 0.07f, 0.2f, 0.2f, -0.09f, 0.06f, 0.0f, 0.07f, 0.1f, 0.03f};

    static Path makeOval(float left, float top, float right, float bottom, int fill, float lineWidth) {
        Path pa = new Path();
        float left2 = left + (lineWidth / 2.0f);
        float right2 = right - (lineWidth / 2.0f);
        float top2 = top + (lineWidth / 2.0f);
        float bottom2 = bottom - (lineWidth / 2.0f);
        float w = right2 - left2;
        float h = bottom2 - top2;
        pa.moveTo(right2, (w / 2.0f) + top2);
        pa.lineTo(right2, bottom2 - (w / 2.0f));
        pa.arcTo(new RectF(left2, bottom2 - w, right2, bottom2), 0.0f, 180.0f);
        pa.lineTo(left2, (w / 2.0f) + top2);
        pa.arcTo(new RectF(left2, top2, right2, top2 + w), 180.0f, 180.0f);
        if (fill == 1) {
            for (int i = 1; i <= FILL1_STRIPES; i++) {
                pa.moveTo(left2, ((((float) i) * h) / 5.0f) + top2);
                pa.lineTo(right2, ((((float) i) * h) / 5.0f) + top2);
            }
        } else if (fill == 3) {
            for (int i2 = 1; i2 <= FILL3_STRIPES; i2++) {
                pa.moveTo(((((float) i2) * w) / 3.0f) + left2, top2);
                pa.lineTo(((((float) i2) * w) / 3.0f) + left2, bottom2);
            }
        }
        return pa;
    }

    static Path makeRectangle(float left, float top, float right, float bottom, int fill, float lineWidth) {
        Path pa = new Path();
        float left2 = left + (lineWidth / 2.0f);
        float right2 = right - (lineWidth / 2.0f);
        float top2 = top + (lineWidth / 2.0f);
        float bottom2 = bottom - (lineWidth / 2.0f);
        float w = right2 - left2;
        float h = bottom2 - top2;
        pa.addRect(left2, top2, right2, bottom2, Direction.CW);
        if (fill == 1) {
            for (int i = 1; i <= FILL1_STRIPES; i++) {
                pa.moveTo(left2, ((((float) i) * h) / 5.0f) + top2);
                pa.lineTo(right2, ((((float) i) * h) / 5.0f) + top2);
            }
        } else if (fill == 3) {
            for (int i2 = 1; i2 <= FILL3_STRIPES; i2++) {
                pa.moveTo(((((float) i2) * w) / 3.0f) + left2, top2);
                pa.lineTo(((((float) i2) * w) / 3.0f) + left2, bottom2);
            }
        }
        return pa;
    }

    static Path makeDiamond(float left, float top, float right, float bottom, int fill, float lineWidth) {
        Path pa = new Path();
        float w = right - left;
        float h = bottom - top;
        double angle = Math.atan((double) (h / w));
        float capX = (lineWidth / ((float) Math.sin(angle))) / 2.0f;
        float capY = (lineWidth / ((float) Math.cos(angle))) / 2.0f;
        Object[] objArr = new Object[5];
        objArr[0] = Float.valueOf(w);
        objArr[1] = Float.valueOf(h);
        objArr[FILL3_STRIPES] = Double.valueOf(angle);
        objArr[3] = Float.valueOf(capX);
        objArr[FILL1_STRIPES] = Float.valueOf(capY);
        Log.i("Zeth", String.format("w=%.0f h=%.0f angle=%.2f capX=%.2f capY=%.2f", objArr));
        float mx = (left + right) / 2.0f;
        float my = (top + bottom) / 2.0f;
        pa.moveTo(left + capX, my);
        pa.lineTo(mx, top + capY);
        pa.lineTo(right - capX, my);
        pa.lineTo(mx, bottom - capY);
        pa.lineTo(left + capX, my);
        pa.lineTo(mx, top + capY);
        if (fill == 1) {
            for (int i = 1; i <= FILL1_STRIPES; i++) {
                float hspace = ((w / 10.0f) * ((float) Math.abs(5 - (i * FILL3_STRIPES)))) + capX;
                pa.moveTo(left + hspace, ((((float) i) * h) / 5.0f) + top);
                pa.lineTo(right - hspace, ((((float) i) * h) / 5.0f) + top);
            }
        } else if (fill == 3) {
            for (int i2 = 1; i2 <= FILL3_STRIPES; i2++) {
                float vspace = ((h / 6.0f) * ((float) Math.abs(3 - (i2 * FILL3_STRIPES)))) + capY;
                pa.moveTo(((((float) i2) * w) / 3.0f) + left, top + vspace);
                pa.lineTo(((((float) i2) * w) / 3.0f) + left, bottom - vspace);
            }
        }
        return pa;
    }

    static Path makeSquiggle(float left, float top, float right, float bottom, int fill, float lineWidth) {
        Path pa = new Path();
        float left2 = left + (lineWidth / 2.0f);
        float right2 = right - (lineWidth / 2.0f);
        float top2 = top + (lineWidth / 2.0f);
        float bottom2 = bottom - (lineWidth / 2.0f);
        float w = right2 - left2;
        float h = bottom2 - top2;
        pa.moveTo((points[0] * w) + left2, (points[1] * h) + top2);
        int i = 0;
        while (i + FILL3_STRIPES < points.length) {
            pa.cubicTo(((points[i] + vector[i]) * w) + left2, ((points[i + 1] + vector[i + 1]) * h) + top2, ((points[i + FILL3_STRIPES] - vector[i + FILL3_STRIPES]) * w) + left2, ((points[i + 3] - vector[i + 3]) * h) + top2, (points[i + FILL3_STRIPES] * w) + left2, (points[i + 3] * h) + top2);
            i += FILL3_STRIPES;
        }
        pa.cubicTo(((points[i] + vector[i]) * w) + left2, ((points[i + 1] + vector[i + 1]) * h) + top2, right2 - ((points[0] - vector[0]) * w), bottom2 - ((points[1] - vector[1]) * h), right2 - (points[0] * w), bottom2 - (points[1] * h));
        int i2 = 0;
        while (i2 + FILL3_STRIPES < points.length) {
            pa.cubicTo(right2 - ((points[i2] + vector[i2]) * w), bottom2 - ((points[i2 + 1] + vector[i2 + 1]) * h), right2 - ((points[i2 + FILL3_STRIPES] - vector[i2 + FILL3_STRIPES]) * w), bottom2 - ((points[i2 + 3] - vector[i2 + 3]) * h), right2 - (points[i2 + FILL3_STRIPES] * w), bottom2 - (points[i2 + 3] * h));
            i2 += FILL3_STRIPES;
        }
        pa.cubicTo(right2 - ((points[i2] + vector[i2]) * w), bottom2 - ((points[i2 + 1] + vector[i2 + 1]) * h), ((points[0] - vector[0]) * w) + left2, ((points[1] - vector[1]) * h) + top2, (points[0] * w) + left2, (points[1] * h) + top2);
        if (fill == 1) {
            for (int i3 = 0; i3 < FILL3_STRIPES; i3++) {
                pa.moveTo((shadeHeight[i3 * FILL3_STRIPES] * w) + left2, ((((float) (i3 + 1)) * h) / 5.0f) + top2);
                pa.lineTo((shadeHeight[(i3 * FILL3_STRIPES) + 1] * w) + left2, ((((float) (i3 + 1)) * h) / 5.0f) + top2);
                pa.moveTo(right2 - (shadeHeight[(i3 * FILL3_STRIPES) + 1] * w), bottom2 - ((((float) (i3 + 1)) * h) / 5.0f));
                pa.lineTo(right2 - (shadeHeight[i3 * FILL3_STRIPES] * w), bottom2 - ((((float) (i3 + 1)) * h) / 5.0f));
            }
        } else if (fill == 3) {
            for (int i4 = 1; i4 <= FILL3_STRIPES; i4++) {
                pa.moveTo(((((float) i4) * w) / 3.0f) + left2, top2);
                pa.lineTo(((((float) i4) * w) / 3.0f) + left2, bottom2);
            }
        }
        return pa;
    }
}
