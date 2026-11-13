-- ============================================================================
-- Schema changes over the official DDL (script-bd-pry-erp-colegio-little-horizon.sql)
-- The assignment allows changes "as long as they can be justified".
-- ============================================================================
USE erp_little_horizons;

-- CHANGE 1: weekly schedule of each teacher assignment.
-- WHY: the requirements ask for "days and hours with basic schedule-conflict validation",
--      but asignaciones_profesor only stores the section, not day or time.
-- HOW: a child table (1 assignment -> N slots). Deleting an assignment deletes its slots.
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
