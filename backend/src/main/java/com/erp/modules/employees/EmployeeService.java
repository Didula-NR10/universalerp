package com.erp.modules.employees;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.erp.security.AccessService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Profile("!foundation")
class EmployeeService {
    record EmployeeView(UUID id,UUID branchId,UUID departmentId,UUID userId,String employeeNumber,String fullName,String email,String jobPosition,String status) {}
    record AttendanceView(UUID id,UUID employeeId,LocalDate workDate,Instant checkIn,Instant checkOut) {}
    record LeaveView(UUID id,UUID employeeId,LocalDate startDate,LocalDate endDate,String reason,String status,UUID requestedBy) {}
    private final JdbcTemplate jdbc;
    private final AccessService access;
    EmployeeService(JdbcTemplate jdbc,AccessService access) { this.jdbc=jdbc;this.access=access; }
    private EmployeeView mapEmployee(ResultSet rs,int row) throws SQLException {
        return new EmployeeView(rs.getObject("id",UUID.class),rs.getObject("branch_id",UUID.class),
                rs.getObject("department_id",UUID.class),rs.getObject("user_id",UUID.class),
                rs.getString("employee_number"),rs.getString("full_name"),rs.getString("email"),
                rs.getString("job_position"),rs.getString("status"));
    }
    @Transactional(readOnly=true)
    List<EmployeeView> list(Jwt jwt,UUID branchId,String search,int limit,int offset) {
        access.require(jwt,"EMPLOYEE_VIEW",branchId);
        return jdbc.query("SELECT * FROM employee WHERE branch_id=? AND (lower(full_name) LIKE lower(?) OR lower(employee_number) LIKE lower(?)) ORDER BY full_name LIMIT ? OFFSET ?",
                this::mapEmployee,branchId,"%"+search+"%","%"+search+"%",limit,offset);
    }
    @Transactional
    EmployeeView create(Jwt jwt,UUID branchId,String employeeNumber,String fullName,String email,String position,UUID userId) {
        access.require(jwt,"EMPLOYEE_MANAGE",branchId);
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO employee(id,branch_id,user_id,employee_number,full_name,email,job_position) VALUES (?,?,?,?,?,?,?)",
                id,branchId,userId,employeeNumber.trim().toUpperCase(),fullName.trim(),email,position.trim());
        audit(jwt,branchId,"EMPLOYEE_CREATED",id);
        return employeeForBranch(id,branchId,false);
    }
    @Transactional
    EmployeeView update(Jwt jwt,UUID branchId,UUID id,String fullName,String email,String position,String status) {
        access.require(jwt,"EMPLOYEE_MANAGE",branchId);
        employeeForBranch(id,branchId,true);
        jdbc.update("UPDATE employee SET full_name=?,email=?,job_position=?,status=?,updated_at=now(),version=version+1 WHERE id=?",
                fullName.trim(),email,position.trim(),status,id);
        audit(jwt,branchId,"EMPLOYEE_UPDATED",id);
        return employeeForBranch(id,branchId,false);
    }
    @Transactional
    AttendanceView checkIn(Jwt jwt,UUID branchId,UUID employeeId,LocalDate date) {
        access.require(jwt,"ATTENDANCE_MANAGE",branchId);employeeForBranch(employeeId,branchId,false);
        UUID id=UUID.randomUUID();Instant now=Instant.now();
        jdbc.update("INSERT INTO attendance(id,employee_id,branch_id,work_date,check_in,recorded_by) VALUES (?,?,?,?,?,?)",
                id,employeeId,branchId,date,now,access.userId(jwt));
        audit(jwt,branchId,"ATTENDANCE_CHECK_IN",id);
        return new AttendanceView(id,employeeId,date,now,null);
    }
    @Transactional
    AttendanceView checkOut(Jwt jwt,UUID branchId,UUID employeeId,LocalDate date) {
        access.require(jwt,"ATTENDANCE_MANAGE",branchId);employeeForBranch(employeeId,branchId,false);
        List<AttendanceView> rows=jdbc.query("SELECT * FROM attendance WHERE employee_id=? AND branch_id=? AND work_date=? FOR UPDATE",
                (rs,row) -> new AttendanceView(rs.getObject("id",UUID.class),employeeId,date,
                        rs.getTimestamp("check_in").toInstant(),rs.getTimestamp("check_out")==null?null:rs.getTimestamp("check_out").toInstant()),
                employeeId,branchId,date);
        if (rows.isEmpty()) throw new EntityNotFoundException("Check-in not found");
        AttendanceView attendance=rows.get(0);
        if (attendance.checkOut()!=null) throw new IllegalArgumentException("Already checked out");
        Instant now=Instant.now();
        jdbc.update("UPDATE attendance SET check_out=? WHERE id=?",now,attendance.id());
        audit(jwt,branchId,"ATTENDANCE_CHECK_OUT",attendance.id());
        return new AttendanceView(attendance.id(),employeeId,date,attendance.checkIn(),now);
    }
    @Transactional
    LeaveView requestLeave(Jwt jwt,UUID branchId,UUID employeeId,LocalDate start,LocalDate end,String reason) {
        access.require(jwt,"LEAVE_REQUEST",branchId);LeavePolicy.validDates(start,end);
        EmployeeView employee=employeeForBranch(employeeId,branchId,false);
        if (!access.userId(jwt).equals(employee.userId())) throw new AccessDeniedException("Cannot request leave for another employee");
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO leave_request(id,employee_id,branch_id,start_date,end_date,reason,requested_by) VALUES (?,?,?,?,?,?,?)",
                id,employeeId,branchId,start,end,reason.trim(),access.userId(jwt));
        audit(jwt,branchId,"LEAVE_REQUESTED",id);
        return leaveForBranch(id,branchId,false);
    }
    @Transactional(readOnly=true)
    List<LeaveView> leaves(Jwt jwt,UUID branchId,int limit,int offset) {
        access.require(jwt,"EMPLOYEE_VIEW",branchId);
        return jdbc.query("SELECT * FROM leave_request WHERE branch_id=? ORDER BY created_at DESC LIMIT ? OFFSET ?",this::mapLeave,branchId,limit,offset);
    }
    @Transactional
    LeaveView decide(Jwt jwt,UUID branchId,UUID id,boolean approve) {
        access.require(jwt,"LEAVE_APPROVE",branchId);
        LeaveView leave=leaveForBranch(id,branchId,true);
        if (leave.requestedBy().equals(access.userId(jwt))) throw new AccessDeniedException("Cannot decide own leave");
        String status=LeavePolicy.decide(leave.status(),approve);
        if (approve) {
            employeeForBranch(leave.employeeId(),branchId,true);
            Integer overlap=jdbc.queryForObject("""
                    SELECT count(*) FROM leave_request WHERE employee_id=? AND status='APPROVED'
                    AND start_date<=? AND end_date>=?
                    """,Integer.class,leave.employeeId(),leave.endDate(),leave.startDate());
            if (overlap!=null && overlap>0) throw new IllegalArgumentException("Approved leave overlaps these dates");
        }
        jdbc.update("UPDATE leave_request SET status=?,decided_by=?,decided_at=now(),version=version+1 WHERE id=?",
                status,access.userId(jwt),id);
        audit(jwt,branchId,approve?"LEAVE_APPROVED":"LEAVE_REJECTED",id);
        return leaveForBranch(id,branchId,false);
    }
    private EmployeeView employeeForBranch(UUID id,UUID branchId,boolean lock) {
        List<EmployeeView> rows=jdbc.query("SELECT * FROM employee WHERE id=? AND branch_id=?"+(lock?" FOR UPDATE":""),this::mapEmployee,id,branchId);
        if (rows.isEmpty()) throw new EntityNotFoundException("Employee not found");return rows.get(0);
    }
    private LeaveView mapLeave(ResultSet rs,int row) throws SQLException {
        return new LeaveView(rs.getObject("id",UUID.class),rs.getObject("employee_id",UUID.class),
                rs.getDate("start_date").toLocalDate(),rs.getDate("end_date").toLocalDate(),
                rs.getString("reason"),rs.getString("status"),rs.getObject("requested_by",UUID.class));
    }
    private LeaveView leaveForBranch(UUID id,UUID branchId,boolean lock) {
        List<LeaveView> rows=jdbc.query("SELECT * FROM leave_request WHERE id=? AND branch_id=?"+(lock?" FOR UPDATE":""),this::mapLeave,id,branchId);
        if (rows.isEmpty()) throw new EntityNotFoundException("Leave request not found");return rows.get(0);
    }
    private void audit(Jwt jwt,UUID branchId,String action,UUID id) {
        jdbc.update("INSERT INTO audit_log(actor_user_id,branch_id,action,resource_type,resource_id,outcome) VALUES (?,?,?,'EMPLOYEE',?,'SUCCESS')",
                access.userId(jwt),branchId,action,id.toString());
    }
}
