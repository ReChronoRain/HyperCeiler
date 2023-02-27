package com.sevtinge.cemiuiler.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.sevtinge.cemiuiler.BuildConfig
import com.sevtinge.cemiuiler.R
import com.sevtinge.cemiuiler.ui.base.SubFragment
import moralnorm.appcompat.app.AppCompatActivity
import moralnorm.internal.utils.ViewUtils
import moralnorm.preference.Preference


class AboutActivity : AppCompatActivity() {
    override fun onCreate(bundle: Bundle?) {
        setTheme(if (ViewUtils.isNightMode(this)) R.style.AppTheme_Dark else R.style.AppTheme)
        super.onCreate(bundle)
        setContentView(R.layout.activity_main)
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, initFragment()).commit()
    }

    fun initFragment(): Fragment {
        return AboutFragment()
    }

    /*override fun attachBaseContext(base: Context) {
        super.attachBaseContext(setLocale(base, getLocale(base)))
    }*/

    class AboutFragment : SubFragment() {
        override fun getContentResId(): Int {
            return R.xml.prefs_about
        }

        override fun initPrefs() {
            val mVersion = findPreference<Preference>("prefs_key_about_version")
            val mQQGroup = findPreference<Preference>("prefs_key_about_join_qq_group")

            mVersion.title = "v" + BuildConfig.VERSION_NAME + " - " + BuildConfig.BUILD_TYPE

            mQQGroup.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                joinQQGroup("g405srEn4hafy8xSJg1_EFJjxceLvpd7")
                true
            }
        }

        /****************
         *
         * 调用 joinQQGroup() 即可发起手Q客户端申请加群
         * @param key 由官网生成的key
         * @return 返回true表示呼起手Q成功，返回false表示呼起失败
         */
        private fun joinQQGroup(key: String): Boolean {
            val intent = Intent()
            intent.data =
                Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D$key")//https://jq.qq.com/?_wv=1027&k=EsyE1RhL
            // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return try {
                startActivity(intent)
                true
            } catch (e: Exception) {
                // 未安装手Q或安装的版本不支持
                false
            }
        }

    }
}