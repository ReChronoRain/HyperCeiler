package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.gallery.EnableHdrEnhance;
import com.sevtinge.cemiuiler.module.hook.gallery.EnableIdPhoto;
import com.sevtinge.cemiuiler.module.hook.gallery.EnableMagicSky;
import com.sevtinge.cemiuiler.module.hook.gallery.EnableOcr;
import com.sevtinge.cemiuiler.module.hook.gallery.EnableOcrForm;
import com.sevtinge.cemiuiler.module.hook.gallery.EnablePdf;
import com.sevtinge.cemiuiler.module.hook.gallery.EnablePhotoMovie;
import com.sevtinge.cemiuiler.module.hook.gallery.EnableRemover2;
import com.sevtinge.cemiuiler.module.hook.gallery.EnableTextYanhua;
import com.sevtinge.cemiuiler.module.hook.gallery.EnableVideoPost;
import com.sevtinge.cemiuiler.module.hook.various.UnlockSuperClipboard;

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
        initHook(UnlockSuperClipboard.INSTANCE, mPrefsMap.getBoolean("various_super_clipboard_enable"));
    }
}



