# Keep Room entities / DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.**

# Compose
-dontwarn org.jetbrains.kotlin.**
