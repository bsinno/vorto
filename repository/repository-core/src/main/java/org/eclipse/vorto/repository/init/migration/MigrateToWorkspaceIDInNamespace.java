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

public class MigrateToWorkspaceIDInNamespace extends MigrationTask {

  public MigrateToWorkspaceIDInNamespace(MigrationTaskChain chain) {
    super(chain);
  }

  @Override
  public boolean verify() {
    boolean applicable;
    // requires no workspace_id field in the namespace table to be applicable
    // we assume the namespace table is present since it has no reason not to be
    // does not check for workspace_id population, as it can only be populated when created
    applicable = template()
        .queryForObject(
            "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_NAME = 'namespace' AND COLUMN_NAME = 'workspace_id'",
            Integer.class).equals(0);
    if (!applicable) {
      logger().info(
          String.format(
              "%s task found a workspace_id column in the namespace table. Task applicable? %b ",
              getClass().getSimpleName(),
              applicable
          )
      );
      return applicable;
    }
    // checking feasibility if applicable
    else {
      boolean tenantTableExists = template()
          .queryForObject(
              "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_NAME = 'tenant'",
              Integer.class)
          .equals(1);
      boolean namespaceHasTenantID = template()
          .queryForObject(
              "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_NAME = 'namespace' AND COLUMN_NAME = 'tenant_id'",
              Integer.class)
          .equals(1);
      // requires tenant table and tenant_id field in namespace table to be feasible
      boolean feasible = tenantTableExists && namespaceHasTenantID;
      if (feasible) {
        logger().info(
            String.format(
                "%s task is applicable and all requirements met. Will migrate.",
                getClass().getSimpleName()
            )
        );

      } else {
        logger().warn(
            String.format(
                "%s task found an issue with either [tenant table exists? %b; tenant_id field in namespace table exists? %b]. Task will fail.",
                getClass().getSimpleName(),
                tenantTableExists,
                namespaceHasTenantID
            )
        );
        fail();
      }
      return feasible;
    }
  }

  @Override
  public boolean run() {
    // at this point, we assume from the verify phase that the namespace table doesn't have the
    // workspace ID, but we can fetch the relevant data from the tenant table
    try {
      template()
          .execute(
              "ALTER TABLE namespace ADD COLUMN workspace_id VARCHAR(255) NOT NULL DEFAULT 'undefined';");
      logger().info(
          String.format(
              "%s task added workspace_id column to namespace table successfully",
              getClass().getSimpleName()
          )
      );
      int rowsAffected = template()
          .update(
              "UPDATE namespace SET workspace_id = (SELECT tenant_id FROM tenant WHERE id = namespace.tenant_id);");
      logger().info(
          String.format(
              "%s task updated workspace_id in namespace table (%d rows impacted)",
              getClass().getSimpleName(),
              rowsAffected
          )
      );
      // checking all rows affected or warning
      int namespaceSize = template()
          .queryForObject("SELECT COUNT(id) from namespace", Integer.class);

      if (namespaceSize != rowsAffected) {
        logger().warn(
            String.format(
                "%s task: some workspace_id values in namespace table were left undefined (%d rows updated vs. %d total size)",
                getClass().getSimpleName(),
                rowsAffected,
                namespaceSize
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
    chain().shutDown(-1);
  }

  @Override
  public String getDescription() {
    return "Replaces the tenant_id foreign key field in the namespace table with the actual JCR workspace ID fetched from the legacy tenant table.";
  }
}
