package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.model.BorrowerType;

/** Loans of resources to teachers (table prestamos_profesor). */
public class TeacherLoanDaoJdbc extends AbstractLoanDaoJdbc {

    public TeacherLoanDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    protected String table() {
        return "prestamos_profesor";
    }

    @Override
    protected String borrowerColumn() {
        return "id_profesor";
    }

    @Override
    public BorrowerType getBorrowerType() {
        return BorrowerType.TEACHER;
    }
}
