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

public class PopulateDefaultNamespaceRoles extends MigrationTask {

  public PopulateDefaultNamespaceRoles(final MigrationTaskChain chain) {
    super(chain);
  }

  @Override
  public boolean verify() {
    int namespaceRolesCount = template()
        .queryForObject("select count(*) from namespace_roles", Integer.class);

    boolean applicable = namespaceRolesCount != NamespaceRole.DEFAULT_NAMESPACE_ROLES.length;
    chain().logger().info(
        String.format(
            "%s task found %d namespace roles in table. Task applicable? %b ",
            getClass().getSimpleName(),
            namespaceRolesCount,
            applicable
        )
    );
    return applicable;
  }

  @Override
  public boolean run() {
    try {
      // assuming no exception means insert successful
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
      chain().logger().warn(
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
    return "Populates the namespace_roles table with default values if empty";
  }
}
