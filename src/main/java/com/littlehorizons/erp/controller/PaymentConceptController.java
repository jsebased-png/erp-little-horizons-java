package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.NotFoundException;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.util.Validator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Business rules for the catalog of payment concepts (tuition, uniforms, trips...).
 * It exists apart from PaymentController so changing prices does not mix with registering payments.
 */
// SOLID: SRP – concept catalog only
public class PaymentConceptController {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99"); // DECIMAL(10,2)

    private final PaymentConceptDao conceptDao;

    public PaymentConceptController(PaymentConceptDao conceptDao) {
        this.conceptDao = conceptDao;
    }

    public PaymentConcept createConcept(PaymentConcept concept) {
        normalize(concept);
        conceptDao.findByCode(concept.getCode()).ifPresent(existing -> {
            throw new BusinessRuleException("Concept code " + concept.getCode() + " already exists.");
        });
        concept.setActive(true);
        concept.setId(conceptDao.insert(concept));
        return concept;
    }

    public void updateConcept(PaymentConcept concept) {
        findConcept(concept.getId());
        normalize(concept);
        conceptDao.findByCode(concept.getCode()).ifPresent(other -> {
            if (other.getId() != concept.getId()) {
                throw new BusinessRuleException("Concept code " + concept.getCode() + " already exists.");
            }
        });
        conceptDao.update(concept);
    }

    public void setActive(int conceptId, boolean active) {
        findConcept(conceptId);
        conceptDao.setActive(conceptId, active);
    }

    public PaymentConcept findConcept(int conceptId) {
        return conceptDao.findById(conceptId)
                .orElseThrow(() -> new NotFoundException("Payment concept not found: id " + conceptId));
    }

    public List<PaymentConcept> listConcepts() {
        return conceptDao.findAll();
    }

    private void normalize(PaymentConcept c) {
        c.setCode(Validator.requireText("Concept code", c.getCode(), 20).toUpperCase());
        c.setName(Validator.requireText("Concept name", c.getName(), 100));
        c.setDescription(Validator.optionalText("Description", c.getDescription(), 1000));
        Validator.requireNotNull("Base amount", c.getBaseAmount());
        if (c.getBaseAmount().signum() <= 0 || c.getBaseAmount().compareTo(MAX_AMOUNT) > 0) {
            throw new BusinessRuleException("Base amount must be greater than zero and fit in DECIMAL(10,2).");
        }
        c.setBaseAmount(c.getBaseAmount().setScale(2, RoundingMode.HALF_UP));
    }
}
