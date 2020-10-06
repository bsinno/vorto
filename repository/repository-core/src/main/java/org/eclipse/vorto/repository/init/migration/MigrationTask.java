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

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Parent class for all migration tasks. <br/>
 * Initialized and started by {@link MigrationTaskChain} during iteration over configured tasks.<br/>
 * Every task will first verify its applicability after being started, as {@link MigrationTask#start()}
 * invokes child implementation of {@link MigrationTask#verify()} and proceeds only if it returns
 * {@literal true}.<br/>
 * If the child implementation of {@link MigrationTask#run()} returns {@literal true}, the task
 * terminates successfully.<br/>
 * Otherwise, the child implementation of {@link MigrationTask#onFailure()} will be invoked after
 * {@link MigrationTask#fail()}.
 */
public abstract class MigrationTask {

  protected final MigrationTaskChain chain;
  protected boolean failed = false;

  public final JdbcTemplate template() {
    return chain().getTemplate();
  }

  protected final MigrationTaskChain chain() {
    return chain;
  }

  public final boolean hasFailed() {
    return failed;
  }

  /**
   * Initializes this migration task with the given {@link JdbcTemplate}.
   *
   * @param chain
   */
  public MigrationTask(final MigrationTaskChain chain) {
    this.chain = chain;
  }

  /**
   * Starts this task. <br/>
   * Will invoke {@link MigrationTask#run()} only if {@link MigrationTask#verify()} returns
   * {@literal true}.<br/>
   * <br/>
   * Will invoke {@link MigrationTask#fail()} and then child implementation of
   * {@link MigrationTask#onFailure()} only if child implementation of {@link MigrationTask#run()}
   * returns {@literal false}.
   */
  public final void start() {
    // checking applicability
    if (verify()) {
      // applicable: checking if run succeeded
      if (!run()) {
        // failing and invoking onFailure
        fail();
      }
      // logging success
      else {
        chain().logger().info(
            String.format(
                "%s task ran successfully.",
                getClass().getSimpleName()
            )
        );
      }
    }
    // no need to run / not applicable
    else {
      chain().logger().info(
          String.format(
              "%s task not applicable - skipping.",
              getClass().getSimpleName()
          )
      );
    }
  }

  /**
   * Implementor logic to verify the applicability of this task, typically by querying table DDL
   * and/or field values or counts through the chain's common {@link JdbcTemplate}.<br/>
   * Tasks will be skipped if not applicable.
   *
   * @return {@literal true} if applicable, {@literal false} otherwise.
   */
  public abstract boolean verify();

  /**
   * Implementor logic to apply the task at hand, typically by modifying DDL or field values through
   * the chain's common {@link JdbcTemplate}.<br/>
   *
   * @return {@literal true} if run succeeded, {@literal false} otherwise.
   */
  public abstract boolean run();

  /**
   * Implementor logic to handle post-failure actions for this task. <br/>
   * This will typically imply informative logging of why the task fails. <br/>
   * For failures that are considered unrecovereable for the Vorto repository's stability, the
   * implementation of this method can tell the {@link MigrationTaskChain} to
   * {@link MigrationTaskChain#shutDown(int)} the repository with a specific error code.
   */
  public abstract void onFailure();

  /**
   * Implementor logic to provide a short, human-readable description of what this task does.
   *
   * @return
   */
  public abstract String getDescription();

  /**
   * Fails this task if the invocation of {@link MigrationTask#run()} in
   * {@link MigrationTask#start()} returned {@literal false}.<br/>
   * Invokes implementor logic {@link MigrationTask#onFailure()} after setting the {@code failed}
   * flag to {@literal true} - see {@link MigrationTask#hasFailed()}.
   */
  public final void fail() {
    chain().logger().warn(
        String.format("%s task failed to run", getClass().getSimpleName())
    );
    failed = true;
    onFailure();
  }

}
