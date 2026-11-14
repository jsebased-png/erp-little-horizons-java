package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.LoanController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.dto.OverdueLoan;
import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.ResourceStatus;
import com.littlehorizons.erp.view.console.ConsoleInput;
import com.littlehorizons.erp.view.console.ConsolePrinter;
import com.littlehorizons.erp.view.console.Menu;
import com.littlehorizons.erp.view.console.PagedTable;
import com.littlehorizons.erp.view.console.ViewFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Screens of "6. Gestión de Recursos": resource catalog and loans to students and teachers.
 */
// SOLID: SRP – resource and loan screens only; the "one loan per resource" rule is in LoanController
public class ResourceView {

    private final ResourceController resources;
    private final LoanController loans;
    private final NameLookup names;
    private final Pickers pickers;
    private final ConsoleInput input;
    private final ConsolePrinter printer;
    private final PagedTable pagedTable;

    public ResourceView(ResourceController resources, LoanController loans, NameLookup names, Pickers pickers,
                        ConsoleInput input, ConsolePrinter printer, PagedTable pagedTable) {
        this.resources = resources;
        this.loans = loans;
        this.names = names;
        this.pickers = pickers;
        this.input = input;
        this.printer = printer;
        this.pagedTable = pagedTable;
    }

    public void menu() {
        new Menu("GESTIÓN DE RECURSOS", "Volver al menú principal", input, printer)
                .addSubmenu("Catálogo de recursos", this::catalogMenu)
                .addSubmenu("Préstamos y devoluciones", this::loansMenu)
                .run();
    }

    // ------------------------------------------------------------------ catalog

    private void catalogMenu() {
        new Menu("CATÁLOGO DE RECURSOS", "Volver", input, printer)
                .add("Listar todos los recursos", () -> showResources(resources.listResources()))
                .add("Listar por estado", this::listByStatus)
                .add("Listar por categoría", this::listByCategory)
                .add("Buscar recurso por código", this::findByCode)
                .add("Registrar recurso", this::create)
                .add("Editar recurso", this::edit)
                .add("Cambiar estado (mantenimiento / baja / disponible)", this::changeStatus)
                .run();
    }

    private void showResources(List<Resource> list) {
        Map<Integer, String> categoryNames = categoryNames();
        List<List<String>> rows = list.stream()
                .map(r -> List.of(String.valueOf(r.getId()), r.getCode(), r.getName(),
                        NameLookup.of(categoryNames, r.getCategoryId()), r.getLocation(), r.getStatus().toString()))
                .toList();
        pagedTable.show(List.of("ID", "Código", "Recurso", "Categoría", "Ubicación", "Estado"), rows);
    }

    private Map<Integer, String> categoryNames() {
        return resources.listCategories().stream()
                .collect(Collectors.toMap(ResourceCategory::getId, ResourceCategory::getName));
    }

    private Optional<ResourceCategory> chooseCategory() {
        return pickers.choose("Categorías de recurso", resources.listCategories(), List.of("Categoría"),
                c -> List.of(c.getName()), ResourceCategory::getId);
    }

    private void listByStatus() {
        ResourceStatus status = input.readEnum("Estado", ResourceStatus.class);
        showResources(resources.listByStatus(status));
    }

    private void listByCategory() {
        chooseCategory().ifPresent(c -> showResources(resources.listByCategory(c.getId())));
    }

    private void findByCode() {
        String code = input.readText("Código del recurso");
        showResources(List.of(resources.findByCode(code)));
    }

    private void create() {
        Optional<ResourceCategory> category = chooseCategory();
        if (category.isEmpty()) {
            return;
        }
        Resource resource = new Resource();
        resource.setCategoryId(category.get().getId());
        resource.setCode(input.readText("Código único (ej. LIB-001)"));
        resource.setName(input.readText("Nombre del recurso"));
        resource.setDescription(input.readOptionalText("Descripción"));
        resource.setAcquisitionDate(input.readOptionalDate("Fecha de adquisición"));
        resource.setLocation(input.readText("Ubicación (ej. Biblioteca, estante 3)"));
        Resource saved = resources.createResource(resource);
        printer.success("Recurso registrado con ID " + saved.getId() + " (estado: " + saved.getStatus() + ").");
    }

    private void edit() {
        Optional<Resource> picked = selectResource(resources.listResources());
        if (picked.isEmpty()) {
            return;
        }
        Resource resource = picked.get();
        printer.info("Presione ENTER en cada dato para dejarlo igual.");
        resource.setCode(input.readTextOrKeep("Código", resource.getCode()));
        resource.setName(input.readTextOrKeep("Nombre", resource.getName()));
        resource.setDescription(input.readTextOrKeep("Descripción", resource.getDescription()));
        resource.setLocation(input.readTextOrKeep("Ubicación", resource.getLocation()));
        resources.updateResource(resource);
        printer.success("Recurso actualizado.");
    }

    private void changeStatus() {
        Optional<Resource> picked = selectResource(resources.listResources());
        if (picked.isEmpty()) {
            return;
        }
        ResourceStatus status = input.readEnum("Nuevo estado", ResourceStatus.class);
        resources.changeStatus(picked.get().getId(), status);
        printer.success(picked.get().getCode() + " ahora está: " + status + ".");
    }

