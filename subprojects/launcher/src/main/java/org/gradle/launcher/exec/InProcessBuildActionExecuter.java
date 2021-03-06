/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.launcher.exec;

import org.gradle.BuildResult;
import org.gradle.api.internal.GradleInternal;
import org.gradle.initialization.*;

public class InProcessBuildActionExecuter implements BuildActionExecuter<BuildActionParameters> {
    private final GradleLauncherFactory gradleLauncherFactory;
    private final BuildActionRunner buildActionRunner;

    public InProcessBuildActionExecuter(GradleLauncherFactory gradleLauncherFactory, BuildActionRunner buildActionRunner) {
        this.gradleLauncherFactory = gradleLauncherFactory;
        this.buildActionRunner = buildActionRunner;
    }

    public Object execute(BuildAction action, BuildRequestContext buildRequestContext, BuildActionParameters actionParameters) {
        DefaultGradleLauncher gradleLauncher = (DefaultGradleLauncher) gradleLauncherFactory.newInstance(action.getStartParameter(), buildRequestContext);
        try {
            DefaultBuildController buildController = new DefaultBuildController(gradleLauncher);
            return buildActionRunner.run(action, buildController);
        } finally {
            gradleLauncher.stop();
        }
    }

    private static class DefaultBuildController implements BuildController {
        private enum State { Created, Completed }
        private State state = State.Created;
        private final DefaultGradleLauncher gradleLauncher;

        public DefaultBuildController(DefaultGradleLauncher gradleLauncher) {
            this.gradleLauncher = gradleLauncher;
        }

        public DefaultGradleLauncher getLauncher() {
            if (state == State.Completed) {
                throw new IllegalStateException("Cannot use launcher after build has completed.");
            }
            return gradleLauncher;
        }

        public GradleInternal getGradle() {
            return getLauncher().getGradle();
        }

        public GradleInternal run() {
            return check(getLauncher().run());
        }

        public GradleInternal configure() {
            return check(getLauncher().getBuildAnalysis());
        }

        private GradleInternal check(BuildResult buildResult) {
            state = State.Completed;
            if (buildResult.getFailure() != null) {
                throw new ReportedException(buildResult.getFailure());
            }
            return (GradleInternal) buildResult.getGradle();
        }
    }
}
