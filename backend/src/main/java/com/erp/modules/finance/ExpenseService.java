package com.erp.modules.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.erp.security.AccessService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Profile("!foundation")
class ExpenseService {
    private final ExpenseRepository expenses;
    private final ExpenseCategoryRepository categories;
    private final AccessService access;
    private final JdbcTemplate jdbc;
    ExpenseService(ExpenseRepository expenses,ExpenseCategoryRepository categories,AccessService access,JdbcTemplate jdbc) {
        this.expenses=expenses; this.categories=categories; this.access=access; this.jdbc=jdbc;
    }
    @Transactional(readOnly=true)
    List<ExpenseCategory> categories(Jwt jwt,UUID branchId) {
        access.require(jwt,"EXPENSE_VIEW",branchId); return categories.findAll();
    }
    @Transactional
    ExpenseCategory createCategory(Jwt jwt,String name) {
        access.require(jwt,"EXPENSE_APPROVE",null);
        if (categories.existsByNameIgnoreCase(name)) throw new IllegalArgumentException("Expense category exists");
        return categories.save(new ExpenseCategory(name));
    }
    @Transactional(readOnly=true)
    Page<Expense> list(Jwt jwt,UUID branchId,Pageable pageable) {
        access.require(jwt,"EXPENSE_VIEW",branchId); return expenses.findByBranchIdOrderByCreatedAtDesc(branchId,pageable);
    }
    @Transactional
    Expense create(Jwt jwt,UUID branchId,UUID categoryId,String description,BigDecimal amount,String currency) {
        access.require(jwt,"EXPENSE_CREATE",branchId);
        if (amount==null || amount.signum()<=0 || amount.scale()>2 || amount.precision()>19)
            throw new IllegalArgumentException("Invalid expense amount");
        ExpenseCategory category=categories.findById(categoryId).orElseThrow(() -> new EntityNotFoundException("Expense category not found"));
        if (!"ACTIVE".equals(category.status)) throw new IllegalArgumentException("Expense category is inactive");
        Expense expense=expenses.save(new Expense(branchId,categoryId,description,amount,currency,access.userId(jwt)));
        audit(jwt,branchId,"EXPENSE_CREATED",expense.id); return expense;
    }
    @Transactional
    Expense submit(Jwt jwt,UUID branchId,UUID id) {
        access.require(jwt,"EXPENSE_CREATE",branchId);
        Expense expense=loadForBranch(branchId,id);
        if (!expense.createdBy.equals(access.userId(jwt))) throw new AccessDeniedException("Only creator can submit expense");
        expense.status=ExpenseWorkflow.submit(expense.status); expense.updatedAt=Instant.now();
        audit(jwt,branchId,"EXPENSE_SUBMITTED",id); return expense;
    }
    @Transactional
    Expense decide(Jwt jwt,UUID branchId,UUID id,boolean approve) {
        access.require(jwt,"EXPENSE_APPROVE",branchId);
        Expense expense=loadForBranch(branchId,id);
        if (expense.createdBy.equals(access.userId(jwt))) throw new AccessDeniedException("Creator cannot approve own expense");
        expense.status=ExpenseWorkflow.decide(expense.status,approve);
        expense.approvedBy=access.userId(jwt); expense.updatedAt=Instant.now();
        audit(jwt,branchId,approve ? "EXPENSE_APPROVED" : "EXPENSE_REJECTED",id); return expense;
    }
    @Transactional(readOnly=true)
    BigDecimal approvedTotal(Jwt jwt,UUID branchId,String currency) {
        access.require(jwt,"EXPENSE_VIEW",branchId); return expenses.approvedTotal(branchId,currency,ExpenseStatus.APPROVED);
    }
    private Expense loadForBranch(UUID branchId,UUID id) {
        Expense expense=expenses.findById(id).orElseThrow(() -> new EntityNotFoundException("Expense not found"));
        if (!expense.branchId.equals(branchId)) throw new EntityNotFoundException("Expense not found");
        return expense;
    }
    private void audit(Jwt jwt,UUID branchId,String action,UUID id) {
        jdbc.update("INSERT INTO audit_log(actor_user_id,branch_id,action,resource_type,resource_id,outcome) VALUES (?,?,?,'EXPENSE',?,'SUCCESS')",
                access.userId(jwt),branchId,action,id.toString());
    }
}
