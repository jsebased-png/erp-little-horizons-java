package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.model.BorrowerType;

/**
 * Creates the right LoanDao for a borrower type.
 * It exists so controllers never decide which loan table to use with if/else:
 * they ask the factory and receive a LoanDao.
 */
// Design pattern: Factory – hides which concrete LoanDao (student or teacher table) is created
public class LoanDaoFactory {

    private final DatabaseConnection database;

    public LoanDaoFactory(DatabaseConnection database) {
        this.database = database;
    }

    public LoanDao create(BorrowerType type) {
        switch (type) {
            case STUDENT:
                return new StudentLoanDaoJdbc(database);
            case TEACHER:
                return new TeacherLoanDaoJdbc(database);
            default:
                throw new IllegalArgumentException("Unsupported borrower type: " + type);
        }
    }
}
