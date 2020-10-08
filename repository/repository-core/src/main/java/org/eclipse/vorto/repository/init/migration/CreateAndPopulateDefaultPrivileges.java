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

public class CreateAndPopulateDefaultPrivileges extends MigrationTask {

  public CreateAndPopulateDefaultPrivileges(final MigrationTaskChain chain) {
    super(chain);
  }

  @Override
  public boolean verify() {
    boolean applicable;
    // checks table actually exists - hardly ever useful unless hibernate props are changed, since
    // tables should be automatically created based on entities otherwise
    boolean privilegesTableExists = template()
        .queryForObject(
            "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_NAME = 'privileges'",
            Integer.class)
        .equals(1);
    // saves for run phase
    flags().put("privilegesTableExists", privilegesTableExists);

    // does not exist - task applicable
    if (!privilegesTableExists) {
      applicable = true;
      logger().info(
          String.format(
              "%s task found no privileges table. Task applicable? %b ",
              getClass().getSimpleName(),
              applicable
          )
      );
    }
    // table exists
    else {
      // comparing number of privileges in table vs defaults
      int privilegesCount = template()
          .queryForObject("SELECT COUNT(*) FROM privileges", Integer.class);

      applicable = privilegesCount != Privilege.DEFAULT_PRIVILEGES.length;
      logger().info(
          String.format(
              "%s task found %d privileges in table. Task applicable? %b ",
              getClass().getSimpleName(),
              privilegesCount,
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
      if (!flags().get("privilegesTableExists")) {
        // assuming key always present as saved in verify phase
        template().execute(
            "CREATE TABLE privileges(privilege BIGINT NOT NULL PRIMARY KEY UNIQUE CHECK ( privilege & (privilege - 1) = 0 ), name VARCHAR(64) NOT NULL UNIQUE);"
        );
      }
      // TODO could be improved by granularly inserting only missing defaults
      template()
          .batchUpdate(
              "INSERT INTO privileges (privilege, name) VALUES(?,?)",
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
    return "Creates and populates the privileges table with default values if missing / empty";
  }
}
