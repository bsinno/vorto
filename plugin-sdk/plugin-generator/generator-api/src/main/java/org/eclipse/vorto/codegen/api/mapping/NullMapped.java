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
package org.eclipse.vorto.codegen.api.mapping;

/**
 * Please use the Plugin SDK API instead
 */
@Deprecated
public class NullMapped<T> implements IMapped<T> {

  private T source;

  public NullMapped(T source) {
    this.source = source;
  }

  @Override
  public T getSource() {
    return source;
  }

  @Override
  public String getStereoType() {
    return "";
  }

  @Override
  public boolean hasAttribute(String attributeName) {
    return false;
  }

  @Override
  public String getAttributeValue(String attributeName, String defaultValue) {
    return defaultValue;
  }

  @Override
  public boolean isMapped() {
    return false;
  }
}
