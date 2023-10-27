package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.gallery.EnableHdrEnhance;
import com.sevtinge.hyperceiler.module.hook.gallery.EnableIdPhoto;
import com.sevtinge.hyperceiler.module.hook.gallery.EnableMagicSky;
import com.sevtinge.hyperceiler.module.hook.gallery.EnableOcr;
import com.sevtinge.hyperceiler.module.hook.gallery.EnableOcrForm;
import com.sevtinge.hyperceiler.module.hook.gallery.EnablePdf;
import com.sevtinge.hyperceiler.module.hook.gallery.EnablePhotoMovie;
import com.sevtinge.hyperceiler.module.hook.gallery.EnableRemover2;
import com.sevtinge.hyperceiler.module.hook.gallery.EnableTextYanhua;
import com.sevtinge.hyperceiler.module.hook.gallery.EnableVideoPost;
import com.sevtinge.hyperceiler.module.hook.various.UnlockSuperClipboard;

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



