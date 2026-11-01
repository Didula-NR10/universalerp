package com.erp.modules.organization;

import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.erp.security.AccessService;
import jakarta.persistence.EntityNotFoundException;

@Service @Profile("!foundation")
class OrganizationService implements OrganizationDirectory {
    private final BranchRepository branches;
    private final WarehouseRepository warehouses;
    private final AccessService access;
    private final DepartmentRepository departments;
    private final JdbcTemplate jdbc;

    record CompanyView(String legalName,String displayName,String baseCurrency,String timezone) {}
    OrganizationService(BranchRepository branches, WarehouseRepository warehouses, DepartmentRepository departments,
                        AccessService access,JdbcTemplate jdbc) {
        this.branches = branches; this.warehouses = warehouses; this.departments=departments;
        this.access = access; this.jdbc=jdbc;
    }

    @Transactional(readOnly=true)
    CompanyView company(Jwt jwt) {
        access.require(jwt,"COMPANY_READ",null);
        List<CompanyView> rows=jdbc.query("SELECT * FROM company_profile WHERE id=1",(rs,row)->
                new CompanyView(rs.getString("legal_name"),rs.getString("display_name"),
                        rs.getString("base_currency").trim(),rs.getString("timezone")));
        if (rows.isEmpty()) throw new EntityNotFoundException("Company profile not configured");
        return rows.get(0);
    }

    @Transactional
    CompanyView saveCompany(Jwt jwt,String legalName,String displayName,String currency,String timezone) {
        access.require(jwt,"COMPANY_MANAGE",null);
        jdbc.update("""
                INSERT INTO company_profile(id,legal_name,display_name,base_currency,timezone) VALUES (1,?,?,?,?)
                ON CONFLICT (id) DO UPDATE SET legal_name=excluded.legal_name,display_name=excluded.display_name,
                base_currency=excluded.base_currency,timezone=excluded.timezone,updated_at=now()
                """,legalName.trim(),displayName.trim(),currency,timezone);
        audit(jwt,null,"COMPANY_UPDATED","COMPANY","1");
        return company(jwt);
    }

    @Transactional(readOnly = true)
    List<Branch> branches(Jwt jwt) { return branches.findAllById(access.visibleBranches(jwt)); }

    @Transactional
    Branch createBranch(Jwt jwt, String code, String name, String timezone) {
        access.require(jwt, "BRANCH_MANAGE", null);
        if (branches.existsByCodeIgnoreCase(code)) throw new IllegalArgumentException("Branch code already exists");
        Branch branch=branches.save(new Branch(code.trim().toUpperCase(), name.trim(), timezone));
        audit(jwt,branch.id,"BRANCH_CREATED","BRANCH",branch.id.toString());
        return branch;
    }

    @Transactional
    Branch updateBranch(Jwt jwt,UUID branchId,String name,String timezone,String status) {
        access.require(jwt,"BRANCH_MANAGE",null);
        Branch branch=branches.findById(branchId).orElseThrow(() -> new EntityNotFoundException("Branch not found"));
        branch.name=name.trim();branch.timezone=timezone;branch.status=status;branch.updatedAt=Instant.now();
        audit(jwt,branchId,"BRANCH_UPDATED","BRANCH",branchId.toString());
        return branch;
    }

    @Transactional(readOnly = true)
    List<Warehouse> warehouses(Jwt jwt, UUID branchId) {
        access.require(jwt, "WAREHOUSE_READ", branchId);
        return warehouses.findByBranchIdOrderByName(branchId);
    }

    @Transactional
    Warehouse createWarehouse(Jwt jwt, UUID branchId, String code, String name) {
        access.require(jwt, "WAREHOUSE_MANAGE", branchId);
        requireActiveBranch(branchId);
        if (warehouses.existsByCodeIgnoreCase(code)) throw new IllegalArgumentException("Warehouse code already exists");
        Warehouse warehouse=warehouses.save(new Warehouse(branchId, code.trim().toUpperCase(), name.trim()));
        audit(jwt,branchId,"WAREHOUSE_CREATED","WAREHOUSE",warehouse.id.toString());
        return warehouse;
    }

