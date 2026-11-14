package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.EventController;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.EventPosition;
import com.littlehorizons.erp.model.EventType;
import com.littlehorizons.erp.model.ParticipationRole;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.view.console.ConsoleInput;
import com.littlehorizons.erp.view.console.ConsolePrinter;
import com.littlehorizons.erp.view.console.Menu;
import com.littlehorizons.erp.view.console.PagedTable;
import com.littlehorizons.erp.view.console.ViewFormat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Screens for school events: catalog, participants and positions (positions only exist in competitive events).
 */
// SOLID: SRP – event screens only
public class EventView {

    private final EventController events;
    private final NameLookup names;
    private final Pickers pickers;
    private final ConsoleInput input;
    private final ConsolePrinter printer;
    private final PagedTable pagedTable;

    public EventView(EventController events, NameLookup names, Pickers pickers, ConsoleInput input,
                     ConsolePrinter printer, PagedTable pagedTable) {
        this.events = events;
        this.names = names;
        this.pickers = pickers;
        this.input = input;
        this.printer = printer;
        this.pagedTable = pagedTable;
    }

    public void menu() {
        new Menu("EVENTOS", "Volver", input, printer)
                .add("Listar eventos", () -> showEvents(events.listEvents()))
                .add("Listar eventos por tipo", this::listByType)
                .add("Crear evento", this::create)
                .add("Editar evento", this::edit)
                .add("Activar / desactivar evento", this::toggleActive)
                .add("Agregar participante a un evento", this::addParticipant)
                .add("Registrar posición (eventos con competencia)", this::recordPosition)
                .add("Quitar participante", this::removeParticipant)
                .add("Ver participantes de un evento", this::showParticipants)
                .add("Ver eventos de un estudiante", this::showStudentEvents)
                .run();
    }

    private void showEvents(List<SchoolEvent> list) {
        Map<Integer, String> teacherNames = names.teachers();
        List<List<String>> rows = list.stream()
                .map(e -> List.of(String.valueOf(e.getId()), e.getName(), e.getType().toString(),
                        ViewFormat.date(e.getEventDate()), ViewFormat.text(e.getPlace()),
                        ViewFormat.yesNo(e.isCompetitive()), NameLookup.of(teacherNames, e.getOrganizerId()),
                        ViewFormat.yesNo(e.isActive())))
                .toList();
        pagedTable.show(List.of("ID", "Evento", "Tipo", "Fecha", "Lugar", "Competencia", "Organizador", "Activo"), rows);
    }

    private void listByType() {
        EventType type = input.readEnum("Tipo de evento", EventType.class);
        showEvents(events.listEventsByType(type));
    }

    private void create() {
        SchoolEvent event = new SchoolEvent();
        event.setName(input.readText("Nombre del evento"));
        event.setDescription(input.readOptionalText("Descripción"));
        event.setType(input.readEnum("Tipo de evento", EventType.class));
        event.setEventDate(input.readDate("Fecha del evento"));
        event.setPlace(input.readOptionalText("Lugar"));
        event.setCompetitive(input.confirm("¿Es un evento con competencia (1er, 2do, 3er lugar)?"));
        if (input.confirm("¿Asignar un profesor organizador?")) {
            event.setOrganizerId(pickers.teacher().map(Teacher::getId).orElse(null));
        }
        SchoolEvent saved = events.createEvent(event);
        printer.success("Evento creado con ID " + saved.getId() + ".");
    }

    private void edit() {
        Optional<SchoolEvent> picked = pickers.event();
        if (picked.isEmpty()) {
            return;
        }
        SchoolEvent event = picked.get();
        printer.info("Presione ENTER en cada dato para dejarlo igual.");
        event.setName(input.readTextOrKeep("Nombre", event.getName()));
        event.setDescription(input.readTextOrKeep("Descripción", event.getDescription()));
        event.setPlace(input.readTextOrKeep("Lugar", event.getPlace()));
        event.setEventDate(input.readDateOrDefault("Fecha del evento", event.getEventDate()));
        events.updateEvent(event);
        printer.success("Evento actualizado.");
    }

