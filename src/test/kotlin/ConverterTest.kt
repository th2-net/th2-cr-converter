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
        const val ACT = "act.yml"
        const val CHECK1 = "check1.yml"
        const val FIX_SERVER = "fix-server.yml"
        const val SCRIPT = "script.yml"
    }

    private val actV1 = readV1RepoResource(ACT)

    private val actV2 = readV2RepoResource(ACT)

    private val check1V1 = readV1RepoResource(CHECK1)

    private val fixServerV1 = readV1RepoResource(FIX_SERVER)

    private val fixServerV2 = readV2RepoResource(FIX_SERVER)

    private val scriptV1 = readV1RepoResource(SCRIPT)

    private val scriptV2 = readV2RepoResource(SCRIPT)

    private val links1 = readV1RepoResource("links1.yml")

    private val links2 = readV1RepoResource("links2.yml")

    private val repoV1BoxesFullSet = sortedSetOf(
        Comparator.comparing { res -> res.metadata.name },
        actV1, check1V1, fixServerV1, scriptV1
    )

    /**
     * Tests Converter.convertFromRequest(targetVersion, resources)
     * From V1 to V2.
     * Pin conversion.
     */
    @Test
    fun testPinsConversionToV2FromRequest() {
        val convertedTh2ResList = Converter.convertFromRequest("v2", repoV1BoxesFullSet)
            .convertedResources
            .sortedBy { it.metadata.name }

        for ((index, v1Res) in repoV1BoxesFullSet.withIndex()) {
            val resFailMessage = v1Res.metadata.name.plus(" conversion v1 -> v2 failed: ")

            val v1ResSpec: GenericBoxSpecV1 = YAML_MAPPER.convertValue(v1Res.spec)
            val v1ResPins = v1ResSpec.pins as List<PinSpecV1> // not testing null pins

            val convertedResSpec = convertedTh2ResList[index].spec as GenericBoxSpec
            val convertedResPins: PinSpec? = convertedResSpec.pins

            assertNotNull(convertedResPins, resFailMessage + "Pin conversion resulted in null")

            val v1MqSubPins: List<PinSpecV1> = v1ResPins
                .filter { it.connectionType == PinType.MQ.value }
                .filter { it.attributes?.contains("publish") == false }

            val v1MqPublisherPins = v1ResPins
                .filter { it.connectionType == PinType.MQ.value }
                .filter { it.attributes?.contains("publish") == true }

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
    fun testConvertFromRequestToV2EntireYAMLStructure() {
        val sampleResSet = setOf(scriptV1, links1, links2)

        val failMessage = "script conversion v1 -> v2 failed"
        val actualConvertedList = Converter.convertFromRequest("v2", sampleResSet).convertedResources
        val actualConvertedScript = actualConvertedList
            .find { it.metadata.name == "script" } as Th2Resource
        var expectedConvertedScriptSpec: GenericBoxSpec? = null
        assertDoesNotThrow("$failMessage: Spec structure or naming is incorrect") {
            expectedConvertedScriptSpec = YAML_MAPPER.convertValue(scriptV2.spec)
        }
        val expectedConvertedScript = Th2Resource(
            scriptV2.apiVersion,
            scriptV2.kind,
            Th2Metadata(scriptV2.metadata.name),
            expectedConvertedScriptSpec!!
        )

        data class Meta(val apiVersion: String, val kind: String, val name: Th2Metadata)

        assertEquals(
            Meta(expectedConvertedScript.apiVersion, expectedConvertedScript.kind, expectedConvertedScript.metadata),
            Meta(actualConvertedScript.apiVersion, actualConvertedScript.kind, actualConvertedScript.metadata),
            "$failMessage: API version, kind or metadata is incorrect"
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
        val convertedResList = Converter.convertFromRequest("v2", setOf(actV1, fixServerV1)).convertedResources
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

    /**
     * Tests whether LinkTo section is correctly added to the boxes where due
     * (either in MQ subscribers or gRPC clients),
     * according to V1 link files
     */
    @Test
    fun testV2LinksInBoxes() {
        val repoV1AllResourcesSet = HashSet(repoV1BoxesFullSet)
        repoV1AllResourcesSet.add(links1)
        repoV1AllResourcesSet.add(links2)
        val convertedResList = Converter.convertFromRequest("v2", repoV1AllResourcesSet).convertedResources
        val convertedResMap = convertedResList.associateBy { it.metadata.name }

        val actualFixServerSpec = convertedResMap["fix-server"]?.spec as GenericBoxSpec
        val actualFixServerLinks = actualFixServerSpec.pins?.mq?.subscribers
            ?.map { sub -> sub.linkTo?.toSet() }
        val expectedFixServerSpec = readV2ResourceSpec(fixServerV2)
        val expectedFixServerLinks = expectedFixServerSpec.pins?.mq?.subscribers
            ?.map { sub -> sub.linkTo?.toSet() }

        assertEquals(
            expectedFixServerLinks, actualFixServerLinks,
            "Links were not added properly in fix-server"
        )

        val actualActSpec = convertedResMap["act"]?.spec as GenericBoxSpec
        val actualActLinks = actualActSpec.pins?.grpc?.client
            ?.map { client -> client.linkTo?.toSet() }
        val expectedActSpec = readV2ResourceSpec(actV2)
        val expectedActLinks = expectedActSpec.pins?.grpc?.client
            ?.map { client -> client.linkTo?.toSet() }

        assertEquals(
            expectedActLinks, actualActLinks,
            "Links were not added properly in act"
        )

        val actualScriptSpec = convertedResMap["script"]?.spec as GenericBoxSpec
        val actualScriptLinks = actualScriptSpec.pins?.grpc?.client
            ?.map { client -> client.linkTo?.toSet() }
        val expectedScriptSpec = readV2ResourceSpec(scriptV2)
        val expectedScriptLinks = expectedScriptSpec.pins?.grpc?.client
            ?.map { client -> client.linkTo?.toSet() }

        assertEquals(
            expectedScriptLinks, actualScriptLinks,
            "Links were not added properly in script"
        )
    }

    private fun readRepoResource(path: String): RepositoryResource {
        return YAML_MAPPER.readValue(
            File(path), RepositoryResource::class.java
        )
    }

    private fun readV2ResourceSpec(res: RepositoryResource): GenericBoxSpec {
        return YAML_MAPPER.convertValue(res.spec, GenericBoxSpec::class.java)
    }

    private fun readV1RepoResource(fileName: String): RepositoryResource {
        return readRepoResource(V1_PATH + fileName)
    }

    private fun readV2RepoResource(fileName: String): RepositoryResource {
        return readRepoResource(V2_PATH + fileName)
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
