# R8/ProGuard rules for the release build.
#
# Hilt, Room, Compose, WorkManager, DataStore, and ML Kit ship their own consumer rules. The rules
# below cover the two things that would otherwise break under obfuscation: kotlinx.serialization
# (used for the remote model config and the .ainb package) and the llama.cpp JNI boundary.

# Readable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,*Annotation*,InnerClasses,Signature

# ---------------------------------------------------------------------------------------------------
# kotlinx.serialization
# ---------------------------------------------------------------------------------------------------
-dontnote kotlinx.serialization.**

# Keep the Companion of serializable classes (avoids getDeclaredClasses serializer lookup).
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep serializer() on companions of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep the generated $serializer classes and their descriptors (serial names survive obfuscation).
-keep,includedescriptorclasses class **$$serializer { *; }

# ---------------------------------------------------------------------------------------------------
# llama.cpp JNI boundary — native method names and the callback invoked from C++ must not be renamed.
# ---------------------------------------------------------------------------------------------------
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.debayan.ainotebook.data.ai.LlamaInferenceEngine { *; }
-keep interface com.debayan.ainotebook.data.ai.TokenCallback { *; }
