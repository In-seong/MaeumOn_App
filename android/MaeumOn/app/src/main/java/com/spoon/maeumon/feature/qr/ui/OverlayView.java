package com.spoon.maeumon.feature.qr.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class OverlayView extends View {
   private PointF[] qrPoints;
   private final Paint paint;

   public OverlayView(Context context, @Nullable AttributeSet attrs) {
      super(context, attrs);
      paint = new Paint();
      paint.setColor(Color.WHITE);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(12f);
      paint.setAntiAlias(true);
   }

   public void setQrPoints(@Nullable PointF[] points) {
      this.qrPoints = points;
      invalidate();
   }

   @Override
   protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);

      if (qrPoints != null && qrPoints.length >= 4) {
         Path path = new Path();
         path.moveTo(qrPoints[0].x, qrPoints[0].y);
         for (int i = 1; i < qrPoints.length; i++) {
            path.lineTo(qrPoints[i].x, qrPoints[i].y);
         }
         path.close();
         canvas.drawPath(path, paint);
      }
   }
}
