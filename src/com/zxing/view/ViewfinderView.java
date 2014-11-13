/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zxing.view;

import java.util.Collection;
import java.util.HashSet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.google.zxing.ResultPoint;
import com.zxing.camera.CameraManager;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 */
public final class ViewfinderView extends View {

    private static final String TAG = ViewfinderView.class.getSimpleName();
    /** 刷新界面的时间 */
    private static final long ANIMATION_DELAY = 10L;
    private static final int OPAQUE = 0xFF;

    /** 手机的屏幕密度 */
    private static float density;
    private static int SCREEN_WIDTH;
    private static int SCREEN_HEIGHT;
    /** 四个边角对应的长度 */
    private int CORNER_HEIGHT = 20;

    /** 四个边角对应的宽度 */
    private final int CORNER_WIDTH = 5;

    /** 扫描框中的中间线的宽 */
    private final int MIDDLE_LINE_WIDTH = 5;

    /** 扫描框中的中间线的与扫描框左右的间隙 */
    private final int MIDDLE_LINE_PADDING = 5;

    /** 中间那条线每次刷新移动的距离 */
    private int SPEEN_DISTANCE = 5;

    /** 扫描提示文字 */
    private final String TIPS = "将二维码放在取景框内，即可自动扫描 ";


    /** 字体大小 */
    private int TEXT_SIZE = 16;
    /** 字体距离扫描框下面的距离 */
    private int TEXT_PADDING_TOP = 40;

    /** 画笔对象的引用 */
    private Paint paint;

    /** 中间滑动线的最顶端位置 */
    private int slideTop;
    private boolean isFirst;

    private Bitmap resultBitmap;
	private final int maskColor = 0x60000000;
	private final int resultColor = 0xb0000000;

	private final int resultPointColor = 0xc0ffff00;
    private Collection<ResultPoint> possibleResultPoints;
    private Collection<ResultPoint> lastPossibleResultPoints;


    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initData(context);
        paint = new Paint();

        possibleResultPoints = new HashSet<ResultPoint>(5);
    }

    private void initData(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        SCREEN_WIDTH = dm.widthPixels;
        SCREEN_HEIGHT = dm.heightPixels;
        density = dm.density;
		Log.i(TAG, "屏幕宽高：" + SCREEN_WIDTH + " X " + SCREEN_HEIGHT);
		Log.i(TAG, "屏幕分辨率:" + density);
        // 将像素转换成dp
        CORNER_HEIGHT = (int) (CORNER_HEIGHT * density);
        TEXT_SIZE = (int) (TEXT_SIZE * density);
        TEXT_PADDING_TOP = (int) (TEXT_PADDING_TOP * density);

    }

    @Override
    public void onDraw(Canvas canvas) {
        // 中间的扫描框，你要修改扫描框的大小，去CameraManager里面修改
        Rect frame = CameraManager.get().getFramingRect();
        if (frame == null) {
            return;
        }

        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        // 获取屏幕的宽和高
        int width = canvas.getWidth();
        int height = canvas.getHeight();
		Log.i(TAG, "画布宽高：" + width + " X " + height);
		Log.i(TAG, "扫描框信息，左上右下：" + "(" + frame.left + " L," + frame.top + " T,"
				+ frame.right + " R,"
                + frame.bottom + " B" + ")");

        drawMaskArea(canvas, frame, width, height);
        draw4CornerArea(canvas, frame);
//        drawSlideMidLine(canvas, frame);
        drawTipsTxArea(canvas, frame, width);

        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(OPAQUE);
            canvas.drawBitmap(resultBitmap, frame.left, frame.top, paint);
        } else {
            drawResultPointArea(canvas, frame);
        }
        // 只刷新扫描框的内容，其他地方不刷新
