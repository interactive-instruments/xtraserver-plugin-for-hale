/*
 * Copyright (c) 2022 interactive instruments GmbH
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     interactive instruments GmbH <http://www.interactive-instruments.de>
 */

package de.ii.xtraserver.webapi.hale.io.writer.handler;

public class JoinInfo {

  private final String baseTableName;
  private final String baseTableJoinField;
  private final String joinTableName;
  private final String joinTableJoinField;

  public JoinInfo(String baseTableName, String baseTableJoinField, String joinTableName,
      String joinTableJoinField) {
    this.baseTableName = baseTableName;
    this.baseTableJoinField = baseTableJoinField;
    this.joinTableName = joinTableName;
    this.joinTableJoinField = joinTableJoinField;
  }

  public String getBaseTableName() {
    return baseTableName;
  }

  public String getBaseTableJoinField() {
    return baseTableJoinField;
  }

  public String getJoinTableName() {
    return joinTableName;
  }

  public String getJoinTableJoinField() {
    return joinTableJoinField;
  }
}
