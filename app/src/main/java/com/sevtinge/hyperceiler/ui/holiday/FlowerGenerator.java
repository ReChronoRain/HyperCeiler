package com.sevtinge.hyperceiler.ui.holiday;

import android.content.Context;

import com.sevtinge.hyperceiler.ui.holiday.weather.PrecipType;
import com.sevtinge.hyperceiler.ui.holiday.weather.confetto.Confetto;
import com.sevtinge.hyperceiler.ui.holiday.weather.confetto.ConfettoGenerator;
import com.sevtinge.hyperceiler.ui.holiday.weather.confetto.ConfettoInfo;

import java.util.Random;

public class FlowerGenerator implements ConfettoGenerator {
	private final ConfettoInfo confettoInfo;
	private final Context context;

	public FlowerGenerator(Context ctx) {
		super();
		this.context = ctx;
		this.confettoInfo = new ConfettoInfo(PrecipType.SNOW);
	}

	public Confetto generateConfetto(Random random) {
		return new FlowerParticle(this.context, this.confettoInfo);
	}

	public final ConfettoInfo getConfettoInfo() {
		return this.confettoInfo;
	}
}
