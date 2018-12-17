/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.gradlebuild.test.integrationtests

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.gradlebuild.BuildEnvironment
import java.util.Timer
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask


@CacheableTask
open class EnvSpecificIntegrationTest : DistributionTest(true) {

    @Internal // different for different JVMs
    override fun getClasspath(): FileCollection {
        return super.getClasspath()
    }

    @Internal // differs between Linux and Windows
    override fun getJvmArgs(): MutableList<String> {
        return super.getJvmArgs()
    }

    @get:Nested
    val distributionSamples: GradleDistributionSamples = GradleDistributionSamples(project, gradleInstallationForTest.gradleHomeDir)

    override fun executeTests() {
        printStacktracesAfterTimeout { super.executeTests() }
    }

    private
    fun printStacktracesAfterTimeout(work: () -> Unit) = if (BuildEnvironment.isCiServer) {
        val timer = Timer(true).apply {
            schedule(timerTask {
                project.javaexec {
                    classpath = this@EnvSpecificIntegrationTest.classpath
                    main = "org.gradle.integtests.fixtures.timeout.JavaProcessStackTracesMonitor"
                }
            }, determineTimeoutMillis())
        }
        try {
            work()
        } finally {
            timer.cancel()
        }
    } else {
        work()
    }

    private
    fun determineTimeoutMillis() =
        when (systemProperties["org.gradle.integtest.executer"]) {
            "embedded" -> TimeUnit.MINUTES.toMillis(30)
            else -> TimeUnit.MINUTES.toMillis(165) // 2h45m
        }
}
