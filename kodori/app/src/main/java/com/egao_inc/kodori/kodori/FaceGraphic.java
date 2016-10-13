package com.egao_inc.kodori.kodori;

/**
 * Created by kanait on 2016/10/13.
 */
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.google.android.gms.vision.face.Face;
import com.egao_inc.kodori.kodori.ui.camera.GraphicOverlay;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 * 関連する内の顔の位置、向き、およびランドマークを描画するためのグラフィックインスタンス。
 * グラフィックオーバーレイビュー。
 */
class FaceGraphic extends GraphicOverlay.Graphic {
    private static final float ID_TEXT_SIZE     = 60.0f;
    private static final float LABEL_Y_OFFSET   = 50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    private static final int VALID_COLOR        = Color.GREEN;
    private static final int INVALID_COLOR      = Color.RED;

    private Paint mPaint;

    private volatile Face mFace;
    private int mFaceId;
    private boolean mIsReady = false;
    private final String mNotReadyMessage;
    private final String mReadyMessage;

    FaceGraphic(GraphicOverlay overlay) {
        super(overlay);
        mNotReadyMessage = overlay.getContext().getResources().getString(R.string.not_ready_message);
        mReadyMessage = overlay.getContext().getResources().getString(R.string.ready_message);
        mPaint = new Paint();
        mPaint.setColor(INVALID_COLOR);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(BOX_STROKE_WIDTH);
        mPaint.setTextSize(ID_TEXT_SIZE);
    }

    void setId(int id) {
        mFaceId = id;
    }


    void setIsReady(boolean isValid) {
        this.mIsReady = isValid;
        mPaint.setColor(isValid ? VALID_COLOR : INVALID_COLOR);
    }


    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     * 直近のフレームの検出から顔のインスタンスを更新
     * 再描画をトリガするために、オーバーレイの関連部分を無効にする
     */
    void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     * 顔の注釈を描画
     */
    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }

        // Draws a circle at the position of the detected face, with the face's track id below.
        float x = translateX(face.getPosition().x + face.getWidth() / 2);
        float y = translateY(face.getPosition().y + face.getHeight() / 2);



        // Draws a bounding box around the face.
        float xOffset = scaleX(face.getWidth() / 2.0f);
        float yOffset = scaleY(face.getHeight() / 2.0f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;

        canvas.drawText(mIsReady ? mReadyMessage : mNotReadyMessage, left, top - LABEL_Y_OFFSET, mPaint);

        canvas.drawRect(left, top, right, bottom, mPaint);
    }
}