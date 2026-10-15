# Glass Clock

Reloj para Android libre y sin rastreo, con interfaz de estilo *Liquid Glass* de Apple: superficies de cristal que refractan lo que tienen detrás como una lente (gracias a [Backdrop](https://github.com/Kyant0/AndroidLiquidGlass)), con brillo en los bordes y botones que se deforman como una gota al pulsarlos.

Inspirado en el reloj de Samsung, pensado para uso personal y publicado bajo GPL-3.0.

## Funciones

**Alarmas**
- Repetición por días o una sola vez, con nombre.
- Tono del sistema, silencio o vibración.
- Volumen creciente configurable (15 s a 2 min).
- Posponer configurable (intervalo y número máximo de veces).
- **Mientras suena puedes elegir cuánto posponer**: chips rápidos (5/10/15/30) y ajuste fino con −/+ de 1 a 120 min.
- Los botones de volumen posponen (desactivable).
- Omitir solo la próxima ocurrencia de una alarma que se repite.
- Silencio automático tras N minutos (pospone si se puede).
- Pantalla completa sobre la pantalla de bloqueo.

**Reloj mundial**: reloj analógico de cristal, búsqueda de ciudades por zona horaria, diferencia horaria y día relativo. Mantén pulsada una ciudad para quitarla.

**Cronómetro**: vueltas, con la más rápida y la más lenta resaltadas. Sigue contando aunque cierres la app.

**Temporizador**: varios a la vez, preajustes guardables, pausa, +1 min y notificación con cuenta atrás.

Mantén pulsada una alarma, un temporizador o una ciudad para ver sus opciones (editar, duplicar, omitir, eliminar...).

**Modo sueño**: recordatorio para acostarte con el tiempo de sueño que tendrás hasta tu próxima alarma.

## Compilar

Requisitos: JDK 17 y Android SDK (plataforma 35 y build-tools).

En Arch / CachyOS:

```bash
sudo pacman -S --needed jdk17-openjdk
yay -S --needed android-sdk-cmdline-tools-latest android-sdk-platform-tools android-sdk-build-tools android-platform
```

Indica dónde está el SDK (con los paquetes de AUR suele ser `/opt/android-sdk`):

```bash
echo "sdk.dir=/opt/android-sdk" > local.properties
```

Compila e instala:

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

`./gradlew assembleRelease` genera un APK minimizado firmado con la clave de depuración, pensado para instalarlo en tu propio teléfono.

## Permisos

| Permiso | Para qué |
|---|---|
| `USE_EXACT_ALARM` / `SCHEDULE_EXACT_ALARM` | Que las alarmas suenen a la hora exacta. |
| `USE_FULL_SCREEN_INTENT` | Mostrar la alarma sobre la pantalla de bloqueo. |
| `POST_NOTIFICATIONS` | Alarmas, temporizadores y recordatorios. |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Reproducir el tono mientras suena. |
| `RECEIVE_BOOT_COMPLETED` | Reprogramar alarmas tras reiniciar. |
| `VIBRATE`, `WAKE_LOCK` | Vibrar y mantener el teléfono despierto mientras suena. |
| `SYSTEM_ALERT_WINDOW` | Opcional, para abrir la alarma aunque estés usando el móvil. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Opcional, para fabricantes que matan apps en segundo plano. |

La app no pide acceso a internet.

## Estructura

```
app/src/main/java/dev/glassclock/
├── data/       Modelos y almacenamiento JSON
├── alarm/      Programación de alarmas, posponer, omitir, reinicio
├── ring/       Servicio que suena y pantalla de alarma
├── timer/      Temporizadores
├── bedtime/    Modo sueño
├── ui/glass/   Componentes liquid glass (superficie, botones, rueda, barra...)
└── ui/screens/ Pantallas
```

## Licencia

[GPL-3.0](LICENSE)
