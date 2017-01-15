package com.egao_inc.kodori.kodori;

/**
 * Created by kanait on 2016/10/13.
 */
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;

import com.google.android.gms.vision.face.Face;
import com.egao_inc.kodori.kodori.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Landmark;

import java.util.List;

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

    private List<Landmark> langMarks;

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

    public List<Landmark> getLandmarks() {
        return this.langMarks;
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

        Log.w("Face Position", "xOffset:" + xOffset +  " yOffset:" + yOffset + " left:" + left+ " top:" + top + " right:" + right + " bottom:" + bottom );
        canvas.drawText(mIsReady ? mReadyMessage : mNotReadyMessage, left, top - LABEL_Y_OFFSET, mPaint);

        canvas.drawRect(left, top, right, bottom, mPaint);

        /*
         getPosition() - 顔が検出された領域の左上の座標を返します。
            getWidth() - 顔が検出された領域の幅を返します。
            getHeight() - 顔が検出された領域の高さを返します。
         */
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        this.langMarks = face.getLandmarks();
        for (Landmark landmark : this.langMarks) {
            // TODO : 倍数は計算で求める
            int cx = (int) (landmark.getPosition().x * 2.5);
            int cy = (int) (landmark.getPosition().y * 2.5);
            canvas.drawCircle(cx, cy, 10, paint);
        }

        /*
        PointF facePoint = face.getPosition();
        for (Landmark land : face.getLandmarks())
        {
            switch (land.getType())
            {
                case Landmark.LEFT_EYE:
                    break;
                case Landmark.LEFT_MOUTH:
                    break;
                case Landmark.RIGHT_EYE:
                    break;
                case Landmark.RIGHT_MOUTH:
                    break;
                case Landmark.NOSE_BASE:
                    break;
                default:
                    break;
            }
            Paint paint = new Paint();
            paint.setColor(Color.BLUE);
            PointF p = land.getPosition();
            canvas.drawRect(
                    facePoint.x + p.x - 3,
                    facePoint.y + p.y - 3,
                    facePoint.x + p.x + 6,
                    facePoint.y + p.y + 6,
                    paint);
        }
        */
    }
}