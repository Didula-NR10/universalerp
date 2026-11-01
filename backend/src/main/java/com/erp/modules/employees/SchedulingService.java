package com.erp.modules.employees;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
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
class SchedulingService {
    record Template(UUID id,UUID branchId,String code,String name,LocalTime startTime,LocalTime endTime,int breakMinutes,String status) {}
    record Availability(UUID id,UUID employeeId,int weekday,LocalTime startTime,LocalTime endTime) {}
    record Shift(UUID id,UUID branchId,UUID employeeId,UUID templateId,Instant startAt,Instant endAt,
                 String status,UUID createdBy,UUID approvedBy) {}
    private final JdbcTemplate jdbc;
    private final AccessService access;
    SchedulingService(JdbcTemplate jdbc,AccessService access) { this.jdbc=jdbc;this.access=access; }

    @Transactional(readOnly=true)
    List<Template> templates(Jwt jwt,UUID branchId) {
        access.require(jwt,"SCHEDULE_VIEW",branchId);
        return jdbc.query("SELECT * FROM shift_template WHERE branch_id=? ORDER BY name",this::mapTemplate,branchId);
    }
    @Transactional
    Template createTemplate(Jwt jwt,UUID branchId,String code,String name,LocalTime start,LocalTime end,int breakMinutes) {
        access.require(jwt,"SCHEDULE_MANAGE",branchId);
        if (start.equals(end) || breakMinutes<0 || breakMinutes>480) throw new IllegalArgumentException("Invalid shift template time or break");
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO shift_template(id,branch_id,code,name,start_time,end_time,break_minutes) VALUES (?,?,?,?,?,?,?)",
                id,branchId,code.trim().toUpperCase(),name.trim(),start,end,breakMinutes);
        audit(jwt,branchId,"SHIFT_TEMPLATE_CREATED",id);
        return jdbc.queryForObject("SELECT * FROM shift_template WHERE id=?",this::mapTemplate,id);
    }
    @Transactional(readOnly=true)
    List<Availability> availability(Jwt jwt,UUID branchId,UUID employeeId) {
        try { access.require(jwt,"SCHEDULE_VIEW",branchId); }
        catch (AccessDeniedException denied) {
            access.require(jwt,"SCHEDULE_SELF_VIEW",branchId);
            if (!access.userId(jwt).equals(employee(employeeId,branchId,false))) throw new AccessDeniedException("Own availability only");
        }
        employee(employeeId,branchId,false);
        return jdbc.query("SELECT * FROM employee_availability WHERE employee_id=? AND branch_id=? ORDER BY weekday,start_time",
                this::mapAvailability,employeeId,branchId);
    }
    @Transactional(readOnly=true)
    UUID ownEmployeeId(Jwt jwt,UUID branchId) {
        access.require(jwt,"SCHEDULE_SELF_VIEW",branchId);
        List<UUID> rows=jdbc.query("SELECT id FROM employee WHERE branch_id=? AND user_id=? AND status='ACTIVE'",
                (rs,row)->rs.getObject(1,UUID.class),branchId,access.userId(jwt));
        if (rows.isEmpty()) throw new EntityNotFoundException("Own employee record not found");
        return rows.get(0);
    }
    @Transactional
    Availability addAvailability(Jwt jwt,UUID branchId,UUID employeeId,int weekday,LocalTime start,LocalTime end) {
        access.require(jwt,"SCHEDULE_AVAILABILITY",branchId);
        UUID ownUser=employee(employeeId,branchId,true);
        if (!access.userId(jwt).equals(ownUser)) {
            access.require(jwt,"SCHEDULE_MANAGE",branchId);
        }
        if (weekday<1 || weekday>7 || !end.isAfter(start)) throw new IllegalArgumentException("Availability must be within one day");
        Integer overlaps=jdbc.queryForObject("""
                SELECT count(*) FROM employee_availability WHERE employee_id=? AND weekday=?
                AND start_time<? AND end_time>?
                """,Integer.class,employeeId,weekday,end,start);
        if (overlaps!=null && overlaps>0) throw new IllegalArgumentException("Availability overlaps an existing window");
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO employee_availability(id,employee_id,branch_id,weekday,start_time,end_time) VALUES (?,?,?,?,?,?)",
                id,employeeId,branchId,weekday,start,end);
        audit(jwt,branchId,"AVAILABILITY_ADDED",id);
        return jdbc.queryForObject("SELECT * FROM employee_availability WHERE id=?",this::mapAvailability,id);
    }
    @Transactional(readOnly=true)
    List<Shift> shifts(Jwt jwt,UUID branchId,Instant from,Instant to) {
        UUID ownEmployee=null;
        try { access.require(jwt,"SCHEDULE_VIEW",branchId); }
        catch (AccessDeniedException denied) { ownEmployee=ownEmployeeId(jwt,branchId); }
        if (!to.isAfter(from) || Duration.between(from,to).toDays()>90) throw new IllegalArgumentException("Invalid schedule range");
        if (ownEmployee!=null) return jdbc.query("SELECT * FROM work_shift WHERE branch_id=? AND employee_id=? AND start_at<? AND end_at>? ORDER BY start_at",
                this::mapShift,branchId,ownEmployee,java.sql.Timestamp.from(to),java.sql.Timestamp.from(from));
        return jdbc.query("SELECT * FROM work_shift WHERE branch_id=? AND start_at<? AND end_at>? ORDER BY start_at",
                this::mapShift,branchId,java.sql.Timestamp.from(to),java.sql.Timestamp.from(from));
    }
    @Transactional
    Shift createShift(Jwt jwt,UUID branchId,UUID employeeId,UUID templateId,Instant start,Instant end) {
        access.require(jwt,"SCHEDULE_MANAGE",branchId);
        if (start==null || end==null || !end.isAfter(start) || Duration.between(start,end).compareTo(Duration.ofHours(24))>0)
            throw new IllegalArgumentException("Shift duration must be greater than zero and at most 24 hours");
        employee(employeeId,branchId,true); // Serializes competing assignments for this employee.
        if (templateId!=null) {
            Integer count=jdbc.queryForObject("SELECT count(*) FROM shift_template WHERE id=? AND branch_id=? AND status='ACTIVE'",
                    Integer.class,templateId,branchId);
            if (count==null || count==0) throw new EntityNotFoundException("Active shift template not found");
        }
        Integer overlaps=jdbc.queryForObject("""
                SELECT count(*) FROM work_shift WHERE employee_id=? AND status IN ('DRAFT','APPROVED')
                AND start_at<? AND end_at>?
                """,Integer.class,employeeId,java.sql.Timestamp.from(end),java.sql.Timestamp.from(start));
        if (overlaps!=null && overlaps>0) throw new IllegalArgumentException("Employee already has a shift in this period");
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO work_shift(id,branch_id,employee_id,template_id,start_at,end_at,created_by) VALUES (?,?,?,?,?,?,?)",
                id,branchId,employeeId,templateId,java.sql.Timestamp.from(start),java.sql.Timestamp.from(end),access.userId(jwt));
        audit(jwt,branchId,"SHIFT_CREATED",id);
        return shift(id,branchId,false);
    }
    @Transactional
    Shift approve(Jwt jwt,UUID branchId,UUID id) {
        access.require(jwt,"SCHEDULE_APPROVE",branchId);
        Shift shift=shift(id,branchId,true);
        if (!"DRAFT".equals(shift.status())) throw new IllegalArgumentException("Only draft shifts can be approved");
        if (shift.createdBy().equals(access.userId(jwt))) throw new AccessDeniedException("Creator cannot approve own shift");
        jdbc.update("UPDATE work_shift SET status='APPROVED',approved_by=?,updated_at=now(),version=version+1 WHERE id=?",access.userId(jwt),id);
        audit(jwt,branchId,"SHIFT_APPROVED",id);
        return shift(id,branchId,false);
    }
    @Transactional
    Shift cancel(Jwt jwt,UUID branchId,UUID id) {
        access.require(jwt,"SCHEDULE_MANAGE",branchId);
        Shift shift=shift(id,branchId,true);
        if ("CANCELLED".equals(shift.status())) throw new IllegalArgumentException("Shift is already cancelled");
        jdbc.update("UPDATE work_shift SET status='CANCELLED',updated_at=now(),version=version+1 WHERE id=?",id);
        audit(jwt,branchId,"SHIFT_CANCELLED",id);
        return shift(id,branchId,false);
    }
    private UUID employee(UUID id,UUID branchId,boolean lock) {
        List<UUID> rows=jdbc.query("SELECT user_id FROM employee WHERE id=? AND branch_id=? AND status='ACTIVE'"+(lock?" FOR UPDATE":""),
                (rs,row)->rs.getObject(1,UUID.class),id,branchId);
        if (rows.isEmpty()) throw new EntityNotFoundException("Active employee not found");
        return rows.get(0);
    }
    private Shift shift(UUID id,UUID branchId,boolean lock) {
        List<Shift> rows=jdbc.query("SELECT * FROM work_shift WHERE id=? AND branch_id=?"+(lock?" FOR UPDATE":""),this::mapShift,id,branchId);
        if (rows.isEmpty()) throw new EntityNotFoundException("Shift not found");
        return rows.get(0);
    }
    private Shift mapShift(ResultSet rs,int row) throws SQLException {
        return new Shift(rs.getObject("id",UUID.class),rs.getObject("branch_id",UUID.class),rs.getObject("employee_id",UUID.class),
                rs.getObject("template_id",UUID.class),rs.getTimestamp("start_at").toInstant(),rs.getTimestamp("end_at").toInstant(),
                rs.getString("status"),rs.getObject("created_by",UUID.class),rs.getObject("approved_by",UUID.class));
    }
    private Template mapTemplate(ResultSet rs,int row) throws SQLException {
        return new Template(rs.getObject("id",UUID.class),rs.getObject("branch_id",UUID.class),rs.getString("code"),rs.getString("name"),
                rs.getTime("start_time").toLocalTime(),rs.getTime("end_time").toLocalTime(),rs.getInt("break_minutes"),rs.getString("status"));
    }
    private Availability mapAvailability(ResultSet rs,int row) throws SQLException {
        return new Availability(rs.getObject("id",UUID.class),rs.getObject("employee_id",UUID.class),rs.getInt("weekday"),
                rs.getTime("start_time").toLocalTime(),rs.getTime("end_time").toLocalTime());
    }
    private void audit(Jwt jwt,UUID branchId,String action,UUID id) {
        jdbc.update("INSERT INTO audit_log(actor_user_id,branch_id,action,resource_type,resource_id,outcome) VALUES (?,?,?,'WORK_SHIFT',?,'SUCCESS')",
                access.userId(jwt),branchId,action,id.toString());
    }
}