    @Transactional
    Warehouse updateWarehouse(Jwt jwt,UUID branchId,UUID id,String name,String status) {
        access.require(jwt,"WAREHOUSE_MANAGE",branchId);
        Warehouse warehouse=warehouses.findById(id).orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));
        if (!warehouse.branchId.equals(branchId)) throw new EntityNotFoundException("Warehouse not found");
        if (status.equals("INACTIVE")) {
            Integer stocked=jdbc.queryForObject("SELECT count(*) FROM stock_balance WHERE warehouse_id=? AND quantity>0",Integer.class,id);
            if (stocked!=null && stocked>0) throw new IllegalArgumentException("Warehouse has stock and cannot be deactivated");
        }
        warehouse.name=name.trim();warehouse.status=status;warehouse.updatedAt=Instant.now();
        audit(jwt,branchId,"WAREHOUSE_UPDATED","WAREHOUSE",id.toString());
        return warehouse;
    }

    @Transactional(readOnly=true)
    List<Department> departments(Jwt jwt,UUID branchId) {
        access.require(jwt,"DEPARTMENT_READ",branchId);
        return departments.findByBranchIdOrderByName(branchId);
    }

    @Transactional
    Department createDepartment(Jwt jwt,UUID branchId,String code,String name) {
        access.require(jwt,"DEPARTMENT_MANAGE",branchId);requireActiveBranch(branchId);
        if (departments.existsByBranchIdAndCodeIgnoreCase(branchId,code)) throw new IllegalArgumentException("Department code exists");
        Department department=departments.save(new Department(branchId,code,name));
        audit(jwt,branchId,"DEPARTMENT_CREATED","DEPARTMENT",department.id.toString());
        return department;
    }

    @Transactional
    Department updateDepartment(Jwt jwt,UUID branchId,UUID id,String name,String status) {
        access.require(jwt,"DEPARTMENT_MANAGE",branchId);
        Department department=departments.findById(id).orElseThrow(() -> new EntityNotFoundException("Department not found"));
        if (!branchId.equals(department.branchId)) throw new EntityNotFoundException("Department not found");
        if (status.equals("INACTIVE")) {
            Integer employees=jdbc.queryForObject("SELECT count(*) FROM employee WHERE department_id=? AND status='ACTIVE'",Integer.class,id);
            if (employees!=null && employees>0) throw new IllegalArgumentException("Department has active employees");
        }
        department.name=name.trim();department.status=status;department.updatedAt=Instant.now();
        audit(jwt,branchId,"DEPARTMENT_UPDATED","DEPARTMENT",id.toString());
        return department;
    }

    @Override @Transactional(readOnly = true)
    public UUID branchForWarehouse(UUID warehouseId) {
        Warehouse warehouse = warehouses.findById(warehouseId).orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));
        if (!"ACTIVE".equals(warehouse.status)) throw new IllegalArgumentException("Warehouse is inactive");
        requireActiveBranch(warehouse.branchId);
        return warehouse.branchId;
    }

    private void requireActiveBranch(UUID id) {
        Branch branch=branches.findById(id).orElseThrow(() -> new EntityNotFoundException("Branch not found"));
        if (!"ACTIVE".equals(branch.status)) throw new IllegalArgumentException("Branch is inactive");
    }

    private void audit(Jwt jwt,UUID branchId,String action,String resourceType,String resourceId) {
        jdbc.update("INSERT INTO audit_log(actor_user_id,branch_id,action,resource_type,resource_id,outcome) VALUES (?,?,?,?,?,'SUCCESS')",
                access.userId(jwt),branchId,action,resourceType,resourceId);
    }
}
