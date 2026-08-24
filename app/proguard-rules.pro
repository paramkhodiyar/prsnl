# R8 / ProGuard Rules for prsnl

# Kotlin Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class **$$serializer {
    *** INSTANCE;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt & Dagger
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}

# Jetpack Compose
-keepclassmembers class * extends androidx.compose.ui.Modifier { *; }

# Retain entity classes in document and storage models
-keep class com.prsnl.document.model.** { *; }
-keep class com.prsnl.storage.entity.** { *; }
