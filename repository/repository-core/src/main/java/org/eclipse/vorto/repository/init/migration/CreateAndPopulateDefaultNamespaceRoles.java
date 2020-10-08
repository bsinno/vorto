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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.eclipse.vorto.repository.domain.NamespaceRole;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;

public class CreateAndPopulateDefaultNamespaceRoles extends MigrationTask {

  public CreateAndPopulateDefaultNamespaceRoles(final MigrationTaskChain chain) {
    super(chain);
  }

  @Override
  public boolean verify() {
    boolean applicable;
    // checks table actually exists - hardly ever useful unless hibernate props are changed, since
    // tables should be automatically created based on entities otherwise
    boolean namespaceRolesTableExists = template()
        .queryForObject(
            "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_NAME = 'namespace_roles'",
            Integer.class) == 1;
    // saves for run phase
    flags().put("namespaceRolesTableExists", namespaceRolesTableExists);

    // does not exist - task applicable
    if (!namespaceRolesTableExists) {
      applicable = true;
      logger().info(
          String.format(
              "%s task found no namespace_roles table. Task applicable? %b ",
              getClass().getSimpleName(),
              applicable
          )
      );
    }
    // table exists
    else {
      // comparing number of namespace roles in table vs defaults
      int namespaceRolesCount = template()
          .queryForObject("SELECT COUNT(*) FROM namespace_roles", Integer.class);

      applicable = namespaceRolesCount != NamespaceRole.DEFAULT_NAMESPACE_ROLES.length;
      logger().info(
          String.format(
              "%s task found %d namespace roles in table. Task applicable? %b ",
              getClass().getSimpleName(),
              namespaceRolesCount,
              applicable
          )
      );
    }
    return applicable;
  }

  @Override
  public boolean run() {
    // assuming no exception means create / insert successful
    try {
      // assuming key always present as saved in verify phase
      if (!flags().get("namespaceRolesTableExists")) {
        template().execute(
            "CREATE TABLE namespace_roles(role BIGINT NOT NULL PRIMARY KEY UNIQUE CHECK ( role & (role - 1) = 0 ), name VARCHAR(64) NOT NULL UNIQUE, privileges BIGINT NOT NULL DEFAULT 0);"
        );
      }
      // TODO could be improved by granularly inserting only missing defaults
      template()
          .batchUpdate(
              "insert into namespace_roles (role, name, privileges) values(?,?,?)",
              new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                  ps.setLong(1, NamespaceRole.DEFAULT_NAMESPACE_ROLES[i].getRole());
                  ps.setString(2, NamespaceRole.DEFAULT_NAMESPACE_ROLES[i].getName());
                  ps.setLong(3, NamespaceRole.DEFAULT_NAMESPACE_ROLES[i].getPrivileges());
                }

                @Override
                public int getBatchSize() {
                  return NamespaceRole.DEFAULT_NAMESPACE_ROLES.length;
                }
              }
          );
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
    return "Creates and populates the namespace_roles table with default values if missing / empty";
  }
}
