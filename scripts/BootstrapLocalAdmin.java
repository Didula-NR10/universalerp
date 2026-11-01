import java.sql.*;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BootstrapLocalAdmin {
    public static void main(String[] args) throws Exception {
        if (args.length!=2) throw new IllegalArgumentException("Email and display name required");
        String email=args[0].trim().toLowerCase(),name=args[1].trim();
        String password=System.getenv("ERP_INITIAL_PASSWORD");
        if (email.isBlank() || name.isBlank() || password==null || password.length()<12 || password.length()>128)
            throw new IllegalArgumentException("Email, display name and a 12-128 character temporary password required");
        String hash=new BCryptPasswordEncoder(12).encode(password);
        try (Connection connection=DriverManager.getConnection(
                System.getenv("DB_URL"),System.getenv("DB_USERNAME"),System.getenv("DB_PASSWORD"))) {
            connection.setAutoCommit(false);
            try {
                try (Statement lock=connection.createStatement()) {
                    lock.execute("LOCK TABLE local_credential IN EXCLUSIVE MODE");
                }
                try (Statement check=connection.createStatement();
                     ResultSet rows=check.executeQuery("SELECT count(*) FROM local_credential")) {
                    rows.next();
                    if (rows.getLong(1)!=0) throw new IllegalStateException("Local accounts already exist; first-admin bootstrap is disabled");
                }
                UUID userId=null;
                try (PreparedStatement find=connection.prepareStatement("SELECT auth_user_id FROM user_profile WHERE lower(email)=?")) {
                    find.setString(1,email);
                    try (ResultSet rows=find.executeQuery()) { if (rows.next()) userId=rows.getObject(1,UUID.class); }
                }
                if (userId==null) {
                    userId=UUID.randomUUID();
                    try (PreparedStatement insert=connection.prepareStatement(
                            "INSERT INTO user_profile(auth_user_id,email,display_name,status) VALUES (?,?,?,'ACTIVE')")) {
                        insert.setObject(1,userId);insert.setString(2,email);insert.setString(3,name);insert.executeUpdate();
                    }
                }
                try (PreparedStatement role=connection.prepareStatement("""
                        INSERT INTO user_role(user_id,role_id,branch_id)
                        SELECT ?,id,NULL FROM erp_role WHERE code='SUPER_ADMIN' ON CONFLICT DO NOTHING
                        """)) {
                    role.setObject(1,userId);
                    if (role.executeUpdate()==0) {
                        try (PreparedStatement verify=connection.prepareStatement("""
                                SELECT 1 FROM user_role ur JOIN erp_role r ON r.id=ur.role_id
                                WHERE ur.user_id=? AND r.code='SUPER_ADMIN' AND ur.branch_id IS NULL
                                """)) {
                            verify.setObject(1,userId);
                            try (ResultSet rows=verify.executeQuery()) {
                                if (!rows.next()) throw new IllegalStateException("SUPER_ADMIN role is missing");
                            }
                        }
                    }
                }
                try (PreparedStatement insert=connection.prepareStatement(
                        "INSERT INTO local_credential(user_id,password_hash,must_change_password) VALUES (?,?,true)")) {
                    insert.setObject(1,userId);insert.setString(2,hash);insert.executeUpdate();
                }
                connection.commit();
                System.out.println("First ERP administrator created. Change the temporary password at first sign-in.");
            } catch (Exception ex) { connection.rollback();throw ex; }
        }
    }
}