//        postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right, frame.bottom);
    }

    /**
     * 绘画阴影区域
     * 
     * @param canvas
     * @param frame
     * @param width
     * @param height
     */
    private void drawMaskArea(Canvas canvas, Rect frame, int width, int height) {
		Log.i(TAG, "-->绘画阴影区域");
        // 画出扫描框外面的阴影部分，共四个部分，扫描框的上面到屏幕上面，扫描框的下面到屏幕下面
        // 扫描框的左边面到屏幕左边，扫描框的右边到屏幕右边
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);
    }

    /**
     * 绘画4个拐角
     * 
     * @param canvas
     * @param frame
     */
    private void draw4CornerArea(Canvas canvas, Rect frame) {
		Log.i(TAG, "-->绘画4个拐角");
        int pad = CORNER_WIDTH + (int) (10 * density);
        Rect frame1 = new Rect(frame.left - pad, frame.top - pad, frame.right + pad, frame.bottom + pad);
        // 画扫描框边上的角，总共8个部分
        paint.setColor(Color.LTGRAY);
        // 左上上
        canvas.drawRect(frame1.left, frame1.top, frame1.left + CORNER_HEIGHT, frame1.top + CORNER_WIDTH, paint);
        // 左上左
        canvas.drawRect(frame1.left, frame1.top, frame1.left + CORNER_WIDTH, frame1.top + CORNER_HEIGHT, paint);
        // 右上上
        canvas.drawRect(frame1.right - CORNER_HEIGHT, frame1.top, frame1.right, frame1.top + CORNER_WIDTH, paint);
        // 右上右
        canvas.drawRect(frame1.right - CORNER_WIDTH, frame1.top, frame1.right, frame1.top + CORNER_HEIGHT, paint);
        // 左下下
        canvas.drawRect(frame1.left, frame1.bottom - CORNER_WIDTH, frame1.left + CORNER_HEIGHT, frame1.bottom, paint);
        // 左下左
        canvas.drawRect(frame1.left, frame1.bottom - CORNER_HEIGHT, frame1.left + CORNER_WIDTH, frame1.bottom, paint);
        // 右下下
        canvas.drawRect(frame1.right - CORNER_HEIGHT, frame1.bottom - CORNER_WIDTH, frame1.right, frame1.bottom, paint);
        // 右下右
        canvas.drawRect(frame1.right - CORNER_WIDTH, frame1.bottom - CORNER_HEIGHT, frame1.right, frame1.bottom, paint);
    }

    /**
     * 绘画移动刷新的中线
     * 
     * @param canvas
     * @param frame
     */
    private void drawSlideMidLine(Canvas canvas, Rect frame) {
        // 绘制中间的线,每次刷新界面，中间的线往下移动SPEEN_DISTANCE
		Log.i(TAG, "-->绘画刷新中线");

        if (!isFirst) {
            isFirst = true;
            slideTop = frame.top;
        }
        slideTop += SPEEN_DISTANCE;
        if (slideTop >= frame.bottom) {
            slideTop = frame.top;
        }
        canvas.drawRect(frame.left + MIDDLE_LINE_PADDING, slideTop - MIDDLE_LINE_WIDTH / 2, frame.right
                - MIDDLE_LINE_PADDING, slideTop + MIDDLE_LINE_WIDTH / 2, paint);
    }

    /**
     * 绘画提示文字区域
     * 
     * @param canvas
     * @param frame
     * @param width
     */
    private void drawTipsTxArea(Canvas canvas, Rect frame, int width) {
        // 画扫描框下面的字
		Log.i(TAG, "绘画提示文字");
        paint.setColor(Color.WHITE);
        paint.setTextSize(TEXT_SIZE);
        paint.setAlpha(0x40);
//        paint.setTypeface(Typeface.DEFAULT_BOLD);
        float textWidth = paint.measureText(TIPS);

		Log.i(TAG, "提示文字长度" + textWidth);
        float x = (width - textWidth) / 2;
        float y = (float) (frame.bottom + (float) TEXT_PADDING_TOP);
		Log.i(TAG, "提示文字位置：" + x + " X " + y);

        int pad = (int) (10 * density);
        Paint paint1 = new Paint();
        paint1.setColor(Color.GRAY);
        paint1.setAlpha(0X40);
        canvas.drawRect(x - pad, y - pad, x + textWidth + pad, y + pad + TEXT_SIZE, paint1);
        canvas.drawText(TIPS, x, y + (pad + TEXT_SIZE) / 2, paint);
    }

    /**
     * 绘画可能的结果点
     * 
     * @param canvas
     * @param frame
     */
    private void drawResultPointArea(Canvas canvas, Rect frame) {
        Collection<ResultPoint> currentPossible = possibleResultPoints;
        Collection<ResultPoint> currentLast = lastPossibleResultPoints;
        if (currentPossible.isEmpty()) {
            lastPossibleResultPoints = null;
        } else {
            possibleResultPoints = new HashSet<ResultPoint>(5);
            lastPossibleResultPoints = currentPossible;
            paint.setAlpha(OPAQUE);
            paint.setColor(resultPointColor);
            for (ResultPoint point : currentPossible) {
                canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 6.0f, paint);
            }
        }
        if (currentLast != null) {
            paint.setAlpha(OPAQUE / 2);
            paint.setColor(resultPointColor);
            for (ResultPoint point : currentLast) {
                canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 3.0f, paint);
            }
        }
    }

    public void drawViewfinder() {
        resultBitmap = null;
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live
     * scanning display.
     * 
     * @param barcode
     *            An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        possibleResultPoints.add(point);
    }

}
