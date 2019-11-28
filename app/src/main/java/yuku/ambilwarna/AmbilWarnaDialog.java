package yuku.ambilwarna;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AbsoluteLayout.LayoutParams;
import android.widget.ImageView;

import com.zeevro.zeth.R;

public class AmbilWarnaDialog {
    private static final String TAG = AmbilWarnaDialog.class.getSimpleName();
    AlertDialog dialog;
    float hue;
    OnAmbilWarnaListener listener;
    ImageView panah;
    float sat;
    float satudp;
    float[] tmp01 = new float[3];
    float ukuranUiDp = 240.0f;
    float ukuranUiPx;
    float val;
    View viewHue;
    ImageView viewKeker;
    AmbilWarnaKotak viewKotak;
    View viewWarnaBaru;
    View viewWarnaLama;
    int warnaBaru;
    int warnaLama;

    public interface OnAmbilWarnaListener {
        void onCancel(AmbilWarnaDialog ambilWarnaDialog);

        void onOk(AmbilWarnaDialog ambilWarnaDialog, int i);
    }

    public AmbilWarnaDialog(Context context, int color, OnAmbilWarnaListener listener2) {
        this.listener = listener2;
        this.warnaLama = color;
        this.warnaBaru = color;
        Color.colorToHSV(color, this.tmp01);
        this.hue = this.tmp01[0];
        this.sat = this.tmp01[1];
        this.val = this.tmp01[2];
        this.satudp = context.getResources().getDimension(R.dimen.ambilwarna_satudp);
        this.ukuranUiPx = this.ukuranUiDp * this.satudp;
        Log.d(TAG, "satudp = " + this.satudp + ", ukuranUiPx=" + this.ukuranUiPx);
        View view = LayoutInflater.from(context).inflate(R.layout.ambilwarna_dialog, null);
        this.viewHue = view.findViewById(R.id.ambilwarna_viewHue);
        this.viewKotak = view.findViewById(R.id.ambilwarna_viewKotak);
        this.panah = view.findViewById(R.id.ambilwarna_panah);
        this.viewWarnaLama = view.findViewById(R.id.ambilwarna_warnaLama);
        this.viewWarnaBaru = view.findViewById(R.id.ambilwarna_warnaBaru);
        this.viewKeker = view.findViewById(R.id.ambilwarna_keker);
        letakkanPanah();
        letakkanKeker();
        this.viewKotak.setHue(this.hue);
        this.viewWarnaLama.setBackgroundColor(color);
        this.viewWarnaBaru.setBackgroundColor(color);
        this.viewHue.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() != 2 && event.getAction() != 0 && event.getAction() != 1) {
                    return false;
                }
                float y = event.getY();
                if (y < 0.0f) {
                    y = 0.0f;
                }
                if (y > AmbilWarnaDialog.this.ukuranUiPx) {
                    y = AmbilWarnaDialog.this.ukuranUiPx - 0.001f;
                }
                AmbilWarnaDialog.this.hue = 360.0f - ((360.0f / AmbilWarnaDialog.this.ukuranUiPx) * y);
                if (AmbilWarnaDialog.this.hue == 360.0f) {
                    AmbilWarnaDialog.this.hue = 0.0f;
                }
                AmbilWarnaDialog.this.warnaBaru = AmbilWarnaDialog.this.hitungWarna();
                AmbilWarnaDialog.this.viewKotak.setHue(AmbilWarnaDialog.this.hue);
                AmbilWarnaDialog.this.letakkanPanah();
                AmbilWarnaDialog.this.viewWarnaBaru.setBackgroundColor(AmbilWarnaDialog.this.warnaBaru);
                return true;
            }
        });
        this.viewKotak.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() != 2 && event.getAction() != 0 && event.getAction() != 1) {
                    return false;
                }
                float x = event.getX();
                float y = event.getY();
                if (x < 0.0f) {
                    x = 0.0f;
                }
                if (x > AmbilWarnaDialog.this.ukuranUiPx) {
                    x = AmbilWarnaDialog.this.ukuranUiPx;
                }
                if (y < 0.0f) {
                    y = 0.0f;
                }
                if (y > AmbilWarnaDialog.this.ukuranUiPx) {
                    y = AmbilWarnaDialog.this.ukuranUiPx;
                }
                AmbilWarnaDialog.this.sat = (1.0f / AmbilWarnaDialog.this.ukuranUiPx) * x;
                AmbilWarnaDialog.this.val = 1.0f - ((1.0f / AmbilWarnaDialog.this.ukuranUiPx) * y);
                AmbilWarnaDialog.this.warnaBaru = AmbilWarnaDialog.this.hitungWarna();
                AmbilWarnaDialog.this.letakkanKeker();
                AmbilWarnaDialog.this.viewWarnaBaru.setBackgroundColor(AmbilWarnaDialog.this.warnaBaru);
                return true;
            }
        });
        this.dialog = new Builder(context).setView(view).setPositiveButton(R.string.ambilwarna_ok, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (AmbilWarnaDialog.this.listener != null) {
                    AmbilWarnaDialog.this.listener.onOk(AmbilWarnaDialog.this, AmbilWarnaDialog.this.warnaBaru);
                }
            }
        }).setNegativeButton(R.string.ambilwarna_cancel, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (AmbilWarnaDialog.this.listener != null) {
                    AmbilWarnaDialog.this.listener.onCancel(AmbilWarnaDialog.this);
                }
            }
        }).create();
    }

    /* access modifiers changed from: protected */
    public void letakkanPanah() {
        float y = this.ukuranUiPx - ((this.hue * this.ukuranUiPx) / 360.0f);
        if (y == this.ukuranUiPx) {
            y = 0.0f;
        }
        LayoutParams layoutParams = (LayoutParams) this.panah.getLayoutParams();
        layoutParams.y = (int) (4.0f + y);
        this.panah.setLayoutParams(layoutParams);
    }

    /* access modifiers changed from: protected */
    public void letakkanKeker() {
        float y = (1.0f - this.val) * this.ukuranUiPx;
        LayoutParams layoutParams = (LayoutParams) this.viewKeker.getLayoutParams();
        layoutParams.x = (int) ((this.sat * this.ukuranUiPx) + 3.0f);
        layoutParams.y = (int) (y + 3.0f);
        this.viewKeker.setLayoutParams(layoutParams);
    }

    /* access modifiers changed from: private */
    public int hitungWarna() {
        this.tmp01[0] = this.hue;
        this.tmp01[1] = this.sat;
        this.tmp01[2] = this.val;
        return Color.HSVToColor(this.tmp01);
    }

    public void show() {
        this.dialog.show();
    }
}
