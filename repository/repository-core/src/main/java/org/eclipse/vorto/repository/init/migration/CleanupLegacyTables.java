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

import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

public class CleanupLegacyTables extends MigrationTask {

  public CleanupLegacyTables(MigrationTaskChain chain) {
    super(chain);
  }

  @Override
  public boolean verify() {
    boolean hasUserRoleTable = template()
        .queryForObject(
            "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_NAME = 'user_role'",
            Integer.class
        )
        .equals(1);
    boolean hasTenantTable = template()
        .queryForObject(
            "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_NAME = 'tenant'",
            Integer.class
        )
        .equals(1);
    boolean hasTenantUserTable = template()
        .queryForObject(
            "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_NAME = 'tenant_user'",
            Integer.class
        )
        .equals(1);
    boolean applicable = hasUserRoleTable || hasTenantTable || hasTenantUserTable;
    if (applicable) {
      // sets flags for run phase
      flags().put("hasUserRoleTable", hasUserRoleTable);
      flags().put("hasTenantTable", hasTenantTable);
      flags().put("hasTenantUserTable", hasTenantUserTable);
    }
    logger().info(
        String.format(
            "%s task found [user_role table exists ? %b; tenant table exists? %b; tenant_user table exists? %b] --> applicable? %b",
            getClass().getSimpleName(),
            hasUserRoleTable,
            hasTenantTable,
            hasTenantUserTable,
            applicable
        )
    );
    return applicable;

  }

  @Override
  public boolean run() {
    try {
      if (flag("hasUserRoleTable")) {
        template().execute("DROP TABLE user_role;");
        logger().info(
            String.format(
                "%s dropped user_role table successfully",
                getClass().getSimpleName()
            )
        );
      }
      if (flag("hasTenantUserTable")) {

        try {
          // assuming this will always return a non-null/empty value
          String tenantIdConstraintInNamespace = template()
              .queryForObject(
                  "SELECT constraint_name FROM information_schema.key_column_usage WHERE referenced_table_name = 'tenant' AND referenced_column_name = 'id' AND table_name = 'namespace' AND column_name = 'tenant_id'",
                  String.class
              );
          // not sure if prepared statement would work here as foreign key not a literal value
          // no injection possible with String.format anyway since it's all internal values
          template().execute(
              String.format(
                  "ALTER TABLE namespace DROP FOREIGN KEY %s?;",
                  tenantIdConstraintInNamespace
              )
          );
          logger().info(
              String.format(
                  "%s task dropped foreign key %s from namespace table successfully",
                  getClass().getSimpleName(),
                  tenantIdConstraintInNamespace
              )
          );
        }
        // occurs when constraint not found
        catch (IncorrectResultSizeDataAccessException irsdae) {
          logger().warn(
              String.format(
                  "%s constraint not found on namespace table - will try and drop the tenant_user table directly",
                  getClass().getSimpleName()
              )
          );
        }
        template().execute(
            "DROP TABLE tenant_user;"
        );
        logger().info(
            String.format(
                "%s dropped tenant_user table successfully",
                getClass().getSimpleName()
            )
        );
      }
      if (flag("hasTenantTable")) {
        template().execute(
            "DROP TABLE tenant;"
        );
        logger().info(
            String.format(
                "%s dropped tenant table successfully",
                getClass().getSimpleName()
            )
        );
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
    logger().error(
        String.format(
            "%s task failed but failure is likely recoverable - see stack trace. Letting Vorto repository start anyway.",
            getClass().getSimpleName()
        )
    );
  }

  @Override
  public String getDescription() {
    return "Drops legacy tables tenant, tenant_user and user_role after dropping constraints where applicable.";
  }
}
