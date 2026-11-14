package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ClubController;
import com.littlehorizons.erp.controller.EventController;
import com.littlehorizons.erp.controller.PaymentConceptController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.SubjectController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Translates ids into readable names for the tables (the tables store ids, people want to read names).
 * Each method asks the controller for the current list and builds an id → name map with a stream.
 */
// SOLID: SRP – only builds display names; DIP – depends on controllers, never on DAOs
public class NameLookup {

    private final StudentController students;
    private final SubjectController subjects;
    private final TeacherController teachers;
    private final ResourceController resources;
    private final ClubController clubs;
    private final EventController events;
    private final PaymentConceptController concepts;

    public NameLookup(StudentController students, SubjectController subjects, TeacherController teachers,
                      ResourceController resources, ClubController clubs, EventController events,
                      PaymentConceptController concepts) {
        this.students = students;
        this.subjects = subjects;
        this.teachers = teachers;
        this.resources = resources;
        this.clubs = clubs;
        this.events = events;
        this.concepts = concepts;
    }

    public Map<Integer, String> students() {
        return students.listStudents().stream().collect(Collectors.toMap(Student::getId, Student::getFullName));
    }

    public Map<Integer, String> subjects() {
        return subjects.listSubjects().stream().collect(Collectors.toMap(Subject::getId, Subject::getName));
    }

    public Map<Integer, String> grades() {
        return subjects.listGrades().stream().collect(Collectors.toMap(Grade::getId, Grade::getName));
    }

    public Map<Integer, String> teachers() {
        return teachers.listTeachers().stream().collect(Collectors.toMap(Teacher::getId, Teacher::getFullName));
    }

    public Map<Integer, String> resources() {
        return resources.listResources().stream()
                .collect(Collectors.toMap(Resource::getId, r -> r.getCode() + " - " + r.getName()));
    }

    public Map<Integer, String> clubs() {
        return clubs.listClubs().stream().collect(Collectors.toMap(Club::getId, Club::getName));
    }

    public Map<Integer, String> events() {
        return events.listEvents().stream().collect(Collectors.toMap(SchoolEvent::getId, SchoolEvent::getName));
    }

    public Map<Integer, String> concepts() {
        return concepts.listConcepts().stream().collect(Collectors.toMap(PaymentConcept::getId, PaymentConcept::getName));
    }

    /** Name for an id, or "#id" when it is not in the map. */
    public static String of(Map<Integer, String> names, Integer id) {
        return id == null ? "-" : names.getOrDefault(id, "#" + id);
    }

    /** Same as of(...) but for use as a method reference in streams: names::get style. */
    public static Function<Integer, String> resolver(Map<Integer, String> names) {
        return id -> of(names, id);
    }
}
