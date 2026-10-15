# Liquid Clock

[English](README.md) | **Español**

Se instala en el móvil como **Reloj** (o *Clock*, según el idioma).

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
- Grupos de alarmas: plegables y con un interruptor que enciende o apaga todas sus alarmas a la vez.
- Desactivar una alarma que se repite solo por hoy: mañana se reactiva sola.
- Notificación 2 horas antes de cada alarma para descartar solo esa vez (desactivable).
- Silencio automático tras N minutos (pospone si se puede).
- Pantalla completa sobre la pantalla de bloqueo.

**Reloj mundial**: reloj analógico de cristal, búsqueda de ciudades por zona horaria, diferencia horaria y día relativo. Mantén pulsada una ciudad para quitarla.

**Cronómetro**: vueltas, con la más rápida y la más lenta resaltadas. Sigue contando aunque cierres la app.

**Temporizador**: varios a la vez, preajustes guardables, pausa, +1 min y notificación con cuenta atrás.

Mantén pulsada una alarma, un temporizador o una ciudad para ver sus opciones (editar, duplicar, omitir, eliminar...).

Desliza a izquierda o derecha para cambiar de pestaña. Toca el número de cualquier rueda para escribirlo con el teclado.

**Apariencia**: tema del sistema, claro u oscuro; fondo de colores en movimiento o un color de énfasis (10 colores con 3 tonos cada uno); e iconos alternativos de la app al estilo Material, Samsung, iPhone y Motorola.

**Modo sueño**: recordatorio para acostarte con el tiempo de sueño que tendrás hasta tu próxima alarma.

## Descargar

Los cambios de cada versión están en el [registro de cambios](CHANGELOG.es.md).

Descarga el APK desde [Releases](../../releases) e instálalo (Android 12 o superior).

Comprueba que el archivo no está alterado:

```bash
sha256sum -c liquid-clock-v0.2.3.apk.sha256
```

Y que está firmado con la clave del proyecto (huella SHA-256 del certificado):

```
D3:09:86:98:9C:2F:A7:E2:EA:C4:44:E1:B3:1F:0B:48:18:C8:EA:57:FE:21:9A:BD:D6:A3:BF:61:1F:86:BE:29
```

```bash
apksigner verify --print-certs liquid-clock-v0.2.3.apk
```

Al abrirla por primera vez, concede los permisos que pide, sobre todo **pantalla completa**:
sin él la alarma no enciende la pantalla del móvil bloqueado.

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

La build de depuración se instala como `dev.glassclock.debug`, así que convive con la release.

`./gradlew assembleRelease` genera un APK minimizado. Se firma con la clave descrita en
`~/.android-keys/liquid-clock.properties` (o la ruta de `-PsigningProps=...` o de la variable
`LIQUID_CLOCK_SIGNING`); si no existe, usa la clave de depuración:

```properties
storeFile=/ruta/a/la/clave.jks
storePassword=...
keyAlias=...
keyPassword=...
```

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
