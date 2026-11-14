# Fase 8 — Checklist final, autoevaluación y entrega en GitHub

---

## 1. Checklist final (rúbrica + enunciado)

Marca cada ítem cuando lo verifiques en tu máquina.

### Compilación y ejecución
- [ ] `mvn clean compile` termina en `BUILD SUCCESS`
- [ ] `config.properties` existe (copiado desde `.example`) con usuario/clave correctos
- [ ] `mysql ... < docs/database.sql` ejecutado sin errores en MySQL 8 limpio
- [ ] `mvn exec:java` (o Run de `Main`) abre el menú principal
- [ ] La app no se cae al ingresar letras donde espera números o al dejar campos vacíos

### MVC y código
- [ ] Vistas sin SQL y sin reglas de negocio pesadas
- [ ] Controladores sin `System.out` de negocio y sin SQL embebido
- [ ] DAOs solo acceso a datos; 100 % `PreparedStatement`
- [ ] Código (clases, métodos, variables, Javadoc) en **inglés**
- [ ] Convenciones: PascalCase clases, camelCase métodos/variables, UPPER_SNAKE constantes

### Datos (DML)
- [ ] `docs/database.sql` con Sección 1 DDL + Sección 2 DML
- [ ] ≥30 registros en: estudiantes, acudientes, profesores, materias, registros_academicos, pagos, clubes, recursos
- [ ] Integridad referencial (FKs) al cargar el script

### Calidad exigida por rúbrica
- [ ] SOLID visible (interfaces DAO, inyección por constructor, comentarios `// SOLID:`)
- [ ] Patrones: Singleton (conexión + logger), DAO, Factory, Strategy
- [ ] Lambdas/Streams en promedios, morosos, vencidos, inasistencias, participación
- [ ] `logs/error_log.txt` se crea al haber errores/INFO relevantes
- [ ] Transacciones en pago+factura y préstamo+estado de recurso
- [ ] Facturas PDF en carpeta `invoices/` con CUFE, QR y resolución simulados
- [ ] Menús con cajas, tablas alineadas, opción de volver, mensajes claros

### Documentación y repo
- [ ] `README.md` con las 16 secciones en orden
- [ ] `docs/database.sql`, `docs/schema-changes.sql`, `docs/preguntas-sustentacion.md`
- [ ] `.gitignore` excluye `config.properties`, `logs/`, `invoices/`, `target/`
- [ ] Repo **privado** en GitHub; trainer como colaborador (lectura)
- [ ] Commits atómicos en inglés (`feat:`, `fix:`, `docs:`, `refactor:`)
- [ ] Último commit en `main` / `master`; anotar el **hash SHA completo**

---

## 2. Autoevaluación contra la rúbrica

| Criterio | Pts | Estado esperado | Evidencia en el proyecto |
|----------|----:|-----------------|---------------------------|
| Dominio del código (sustentación) | 49 | Preparado | Código simple + `docs/preguntas-sustentacion.md` (30 Q&A) + guías por fase |
| DML | 5 | Cubierto | `docs/database.sql` Sección 2 (≥30 en 8 tablas) |
| MVC | 10 | Cubierto | Paquetes view / controller / model / dao; diagrama en README §8 |
| SOLID | 10 | Cubierto | Comentarios en código + tabla README §9.1 |
| Patrones | 10 | Cubierto | Singleton, DAO, Factory, Strategy documentados en README §9.2 |
| Lambdas / Streams | 10 | Cubierto | Reportes, promedios, morosos, inasistencias, vencidos |
| Archivos y logs | 8 | Cubierto | `AppLogger`, `logs/error_log.txt`, nio + try-with-resources, PDF |
| JDBC | 5 | Cubierto | PreparedStatement, transacciones, Singleton de conexión, config |
| Consola UX | 10 | Cubierto | `Menu`, `TableRenderer`, validación en `ConsoleInput` / `Validator` |
| Documentación y repo | 6 | Cubierto al cerrar GitHub | README 16 secciones, docs/, .gitignore, commits |

**Total teórico si la sustentación oral sale bien:** 100 pts de rúbrica.

Pendiente **solo en tu entorno local**:

