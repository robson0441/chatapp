# CipherChat Production ProGuard / R8 Rules

# Keep Room database and DAO classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Keep Cryptographic models
-keep class com.example.crypto.** { *; }
-keep class com.example.data.** { *; }

# Keep Coroutines and Kotlin attributes
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod
-dontwarn kotlinx.coroutines.**

# Suppress warnings for unused optional libraries
-dontwarn okhttp3.**
-dontwarn retrofit2.**
