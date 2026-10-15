# kotlinx.serialization trae sus propias reglas; mantenemos los modelos por claridad.
-keep,includedescriptorclasses class dev.glassclock.data.**$$serializer { *; }
-keepclassmembers class dev.glassclock.data.** {
    *** Companion;
}
