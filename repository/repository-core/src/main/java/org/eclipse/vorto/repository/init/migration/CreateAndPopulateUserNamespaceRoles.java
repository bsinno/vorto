/**
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.vorto.repository.init.migration;

import java.util.BitSet;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.rowset.SqlRowSet;

public class CreateAndPopulateUserNamespaceRoles extends MigrationTask {

  public CreateAndPopulateUserNamespaceRoles(MigrationTaskChain chain) {
    super(chain);
  }

  @Override
  public boolean verify() {
    boolean applicable;
    // checks table actually exists - hardly ever useful unless hibernate props are changed, since
    // tables should be automatically created based on entities otherwise
    boolean userNamespaceRolesTableExists = template()
        .queryForObject(
            "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_NAME = 'user_namespace_roles'",
            Integer.class).equals(1);
    // saves for run phase
    flags().put("userNamespaceRolesTableExists", userNamespaceRolesTableExists);

    // does not exist - task applicable
    if (!userNamespaceRolesTableExists) {
      applicable = true;
      logger().info(
          String.format(
              "%s task found no user_namespace_roles table. Task applicable? %b ",
              getClass().getSimpleName(),
              applicable
          )
      );
    }
    // table exists
    else {
      // verifying table has values
      int userNamespaceRolesCount = template()
          .queryForObject("SELECT COUNT(*) FROM user_namespace_roles", Integer.class);
      // saves for run phase
      flags().put("userNamespaceRolesEmpty", userNamespaceRolesCount == (0));

      // table is empty
      if (userNamespaceRolesCount == 0) {
        /*
        Some heuristic explanation in logging: if the legacy "user_role", "tenant" and "tenant_user"
        tables exist, we can assume the user_namespace_roles table has been automatically created
        through the annotated entity, but needs migrating data from user_role etc. - therefore a
        migration is applicable.
        Conversely if those tables don't exist, this is likely just a new/empty Vorto repository,
        which means it makes no sense to enter the run phase here.
        Another edge condition would be if not all legacy tables exist, then the migration is
        applicable but NOT feasible, because it needs all tables.
        In that case we should fail and shutdown the repository with appropriate logging.
        */
        BitSet legacyTablesCondition = new BitSet();
        boolean userRoleTableExists =
            template()
                .queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_NAME = 'user_role'",
                    Integer.class
                )
                .equals(1);
        legacyTablesCondition.set(0, userRoleTableExists);
        boolean tenantTableExists =
            template()
                .queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_NAME = 'tenant'",
                    Integer.class
                )
                .equals(1);
        legacyTablesCondition.set(1, tenantTableExists);
        boolean tenantUserTableExists =
            template()
                .queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_NAME = 'tenant_user'",
                    Integer.class
                )
                .equals(1);
        legacyTablesCondition.set(2, tenantUserTableExists);
        // if all conditions are true, migration is required
        // if some are true and some false, migration must fail
        // if all are false, migration is not applicable
        BitSet allConditionsTrue = new BitSet(3);
        allConditionsTrue.flip(0, 3);
        BitSet allConditionsFalse = new BitSet(3);
        // some legacy tables exist, some don't - we can only fail
        if (!legacyTablesCondition.equals(allConditionsTrue) && !legacyTablesCondition
            .equals(allConditionsFalse)) {
          logger().warn(
              String.format(
                  "The user_namespace_roles table is empty, and some, but not all of the legacy tables required for data migration are present (user_role: %b; tenant: %b; tenant_user: %b). Therefore the migration cannot succeed. The repository should not start.",
                  userRoleTableExists,
                  tenantTableExists,
                  tenantUserTableExists
                  )
          );
          fail();
          return false;
        }
        // setting applicable as either al legacy tables exist --> , or none --> false
        applicable = userRoleTableExists && tenantTableExists && tenantUserTableExists;
        // save for run phase
        flags().put("legacyTablesExist", applicable);

        String reasonWhyTableEmpty = applicable ?
            "This is likely because a migration from legacy tables is necessary."
            :
                "This is likely because the Vorto repository has no user/namespace/role data yet.";

        logger().info(
            String.format(
                "%s task found table user_namespace_roles empty. %s Applicable? %b",
                getClass().getSimpleName(),
                reasonWhyTableEmpty,
                applicable
            )
        );
      }
      // table has data - nothing to do
      else {
        applicable = false;
        logger().info(
            String.format(
                "%s task found table user_namespace_roles has %d rows. Applicable? %b",
                getClass().getSimpleName(),
                userNamespaceRolesCount,
                applicable
            )
        );
      }

    }
    return applicable;
  }

  @Override
  public boolean run() {
    // assuming no exception means create / insert successful
    try {
      // assuming key always present as saved in verify phase
      if (!flags().get("userNamespaceRolesTableExists")) {
        template().execute(
            "CREATE TABLE user_namespace_roles(user_id BIGINT NOT NULL, namespace_id BIGINT NOT NULL, roles BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (user_id, namespace_id), FOREIGN KEY (user_id) REFERENCES user (id), FOREIGN KEY (namespace_id) REFERENCES namespace (id));"
        );
      }
      /*
      If the "legacyTablesExist" key is present in the map and its value is positive
      (meaning legacy table exists) AND user_namespace_roles table is empty (similarly,
      "userNamespaceRolesEmpty" key present and value positive), then the migration from legacy
      user_role to user_namespace_roles should be started.
      */
      if (flag("legacyTablesExist") &&flag("userNamespaceRolesEmpty")) {
        // sets limit to further queries
        int maxLimit = template().queryForObject("select count(id) from user_role", Integer.class);
        //
        SqlRowSet rowSet = template().queryForRowSet(
          "select tu.user_id, n.id  from user_role inner join tenant_user tu on tenant_user_id = tu.id  inner join tenant t on tu.tenant_id = t.id  inner join user u on tu.user_id = u.id inner join namespace n on tu.tenant_id = n.tenant_id where role != 'SYS_ADMIN' limit ?",
            maxLimit
        );
        // TODO pre_populate_user_namespace_roles and populate_user_namespace_roles
        while (rowSet.next()) {

        }
      }

      return true;
    } catch (DataAccessException dae) {
      logger().warn(
          String.format("%s task failed to run with exception", getClass().getSimpleName()),
          dae
      );
      return false;
    }
  }

  @Override
  public void onFailure() {
    chain().shutDown(-1);
  }

  @Override
  public String getDescription() {
    return "Creates the user_namespace_roles table if absent / populates it if empty from legacy user_roles table if present or fails.";
  }
}
