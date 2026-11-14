# Guía de sustentación — Fase 5: Factura electrónica PDF (simulada DIAN)

## 1. Qué se agregó

| Archivo | Para qué existe |
|---|---|
| `service/InvoiceGenerator` | Interfaz: "escribe la factura de este pago en este archivo". El controlador no sabe qué librería dibuja. |
| `service/PdfBoxInvoiceGenerator` | Dibuja la factura A4 con PDFBox: encabezado, adquirente, ítems, totales, CUFE, QR y pie. |
| `service/QrCodeGenerator` + `ZxingQrCodeGenerator` | Crea la imagen del QR con ZXing. Interfaz aparte para no atar el PDF a una librería de QR. |
| `service/SchoolInfo` | Datos del colegio (nombre, NIT, dirección, teléfono, correo, resolución DIAN) leídos de `config.properties`. |
| `controller/InvoiceController` | Ciclo de vida del archivo: nombre único, carpeta, guardar nombre y ruta en BD, regenerar. |
| `AppContext`, `MainView`, `FinanceView` | Conexión (composition root) y pantallas: la ruta del PDF se muestra al pagar; opción nueva "Regenerar el PDF de una factura". |
| `pom.xml` | Dependencias `pdfbox 2.0.31` y `zxing core 3.5.3`. |
| `docs/examples/factura_000123.pdf` y `docs/images/factura-ejemplo.png` | Muestra real de la factura (sirve para la sección 12 del README). |

## 2. Cómo agregar las librerías (lo que pide el enunciado)

**Elegí PDFBox + ZXing y no iText 7** porque ambos son Apache 2.0. iText 7 es AGPL: para un proyecto cerrado exige licencia comercial.

- **Con Maven (lo que usamos):** ya están en `pom.xml`. En IntelliJ: clic derecho sobre `pom.xml` → *Maven* → *Reload project*. Maven descarga los `.jar` (y `fontbox`, `commons-logging` que PDFBox necesita) a `~/.m2`.
- **Sin Maven (alternativa):** descarga `pdfbox`, `fontbox`, `commons-logging` y `zxing core` desde Maven Central, y en IntelliJ: *File → Project Structure → Libraries → + → Java* y selecciona los `.jar`.
- **Verifica:** `mvn compile` debe terminar en `BUILD SUCCESS`.

## 3. Flujo completo: registrar un pago (vista → controlador → DAO → BD → archivo)

```mermaid
sequenceDiagram
    participant V as FinanceView
    participant P as PaymentController
    participant D as PaymentDaoJdbc
    participant DB as MySQL
    participant I as InvoiceController
    participant G as PdfBoxInvoiceGenerator
    participant F as invoices/factura_000123.pdf

    V->>P: registerPayment(request)
    P->>P: valida, descuento (Strategy), IVA, número de recibo y de factura, CUFE
    P->>D: registerWithInvoice(payment, invoice)
    D->>DB: BEGIN; INSERT pagos; INSERT facturas; COMMIT
    P-->>V: PaymentResult
    V->>I: issuePdf(result)
    I->>G: generate(result, ruta)
    G->>F: Files.newOutputStream + document.save
    I->>DB: UPDATE facturas SET nombre_archivo_pdf, ruta_archivo
    I-->>V: Optional(ruta)  /  vacío si falló
    V->>V: muestra la ruta o una advertencia
```

Puntos clave del flujo:
1. El **pago y la factura se guardan juntos en una transacción** (Fase 2). Si algo falla, no queda nada a medias.
2. **El PDF se crea después del `COMMIT`.** Un archivo no se puede "deshacer" con `ROLLBACK`; si estuviera dentro de la transacción, un fallo al escribir el PDF anularía un pago ya cobrado.
3. Si el PDF falla, el pago **no se pierde**: se registra en `error_log.txt`, la vista muestra una advertencia y el usuario puede usar **"Regenerar el PDF"**.

## 4. Qué contiene la factura (requisitos del enunciado)

| Requisito | Dónde se cumple |
|---|---|
| Razón social, NIT, dirección, teléfono, correo | `drawHeader` con `SchoolInfo` (de `config.properties`) |
| Datos del acudiente responsable | `drawParties` (nombre, documento, teléfono, correo, dirección) |
| Número consecutivo y fecha | `FE-000123` (lo crea `PaymentController`) y fecha de emisión |
| Ítems: descripción, cantidad, valor unitario, subtotal | `drawItems` (una línea del concepto y, si hay descuento, una línea negativa) |
| Subtotal, IVA y total | `drawTotalsAndPayment` |
| CUFE simulado (hash de número + NIT + fecha) | `CufeGenerator`: SHA-384 → 96 caracteres hexadecimales (la columna admite 100) |
| Código QR simulado | `ZxingQrCodeGenerator`, contiene número, NIT, fecha, valores y CUFE |
| Resolución de habilitación simulada | Primera línea bajo el encabezado, desde `school.dian.resolution` |
| Nombre único `factura_000123.pdf` en una carpeta | `InvoiceController.fileNameOf` → carpeta `invoices.dir` |
| Se muestra la ruta | `FinanceView.showPdfResult` |

## 5. Cinco preguntas probables del evaluador

