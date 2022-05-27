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

package com.exactpro.th2.converter.controllers

import com.exactpro.th2.converter.Mapper.YAML_MAPPER
import com.exactpro.th2.converter.ProjectConstants.API_VERSION_V1
import com.exactpro.th2.converter.ProjectConstants.API_VERSION_V2
import com.exactpro.th2.converter.ProjectConstants.SHORT_API_VERSION_V2
import com.exactpro.th2.converter.config.ApplicationConfig
import com.exactpro.th2.converter.controllers.errors.NotAcceptableException
import com.exactpro.th2.converter.controllers.errors.ServiceException
import com.exactpro.th2.converter.model.Convertible
import com.exactpro.th2.converter.model.GenericResource
import com.exactpro.th2.converter.model.latest.Th2Metadata
import com.exactpro.th2.converter.model.latest.box.GenericBoxSpec
import com.exactpro.th2.converter.model.latest.link.GenericLinkSpec
import com.exactpro.th2.converter.model.v1.box.GenericBoxSpecV1
import com.exactpro.th2.converter.model.v1.link.GenericLinkSpecV1
import com.exactpro.th2.infrarepo.Gitter
import com.exactpro.th2.infrarepo.GitterContext
import com.exactpro.th2.infrarepo.InconsistentRepositoryStateException
import com.exactpro.th2.infrarepo.Repository
import com.exactpro.th2.infrarepo.RepositoryResource
import com.exactpro.th2.infrarepo.ResourceType
import com.fasterxml.jackson.module.kotlin.convertValue
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ConverterController {
    private val logger = KotlinLogging.logger { }

    @PutMapping("convert/{schemaName}/{version}")
    fun convert(
        @PathVariable(name = "schemaName") schemaName: String,
        @PathVariable(name = "version") version: String
    ): ConverterControllerResponse {
        checkRequestedVersion(version)
        val gitterContext: GitterContext = GitterContext.getContext(ApplicationConfig.git)
        if (!schemaExists(schemaName, gitterContext)) {
            throw NotAcceptableException("Specified schema doesn't exist")
        }
        val gitter = gitterContext.getGitter(schemaName)
        try {
            gitter.lock()
            when (version) {
                SHORT_API_VERSION_V2 -> {
                    val boxesToConvert = HashSet(Repository.getAllBoxesAndStores(gitter))
                    val linksToConvert = HashSet(Repository.getResourcesByKind(gitter, ResourceType.Th2Link))

                    val conversionContext = ConversionContext()
                    val response = ConverterControllerResponse()
                    convertToV2<GenericBoxSpecV1, GenericBoxSpec>(boxesToConvert, conversionContext, response)
                    convertToV2<GenericLinkSpecV1, GenericLinkSpec>(linksToConvert, conversionContext, response)
                    updateRepository(conversionContext, gitter, response)
                    return response
                }
                else -> throw NotAcceptableException("Conversion to specified version: '$version' is not supported")
            }
        } finally {
            gitter.unlock()
        }
    }

    private inline fun <reified From : Convertible, reified Target : Convertible> convertToV2(
        resources: Set<RepositoryResource>,
        conversionContext: ConversionContext,
        response: ConverterControllerResponse
    ) {
        for (resource in resources) {
            val th2Metadata: Th2Metadata = YAML_MAPPER.convertValue(resource.metadata)

            if (resource.apiVersion.equals(API_VERSION_V2)) {
                try {
                    val spec: Target = YAML_MAPPER.convertValue(resource.spec)
                    conversionContext.alreadyUpToVersionResources.add(
                        GenericResource(
                            resource.apiVersion,
                            resource.kind,
                            th2Metadata,
                            spec
                        )
                    )
                } catch (e: Exception) {
                    response.errorMessages.add(ErrorMessage(resource.metadata.name, e.message))
                } finally {
                    continue
                }
            }
            if (resource.apiVersion.equals(API_VERSION_V1)) {
                try {
                    val specFrom: From = YAML_MAPPER.convertValue(resource.spec)
                    val resourceFrom = GenericResource(resource.apiVersion, resource.kind, th2Metadata, specFrom)
                    conversionContext.convertedResources.add(resourceFrom.toNextVersion())
                    response.convertedResources.add(resource.metadata.name)
                } catch (e: Exception) {
                    response.errorMessages.add(ErrorMessage(resource.metadata.name, e.message))
                }
            }
        }
    }

    private fun updateRepository(
        conversionContext: ConversionContext,
        gitter: Gitter,
        response: ConverterControllerResponse
    ) {
        try {
            for (convertedRes in conversionContext.convertedResources) {
                Repository.update(
                    gitter,
                    RepositoryResource(
                        convertedRes.getApiVersion(),
                        convertedRes.getKind(),
                        RepositoryResource.Metadata(convertedRes.getMetadata().name),
                        convertedRes.getSpec()
                    )
                )
            }
            response.commitRef = gitter.commitAndPush("Schema conversion")
        } catch (irse: InconsistentRepositoryStateException) {
            logger.error("Inconsistent repository state exception for branch \"{}\"", gitter.branch, irse)
            val se = ServiceException(HttpStatus.INTERNAL_SERVER_ERROR.value(), irse.message)
            se.addSuppressed(irse)
            try {
                gitter.recreateCache()
            } catch (re: Exception) {
                logger.error("Exception recreating repository's local cache for branch \"{}\"", gitter.branch, re)
                se.addSuppressed(re)
            }
            throw se
        } catch (e: Exception) {
            logger.error("Exception updating repository for branch \"{}\"", gitter.branch, e)
            gitter.reset()
            throw NotAcceptableException(e.message)
        }
    }

    private fun schemaExists(schemaName: String, ctx: GitterContext): Boolean {
        val branches: Set<String> = try {
            ctx.branches
        } catch (e: Exception) {
            throw ServiceException(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                e.message
            )
        }
        return branches.contains(schemaName)
    }

    private fun checkRequestedVersion(version: String) {
        if (version != SHORT_API_VERSION_V2) {
            throw NotAcceptableException("Conversion to specified version: '$version' is not supported")
        }
    }

    @GetMapping("test")
    fun testApi(): String {
        return "Conversion API is working !"
    }
}