    /** The user types a code, or presses ENTER to choose from the given list. */
    private Optional<Resource> selectResource(List<Resource> fallbackList) {
        String code = input.readLine("Código del recurso (ENTER = ver lista)");
        if (!code.isEmpty()) {
            return Optional.of(resources.findByCode(code));
        }
        Map<Integer, String> categoryNames = categoryNames();
        return pickers.choose("Recursos", fallbackList, List.of("Código", "Recurso", "Categoría", "Estado"),
                r -> List.of(r.getCode(), r.getName(), NameLookup.of(categoryNames, r.getCategoryId()),
                        r.getStatus().toString()),
                Resource::getId);
    }

    // ------------------------------------------------------------------ loans

    private void loansMenu() {
        new Menu("PRÉSTAMOS Y DEVOLUCIONES", "Volver", input, printer)
                .add("Registrar préstamo", this::registerLoan)
                .add("Registrar devolución", this::returnLoan)
                .add("Ver préstamos sin devolver", this::showOutstanding)
                .add("Ver préstamos de una persona", this::showLoansOfBorrower)
                .add("Reporte de préstamos vencidos", this::showOverdueLoans)
                .run();
    }

    private void registerLoan() {
        BorrowerType type = input.readEnum("¿Quién recibe el recurso?", BorrowerType.class);
        Optional<Integer> borrowerId = chooseBorrower(type);
        if (borrowerId.isEmpty()) {
            return;
        }
        Optional<Resource> resource = selectResource(resources.listByStatus(ResourceStatus.AVAILABLE));
        if (resource.isEmpty()) {
            return;
        }
        LocalDate expected = input.readDateOrDefault("Fecha de devolución programada", LocalDate.now().plusDays(14));
        String notes = input.readOptionalText("Observaciones");
        Loan loan = loans.registerLoan(type, borrowerId.get(), resource.get().getId(), expected, notes);
        printer.success("Préstamo " + loan.getId() + " registrado: " + resource.get().getName()
                + ", devolver antes del " + ViewFormat.date(expected) + ".");
    }

    private void returnLoan() {
        BorrowerType type = input.readEnum("Tipo de préstamo a devolver", BorrowerType.class);
        Optional<Loan> picked = chooseOutstandingLoan(type);
        if (picked.isPresent()) {
            loans.returnLoan(type, picked.get().getId());
            printer.success("Devolución registrada. El recurso quedó Disponible.");
        }
    }

    private void showOutstanding() {
        BorrowerType type = input.readEnum("Tipo de prestatario", BorrowerType.class);
        printer.section("Préstamos sin devolver (" + type + ")");
        showLoans(type, loans.listOutstanding(type));
    }

    private void showLoansOfBorrower() {
        BorrowerType type = input.readEnum("Tipo de prestatario", BorrowerType.class);
        Optional<Integer> borrowerId = chooseBorrower(type);
        if (borrowerId.isPresent()) {
            showLoans(type, loans.listByBorrower(type, borrowerId.get()));
        }
    }

    /** Public because the reports menu reuses this same screen. */
    public void showOverdueLoans() {
        List<OverdueLoan> overdue = loans.findOverdueLoans();
        List<List<String>> rows = overdue.stream()
                .map(o -> List.of(o.borrowerType().toString(), o.borrowerName(), o.resourceCode() + " - " + o.resourceName(),
                        ViewFormat.date(o.expectedReturnDate()), String.valueOf(o.daysLate())))
                .toList();
        printer.section("Préstamos vencidos");
        pagedTable.show(List.of("Tipo", "Prestatario", "Recurso", "Debía devolver", "Días de retraso"), rows);
    }

    private Optional<Integer> chooseBorrower(BorrowerType type) {
        return switch (type) {
            case STUDENT -> pickers.student().map(s -> s.getId());
            case TEACHER -> pickers.teacher().map(t -> t.getId());
        };
    }

    private Optional<Loan> chooseOutstandingLoan(BorrowerType type) {
        Map<Integer, String> resourceNames = names.resources();
        Map<Integer, String> borrowerNames = borrowerNames(type);
        return pickers.choose("Préstamos sin devolver", loans.listOutstanding(type),
                List.of("Recurso", "Prestatario", "Devolución programada", "Estado"),
                l -> List.of(NameLookup.of(resourceNames, l.getResourceId()),
                        NameLookup.of(borrowerNames, l.getBorrowerId()),
                        ViewFormat.date(l.getExpectedReturnDate()), l.getStatus().toString()),
                Loan::getId);
    }

    private void showLoans(BorrowerType type, List<Loan> list) {
        Map<Integer, String> resourceNames = names.resources();
        Map<Integer, String> borrowerNames = borrowerNames(type);
        List<List<String>> rows = list.stream()
                .map(l -> List.of(String.valueOf(l.getId()), NameLookup.of(resourceNames, l.getResourceId()),
                        NameLookup.of(borrowerNames, l.getBorrowerId()), ViewFormat.date(l.getLoanDate()),
                        ViewFormat.date(l.getExpectedReturnDate()), ViewFormat.date(l.getActualReturnDate()),
                        l.getStatus().toString()))
                .toList();
        pagedTable.show(List.of("ID", "Recurso", "Prestatario", "Préstamo", "Programada", "Devuelto", "Estado"), rows);
    }

    private Map<Integer, String> borrowerNames(BorrowerType type) {
        return type == BorrowerType.STUDENT ? names.students() : names.teachers();
    }
}
