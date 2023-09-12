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

package com.exactpro.th2.converter.conversion

import com.exactpro.th2.converter.controllers.ConversionResult
import com.exactpro.th2.converter.controllers.ConversionSummary
import com.exactpro.th2.converter.controllers.ErrorMessage
import com.exactpro.th2.converter.controllers.errors.BadRequestException
import com.exactpro.th2.converter.controllers.errors.ErrorCode
import com.exactpro.th2.converter.`fun`.Convertible
import com.exactpro.th2.converter.`fun`.ConvertibleBoxSpecV1
import com.exactpro.th2.converter.`fun`.ConvertibleBoxSpecV2
import com.exactpro.th2.converter.`fun`.ConvertibleBoxSpecV2x2
import com.exactpro.th2.converter.model.Th2Resource
import com.exactpro.th2.converter.util.Mapper.YAML_MAPPER
import com.exactpro.th2.converter.util.RepositoryContext
import com.exactpro.th2.converter.util.SchemaVersion
import com.exactpro.th2.infrarepo.ResourceType
import com.exactpro.th2.infrarepo.git.Gitter
import com.exactpro.th2.infrarepo.repo.Repository
import com.exactpro.th2.infrarepo.repo.RepositoryResource
import com.exactpro.th2.model.latest.box.Spec
import com.exactpro.th2.model.v1.box.SpecV1
import com.exactpro.th2.model.v2.SpecV2
import com.fasterxml.jackson.module.kotlin.convertValue

object Converter {
    fun convertFromGit(
        currentVersion: SchemaVersion,
        targetVersion: SchemaVersion,
        sourceBranchGitter: Gitter,
        newBranchGitter: Gitter? = null
    ): ConversionResult {
        val summary = ConversionSummary()
        val repositoryContext = RepositoryContext.load(sourceBranchGitter)

        if (!validateCurrentSchemaVersion(repositoryContext.allResources, currentVersion.apiVersion, summary)) {
            return ConversionResult(summary, emptyList())
        }

        val gitter = newBranchGitter ?: sourceBranchGitter
        return processSwitching(currentVersion, targetVersion, repositoryContext, summary, gitter)
    }

    fun convertFromRequest(
        currentVersion: SchemaVersion,
        targetVersion: SchemaVersion,
        resources: Set<RepositoryResource>
    ): ConversionResult {
        val summary = ConversionSummary()
        val linkKind = ResourceType.Th2Link.kind()
        val boxKinds = setOf<String>(
            ResourceType.Th2Box.kind(),
            ResourceType.Th2CoreBox.kind(),
            ResourceType.Th2Estore.kind(),
            ResourceType.Th2Mstore.kind()
        )

        val boxes = resources.filterTo(HashSet()) { boxKinds.contains(it.kind) }
        val links = resources.filterTo(HashSet()) { it.kind.equals(linkKind) }

        val repositoryContext = RepositoryContext(boxes, links)
        if (!validateCurrentSchemaVersion(repositoryContext.allResources, currentVersion.apiVersion, summary)) {
            return ConversionResult(summary, emptyList())
        }

        return processSwitching(currentVersion, targetVersion, repositoryContext, summary)
    }

    fun convertLocal(
        currentVersion: SchemaVersion,
        targetVersion: SchemaVersion,
        gitter: Gitter
    ): ConversionResult {
        val summary = ConversionSummary()

        val boxes = Repository.getAllBoxesAndStores(gitter, false)
        val links = Repository.getResourcesByKind(gitter, ResourceType.Th2Link, false)
        val repositoryContext = RepositoryContext(boxes, links)

        if (!validateCurrentSchemaVersion(repositoryContext.allResources, currentVersion.apiVersion, summary)) {
            return ConversionResult(summary, emptyList())
        }

        return processSwitching(currentVersion, targetVersion, repositoryContext, summary)
    }

