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

/**
 * Attribute to be used in context elements and queries
 *
 * @author Ivan Gracia (izanmail@gmail.com)
 * @param <T>
 *          The type of attribute
 *
 */
public class OrionAttribute<T> {

  private String name;
  private String type;
  private T value;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public T getValue() {
    return value;
  }

  public void setValue(T value) {
    this.value = value;
  }

  public OrionAttribute(String name, String type, T value) {
    super();
    this.name = name;
    this.type = type;
    this.value = value;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append(" Name: ").append(name).append("\n");
    sb.append(" Type: ").append(type).append("\n");
    sb.append(" Value: ").append(value).append("\n");

    return sb.toString();
  }
}
