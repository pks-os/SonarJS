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
package org.sonar.plugins.javascript;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.check.Rule;
import org.sonar.javascript.checks.CheckList;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

public class JavaScriptProfilesDefinition implements BuiltInQualityProfilesDefinition {

  static final String SONAR_WAY = "Sonar way";
  static final String SONAR_WAY_RECOMMENDED = "Sonar way Recommended";

  public static final String RESOURCE_PATH = "org/sonar/l10n/javascript/rules/javascript";
  public static final String SONAR_WAY_JSON = RESOURCE_PATH + "/Sonar_way_profile.json";
  public static final String SONAR_WAY_RECOMMENDED_JSON = RESOURCE_PATH + "/Sonar_way_recommended_profile.json";

  private static final Map<String, String> PROFILES = new HashMap<>();
  static {
    PROFILES.put(SONAR_WAY, SONAR_WAY_JSON);
    PROFILES.put(SONAR_WAY_RECOMMENDED, SONAR_WAY_RECOMMENDED_JSON);
  }

  private static final Map<String, String> REPO_BY_LANGUAGE = new HashMap<>();
  static {
    REPO_BY_LANGUAGE.put(JavaScriptLanguage.KEY, CheckList.JS_REPOSITORY_KEY);
    REPO_BY_LANGUAGE.put(TypeScriptLanguage.KEY, CheckList.TS_REPOSITORY_KEY);
  }

  @Override
  public void define(Context context) {
    List<Class> javaScriptChecks = CheckList.getJavaScriptChecks();
    createProfile(SONAR_WAY, JavaScriptLanguage.KEY, javaScriptChecks, context);
    createProfile(SONAR_WAY_RECOMMENDED, JavaScriptLanguage.KEY, javaScriptChecks, context);

    List<Class> typeScriptChecks = CheckList.getTypeScriptChecks();
    createProfile(SONAR_WAY, TypeScriptLanguage.KEY, typeScriptChecks, context);
    createProfile(SONAR_WAY_RECOMMENDED, TypeScriptLanguage.KEY, typeScriptChecks, context);
  }

  private void createProfile(String profileName, String language, List<Class> checks, Context context) {
    NewBuiltInQualityProfile newProfile = context.createBuiltInQualityProfile(profileName, language);
    String jsonProfilePath = PROFILES.get(profileName);
    String repositoryKey = REPO_BY_LANGUAGE.get(language);
    Set<String> activeKeysForBothLanguages = BuiltInQualityProfileJsonLoader.loadActiveKeysFromJsonProfile(jsonProfilePath);

    checks.stream()
      .map(c -> ((Rule) c.getAnnotation(Rule.class)).key())
      .filter(activeKeysForBothLanguages::contains)
      .forEach(key -> newProfile.activateRule(repositoryKey, key));

    if (profileName.equals(SONAR_WAY_RECOMMENDED)) {
      newProfile.activateRule("common-" + language, "DuplicatedBlocks");
    }

    newProfile.done();
  }
}