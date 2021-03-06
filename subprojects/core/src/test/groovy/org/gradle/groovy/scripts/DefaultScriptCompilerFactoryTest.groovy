/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.groovy.scripts

import org.gradle.api.Action
import org.gradle.groovy.scripts.internal.CompiledScript
import org.gradle.groovy.scripts.internal.MetadataExtractingTransformer
import org.gradle.groovy.scripts.internal.ScriptClassCompiler
import org.gradle.groovy.scripts.internal.ScriptRunnerFactory
import org.gradle.internal.service.ServiceRegistry
import org.gradle.logging.StandardOutputCapture
import spock.lang.Specification

class DefaultScriptCompilerFactoryTest extends Specification {
    final ScriptRunnerFactory scriptRunnerFactory = Mock()
    final ScriptClassCompiler scriptClassCompiler = Mock()
    final ScriptSource source = Mock()
    final ScriptRunner<TestScript, ?> runner = Mock()
    final ClassLoader classLoader = Mock()
    final MetadataExtractingTransformer<?> extractingTransformer = Mock()
    final CompiledScript<TestScript, ?> compiledScript = Mock() {
        loadClass() >> TestScript
    }
    final verifier = Mock(Action)
    final DefaultScriptCompilerFactory factory = new DefaultScriptCompilerFactory(scriptClassCompiler, scriptRunnerFactory)

    def "compiles script into class and wraps instance in script runner"() {
        when:
        def compiler = factory.createCompiler(source)
        compiler.classloader = classLoader
        compiler.verifier = verifier
        compiler.classpathClosureName = "buildscript"
        def result = compiler.compile(Script, extractingTransformer)

        then:
        result == runner
        1 * scriptClassCompiler.compile({it instanceof CachingScriptSource}, classLoader, extractingTransformer, "buildscript", Script, verifier) >> compiledScript
        1 * scriptRunnerFactory.create(compiledScript, {it instanceof CachingScriptSource}, classLoader) >> runner
        0 * scriptRunnerFactory._
        0 * scriptClassCompiler._
    }
}

class TestScript extends Script {
    @Override
    StandardOutputCapture getStandardOutputCapture() {
    }

    @Override
    void init(Object target, ServiceRegistry services) {
    }

    Object run() {
    }
}
