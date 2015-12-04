/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

package org.kurento.orion.entities;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

/**
 * A context element from Orion.
 *
 * @author Ivan Gracia (izanmail@gmail.com)
 *
 */
public class OrionContextElement {

  private String type;

  private boolean isPattern;
  private String id;
  private final List<OrionAttribute<?>> attributes = newArrayList();

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isPattern() {
    return isPattern;
  }

  public void setPattern(boolean isPattern) {
    this.isPattern = isPattern;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<OrionAttribute<?>> getAttributes() {
    return attributes;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append(" Type: ").append(type).append("\n");
    sb.append(" Id: ").append(id).append("\n");
    sb.append(" IsPattern: ").append(isPattern).append("\n");

    return sb.toString();
  }
}
