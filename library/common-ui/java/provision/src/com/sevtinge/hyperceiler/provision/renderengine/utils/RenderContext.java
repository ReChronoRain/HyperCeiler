/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.provision.renderengine.utils;

import miuix.mgl.MglContext;

public class RenderContext {
    private long mGlobalStartTime;
    private MaterialRepo mMaterialRepo;
    private float mTime;
    private int mWidth = 1;
    private int mHeight = 1;
    private float[] mSourceResolution = new float[2];
    private MglContext mContext = new MglContext();

    public RenderContext() {
        MaterialRepo materialRepo = new MaterialRepo();
        this.mMaterialRepo = materialRepo;
        materialRepo.initContext(this.mContext);
        this.mGlobalStartTime = System.nanoTime();
        float[] fArr = this.mSourceResolution;
        fArr[0] = 1080.0f;
        fArr[1] = 2400.0f;
    }

    public void tick() {
        this.mTime = (System.nanoTime() - this.mGlobalStartTime) / 1.0E9f;
    }

    public void resetTime() {
        this.mGlobalStartTime = System.nanoTime();
    }

    public float getTime() {
        return this.mTime;
    }

    public float getAspect() {
        return this.mHeight / this.mWidth;
    }

    public void setSize(int i, int i2) {
        this.mWidth = i;
        this.mHeight = i2;
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public MaterialRepo getMaterialRepo() {
        return this.mMaterialRepo;
    }

    public MglContext getMglContext() {
        return this.mContext;
    }

    public void clearNativeResource() {
        this.mMaterialRepo.clear();
        this.mContext.destroy();
    }

    public float[] getSourceResolution() {
        return this.mSourceResolution;
    }
}
