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
import org.eclipse.vorto.repository.domain.Privilege;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;

public class PopulateDefaultPrivileges extends MigrationTask {

  public PopulateDefaultPrivileges(final MigrationTaskChain chain) {
    super(chain);
  }

  @Override
  public boolean verify() {
    int privilegesCount = template()
        .queryForObject("select count(*) from privileges", Integer.class);

    boolean applicable = privilegesCount != Privilege.DEFAULT_PRIVILEGES.length;
    chain().logger().info(
        String.format(
            "%s task found %d privileges in table. Task applicable? %b ",
            getClass().getSimpleName(),
            privilegesCount,
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
              "insert into privileges (privilege, name) values(?,?)",
              new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                  ps.setLong(1, Privilege.DEFAULT_PRIVILEGES[i].getPrivilege());
                  ps.setString(2, Privilege.DEFAULT_PRIVILEGES[i].getName());
                }

                @Override
                public int getBatchSize() {
                  return Privilege.DEFAULT_PRIVILEGES.length;
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
    return "Populates the privileges table with default values if empty";
  }
}
