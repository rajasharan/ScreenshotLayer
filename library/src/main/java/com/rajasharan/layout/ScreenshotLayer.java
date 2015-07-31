package com.rajasharan.layout;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.*;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by raja on 6/25/15.
 */
public class ScreenshotLayer extends ViewGroup implements Handler.Callback {
    private static final String TAG = "ScreenshotLayer";
    private static final String THREAD = "SCREENSHOT-THREAD";
    private static final int WHAT_SAVE_MESSAGE = 0;
    private static final int DELAY_TIMER = 1200;

    private static final int NO_EDGE = 10;
    private static final int RIGHT_EDGE = 11;
    private static final int LEFT_EDGE = 12;

    private static final int MODE_RESET = 100;
    private static final int MODE_INPROGRESS = 101;
    private static final int MODE_SNAP = 102;
    private static final int MODE_DELAY = 103;

    private int mScreenshotMode;
    private Paint mFramePaint;
    private Paint mPhotoPaint;
    private Paint mLayerPaint;
    private float mScale;
    private ScreenshotListener mListener;
    private Handler mHandler;
    private PointF mCurrentPoint;
    private PointF mMirrorPoint;
    private int mEdgeFlag;

    public ScreenshotLayer(Context context) {
        this(context, null);
    }

    public ScreenshotLayer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScreenshotLayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mScreenshotMode = MODE_RESET;
        mScale = 0.8f;

        mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint.setStyle(Paint.Style.STROKE);
        mFramePaint.setStrokeWidth(15f);

        mPhotoPaint = new Paint();
        mPhotoPaint.setAlpha(128);

        mLayerPaint = new Paint();
        mLayerPaint.setStyle(Paint.Style.FILL);
        mLayerPaint.setColor(Color.BLACK);
        mLayerPaint.setAlpha(75);

        initHandler();
    }

    private void initHandler() {
        HandlerThread t = new HandlerThread(THREAD);
        t.start();
        mHandler = new Handler(t.getLooper(), this);
    }

    public void setScreenshotListener(ScreenshotListener listener) {
        mListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        View view = getChildAt(0);
        view.measure(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View view = getChildAt(0);
        view.layout(l, t, r, b);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            float x = ev.getX();
            int edgeInsets = ViewConfiguration.get(getContext()).getScaledEdgeSlop() / 2;
            int width = getWidth();
            if (width - x <= edgeInsets || x <= edgeInsets) {
                mScreenshotMode = MODE_INPROGRESS;
                return true;
            }
        }
        mScreenshotMode = MODE_RESET;
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float w = getWidth();
        float h = getHeight();
        mCurrentPoint = new PointF(x, y);
        mMirrorPoint = new PointF(w-x, y);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (mScreenshotMode == MODE_INPROGRESS) {
                    if (x < w/2) {
                        mEdgeFlag = LEFT_EDGE;
                    } else {
                        mEdgeFlag = RIGHT_EDGE;
                    }
                } else {
                    mEdgeFlag = NO_EDGE;
                    return false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mScreenshotMode == MODE_INPROGRESS && mEdgeFlag != NO_EDGE) {
                    if (mEdgeFlag == LEFT_EDGE && x > w/2 || mEdgeFlag == RIGHT_EDGE && x < w/2) {
                        mScreenshotMode = MODE_SNAP;
                        mEdgeFlag = NO_EDGE;
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
            default:
                if (mScreenshotMode == MODE_SNAP || mScreenshotMode == MODE_DELAY) {
                    return false;
                }
                mScreenshotMode = MODE_RESET;
                mEdgeFlag = NO_EDGE;
                break;
        }
        invalidate();
        return true;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        switch (mScreenshotMode) {
            case MODE_RESET:
                super.dispatchDraw(canvas);
                break;
            case MODE_INPROGRESS:
                super.dispatchDraw(canvas);
                if (mEdgeFlag == LEFT_EDGE) {
                    canvas.drawRect(0f, 0f, mCurrentPoint.x, h, mLayerPaint);
                    canvas.drawRect(mMirrorPoint.x, 0, w, h, mLayerPaint);
                } else if (mEdgeFlag == RIGHT_EDGE) {
                    canvas.drawRect(mCurrentPoint.x, 0, w, h, mLayerPaint);
                    canvas.drawRect(0f, 0f, mMirrorPoint.x, h, mLayerPaint);
                }
                break;
            case MODE_SNAP:
                Bitmap bitmap = Bitmap.createBitmap((int)w, (int)h, Bitmap.Config.ARGB_8888);
                Canvas newCanvas = new Canvas(bitmap);
                super.dispatchDraw(newCanvas);
                mHandler.sendMessage(Message.obtain(mHandler, WHAT_SAVE_MESSAGE, bitmap));
                mScreenshotMode = MODE_DELAY;
                break;
            case MODE_DELAY:
                drawScaledBitmap(canvas, w, h);
        }
    }

    private void drawScaledBitmap(Canvas canvas, float w, float h) {
        int savepoint = canvas.save();
        canvas.scale(mScale, mScale);
        View view = getChildAt(0);
        view.setDrawingCacheEnabled(true);
        Bitmap bitmap = view.getDrawingCache(true);
        if (bitmap != null) {
            Rect rect = new Rect();
            view.getHitRect(rect);
            RectF rectF = new RectF(rect);
            rectF.offset((w - w*mScale)/2, (h - h*mScale)/2);
            canvas.drawRect(rectF, mFramePaint);
            canvas.drawBitmap(bitmap, rectF.left, rectF.top, mPhotoPaint);
        }
        canvas.restoreToCount(savepoint);
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.d(Thread.currentThread().getName(), msg.toString());
        switch (msg.what) {
            case WHAT_SAVE_MESSAGE:
                Bitmap bitmap = (Bitmap) msg.obj;
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                String name = "demo-" + SystemClock.elapsedRealtime() + ".png";
                final File file = new File(path, name);
                boolean result = false;
                String errormsg = "Bitmap Compression Failed";
                try {
                    path.mkdirs();
                    file.createNewFile();
                    FileOutputStream fOut = new FileOutputStream(file);
                    result = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                    fOut.close();
                    MediaScannerConnection.scanFile(getContext(), new String[]{file.getAbsolutePath()}, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            Log.d(TAG, path + " --> " + uri.toString());
                        }
                    });
                } catch (FileNotFoundException e) {
                    result = false;
                    errormsg = e.getMessage();
                    Log.e(THREAD, "FileNotFound: ", e);
                } catch (IOException e) {
                    result = false;
                    errormsg = e.getMessage();
                    Log.e(Thread.currentThread().getName(), "IOException: ", e);
                }

                final boolean finalResult = result;
                final String finalErrorMsg = errormsg;
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (finalResult) {
                            mListener.onScreenshotSaved(file.getAbsolutePath());
                            playSoundEffect(SoundEffectConstants.CLICK);
                        } else {
                            mListener.onScreenshotError(finalErrorMsg);
                        }
                        mScreenshotMode = MODE_RESET;
                        invalidate();
                    }
                }, DELAY_TIMER);
                break;
        }
        return true;
    }

    public interface ScreenshotListener {
        void onScreenshotSaved(String path);
        void onScreenshotError(String reason);
    }
}
