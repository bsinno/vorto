package org.eclipse.vorto.repository.init.migration;

import org.springframework.dao.DataAccessException;

public class AddAndPopulateUserCreatedBy extends MigrationTask {

  public AddAndPopulateUserCreatedBy(MigrationTaskChain chain) {
    super(chain);
  }

  @Override
  public boolean verify() {
    boolean applicable = template()
        .queryForObject(
            "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_NAME='user' AND COLUMN_NAME='created_by'",
            Integer.class
        )
        .equals(0);
    logger()
        .info(
            String.format(
                "%s task applicable? %b",
                getClass().getSimpleName(),
                applicable
            )
        );
    return applicable;
  }

  @Override
  public boolean run() {
    try {
      template().execute("ALTER TABLE user ADD COLUMN created_by BIGINT(20);");
      logger().info(
          String.format(
              "%s task successfully added created_by column to the user table",
              getClass().getSimpleName()
          )
      );
      int rowsAffected = template().update(
          "update user set user.created_by = user.id"
      );
      logger().info(
          String.format(
              "%s task updated created_by values in user table successfully",
              getClass().getSimpleName()
          )
      );
      int totalRows = template().queryForObject("SELECT COUNT(id) from user", Integer.class);
      // unlikely: if some rows not updated, warn
      if (rowsAffected != totalRows) {
        logger().warn(
            String.format(
                "%s found a difference between rows updated and total rows in the user table [rows updated: %d; total rows: %d]",
                getClass().getSimpleName(),
                rowsAffected,
                totalRows
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
    return "Adds and populates the created_by field of the user table (with default values) when applicable";
  }
}
