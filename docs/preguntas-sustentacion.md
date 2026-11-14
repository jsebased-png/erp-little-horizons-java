# Preguntas de sustentación — Little Horizons ERP

Documento final (Fase 8). Las respuestas se basan en el código real del proyecto.  
Úsalas para practicar la sustentación oral (49 pts de dominio del código).

---

## Arquitectura y MVC (1–6)

### 1. ¿Qué es MVC en este proyecto y cómo se ve en los paquetes?

**Respuesta:** Tres capas separadas:

- **View** (`view/`, `view/console/`): menús, tablas, lectura de teclado. No tiene SQL ni reglas de negocio.
- **Controller** (`controller/`): reglas (cupos, promedios, morosos, transacciones). No imprime ni escribe SQL.
- **Model** (`model/`): POJOs y enums. Solo datos.

La persistencia está en `dao/` (interfaces) y `dao/impl/` (JDBC). El diagrama está en el README sección 8.

### 2. ¿Quién crea los controladores y los DAOs?

**Respuesta:** `AppContext` (`config/AppContext.java`) es el composition root. En el constructor instancia las implementaciones JDBC, las pasa a los controladores por constructor y expone getters. `Main` solo hace `new MainView(new AppContext(), input, printer)`.

### 3. ¿Por qué las vistas no usan `System.out` directamente para todo?

**Respuesta:** Usan `ConsolePrinter` y `ConsoleInput` para centralizar formato (cajas ╔═╗, tablas alineadas, UTF-8) y validación. Así la vista no mezcla presentación con lógica y se puede cambiar el “skin” de consola sin tocar controladores.

### 4. ¿Qué hace `Main` exactamente?

**Respuesta:** Configura UTF-8, obtiene el logger Singleton, comprueba que la BD sea alcanzable con `DatabaseConnection.getInstance().isReachable()`, construye `AppContext` y lanza `MainView.run()`. Si falla la conexión, muestra mensaje y no abre el menú. No tiene lógica de negocio.

### 5. ¿Dónde está el SQL?

**Respuesta:** Solo en `dao/impl/*DaoJdbc.java` y en la clase base `BaseJdbcDao`. Controladores y vistas no contienen SQL. Todas las consultas usan `PreparedStatement`.

### 6. ¿Qué es un POJO aquí y por qué no tiene lógica?

**Respuesta:** Clases en `model/` (por ejemplo `Student`, `Payment`) con campos, getters/setters y a veces un método de presentación como `getFullName()`. La lógica (validar nota, calcular descuento) vive en controladores o strategies, para no mezclar datos con reglas (SRP).

---

## SOLID (7–11)

### 7. Dame un ejemplo claro de Single Responsibility (SRP).

**Respuesta:** `StudentController` solo maneja estudiantes y acudientes. Los pagos están en `PaymentController`, los préstamos en `LoanController`, los promedios oficiales en `GradeController` y las estadísticas en `PerformanceReportController`. Cada clase tiene un motivo de cambio distinto. Comentarios `// SOLID: SRP` en el código.

### 8. ¿Open/Closed (OCP)?

**Respuesta:** `DiscountStrategy`: para un descuento nuevo (por ejemplo “hijos de empleados”) se crea una clase que implementa la interfaz; `PaymentController` no se modifica. Igual con `AbstractLoanDaoJdbc`: un nuevo tipo de prestatario sería una subclase, no un cambio en la clase abstracta.

### 9. ¿Dependency Inversion (DIP)?

**Respuesta:** Los controladores reciben interfaces (`StudentDao`, `PaymentDao`, etc.) por constructor, no las clases `*DaoJdbc`. `LoanController` recibe un `Function<BorrowerType, LoanDao>` (el factory) para no acoplarse a implementaciones concretas.

### 10. ¿Interface Segregation (ISP)?

**Respuesta:** `ReadableDao` expone solo operaciones de lectura. Los catálogos que no se modifican en runtime no se ven obligados a implementar `save`/`delete`.

### 11. ¿Liskov Substitution?

**Respuesta:** Cualquier `DiscountStrategy` (`NoDiscount`, `SiblingsDiscount`, `ScholarshipDiscount`, `PercentageDiscount`) puede sustituirse en `PaymentController` y el flujo de pago sigue funcionando: solo se llama `apply()` y `describe()`.

---

