package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.PaymentConcept;

import java.util.List;
import java.util.Optional;

/** Data access contract for payment concepts (tuition, uniforms, etc.). */
public interface PaymentConceptDao extends ReadableDao<PaymentConcept> {

    int insert(PaymentConcept concept);

    boolean update(PaymentConcept concept);

    boolean setActive(int conceptId, boolean active);

    Optional<PaymentConcept> findByCode(String code);

    List<PaymentConcept> findActive();
}
