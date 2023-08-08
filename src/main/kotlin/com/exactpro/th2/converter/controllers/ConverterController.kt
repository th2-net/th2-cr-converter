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

import com.exactpro.th2.converter.config.ApplicationConfig
import com.exactpro.th2.converter.controllers.errors.BadRequestException
import com.exactpro.th2.converter.controllers.errors.ErrorCode
import com.exactpro.th2.converter.conversion.Converter.convertFromGit
import com.exactpro.th2.converter.conversion.Converter.convertFromRequest
import com.exactpro.th2.converter.util.RepositoryUtils.PROPAGATION_DENY
import com.exactpro.th2.converter.util.RepositoryUtils.PROPAGATION_RULE
import com.exactpro.th2.converter.util.RepositoryUtils.SOURCE_BRANCH
import com.exactpro.th2.converter.util.RepositoryUtils.schemaExists
import com.exactpro.th2.converter.util.RepositoryUtils.updateRepository
import com.exactpro.th2.converter.util.RepositoryUtils.updateSchemaK8sPropagation
import com.exactpro.th2.converter.util.SchemaVersion
import com.exactpro.th2.infrarepo.git.GitterContext
import com.exactpro.th2.infrarepo.repo.Repository
import com.exactpro.th2.infrarepo.repo.RepositoryResource
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import org.eclipse.jgit.errors.EntryExistsException

@Controller
@Produces(MediaType.APPLICATION_JSON)
class ConverterController {

    @Post("convert/{schemaName}/{currentVersion}/{targetVersion}")
    fun convertInSameBranch(
        schemaName: String,
        currentVersion: SchemaVersion,
        targetVersion: SchemaVersion
    ): ConversionSummary {
        val gitterContext = GitterContext.getContext(ApplicationConfig.git)
        checkSourceSchema(schemaName, gitterContext)
        val gitter = gitterContext.getGitter(schemaName)
        val conversionResult = convertFromGit(currentVersion, targetVersion, gitter)
        val currentResponse = conversionResult.summary
        if (currentResponse.hasErrors()) {
            return currentResponse
        }

        try {
            gitter.lock()
            Repository.removeLinkResources(gitter)
            updateRepository(conversionResult.convertedResources, gitter, Repository::update)
            conversionResult.summary.commitRef = gitter.commitAndPush("Schema conversion")
        } finally {
            gitter.unlock()
        }
        return conversionResult.summary
    }

    @Post("convert/{sourceSchemaName}/{newSchemaName}/{currentVersion}/{targetVersion}")
    fun convertInNewBranch(
        sourceSchemaName: String,
        newSchemaName: String,
        currentVersion: SchemaVersion,
        targetVersion: SchemaVersion
    ): ConversionSummary {
        val gitterContext: GitterContext = GitterContext.getContext(ApplicationConfig.git)
        checkSourceSchema(sourceSchemaName, gitterContext)
        val sourceBranchGitter = gitterContext.getGitter(sourceSchemaName)
        val conversionResult = convertFromGit(currentVersion, targetVersion, sourceBranchGitter)
        val currentResponse = conversionResult.summary
        if (currentResponse.hasErrors()) {
            return currentResponse
        }

        try {
            sourceBranchGitter.lock()
            if (updateSchemaK8sPropagation(PROPAGATION_DENY, sourceBranchGitter)) {
                sourceBranchGitter.commitAndPush("set k8s-propagation to deny for the schema '$sourceSchemaName'")
            }
        } finally {
            sourceBranchGitter.unlock()
        }

        val newBranchGitter = gitterContext.getGitter(newSchemaName)
        try {
            newBranchGitter.lock()
            newBranchGitter.createBranch(sourceSchemaName)
            updateSchemaK8sPropagation(PROPAGATION_RULE, newBranchGitter)
            Repository.removeLinkResources(newBranchGitter)
            updateRepository(conversionResult.convertedResources, newBranchGitter, Repository::update)
            conversionResult.summary.commitRef = newBranchGitter.commitAndPush("Schema conversion")
        } catch (e: EntryExistsException) {
            throw BadRequestException(
                ErrorCode.BRANCH_ALREADY_EXISTS,
                "Branch '$newSchemaName' already exists on git"
            )
        } finally {
            newBranchGitter.unlock()
        }
        return conversionResult.summary
    }

    @Get("convert/{currentVersion}/{targetVersion}")
    fun convertRequestedResources(
        currentVersion: SchemaVersion,
        targetVersion: SchemaVersion,
        @Body resources: Set<RepositoryResource>
    ): ConversionResult {
        return convertFromRequest(currentVersion, targetVersion, resources)
    }

    @Get("test")
    fun testApi(): String {
        return "Conversion API is working !"
    }

    private fun checkSourceSchema(schemaName: String, gitterContext: GitterContext) {
        if (!schemaExists(schemaName, gitterContext)) {
            throw BadRequestException(
                ErrorCode.BRANCH_NOT_FOUND,
                "Specified schema doesn't exist"
            )
        }
        if (schemaName == SOURCE_BRANCH) {
            throw BadRequestException(
                ErrorCode.BRANCH_NOT_ALLOWED,
                "Specified schema must not be the same as $SOURCE_BRANCH"
            )
        }
    }
}
