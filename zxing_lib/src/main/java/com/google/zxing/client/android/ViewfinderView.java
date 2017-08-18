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

package com.google.zxing.client.android;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

  private static final int[] SCANNER_ALPHA = { 0, 64, 128, 192, 255, 192,
          128, 64 };
  private static final long ANIMATION_DELAY = 100L;
  private static final int OPAQUE = 0xFF;

  private final Paint paint;
  private Bitmap resultBitmap;
  private final int maskColor;
  private final int resultColor;
  private final int frameColor;
  private final int laserColor;
  private final int resultPointColor;
  private int scannerAlpha;
  private CameraManager cameraManager;
  private Collection<ResultPoint> possibleResultPoints;
  private Collection<ResultPoint> lastPossibleResultPoints;

  /**
   * �ֻ�����Ļ�ܶ�
   */
  private static float density;

  /**
   * �ĸ���ɫ�߽Ƕ�Ӧ�ĳ���
   */
  private int ScreenRate;

  /**
   * �ĸ���ɫ�߽Ƕ�Ӧ�Ŀ��
   */
  private static final int CORNER_WIDTH = 10;

  /**
   * �м�������ÿ��ˢ���ƶ��ľ���
   */
  private static final int SPEEN_DISTANCE = 5;

  /**
   * �м们���ߵ����λ��
   */
  private int slideTop;

  /**
   * �м们���ߵ���׶�λ��
   */
  private int slideBottom;

  /**
   * ɨ����е��м��ߵĿ��
   */
  private static final int MIDDLE_LINE_WIDTH = 6;

  /**
   * ɨ����е��м��ߵ���ɨ������ҵļ�϶
   */
  private static final int MIDDLE_LINE_PADDING = 5;

  private boolean isFirst;

  // This constructor is used when the class is built from an XML resource.
  public ViewfinderView(Context context, AttributeSet attrs) {
    super(context, attrs);

    density = context.getResources().getDisplayMetrics().density;
    // ������ת����dp
    ScreenRate = (int) (20 * density);

    paint = new Paint();
    Resources resources = getResources();
    maskColor = resources.getColor(R.color.viewfinder_mask);
    resultColor = resources.getColor(R.color.result_view);
    frameColor = resources.getColor(R.color.result_points);
    laserColor = resources.getColor(R.color.result_points);
    resultPointColor = resources.getColor(R.color.possible_result_points);
    scannerAlpha = 0;
    possibleResultPoints = new HashSet<ResultPoint>(5);
  }
  public void setCameraManager(CameraManager cameraManager) {
    this.cameraManager = cameraManager;
  }
  @Override
  public void onDraw(Canvas canvas) {
    Rect frame = cameraManager.getFramingRect();
    if (frame == null) {
      return;
    }

    // ��ʼ���м��߻��������ϱߺ����±�
    if (!isFirst) {
      isFirst = true;
      slideTop = frame.top;
      slideBottom = frame.bottom;
    }

    int width = canvas.getWidth();
    int height = canvas.getHeight();

    // Draw the exterior (i.e. outside the framing rect) darkened
    paint.setColor(resultBitmap != null ? resultColor : maskColor);
    canvas.drawRect(0, 0, width, frame.top, paint);
    canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
    canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1,
            paint);
    canvas.drawRect(0, frame.bottom + 1, width, height, paint);

    if (resultBitmap != null) {
      // Draw the opaque result bitmap over the scanning rectangle
      paint.setAlpha(OPAQUE);
      canvas.drawBitmap(resultBitmap, frame.left, frame.top, paint);
    } else {

      // ��ɨ���
      paint.setColor(frameColor);
      canvas.drawRect(frame.left, frame.top, frame.right + 1,
              frame.top + 2, paint);
      canvas.drawRect(frame.left, frame.top + 2, frame.left + 2,
              frame.bottom - 1, paint);
      canvas.drawRect(frame.right - 1, frame.top, frame.right + 1,
              frame.bottom - 1, paint);
      canvas.drawRect(frame.left, frame.bottom - 1, frame.right + 1,
              frame.bottom + 1, paint);

      paint.setColor(laserColor);

      // ��ɨ�����ϵĽǣ��ܹ�8������
      canvas.drawRect(frame.left, frame.top, frame.left + ScreenRate,
              frame.top + CORNER_WIDTH, paint);
      canvas.drawRect(frame.left, frame.top, frame.left + CORNER_WIDTH,
              frame.top + ScreenRate, paint);
      canvas.drawRect(frame.right - ScreenRate, frame.top, frame.right,
              frame.top + CORNER_WIDTH, paint);
      canvas.drawRect(frame.right - CORNER_WIDTH, frame.top, frame.right,
              frame.top + ScreenRate, paint);
      canvas.drawRect(frame.left, frame.bottom - CORNER_WIDTH, frame.left
              + ScreenRate, frame.bottom, paint);
      canvas.drawRect(frame.left, frame.bottom - ScreenRate, frame.left
              + CORNER_WIDTH, frame.bottom, paint);
      canvas.drawRect(frame.right - ScreenRate, frame.bottom
              - CORNER_WIDTH, frame.right, frame.bottom, paint);
      canvas.drawRect(frame.right - CORNER_WIDTH, frame.bottom
              - ScreenRate, frame.right, frame.bottom, paint);

      paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
      scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;

      // �����м����,ÿ��ˢ�½��棬�м���������ƶ�SPEEN_DISTANCE
      slideTop += SPEEN_DISTANCE;
      if (slideTop >= frame.bottom) {
        slideTop = frame.top;
      }
      canvas.drawRect(frame.left + MIDDLE_LINE_PADDING, slideTop
                      - MIDDLE_LINE_WIDTH / 2, frame.right - MIDDLE_LINE_PADDING,
              slideTop + MIDDLE_LINE_WIDTH / 2, paint);

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
          canvas.drawCircle(frame.left + point.getX(), frame.top
                  + point.getY(), 6.0f, paint);
        }
      }
      if (currentLast != null) {
        paint.setAlpha(OPAQUE / 2);
        paint.setColor(resultPointColor);
        for (ResultPoint point : currentLast) {
          canvas.drawCircle(frame.left + point.getX(), frame.top
                  + point.getY(), 3.0f, paint);
        }
      }

      postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top,
              frame.right, frame.bottom);
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