    private fun processSwitching(
        currentVersion: SchemaVersion,
        targetVersion: SchemaVersion,
        repositoryContext: RepositoryContext,
        summary: ConversionSummary,
        gitter: Gitter? = null,
    ): ConversionResult {
        when (targetVersion) {
            SchemaVersion.V1 -> throw BadRequestException(
                ErrorCode.VERSION_NOT_ALLOWED,
                "Conversion to specified version: '${SchemaVersion.V1}' is not supported"
            )

            SchemaVersion.V2 -> {
                if (currentVersion != SchemaVersion.V1) {
                    throw BadRequestException(
                        ErrorCode.VERSION_NOT_ALLOWED,
                        """Conversion to v2 is only allowed from v1.specified. 2
                            |specified current version: '$currentVersion' is not supported
                        """.trimMargin()
                    )
                }
                val convertedResources = convert<SpecV1>(repositoryContext.boxes, summary)
                val linksInserter = LinksInserter()
                linksInserter.insertLinksIntoBoxes(convertedResources, repositoryContext.links)
                linksInserter.addErrorsToSummary(summary)
                if (gitter != null) {
                    Repository.removeLinkResources(gitter)
                }
                return ConversionResult(summary, convertedResources)
            }

            SchemaVersion.V2_2 -> {
                when (currentVersion) {
                    SchemaVersion.V1 -> {
                        var convertedResources = convert<SpecV1>(repositoryContext.boxes, summary)
                        val linksInserter = LinksInserter()
                        linksInserter.insertLinksIntoBoxes(convertedResources, repositoryContext.links)
                        linksInserter.addErrorsToSummary(summary)
                        if (gitter != null) {
                            Repository.removeLinkResources(gitter)
                        }
                        val repositoryResourcesV2 = convertedResources.map(Th2Resource::toRepositoryResource)
                        convertedResources = convert<SpecV2>(repositoryResourcesV2, summary)
                        return ConversionResult(summary, convertedResources)
                    }

                    SchemaVersion.V2 -> {
                        val convertedResources = convert<SpecV2>(repositoryContext.boxes, summary)
                        return ConversionResult(summary, convertedResources)
                    }

                    else -> {
                        throw BadRequestException(
                            ErrorCode.VERSION_NOT_ALLOWED,
                            """Conversion to v2-2 is only allowed from v1 or v2.specified. 
                                        |specified current version: '$currentVersion' is not supported
                            """.trimMargin()
                        )
                    }
                }
            }
        }
    }

    private inline fun <reified From> convert(
        resources: Collection<RepositoryResource>,
        summary: ConversionSummary
    ): List<Th2Resource> {
        val convertedResources: MutableList<Th2Resource> = ArrayList()
        for (resource in resources) {
            try {
                val specFrom: From = YAML_MAPPER.convertValue(resource.spec)
                val resourceFrom = Th2Resource(
                    SchemaVersion.fromApiVersion(resource.apiVersion),
                    resource.kind,
                    resource.metadata,
                    wrap(specFrom)
                )
                convertedResources.add(resourceFrom.toNextVersion())
                summary.convertedResourceNames.add(resource.metadata.name)
            } catch (e: Exception) {
                summary.errorMessages.add(ErrorMessage(resource.metadata.name, e.message))
            }
        }
        return convertedResources
    }

    private fun validateCurrentSchemaVersion(
        resources: Set<RepositoryResource>,
        currentVersion: String,
        summary: ConversionSummary
    ): Boolean {
        var isValid = true
        resources.forEach { resource ->
            if (currentVersion != resource.apiVersion) {
                isValid = false
                summary.errorMessages.add(
                    ErrorMessage(
                        resource.metadata.name,
                        "Resource api version ${resource.version}. is different requested api version: $currentVersion"
                    )
                )
            }
        }
        return isValid
    }

    private fun <From> wrap(specFrom: From): Convertible {
        return when (specFrom) {
            is SpecV1 -> ConvertibleBoxSpecV1(specFrom)
            is SpecV2 -> ConvertibleBoxSpecV2(specFrom)
            is Spec -> ConvertibleBoxSpecV2x2(specFrom)
            else -> throw AssertionError("Provided spec class is not supported for conversion")
        }
    }
}
