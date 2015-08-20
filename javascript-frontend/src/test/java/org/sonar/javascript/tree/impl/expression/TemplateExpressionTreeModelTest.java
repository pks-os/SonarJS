/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011 SonarSource and Eriks Nukis
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.javascript.tree.impl.expression;

import org.junit.Test;
import org.sonar.javascript.lexer.JavaScriptPunctuator;
import org.sonar.javascript.utils.JavaScriptTreeModelTest;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;

import static org.fest.assertions.Assertions.assertThat;

public class TemplateExpressionTreeModelTest extends JavaScriptTreeModelTest {

  @Test
  public void test() throws Exception {
    TemplateExpressionTreeImpl tree = parse("` ${ expression } `", Kind.TEMPLATE_EXPRESSION);

    assertThat(tree.is(Kind.TEMPLATE_EXPRESSION)).isTrue();
    assertThat(tree.dollar().text()).isEqualTo("$");
    assertThat(tree.openCurlyBrace().text()).isEqualTo(JavaScriptPunctuator.LCURLYBRACE.getValue());
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.closeCurlyBrace().text()).isEqualTo(JavaScriptPunctuator.RCURLYBRACE.getValue());
  }

}