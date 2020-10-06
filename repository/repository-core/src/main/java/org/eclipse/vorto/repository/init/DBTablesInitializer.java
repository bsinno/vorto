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

import org.eclipse.vorto.repository.init.migration.MigrationTaskChain;
import org.eclipse.vorto.repository.init.migration.PopulateDefaultNamespaceRoles;
import org.eclipse.vorto.repository.init.migration.PopulateDefaultPrivileges;
import org.eclipse.vorto.repository.init.migration.PopulateDefaultRepositoryRoles;
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
 * This class hooks up at repository initialization, autowires the 3 repositories where
 * "runtime-constant" data is held (privileges, namespace roles and repository roles) and verifies
 * whether those tables have any data.<br/>
 * If they do not, logs a debug message and populates them automatically with default values.<br/>
 * Typically, this would occur when starting a Vorto repository for the first time.
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
          PopulateDefaultPrivileges.class,
          PopulateDefaultNamespaceRoles.class,
          PopulateDefaultRepositoryRoles.class
      );
    }

    /*if (privilegeRepository.findAll().isEmpty()) {
      LOGGER.debug("Found privileges table empty - populating with default values.");
      Arrays.stream(Privilege.DEFAULT_PRIVILEGES).forEach(privilegeRepository::save);
    }*/
    /*if (namespaceRoleRepository.findAll().isEmpty()) {
      LOGGER.debug("Found namespace roles table empty - populating with default values.");
      Arrays.stream(NamespaceRole.DEFAULT_NAMESPACE_ROLES).forEach(namespaceRoleRepository::save);
    }*/
    /*if (repositoryRoleRepository.findAll().isEmpty()) {
      LOGGER.debug("Found repository roles table empty - populating with default values.");
      Arrays.stream(RepositoryRole.DEFAULT_REPOSITORY_ROLES)
          .forEach(repositoryRoleRepository::save);
    }*/
  }

}
