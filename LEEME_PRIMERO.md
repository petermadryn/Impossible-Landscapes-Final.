# DG · Impossible Landscapes — Archivo del Supervisor (v3)

**Proyecto Android offline para la Redmi Pad Pro (Android), pensado para pantalla apaisada.**

## IMPORTANTE: dos ZIP distintos

- `IMPOSSIBLE_LANDSCAPES_FINAL_GITHUB.zip` (**fuentes públicos/privados para compilar**): se sube a GitHub. Contiene solamente código, índice, referencias, mapa de relaciones e icono, sin los documentos de terceros.
- `IMPOSSIBLE_LANDSCAPES_BIBLIOTECA_PRIVADA.zip` (**material privado**): NO se sube a GitHub. Contiene 27 archivos proporcionados por el usuario: imágenes seleccionadas, mapas, tres PDF de ayudas y el MP3 de Carmen. Transferilo a la tablet e importalo con el botón **Ayudas ZIP** dentro de la app. Este ZIP no contiene el manual completo de 370 páginas: importalo aparte.

La biblioteca es exclusivamente para uso privado de quien tenga derecho a acceder a esos materiales. No redistribuir. El paquete se ha preparado para el uso personal con los documentos recibidos; no se ha verificado de forma independiente la licencia de cada imagen comunitaria.

## Compilar la APK usando GitHub, paso a paso

1. **Crear un repositorio nuevo privado**, por ejemplo `Impossible-Landscapes-Final`, desde GitHub. No uses tu repositorio de BLACK STATIC o Consola-Rol.
2. En Linux, extraé `IMPOSSIBLE_LANDSCAPES_FINAL_GITHUB.zip`. Dentro deben estar `app`, `.github`, `build.gradle`, `settings.gradle`, `gradle.properties` y este archivo.
3. Subí **el contenido**, no la carpeta envolvente, a la raíz del repositorio desde **Add file > Upload files**. Activá **Ctrl+H** si tu gestor de archivos oculta `.github`.
4. Verificá que aparezca `.github/workflows/compilar-apk.yml`. Si no podés subir la carpeta oculta, en GitHub elegí **Add file > Create new file** y escribí el nombre completo `.github/workflows/compilar-apk.yml`; luego pegá el contenido del archivo local.
5. Ir a **Actions**. La acción `Compilar Impossible Landscapes Final` debe ejecutarse tras la subida. También podés pulsar **Run workflow**.
6. Esperá que aparezca la ejecución en verde. Al abrirla, buscá **Artifacts > Impossible_Landscapes_Final_APK**.
7. Descargá y extraé ese archivo. Encontrarás un `app-debug.apk`.
8. Copiá el APK a Descargas de la tablet e instalalo desde el gestor de archivos.

No se ha compilado todavía una APK en GitHub ni probado en la Redmi Pad Pro; el proyecto tiene comprobaciones estáticas, no una validación de instalación real.

## Preparar la tablet

Copiá a la tablet estos tres elementos:

- `app-debug.apk` (de GitHub Actions)
- `IMPOSSIBLE_LANDSCAPES_BIBLIOTECA_PRIVADA.zip` (el ZIP privado, sin descomprimir)
- `Delta_Green_Impossible_Landscapes_2021-02-01(1).pdf` (el manual original de **370 páginas**, que ya tenés)

Abriendo `DG · Impossible Landscapes`:

