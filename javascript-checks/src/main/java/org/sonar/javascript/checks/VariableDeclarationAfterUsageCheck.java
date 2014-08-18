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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.sonar.sslr.api.AstNode;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.javascript.api.EcmaScriptTokenType;
import org.sonar.javascript.checks.utils.IdentifierUtils;
import org.sonar.javascript.parser.EcmaScriptGrammar;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.List;
import java.util.Map;

@Rule(
  key = "VariableDeclarationAfterUsage",
  priority = Priority.MAJOR)
@BelongsToProfile(title = CheckList.SONAR_WAY_PROFILE, priority = Priority.MAJOR)
public class VariableDeclarationAfterUsageCheck extends SquidCheck<LexerlessGrammar> {

  private static class Scope {
    private final Scope outerScope;
    Map<String, AstNode> firstDeclaration = Maps.newHashMap();
    Map<String, AstNode> firstUsage = Maps.newHashMap();

    public Scope() {
      this.outerScope = null;
    }

    public Scope(Scope outerScope) {
      this.outerScope = outerScope;
    }

    private void declare(AstNode astNode) {
      Preconditions.checkState(astNode.is(EcmaScriptTokenType.IDENTIFIER));
      String identifier = astNode.getTokenValue();
      if (!firstDeclaration.containsKey(identifier)) {
        firstDeclaration.put(identifier, astNode);
      }
    }

    private void use(AstNode astNode) {
      Preconditions.checkState(astNode.is(EcmaScriptTokenType.IDENTIFIER));
      String identifier = astNode.getTokenValue();
      if (!firstUsage.containsKey(identifier)) {
        firstUsage.put(identifier, astNode);
      }
    }
  }

  private static final GrammarRuleKey[] FUNCTION_NODES = {
    EcmaScriptGrammar.FUNCTION_EXPRESSION,
    EcmaScriptGrammar.FUNCTION_DECLARATION,
    EcmaScriptGrammar.GENERATOR_DECLARATION,
    EcmaScriptGrammar.GENERATOR_EXPRESSION};

  private Scope currentScope;

  @Override
  public void init() {
    subscribeTo(
      EcmaScriptGrammar.VARIABLE_DECLARATION,
      EcmaScriptGrammar.VARIABLE_DECLARATION_NO_IN,
      EcmaScriptGrammar.PRIMARY_EXPRESSION,
      EcmaScriptGrammar.FORMAL_PARAMETER_LIST);
    subscribeTo(FUNCTION_NODES);
  }

  @Override
  public void visitFile(AstNode astNode) {
    currentScope = new Scope();
  }

  @Override
  public void visitNode(AstNode astNode) {
    if (astNode.is(FUNCTION_NODES)) {
      // enter new scope
      currentScope = new Scope(currentScope);
    } else if (astNode.is(EcmaScriptGrammar.FORMAL_PARAMETER_LIST)) {
      declareInCurrentScope(IdentifierUtils.getParametersIdentifier(astNode));

    } else if (astNode.is(EcmaScriptGrammar.VARIABLE_DECLARATION, EcmaScriptGrammar.VARIABLE_DECLARATION_NO_IN)) {
      declareInCurrentScope(IdentifierUtils.getVariableIdentifiers(astNode));

    } else if (astNode.is(EcmaScriptGrammar.PRIMARY_EXPRESSION)) {
      AstNode identifier = astNode.getFirstChild(EcmaScriptTokenType.IDENTIFIER);
      if (identifier != null) {
        currentScope.use(identifier);
      }
    }
  }

  private void declareInCurrentScope(List<AstNode> identifiers) {
    for (AstNode identifier : identifiers) {
      currentScope.declare(identifier);
    }
  }

  @Override
  public void leaveNode(AstNode astNode) {
    if (astNode.is(FUNCTION_NODES)) {
      // leave scope
      checkCurrentScope();
      for (Map.Entry<String, AstNode> entry : currentScope.firstUsage.entrySet()) {
        if (!currentScope.firstDeclaration.containsKey(entry.getKey())) {
          currentScope.outerScope.use(entry.getValue());
        }
      }
      currentScope = currentScope.outerScope;
    }
  }

  private void checkCurrentScope() {
    for (Map.Entry<String, AstNode> entry : currentScope.firstDeclaration.entrySet()) {
      AstNode declaration = entry.getValue();
      AstNode usage = currentScope.firstUsage.get(entry.getKey());
      if (usage != null && usage.getTokenLine() < declaration.getTokenLine()) {
        getContext().createLineViolation(this, "Variable '" + entry.getKey() + "' referenced before declaration.", usage);
      }
    }
  }

  @Override
  public void leaveFile(AstNode astNode) {
    checkCurrentScope();
    currentScope = null;
  }

}
