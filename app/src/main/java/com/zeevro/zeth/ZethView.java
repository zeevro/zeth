package com.zeevro.zeth;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class ZethView extends View {
    public static final int CHEATER_COLOR = -10080767;
    public static final int ERROR_COLOR = 14487825;
    public static final int NORMAL_COLOR = -14671808;
    protected int CARDHEIGHT;
    protected int CARDWIDTH;
    protected int CORNER_ROUND;
    protected int ERROR_TIME = 500;
    protected int FLIP_TIME = 250;
    protected int GAP;
    protected int HIGHLIGHT = 3;
    protected int IMGHEIGHT = 11;
    protected int IMGWIDTH = 5;
    protected float LINE_WIDTH;
    protected int PADDING;
    protected long errorStarted;
    protected long flipStarted;
    protected GameState game;
    private RectF scratchRect = new RectF();
    protected Bitmap[] shapes;
    protected Vibrator vibrator;

    public ZethView(Context ctx, GameState gameState) {
        super(ctx);
        this.vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        this.game = gameState;
    }

    void setSizes(int w, int h) {
        this.GAP = (int) (((double) w) * 0.025d);
        this.PADDING = (int) (((double) w) * 0.015d);
        this.IMGWIDTH = ((w - ((game.getNumRows() + 1) * GAP)) - ((game.getNumRows() * (game.nvariants + 1)) * PADDING)) / (game.getNumRows() * game.nvariants);
        this.IMGHEIGHT = ((h - ((game.getMaxColumns() + 1) * GAP)) - ((game.getMaxColumns() * 2) * PADDING)) / game.getMaxColumns();
        if (IMGHEIGHT * 25 > IMGWIDTH * 54) {
            this.IMGHEIGHT = (IMGWIDTH * 54) / 25;
        } else if (IMGHEIGHT * 25 < IMGWIDTH * 54) {
            this.IMGWIDTH = (IMGHEIGHT * 25) / 54;
        }
        if (IMGHEIGHT < 11 || IMGWIDTH < 5) {
            this.IMGHEIGHT = 11;
            this.IMGWIDTH = 5;
        }
        this.CORNER_ROUND = w / 100;
        this.CARDWIDTH = (IMGWIDTH * game.nvariants) + (PADDING * (game.nvariants + 1));
        this.CARDHEIGHT = IMGHEIGHT + (PADDING * 2);
        this.HIGHLIGHT = CARDWIDTH / 16;
        if (HIGHLIGHT < 1) {
            HIGHLIGHT = 1;
        }
        this.LINE_WIDTH = IMGWIDTH / 7f;

        Log.d("Zeth", String.format("GAP=%d PADDING=%d IMG=%dx%d CORNER=%d CARD=%dx%d LINE_WIDTH=%.1f", GAP, PADDING, IMGWIDTH, IMGHEIGHT, CORNER_ROUND, CARDWIDTH, CARDHEIGHT, LINE_WIDTH));
    }

    public void loadImages(Context ctx) {
        int idx;

        setSizes(getWidth(), getHeight());

        Log.d("Zeth", "Begin image loading: " + System.currentTimeMillis());

        this.shapes = new Bitmap[(game.getNCards() / game.nvariants)];

        for (int fill = 0; fill < game.nvariants; fill++) {
            if (game.ntraits >= 4 || fill == 0) {
                float effectiveLineWidth = fill == 0 ? 0 : LINE_WIDTH;

                Path[] paths = {
                        Shapes.makeOval(0, 0, IMGWIDTH - 1, IMGHEIGHT - 1, fill, effectiveLineWidth),
                        Shapes.makeDiamond(0, 0, IMGWIDTH - 1, IMGHEIGHT - 1, fill, effectiveLineWidth),
                        Shapes.makeSquiggle(0, 0, IMGWIDTH - 1, IMGHEIGHT - 1, fill, effectiveLineWidth),
                        Shapes.makeRectangle(0, 0, IMGWIDTH - 1, IMGHEIGHT - 1, fill, effectiveLineWidth),
                };

                for (int color = 0; color < game.nvariants; color++) {
                    for (int shape = 0; shape < game.nvariants; shape++) {
                        switch (game.ntraits) {
                            case 3:
                                idx = (game.nvariants * color) + shape;
                                break;
                            case 4:
                                idx = (game.nvariants * color * game.nvariants) + (game.nvariants * fill) + shape;
                                break;
                            default:
                                throw new RuntimeException("not defined for ntraits=" + game.ntraits);
                        }
                        Bitmap dest = Bitmap.createBitmap(IMGWIDTH, IMGHEIGHT, Config.ARGB_8888);
                        this.shapes[idx] = dest;

                        Canvas c = new Canvas(dest);

                        Paint p = new Paint();
                        p.setAntiAlias(true);
                        p.setColor(game.colors[color]);
                        p.setStyle(fill == 0 ? Style.FILL : Style.STROKE);
                        p.setStrokeWidth(effectiveLineWidth);

                        c.drawPath(paths[shape], p);
                    }
                }
            }
        }

        Log.d("Zeth", "End image loading: " + System.currentTimeMillis());
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int selectedCard = -1;
        int action = ev.getAction() & 255;
        if (action == 0 || action == 5) {
            Point where = new Point();
            for (int finger = 0; finger < ev.getPointerCount(); finger++) {
                int mindist = Integer.MAX_VALUE;
                for (int i = 0; i < game.numDealt; i++) {
                    getCardPosition(i % game.getNumRows(), i / game.getNumRows(), where);
                    int dx = where.x + (CARDWIDTH / 2) - (int)ev.getX(finger);
                    int dy = where.y + (CARDHEIGHT / 2) - (int)ev.getY(finger);
                    int distSq = (dx * dx) + (dy * dy);
                    if (distSq < mindist) {
                        selectedCard = i;
                        mindist = distSq;
                    }
                }
            }
            if (selectedCard != -1) {
                if (game.vibrate_on_toggle) {
                    vibrator.vibrate(10);
                }
                game.ToggleCard(selectedCard);
                if (game.numSelected == game.nvariants) {
                    game.CheckSet(this);
                }
                invalidate();
            }
        }
        return true;
    }

    public void startFlip() {
        this.flipStarted = System.currentTimeMillis();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setStyle(Style.FILL);
        int bgcolor = game.cheatedDuringCurrentGame ? CHEATER_COLOR : NORMAL_COLOR;
        if (errorStarted > 0) {
            long now = System.currentTimeMillis();
            if (errorStarted > now) {
                this.errorStarted = now;
            }
            double errorPos = 1 - ((double)(now - errorStarted) / (double)ERROR_TIME);
            if (errorPos > 0) {
                bgcolor = Color.rgb(
                        (int)((((double) Color.red(ERROR_COLOR)) * errorPos) + (((double) Color.red(bgcolor)) * (1 - errorPos))),
                        (int)((((double) Color.green(ERROR_COLOR)) * errorPos) + (((double) Color.green(bgcolor)) * (1 - errorPos))),
                        (int)((((double) Color.blue(ERROR_COLOR)) * errorPos) + (((double) Color.blue(bgcolor)) * (1 - errorPos)))
                );
                invalidate();
            } else {
                this.errorStarted = 0;
            }
        }

        canvas.drawColor(bgcolor);

        double animateFrame = 0;
        if (game.animations && flipStarted > 0) {
            long now = System.currentTimeMillis();

            if (flipStarted > now) {
                this.flipStarted = now;
            }

            animateFrame = 1d - ((double)(now - flipStarted) / (double)FLIP_TIME);

            if (animateFrame > 0) {
                invalidate();
            } else {
                this.flipStarted = 0;
                animateFrame = 0;
            }
        }

        for (int i = 0; i < game.numDealt; i++) {
            int divisor = game.getNCards() / game.nvariants;
            drawCard(canvas, p, i % game.getNumRows(), i / game.getNumRows(), game.cardsDealt[i] % divisor, (game.cardsDealt[i] / divisor) + 1, game.selected[i], game.animate[i], animateFrame);
        }

        p.setColor(0xFFFFFFFF);
        p.setTextSize((float) ((int) (((double) getHeight()) * 0.035d)));

        ((Activity) getContext()).setTitle(getContext().getString(R.string.app_name) + ": " + String.format(getContext().getString(R.string.cards_remaining), game.getNCards() - game.deckPos));
        ((Activity) getContext()).setProgress(((game.deckPos - game.getMinBoard()) * 9999) / (game.getNCards() - game.getMinBoard()));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d("Zeth", "size changed from " + oldw + "x" + oldh + " to " + w + "x" + h);
        super.onSizeChanged(w, h, oldw, oldh);
        setSizes(w, h);
        loadImages(getContext());
    }

    Point getCardPosition(int x, int y, Point where) {
        int numcols = this.game.numDealt / this.game.getNumRows();
        int ygap = (getHeight() - (this.CARDHEIGHT * numcols)) / (numcols + 1);
        where.set(this.GAP + this.PADDING + ((this.GAP + this.CARDWIDTH) * x), this.PADDING + ygap + ((this.CARDHEIGHT + ygap) * y));
        return where;
    }

    protected void drawCard(Canvas canvas, Paint p, int x, int y, int img, int count, boolean highlight, int prevCard, double animateFrame) {
        int divisor = game.getDivisor();
        p.setStyle(Style.FILL);
        p.setStrokeWidth(1.0f);
        Point where = getCardPosition(x, y, new Point());
        double shrink = 0;
        if (highlight) {
            p.setColor(-5592406);
            scratchRect.set(where.x - PADDING - HIGHLIGHT, where.y - PADDING + HIGHLIGHT, where.x + CARDWIDTH - PADDING - HIGHLIGHT, where.y + CARDHEIGHT - PADDING + HIGHLIGHT);
            canvas.drawRoundRect(scratchRect, CORNER_ROUND, CORNER_ROUND, p);
            where.x += HIGHLIGHT;
            where.y -= HIGHLIGHT;
        } else if ((prevCard & GameState.ANIMATE_MASK) != 0 && animateFrame > 0) {
            shrink = 2.0d * animateFrame;
            if (shrink > 1) {
                img = (prevCard - 4096) % divisor;
                shrink = 2.0d - shrink;
            }
        }
        p.setColor(-1);
        int yshrink = (int) ((((double) CARDHEIGHT) * shrink) / 2d);
        scratchRect.set(where.x - PADDING, where.y - PADDING + yshrink, where.x + CARDWIDTH - PADDING, where.y + CARDHEIGHT - PADDING - yshrink);
        canvas.drawRoundRect(scratchRect, CORNER_ROUND, CORNER_ROUND, p);
        where.x += ((PADDING + IMGWIDTH) * (game.nvariants - count)) / 2;
        int yshrink2 = (int) ((((double) IMGHEIGHT) * shrink) / 2d);
        for (int i = 0; i < count; i++) {
            scratchRect.set(where.x, where.y + yshrink2, where.x + IMGWIDTH, where.y + IMGHEIGHT - yshrink2);
            canvas.drawBitmap(shapes[img % divisor], null, scratchRect, p);
            where.x += PADDING + IMGWIDTH;
        }
    }

    public void doError() {
        if (this.game.vibrate_on_error) {
            this.vibrator.vibrate(400);
        }
        this.errorStarted = System.currentTimeMillis();
        this.game.errors++;
    }
}
