-- ============================================================================
-- Little Horizons ERP - docs/database.sql
-- Colegio Internacional Little Horizons S.A.S.
-- MySQL 8.0+ required
--
-- SECTION 1: DDL  (official schema + one justified change)
-- SECTION 2: DML  (realistic Colombian seed data, ≥30 rows in required tables)
--
-- Execution: mysql -u root -p < docs/database.sql
-- ============================================================================

-- ############################################################################
-- SECTION 1: DDL
-- Source of truth: script-bd-pry-erp-colegio-little-horizon.sql
-- Justified change: table horarios_asignacion (see comment below)
-- ############################################################################

DROP DATABASE IF EXISTS erp_little_horizons;

CREATE DATABASE erp_little_horizons
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE erp_little_horizons;

-- ============================================
-- 1. MÓDULO DE ESTUDIANTES Y ACUDIENTES
-- ============================================

CREATE TABLE grados (
    id_grado INT AUTO_INCREMENT PRIMARY KEY,
    nombre_grado VARCHAR(50) NOT NULL UNIQUE COMMENT 'Ej: Preescolar, Primero, Segundo, ... Once',
    nivel_numerico INT NOT NULL UNIQUE COMMENT 'Nivel numérico para ordenamiento (0 para preescolar, 1-11 para grados)',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Catálogo de grados académicos del colegio';

CREATE TABLE estudiantes (
    id_estudiante INT AUTO_INCREMENT PRIMARY KEY,
    tipo_documento ENUM('TI', 'CC', 'CE', 'Pasaporte') NOT NULL DEFAULT 'TI',
    numero_documento VARCHAR(20) NOT NULL UNIQUE,
    primer_nombre VARCHAR(100) NOT NULL,
    segundo_nombre VARCHAR(100),
    primer_apellido VARCHAR(100) NOT NULL,
    segundo_apellido VARCHAR(100),
    fecha_nacimiento DATE NOT NULL,
    genero ENUM('M', 'F', 'Otro') NOT NULL,
    direccion VARCHAR(255),
    telefono VARCHAR(20),
    correo_electronico VARCHAR(100),
    tipo_sangre VARCHAR(5),
    alergias TEXT,
    condiciones_medicas TEXT,
    fecha_ingreso DATE NOT NULL,
    estado ENUM('Activo', 'Graduado', 'Retirado', 'Suspendido') DEFAULT 'Activo',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_documento (tipo_documento, numero_documento),
    INDEX idx_estado (estado),
    INDEX idx_nombre (primer_apellido, segundo_apellido, primer_nombre)
) ENGINE=InnoDB COMMENT='Información completa de estudiantes';

CREATE TABLE acudientes (
    id_acudiente INT AUTO_INCREMENT PRIMARY KEY,
    tipo_documento ENUM('CC', 'CE', 'Pasaporte') NOT NULL DEFAULT 'CC',
    numero_documento VARCHAR(20) NOT NULL UNIQUE,
    primer_nombre VARCHAR(100) NOT NULL,
    segundo_nombre VARCHAR(100),
    primer_apellido VARCHAR(100) NOT NULL,
    segundo_apellido VARCHAR(100),
    parentesco VARCHAR(50) NOT NULL COMMENT 'Padre, Madre, Abuelo, Tío, Tutor Legal, etc.',
    telefono_principal VARCHAR(20) NOT NULL,
    telefono_secundario VARCHAR(20),
    correo_electronico VARCHAR(100),
    direccion VARCHAR(255),
    ocupacion VARCHAR(100),
    es_responsable_financiero BOOLEAN DEFAULT TRUE COMMENT 'Indica si es responsable de pagos',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_documento (tipo_documento, numero_documento),
    INDEX idx_nombre (primer_apellido, segundo_apellido)
) ENGINE=InnoDB COMMENT='Acudientes y tutores legales';

CREATE TABLE estudiante_acudiente (
    id_estudiante INT NOT NULL,
    id_acudiente INT NOT NULL,
    es_principal BOOLEAN DEFAULT FALSE COMMENT 'Acudiente principal para comunicaciones',
    es_contacto_emergencia BOOLEAN DEFAULT TRUE COMMENT 'Disponible para emergencias',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_estudiante, id_acudiente),
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_acudiente) REFERENCES acudientes(id_acudiente) ON DELETE CASCADE,
    INDEX idx_estudiante (id_estudiante),
    INDEX idx_acudiente (id_acudiente)
) ENGINE=InnoDB COMMENT='Relación entre estudiantes y sus acudientes';

CREATE TABLE matriculas (
    id_matricula INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_grado INT NOT NULL,
    año_academico INT NOT NULL COMMENT 'Año académico (ej: 2024)',
    estado_matricula ENUM('Activa', 'Completada', 'Retirada') DEFAULT 'Activa',
    fecha_matricula DATE NOT NULL,
    fecha_retiro DATE,
    observaciones TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_grado) REFERENCES grados(id_grado),
    UNIQUE KEY matricula_unica (id_estudiante, año_academico),
    INDEX idx_año_grado (año_academico, id_grado),
    INDEX idx_estado (estado_matricula)
) ENGINE=InnoDB COMMENT='Historial de matrículas de estudiantes por año y grado';

-- ============================================
-- 2. MÓDULO ACADÉMICO
-- ============================================

CREATE TABLE materias (
    id_materia INT AUTO_INCREMENT PRIMARY KEY,
    codigo_materia VARCHAR(20) NOT NULL UNIQUE COMMENT 'Código único de la materia (ej: MAT-5, ESP-3)',
    nombre_materia VARCHAR(100) NOT NULL,
    descripcion TEXT,
    id_grado INT NOT NULL COMMENT 'Grado al que pertenece la materia',
    horas_semanales INT DEFAULT 2 COMMENT 'Horas semanales asignadas',
    esta_activa BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_grado) REFERENCES grados(id_grado),
    INDEX idx_grado (id_grado),
    INDEX idx_activa (esta_activa),
    INDEX idx_codigo (codigo_materia)
) ENGINE=InnoDB COMMENT='Catálogo de materias por grado';

CREATE TABLE profesores (
    id_profesor INT AUTO_INCREMENT PRIMARY KEY,
    tipo_documento ENUM('CC', 'CE', 'Pasaporte') NOT NULL DEFAULT 'CC',
    numero_documento VARCHAR(20) NOT NULL UNIQUE,
    primer_nombre VARCHAR(100) NOT NULL,
    segundo_nombre VARCHAR(100),
    primer_apellido VARCHAR(100) NOT NULL,
    segundo_apellido VARCHAR(100),
    correo_electronico VARCHAR(100) NOT NULL UNIQUE,
    telefono VARCHAR(20),
    especializacion VARCHAR(100) COMMENT 'Área de especialización (Matemáticas, Español, etc.)',
    fecha_contratacion DATE NOT NULL,
    estado ENUM('Activo', 'Inactivo', 'Licencia') DEFAULT 'Activo',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_documento (tipo_documento, numero_documento),
    INDEX idx_estado (estado),
    INDEX idx_nombre (primer_apellido, segundo_apellido)
) ENGINE=InnoDB COMMENT='Información de profesores';

CREATE TABLE asignaciones_profesor (
    id_asignacion INT AUTO_INCREMENT PRIMARY KEY,
    id_profesor INT NOT NULL,
    id_materia INT NOT NULL,
    año_academico INT NOT NULL,
    seccion VARCHAR(10) COMMENT 'Sección o grupo (A, B, C)',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_profesor) REFERENCES profesores(id_profesor) ON DELETE CASCADE,
    FOREIGN KEY (id_materia) REFERENCES materias(id_materia) ON DELETE CASCADE,
    UNIQUE KEY asignacion_unica (id_profesor, id_materia, año_academico, seccion),
    INDEX idx_profesor_año (id_profesor, año_academico),
    INDEX idx_materia_año (id_materia, año_academico)
) ENGINE=InnoDB COMMENT='Asignación de profesores a materias por año académico';

-- JUSTIFIED CHANGE (requirements: "días y horas con validación de conflictos")
-- asignaciones_profesor only stores section; this child table holds day/time slots.
CREATE TABLE horarios_asignacion (
    id_horario INT AUTO_INCREMENT PRIMARY KEY,
    id_asignacion INT NOT NULL,
    dia_semana ENUM('Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado') NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_asignacion) REFERENCES asignaciones_profesor(id_asignacion) ON DELETE CASCADE,
    CHECK (hora_fin > hora_inicio),
    INDEX idx_asignacion (id_asignacion),
    INDEX idx_dia_hora (dia_semana, hora_inicio)
) ENGINE=InnoDB COMMENT='Weekly time slots of a teacher assignment';

CREATE TABLE inscripciones_materia (
    id_inscripcion INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_materia INT NOT NULL,
    año_academico INT NOT NULL,
    periodo_academico ENUM('I', 'II', 'III', 'IV') NOT NULL COMMENT 'Período académico',
    seccion VARCHAR(10) COMMENT 'Sección o grupo asignado',
    fecha_inscripcion DATE NOT NULL,
    estado ENUM('Activa', 'Retirada', 'Completada') DEFAULT 'Activa',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_materia) REFERENCES materias(id_materia) ON DELETE CASCADE,
    UNIQUE KEY inscripcion_unica (id_estudiante, id_materia, año_academico, periodo_academico),
    INDEX idx_estudiante_año (id_estudiante, año_academico),
    INDEX idx_materia_periodo (id_materia, periodo_academico)
) ENGINE=InnoDB COMMENT='Inscripción de estudiantes en materias específicas';

CREATE TABLE registros_academicos (
    id_registro INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_materia INT NOT NULL,
    id_profesor INT NOT NULL,
    año_academico INT NOT NULL,
    periodo_academico ENUM('I', 'II', 'III', 'IV', 'Final') NOT NULL,
    valor_nota DECIMAL(2,1) NOT NULL COMMENT 'Nota de 0.0 a 5.0 según estándar colombiano',
    observaciones TEXT,
    fecha_registro DATE NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_materia) REFERENCES materias(id_materia) ON DELETE CASCADE,
    FOREIGN KEY (id_profesor) REFERENCES profesores(id_profesor),
    CHECK (valor_nota >= 0.0 AND valor_nota <= 5.0),
    UNIQUE KEY registro_unico (id_estudiante, id_materia, año_academico, periodo_academico),
    INDEX idx_estudiante_año (id_estudiante, año_academico),
    INDEX idx_materia_periodo (id_materia, periodo_academico),
    INDEX idx_valor_nota (valor_nota)
) ENGINE=InnoDB COMMENT='Registro de calificaciones de estudiantes';

CREATE TABLE asistencias (
    id_asistencia INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_materia INT NOT NULL,
    fecha_asistencia DATE NOT NULL,
    estado_asistencia ENUM('Presente', 'Ausente', 'Tardanza', 'Excusa Médica') NOT NULL,
    observaciones TEXT,
    registrado_por INT COMMENT 'ID del profesor que registró la asistencia',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_materia) REFERENCES materias(id_materia) ON DELETE CASCADE,
    FOREIGN KEY (registrado_por) REFERENCES profesores(id_profesor),
    UNIQUE KEY asistencia_unica (id_estudiante, id_materia, fecha_asistencia),
    INDEX idx_estudiante_fecha (id_estudiante, fecha_asistencia),
    INDEX idx_materia_fecha (id_materia, fecha_asistencia),
    INDEX idx_estado (estado_asistencia)
) ENGINE=InnoDB COMMENT='Registro de asistencia diaria de estudiantes';

-- ============================================
-- 3. MÓDULO DE ACTIVIDADES EXTRACURRICULARES
-- ============================================

