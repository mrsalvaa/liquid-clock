# Registro de cambios

[English](CHANGELOG.md) | **Español**

## 0.4.1

### Novedades
- Fondo *Liso*, ahora el predeterminado: fondo plano con acento azul, más claro en tema oscuro y más intenso en tema claro. Siguen disponibles los fondos *En movimiento* y *Color de énfasis*.
- El icono de estilo Samsung pasa a ser el icono por defecto de la app. Las notificaciones también lo muestran, junto con el azul del tema.

### Mejoras
- Mucho más fluida con los fondos *Liso* y *Color de énfasis*. Sobre un fondo plano o casi plano los efectos de cristal se ven igual sin refracción ni desenfoque, así que tarjetas, botones y chips ya no los calculan. La barra inferior y los menús siguen refractando el contenido que pasa por debajo. En un Galaxy S24 FE, el peor fotograma al cambiar de pestaña pasó de 40 ms a 15 ms.
- En tema claro, los botones azules y los chips seleccionados son más opacos y su texto se lee mejor.

### Correcciones
- Deslizar entre pestañas podía dejar de funcionar al empujar más allá de la primera pestaña (Alarmas), igual que pasaba en la última.

### Para desarrolladores
- Nuevo tipo de compilación `profile`: optimizada como la release, pero se instala como `dev.glassclock.debug` para probar con el rendimiento real.

## 0.2.4

### Correcciones
- Deslizar entre pestañas podía dejar de funcionar para siempre al empujar más allá de la última pestaña (Mundial) justo después de un deslizamiento rápido. Ahora las pestañas usan su propio manejo del gesto, y al empujar contra el borde ya no rebotan.

### Cambios
- Las fechas se muestran como día/mes/año, por ejemplo "Jueves 15/10/2026" en el reloj mundial y "vie 17/10" en la lista de alarmas.
- "Desactivar hasta mañana" pasa a llamarse "Desactivar solo por hoy".

## 0.2.3

### Mejoras
- Colores de énfasis más apagados: los mismos tonos, menos saturados, y un brillo más suave en el fondo.
- Los iconos, textos y líneas en color de énfasis se ajustan para leerse bien. Un tono oscuro en tema oscuro, o uno claro en tema claro, ya no se pierde en el fondo.
- Los títulos de cada pantalla tienen un pequeño margen arriba para no quedar pegados a la barra de estado.

### Cambios
- La cabecera de alarmas vuelve a como estaba: "Próxima en 8 h 54 min · hoy a las 23:30" al lado de los botones.

## 0.2.1

### Novedades
- Grupos de alarmas. Cada grupo se puede plegar y tiene su propio interruptor, que enciende o apaga todas sus alarmas a la vez; cada alarma conserva el suyo. Las alarmas se pueden mover entre grupos, y un grupo se puede eliminar con sus alarmas o sin ellas.
- Desactivar una alarma que se repite solo por hoy: mañana se reactiva sola.
- Notificación 2 horas antes de cada alarma para descartar solo esa vez. Se puede desactivar en Ajustes.
- Deslizar a izquierda o derecha para cambiar de pestaña. La burbuja de la barra inferior sigue al dedo.
- Tocar el número de cualquier rueda para escribirlo con el teclado.
- Ajustes de apariencia:
  - Tema del sistema, claro u oscuro.
  - Fondo de colores en movimiento o de un solo color de énfasis. Hay 10 colores con 3 tonos cada uno.
  - Iconos alternativos de la app al estilo Material, Samsung, iPhone y Motorola.
- El botón Hecho del teclado confirma los diálogos.

### Mejoras
- Más fluida a 120 Hz. El fondo animado ya no redibuja el cristal en cada fotograma (unos 16 por segundo en reposo en vez de 120), y las pestañas cambian al instante sin reconstruir la pantalla.
- La cabecera de alarmas ahora dice "Próxima alarma en 8 hs 54 min ·" y debajo el día y la hora.

### Correcciones
- Los ":" de las ruedas del temporizador quedaban más bajos que los números.
- Al encadenar deslizamientos rápidos entre pestañas, el gesto se bloqueaba un momento.
- Al terminar de deslizar, la burbuja de la barra volvía atrás antes de ir a la pestaña nueva.
- Al cambiar de tema, el cristal podía quedarse con el tinte del tema anterior.

## 0.1.0

Primera versión: alarmas con posponer ajustable mientras suena, cronómetro, temporizadores, reloj mundial, recordatorio para dormir e interfaz Liquid Glass.
