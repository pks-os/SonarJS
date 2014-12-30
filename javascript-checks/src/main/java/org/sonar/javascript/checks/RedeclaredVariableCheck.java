/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011 SonarSource and Eriks Nukis
 * dev@sonar.codehaus.org
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
package org.sonar.javascript.checks;

import com.sonar.sslr.api.AstNode;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.javascript.checks.utils.CheckUtils;
import org.sonar.javascript.checks.utils.IdentifierUtils;
import org.sonar.javascript.model.interfaces.Tree.Kind;
import org.sonar.javascript.parser.EcmaScriptGrammar;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

@Rule(
  key = "RedeclaredVariable",
  priority = Priority.MAJOR)
@BelongsToProfile(title = CheckList.SONAR_WAY_PROFILE, priority = Priority.MAJOR)
public class RedeclaredVariableCheck extends SquidCheck<LexerlessGrammar> {

  private Stack<Set<String>> stack;


  @Override
  public void init() {
    subscribeTo(
      Kind.VARIABLE_DECLARATION,
      EcmaScriptGrammar.VARIABLE_DECLARATION_NO_IN,
      EcmaScriptGrammar.FOR_BINDING);
    subscribeTo(CheckUtils.functionNodesArray());
  }

  @Override
  public void visitFile(AstNode astNode) {
    stack = new Stack<Set<String>>();
    stack.add(new HashSet<String>());
  }

  @Override
  public void visitNode(AstNode astNode) {
    if (CheckUtils.isFunction(astNode)) {
      Set<String> currentScope = new HashSet<String>();
      stack.add(currentScope);
      addParametersToScope(astNode, currentScope);
    } else {
      Set<String> currentScope = stack.peek();

      for (AstNode identifier : IdentifierUtils.getVariableIdentifiers(astNode)) {
        String variableName = identifier.getTokenValue();

        if (currentScope.contains(variableName)) {
          getContext().createLineViolation(this, "Rename variable \"" + variableName + "\" as this name is already used.", astNode);
        } else {
          currentScope.add(variableName);
        }
      }
    }
  }

  @Override
  public void leaveNode(AstNode astNode) {
    if (CheckUtils.isFunction(astNode)) {
      stack.pop();
    }
  }

  @Override
  public void leaveFile(AstNode astNode) {
    stack = null;
  }

  private void addParametersToScope(AstNode functionNode, Set<String> currentScope) {
    if (functionNode.is(Kind.ARROW_FUNCTION)) {
      addArrowParametersToScope(functionNode.getFirstChild(Kind.ARROW_PARAMETER_LIST, Kind.IDENTIFIER), currentScope);
    } else {
      addFormalParametersToScope(functionNode.getFirstChild(Kind.FORMAL_PARAMETER_LIST), currentScope);
    }
  }

  private void addArrowParametersToScope(AstNode arrowParameters, Set<String> currentScope) {
    if (arrowParameters != null) {
      for (AstNode parameter : IdentifierUtils.getArrowParametersIdentifier(arrowParameters)) {
        currentScope.add(parameter.getTokenValue());
      }
    }
  }

  private void addFormalParametersToScope(AstNode formalParameterList, Set<String> currentScope) {
    if (formalParameterList != null) {
      for (AstNode identifier : IdentifierUtils.getParametersIdentifier(formalParameterList)) {
        currentScope.add(identifier.getTokenValue());
      }
    }
  }

}
