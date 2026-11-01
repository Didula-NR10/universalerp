package com.erp.modules.notifications;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.erp.security.AccessService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Profile("!foundation")
class NotificationService implements NotificationPublisher {
    record NotificationView(UUID id,UUID branchId,String type,String title,String message,Instant createdAt,Instant readAt) {}
    private final JdbcTemplate jdbc;
    private final AccessService access;
    NotificationService(JdbcTemplate jdbc,AccessService access) { this.jdbc=jdbc;this.access=access; }
    @Override @Transactional
    public void lowStock(UUID branchId,UUID warehouseId,UUID productId,String productName,BigDecimal quantity,UUID movementId) {
        String eventKey="LOW_STOCK:"+movementId;
        jdbc.update("""
                INSERT INTO notification(id,user_id,branch_id,event_key,notification_type,title,message)
                SELECT gen_random_uuid(),u.auth_user_id,?,?,'LOW_STOCK','Low stock',?
                FROM user_profile u JOIN user_branch ub ON ub.user_id=u.auth_user_id
                JOIN user_role ur ON ur.user_id=u.auth_user_id AND ur.branch_id=ub.branch_id
                JOIN erp_role r ON r.id=ur.role_id
                WHERE u.status='ACTIVE' AND ub.branch_id=? AND r.code IN ('BRANCH_MANAGER','INVENTORY_MANAGER')
                AND NOT EXISTS(SELECT 1 FROM notification_preference p WHERE p.user_id=u.auth_user_id
                    AND p.notification_type='LOW_STOCK' AND p.enabled=false)
                GROUP BY u.auth_user_id ON CONFLICT (user_id,event_key) DO NOTHING
                """,branchId,eventKey,productName+" is below its reorder level ("+quantity+") in warehouse "+warehouseId,branchId);
    }
    @Transactional(readOnly=true)
    List<NotificationView> list(Jwt jwt,int limit) {
        UUID userId=access.userId(jwt);
        return jdbc.query("""
                SELECT n.* FROM notification n JOIN user_profile u ON u.auth_user_id=n.user_id
                WHERE n.user_id=? AND u.status='ACTIVE' ORDER BY n.created_at DESC LIMIT ?
                """,(rs,row)->new NotificationView(rs.getObject("id",UUID.class),rs.getObject("branch_id",UUID.class),
                rs.getString("notification_type"),rs.getString("title"),rs.getString("message"),
                rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("read_at")==null?null:rs.getTimestamp("read_at").toInstant()),userId,limit);
    }
    @Transactional
    void markRead(Jwt jwt,UUID id) {
        UUID userId=access.userId(jwt);
        int updated=jdbc.update("""
                UPDATE notification SET read_at=coalesce(read_at,now()) WHERE id=? AND user_id=?
                AND EXISTS(SELECT 1 FROM user_profile WHERE auth_user_id=? AND status='ACTIVE')
                """,id,userId,userId);
        if (updated==0) throw new EntityNotFoundException("Notification not found");
    }
    @Transactional
    void preference(Jwt jwt,String type,boolean enabled) {
        UUID userId=access.userId(jwt);
        Integer active=jdbc.queryForObject("SELECT count(*) FROM user_profile WHERE auth_user_id=? AND status='ACTIVE'",Integer.class,userId);
        if (active==null||active==0) throw new EntityNotFoundException("ERP user profile not found");
        jdbc.update("""
                INSERT INTO notification_preference(user_id,notification_type,enabled) VALUES (?,?,?)
                ON CONFLICT (user_id,notification_type) DO UPDATE SET enabled=excluded.enabled
                """,userId,type,enabled);
    }
}