## Patrones de diseño (12–16)

### 12. ¿Dónde está el Singleton y por qué?

**Respuesta:**

- `DatabaseConnection.getInstance()`: un solo proveedor de conexiones configurado desde `config.properties`.
- `AppLogger.getInstance()`: un solo escritor al archivo `logs/error_log.txt`, sin varios hilos abriendo el mismo archivo de forma descoordinada.

Ambos son `final` con constructor privado y acceso estático sincronizado o holder.

### 13. ¿Cómo funciona el patrón DAO?

**Respuesta:** Interfaz en `dao/` (contrato) + implementación JDBC en `dao/impl/`. El controlador depende de la interfaz. Si mañana se cambiara a otro motor, se implementaría otra clase y se wirearía en `AppContext` sin tocar controladores.

### 14. ¿Factory en este proyecto?

**Respuesta:**

- `LoanDaoFactory.create(BorrowerType)`: elige `StudentLoanDaoJdbc` o `TeacherLoanDaoJdbc` según el prestatario (tablas separadas en el DDL).
- `DiscountStrategyFactory.create(...)`: elige la estrategia de descuento según hermanos activos, beca, etc.

El controlador no tiene `if (type == STUDENT) ... else ...` para crear el DAO correcto.

### 15. ¿Strategy para descuentos?

**Respuesta:** Interfaz `DiscountStrategy` con `apply(BigDecimal)` y `describe()`. `PaymentController` obtiene la estrategia del factory, aplica el descuento al monto base, calcula IVA con `TaxCalculator` y guarda el monto final y las notas del pago. No hay un `switch` de tipos de descuento en el controlador.

### 16. ¿Hay algo parecido a Command?

**Respuesta:** En `Menu` (`view/console/Menu.java`) cada opción es una etiqueta + `Runnable`. El menú ejecuta la acción sin saber qué hace. Añadir una opción es `add("label", this::metodo)` sin cambiar el bucle del menú (OCP).

---

## JDBC, transacciones y archivos (17–21)

### 17. ¿Cómo se garantiza que no hay SQL injection?

**Respuesta:** El 100 % de las consultas usan `PreparedStatement` con parámetros (`?`). Nunca se concatena entrada del usuario en el SQL.

### 18. ¿Try-with-resources?

**Respuesta:** En DAOs, `Connection`, `PreparedStatement` y `ResultSet` se abren en try-with-resources para cerrarse siempre. El logger y la escritura de PDF también usan try-with-resources / `Files` de `java.nio.file`.

### 19. ¿Dónde hay transacciones (commit/rollback)?

**Respuesta:** Operaciones compuestas, por ejemplo:

- Pago + factura (`PaymentController` / flujo que persiste pago e invoice).
- Préstamo + cambio de estado del recurso a `Prestado` (y al devolver, a `Disponible`).

Se desactiva auto-commit, se ejecutan los pasos y se hace `commit()`; si algo falla, `rollback()` y se lanza `DatabaseException` o `BusinessRuleException`.

### 20. ¿Dónde están las credenciales?

**Respuesta:** En `config.properties` (no versionado). La plantilla es `config.properties.example`. `ConfigLoader` las lee; `.gitignore` excluye `config.properties`, `logs/` e `invoices/`.

### 21. ¿Cómo funciona el log de errores?

**Respuesta:** `AppLogger` escribe en `logs/error_log.txt` con timestamp, nivel, clase y mensaje; en ERROR añade el stack trace. Niveles: INFO, WARNING, ERROR. La vista muestra un mensaje amigable; el detalle técnico queda en el archivo.

---

## Lambdas y Streams (22–25)

### 22. ¿Dónde se usan Streams de forma significativa?

**Respuesta:** Entre otros:

- `GradeController` / `Averages` / `PerformanceReportController`: promedios por materia, por estudiante, por curso (`averagingDouble`, `groupingBy`, `Collectors`).
- `AttendanceController`: inasistencias preocupantes — `filter` por `ABSENT`, `groupingBy` estudiante y semana, conteo > 3, `sorted`.
- `PaymentController` / reportes de morosos: filtrar estudiantes activos sin pensión reciente.
- `LoanController` / reportes: préstamos vencidos.
- Nombres: `Student::getFullName`, `Attendance::getStudentId`, etc.

### 23. Ejemplo concreto de pipeline de inasistencias.

