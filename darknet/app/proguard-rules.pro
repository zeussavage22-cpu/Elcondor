# Go engine (gomobile): the native side finds these classes by name.
-keep class go.** { *; }
-keep class engine.** { *; }
-dontwarn go.**
-dontwarn engine.**

# Runs in the Shizuku process, created by name.
-keep class dev.darknet.service.PrivilegedService { *; }
-keep interface dev.darknet.IDarkNetService { *; }
-keep class dev.darknet.IDarkNetService$* { *; }

-keep class rikka.shizuku.** { *; }
-keep class org.lsposed.hiddenapibypass.** { *; }
-dontwarn rikka.shizuku.**

-keepattributes Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