1. **¿Por qué PDFBox y ZXing y no iText 7?** Por la licencia: PDFBox y ZXing son Apache 2.0; iText 7 es AGPL y obliga a liberar el código o pagar licencia. Además, PDFBox dibuja con coordenadas simples y lo puedo explicar línea por línea.
2. **¿Qué pasa si se cobra el pago pero el PDF no se puede crear?** El pago y su registro de factura ya están confirmados (`COMMIT`). `issuePdf` captura el error, lo guarda en `error_log.txt` y devuelve vacío; la vista muestra una advertencia y la opción "Regenerar el PDF" lo crea de nuevo desde los datos guardados.
3. **¿Cómo se calcula el CUFE y por qué es "simulado"?** Es el hash SHA-384 de *número de factura + NIT + fecha*, como pide el enunciado. El CUFE real de la DIAN usa más campos (valores, impuestos, cliente) y una clave técnica asignada por la DIAN, que aquí no existe.
4. **¿Cómo garantizas que el nombre del archivo sea único?** Sale del número de factura (`FE-000123` → `factura_000123.pdf`), y `numero_factura` es `UNIQUE` en la base de datos. Dos facturas nunca comparten nombre.
5. **¿Dónde usas `java.nio.file` y try-with-resources en esta fase?** En `PdfBoxInvoiceGenerator.generate`: `PDDocument`, `PDPageContentStream` y el `OutputStream` se cierran solos aunque falle algo; `Files.createDirectories` crea la carpeta y `Files.newOutputStream` escribe el archivo. En `InvoiceController`: `Path` y `Files.exists` para comprobar que el PDF sigue en disco.

Extras que pueden preguntar:
- **¿Por qué `clean()` convierte el texto a Latin-1?** Las fuentes estándar de PDF solo codifican Latin-1 (incluye tildes y ñ). Si un nombre trae un carácter raro (por ejemplo un emoji), PDFBox lanza excepción; se reemplaza por `?` para que una factura nunca se bloquee por un símbolo.
- **¿Por qué las coordenadas "bajan" restando `y`?** PDFBox usa el origen en la esquina *inferior* izquierda de la página.
- **¿Por qué silenciar los avisos de PDFBox?** Escribe líneas `WARNING` en la consola (caché de fuentes) que romperían los menús; se bajan a `SEVERE` con `java.util.logging`.

## 6. Decisiones de diseño

| Decisión | Justificación |
|---|---|
| `InvoiceGenerator` como interfaz | **DIP/OCP:** cambiar a otra librería o a HTML es una clase nueva; `InvoiceController` no cambia. |
| PDF **después** del commit | Un archivo no entra en una transacción SQL; no se anula un pago real por un fallo de disco. |
| `issuePdf` no lanza excepción, `regeneratePdf` sí | Al pagar, el usuario debe ver el recibo aunque falle el PDF. Al regenerar, el usuario pidió justo eso y debe ver el error. |
| `regeneratePdf` recalcula subtotal e IVA desde el monto pagado | Los totales del PDF siempre coinciden con el pago guardado, aunque el precio del concepto haya cambiado después. |
| Solo fuentes estándar (Helvetica, Courier) | No hay archivos de fuente que distribuir; funciona igual en Windows, Mac y Linux. |
| Ruta absoluta guardada en `ruta_archivo` | La columna se llama "ruta completa". |
| ZXing `core` sin el módulo `javase` | Menos dependencias: el `BitMatrix` se convierte a imagen con un bucle de 6 líneas. |
| La vista llama a los dos controladores (pagar y luego `issuePdf`) | Mantiene `PaymentController` sin dependencias de archivos. Alternativa: inyectar el `InvoiceController` en el de pagos. |

## 7. Límites conocidos (dilos tú antes que el evaluador)

- **No se ha probado contra MySQL real.** El PDF se probó con PDFBox **2.0.24** y ZXing **3.5.3** (compilado desde sus fuentes) porque en mi entorno no se puede descargar de Maven Central; el `pom.xml` usa PDFBox **2.0.31**, de la misma serie 2.0.x y con la misma API. Si `mvn compile` marcara algún problema, avísame.
- **Primera factura lenta:** PDFBox 2.x construye una caché de fuentes del sistema la primera vez (unos segundos). Luego es inmediato.
- **Regenerar** estima la línea de descuento como *precio actual del concepto − subtotal*, con el texto genérico "Descuento aplicado". Los totales sí son exactos.
- La factura es de **una sola página** con una línea de ítem (más el descuento): cada pago tiene un solo concepto. Un pago con muchos ítems necesitaría paginación.
- Los textos de la factura están en español (documento para el acudiente) y el código en inglés.

## 8. Autoevaluación contra la rúbrica

| Criterio | Estado |
|---|---|
| Archivos y logs (8) | **Completo:** `java.nio.file` y try-with-resources en logs, config y carpeta `invoices/`; fallos del PDF van a `error_log.txt`. |
| Patrones (10) | Singleton ×2, DAO, Factory ×2, Strategy (descuentos), Command (`Menu`) y la interfaz `InvoiceGenerator`. |
| SOLID (10) | DIP/OCP/SRP marcados en `service/` y en `InvoiceController`. La tabla para el README va en la Fase 7. |
| MVC (10) | Vistas sin SQL ni librerías de PDF; controladores sin `System.out`; PDFBox y ZXing solo en 2 clases de `service`. |
| JDBC (5) | Sin cambios: el PDF no toca la transacción pago + factura. |
| Consola (10) | Ruta del PDF visible, advertencia si falla, nueva opción de regenerar. |
| DML (5), Documentación y repo (6) | Pendientes (Fases 6 y 7). |
| Dominio del código (49) | Esta guía; las 30 preguntas llegan en la Fase 8. |

## 9. Cómo se probó

- 17 pruebas de `InvoiceController` con DAOs simulados y el generador **real**: archivo creado y nombrado bien, ruta guardada, fallo del generador, fallo de BD tras crear el archivo, regeneración con IVA 19 % y con descuento 10 %, precio de concepto cambiado, pago inexistente, existencia del archivo.
- El QR del PDF se renderizó a imagen y se **decodificó con ZXing**: devuelve exactamente el texto esperado.
- El `SmokeTest` de la Fase 4 sigue en **38 de 38**.
