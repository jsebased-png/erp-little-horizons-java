package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ClubController;
import com.littlehorizons.erp.dto.BatchResult;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.view.console.ConsoleInput;
import com.littlehorizons.erp.view.console.ConsolePrinter;
import com.littlehorizons.erp.view.console.Menu;
import com.littlehorizons.erp.view.console.PagedTable;
import com.littlehorizons.erp.view.console.ViewFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Screens for clubs: catalog, capacity, single and multiple enrollment, withdrawal and member lists.
 */
// SOLID: SRP – club screens only; the capacity rule is checked by ClubController, not here
public class ClubView {

    private final ClubController clubs;
    private final NameLookup names;
    private final Pickers pickers;
    private final ConsoleInput input;
    private final ConsolePrinter printer;
    private final PagedTable pagedTable;

    public ClubView(ClubController clubs, NameLookup names, Pickers pickers, ConsoleInput input,
                    ConsolePrinter printer, PagedTable pagedTable) {
        this.clubs = clubs;
        this.names = names;
        this.pickers = pickers;
        this.input = input;
        this.printer = printer;
        this.pagedTable = pagedTable;
    }

    public void menu() {
        new Menu("CLUBES", "Volver", input, printer)
                .add("Listar clubes (con cupos)", this::list)
                .add("Crear club", this::create)
                .add("Editar club", this::edit)
                .add("Activar / desactivar club", this::toggleActive)
                .add("Inscribir estudiante en un club", this::join)
                .add("Inscribir estudiante en varios clubes", this::joinSeveral)
                .add("Retirar estudiante de un club", this::leave)
                .add("Ver miembros de un club", this::showMembers)
                .add("Ver clubes de un estudiante", this::showStudentClubs)
                .run();
    }

    private void list() {
        Map<Integer, String> teacherNames = names.teachers();
        List<List<String>> rows = clubs.listClubs().stream()
                .map(c -> {
                    int free = clubs.availableSeats(c.getId());
                    return List.of(String.valueOf(c.getId()), c.getName(),
                            NameLookup.of(teacherNames, c.getSupervisorId()),
                            String.valueOf(c.getMaxCapacity()), String.valueOf(c.getMaxCapacity() - free),
                            String.valueOf(free), ViewFormat.yesNo(c.isActive()));
                })
                .toList();
        pagedTable.show(List.of("ID", "Club", "Encargado", "Cupo", "Inscritos", "Libres", "Activo"), rows);
    }

    private void create() {
        Club club = new Club();
        club.setName(input.readText("Nombre del club"));
        club.setDescription(input.readOptionalText("Descripción"));
        club.setMaxCapacity(input.readInt("Cupo máximo", 1, 500));
        club.setMeetingSchedule(input.readOptionalText("Horario de reunión (ej. Martes 3:00 pm)"));
        if (input.confirm("¿Asignar un profesor encargado?")) {
            club.setSupervisorId(pickers.teacher().map(Teacher::getId).orElse(null));
        }
        Club saved = clubs.createClub(club);
        printer.success("Club creado con ID " + saved.getId() + ".");
    }

    private void edit() {
        Optional<Club> picked = pickers.club();
        if (picked.isEmpty()) {
            return;
        }
        Club club = picked.get();
        printer.info("Presione ENTER en cada dato para dejarlo igual.");
        club.setName(input.readTextOrKeep("Nombre", club.getName()));
        club.setDescription(input.readTextOrKeep("Descripción", club.getDescription()));
        club.setMaxCapacity(input.readIntOrDefault("Cupo máximo", 1, 500, club.getMaxCapacity()));
        club.setMeetingSchedule(input.readTextOrKeep("Horario de reunión", club.getMeetingSchedule()));
        if (input.confirm("¿Cambiar el profesor encargado?")) {
            club.setSupervisorId(pickers.teacher().map(Teacher::getId).orElse(club.getSupervisorId()));
        }
        clubs.updateClub(club);
        printer.success("Club actualizado.");
    }

