package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.LoanStatus;
import com.littlehorizons.erp.model.ResourceStatus;
import com.littlehorizons.erp.util.EnumUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Shared JDBC logic for both loan tables (students and teachers).
 * The two tables have the same columns except the borrower column, so only
 * the table name and that column change in each subclass.
 * Table and column names are fixed constants of the subclasses (never user input);
 * every VALUE still goes through a "?" parameter of a PreparedStatement.
 */
// SOLID: OCP – a new borrower type (for example "staff") only needs a new subclass, nothing here changes
public abstract class AbstractLoanDaoJdbc extends BaseJdbcDao implements LoanDao {

    protected AbstractLoanDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    /** Name of the loans table. */
    protected abstract String table();

    /** Name of the column that holds the borrower id. */
    protected abstract String borrowerColumn();

    private String select() {
        return "SELECT id_prestamo, id_recurso, " + borrowerColumn() + " AS id_prestatario, fecha_prestamo, "
                + "fecha_devolucion_esperada, fecha_devolucion_real, estado_prestamo, observaciones FROM " + table() + " ";
    }

    @Override
    public Optional<Loan> findById(int id) {
        return queryOne(select() + "WHERE id_prestamo = ?", this::map, id);
    }

    @Override
    public List<Loan> findAll() {
        return queryList(select() + "ORDER BY fecha_prestamo DESC", this::map);
    }

    @Override
    public boolean registerLoan(Loan loan) {
        // Two statements, one transaction: the loan row and the resource status change together.
        return inTransaction(connection -> {
            int changed = executeUpdate(connection,
                    "UPDATE recursos SET estado_recurso = ? WHERE id_recurso = ? AND estado_recurso = ?",
                    ResourceStatus.LOANED, loan.getResourceId(), ResourceStatus.AVAILABLE);
            if (changed == 0) {
                return false; // resource was not available: nothing was changed
            }
            int id = executeInsert(connection,
                    "INSERT INTO " + table() + " (id_recurso, " + borrowerColumn() + ", fecha_prestamo, "
                    + "fecha_devolucion_esperada, estado_prestamo, observaciones) VALUES (?, ?, ?, ?, ?, ?)",
                    loan.getResourceId(), loan.getBorrowerId(), loan.getLoanDate(), loan.getExpectedReturnDate(),
                    LoanStatus.ACTIVE, loan.getNotes());
            loan.setId(id);
            return true;
        });
    }

    @Override
    public boolean returnLoan(int loanId, LocalDate returnDate) {
        return inTransaction(connection -> {
            List<Integer> resourceIds = queryList(connection,
                    "SELECT id_recurso FROM " + table() + " WHERE id_prestamo = ? AND estado_prestamo <> ?",
                    rs -> rs.getInt(1), loanId, LoanStatus.RETURNED);
            if (resourceIds.isEmpty()) {
                return false; // unknown loan or already returned
            }
            executeUpdate(connection,
                    "UPDATE " + table() + " SET estado_prestamo = ?, fecha_devolucion_real = ? WHERE id_prestamo = ?",
                    LoanStatus.RETURNED, returnDate, loanId);
            executeUpdate(connection, "UPDATE recursos SET estado_recurso = ? WHERE id_recurso = ?",
                    ResourceStatus.AVAILABLE, resourceIds.get(0));
            return true;
        });
    }

    @Override
    public List<Loan> findByBorrower(int borrowerId) {
        return queryList(select() + "WHERE " + borrowerColumn() + " = ? ORDER BY fecha_prestamo DESC",
                this::map, borrowerId);
    }

    @Override
    public List<Loan> findByStatus(LoanStatus status) {
        return queryList(select() + "WHERE estado_prestamo = ? ORDER BY fecha_devolucion_esperada",
                this::map, status);
    }

    @Override
    public List<Loan> findOutstanding() {
        return queryList(select() + "WHERE estado_prestamo <> ? ORDER BY fecha_devolucion_esperada",
                this::map, LoanStatus.RETURNED);
    }

    @Override
    public int markOverdue(LocalDate today) {
        return executeUpdate("UPDATE " + table() + " SET estado_prestamo = ? "
                + "WHERE estado_prestamo = ? AND fecha_devolucion_esperada < ?",
                LoanStatus.OVERDUE, LoanStatus.ACTIVE, today);
    }

    private Loan map(ResultSet rs) throws SQLException {
        Loan loan = new Loan();
        loan.setId(rs.getInt("id_prestamo"));
        loan.setResourceId(rs.getInt("id_recurso"));
        loan.setBorrowerId(rs.getInt("id_prestatario"));
        loan.setBorrowerType(getBorrowerType());
        loan.setLoanDate(date(rs, "fecha_prestamo"));
        loan.setExpectedReturnDate(date(rs, "fecha_devolucion_esperada"));
        loan.setActualReturnDate(date(rs, "fecha_devolucion_real"));
        loan.setStatus(EnumUtil.fromLabel(LoanStatus.class, rs.getString("estado_prestamo")));
        loan.setNotes(rs.getString("observaciones"));
        return loan;
    }
}
