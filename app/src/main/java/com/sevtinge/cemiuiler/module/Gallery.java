package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.gallery.*;

public class Gallery extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new EnableHdrEnhance(), mPrefsMap.getBoolean("gallery_enable_hdr_enhanced"));
        initHook(new EnableMagicSky(), mPrefsMap.getBoolean("gallery_enable_magic_sky"));
        initHook(new EnablePdf(), mPrefsMap.getBoolean("gallery_enable_pdf"));
        initHook(new EnablePhotoMovie(), mPrefsMap.getBoolean("gallery_enable_photo_movie"));
        initHook(new EnableRemover2(), mPrefsMap.getBoolean("gallery_enable_remover_2"));
        initHook(new EnableTextYanhua(), mPrefsMap.getBoolean("gallery_enable_text_yanhua"));
        initHook(new EnableIdPhoto(), mPrefsMap.getBoolean("gallery_enable_id_photo"));
        initHook(new EnableIdPhoto(), mPrefsMap.getBoolean("gallery_enable_magic_matting"));
        initHook(new EnableVideoPost(), mPrefsMap.getBoolean("gallery_enable_video_post"));
        initHook(new EnableVideoPost(), mPrefsMap.getBoolean("gallery_enable_video_editor"));
        initHook(new EnableOcr(), mPrefsMap.getBoolean("gallery_enable_ocr"));
        initHook(new EnableOcrForm(), mPrefsMap.getBoolean("gallery_enable_ocr_form"));
    }
}