    private void toggleActive() {
        Optional<Club> picked = pickers.club();
        if (picked.isEmpty()) {
            return;
        }
        boolean newState = !picked.get().isActive();
        if (input.confirm((newState ? "¿Activar " : "¿Desactivar ") + picked.get().getName() + "?")) {
            clubs.setActive(picked.get().getId(), newState);
            printer.success("Club " + (newState ? "activado." : "desactivado."));
        }
    }

    // ------------------------------------------------------------------ memberships

    private void join() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        Optional<Club> club = pickers.club();
        if (club.isEmpty()) {
            return;
        }
        clubs.joinClub(student.get().getId(), club.get().getId());
        printer.success(student.get().getFullName() + " inscrito en " + club.get().getName() + ".");
    }

    /** Multiple enrollment: the user collects several clubs and the controller reports one result per club. */
    private void joinSeveral() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        List<Integer> clubIds = new ArrayList<>();
        Optional<Club> club;
        do {
            club = pickers.club();
            club.filter(c -> !clubIds.contains(c.getId())).ifPresent(c -> clubIds.add(c.getId()));
        } while (club.isPresent() && input.confirm("¿Agregar otro club a la lista?"));
        if (clubIds.isEmpty()) {
            printer.info("No se seleccionó ningún club.");
            return;
        }
        BatchResult result = clubs.joinClubs(student.get().getId(), clubIds);
        Map<Integer, String> clubNames = names.clubs();
        result.accepted().forEach(id -> printer.success("Inscrito en " + NameLookup.of(clubNames, id) + "."));
        result.rejected().forEach((id, reason) ->
                printer.warning("No se pudo inscribir en " + NameLookup.of(clubNames, id) + ": " + reason));
    }

    private void leave() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        Map<Integer, String> clubNames = names.clubs();
        List<ClubMembership> active = clubs.listMembershipsOfStudent(student.get().getId()).stream()
                .filter(m -> m.getStatus() == EnrollmentStatus.ACTIVE)
                .toList();
        Optional<ClubMembership> picked = pickers.choose("Clubes activos del estudiante", active,
                List.of("Club", "Ingreso"),
                m -> List.of(NameLookup.of(clubNames, m.getClubId()), ViewFormat.date(m.getJoinDate())),
                ClubMembership::getId);
        if (picked.isPresent()) {
            String reason = input.readOptionalText("Motivo del retiro");
            clubs.leaveClub(picked.get().getId(), reason);
            printer.success("Estudiante retirado del club.");
        }
    }

    private void showMembers() {
        Optional<Club> club = pickers.club();
        if (club.isEmpty()) {
            return;
        }
        Map<Integer, String> studentNames = names.students();
        List<List<String>> rows = clubs.listMembers(club.get().getId()).stream()
                .map(m -> List.of(NameLookup.of(studentNames, m.getStudentId()), ViewFormat.date(m.getJoinDate()),
                        m.getStatus().toString(), ViewFormat.date(m.getLeaveDate()), ViewFormat.text(m.getLeaveReason())))
                .toList();
        printer.section("Miembros de " + club.get().getName());
        pagedTable.show(List.of("Estudiante", "Ingreso", "Estado", "Retiro", "Motivo"), rows);
    }

    private void showStudentClubs() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        Map<Integer, String> clubNames = names.clubs();
        List<List<String>> rows = clubs.listMembershipsOfStudent(student.get().getId()).stream()
                .map(m -> List.of(NameLookup.of(clubNames, m.getClubId()), ViewFormat.date(m.getJoinDate()),
                        m.getStatus().toString(), ViewFormat.date(m.getLeaveDate())))
                .toList();
        printer.section("Clubes de " + student.get().getFullName());
        printer.table(List.of("Club", "Ingreso", "Estado", "Retiro"), rows);
    }
}