**Respuesta (idea del código):**  
`attendanceDao.findBetween(from, to).stream()`  
→ `filter(a -> status == ABSENT)`  
→ `groupingBy(studentId, groupingBy(weekStart, mapping(date, toSet())))`  
→ filtrar grupos con más de 3 días distintos  
→ ordenar de peor a mejor  
→ armar DTOs con nombres desde un `Map` hecho con `studentDao.findAll().stream().collect(toMap(...))`.

### 24. ¿Por qué `Optional`?

**Respuesta:** Para búsquedas que pueden no encontrar fila (`findById`, `find` de asistencia, etc.) sin devolver `null`. Se usa `orElseGet`, `orElseThrow`, `ifPresent` / `map` según el caso. Evita `NullPointerException` y deja la ausencia de valor explícita.

### 25. ¿BigDecimal o double para notas y dinero?

**Respuesta:** Notas y montos van con `BigDecimal` (o `DECIMAL` mapeado a BigDecimal) para no acumular error de punto flotante. Los promedios se redondean al mostrar. El CHECK de la BD limita la nota a 0.0–5.0; Java valida de nuevo antes de guardar.

---

## Funcional y dominio (26–30)

### 26. ¿Cómo se detecta un moroso si no hay tabla de cuentas por cobrar?

**Respuesta:** Inferencia: estudiante con matrícula activa cuyo último pago del concepto `PENSION` tiene más de 30 días, o que nunca pagó pensión en el período. El reporte usa Streams sobre pagos + matrículas. Documentado en Fase 0 y en el README (problemas conocidos / diseño).

### 27. ¿Cómo se validan cupos de club y conflictos de horario?

**Respuesta:**

- **Club:** se cuentan membresías con estado `Activa` y se compara con `capacidad_maxima` dentro de una transacción / regla de negocio en el controlador.
- **Horario:** tabla `horarios_asignacion` (cambio justificado al DDL). Al asignar un slot se consulta si el mismo profesor tiene otro intervalo que se solape el mismo día.

### 28. ¿Qué contiene la factura PDF y cómo se genera?

**Respuesta:** `PdfBoxInvoiceGenerator` implementa `InvoiceGenerator`. Dibuja en A4: encabezado del colegio (`SchoolInfo` desde config), datos del acudiente y estudiante, ítem (concepto, cantidad, valor, subtotal), IVA según `TaxCalculator`, total, número de factura, CUFE simulado (`CufeGenerator`: hash de número + NIT + fecha), imagen QR (`ZxingQrCodeGenerator`), resolución DIAN simulada. Se guarda como `factura_NNNNNN.pdf` en `invoices/` y se registra en la tabla `facturas`.

### 29. ¿Por qué hay dos tablas de préstamos?

**Respuesta:** El DDL oficial separa `prestamos_estudiante` y `prestamos_profesor`. El código respeta eso con `LoanDao` + `LoanDaoFactory` y `AbstractLoanDaoJdbc` (OCP). El controlador no elige la tabla: pide el DAO según `BorrowerType`.

### 30. ¿Qué harías si el evaluador pide ver el flujo completo de un pago?

**Respuesta (secuencia):**

1. `FinanceView` pide datos y valida formato.
2. `PaymentController` valida reglas, obtiene hermanos activos, elige `DiscountStrategy`, calcula subtotal/IVA/total.
3. Abre transacción: inserta en `pagos` (`PaymentDaoJdbc`), genera PDF, inserta en `facturas` (`InvoiceDaoJdbc`), commit.
4. Si falla el PDF o el INSERT, rollback y log ERROR.
5. La vista muestra éxito y la ruta del PDF.

Puedes dibujar el sequence diagram de la guía de Fase 5.

---

## Consejos para el día de la sustentación

1. Ten abierto el proyecto en el IDE y `docs/database.sql` a mano.
2. Practica explicar **una** operación de punta a punta (pago o préstamo).
3. Si preguntan por la ñ: “columna del DDL oficial; en Java `academicYear`; backticks + UTF-8 en JDBC”.
4. Si preguntan por descuentos sin tabla: “Strategy en memoria; monto final y notas en `pagos`”.
5. Si preguntan por Spring: “No se usa; inyección manual en `AppContext` para cumplir el enunciado de Java estándar”.

*Fin del documento de 30 preguntas — Fase 8.*