CREATE TABLE clubes (
    id_club INT AUTO_INCREMENT PRIMARY KEY,
    nombre_club VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    id_profesor_supervisor INT COMMENT 'Profesor encargado del club',
    capacidad_maxima INT DEFAULT 30 COMMENT 'Cupo máximo de estudiantes',
    horario_reunion VARCHAR(100) COMMENT 'Días y horario de reunión (ej: Martes 3-5 PM)',
    esta_activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_profesor_supervisor) REFERENCES profesores(id_profesor),
    INDEX idx_activo (esta_activo),
    INDEX idx_nombre (nombre_club)
) ENGINE=InnoDB COMMENT='Catálogo de clubes extracurriculares';

CREATE TABLE membresias_club (
    id_membresia INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_club INT NOT NULL,
    fecha_ingreso DATE NOT NULL,
    estado_membresia ENUM('Activa', 'Retirada') DEFAULT 'Activa',
    fecha_retiro DATE,
    motivo_retiro TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_club) REFERENCES clubes(id_club) ON DELETE CASCADE,
    INDEX idx_estudiante (id_estudiante),
    INDEX idx_club_estado (id_club, estado_membresia)
) ENGINE=InnoDB COMMENT='Membresías de estudiantes en clubes';

CREATE TABLE eventos (
    id_evento INT AUTO_INCREMENT PRIMARY KEY,
    nombre_evento VARCHAR(150) NOT NULL,
    descripcion TEXT,
    tipo_evento ENUM('Académico', 'Cultural', 'Deportivo', 'Social', 'Científico') NOT NULL,
    fecha_evento DATE NOT NULL,
    lugar VARCHAR(150),
    tiene_competencia BOOLEAN DEFAULT FALSE COMMENT 'Indica si el evento tiene competencia con posiciones',
    id_organizador INT COMMENT 'Profesor organizador del evento',
    esta_activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_organizador) REFERENCES profesores(id_profesor),
    INDEX idx_fecha (fecha_evento),
    INDEX idx_tipo (tipo_evento),
    INDEX idx_activo (esta_activo)
) ENGINE=InnoDB COMMENT='Catálogo de eventos del colegio';

CREATE TABLE participantes_evento (
    id_participacion INT AUTO_INCREMENT PRIMARY KEY,
    id_evento INT NOT NULL,
    id_estudiante INT NOT NULL,
    rol_participacion ENUM('Participante', 'Competidor', 'Organizador', 'Asistente') DEFAULT 'Participante',
    posicion_obtenida VARCHAR(20) COMMENT 'Posición obtenida si hay competencia (1er Lugar, 2do Lugar, 3er Lugar, Participante)',
    observaciones TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_evento) REFERENCES eventos(id_evento) ON DELETE CASCADE,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    UNIQUE KEY participacion_unica (id_evento, id_estudiante),
    INDEX idx_evento (id_evento),
    INDEX idx_estudiante (id_estudiante)
) ENGINE=InnoDB COMMENT='Registro de participación de estudiantes en eventos';

-- ============================================
-- 4. MÓDULO FINANCIERO
-- ============================================

CREATE TABLE conceptos_pago (
    id_concepto INT AUTO_INCREMENT PRIMARY KEY,
    codigo_concepto VARCHAR(20) NOT NULL UNIQUE,
    nombre_concepto VARCHAR(100) NOT NULL,
    descripcion TEXT,
    monto_base DECIMAL(10,2) NOT NULL COMMENT 'Valor base del concepto en pesos colombianos',
    es_recurrente BOOLEAN DEFAULT FALSE COMMENT 'Indica si es un pago recurrente (ej: pensión mensual)',
    esta_activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_activo (esta_activo),
    INDEX idx_codigo (codigo_concepto)
) ENGINE=InnoDB COMMENT='Catálogo de conceptos de pago del colegio';

CREATE TABLE pagos (
    id_pago INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_acudiente INT COMMENT 'Acudiente que realizó el pago',
    id_concepto INT NOT NULL,
    monto_pagado DECIMAL(10,2) NOT NULL,
    fecha_pago DATE NOT NULL,
    metodo_pago ENUM('Efectivo', 'Transferencia', 'Tarjeta Débito', 'Tarjeta Crédito', 'Cheque') NOT NULL,
    numero_recibo VARCHAR(50) UNIQUE,
    año_academico INT COMMENT 'Año al que aplica el pago',
    periodo_academico ENUM('I', 'II', 'III', 'IV', 'Anual') COMMENT 'Período al que aplica el pago',
    notas TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_acudiente) REFERENCES acudientes(id_acudiente),
    FOREIGN KEY (id_concepto) REFERENCES conceptos_pago(id_concepto),
    INDEX idx_estudiante_fecha (id_estudiante, fecha_pago),
    INDEX idx_concepto (id_concepto),
    INDEX idx_fecha (fecha_pago),
    INDEX idx_recibo (numero_recibo)
) ENGINE=InnoDB COMMENT='Registro de todos los pagos realizados';

CREATE TABLE facturas (
    id_factura INT AUTO_INCREMENT PRIMARY KEY,
    numero_factura VARCHAR(50) NOT NULL UNIQUE COMMENT 'Número consecutivo de factura',
    id_pago INT NOT NULL COMMENT 'Pago asociado a esta factura',
    fecha_emision DATE NOT NULL,
    cufe VARCHAR(100) COMMENT 'Código Único de Factura Electrónica (simulado)',
    nombre_archivo_pdf VARCHAR(255) COMMENT 'Nombre del archivo PDF generado',
    ruta_archivo VARCHAR(500) COMMENT 'Ruta completa donde se guardó el PDF',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_pago) REFERENCES pagos(id_pago) ON DELETE CASCADE,
    INDEX idx_numero_factura (numero_factura),
    INDEX idx_fecha_emision (fecha_emision),
    INDEX idx_pago (id_pago)
) ENGINE=InnoDB COMMENT='Registro de facturas electrónicas generadas';

-- ============================================
-- 5. MÓDULO DE GESTIÓN DE RECURSOS
-- ============================================

CREATE TABLE categorias_recurso (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre_categoria VARCHAR(50) NOT NULL UNIQUE,
    descripcion TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Categorías de recursos (Libro, Equipo de Laboratorio, etc.)';

CREATE TABLE recursos (
    id_recurso INT AUTO_INCREMENT PRIMARY KEY,
    codigo_recurso VARCHAR(50) NOT NULL UNIQUE COMMENT 'Código de inventario único',
    nombre_recurso VARCHAR(150) NOT NULL,
    id_categoria INT NOT NULL,
    descripcion TEXT,
    fecha_adquisicion DATE,
    ubicacion_fisica VARCHAR(100) COMMENT 'Ubicación física (Biblioteca, Laboratorio 1, etc.)',
    estado_recurso ENUM('Disponible', 'Prestado', 'En Mantenimiento', 'Dado de Baja') DEFAULT 'Disponible',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_categoria) REFERENCES categorias_recurso(id_categoria),
    INDEX idx_codigo (codigo_recurso),
    INDEX idx_estado (estado_recurso),
    INDEX idx_categoria (id_categoria),
    INDEX idx_nombre (nombre_recurso)
) ENGINE=InnoDB COMMENT='Inventario de recursos prestables del colegio';

CREATE TABLE prestamos_estudiante (
    id_prestamo INT AUTO_INCREMENT PRIMARY KEY,
    id_recurso INT NOT NULL,
    id_estudiante INT NOT NULL COMMENT 'Estudiante que toma prestado el recurso',
    fecha_prestamo DATE NOT NULL,
    fecha_devolucion_esperada DATE NOT NULL,
    fecha_devolucion_real DATE,
    estado_prestamo ENUM('Activo', 'Devuelto', 'Vencido') DEFAULT 'Activo',
    observaciones TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_recurso) REFERENCES recursos(id_recurso) ON DELETE CASCADE,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    INDEX idx_recurso_estado (id_recurso, estado_prestamo),
    INDEX idx_estudiante (id_estudiante),
    INDEX idx_estado (estado_prestamo),
    INDEX idx_fecha_devolucion (fecha_devolucion_esperada)
) ENGINE=InnoDB COMMENT='Registro de préstamos de recursos a estudiantes';

CREATE TABLE prestamos_profesor (
    id_prestamo INT AUTO_INCREMENT PRIMARY KEY,
    id_recurso INT NOT NULL,
    id_profesor INT NOT NULL COMMENT 'Profesor que toma prestado el recurso',
    fecha_prestamo DATE NOT NULL,
    fecha_devolucion_esperada DATE NOT NULL,
    fecha_devolucion_real DATE,
    estado_prestamo ENUM('Activo', 'Devuelto', 'Vencido') DEFAULT 'Activo',
    observaciones TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_recurso) REFERENCES recursos(id_recurso) ON DELETE CASCADE,
    FOREIGN KEY (id_profesor) REFERENCES profesores(id_profesor) ON DELETE CASCADE,
    INDEX idx_recurso_estado (id_recurso, estado_prestamo),
    INDEX idx_profesor (id_profesor),
    INDEX idx_estado (estado_prestamo),
    INDEX idx_fecha_devolucion (fecha_devolucion_esperada)
) ENGINE=InnoDB COMMENT='Registro de préstamos de recursos a profesores';

-- ============================================
-- DATOS INICIALES BÁSICOS (from official script)
-- ============================================

INSERT INTO grados (nombre_grado, nivel_numerico) VALUES
('Preescolar', 0),
('Primero', 1),
('Segundo', 2),
('Tercero', 3),
('Cuarto', 4),
('Quinto', 5),
('Sexto', 6),
('Séptimo', 7),
('Octavo', 8),
('Noveno', 9),
('Décimo', 10),
('Once', 11);

INSERT INTO categorias_recurso (nombre_categoria, descripcion) VALUES
('Libro', 'Libros de la biblioteca escolar'),
('Equipo de Laboratorio', 'Equipos científicos y de laboratorio (microscopios, balanzas, etc.)'),
('Equipo Deportivo', 'Equipos y materiales deportivos (balones, conos, etc.)'),
('Dispositivo Tecnológico', 'Tablets, calculadoras científicas, computadores portátiles'),
('Material Didáctico', 'Materiales educativos diversos (mapas, modelos, etc.)');

INSERT INTO conceptos_pago (codigo_concepto, nombre_concepto, descripcion, monto_base, es_recurrente) VALUES
('MAT', 'Matrícula', 'Pago anual de matrícula', 500000.00, FALSE),
('PENSION', 'Pensión Mensual', 'Pago mensual de pensión', 350000.00, TRUE),
('UNIFORME', 'Uniforme', 'Uniforme escolar completo', 250000.00, FALSE),
('TRANSPORTE', 'Transporte Escolar', 'Servicio de transporte mensual', 150000.00, TRUE),
('ALIMENTACION', 'Servicio de Alimentación', 'Almuerzo escolar mensual', 120000.00, TRUE),
('ACTIVIDAD', 'Actividad Especial', 'Salida pedagógica o actividad especial', 50000.00, FALSE),
('CERTIFICADO', 'Certificado', 'Certificado de estudio o constancia', 15000.00, FALSE);


-- ############################################################################
-- SECTION 2: DML (seed data)
-- Realistic Colombian names, COP amounts, referential integrity.
-- Counts (approx): estudiantes 35, acudientes 32, profesores 32, materias 36,
-- registros_academicos 40+, pagos 35+, clubes 30, recursos 32.
-- Academic year used: 2025 and 2026 where relevant.
-- ############################################################################

