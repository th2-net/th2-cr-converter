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

package com.exactpro.th2.converter.util

import com.exactpro.th2.converter.controllers.ConverterControllerResponse
import com.exactpro.th2.converter.controllers.ErrorMessage
import com.exactpro.th2.converter.controllers.errors.NotAcceptableException
import com.exactpro.th2.converter.model.Convertible
import com.exactpro.th2.converter.model.GenericResource
import com.exactpro.th2.converter.model.Th2Resource
import com.exactpro.th2.converter.model.latest.Th2Metadata
import com.exactpro.th2.converter.model.latest.box.GenericBoxSpec
import com.exactpro.th2.converter.model.latest.link.GenericLinkSpec
import com.exactpro.th2.converter.model.v1.box.GenericBoxSpecV1
import com.exactpro.th2.converter.model.v1.link.GenericLinkSpecV1
import com.exactpro.th2.converter.util.Mapper.YAML_MAPPER
import com.exactpro.th2.converter.util.ProjectConstants.SHORT_API_VERSION_V2
import com.exactpro.th2.infrarepo.ResourceType
import com.exactpro.th2.infrarepo.git.GitterContext
import com.exactpro.th2.infrarepo.repo.Repository
import com.exactpro.th2.infrarepo.repo.RepositoryResource
import com.fasterxml.jackson.module.kotlin.convertValue

object Converter {

    fun convertFromGit(
        sourceSchema: String,
        version: String,
        gitterContext: GitterContext
    ): ConversionResult {
        val gitter = gitterContext.getGitter(sourceSchema)

        when (version) {
            SHORT_API_VERSION_V2 -> {
                val boxesToConvert: Set<RepositoryResource>
                val linksToConvert: Set<RepositoryResource>
                try {
                    gitter.lock()
                    boxesToConvert = HashSet(Repository.getAllBoxesAndStores(gitter))
                    linksToConvert = HashSet(Repository.getResourcesByKind(gitter, ResourceType.Th2Link))
                } finally {
                    gitter.unlock()
                }
                val conversionContext = ConversionContext()
                val response = ConverterControllerResponse()
                convert<GenericBoxSpecV1, GenericBoxSpec>(boxesToConvert, conversionContext, response)
                convert<GenericLinkSpecV1, GenericLinkSpec>(linksToConvert, conversionContext, response)

                return ConversionResult(conversionContext.convertedResources, response)
            }
            else -> throw NotAcceptableException("Conversion to specified version: '$version' is not supported")
        }
    }

    fun convertFromRequest(
        targetVersion: String,
        resources: Set<RepositoryResource>
    ): List<Th2Resource> {
        val conversionContext = ConversionContext()
        val response = ConverterControllerResponse()
        when (targetVersion) {
            SHORT_API_VERSION_V2 -> {
                val linkKind = ResourceType.Th2Link.kind()
                val boxKinds = setOf<String>(
                    ResourceType.Th2Box.kind(),
                    ResourceType.Th2CoreBox.kind(),
                    ResourceType.Th2Estore.kind(),
                    ResourceType.Th2Mstore.kind()
                )

                val linksToConvert = resources.filterTo(HashSet()) { it.kind.equals(linkKind) }
                convert<GenericLinkSpecV1, GenericLinkSpec>(linksToConvert, conversionContext, response)
                val boxesToConvert = resources.filterTo(HashSet()) { boxKinds.contains(it.kind) }
                convert<GenericBoxSpecV1, GenericBoxSpec>(boxesToConvert, conversionContext, response)
            }
            else -> throw NotAcceptableException("Conversion to specified version: '$targetVersion' is not supported")
        }
        return conversionContext.convertedResources
    }

    private inline fun <reified From : Convertible, reified Target : Convertible> convert(
        resources: Set<RepositoryResource>,
        conversionContext: ConversionContext,
        response: ConverterControllerResponse
    ) {
        for (resource in resources) {
            val th2Metadata: Th2Metadata = YAML_MAPPER.convertValue(resource.metadata)

            if (resource.apiVersion.equals(ProjectConstants.API_VERSION_V2)) {
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
            if (resource.apiVersion.equals(ProjectConstants.API_VERSION_V1)) {
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
}

data class ConversionResult(val convertedResources: List<Th2Resource>, val response: ConverterControllerResponse)

data class ConversionContext(
    val alreadyUpToVersionResources: MutableList<Th2Resource> = ArrayList(),
    val convertedResources: MutableList<Th2Resource> = ArrayList()
)
