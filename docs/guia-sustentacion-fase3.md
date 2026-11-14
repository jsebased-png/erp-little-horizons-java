# Guía de sustentación – Fase 3 (Controladores con lógica de negocio)

## (a) Qué hace cada clase

**Regla de la capa:** un controlador recibe interfaces DAO por constructor, valida, aplica reglas y lanza
`ErpException`. No tiene SQL, no usa `System.out` y no importa `java.sql`.

| Clase | Qué hace / por qué existe |
|---|---|
| `StudentController` | Registro de estudiante + primer acudiente, edición, retiro lógico (estado Retirado), acudientes (nunca quedar sin ninguno). |
| `SubjectController` | Catálogo de materias y lectura de grados. |
| `EnrollmentController` | Matrícula por grado (una por año) e inscripción en materias (solo del grado del estudiante, por período). |
| `GradeController` | Registrar/corregir notas, promedios (materia, período, curso) con `BigDecimal`, historial. |
| `AttendanceController` | Asistencia diaria (si ya existe, se corrige) y alerta de más de 3 días ausente en una semana. |
| `TeacherController` | Directorio, asignación de materias y horarios con validación de choques. |
| `ClubController` | Clubes, cupo máximo, inscripción simple y múltiple, retiro. |
| `EventController` | Eventos, participantes y posición (solo si el evento es competitivo). |
| `PaymentController` | Pago: descuento (Strategy), IVA, recibo, factura, transacción; morosos; recaudo por rango. |
| `PaymentConceptController` | Catálogo de conceptos y valores base. |
| `ResourceController` | Catálogo de recursos; protege el estado "Prestado". |
| `LoanController` | Préstamos a estudiantes y profesores (elige DAO con una `Function`), devoluciones, vencidos. |
| `PerformanceReportController` | Ranking, promedios por materia, aprobados/reprobados, asistencia por materia. |
| `ParticipationReportController` | Miembros por club, ocupación, participantes por evento, podios. |
| `strategy/*` | `DiscountStrategy` + `NoDiscount`, `SiblingsDiscount`, `ScholarshipDiscount` (base `PercentageDiscount`) y `DiscountStrategyFactory`. |
| `util/Validator` | Validaciones reutilizables (texto, documento, correo, teléfono, fechas, nota 0.0–5.0). |
| `util/Averages` | Promedio exacto con `BigDecimal`, 1 decimal, HALF_UP. |
| `util/TaxCalculator`, `ConceptCodes`, `CufeGenerator` | Regla de IVA, códigos de concepto, hash SHA-384 del CUFE simulado. |
| `dto/*` | Records de solo lectura que devuelven los reportes y el pago (`PaymentResult`). |
| `config/AppContext` | Único lugar donde se hace `new` de DAOs JDBC y controladores (inyección manual). |

## (b) Flujo completo: registrar un pago (vista → controlador → DAO → BD)

1. **Vista (Fase 4)** pide estudiante, concepto, método, período y beca; arma un `PaymentRequest`.
2. **`PaymentController.registerPayment`** valida el método y busca estudiante (ACTIVO) y concepto (activo).
3. Decide el período (los recurrentes lo exigen; los demás quedan "Anual") y bloquea pagar dos veces la matrícula del año.
4. Resuelve el pagador: el acudiente elegido o el responsable financiero (`GuardianDao.findFinancialGuardian`).
5. **Strategy:** `DiscountStrategyFactory.create(...)` devuelve el descuento que más rebaja (hermanos 10 % o beca; no se acumulan) y solo aplica a MAT y PENSION.
6. `TaxCalculator` calcula el IVA (0 % MAT/PENSION, 19 % el resto, valores en `config.properties`); `total = subtotal + IVA`.
7. Se construyen `Payment` (recibo `REC-AAAA-000007`) e `Invoice` (`FE-000123` y CUFE SHA-384 de número + NIT + fecha).
8. **DAO:** `PaymentDao.registerWithInvoice` → `BaseJdbcDao.inTransaction` → `INSERT pagos` + `INSERT facturas` → commit (rollback si algo falla).
9. Devuelve `PaymentResult`; la Fase 5 genera el PDF y llama `InvoiceDao.updateFileInfo`.

## (c) Cinco preguntas probables

