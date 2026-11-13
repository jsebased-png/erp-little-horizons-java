package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dto.OverdueLoan;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.NotFoundException;
import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.LoanStatus;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceStatus;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeacherStatus;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Business rules for resource loans to students and teachers.
 * The tables are separate, so this controller never chooses a table: it asks a provider
 * (Function BorrowerType -> LoanDao, implemented by LoanDaoFactory) for the right DAO.
 * Rules: resource must be Disponible; borrower must be active; return date within 30 days;
 * at most 3 unreturned loans per borrower.
 */
// SOLID: SRP – loan rules only
// SOLID: DIP – receives a Function instead of the concrete factory, so tests can pass any provider
public class LoanController {

    public static final int MAX_LOAN_DAYS = 30;
    public static final int MAX_ACTIVE_LOANS_PER_BORROWER = 3;

    private final Function<BorrowerType, LoanDao> loanDaoProvider;
    private final ResourceDao resourceDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;
    private final AppLogger logger = AppLogger.getInstance();

    public LoanController(Function<BorrowerType, LoanDao> loanDaoProvider, ResourceDao resourceDao,
                          StudentDao studentDao, TeacherDao teacherDao) {
        this.loanDaoProvider = loanDaoProvider;
        this.resourceDao = resourceDao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
    }

    // ------------------------------------------------------------------ loans

    /** Registers a loan. The DAO saves the loan and changes the resource to "Prestado" in one transaction. */
    public Loan registerLoan(BorrowerType type, int borrowerId, int resourceId, LocalDate expectedReturn, String notes) {
        Validator.requireNotNull("Borrower type", type);
        Validator.requireNotNull("Expected return date", expectedReturn);
        LocalDate today = LocalDate.now();
        if (!expectedReturn.isAfter(today) || expectedReturn.isAfter(today.plusDays(MAX_LOAN_DAYS))) {
            throw new BusinessRuleException("The return date must be after today and at most " + MAX_LOAN_DAYS + " days away.");
        }

        Resource resource = resourceDao.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found: id " + resourceId));
        if (resource.getStatus() != ResourceStatus.AVAILABLE) {
            throw new BusinessRuleException("The resource \"" + resource.getName() + "\" is not available (status: "
                    + resource.getStatus() + ").");
        }
        checkBorrowerIsActive(type, borrowerId);

        LoanDao dao = loanDaoProvider.apply(type);
        long unreturned = dao.findByBorrower(borrowerId).stream()
                .filter(l -> l.getStatus() != LoanStatus.RETURNED)
                .count();
        if (unreturned >= MAX_ACTIVE_LOANS_PER_BORROWER) {
            throw new BusinessRuleException("The borrower already has " + MAX_ACTIVE_LOANS_PER_BORROWER + " unreturned loans.");
        }

        Loan loan = new Loan();
        loan.setResourceId(resourceId);
        loan.setBorrowerId(borrowerId);
        loan.setBorrowerType(type);
        loan.setLoanDate(today);
        loan.setExpectedReturnDate(expectedReturn);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setNotes(Validator.optionalText("Notes", notes, 500));

        if (!dao.registerLoan(loan)) {
            // Another operation took the resource between our check and the transaction
            throw new BusinessRuleException("The resource is no longer available.");
        }
        logger.info(getClass(), "Loan " + loan.getId() + " registered (" + type + " " + borrowerId + ", resource " + resourceId + ")");
        return loan;
    }

    /** Registers the return today: loan "Devuelto" with real date and resource "Disponible" (one transaction). */
    public void returnLoan(BorrowerType type, int loanId) {
        Validator.requireNotNull("Borrower type", type);
        if (!loanDaoProvider.apply(type).returnLoan(loanId, LocalDate.now())) {
            throw new BusinessRuleException("The loan does not exist or was already returned.");
        }
        logger.info(getClass(), "Loan " + loanId + " (" + type + ") returned");
    }

    public List<Loan> listOutstanding(BorrowerType type) {
        return loanDaoProvider.apply(Validator.requireNotNull("Borrower type", type)).findOutstanding();
    }

    public List<Loan> listByBorrower(BorrowerType type, int borrowerId) {
        return loanDaoProvider.apply(Validator.requireNotNull("Borrower type", type)).findByBorrower(borrowerId);
    }

    // ------------------------------------------------------------------ overdue

    /** Marks as "Vencido" every active loan whose date has passed (both tables). Returns how many changed. */
    public int refreshOverdueLoans() {
        LocalDate today = LocalDate.now();
        return Arrays.stream(BorrowerType.values())
                .mapToInt(type -> loanDaoProvider.apply(type).markOverdue(today))
                .sum();
    }

    /** Overdue report for students and teachers together, with names, sorted by days late (worst first). */
    public List<OverdueLoan> findOverdueLoans() {
        refreshOverdueLoans(); // "Vencido" is not set by the database by itself, so we refresh before reading
        LocalDate today = LocalDate.now();

        Map<Integer, Resource> resources = resourceDao.findAll().stream()
                .collect(Collectors.toMap(Resource::getId, r -> r));
        Map<BorrowerType, Map<Integer, String>> borrowerNames = Map.of(
                BorrowerType.STUDENT, studentDao.findAll().stream()
                        .collect(Collectors.toMap(Student::getId, Student::getFullName)),
                BorrowerType.TEACHER, teacherDao.findAll().stream()
                        .collect(Collectors.toMap(Teacher::getId, Teacher::getFullName)));

        // flatMap: one stream per borrower type, merged into a single list
        return Arrays.stream(BorrowerType.values())
                .flatMap(type -> loanDaoProvider.apply(type).findByStatus(LoanStatus.OVERDUE).stream()
                        .map(loan -> {
                            Resource resource = resources.get(loan.getResourceId());
                            return new OverdueLoan(type, loan.getId(),
                                    resource != null ? resource.getCode() : "?",
                                    resource != null ? resource.getName() : "Unknown resource",
                                    borrowerNames.get(type).getOrDefault(loan.getBorrowerId(), "Unknown"),
                                    loan.getExpectedReturnDate(),
                                    ChronoUnit.DAYS.between(loan.getExpectedReturnDate(), today));
                        }))
                .sorted(Comparator.comparingLong(OverdueLoan::daysLate).reversed())
                .toList();
    }

    // ------------------------------------------------------------------ private helpers

    private void checkBorrowerIsActive(BorrowerType type, int borrowerId) {
        if (type == BorrowerType.STUDENT) {
            Student student = studentDao.findById(borrowerId)
                    .orElseThrow(() -> new NotFoundException("Student not found: id " + borrowerId));
            if (student.getStatus() != StudentStatus.ACTIVE) {
                throw new BusinessRuleException("Only ACTIVE students can borrow resources.");
            }
        } else {
            Teacher teacher = teacherDao.findById(borrowerId)
                    .orElseThrow(() -> new NotFoundException("Teacher not found: id " + borrowerId));
            if (teacher.getStatus() != TeacherStatus.ACTIVE) {
                throw new BusinessRuleException("Only ACTIVE teachers can borrow resources.");
            }
        }
    }
}
