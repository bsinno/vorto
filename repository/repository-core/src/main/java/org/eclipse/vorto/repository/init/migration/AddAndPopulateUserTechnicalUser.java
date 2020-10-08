package org.eclipse.vorto.repository.init.migration;

// TODO
public class AddAndPopulateUserTechnicalUser extends MigrationTask {

  public AddAndPopulateUserTechnicalUser(MigrationTaskChain chain) {
    super(chain);
  }

  @Override
  public boolean verify() {
    return false;
  }

  @Override
  public boolean run() {
    return false;
  }

  @Override
  public void onFailure() {

  }

  @Override
  public String getDescription() {
    return null;
  }
}
