package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.model.BorrowerType;

/** Loans of resources to students (table prestamos_estudiante). */
// SOLID: LSP – can be used anywhere a LoanDao is expected, same behavior as the teacher version
public class StudentLoanDaoJdbc extends AbstractLoanDaoJdbc {

    public StudentLoanDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    protected String table() {
        return "prestamos_estudiante";
    }

    @Override
    protected String borrowerColumn() {
        return "id_estudiante";
    }

    @Override
    public BorrowerType getBorrowerType() {
        return BorrowerType.STUDENT;
    }
}
