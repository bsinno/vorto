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

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Represents a sequential orchestrator for database migration or sanitization tasks. <br/>
 * Invoke with static {@link MigrationTaskChain#startWith(JdbcTemplate, Class[])}.
 */
public class MigrationTaskChain {

  private static final Logger LOGGER = Logger.getLogger(MigrationTaskChain.class);
  private JdbcTemplate template;
  private static final String EMPTY = "No migration tasks injected. Resuming normal application lifecycle. ";

  protected final JdbcTemplate getTemplate() {
    return template;
  }

  protected final Logger logger() {
    return LOGGER;
  }

  private MigrationTaskChain(JdbcTemplate template, Class<? extends MigrationTask>... taskClasses) {
    // verifying there are tasks, or shutting down quietly if none available
    if (Objects.isNull(taskClasses)) {
      LOGGER.warn(EMPTY);
      return;
    }
    int size = taskClasses.length;
    if (size == 0) {
      LOGGER.warn(EMPTY);
      return;
    }
    // assigning fields
    this.template = template;

    // iterating over tasks
    for (int i = 0; i < size; i++) {
      Class<? extends MigrationTask> taskClass = taskClasses[i];
      try {
        // initialization
        MigrationTask task = taskClass.getConstructor(MigrationTaskChain.class).newInstance(this);
        // logging start status
        LOGGER.info(
            String.format(
                "About to start migration task type [%s]: [%s] (%d of %d)...",
                task.getClass().getSimpleName(),
                task.getDescription(),
                i + 1,
                size
            )
        );
        // starting - failures are handled within MigrationTask
        task.start();
      }
      // should never happen, as the tasks themselves must extend MigrationTask and therefore
      // feature a compatible constructor
      catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
        LOGGER.error("Could not initialize one of the migration tasks");
        shutDown(-1);
      }
    }
  }

  /**
   * Initializes and starts a migration chain by:
   * <ul>
   *   <li>
   *     Injecting an (autowired) {@link JdbcTemplate}.
   *   </li>
   *   <li>
   *     Attempting to initialize objects of, verify and if so required, execute the given tasks
   *     extending {@link MigrationTask} <b>in their order of declaration</b>.
   *   </li>
   * </ul>
   * Instances of {@link MigrationTask} will first verify if they are applicable and is so, run
   * sequentially. <br/>
   * Running tasks can either succeed, fail "quietly" or fail "hard" (by stopping the Vorto repository).
   * <br/>
   * The {@link MigrationTaskChain} logs informative messages on progress and task status.
   *
   * @param template
   * @param taskClasses
   * @return
   */
  public static MigrationTaskChain startWith(JdbcTemplate template,
      Class<? extends MigrationTask>... taskClasses) {
    return new MigrationTaskChain(template, taskClasses);
  }

  /**
   * Can be invoked by any {@link MigrationTask} if it fails to run and its failure should prevent
   * the Vorto repository from even starting. <br/>
   * Will shut down the Vorto repository immediately with the given error code.
   *
   * @param code
   */
  public void shutDown(final int code) {
    LOGGER.warn(
        String.format("Shutting down the repository now with exit code %d", code)
    );
    System.exit(code);
  }
}
