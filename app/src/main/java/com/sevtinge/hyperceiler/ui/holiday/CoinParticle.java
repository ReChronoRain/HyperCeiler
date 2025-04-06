package com.sevtinge.hyperceiler.ui.holiday;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.Surface;
import android.view.WindowManager;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.ui.holiday.weather.confetto.Confetto;
import com.sevtinge.hyperceiler.ui.holiday.weather.confetto.ConfettoInfo;

import java.util.Random;

@SuppressWarnings("FieldCanBeLocal")
public class CoinParticle extends Confetto {
	private final Context mContext;
	private float startX;
	private float startY;
	private int signX;
	private int signY;
	private int distance;
	private int maxAlpha;
	private final ConfettoInfo confettoInfo;
	private final Bitmap coin;
	private final float coinScale;
	private final int[] coins = new int[] {
		R.drawable.coin1, R.drawable.coin2, R.drawable.coin3, R.drawable.coin4, R.drawable.coin5, R.drawable.coin6, R.drawable.coin7, R.drawable.coin8, R.drawable.coin9, R.drawable.coin10,
		R.drawable.coin11, R.drawable.coin12, R.drawable.coin13, R.drawable.coin14, R.drawable.coin15, R.drawable.coin16, R.drawable.coin17, R.drawable.coin18, R.drawable.coin19, R.drawable.coin20,
		R.drawable.coin21, R.drawable.coin22
	};

	private void randomizeStartPoint() {
		int width = mContext.getResources().getDisplayMetrics().widthPixels;
		int height = mContext.getResources().getDisplayMetrics().heightPixels;
		float gapX = width / 20.0f;
		float gapY = height / 15.0f;

		int rotation = ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
		boolean isLandscape = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270;
		Random rand = new Random();
		float selector = rand.nextFloat();
		if (selector < 0.25f) {
			startX = rand.nextFloat() * (isLandscape ? gapY : gapX);
			startY = rand.nextFloat() * (isLandscape ? width : height);
		} else if (selector >= 0.25f && selector < 0.5f) {
			startX = width - rand.nextFloat() * (isLandscape ? gapY : gapX);
			startY = rand.nextFloat() * (isLandscape ? width : height);
		} else if (selector >= 0.5f && selector < 0.75f) {
			startX = rand.nextFloat() * (isLandscape ? height : width);
			startY = rand.nextFloat() * (isLandscape ? gapX : gapY);
		} else {
			startX = rand.nextFloat() * (isLandscape ? height : width);
			startY = height - rand.nextFloat() * (isLandscape ? gapX : gapY);
		}
		signX = rand.nextInt(3) - 1;
		signY = rand.nextInt(3) - 1;
		maxAlpha = rand.nextInt(40) + 30;
		distance = rand.nextInt(76) + 75;
	}

	CoinParticle(Context context, ConfettoInfo confettoInfo) {
		super();
		this.confettoInfo = confettoInfo;
		mContext = context;
		coinScale = 1.2f - new Random().nextFloat() * 0.2f;
		coin = BitmapFactory.decodeResource(context.getResources(), coins[new Random().nextInt(coins.length)]);
		randomizeStartPoint();
	}

	public int getHeight() {
		return 0;
	}

	public int getWidth() {
		return 0;
	}

	public void reset() {
		super.reset();
		randomizeStartPoint();
	}

	protected void configurePaint(Paint paint) {
		super.configurePaint(paint);
		paint.setColor(-1);
		paint.setAntiAlias(true);
	}

	protected void drawInternal(Canvas canvas, Matrix matrix, Paint paint, float x, float y, float rotation, float percentageAnimated) {
		matrix.postScale(coinScale, coinScale);
		matrix.postRotate(rotation, coin.getWidth() / 2f, coin.getHeight() / 2f);
		matrix.postTranslate(startX + signX * distance * percentageAnimated, startY + signY * distance * percentageAnimated);
		if (percentageAnimated < 0.1f)
			paint.setAlpha(Math.round(maxAlpha * percentageAnimated / 0.1f));
		else if (percentageAnimated > 0.9f)
			paint.setAlpha(Math.round(maxAlpha * (1.0f - percentageAnimated) / 0.1f));
		else
			paint.setAlpha(maxAlpha);
		canvas.drawBitmap(coin, matrix, paint);
	}

	public final ConfettoInfo getConfettoInfo() {
		return this.confettoInfo;
	}
}
