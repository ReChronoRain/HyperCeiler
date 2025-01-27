package com.sevtinge.hyperceiler.ui.app.holiday.weather.confetti;

import com.sevtinge.hyperceiler.ui.app.holiday.weather.ConfettiSource;

public class MutableRectSource extends ConfettiSource {

    private volatile int x;
    private volatile int y;
    private volatile int otherX;
    private volatile int otherY;
    public MutableRectSource(int x, int y) {
        super(x, y);
    }

    public MutableRectSource(int x0, int y0, int x1, int y1) {
        super(x0, y0, x1, y1);
        this.x = x;
        this.y = y;
        this.otherX = otherX;
        this.otherY = otherY;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getOtherX() {
        return otherX;
    }

    public void setOtherX(int otherX) {
        this.otherX = otherX;
    }

    public int getOtherY() {
        return otherY;
    }

    public void setOtherY(int otherY) {
        this.otherY = otherY;
    }

    public int getXRange() {
        return this.otherX - this.x;
    }

    public int getYRange() {
        return this.otherY - this.y;
    }

    @Override
    protected float getInitialX(float random) {
        return (getXRange() * random) + this.x;
    }

    @Override
    protected float getInitialY(float random) {
        return (getYRange() * random) + this.y;
    }

    public void setBounds(int x0, int y0, int x1, int y1) {
        this.x = x0;
        this.y = y0;
        this.otherX = x1;
        this.otherY = y1;
    }
}