-- ============================================
-- PROFESSORS (32)
-- ============================================
INSERT INTO profesores (tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, correo_electronico, telefono, especializacion, fecha_contratacion, estado) VALUES
('CC', '1098765432', 'Carlos', 'Andrés', 'Rodríguez', 'Pérez', 'carlos.rodriguez@littlehorizons.edu.co', '3001234567', 'Matemáticas', '2018-02-01', 'Activo'),
('CC', '1098765433', 'María', 'Elena', 'González', 'López', 'maria.gonzalez@littlehorizons.edu.co', '3001234568', 'Español y Literatura', '2017-08-15', 'Activo'),
('CC', '1098765434', 'Juan', 'Pablo', 'Martínez', 'García', 'juan.martinez@littlehorizons.edu.co', '3001234569', 'Ciencias Naturales', '2019-01-20', 'Activo'),
('CC', '1098765435', 'Ana', 'Lucía', 'Hernández', 'Ruiz', 'ana.hernandez@littlehorizons.edu.co', '3001234570', 'Inglés', '2016-03-10', 'Activo'),
('CC', '1098765436', 'Luis', 'Fernando', 'Sánchez', 'Díaz', 'luis.sanchez@littlehorizons.edu.co', '3001234571', 'Educación Física', '2020-07-01', 'Activo'),
('CC', '1098765437', 'Patricia', 'Isabel', 'Ramírez', 'Torres', 'patricia.ramirez@littlehorizons.edu.co', '3001234572', 'Ciencias Sociales', '2018-09-12', 'Activo'),
('CC', '1098765438', 'Diego', 'Alejandro', 'Moreno', 'Castro', 'diego.moreno@littlehorizons.edu.co', '3001234573', 'Física', '2019-05-22', 'Activo'),
('CC', '1098765439', 'Carmen', 'Rosa', 'Vargas', 'Mendoza', 'carmen.vargas@littlehorizons.edu.co', '3001234574', 'Química', '2017-11-03', 'Activo'),
('CC', '1098765440', 'Andrés', 'Felipe', 'Jiménez', 'Ortiz', 'andres.jimenez@littlehorizons.edu.co', '3001234575', 'Biología', '2021-02-15', 'Activo'),
('CC', '1098765441', 'Laura', 'Beatriz', 'Castro', 'Silva', 'laura.castro@littlehorizons.edu.co', '3001234576', 'Artes Plásticas', '2018-04-18', 'Activo'),
('CC', '1098765442', 'Ricardo', 'José', 'López', 'Navarro', 'ricardo.lopez@littlehorizons.edu.co', '3001234577', 'Música', '2019-08-25', 'Activo'),
('CC', '1098765443', 'Sandra', 'Milena', 'Díaz', 'Rojas', 'sandra.diaz@littlehorizons.edu.co', '3001234578', 'Informática', '2020-01-10', 'Activo'),
('CC', '1098765444', 'Fernando', 'Alberto', 'Pérez', 'Gómez', 'fernando.perez@littlehorizons.edu.co', '3001234579', 'Filosofía', '2016-06-30', 'Activo'),
('CC', '1098765445', 'Gloria', 'Patricia', 'Torres', 'Vargas', 'gloria.torres@littlehorizons.edu.co', '3001234580', 'Ética y Valores', '2017-03-14', 'Activo'),
('CC', '1098765446', 'Jorge', 'Luis', 'Mendoza', 'Cárdenas', 'jorge.mendoza@littlehorizons.edu.co', '3001234581', 'Geografía', '2018-10-05', 'Activo'),
('CC', '1098765447', 'Mónica', 'Andrea', 'Ruiz', 'Pineda', 'monica.ruiz@littlehorizons.edu.co', '3001234582', 'Historia', '2019-12-01', 'Activo'),
('CC', '1098765448', 'Óscar', 'Iván', 'Cárdenas', 'Mejía', 'oscar.cardenas@littlehorizons.edu.co', '3001234583', 'Matemáticas', '2021-03-20', 'Activo'),
('CC', '1098765449', 'Claudia', 'María', 'Mejía', 'Soto', 'claudia.mejia@littlehorizons.edu.co', '3001234584', 'Español', '2018-07-08', 'Activo'),
('CC', '1098765450', 'Héctor', 'Manuel', 'Soto', 'León', 'hector.soto@littlehorizons.edu.co', '3001234585', 'Inglés', '2020-09-15', 'Activo'),
('CC', '1098765451', 'Elena', 'Sofía', 'León', 'Pardo', 'elena.leon@littlehorizons.edu.co', '3001234586', 'Ciencias Naturales', '2017-05-22', 'Activo'),
('CC', '1098765452', 'Raúl', 'Antonio', 'Pardo', 'Nieto', 'raul.pardo@littlehorizons.edu.co', '3001234587', 'Educación Física', '2019-04-11', 'Activo'),
('CC', '1098765453', 'Diana', 'Carolina', 'Nieto', 'Beltrán', 'diana.nieto@littlehorizons.edu.co', '3001234588', 'Preescolar', '2018-01-25', 'Activo'),
('CC', '1098765454', 'Pedro', 'Enrique', 'Beltrán', 'Quiroga', 'pedro.beltran@littlehorizons.edu.co', '3001234589', 'Preescolar', '2020-02-14', 'Activo'),
('CC', '1098765455', 'Isabel', 'Cristina', 'Quiroga', 'Acosta', 'isabel.quiroga@littlehorizons.edu.co', '3001234590', 'Primaria', '2016-09-01', 'Activo'),
('CC', '1098765456', 'Mauricio', 'David', 'Acosta', 'Vega', 'mauricio.acosta@littlehorizons.edu.co', '3001234591', 'Primaria', '2021-06-18', 'Activo'),
('CC', '1098765457', 'Natalia', 'Andrea', 'Vega', 'Rincón', 'natalia.vega@littlehorizons.edu.co', '3001234592', 'Química', '2019-10-30', 'Activo'),
('CC', '1098765458', 'Sergio', 'Camilo', 'Rincón', 'Salazar', 'sergio.rincon@littlehorizons.edu.co', '3001234593', 'Física', '2018-12-12', 'Activo'),
('CC', '1098765459', 'Adriana', 'Lucía', 'Salazar', 'Guerrero', 'adriana.salazar@littlehorizons.edu.co', '3001234594', 'Biología', '2020-04-05', 'Activo'),
('CC', '1098765460', 'Felipe', 'Andrés', 'Guerrero', 'Mora', 'felipe.guerrero@littlehorizons.edu.co', '3001234595', 'Informática', '2017-08-20', 'Activo'),
('CC', '1098765461', 'Valentina', 'Sofía', 'Mora', 'Peña', 'valentina.mora@littlehorizons.edu.co', '3001234596', 'Artes', '2021-01-08', 'Activo'),
('CC', '1098765462', 'Camilo', 'José', 'Peña', 'Cortés', 'camilo.pena@littlehorizons.edu.co', '3001234597', 'Música', '2019-07-17', 'Activo'),
('CC', '1098765463', 'Juliana', 'María', 'Cortés', 'Bautista', 'juliana.cortes@littlehorizons.edu.co', '3001234598', 'Ética', '2018-05-28', 'Activo');

-- ============================================
-- GUARDIANS / ACUDIENTES (32)
-- ============================================
INSERT INTO acudientes (tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, parentesco, telefono_principal, telefono_secundario, correo_electronico, direccion, ocupacion, es_responsable_financiero) VALUES
('CC', '912345678', 'Roberto', 'Carlos', 'Gómez', 'Sánchez', 'Padre', '3101112233', '6012345678', 'roberto.gomez@email.com', 'Calle 36 # 15-20, Bucaramanga', 'Ingeniero Civil', TRUE),
('CC', '912345679', 'María', 'Fernanda', 'Gómez', 'López', 'Madre', '3101112234', NULL, 'maria.gomez@email.com', 'Calle 36 # 15-20, Bucaramanga', 'Contadora', TRUE),
('CC', '912345680', 'José', 'Antonio', 'Ramírez', 'Vargas', 'Padre', '3101112235', '6012345679', 'jose.ramirez@email.com', 'Carrera 27 # 42-10, Bucaramanga', 'Médico', TRUE),
('CC', '912345681', 'Claudia', 'Patricia', 'Ramírez', 'Mendoza', 'Madre', '3101112236', NULL, 'claudia.ramirez@email.com', 'Carrera 27 # 42-10, Bucaramanga', 'Abogada', FALSE),
('CC', '912345682', 'Luis', 'Eduardo', 'Torres', 'Castro', 'Padre', '3101112237', '6012345680', 'luis.torres@email.com', 'Avenida González Valencia # 48-30', 'Empresario', TRUE),
('CC', '912345683', 'Sandra', 'Liliana', 'Torres', 'Díaz', 'Madre', '3101112238', NULL, 'sandra.torres@email.com', 'Avenida González Valencia # 48-30', 'Diseñadora', TRUE),
('CC', '912345684', 'Carlos', 'Alberto', 'Mendoza', 'Ruiz', 'Padre', '3101112239', '6012345681', 'carlos.mendoza@email.com', 'Calle 45 # 28-15, Floridablanca', 'Arquitecto', TRUE),
('CC', '912345685', 'Patricia', 'Elena', 'Mendoza', 'Soto', 'Madre', '3101112240', NULL, 'patricia.mendoza@email.com', 'Calle 45 # 28-15, Floridablanca', 'Profesora', FALSE),
('CC', '912345686', 'Andrés', 'Felipe', 'Vargas', 'León', 'Padre', '3101112241', '6012345682', 'andres.vargas@email.com', 'Carrera 33 # 52-08, Bucaramanga', 'Ingeniero de Sistemas', TRUE),
('CC', '912345687', 'Laura', 'Catalina', 'Vargas', 'Pardo', 'Madre', '3101112242', NULL, 'laura.vargas@email.com', 'Carrera 33 # 52-08, Bucaramanga', 'Psicóloga', TRUE),
('CC', '912345688', 'Diego', 'Hernán', 'Castro', 'Nieto', 'Padre', '3101112243', '6012345683', 'diego.castro@email.com', 'Calle 56 # 21-40, Piedecuesta', 'Comerciante', TRUE),
('CC', '912345689', 'Mónica', 'Andrea', 'Castro', 'Beltrán', 'Madre', '3101112244', NULL, 'monica.castro@email.com', 'Calle 56 # 21-40, Piedecuesta', 'Administradora', TRUE),
('CC', '912345690', 'Jorge', 'Iván', 'Díaz', 'Quiroga', 'Padre', '3101112245', '6012345684', 'jorge.diaz@email.com', 'Carrera 15 # 34-22, Bucaramanga', 'Veterinario', TRUE),
('CC', '912345691', 'Elena', 'María', 'Díaz', 'Acosta', 'Madre', '3101112246', NULL, 'elena.diaz@email.com', 'Carrera 15 # 34-22, Bucaramanga', 'Enfermera', FALSE),
('CC', '912345692', 'Fernando', 'José', 'Ruiz', 'Vega', 'Padre', '3101112247', '6012345685', 'fernando.ruiz@email.com', 'Calle 12 # 8-50, Girón', 'Agrónomo', TRUE),
('CC', '912345693', 'Gloria', 'Isabel', 'Ruiz', 'Rincón', 'Madre', '3101112248', NULL, 'gloria.ruiz@email.com', 'Calle 12 # 8-50, Girón', 'Ama de casa', TRUE),
('CC', '912345694', 'Héctor', 'Manuel', 'Soto', 'Salazar', 'Padre', '3101112249', '6012345686', 'hector.soto.g@email.com', 'Avenida Quebrada Seca # 18-09', 'Contador Público', TRUE),
('CC', '912345695', 'Diana', 'Carolina', 'Soto', 'Guerrero', 'Madre', '3101112250', NULL, 'diana.soto@email.com', 'Avenida Quebrada Seca # 18-09', 'Ingeniera Industrial', TRUE),
('CC', '912345696', 'Pedro', 'Luis', 'León', 'Mora', 'Padre', '3101112251', '6012345687', 'pedro.leon@email.com', 'Calle 72 # 30-14, Bucaramanga', 'Mecánico', TRUE),
('CC', '912345697', 'Isabel', 'Andrea', 'León', 'Peña', 'Madre', '3101112252', NULL, 'isabel.leon@email.com', 'Calle 72 # 30-14, Bucaramanga', 'Secretaria', TRUE),
('CC', '912345698', 'Mauricio', 'Andrés', 'Pardo', 'Cortés', 'Padre', '3101112253', '6012345688', 'mauricio.pardo@email.com', 'Carrera 21 # 45-67, Floridablanca', 'Abogado', TRUE),
('CC', '912345699', 'Natalia', 'Sofía', 'Pardo', 'Bautista', 'Madre', '3101112254', NULL, 'natalia.pardo@email.com', 'Carrera 21 # 45-67, Floridablanca', 'Periodista', FALSE),
('CC', '912345700', 'Sergio', 'Camilo', 'Nieto', 'Gómez', 'Padre', '3101112255', '6012345689', 'sergio.nieto@email.com', 'Calle 29 # 16-33, Bucaramanga', 'Docente Universitario', TRUE),
('CC', '912345701', 'Adriana', 'Lucía', 'Nieto', 'Ramírez', 'Madre', '3101112256', NULL, 'adriana.nieto@email.com', 'Calle 29 # 16-33, Bucaramanga', 'Farmacéutica', TRUE),
('CC', '912345702', 'Felipe', 'José', 'Beltrán', 'Torres', 'Padre', '3101112257', '6012345690', 'felipe.beltran@email.com', 'Carrera 35 # 50-12, Piedecuesta', 'Ingeniero Eléctrico', TRUE),
('CC', '912345703', 'Valentina', 'María', 'Beltrán', 'Mendoza', 'Madre', '3101112258', NULL, 'valentina.beltran@email.com', 'Carrera 35 # 50-12, Piedecuesta', 'Nutricionista', TRUE),
('CC', '912345704', 'Camilo', 'Andrés', 'Quiroga', 'Vargas', 'Padre', '3101112259', '6012345691', 'camilo.quiroga@email.com', 'Calle 48 # 22-05, Bucaramanga', 'Comerciante', TRUE),
('CC', '912345705', 'Juliana', 'Sofía', 'Quiroga', 'Castro', 'Madre', '3101112260', NULL, 'juliana.quiroga@email.com', 'Calle 48 # 22-05, Bucaramanga', 'Estilista', TRUE),
('CC', '912345706', 'Daniel', 'Fernando', 'Acosta', 'Díaz', 'Padre', '3101112261', '6012345692', 'daniel.acosta@email.com', 'Avenida La Rosita # 9-40', 'Piloto', TRUE),
('CC', '912345707', 'Camila', 'Andrea', 'Acosta', 'Ruiz', 'Madre', '3101112262', NULL, 'camila.acosta@email.com', 'Avenida La Rosita # 9-40', 'Azafata', FALSE),
('CC', '912345708', 'Sebastián', 'Luis', 'Vega', 'Soto', 'Padre', '3101112263', '6012345693', 'sebastian.vega@email.com', 'Calle 60 # 14-28, Floridablanca', 'Economista', TRUE),
('CC', '912345709', 'Mariana', 'Isabel', 'Vega', 'León', 'Madre', '3101112264', NULL, 'mariana.vega@email.com', 'Calle 60 # 14-28, Floridablanca', 'Administradora de Empresas', TRUE);

