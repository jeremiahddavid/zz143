# Consumer ProGuard rules for zz143-replay
# Keep rules for replay annotation-driven API and kotlin-reflect.

# Keep @WatchAction annotated classes and their members
-keep @com.zz143.replay.WatchAction class * { *; }
-keep @com.zz143.replay.WatchAction interface * { *; }

# Keep @WatchParam annotated fields and methods
-keepclassmembers class * {
    @com.zz143.replay.WatchParam <fields>;
    @com.zz143.replay.WatchParam <methods>;
}

# Keep @WatchGuard annotated methods
-keepclassmembers class * {
    @com.zz143.replay.WatchGuard <methods>;
}

# Keep the annotation classes themselves
-keep @interface com.zz143.replay.WatchAction
-keep @interface com.zz143.replay.WatchParam
-keep @interface com.zz143.replay.WatchGuard

# kotlin-reflect requires metadata to function correctly
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.jvm.internal.**
