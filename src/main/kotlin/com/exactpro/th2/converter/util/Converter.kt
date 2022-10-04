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

import com.exactpro.th2.converter.controllers.ConversionResult
import com.exactpro.th2.converter.controllers.ConversionSummary
import com.exactpro.th2.converter.controllers.ErrorMessage
import com.exactpro.th2.converter.controllers.errors.NotAcceptableException
import com.exactpro.th2.converter.`fun`.Convertible
import com.exactpro.th2.converter.`fun`.ConvertibleBoxSpecV1
import com.exactpro.th2.converter.`fun`.ConvertibleBoxSpecV2
import com.exactpro.th2.converter.model.Th2Resource
import com.exactpro.th2.converter.util.Mapper.YAML_MAPPER
import com.exactpro.th2.converter.util.ProjectConstants.API_VERSION_V1
import com.exactpro.th2.converter.util.ProjectConstants.SHORT_API_VERSION_V2
import com.exactpro.th2.infrarepo.ResourceType
import com.exactpro.th2.infrarepo.git.GitterContext
import com.exactpro.th2.infrarepo.repo.Repository
import com.exactpro.th2.infrarepo.repo.RepositoryResource
import com.exactpro.th2.model.latest.box.Spec
import com.exactpro.th2.model.v1.box.SpecV1
import com.fasterxml.jackson.module.kotlin.convertValue

object Converter {

    fun convertFromGit(
        sourceSchema: String,
        version: String,
        gitterContext: GitterContext
    ): ConversionResult {
        val gitter = gitterContext.getGitter(sourceSchema)
        val summary = ConversionSummary()
        val convertedResources: List<Th2Resource>

        when (version) {
            SHORT_API_VERSION_V2 -> {
                val boxesToConvert: Set<RepositoryResource>
                val links: Set<RepositoryResource>
                try {
                    gitter.lock()
                    boxesToConvert = HashSet(Repository.getAllBoxesAndStores(gitter))
                    links = HashSet(Repository.getResourcesByKind(gitter, ResourceType.Th2Link))
                } finally {
                    gitter.unlock()
                }
                convertedResources = convert<SpecV1>(boxesToConvert, API_VERSION_V1, summary)

                val linksInserter = LinksInserter()
                linksInserter.insertLinksIntoBoxes(convertedResources, links)
                linksInserter.addErrorsToSummary(summary)
                return ConversionResult(summary, convertedResources)
            }
            else -> throw NotAcceptableException("Conversion to specified version: '$version' is not supported")
        }
    }

    fun convertFromRequest(
        targetVersion: String,
        resources: Set<RepositoryResource>
    ): ConversionResult {
        val summary = ConversionSummary()
        val convertedResources: List<Th2Resource>

        when (targetVersion) {
            SHORT_API_VERSION_V2 -> {
                val linkKind = ResourceType.Th2Link.kind()
                val boxKinds = setOf<String>(
                    ResourceType.Th2Box.kind(),
                    ResourceType.Th2CoreBox.kind(),
                    ResourceType.Th2Estore.kind(),
                    ResourceType.Th2Mstore.kind()
                )

                val boxesToConvert = resources.filterTo(HashSet()) { boxKinds.contains(it.kind) }
                convertedResources = convert<SpecV1>(boxesToConvert, API_VERSION_V1, summary)

                val links = resources.filterTo(HashSet()) { it.kind.equals(linkKind) }
                val linksInserter = LinksInserter()
                linksInserter.insertLinksIntoBoxes(convertedResources, links)
                linksInserter.addErrorsToSummary(summary)
            }
            else -> throw NotAcceptableException("Conversion to specified version: '$targetVersion' is not supported")
        }
        return ConversionResult(summary, convertedResources)
    }

    private inline fun <reified From> convert(
        resources: Set<RepositoryResource>,
        fromVersion: String,
        summary: ConversionSummary
    ): List<Th2Resource> {
        val convertedResources: MutableList<Th2Resource> = ArrayList()
        for (resource in resources) {

            if (!resource.apiVersion.equals(fromVersion)) {
                summary.errorMessages.add(
                    ErrorMessage(
                        resource.metadata.name,
                        "Resource must be of version: $fromVersion"
                    )
                )
                continue
            }

            try {
                val specFrom: From = YAML_MAPPER.convertValue(resource.spec)
                val resourceFrom = Th2Resource(resource.apiVersion, resource.kind, resource.metadata, wrap(specFrom))
                convertedResources.add(resourceFrom.toNextVersion())
                summary.convertedResourceNames.add(resource.metadata.name)
            } catch (e: Exception) {
                summary.errorMessages.add(ErrorMessage(resource.metadata.name, e.message))
            }
        }
        return convertedResources
    }

    private fun <From> wrap(specFrom: From): Convertible {
        return when (specFrom) {
            is SpecV1 -> ConvertibleBoxSpecV1(specFrom)
            is Spec -> ConvertibleBoxSpecV2(specFrom)
            else -> throw AssertionError("Provided spec class is not supported for conversion")
        }
    }
}
