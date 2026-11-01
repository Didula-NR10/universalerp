# Authorization

Role permissions are stored in PostgreSQL. Global `SUPER_ADMIN`, `COMPANY_ADMIN` and `AUDITOR` assignments can apply company-wide. Branch roles require both a matching `user_role` and `user_branch` assignment to an active branch. Application services check a requested branch before branch-sensitive reads and writes; a supplied branch ID alone is never proof of access.

The first admin is bootstrapped with [setup](../development/setup.md#first-administrator). Later admins can link Auth users and assign roles. A company administrator cannot grant `SUPER_ADMIN` without already having that role. Integration tests for cross-branch access are still required; see [testing](../development/testing.md).
