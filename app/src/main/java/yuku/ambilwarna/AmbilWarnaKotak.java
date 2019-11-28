package yuku.ambilwarna;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.View;

import com.zeevro.zeth.R;

public class AmbilWarnaKotak extends View {
    Shader dalam;
    float hue;
    Shader luar;
    Paint paint;
    float satudp;
    float[] tmp00;
    float ukuranUiDp;
    float ukuranUiPx;

    public AmbilWarnaKotak(Context context) {
        this(context, null);
    }

    public AmbilWarnaKotak(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AmbilWarnaKotak(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.ukuranUiDp = 240.0f;
        this.tmp00 = new float[3];
        this.satudp = context.getResources().getDimension(R.dimen.ambilwarna_satudp);
        this.ukuranUiPx = this.ukuranUiDp * this.satudp;
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.paint == null) {
            this.paint = new Paint();
            this.luar = new LinearGradient(0.0f, 0.0f, 0.0f, this.ukuranUiPx, -1, -16777216, TileMode.CLAMP);
        }
        float[] fArr = this.tmp00;
        this.tmp00[2] = 1.0f;
        fArr[1] = 1.0f;
        this.tmp00[0] = this.hue;
        this.dalam = new LinearGradient(0.0f, 0.0f, this.ukuranUiPx, 0.0f, -1, Color.HSVToColor(this.tmp00), TileMode.CLAMP);
        this.paint.setShader(new ComposeShader(this.luar, this.dalam, Mode.MULTIPLY));
        canvas.drawRect(0.0f, 0.0f, this.ukuranUiPx, this.ukuranUiPx, this.paint);
    }

    /* access modifiers changed from: 0000 */
    public void setHue(float hue2) {
        this.hue = hue2;
        invalidate();
    }
}
