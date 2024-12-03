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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.app;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.gallery.ChangeBackupServer;
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
import com.sevtinge.hyperceiler.module.hook.gallery.UnPrivacyWatermark;
import com.sevtinge.hyperceiler.module.hook.gallery.UnlockAIGallery;
import com.sevtinge.hyperceiler.module.hook.various.UnlockSuperClipboard;

@HookBase(targetPackage = "com.miui.gallery",  isPad = false)
public class Gallery extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new UnPrivacyWatermark(), mPrefsMap.getBoolean("gallery_enable_un_privacy_watermark"));
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
        initHook(new ChangeBackupServer(), mPrefsMap.getStringAsInt("gallery_backup_server", 0) != 0);
        initHook(UnlockAIGallery.INSTANCE, mPrefsMap.getBoolean("gallery_enable_ai_gallery"));
        initHook(UnlockSuperClipboard.INSTANCE, mPrefsMap.getStringAsInt("various_super_clipboard_e", 0) != 0);
    }
}
