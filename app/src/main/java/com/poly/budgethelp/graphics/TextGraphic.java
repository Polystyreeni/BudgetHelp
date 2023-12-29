// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


package com.poly.budgethelp.graphics;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.util.Pair;

import com.google.mlkit.vision.text.Text;

public class TextGraphic extends GraphicOverlay.Graphic {
    private static final String TAG = "TextGraphic";
    private static final int BOX_COLOR = Color.GREEN;
    private static final float BOX_STROKE = 4.0f;

    private final Paint rectPaint;
    private final Pair<String, Rect> lineData;
    private final Paint textPaint;

    public TextGraphic (GraphicOverlay overlay, Pair<String, Rect> lineData) {
        super(overlay);

        this.lineData = lineData;

        rectPaint = new Paint();
        rectPaint.setColor(BOX_COLOR);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(BOX_STROKE);

        textPaint = new Paint();
        textPaint.setColor(BOX_COLOR);
        textPaint.setTextSize(32.0f);

        postInvalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        if (lineData == null) {
            throw new IllegalStateException("Attempting to draw a null text element");
        }

        Rect bb = lineData.second;
        if (bb == null)
            return;

        Rect scaledRect = new Rect(
                (int)(scaleX((float)bb.left)),
                (int)(scaleX((float)bb.top)),
                (int)(scaleX((float)bb.right)),
                (int)(scaleX((float)bb.bottom))
        );

        // Draw rect around the text
        RectF rect = new RectF(scaledRect);
        canvas.drawRect(rect, rectPaint);

        // Renders the text at the bottom of the box.
        canvas.drawText(lineData.first, scaledRect.left, scaledRect.bottom, textPaint);
    }
}