-- ============================================
-- STUDENTS (35)
-- ============================================
INSERT INTO estudiantes (tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, fecha_nacimiento, genero, direccion, telefono, correo_electronico, tipo_sangre, alergias, condiciones_medicas, fecha_ingreso, estado) VALUES
('TI', '1001234567', 'Santiago', 'Andrés', 'Gómez', 'Gómez', '2012-03-15', 'M', 'Calle 36 # 15-20, Bucaramanga', '3201112233', 'santiago.gomez@student.lh.edu.co', 'O+', NULL, NULL, '2024-01-20', 'Activo'),
('TI', '1001234568', 'Valentina', 'Sofía', 'Gómez', 'Gómez', '2014-07-22', 'F', 'Calle 36 # 15-20, Bucaramanga', '3201112234', 'valentina.gomez@student.lh.edu.co', 'A+', 'Polen', NULL, '2024-01-20', 'Activo'),
('TI', '1001234569', 'Mateo', 'José', 'Ramírez', 'Ramírez', '2011-11-08', 'M', 'Carrera 27 # 42-10, Bucaramanga', '3201112235', 'mateo.ramirez@student.lh.edu.co', 'B+', NULL, 'Asma leve', '2023-01-18', 'Activo'),
('TI', '1001234570', 'Isabella', 'María', 'Ramírez', 'Ramírez', '2013-05-30', 'F', 'Carrera 27 # 42-10, Bucaramanga', '3201112236', 'isabella.ramirez@student.lh.edu.co', 'O-', NULL, NULL, '2023-01-18', 'Activo'),
('TI', '1001234571', 'Sebastián', 'Luis', 'Torres', 'Torres', '2010-09-12', 'M', 'Avenida González Valencia # 48-30', '3201112237', 'sebastian.torres@student.lh.edu.co', 'A-', 'Mariscos', NULL, '2022-01-15', 'Activo'),
('TI', '1001234572', 'Camila', 'Andrea', 'Torres', 'Torres', '2015-01-25', 'F', 'Avenida González Valencia # 48-30', '3201112238', 'camila.torres@student.lh.edu.co', 'AB+', NULL, NULL, '2024-01-20', 'Activo'),
('TI', '1001234573', 'Nicolás', 'Fernando', 'Mendoza', 'Mendoza', '2009-04-18', 'M', 'Calle 45 # 28-15, Floridablanca', '3201112239', 'nicolas.mendoza@student.lh.edu.co', 'O+', NULL, NULL, '2021-01-12', 'Activo'),
('TI', '1001234574', 'Mariana', 'Lucía', 'Mendoza', 'Mendoza', '2012-12-03', 'F', 'Calle 45 # 28-15, Floridablanca', '3201112240', 'mariana.mendoza@student.lh.edu.co', 'A+', NULL, NULL, '2024-01-20', 'Activo'),
('TI', '1001234575', 'Samuel', 'David', 'Vargas', 'Vargas', '2011-06-20', 'M', 'Carrera 33 # 52-08, Bucaramanga', '3201112241', 'samuel.vargas@student.lh.edu.co', 'B-', NULL, NULL, '2023-01-18', 'Activo'),
('TI', '1001234576', 'Luciana', 'Isabel', 'Vargas', 'Vargas', '2014-08-14', 'F', 'Carrera 33 # 52-08, Bucaramanga', '3201112242', 'luciana.vargas@student.lh.edu.co', 'O+', 'Lácteos', NULL, '2024-01-20', 'Activo'),
('TI', '1001234577', 'Emiliano', 'José', 'Castro', 'Castro', '2010-02-28', 'M', 'Calle 56 # 21-40, Piedecuesta', '3201112243', 'emiliano.castro@student.lh.edu.co', 'A+', NULL, NULL, '2022-01-15', 'Activo'),
('TI', '1001234578', 'Antonella', 'Sofía', 'Castro', 'Castro', '2013-10-09', 'F', 'Calle 56 # 21-40, Piedecuesta', '3201112244', 'antonella.castro@student.lh.edu.co', 'AB-', NULL, NULL, '2023-01-18', 'Activo'),
('TI', '1001234579', 'Tomás', 'Andrés', 'Díaz', 'Díaz', '2008-07-05', 'M', 'Carrera 15 # 34-22, Bucaramanga', '3201112245', 'tomas.diaz@student.lh.edu.co', 'O+', NULL, NULL, '2020-01-14', 'Activo'),
('TI', '1001234580', 'Salomé', 'María', 'Díaz', 'Díaz', '2011-03-17', 'F', 'Carrera 15 # 34-22, Bucaramanga', '3201112246', 'salome.diaz@student.lh.edu.co', 'A+', NULL, 'Diabetes tipo 1', '2023-01-18', 'Activo'),
('TI', '1001234581', 'Benjamín', 'Luis', 'Ruiz', 'Ruiz', '2012-09-21', 'M', 'Calle 12 # 8-50, Girón', '3201112247', 'benjamin.ruiz@student.lh.edu.co', 'B+', NULL, NULL, '2024-01-20', 'Activo'),
('TI', '1001234582', 'Emma', 'Catalina', 'Ruiz', 'Ruiz', '2015-05-11', 'F', 'Calle 12 # 8-50, Girón', '3201112248', 'emma.ruiz@student.lh.edu.co', 'O-', NULL, NULL, '2024-01-20', 'Activo'),
('TI', '1001234583', 'Martín', 'Felipe', 'Soto', 'Soto', '2009-12-30', 'M', 'Avenida Quebrada Seca # 18-09', '3201112249', 'martin.soto@student.lh.edu.co', 'A+', NULL, NULL, '2021-01-12', 'Activo'),
('TI', '1001234584', 'Julieta', 'Andrea', 'Soto', 'Soto', '2013-04-07', 'F', 'Avenida Quebrada Seca # 18-09', '3201112250', 'julieta.soto@student.lh.edu.co', 'AB+', NULL, NULL, '2023-01-18', 'Activo'),
('TI', '1001234585', 'Joaquín', 'Camilo', 'León', 'León', '2010-08-19', 'M', 'Calle 72 # 30-14, Bucaramanga', '3201112251', 'joaquin.leon@student.lh.edu.co', 'O+', NULL, NULL, '2022-01-15', 'Activo'),
('TI', '1001234586', 'Renata', 'Sofía', 'León', 'León', '2014-11-26', 'F', 'Calle 72 # 30-14, Bucaramanga', '3201112252', 'renata.leon@student.lh.edu.co', 'A-', NULL, NULL, '2024-01-20', 'Activo'),
('TI', '1001234587', 'Thiago', 'José', 'Pardo', 'Pardo', '2011-01-14', 'M', 'Carrera 21 # 45-67, Floridablanca', '3201112253', 'thiago.pardo@student.lh.edu.co', 'B+', NULL, NULL, '2023-01-18', 'Activo'),
('TI', '1001234588', 'Amelia', 'Lucía', 'Pardo', 'Pardo', '2013-06-02', 'F', 'Carrera 21 # 45-67, Floridablanca', '3201112254', 'amelia.pardo@student.lh.edu.co', 'O+', NULL, NULL, '2023-01-18', 'Activo'),
('TI', '1001234589', 'Gael', 'Andrés', 'Nieto', 'Nieto', '2008-10-28', 'M', 'Calle 29 # 16-33, Bucaramanga', '3201112255', 'gael.nieto@student.lh.edu.co', 'A+', NULL, NULL, '2020-01-14', 'Activo'),
('TI', '1001234590', 'Olivia', 'María', 'Nieto', 'Nieto', '2012-02-16', 'F', 'Calle 29 # 16-33, Bucaramanga', '3201112256', 'olivia.nieto@student.lh.edu.co', 'AB-', NULL, NULL, '2024-01-20', 'Activo'),
('TI', '1001234591', 'Dylan', 'Luis', 'Beltrán', 'Beltrán', '2009-05-23', 'M', 'Carrera 35 # 50-12, Piedecuesta', '3201112257', 'dylan.beltran@student.lh.edu.co', 'O+', NULL, NULL, '2021-01-12', 'Activo'),
('TI', '1001234592', 'Victoria', 'Isabel', 'Beltrán', 'Beltrán', '2014-09-04', 'F', 'Carrera 35 # 50-12, Piedecuesta', '3201112258', 'victoria.beltran@student.lh.edu.co', 'A+', NULL, NULL, '2024-01-20', 'Activo'),
('TI', '1001234593', 'Ian', 'Fernando', 'Quiroga', 'Quiroga', '2010-12-11', 'M', 'Calle 48 # 22-05, Bucaramanga', '3201112259', 'ian.quiroga@student.lh.edu.co', 'B-', NULL, NULL, '2022-01-15', 'Activo'),
('TI', '1001234594', 'Regina', 'Sofía', 'Quiroga', 'Quiroga', '2015-03-29', 'F', 'Calle 48 # 22-05, Bucaramanga', '3201112260', 'regina.quiroga@student.lh.edu.co', 'O+', NULL, NULL, '2024-01-20', 'Activo'),
('TI', '1001234595', 'Álvaro', 'José', 'Acosta', 'Acosta', '2011-07-13', 'M', 'Avenida La Rosita # 9-40', '3201112261', 'alvaro.acosta@student.lh.edu.co', 'A+', NULL, NULL, '2023-01-18', 'Activo'),
('TI', '1001234596', 'Paulina', 'Andrea', 'Acosta', 'Acosta', '2013-11-20', 'F', 'Avenida La Rosita # 9-40', '3201112262', 'paulina.acosta@student.lh.edu.co', 'AB+', NULL, NULL, '2023-01-18', 'Activo'),
('TI', '1001234597', 'Bruno', 'Camilo', 'Vega', 'Vega', '2009-08-07', 'M', 'Calle 60 # 14-28, Floridablanca', '3201112263', 'bruno.vega@student.lh.edu.co', 'O-', NULL, NULL, '2021-01-12', 'Activo'),
('TI', '1001234598', 'Elena', 'María', 'Vega', 'Vega', '2012-04-25', 'F', 'Calle 60 # 14-28, Floridablanca', '3201112264', 'elena.vega@student.lh.edu.co', 'A+', NULL, NULL, '2024-01-20', 'Activo'),
('TI', '1001234599', 'Simón', 'Andrés', 'Gómez', 'Ramírez', '2010-06-18', 'M', 'Calle 36 # 15-20, Bucaramanga', '3201112265', 'simon.gomez@student.lh.edu.co', 'B+', NULL, NULL, '2022-01-15', 'Activo'),
('TI', '1001234600', 'Catalina', 'Sofía', 'Torres', 'Mendoza', '2014-01-09', 'F', 'Avenida González Valencia # 48-30', '3201112266', 'catalina.torres@student.lh.edu.co', 'O+', NULL, NULL, '2024-01-20', 'Activo'),
('TI', '1001234601', 'Martín', 'José', 'Castro', 'Díaz', '2008-03-03', 'M', 'Calle 56 # 21-40, Piedecuesta', '3201112267', 'martin.castro@student.lh.edu.co', 'A+', NULL, NULL, '2020-01-14', 'Activo');