1. Probar de punta a punta cada menú con la BD cargada.
2. Generar al menos una factura real y comprobar el PDF.
3. Publicar el repo privado e invitar al trainer.
4. Guardar el SHA del último commit de `main`.

---

## 3. Guía de entrega en GitHub

### 3.1 Antes del primer push

```bash
cd erp-little-horizons

# Si aún no es un repo
git init
git checkout -b main

# Credenciales NUNCA en el commit
cp config.properties.example config.properties
# edita config.properties con tus datos reales — no hagas add de ese archivo

git status   # no debe aparecer config.properties ni logs/ ni invoices/
```

### 3.2 Commits atómicos (ejemplos en inglés)

```bash
git add pom.xml config.properties.example .gitignore
git commit -m "chore: initial Maven project and config template"

git add src/main/java/com/littlehorizons/erp/model/
git commit -m "feat: domain model POJOs and enums"

git add src/main/java/com/littlehorizons/erp/dao/
git commit -m "feat: DAO interfaces and JDBC implementations"

git add src/main/java/com/littlehorizons/erp/controller/
git commit -m "feat: business controllers with streams and transactions"

git add src/main/java/com/littlehorizons/erp/view/
git commit -m "feat: console views and menus"

git add src/main/java/com/littlehorizons/erp/service/ src/main/java/com/littlehorizons/erp/strategy/
git commit -m "feat: invoice PDF generation and discount strategies"

git add docs/database.sql docs/schema-changes.sql
git commit -m "docs: database DDL and seed DML"

git add README.md docs/preguntas-sustentacion.md docs/checklist-y-entrega.md
git commit -m "docs: README, sustentation questions and delivery checklist"
```

(Ajusta el historial a como hayas trabajado por fases; lo importante es mensajes claros y un historial legible.)

### 3.3 Crear repo privado e invitar al trainer

1. En GitHub: **New repository** → nombre p. ej. `erp-little-horizons` → **Private**.
2. No inicialices con README si ya tienes uno local.
3. Enlaza y sube:

```bash
git remote add origin https://github.com/<tu-usuario>/erp-little-horizons.git
git push -u origin main
```

4. **Settings → Collaborators** → Add people → usuario del trainer → permiso **Read** (o el que indique el enunciado).

### 3.4 SHA del último commit

```bash
git rev-parse HEAD
```

Copia el hash completo (40 caracteres) y guárdalo en el informe o en el mensaje de entrega al trainer.

### 3.5 Qué enviar al trainer

1. URL del repositorio privado.
2. Confirmación de que ya es colaborador.
3. SHA completo del último commit en `main`.
4. (Opcional) Este checklist marcado y una captura del menú principal / de una factura.

### 3.6 Verificación rápida post-push

- Clona en otra carpeta y sigue el README §6–§7.
- Sin `config.properties` el clon no tiene secretos; el evaluador crea el suyo desde `.example`.
- `docs/database.sql` debe bastar para levantar la BD.

---

## 4. Estructura de `docs/` al cerrar la Fase 8

```text
docs/
├── database.sql                 # DDL + DML
├── schema-changes.sql           # Justificación horarios_asignacion
├── preguntas-sustentacion.md    # 30 preguntas con respuesta
├── checklist-y-entrega.md       # Este archivo
├── guia-sustentacion-fase3.md
├── guia-sustentacion-fase4.md
├── guia-sustentacion-fase5.md
├── examples/factura_000123.pdf
└── images/factura-ejemplo.png
```

---

## 5. Recordatorio de riesgos frecuentes en sustentación

| Tema | Respuesta corta |
|------|-----------------|
| ñ en `año_academico` | DDL oficial; Java `academicYear`; backticks + UTF-8 |
| Descuentos sin tabla | Strategy; monto en `pagos.monto_pagado` y detalle en `notas` |
| Morosos | Inferidos por último pago de PENSION > 30 días |
| Dos tablas de préstamos | DDL oficial; Factory + AbstractLoanDaoJdbc |
| ¿Por qué no Spring? | Enunciado: Java estándar; inyección manual en AppContext |
| Factura “DIAN” | Simulada: CUFE hash, QR ZXing, resolución en config |

---

*Fase 8 — checklist y guía de entrega. Completa las casillas en tu entorno y publica el repo.*
