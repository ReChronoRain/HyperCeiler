package com.sevtinge.hyperceiler.holiday;

import android.content.Context;

import com.sevtinge.hyperceiler.holiday.weather.PrecipType;
import com.sevtinge.hyperceiler.holiday.weather.confetto.Confetto;
import com.sevtinge.hyperceiler.holiday.weather.confetto.ConfettoGenerator;
import com.sevtinge.hyperceiler.holiday.weather.confetto.ConfettoInfo;

import java.util.Random;

public class CoinGenerator implements ConfettoGenerator {
	private final ConfettoInfo confettoInfo;
	private final Context context;

	public CoinGenerator(Context ctx) {
		super();
		this.context = ctx;
		this.confettoInfo = new ConfettoInfo(PrecipType.CLEAR);
	}

	public Confetto generateConfetto(Random random) {
		return new CoinParticle(this.context, this.confettoInfo);
	}

	public final ConfettoInfo getConfettoInfo() {
		return this.confettoInfo;
	}
}