1. Tocar **Importar libro**. Seleccionar el PDF de 370 páginas y esperar a que termine de copiarse. Si el documento tiene otro número de páginas, la aplicación no lo acepta para no romper los enlaces del índice.
2. Tocar **Ayudas ZIP**. Seleccionar `IMPOSSIBLE_LANDSCAPES_BIBLIOTECA_PRIVADA.zip` (la tablet lo mantendrá en Descargas, pero la app hará una copia de los recursos en su almacenamiento privado).
3. En **Inicio** o **Evidencias** debe decir `27 de 27 recursos importados`.
4. En **Mapas**, probar «Planta baja (Supervisor)», «Primer piso» y «Segundo piso». Los mapas no etiquetados como completamente seguros son **Solo Supervisor**.
5. En **Evidencias**, buscar `Carmen` y pulsar el audio. Se reproduce una sola vez, no en bucle.
6. En **Índice 574**, probar una referencia. Debe abrir el lector PDF en la página física correcta; volver a **Archivo** mantiene el índice.
7. Abrir **Supervisor > Exportar respaldo** para guardar un JSON con el cuaderno de la campaña y los estados/favoritos de las ayudas.

## Operaciones y funciones

- **Inicio**: atajos rápidos y resumen de recursos importados.
- **Evidencias**: 27 fichas iniciales con importación, búsqueda, filtros, favoritos, estados «No mostrado / Preparado / Mostrado» y apertura de imágenes, PDF y audio.
- **Mapas**: variantes de planos del Macallistar y residencia Samigina. Zoom y desplazamiento sobre imágenes importadas.
- **Casos**: ayudas oficiales y complementos `Ancillary Casualties` y `EXEUNT: The Missing Room`.
- **Supervisor**: cuaderno que ya existía, con puntuaciones de Corrupción 0–10, pistas, notas y mapa de relaciones. Exportación e importación de respaldo ahora incluyen el estado de la biblioteca.
- **Índice 574**: búsqueda y enlaces a las 370 páginas originales, con las 96 entradas resumidas.

Para agregar una evidencia tuya: **Evidencias > + Agregar ayuda propia**. Tras poner nombre, operación y tipo, la app te pide seleccionar la imagen, PDF o audio desde Android. El nuevo recurso no se agrega al ZIP de distribución y está vinculado exclusivamente a esta instalación.

## Privacidad, seguridad y mantenimiento

- No se solicita permiso de Internet. Los archivos de ayuda se guardan en el almacenamiento privado de la app; permanecen disponibles offline.
- La APK incorpora un identificador nuevo `ar.deltagreen.impossible.archivo` para instalar en paralelo con las versiones antiguas, sin sobrescribirlas. El icono está integrado en la APK: no requiere Nova Launcher.
- Los respaldos JSON contienen texto, estados y la estructura del cuaderno, **NO** contienen ni exportan imágenes, audio ni PDF: reimportá el ZIP y el libro si reinstalás.
- **No desinstales la app ni borres sus datos sin exportar tu respaldo**.
- Las APK `debug` generadas en distintas máquinas GitHub pueden no compartir la misma firma y, por tanto, no siempre pueden actualizarse unas a otras. Para un proyecto estable a largo plazo se recomienda firmar versiones de publicación con un keystore privado persistente. No lo subas al repositorio.
- El lector Android usa `PdfRenderer`: el número «PDF» es la página física del archivo; para el libro original la página física equivale a la impresa **+ 2**.

## Dependencias y validación

- Android Gradle Plugin: 8.7.3; Gradle 8.9; SDK 35; Java 17; minSdk 26, target 34.
- No depende de bibliotecas de terceros en tiempo de ejecución; el lector PDF utiliza `android.graphics.pdf.PdfRenderer`.
- Se comprobó la sintaxis JavaScript de los cinco bloques del HTML y de `catalog.js` / `ui-final.js`, la estructura ZIP y la compatibilidad de nombres de archivos. **No se ha compilado ni probado la APK Android en un dispositivo**, y pueden aparecer errores de compilación o de comportamiento a corregir a partir del registro de Actions.

## Tamaño y origen de archivos

El ZIP personal pesa aproximadamente 33 MB porque reutiliza solamente los archivos aportados que resultaron relevantes. La carpeta original de Dennis (3,5 GB), el tarot físico/digital, los archivos comunitarios no subidos y los audios comerciales no se incluyen.