-- ============================================
-- STUDENT-GUARDIAN LINKS
-- ============================================
INSERT INTO estudiante_acudiente (id_estudiante, id_acudiente, es_principal, es_contacto_emergencia) VALUES
(1, 1, TRUE, TRUE), (1, 2, FALSE, TRUE),
(2, 1, TRUE, TRUE), (2, 2, FALSE, TRUE),
(3, 3, TRUE, TRUE), (3, 4, FALSE, TRUE),
(4, 3, TRUE, TRUE), (4, 4, FALSE, TRUE),
(5, 5, TRUE, TRUE), (5, 6, FALSE, TRUE),
(6, 5, TRUE, TRUE), (6, 6, FALSE, TRUE),
(7, 7, TRUE, TRUE), (7, 8, FALSE, TRUE),
(8, 7, TRUE, TRUE), (8, 8, FALSE, TRUE),
(9, 9, TRUE, TRUE), (9, 10, FALSE, TRUE),
(10, 9, TRUE, TRUE), (10, 10, FALSE, TRUE),
(11, 11, TRUE, TRUE), (11, 12, FALSE, TRUE),
(12, 11, TRUE, TRUE), (12, 12, FALSE, TRUE),
(13, 13, TRUE, TRUE), (13, 14, FALSE, TRUE),
(14, 13, TRUE, TRUE), (14, 14, FALSE, TRUE),
(15, 15, TRUE, TRUE), (15, 16, FALSE, TRUE),
(16, 15, TRUE, TRUE), (16, 16, FALSE, TRUE),
(17, 17, TRUE, TRUE), (17, 18, FALSE, TRUE),
(18, 17, TRUE, TRUE), (18, 18, FALSE, TRUE),
(19, 19, TRUE, TRUE), (19, 20, FALSE, TRUE),
(20, 19, TRUE, TRUE), (20, 20, FALSE, TRUE),
(21, 21, TRUE, TRUE), (21, 22, FALSE, TRUE),
(22, 21, TRUE, TRUE), (22, 22, FALSE, TRUE),
(23, 23, TRUE, TRUE), (23, 24, FALSE, TRUE),
(24, 23, TRUE, TRUE), (24, 24, FALSE, TRUE),
(25, 25, TRUE, TRUE), (25, 26, FALSE, TRUE),
(26, 25, TRUE, TRUE), (26, 26, FALSE, TRUE),
(27, 27, TRUE, TRUE), (27, 28, FALSE, TRUE),
(28, 27, TRUE, TRUE), (28, 28, FALSE, TRUE),
(29, 29, TRUE, TRUE), (29, 30, FALSE, TRUE),
(30, 29, TRUE, TRUE), (30, 30, FALSE, TRUE),
(31, 31, TRUE, TRUE), (31, 32, FALSE, TRUE),
(32, 31, TRUE, TRUE), (32, 32, FALSE, TRUE),
(33, 1, TRUE, TRUE),
(34, 5, TRUE, TRUE),
(35, 11, TRUE, TRUE);

-- ============================================
-- ENROLLMENTS (matriculas) 2025-2026
-- ============================================
INSERT INTO matriculas (id_estudiante, id_grado, año_academico, estado_matricula, fecha_matricula, observaciones) VALUES
(1, 6, 2025, 'Completada', '2025-01-15', NULL),
(1, 7, 2026, 'Activa', '2026-01-20', NULL),
(2, 4, 2025, 'Completada', '2025-01-15', NULL),
(2, 5, 2026, 'Activa', '2026-01-20', NULL),
(3, 7, 2025, 'Completada', '2025-01-15', NULL),
(3, 8, 2026, 'Activa', '2026-01-20', NULL),
(4, 5, 2025, 'Completada', '2025-01-15', NULL),
(4, 6, 2026, 'Activa', '2026-01-20', NULL),
(5, 8, 2025, 'Completada', '2025-01-15', NULL),
(5, 9, 2026, 'Activa', '2026-01-20', NULL),
(6, 3, 2025, 'Completada', '2025-01-15', NULL),
(6, 4, 2026, 'Activa', '2026-01-20', NULL),
(7, 9, 2025, 'Completada', '2025-01-15', NULL),
(7, 10, 2026, 'Activa', '2026-01-20', NULL),
(8, 6, 2025, 'Completada', '2025-01-15', NULL),
(8, 7, 2026, 'Activa', '2026-01-20', NULL),
(9, 7, 2025, 'Completada', '2025-01-15', NULL),
(9, 8, 2026, 'Activa', '2026-01-20', NULL),
(10, 4, 2025, 'Completada', '2025-01-15', NULL),
(10, 5, 2026, 'Activa', '2026-01-20', NULL),
(11, 8, 2025, 'Completada', '2025-01-15', NULL),
(11, 9, 2026, 'Activa', '2026-01-20', NULL),
(12, 5, 2025, 'Completada', '2025-01-15', NULL),
(12, 6, 2026, 'Activa', '2026-01-20', NULL),
(13, 10, 2025, 'Completada', '2025-01-15', NULL),
(13, 11, 2026, 'Activa', '2026-01-20', NULL),
(14, 7, 2025, 'Completada', '2025-01-15', NULL),
(14, 8, 2026, 'Activa', '2026-01-20', NULL),
(15, 6, 2025, 'Completada', '2025-01-15', NULL),
(15, 7, 2026, 'Activa', '2026-01-20', NULL),
(16, 3, 2025, 'Completada', '2025-01-15', NULL),
(16, 4, 2026, 'Activa', '2026-01-20', NULL),
(17, 9, 2025, 'Completada', '2025-01-15', NULL),
(17, 10, 2026, 'Activa', '2026-01-20', NULL),
(18, 5, 2025, 'Completada', '2025-01-15', NULL),
(18, 6, 2026, 'Activa', '2026-01-20', NULL),
(19, 8, 2025, 'Completada', '2025-01-15', NULL),
(19, 9, 2026, 'Activa', '2026-01-20', NULL),
(20, 4, 2025, 'Completada', '2025-01-15', NULL),
(20, 5, 2026, 'Activa', '2026-01-20', NULL),
(21, 7, 2025, 'Completada', '2025-01-15', NULL),
(21, 8, 2026, 'Activa', '2026-01-20', NULL),
(22, 5, 2025, 'Completada', '2025-01-15', NULL),
(22, 6, 2026, 'Activa', '2026-01-20', NULL),
(23, 10, 2025, 'Completada', '2025-01-15', NULL),
(23, 11, 2026, 'Activa', '2026-01-20', NULL),
(24, 6, 2025, 'Completada', '2025-01-15', NULL),
(24, 7, 2026, 'Activa', '2026-01-20', NULL),
(25, 9, 2025, 'Completada', '2025-01-15', NULL),
(25, 10, 2026, 'Activa', '2026-01-20', NULL),
(26, 4, 2025, 'Completada', '2025-01-15', NULL),
(26, 5, 2026, 'Activa', '2026-01-20', NULL),
(27, 8, 2025, 'Completada', '2025-01-15', NULL),
(27, 9, 2026, 'Activa', '2026-01-20', NULL),
(28, 3, 2025, 'Completada', '2025-01-15', NULL),
(28, 4, 2026, 'Activa', '2026-01-20', NULL),
(29, 7, 2025, 'Completada', '2025-01-15', NULL),
(29, 8, 2026, 'Activa', '2026-01-20', NULL),
(30, 5, 2025, 'Completada', '2025-01-15', NULL),
(30, 6, 2026, 'Activa', '2026-01-20', NULL),
(31, 9, 2025, 'Completada', '2025-01-15', NULL),
(31, 10, 2026, 'Activa', '2026-01-20', NULL),
(32, 6, 2025, 'Completada', '2025-01-15', NULL),
(32, 7, 2026, 'Activa', '2026-01-20', NULL),
(33, 8, 2025, 'Completada', '2025-01-15', NULL),
(33, 9, 2026, 'Activa', '2026-01-20', NULL),
(34, 4, 2025, 'Completada', '2025-01-15', NULL),
(34, 5, 2026, 'Activa', '2026-01-20', NULL),
(35, 10, 2025, 'Completada', '2025-01-15', NULL),
(35, 11, 2026, 'Activa', '2026-01-20', NULL);