1. **¿Por qué no guardas los promedios en la BD?**
   Porque se pueden calcular siempre desde `registros_academicos`; si guardara el promedio, podría quedar desactualizado al corregir una nota. Uso `BigDecimal` (no `double`) y `Averages` redondea siempre igual (1 decimal, HALF_UP). La nota Final no entra en el promedio de períodos.
2. **¿Cómo garantizas que pago y factura se guarden juntos?**
   El controlador solo llama `registerWithInvoice`; el DAO abre una conexión con `setAutoCommit(false)`, hace los dos INSERT y hace `commit`; ante cualquier error, `rollback`. El PDF se crea después del commit.
3. **¿Dónde está Strategy y qué ganas con él?**
   `DiscountStrategy` define `apply` y `describe`. Cada regla es una clase. Si el colegio crea "descuento por hijo de empleado", agrego una clase y la factory, y `PaymentController` no cambia (OCP). La factory elige la estrategia (patrón Factory).
4. **No hay tabla de cuentas por cobrar; ¿cómo detectas morosos?**
   Se infiere: estudiante ACTIVO con matrícula activa cuyo último pago de PENSION tiene más de 30 días (o que nunca pagó y se matriculó hace más de 30 días). Es una decisión de diseño documentada; con una tabla de obligaciones sería exacto por mes.
5. **¿Qué pasa si dos usuarios se inscriben al último cupo de un club?**
   En esta app de consola `joinClub` es `synchronized`, lo que evita la carrera dentro de una sola JVM. Con varios usuarios habría que mover la validación a una transacción del DAO (`SELECT ... FOR UPDATE` sobre el club). Es una limitación conocida.

## (d) Decisiones de diseño y justificación

- **Controladores con lógica, sin capa `service/`:** el enunciado pide MVC; un controlador por área evita una capa extra que tendrías que explicar.
- **Excepciones sin checked:** `ErpException` es `RuntimeException`, así funcionan dentro de lambdas y la vista captura un solo tipo.
- **`AppContext` como raíz de composición:** la inyección por constructor queda visible en un solo archivo (DIP).
- **`LoanController` recibe `Function<BorrowerType, LoanDao>`:** depende de una abstracción; `AppContext` le pasa `loanDaoFactory::create`.
- **Registros (`record`) como DTO:** inmutables y cortos; sirven a los reportes sin ensuciar los POJO de las tablas.
- **Retiro lógico del estudiante:** evita el `ON DELETE CASCADE` que borraría notas y pagos.
- **Ausencias:** se cuentan *días distintos* con "Ausente" en la semana (lunes a domingo), no registros; así faltar a todas las clases de un día cuenta una vez.
- **Alta de estudiante + acudiente:** hay compensación (si falla el vínculo, el estudiante queda Retirado); no es una transacción atómica. Mejora posible: un método transaccional en el DAO.
- **Factura:** el registro existe en la BD antes que el PDF; la Fase 5 debe permitir regenerar el PDF si falla.

## Supuestos que debes confirmar

Descuento por hermanos 10 % (solo MAT y PENSION), IVA 0 % para MAT/PENSION y 19 % para el resto, morosidad 30 días, préstamo máximo 30 días y 3 préstamos sin devolver por persona, nota mínima aprobatoria 3.0, años de matrícula permitidos (año actual ±1).

## Autoevaluación contra la rúbrica (Fase 3)

| Criterio | Estado |
|---|---|
| Dominio del código (49) | Javadoc en cada clase, reglas explicadas en esta guía; falta la guía final de 30 preguntas (Fase 8). |
| MVC (10) | Controladores sin SQL ni `System.out` (verificado con `grep`); las vistas llegan en la Fase 4. |
| SOLID (10) | SRP/DIP/OCP marcados con `// SOLID:`; falta la tabla para el README (Fase 7). |
| Patrones (10) | Strategy + Factory (descuentos), Factory (DAO de préstamos), Singleton (Fase 1), DAO (Fase 2). |
| Lambdas/Streams (10) | `filter`, `map`, `flatMap`, `groupingBy`, `partitioningBy`, `counting`, `reducing`, `reduce`, `averagingDouble`, `sorted`, `Optional`, referencias `::`. |
| Archivos/logs (8) | Logs en pagos y préstamos; el resto de la capa de archivos llega con las facturas (Fase 5). |
| JDBC (5) | Transacciones ya en DAO; los controladores no tocan SQL. |
| Consola (10) | Pendiente (Fase 4). |
| DML (5) / Docs (6) | Pendientes (Fases 6 y 7). |
