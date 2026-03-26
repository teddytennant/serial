-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Jsoup
-keeppackagenames org.jsoup.nodes

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