-- ============================================
-- SUBJECTS (36) – one or more per grade
-- ============================================
INSERT INTO materias (codigo_materia, nombre_materia, descripcion, id_grado, horas_semanales, esta_activa) VALUES
('MAT-0', 'Matemáticas Preescolar', 'Nociones numéricas básicas', 1, 4, TRUE),
('ESP-0', 'Lenguaje Preescolar', 'Comunicación oral y escrita inicial', 1, 4, TRUE),
('ART-0', 'Expresión Artística', 'Dibujo y manualidades', 1, 2, TRUE),
('EFI-0', 'Motricidad', 'Desarrollo motor grueso y fino', 1, 2, TRUE),
('MAT-1', 'Matemáticas Primero', 'Números y operaciones básicas', 2, 5, TRUE),
('ESP-1', 'Español Primero', 'Lectura y escritura', 2, 5, TRUE),
('CIE-1', 'Ciencias Naturales Primero', 'Exploración del entorno', 2, 3, TRUE),
('SOC-1', 'Ciencias Sociales Primero', 'Familia y comunidad', 2, 3, TRUE),
('MAT-3', 'Matemáticas Tercero', 'Fracciones y geometría básica', 4, 5, TRUE),
('ESP-3', 'Español Tercero', 'Comprensión lectora', 4, 5, TRUE),
('ING-3', 'Inglés Tercero', 'Vocabulario y estructuras simples', 4, 3, TRUE),
('CIE-3', 'Ciencias Naturales Tercero', 'Seres vivos', 4, 3, TRUE),
('MAT-5', 'Matemáticas Quinto', 'Área, perímetro y proporciones', 6, 5, TRUE),
('ESP-5', 'Español Quinto', 'Redacción y ortografía', 6, 5, TRUE),
('ING-5', 'Inglés Quinto', 'Conversación básica', 6, 3, TRUE),
('CIE-5', 'Ciencias Naturales Quinto', 'Sistemas del cuerpo humano', 6, 3, TRUE),
('SOC-5', 'Ciencias Sociales Quinto', 'Historia de Colombia', 6, 3, TRUE),
('MAT-7', 'Matemáticas Séptimo', 'Álgebra introductoria', 8, 5, TRUE),
('ESP-7', 'Español Séptimo', 'Literatura y análisis textual', 8, 5, TRUE),
('ING-7', 'Inglés Séptimo', 'Gramática intermedia', 8, 4, TRUE),
('FIS-7', 'Física Séptimo', 'Cinemática básica', 8, 3, TRUE),
('QUI-7', 'Química Séptimo', 'Estructura de la materia', 8, 3, TRUE),
('BIO-7', 'Biología Séptimo', 'Célula y tejidos', 8, 3, TRUE),
('MAT-9', 'Matemáticas Noveno', 'Funciones y trigonometría', 10, 5, TRUE),
('ESP-9', 'Español Noveno', 'Literatura latinoamericana', 10, 4, TRUE),
('ING-9', 'Inglés Noveno', 'Preparación para exámenes', 10, 4, TRUE),
('FIS-9', 'Física Noveno', 'Dinámica y energía', 10, 4, TRUE),
('QUI-9', 'Química Noveno', 'Reacciones químicas', 10, 4, TRUE),
('SOC-9', 'Ciencias Sociales Noveno', 'Economía y política', 10, 3, TRUE),
('MAT-11', 'Matemáticas Once', 'Cálculo diferencial', 12, 5, TRUE),
('ESP-11', 'Español Once', 'Ensayo y argumentación', 12, 4, TRUE),
('ING-11', 'Inglés Once', 'Inglés avanzado', 12, 4, TRUE),
('FIS-11', 'Física Once', 'Electromagnetismo', 12, 4, TRUE),
('QUI-11', 'Química Once', 'Química orgánica', 12, 4, TRUE),
('FIL-11', 'Filosofía Once', 'Ética y pensamiento crítico', 12, 3, TRUE),
('INF-7', 'Informática Séptimo', 'Ofimática y programación básica', 8, 2, TRUE);

-- ============================================
-- TEACHER ASSIGNMENTS + SCHEDULE SLOTS (sample)
-- ============================================
INSERT INTO asignaciones_profesor (id_profesor, id_materia, año_academico, seccion) VALUES
(1, 13, 2026, 'A'), (1, 18, 2026, 'A'), (1, 24, 2026, 'A'),
(2, 14, 2026, 'A'), (2, 19, 2026, 'A'), (2, 25, 2026, 'A'),
(3, 16, 2026, 'A'), (3, 23, 2026, 'A'),
(4, 15, 2026, 'A'), (4, 20, 2026, 'A'), (4, 26, 2026, 'A'),
(5, 4, 2026, 'A'),
(6, 17, 2026, 'A'), (6, 29, 2026, 'A'),
(7, 21, 2026, 'A'), (7, 27, 2026, 'A'),
(8, 22, 2026, 'A'), (8, 28, 2026, 'A'),
(9, 23, 2026, 'B'),
(10, 3, 2026, 'A'),
(12, 36, 2026, 'A'),
(13, 35, 2026, 'A'),
(17, 30, 2026, 'A'),
(18, 31, 2026, 'A'),
(19, 32, 2026, 'A');

INSERT INTO horarios_asignacion (id_asignacion, dia_semana, hora_inicio, hora_fin) VALUES
(1, 'Lunes', '07:00:00', '08:00:00'),
(1, 'Miércoles', '07:00:00', '08:00:00'),
(2, 'Martes', '08:00:00', '09:00:00'),
(2, 'Jueves', '08:00:00', '09:00:00'),
(3, 'Lunes', '09:00:00', '10:00:00'),
(4, 'Martes', '07:00:00', '08:00:00'),
(5, 'Miércoles', '08:00:00', '09:00:00'),
(6, 'Viernes', '07:00:00', '08:00:00'),
(7, 'Lunes', '10:00:00', '11:00:00'),
(8, 'Jueves', '09:00:00', '10:00:00');

