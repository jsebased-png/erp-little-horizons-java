package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ResourceCategoryDao;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.NotFoundException;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.ResourceStatus;
import com.littlehorizons.erp.util.Validator;

import java.util.List;

/**
 * Business rules for the resource catalog (books, lab equipment, sports equipment, devices).
 * Rules: unique code; a LOANED resource can only change status through a loan return, never by hand.
 */
// SOLID: SRP – resource catalog only; loans are in LoanController
public class ResourceController {

    private final ResourceDao resourceDao;
    private final ResourceCategoryDao categoryDao;

    public ResourceController(ResourceDao resourceDao, ResourceCategoryDao categoryDao) {
        this.resourceDao = resourceDao;
        this.categoryDao = categoryDao;
    }

    public Resource createResource(Resource resource) {
        normalize(resource);
        resourceDao.findByCode(resource.getCode()).ifPresent(existing -> {
            throw new BusinessRuleException("A resource with code " + resource.getCode() + " already exists.");
        });
        resource.setStatus(ResourceStatus.AVAILABLE);
        resource.setId(resourceDao.insert(resource));
        return resource;
    }

    public void updateResource(Resource resource) {
        Resource current = findResource(resource.getId());
        normalize(resource);
        resourceDao.findByCode(resource.getCode()).ifPresent(other -> {
            if (other.getId() != resource.getId()) {
                throw new BusinessRuleException("A resource with code " + resource.getCode() + " already exists.");
            }
        });
        resource.setStatus(current.getStatus()); // the status is not edited here
        resourceDao.update(resource);
    }

    /** Manual status change (maintenance, discarded, back to available). Loaned resources are protected. */
    public void changeStatus(int resourceId, ResourceStatus newStatus) {
        Resource resource = findResource(resourceId);
        Validator.requireNotNull("Status", newStatus);
        if (resource.getStatus() == ResourceStatus.LOANED) {
            throw new BusinessRuleException("The resource is on loan. Register the return first.");
        }
        if (newStatus == ResourceStatus.LOANED) {
            throw new BusinessRuleException("A resource becomes LOANED only by registering a loan.");
        }
        resourceDao.updateStatus(resourceId, newStatus);
    }

    public Resource findResource(int resourceId) {
        return resourceDao.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found: id " + resourceId));
    }

    public Resource findByCode(String code) {
        return resourceDao.findByCode(Validator.requireText("Resource code", code, 50).toUpperCase())
                .orElseThrow(() -> new NotFoundException("No resource with code " + code));
    }

    public List<Resource> listResources() {
        return resourceDao.findAll();
    }

    public List<Resource> listByStatus(ResourceStatus status) {
        return resourceDao.findByStatus(Validator.requireNotNull("Status", status));
    }

    public List<Resource> listByCategory(int categoryId) {
        return resourceDao.findByCategory(categoryId);
    }

    public List<ResourceCategory> listCategories() {
        return categoryDao.findAll();
    }

    private void normalize(Resource r) {
        r.setCode(Validator.requireText("Resource code", r.getCode(), 50).toUpperCase());
        r.setName(Validator.requireText("Resource name", r.getName(), 150));
        r.setDescription(Validator.optionalText("Description", r.getDescription(), 1000));
        r.setLocation(Validator.requireText("Location", r.getLocation(), 100));
        if (r.getAcquisitionDate() != null) {
            Validator.requireNotFuture("Acquisition date", r.getAcquisitionDate());
        }
        categoryDao.findById(r.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Resource category not found: id " + r.getCategoryId()));
    }
}
