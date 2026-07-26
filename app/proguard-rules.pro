# R8/ProGuard rules for the release build.
#
# Hilt, Room, Kotlin coroutines, DataStore, and WorkManager ship their own consumer rules, so no
# manual keeps are needed for them at this stage. Add app-specific keep rules here as features that
# rely on reflection (e.g. serialization) are introduced.

# Keep source file names and line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
