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
package org.eclipse.vorto.repository.init;

import org.eclipse.vorto.repository.init.migration.CleanupLegacyTables;
import org.eclipse.vorto.repository.init.migration.CreateAndPopulateDefaultNamespaceRoles;
import org.eclipse.vorto.repository.init.migration.CreateAndPopulateDefaultPrivileges;
import org.eclipse.vorto.repository.init.migration.CreateAndPopulateDefaultRepositoryRoles;
import org.eclipse.vorto.repository.init.migration.CreateAndPopulateUserNamespaceRoles;
import org.eclipse.vorto.repository.init.migration.MigrateToWorkspaceIDInNamespace;
import org.eclipse.vorto.repository.init.migration.MigrationTaskChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * This class hooks up at repository initialization and performs database queries and updates based
 * on tables, columns and data missing - e.g. it can migrate data before the repository starts. <br/>
 * In order for this to work, the property {@literal spring.jpa.hibernate.ddl-auto} <i>cannot</i>
 * be set to {@literal validate} - otherwise a source update containing new entities or changes in
 * existing entities would cause the repository initialization to crash before this
 * {@link ApplicationRunner} is executed, thus defiling its purpose.<br/>
 * At the time of writing, the {@literal spring.jpa.hibernate.ddl-auto} property is set to
 * {@literal none} (default value) in productive environments, simply because it has been set
 * incorrectly as <s>{@literal spring.hibernate.ddl-auto}</s> (note the missing {@literal jpa} here).
 */
@Service
@Profile("!test")
public class DBTablesInitializer implements ApplicationRunner {

  private final Logger LOGGER = LoggerFactory.getLogger(getClass());

  @Autowired
  private JdbcTemplate template;

  @Value("${config.enableDBTableAutoInitialization:#{true}}")
  private boolean enableDBTableAutoInitialization;

  @Override
  public void run(ApplicationArguments applicationArguments) {

    LOGGER.info(
        String.format(
            "Automatic DB table initialization enabled? %b",
            enableDBTableAutoInitialization
        )
    );

    if (enableDBTableAutoInitialization) {
      MigrationTaskChain.startWith(
          template,
          CreateAndPopulateDefaultPrivileges.class,
          CreateAndPopulateDefaultNamespaceRoles.class,
          CreateAndPopulateDefaultRepositoryRoles.class,
          // TODO maybe super old migration for user_role? probably unnecessary
          // TODO technical user
          CreateAndPopulateUserNamespaceRoles.class, // TODO this one is not finished, see run implementation
          MigrateToWorkspaceIDInNamespace.class,
          // TODO createdBy
          CleanupLegacyTables.class
      );
    }

  }

}
