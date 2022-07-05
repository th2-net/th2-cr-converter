/*
 * Copyright 2020-2022 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.exactpro.th2.converter.controllers.errors.NotAcceptableException
import com.exactpro.th2.converter.model.ComparableTo
import com.exactpro.th2.converter.model.Th2Resource
import com.exactpro.th2.converter.model.latest.Th2Metadata
import com.exactpro.th2.converter.model.latest.box.GenericBoxSpec
import com.exactpro.th2.converter.model.latest.box.pins.GrpcClient
import com.exactpro.th2.converter.model.latest.box.pins.GrpcServer
import com.exactpro.th2.converter.model.latest.box.pins.PinSpec
import com.exactpro.th2.converter.model.v1.box.GenericBoxSpecV1
import com.exactpro.th2.converter.model.v1.box.PinType
import com.exactpro.th2.converter.model.v1.box.pins.PinSpecV1
import com.exactpro.th2.converter.util.Converter
import com.exactpro.th2.converter.util.Mapper.YAML_MAPPER
import com.exactpro.th2.infrarepo.repo.RepositoryResource
import com.fasterxml.jackson.module.kotlin.convertValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.util.*
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class ConverterTest {
    companion object {
        const val V1_PATH = "src/test/resources/V1/"
        const val V2_PATH = "src/test/resources/V2/"
    }

    private val actV1 = readV1RepoResource("act.yml")

    private val check1V1 = readV1RepoResource("check1.yml")

    private val fixServerV1 = readV1RepoResource("fix-server.yml")

    private val scriptV1 = readV1RepoResource("script.yml")

    private val scriptV2 = readV2RepoResource("script.yml")

    private val repoV1BoxesFullSet = sortedSetOf(
        Comparator.comparing { RepositoryResource::getMetadata.name },
        actV1, check1V1, fixServerV1, scriptV1
    )

    private fun readRepoResource(path: String): RepositoryResource {
        return YAML_MAPPER.readValue(
            File(path), RepositoryResource::class.java
        )
    }

    private fun readV1RepoResource(fileName: String): RepositoryResource {
        return readRepoResource(V1_PATH + fileName)
    }

    private fun readV2RepoResource(fileName: String): RepositoryResource {
        return readRepoResource(V2_PATH + fileName)
    }

    /**
     * Tests Converter.convertFromRequest(targetVersion, resources)
     * From V1 to V2.
     * Pin conversion.
     */
    @Test
    fun testPinsConversionToV2FromRequest() {
        val convertedTh2ResList = Converter.convertFromRequest("v2", repoV1BoxesFullSet)

        for ((index, v1Res) in repoV1BoxesFullSet.withIndex()) {
            val resFailMessage = v1Res.metadata.name.plus(" conversion v1 -> v2 failed: ")

            val v1ResSpec: GenericBoxSpecV1 = YAML_MAPPER.convertValue(v1Res.spec)
            val v1ResPins = v1ResSpec.pins as List<PinSpecV1> // not testing null pins

            val convertedResSpec = convertedTh2ResList[index].spec as GenericBoxSpec
            val convertedResPins: PinSpec? = convertedResSpec.pins

            assertNotNull(convertedResPins, resFailMessage + "Pin conversion resulted in null")

            val v1MqSubPins: List<PinSpecV1> = v1ResPins
                .filter { it.connectionType == PinType.MQ.value }
                .filter { it.attributes?.contains("subscribe") ?: false }

            val v1MqPublisherPins = v1ResPins
                .filter { it.connectionType == PinType.MQ.value }
                .filter { it.attributes?.contains("publish") ?: false }

            val mqSection = convertedResPins.mq
            val v2MqSubPins = mqSection?.subscribers
            val v2MqPublisherPins = mqSection?.publishers

            testContentsMatch(
                v2MqSubPins, v1MqSubPins,
                resFailMessage + "Contents in MQ subscriber pins don't match"
            )

            testContentsMatch(
                v2MqPublisherPins, v1MqPublisherPins,
                resFailMessage + "Contents in MQ publisher pins don't match"
            )

            val v1GrpcServerPins: List<PinSpecV1> = v1ResPins
                .filter { it.connectionType == PinType.GRPC_SERVER.value }
            val v2GrpcServerPins: List<GrpcServer>? = convertedResPins.grpc?.server

            val v1GrpcClientPins: List<PinSpecV1> = v1ResPins
                .filter { it.connectionType == PinType.GRPC_CLIENT.value }
            val v2GrpcClientPins: List<GrpcClient>? = convertedResPins.grpc?.client

            testContentsMatch(
                v2GrpcClientPins, v1GrpcClientPins,
                resFailMessage + "Contents in Grpc client pins don't match"
            )

            testContentsMatch(
                v2GrpcServerPins, v1GrpcServerPins,
                resFailMessage + "Contents in Grpc server pins don't match"
            )
        }
    }

    private fun <T> testContentsMatch(converted: List<ComparableTo<T>>?, base: List<T>, failMessage: String) {
        if (converted != null) {
            assertTrue(failMessage, resourceListsEqual(converted, base))
        }
    }

    /**
     * Tests Converter.convertFromRequest(targetVersion, resources).
     * Converts YAML sample(s) from V1 to V2
     * and tests whether all the naming and section conversions were correct
     */

    @Test
    fun testConvertFromRequestToV2EntireYAML() {
        val sampleResSet = setOf(scriptV1)

        val actualConvertedList = Converter.convertFromRequest("v2", sampleResSet)
        val actualConvertedScript = actualConvertedList
            .find { it.metadata.name == "script" } as Th2Resource
        var expectedConvertedScriptSpec: GenericBoxSpec? = null
        assertDoesNotThrow("script conversion v1 -> v2 failed: Spec is incorrect") {
            expectedConvertedScriptSpec = YAML_MAPPER.convertValue(scriptV2.spec)
        }
        val expectedConvertedScript = Th2Resource(
            scriptV2.apiVersion,
            scriptV2.kind,
            Th2Metadata(scriptV2.metadata.name),
            expectedConvertedScriptSpec!!
        )
        assertEquals(
            expectedConvertedScript, actualConvertedScript,
            "script conversion v1 -> v2 failed: API version, kind or metadata is incorrect"
        )
    }

    /**
     * Tests Converter.convertFromRequest(targetVersion, resources).
     * Tests scenarios where we specify unsupported version
     * And when we specify the supported one
     */
    @Test
    fun testConvertFromRequestTargetVersion() {
        assertThrows<NotAcceptableException>("Function didn't throw Exception when specified unsupported version") {
            Converter.convertFromRequest("v4", repoV1BoxesFullSet)
        }
        assertDoesNotThrow("Function threw Exception when specified supported version") {
            Converter.convertFromRequest("v2", repoV1BoxesFullSet)
        }
    }

    /**
     * Tests Converter.convertFromRequest(targetVersion, resources).
     * From V1 to V2.
     * Service subsection(of extendedSettings) conversion
     */
    @Test
    fun testServiceConversionToV2FromRequest() {
        val convertedResList = Converter.convertFromRequest("v2", setOf(actV1, fixServerV1))
        val convertedResMap = convertedResList.associateBy { it.metadata.name }
        val actSpec = convertedResMap["act"]?.spec as GenericBoxSpec
        val actualActService = YAML_MAPPER.writeValueAsString(actSpec.extendedSettings?.service)
        val expectedActService = """
            enabled: true
            nodePort:
            - name: grpc
              containerPort: 8080
              exposedPort: 30741
        """.trimIndent().plus("\n")

        assertEquals(
            expectedActService, actualActService,
            "Service conversion in act v1 -> v2 failed"
        )

        val fixServerSpec = convertedResMap["fix-server"]?.spec as GenericBoxSpec
        val actualFixServerService = YAML_MAPPER.writeValueAsString(fixServerSpec.extendedSettings?.service)
        val expectedFixServerService = """
            enabled: true
            clusterIP:
            - name: other
              containerPort: 8080
        """.trimIndent().plus("\n")

        assertEquals(
            expectedFixServerService, actualFixServerService,
            "Service conversion in fix-server v1 -> v2 failed"
        )
    }

    private fun <T> resourceListsEqual(converted: List<ComparableTo<T>>, base: List<T>): Boolean {
        if (converted.size != base.size) return false
        var isContentSame = true
        for (i in converted.indices) {
            if (!converted[i].contentEquals(base[i])) {
                isContentSame = false
                break
            }
        }
        return isContentSame
    }
}
