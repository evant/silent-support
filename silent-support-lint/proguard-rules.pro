# Include java runtime classes
-libraryjars <java.home>/lib/rt.jar

-dontobfuscate
-dontoptimize

# Disable certain proguard optimizations which remove stackframes
#-optimizations !method/inlining/*,!code/allocation/variable

# Keep filenames and line numbers
-keepattributes SourceFile,LineNumberTable

-keep public class **.IssueRegistry { *; }

-dontwarn org.jetbrains.uast.**
#-dontwarn com.intellij.**
#-dontwarn com.android.tools.lint.**
#-dontwarn org.objectweb.asm.**
#-dontwarn com.android.**
#-dontwarn javax.annotation.**
#-dontwarn com.google.errorprone.**
#-dontwarn javax.crypto.**
#-dontwarn com.google.j2objc.**
#-dontwarn com.google.common.**
#-dontwarn lombok.**
#-dontwarn org.apache.**
#-dontwarn org.bouncycastle.**
#-dontwarn gnu.trove.**