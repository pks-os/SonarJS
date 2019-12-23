/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
// https://jira.sonarsource.com/browse/RSPEC-1128

import { Rule } from "eslint";
import * as estree from "estree";

const EXCLUDED_IMPORTS = ["React"];

export const rule: Rule.RuleModule = {
  create(context: Rule.RuleContext) {
    const unusedImports: estree.Identifier[] = [];
    return {
      "Program": () => {
        const sourceCode = context.getSourceCode();
        console.log(sourceCode);
      },
      "ImportDeclaration": (node: estree.Node) => {
        const variables = context.getDeclaredVariables(node);
        for (const variable of variables) {
          if (!EXCLUDED_IMPORTS.includes(variable.name) && variable.references.length === 0) {
            unusedImports.push(variable.identifiers[0]);
          }
        }
      },
      "Program:exit": () => {
        const jsxIdentifiers = context
          .getSourceCode()
          .ast.tokens.filter(token => token.type === "JSXIdentifier")
          .map(token => token.value);
        unusedImports.filter(unused => !jsxIdentifiers.includes(unused.name)).forEach(unused =>
          context.report({
            message: `Remove this unused import of '${unused.name}'.`,
            node: unused,
          }),
        );
      },
    };
  },
};