    private void toggleActive() {
        Optional<SchoolEvent> picked = pickers.event();
        if (picked.isEmpty()) {
            return;
        }
        boolean newState = !picked.get().isActive();
        if (input.confirm((newState ? "¿Activar " : "¿Desactivar ") + picked.get().getName() + "?")) {
            events.setActive(picked.get().getId(), newState);
            printer.success("Evento " + (newState ? "activado." : "desactivado."));
        }
    }

    // ------------------------------------------------------------------ participants

    private void addParticipant() {
        Optional<SchoolEvent> event = pickers.event();
        if (event.isEmpty()) {
            return;
        }
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        ParticipationRole role = input.readEnum("Rol del estudiante", ParticipationRole.class);
        String observations = input.readOptionalText("Observaciones");
        events.addParticipant(event.get().getId(), student.get().getId(), role, observations);
        printer.success(student.get().getFullName() + " agregado a " + event.get().getName() + ".");
    }

    private void recordPosition() {
        Optional<SchoolEvent> event = pickers.event();
        if (event.isEmpty()) {
            return;
        }
        if (!event.get().isCompetitive()) {
            printer.warning("Ese evento no tiene competencia: no admite posiciones.");
            return;
        }
        Map<Integer, String> studentNames = names.students();
        Optional<EventParticipant> participant = pickers.choose("Participantes", events.listParticipants(event.get().getId()),
                List.of("Estudiante", "Rol", "Posición actual"),
                p -> List.of(NameLookup.of(studentNames, p.getStudentId()), p.getRole().toString(),
                        ViewFormat.text(p.getPosition())),
                EventParticipant::getId);
        if (participant.isEmpty()) {
            return;
        }
        EventPosition position = input.readEnum("Posición obtenida", EventPosition.class);
        events.recordPosition(participant.get().getId(), position);
        printer.success("Posición registrada: " + position + ".");
    }

    private void removeParticipant() {
        Optional<SchoolEvent> event = pickers.event();
        if (event.isEmpty()) {
            return;
        }
        Map<Integer, String> studentNames = names.students();
        Optional<EventParticipant> participant = pickers.choose("Participantes", events.listParticipants(event.get().getId()),
                List.of("Estudiante", "Rol"),
                p -> List.of(NameLookup.of(studentNames, p.getStudentId()), p.getRole().toString()),
                EventParticipant::getId);
        if (participant.isPresent() && input.confirm("¿Quitar este participante?")) {
            events.removeParticipant(participant.get().getId());
            printer.success("Participante eliminado.");
        }
    }

    private void showParticipants() {
        Optional<SchoolEvent> event = pickers.event();
        if (event.isEmpty()) {
            return;
        }
        Map<Integer, String> studentNames = names.students();
        List<List<String>> rows = events.listParticipants(event.get().getId()).stream()
                .map(p -> List.of(NameLookup.of(studentNames, p.getStudentId()), p.getRole().toString(),
                        ViewFormat.text(p.getPosition()), ViewFormat.text(p.getObservations())))
                .toList();
        printer.section("Participantes de " + event.get().getName());
        pagedTable.show(List.of("Estudiante", "Rol", "Posición", "Observaciones"), rows);
    }

    private void showStudentEvents() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        Map<Integer, String> eventNames = names.events();
        List<List<String>> rows = events.listParticipationsOfStudent(student.get().getId()).stream()
                .map(p -> List.of(NameLookup.of(eventNames, p.getEventId()), p.getRole().toString(),
                        ViewFormat.text(p.getPosition())))
                .toList();
        printer.section("Eventos de " + student.get().getFullName());
        printer.table(List.of("Evento", "Rol", "Posición"), rows);
    }
}