-- ============================================
-- SUBJECT ENROLLMENTS (sample for period I 2026)
-- ============================================
INSERT INTO inscripciones_materia (id_estudiante, id_materia, año_academico, periodo_academico, seccion, fecha_inscripcion, estado) VALUES
(1, 18, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(1, 19, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(1, 20, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(3, 18, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(3, 21, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(5, 24, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(5, 25, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(7, 24, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(7, 27, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(9, 18, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(11, 24, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(13, 30, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(13, 31, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(13, 32, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(15, 18, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(17, 24, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(19, 24, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(21, 18, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(23, 30, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(25, 24, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(27, 24, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(29, 18, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(31, 24, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(33, 24, 2026, 'I', 'A', '2026-01-25', 'Activa'),
(35, 30, 2026, 'I', 'A', '2026-01-25', 'Activa');

-- ============================================
-- ACADEMIC RECORDS (≥40)
-- ============================================
INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, observaciones, fecha_registro) VALUES
(1, 18, 1, 2026, 'I', 4.2, 'Buen desempeño', '2026-03-15'),
(1, 19, 2, 2026, 'I', 3.8, NULL, '2026-03-15'),
(1, 20, 4, 2026, 'I', 4.5, 'Excelente participación', '2026-03-15'),
(3, 18, 1, 2026, 'I', 3.5, NULL, '2026-03-15'),
(3, 21, 7, 2026, 'I', 4.0, NULL, '2026-03-15'),
(5, 24, 1, 2026, 'I', 4.3, NULL, '2026-03-15'),
(5, 25, 2, 2026, 'I', 3.9, NULL, '2026-03-15'),
(7, 24, 1, 2026, 'I', 2.8, 'Requiere refuerzo', '2026-03-15'),
(7, 27, 7, 2026, 'I', 3.6, NULL, '2026-03-15'),
(9, 18, 1, 2026, 'I', 4.7, 'Destacado', '2026-03-15'),
(11, 24, 1, 2026, 'I', 3.2, NULL, '2026-03-15'),
(13, 30, 17, 2026, 'I', 4.1, NULL, '2026-03-15'),
(13, 31, 18, 2026, 'I', 4.4, NULL, '2026-03-15'),
(13, 32, 19, 2026, 'I', 3.7, NULL, '2026-03-15'),
(15, 18, 1, 2026, 'I', 3.9, NULL, '2026-03-15'),
(17, 24, 1, 2026, 'I', 4.0, NULL, '2026-03-15'),
(19, 24, 1, 2026, 'I', 3.4, NULL, '2026-03-15'),
(21, 18, 1, 2026, 'I', 4.6, NULL, '2026-03-15'),
(23, 30, 17, 2026, 'I', 3.8, NULL, '2026-03-15'),
(25, 24, 1, 2026, 'I', 2.5, 'En riesgo académico', '2026-03-15'),
(27, 24, 1, 2026, 'I', 4.2, NULL, '2026-03-15'),
(29, 18, 1, 2026, 'I', 3.6, NULL, '2026-03-15'),
(31, 24, 1, 2026, 'I', 4.8, 'Excelente', '2026-03-15'),
(33, 24, 1, 2026, 'I', 3.3, NULL, '2026-03-15'),
(35, 30, 17, 2026, 'I', 4.0, NULL, '2026-03-15'),
-- Period II samples
(1, 18, 1, 2026, 'II', 4.0, NULL, '2026-06-15'),
(1, 19, 2, 2026, 'II', 4.1, NULL, '2026-06-15'),
(3, 18, 1, 2026, 'II', 3.7, NULL, '2026-06-15'),
(5, 24, 1, 2026, 'II', 4.5, NULL, '2026-06-15'),
(7, 24, 1, 2026, 'II', 3.0, 'Mejora leve', '2026-06-15'),
(9, 18, 1, 2026, 'II', 4.5, NULL, '2026-06-15'),
(13, 30, 17, 2026, 'II', 4.2, NULL, '2026-06-15'),
(13, 31, 18, 2026, 'II', 4.0, NULL, '2026-06-15'),
(21, 18, 1, 2026, 'II', 4.3, NULL, '2026-06-15'),
(25, 24, 1, 2026, 'II', 2.9, NULL, '2026-06-15'),
(31, 24, 1, 2026, 'II', 4.6, NULL, '2026-06-15'),
-- Final samples
(1, 18, 1, 2026, 'Final', 4.1, 'Promedio calculado', '2026-11-01'),
(5, 24, 1, 2026, 'Final', 4.4, NULL, '2026-11-01'),
(9, 18, 1, 2026, 'Final', 4.6, NULL, '2026-11-01'),
(13, 30, 17, 2026, 'Final', 4.15, NULL, '2026-11-01'),
(31, 24, 1, 2026, 'Final', 4.7, NULL, '2026-11-01');

-- ============================================
-- ATTENDANCE (sample)
-- ============================================
INSERT INTO asistencias (id_estudiante, id_materia, fecha_asistencia, estado_asistencia, observaciones, registrado_por) VALUES
(1, 18, '2026-02-03', 'Presente', NULL, 1),
(1, 18, '2026-02-05', 'Presente', NULL, 1),
(1, 18, '2026-02-10', 'Ausente', 'Sin justificación', 1),
(1, 18, '2026-02-12', 'Tardanza', NULL, 1),
(3, 18, '2026-02-03', 'Presente', NULL, 1),
(3, 18, '2026-02-05', 'Ausente', NULL, 1),
(3, 18, '2026-02-10', 'Ausente', NULL, 1),
(3, 18, '2026-02-12', 'Ausente', 'Falta reiterada', 1),
(5, 24, '2026-02-04', 'Presente', NULL, 1),
(7, 24, '2026-02-04', 'Excusa Médica', 'Cita médica', 1),
(9, 18, '2026-02-03', 'Presente', NULL, 1),
(13, 30, '2026-02-06', 'Presente', NULL, 17),
(25, 24, '2026-02-04', 'Ausente', NULL, 1),
(25, 24, '2026-02-06', 'Ausente', NULL, 1),
(25, 24, '2026-02-11', 'Ausente', NULL, 1),
(25, 24, '2026-02-13', 'Ausente', 'Más de 3 ausencias en la semana', 1);

-- ============================================
-- CLUBS (30)
-- ============================================
INSERT INTO clubes (nombre_club, descripcion, id_profesor_supervisor, capacidad_maxima, horario_reunion, esta_activo) VALUES
('Club de Matemáticas', 'Olimpiadas y resolución de problemas', 1, 25, 'Martes 15:00-16:30', TRUE),
('Club de Lectura', 'Lectura crítica y club de libros', 2, 20, 'Miércoles 15:00-16:00', TRUE),
('Club de Ciencias', 'Experimentos y feria científica', 3, 20, 'Jueves 15:00-16:30', TRUE),
('Club de Inglés', 'Conversación y debate en inglés', 4, 25, 'Lunes 15:00-16:00', TRUE),
('Club de Fútbol', 'Entrenamiento y partidos intercolegiados', 5, 30, 'Lunes y Miércoles 16:00-17:30', TRUE),
('Club de Historia', 'Investigación histórica local', 6, 15, 'Viernes 15:00-16:00', TRUE),
('Club de Física', 'Proyectos de física experimental', 7, 15, 'Martes 15:00-16:30', TRUE),
('Club de Química', 'Laboratorio y proyectos verdes', 8, 15, 'Jueves 15:00-16:30', TRUE),
('Club de Biología', 'Ecología y medio ambiente', 9, 20, 'Miércoles 15:00-16:00', TRUE),
('Club de Artes', 'Pintura, dibujo y exposiciones', 10, 20, 'Viernes 15:00-17:00', TRUE),
('Club de Música', 'Coro y banda escolar', 11, 30, 'Lunes y Jueves 15:00-16:30', TRUE),
('Club de Programación', 'Scratch, Python y robots', 12, 20, 'Martes 15:00-16:30', TRUE),
('Club de Debate', 'Oratoria y argumentación', 13, 15, 'Miércoles 15:00-16:30', TRUE),
('Club de Ética', 'Proyectos de servicio social', 14, 20, 'Viernes 15:00-16:00', TRUE),
('Club de Geografía', 'Cartografía y viajes virtuales', 15, 15, 'Jueves 15:00-16:00', TRUE),
('Club de Ajedrez', 'Torneos y entrenamiento', 1, 20, 'Lunes 15:00-16:00', TRUE),
('Club de Teatro', 'Montaje de obras escolares', 10, 25, 'Martes 15:00-17:00', TRUE),
('Club de Baloncesto', 'Entrenamiento y ligas', 5, 20, 'Martes y Jueves 16:00-17:30', TRUE),
('Club de Voleibol', 'Técnica y competencias', 5, 18, 'Miércoles 16:00-17:30', TRUE),
('Club de Periodismo', 'Periódico escolar digital', 2, 15, 'Viernes 15:00-16:30', TRUE),
('Club de Robótica', 'Competencias de robots', 12, 16, 'Jueves 15:00-17:00', TRUE),
('Club de Fotografía', 'Técnica fotográfica y edición', 10, 12, 'Miércoles 15:00-16:30', TRUE),
('Club de Medio Ambiente', 'Huerta escolar y reciclaje', 9, 25, 'Lunes 15:00-16:00', TRUE),
('Club de Danza', 'Danzas folclóricas y modernas', 10, 20, 'Jueves 15:00-17:00', TRUE),
('Club de Emprendimiento', 'Ideas de negocio y ferias', 6, 20, 'Martes 15:00-16:00', TRUE),
('Club de Astronomía', 'Observación y planetario', 7, 15, 'Viernes 16:00-17:30', TRUE),
('Club de Cocina Saludable', 'Hábitos alimenticios', 14, 15, 'Miércoles 15:00-16:30', TRUE),
('Club de Liderazgo', 'Habilidades blandas y liderazgo', 13, 20, 'Lunes 15:00-16:00', TRUE),
('Club de Idiomas', 'Francés e italiano básico', 4, 15, 'Viernes 15:00-16:00', TRUE),
('Club de Voluntariado', 'Apoyo a comunidades cercanas', 14, 30, 'Sábados 09:00-12:00', TRUE);

-- Club memberships (sample)
INSERT INTO membresias_club (id_estudiante, id_club, fecha_ingreso, estado_membresia) VALUES
(1, 1, '2026-02-01', 'Activa'),
(1, 16, '2026-02-01', 'Activa'),
(3, 1, '2026-02-01', 'Activa'),
(5, 5, '2026-02-01', 'Activa'),
(7, 12, '2026-02-01', 'Activa'),
(9, 1, '2026-02-01', 'Activa'),
(11, 5, '2026-02-01', 'Activa'),
(13, 12, '2026-02-01', 'Activa'),
(15, 2, '2026-02-01', 'Activa'),
(17, 7, '2026-02-01', 'Activa'),
(19, 5, '2026-02-01', 'Activa'),
(21, 1, '2026-02-01', 'Activa'),
(23, 13, '2026-02-01', 'Activa'),
(25, 5, '2026-02-01', 'Activa'),
(27, 11, '2026-02-01', 'Activa'),
(29, 1, '2026-02-01', 'Activa'),
(31, 16, '2026-02-01', 'Activa'),
(33, 5, '2026-02-01', 'Activa'),
(2, 2, '2026-02-01', 'Activa'),
(4, 10, '2026-02-01', 'Activa'),
(6, 4, '2026-02-01', 'Activa'),
(8, 10, '2026-02-01', 'Activa'),
(10, 2, '2026-02-01', 'Activa'),
(12, 9, '2026-02-01', 'Activa'),
(14, 3, '2026-02-01', 'Activa');

-- ============================================
-- EVENTS + PARTICIPANTS
-- ============================================
INSERT INTO eventos (nombre_evento, descripcion, tipo_evento, fecha_evento, lugar, tiene_competencia, id_organizador, esta_activo) VALUES
('Feria de la Ciencia 2026', 'Exposición de proyectos científicos', 'Científico', '2026-05-15', 'Auditorio principal', TRUE, 3, TRUE),
('Día del Idioma', 'Celebración de la lengua castellana', 'Cultural', '2026-04-23', 'Patio central', FALSE, 2, TRUE),
('Olimpiadas Matemáticas Internas', 'Competencia de resolución de problemas', 'Académico', '2026-06-10', 'Salones de matemáticas', TRUE, 1, TRUE),
('Torneo Intercolegiado de Fútbol', 'Fase zonal de fútbol', 'Deportivo', '2026-09-20', 'Cancha principal', TRUE, 5, TRUE),
('Festival de Talentos', 'Música, danza y teatro', 'Cultural', '2026-10-05', 'Auditorio', FALSE, 11, TRUE),
('Jornada de Servicio Social', 'Apoyo a hogar de ancianos', 'Social', '2026-08-12', 'Hogar San José', FALSE, 14, TRUE),
('Concurso de Ortografía', 'Eliminatorias de ortografía', 'Académico', '2026-03-18', 'Biblioteca', TRUE, 2, TRUE),
('Semana de la Ciencia', 'Talleres y conferencias', 'Científico', '2026-05-12', 'Laboratorios', FALSE, 8, TRUE);

INSERT INTO participantes_evento (id_evento, id_estudiante, rol_participacion, posicion_obtenida) VALUES
(1, 1, 'Competidor', '2do Lugar'),
(1, 3, 'Competidor', 'Participante'),
(1, 9, 'Competidor', '1er Lugar'),
(1, 13, 'Competidor', '3er Lugar'),
(3, 1, 'Competidor', '1er Lugar'),
(3, 9, 'Competidor', '2do Lugar'),
(3, 21, 'Competidor', '3er Lugar'),
(3, 31, 'Competidor', 'Participante'),
(4, 5, 'Competidor', 'Participante'),
(4, 11, 'Competidor', 'Participante'),
(4, 19, 'Competidor', 'Participante'),
(4, 25, 'Competidor', 'Participante'),
(2, 2, 'Participante', NULL),
(2, 4, 'Participante', NULL),
(2, 8, 'Participante', NULL),
(5, 14, 'Participante', NULL),
(5, 27, 'Participante', NULL),
(7, 1, 'Competidor', '1er Lugar'),
(7, 15, 'Competidor', '2do Lugar'),
(7, 21, 'Competidor', '3er Lugar');

-- ============================================
-- PAYMENTS (≥35)
-- ============================================
INSERT INTO pagos (id_estudiante, id_acudiente, id_concepto, monto_pagado, fecha_pago, metodo_pago, numero_recibo, año_academico, periodo_academico, notas) VALUES
(1, 1, 1, 500000.00, '2026-01-18', 'Transferencia', 'REC-2026-0001', 2026, 'Anual', 'Matrícula 2026'),
(1, 1, 2, 315000.00, '2026-02-05', 'Transferencia', 'REC-2026-0002', 2026, 'I', 'Pensión Feb - descuento hermanos 10%'),
(1, 1, 2, 315000.00, '2026-03-05', 'Transferencia', 'REC-2026-0003', 2026, 'I', 'Pensión Mar - descuento hermanos'),
(2, 1, 1, 500000.00, '2026-01-18', 'Transferencia', 'REC-2026-0004', 2026, 'Anual', NULL),
(2, 1, 2, 315000.00, '2026-02-05', 'Transferencia', 'REC-2026-0005', 2026, 'I', 'Descuento hermanos'),
(3, 3, 1, 500000.00, '2026-01-19', 'Tarjeta Débito', 'REC-2026-0006', 2026, 'Anual', NULL),
(3, 3, 2, 350000.00, '2026-02-08', 'Tarjeta Débito', 'REC-2026-0007', 2026, 'I', NULL),
(3, 3, 2, 350000.00, '2026-03-08', 'Tarjeta Débito', 'REC-2026-0008', 2026, 'I', NULL),
(4, 3, 1, 500000.00, '2026-01-19', 'Tarjeta Débito', 'REC-2026-0009', 2026, 'Anual', NULL),
(5, 5, 1, 500000.00, '2026-01-20', 'Efectivo', 'REC-2026-0010', 2026, 'Anual', NULL),
(5, 5, 2, 350000.00, '2026-02-10', 'Efectivo', 'REC-2026-0011', 2026, 'I', NULL),
(5, 5, 3, 250000.00, '2026-01-25', 'Efectivo', 'REC-2026-0012', 2026, 'Anual', 'Uniforme completo'),
(6, 5, 1, 500000.00, '2026-01-20', 'Efectivo', 'REC-2026-0013', 2026, 'Anual', NULL),
(7, 7, 1, 450000.00, '2026-01-21', 'Transferencia', 'REC-2026-0014', 2026, 'Anual', 'Beca 10%'),
(7, 7, 2, 315000.00, '2026-02-12', 'Transferencia', 'REC-2026-0015', 2026, 'I', 'Beca parcial'),
(8, 7, 1, 500000.00, '2026-01-21', 'Transferencia', 'REC-2026-0016', 2026, 'Anual', NULL),
(9, 9, 1, 500000.00, '2026-01-22', 'Tarjeta Crédito', 'REC-2026-0017', 2026, 'Anual', NULL),
(9, 9, 2, 350000.00, '2026-02-15', 'Tarjeta Crédito', 'REC-2026-0018', 2026, 'I', NULL),
(10, 9, 1, 500000.00, '2026-01-22', 'Tarjeta Crédito', 'REC-2026-0019', 2026, 'Anual', NULL),
(11, 11, 1, 500000.00, '2026-01-23', 'Transferencia', 'REC-2026-0020', 2026, 'Anual', NULL),
(11, 11, 2, 350000.00, '2026-02-18', 'Transferencia', 'REC-2026-0021', 2026, 'I', NULL),
(12, 11, 1, 500000.00, '2026-01-23', 'Transferencia', 'REC-2026-0022', 2026, 'Anual', NULL),
(13, 13, 1, 500000.00, '2026-01-24', 'Transferencia', 'REC-2026-0023', 2026, 'Anual', NULL),
(13, 13, 2, 350000.00, '2026-02-20', 'Transferencia', 'REC-2026-0024', 2026, 'I', NULL),
(14, 13, 1, 500000.00, '2026-01-24', 'Transferencia', 'REC-2026-0025', 2026, 'Anual', NULL),
(15, 15, 1, 500000.00, '2026-01-25', 'Efectivo', 'REC-2026-0026', 2026, 'Anual', NULL),
(15, 15, 2, 350000.00, '2026-02-22', 'Efectivo', 'REC-2026-0027', 2026, 'I', NULL),
(17, 17, 1, 500000.00, '2026-01-26', 'Transferencia', 'REC-2026-0028', 2026, 'Anual', NULL),
(17, 17, 2, 350000.00, '2026-02-25', 'Transferencia', 'REC-2026-0029', 2026, 'I', NULL),
(19, 19, 1, 500000.00, '2026-01-27', 'Tarjeta Débito', 'REC-2026-0030', 2026, 'Anual', NULL),
(19, 19, 2, 350000.00, '2026-02-28', 'Tarjeta Débito', 'REC-2026-0031', 2026, 'I', NULL),
(21, 21, 1, 500000.00, '2026-01-28', 'Transferencia', 'REC-2026-0032', 2026, 'Anual', NULL),
(23, 23, 1, 500000.00, '2026-01-29', 'Transferencia', 'REC-2026-0033', 2026, 'Anual', NULL),
(25, 25, 1, 500000.00, '2026-01-30', 'Efectivo', 'REC-2026-0034', 2026, 'Anual', NULL),
(27, 27, 1, 500000.00, '2026-01-31', 'Transferencia', 'REC-2026-0035', 2026, 'Anual', NULL),
(29, 29, 1, 500000.00, '2026-02-01', 'Transferencia', 'REC-2026-0036', 2026, 'Anual', NULL),
(31, 31, 1, 500000.00, '2026-02-02', 'Tarjeta Crédito', 'REC-2026-0037', 2026, 'Anual', NULL),
(33, 1, 1, 500000.00, '2026-02-03', 'Transferencia', 'REC-2026-0038', 2026, 'Anual', NULL),
(35, 11, 1, 500000.00, '2026-02-04', 'Transferencia', 'REC-2026-0039', 2026, 'Anual', NULL),
-- Some students without recent pension (potential morosos)
(7, 7, 4, 150000.00, '2026-02-12', 'Transferencia', 'REC-2026-0040', 2026, 'I', 'Transporte');

-- Sample invoices linked to first payments
INSERT INTO facturas (numero_factura, id_pago, fecha_emision, cufe, nombre_archivo_pdf, ruta_archivo) VALUES
('LH-FE-000001', 1, '2026-01-18', 'CUFE-SIM-LH-FE-000001-900123456-7-20260118', 'factura_000001.pdf', 'invoices/factura_000001.pdf'),
('LH-FE-000002', 2, '2026-02-05', 'CUFE-SIM-LH-FE-000002-900123456-7-20260205', 'factura_000002.pdf', 'invoices/factura_000002.pdf'),
('LH-FE-000003', 6, '2026-01-19', 'CUFE-SIM-LH-FE-000003-900123456-7-20260119', 'factura_000003.pdf', 'invoices/factura_000003.pdf'),
('LH-FE-000004', 10, '2026-01-20', 'CUFE-SIM-LH-FE-000004-900123456-7-20260120', 'factura_000004.pdf', 'invoices/factura_000004.pdf'),
('LH-FE-000005', 14, '2026-01-21', 'CUFE-SIM-LH-FE-000005-900123456-7-20260121', 'factura_000005.pdf', 'invoices/factura_000005.pdf');

-- ============================================
-- RESOURCES (32)
-- ============================================
INSERT INTO recursos (codigo_recurso, nombre_recurso, id_categoria, descripcion, fecha_adquisicion, ubicacion_fisica, estado_recurso) VALUES
('LIB-001', 'Cien años de soledad', 1, 'Edición escolar Gabriel García Márquez', '2020-03-10', 'Biblioteca', 'Disponible'),
('LIB-002', 'El principito', 1, 'Antoine de Saint-Exupéry', '2019-05-15', 'Biblioteca', 'Disponible'),
('LIB-003', 'Don Quijote de la Mancha (adaptado)', 1, 'Edición juvenil', '2021-02-20', 'Biblioteca', 'Disponible'),
('LIB-004', 'Matemáticas 7 - Editorial Norma', 1, 'Texto guía séptimo', '2024-01-10', 'Biblioteca', 'Disponible'),
('LIB-005', 'Biología Molecular - Lodish', 1, 'Referencia avanzada', '2022-08-12', 'Biblioteca', 'Prestado'),
('LIB-006', 'Historia de Colombia', 1, 'Texto de sociales', '2023-01-15', 'Biblioteca', 'Disponible'),
('LIB-007', 'Inglés Interactivo 9', 1, 'Libro de trabajo', '2024-01-10', 'Biblioteca', 'Disponible'),
('LIB-008', 'Física Conceptual - Hewitt', 1, 'Referencia física', '2021-06-01', 'Biblioteca', 'Disponible'),
('LAB-001', 'Microscopio óptico binocular', 2, 'Marca Olympus, 40x-1000x', '2019-09-20', 'Laboratorio 1', 'Disponible'),
('LAB-002', 'Balanza analítica digital', 2, 'Precisión 0.0001 g', '2020-11-05', 'Laboratorio 2', 'Disponible'),
('LAB-003', 'Kit de química orgánica', 2, 'Vidriería y reactivos básicos', '2022-03-18', 'Laboratorio 2', 'Disponible'),
('LAB-004', 'Esqueleto humano articulado', 2, 'Modelo anatómico', '2018-04-22', 'Laboratorio 1', 'Disponible'),
('LAB-005', 'Osciloscopio digital', 2, 'Para experimentos de física', '2021-10-30', 'Laboratorio 3', 'En Mantenimiento'),
('DEP-001', 'Balón de fútbol N5', 3, 'Cuero sintético', '2023-02-14', 'Depósito deportivo', 'Disponible'),
('DEP-002', 'Balón de baloncesto', 3, 'Talla 7', '2023-02-14', 'Depósito deportivo', 'Disponible'),
('DEP-003', 'Red de voleibol', 3, 'Oficial con postes', '2022-05-08', 'Cancha múltiple', 'Disponible'),
('DEP-004', 'Conos de entrenamiento (set 20)', 3, 'Plástico flexible', '2024-01-20', 'Depósito deportivo', 'Disponible'),
('DEP-005', 'Colchonetas (set 10)', 3, 'Para gimnasia', '2021-03-12', 'Gimnasio', 'Disponible'),
('TEC-001', 'Tablet Samsung Galaxy Tab A8', 4, 'Para uso en aula', '2023-08-01', 'Sala de sistemas', 'Disponible'),
('TEC-002', 'Tablet Samsung Galaxy Tab A8', 4, 'Para uso en aula', '2023-08-01', 'Sala de sistemas', 'Prestado'),
('TEC-003', 'Calculadora científica Casio fx-991', 4, 'Modelo ES Plus', '2022-01-15', 'Sala de sistemas', 'Disponible'),
('TEC-004', 'Portátil Dell Latitude 3420', 4, 'Para docentes', '2023-06-10', 'Sala de sistemas', 'Disponible'),
('TEC-005', 'Proyector Epson PowerLite', 4, 'Sala audiovisual', '2020-09-05', 'Auditorio', 'Disponible'),
('DID-001', 'Mapa físico de Colombia (grande)', 5, 'Laminado', '2019-01-20', 'Aula de sociales', 'Disponible'),
('DID-002', 'Globo terráqueo iluminado', 5, 'Diámetro 30 cm', '2020-02-28', 'Aula de geografía', 'Disponible'),
('DID-003', 'Modelo de sistema solar', 5, 'Móvil de planetas', '2021-04-15', 'Aula de ciencias', 'Disponible'),
('DID-004', 'Ábaco de madera', 5, 'Para primaria', '2018-08-10', 'Aula preescolar', 'Disponible'),
('DID-005', 'Juego de bloques geométricos', 5, 'Material Montessori', '2022-07-22', 'Aula preescolar', 'Disponible'),
('LIB-009', 'Química General - Chang', 1, 'Texto universitario adaptado', '2023-03-01', 'Biblioteca', 'Disponible'),
('LIB-010', 'Cálculo de una variable - Stewart', 1, 'Referencia once', '2022-11-18', 'Biblioteca', 'Disponible'),
('LAB-006', 'Cronómetro digital (set 5)', 2, 'Para física', '2024-02-01', 'Laboratorio 3', 'Disponible'),
('TEC-006', 'Cámara digital Canon', 4, 'Para club de fotografía', '2023-05-20', 'Sala de artes', 'Disponible');

-- Sample loans
INSERT INTO prestamos_estudiante (id_recurso, id_estudiante, fecha_prestamo, fecha_devolucion_esperada, fecha_devolucion_real, estado_prestamo, observaciones) VALUES
(5, 13, '2026-02-10', '2026-03-10', NULL, 'Activo', 'Para proyecto de biología'),
(20, 7, '2026-01-15', '2026-02-15', NULL, 'Vencido', 'Tablet no devuelta a tiempo'),
(1, 15, '2026-02-20', '2026-03-20', NULL, 'Activo', NULL),
(9, 3, '2026-02-05', '2026-02-20', '2026-02-18', 'Devuelto', NULL);

INSERT INTO prestamos_profesor (id_recurso, id_profesor, fecha_prestamo, fecha_devolucion_esperada, fecha_devolucion_real, estado_prestamo, observaciones) VALUES
(22, 12, '2026-01-10', '2026-06-30', NULL, 'Activo', 'Uso en clases de informática'),
(23, 3, '2026-02-01', '2026-02-28', NULL, 'Activo', 'Presentaciones feria de ciencias');


-- Extra rows to meet ≥30 requirement on all 8 tables
-- ORDER MATTERS: parents first (profesores, acudientes, estudiantes), then child tables (links, matriculas)

INSERT INTO profesores (tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, correo_electronico, telefono, especializacion, fecha_contratacion, estado) VALUES
('CC', '1098765464', 'Rocio', 'Elena', 'Bautista', 'Pineda', 'rocio.bautista@littlehorizons.edu.co', '3001234599', 'Primaria', '2022-02-14', 'Activo');

INSERT INTO acudientes (tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, parentesco, telefono_principal, correo_electronico, direccion, ocupacion, es_responsable_financiero) VALUES
('CC', '912345710', 'Oscar', 'David', 'Rincon', 'Salazar', 'Padre', '3101112265', 'oscar.rincon@email.com', 'Calle 40 # 10-20, Bucaramanga', 'Ingeniero', TRUE);

INSERT INTO estudiantes (tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, fecha_nacimiento, genero, direccion, telefono, correo_electronico, tipo_sangre, fecha_ingreso, estado) VALUES
('TI', '1001234602', 'Lucas', 'Andres', 'Rincon', 'Salazar', '2013-08-16', 'M', 'Calle 40 # 10-20, Bucaramanga', '3201112268', 'lucas.rincon@student.lh.edu.co', 'O+', '2023-01-18', 'Activo');

-- Now that ids 36 (student) and 33 (guardian) exist:
INSERT INTO estudiante_acudiente (id_estudiante, id_acudiente, es_principal, es_contacto_emergencia) VALUES
(36, 33, TRUE, TRUE);

INSERT INTO matriculas (id_estudiante, id_grado, año_academico, estado_matricula, fecha_matricula) VALUES
(36, 5, 2025, 'Completada', '2025-01-15'),
(36, 6, 2026, 'Activa', '2026-01-20');

INSERT INTO materias (codigo_materia, nombre_materia, descripcion, id_grado, horas_semanales, esta_activa) VALUES
('EFI-5', 'Educacion Fisica Quinto', 'Deporte y salud', 6, 2, TRUE);

INSERT INTO clubes (nombre_club, descripcion, id_profesor_supervisor, capacidad_maxima, horario_reunion, esta_activo) VALUES
('Club de Yoga y Mindfulness', 'Bienestar emocional y fisico', 14, 20, 'Viernes 16:00-17:00', TRUE);

INSERT INTO recursos (codigo_recurso, nombre_recurso, id_categoria, descripcion, fecha_adquisicion, ubicacion_fisica, estado_recurso) VALUES
('DEP-006', 'Raqueta de tenis (par)', 3, 'Para educacion fisica', '2024-03-01', 'Deposito deportivo', 'Disponible');

-- ============================================
-- VERIFICATION QUERIES (optional, for the evaluator)
-- ============================================
-- SELECT 'estudiantes' AS tabla, COUNT(*) AS total FROM estudiantes
-- UNION ALL SELECT 'acudientes', COUNT(*) FROM acudientes
-- UNION ALL SELECT 'profesores', COUNT(*) FROM profesores
-- UNION ALL SELECT 'materias', COUNT(*) FROM materias
-- UNION ALL SELECT 'registros_academicos', COUNT(*) FROM registros_academicos
-- UNION ALL SELECT 'pagos', COUNT(*) FROM pagos
-- UNION ALL SELECT 'clubes', COUNT(*) FROM clubes
-- UNION ALL SELECT 'recursos', COUNT(*) FROM recursos;

-- End of docs/database.sql
