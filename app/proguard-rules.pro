-keep class com.sevtinge.hyperceiler.oldui.ui.**{ *; }
-keep class com.sevtinge.hyperceiler.oldui.main.**{ *; }
-keep class com.sevtinge.hyperceiler.oldui.utils.XposedActivateHelper { *; }

-keep class com.fan.**{ *; }

-keepattributes SourceFile,LineNumberTable

-allowaccessmodification
#-obfuscationdictionary          dict.txt
#-classobfuscationdictionary     dict.txt
#-packageobfuscationdictionary   dict.txt


# --- Room 数据库混淆规则 ---

# 1. 保留所有的 Entity 类，确保字段名不被修改（否则数据库找不到列名）
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}

# 2. 保留 Entity 中的字段，防止 Room 映射失败
-keepclassmembers class com.sevtinge.hyperceiler.search.data.ModEntity {
    <fields>;
}

# 3. 保留 Room 生成的代码（Room 会生成类似 AppDatabase_Impl 的类）
-keep class androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.util.TableInfo{ *; }
-keep class androidx.room.util.TableInfo$Column{ *; }
-keep class androidx.room.util.TableInfo$Index{ *; }
-keep class androidx.room.util.TableInfo$ForeignKey{ *; }

# 4. 如果你使用了 FTS (全文搜索)
-keep class * extends androidx.room.RoomOpenHelper
-keep class androidx.sqlite.db.SupportSQLiteOpenHelper
-keep class androidx.sqlite.db.SupportSQLiteDatabase

# 5. 保留你的 Dao 接口（以便 Room 能够实例化它们）
-keep interface * extends androidx.room.RoomDatabase
-keep interface com.sevtinge.hyperceiler.search.data.ModDao { *; }

# 6. 防止 Room 内部库报错
-dontwarn androidx.room.**



# 1. 保护主接口类及其所有嵌套类（包含 Global, Secure, System, Cache 等）
-keep class fan.provider.Settings** { *; }

# 2. 保护 LiveData 封装类及其内部接口
-keep class fan.provider.SettingsLiveData** { *; }

# 3. 保护 Provider 类（防止系统找不到 Provider 路径）
-keep class fan.provider.SettingsProvider { *; }

# 4. 保持泛型签名（如果不保持，List<Long> 在混淆后可能会变成 List<Object> 导致类型转换异常）
-keepattributes Signature

