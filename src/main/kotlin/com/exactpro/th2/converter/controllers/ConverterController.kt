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
import com.exactpro.th2.converter.controllers.errors.NotAcceptableException
import com.exactpro.th2.converter.model.Th2Resource
import com.exactpro.th2.converter.util.Converter.convertFromGit
import com.exactpro.th2.converter.util.Converter.convertFromRequest
import com.exactpro.th2.converter.util.ProjectConstants
import com.exactpro.th2.converter.util.ProjectConstants.PROPAGATION_DENY
import com.exactpro.th2.converter.util.ProjectConstants.PROPAGATION_RULE
import com.exactpro.th2.converter.util.ProjectConstants.SOURCE_BRANCH
import com.exactpro.th2.converter.util.RepositoryUtils.schemaExists
import com.exactpro.th2.converter.util.RepositoryUtils.updateRepositoryAndPush
import com.exactpro.th2.converter.util.RepositoryUtils.updateSchemaK8sPropagation
import com.exactpro.th2.infrarepo.git.GitterContext
import com.exactpro.th2.infrarepo.repo.RepositoryResource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ConverterController {

    @PutMapping("convert/{schemaName}/{targetVersion}")
    fun convertInSameBranch(
        @PathVariable schemaName: String,
        @PathVariable targetVersion: String
    ): ConverterControllerResponse {

        checkRequestedVersion(targetVersion)
        val gitterContext = GitterContext.getContext(ApplicationConfig.git)
        checkSourceSchema(schemaName, gitterContext)
        val conversionResult = convertFromGit(schemaName, targetVersion, gitterContext)
        val currentResponse = conversionResult.response
        if (currentResponse.hasErrors()) {
            return currentResponse
        }

        val gitter = gitterContext.getGitter(schemaName)
        try {
            gitter.lock()
            updateRepositoryAndPush(conversionResult, gitter)
        } finally {
            gitter.unlock()
        }
        return conversionResult.response
    }

    @PostMapping("convert/{sourceSchemaName}/{newSchemaName}/{targetVersion}")
    fun convertInNewBranch(
        @PathVariable sourceSchemaName: String,
        @PathVariable newSchemaName: String,
        @PathVariable targetVersion: String
    ): ConverterControllerResponse {

        checkRequestedVersion(targetVersion)
        val gitterContext: GitterContext = GitterContext.getContext(ApplicationConfig.git)
        checkSourceSchema(sourceSchemaName, gitterContext)
        val conversionResult = convertFromGit(sourceSchemaName, targetVersion, gitterContext)
        val currentResponse = conversionResult.response
        if (currentResponse.hasErrors()) {
            return currentResponse
        }

        val sourceBranchGitter = gitterContext.getGitter(sourceSchemaName)

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
            updateRepositoryAndPush(conversionResult, newBranchGitter)
        } finally {
            newBranchGitter.unlock()
        }
        return conversionResult.response
    }

    @GetMapping("convert/{targetVersion}")
    fun convertRequestedResources(
        @PathVariable targetVersion: String,
        @RequestBody resources: Set<RepositoryResource>
    ): List<Th2Resource> {
        checkRequestedVersion(targetVersion)
        return convertFromRequest(targetVersion, resources)
    }

    @GetMapping("test")
    fun testApi(): String {
        return "Conversion API is working !"
    }

    private fun checkRequestedVersion(version: String) {
        if (version !in ProjectConstants.ACCEPTED_VERSIONS) {
            throw NotAcceptableException("Conversion to specified version: '$version' is not supported")
        }
    }

    private fun checkSourceSchema(schemaName: String, gitterContext: GitterContext) {
        if (!schemaExists(schemaName, gitterContext)) {
            throw NotAcceptableException("Specified schema doesn't exist")
        }
        if (schemaName == SOURCE_BRANCH) {
            throw NotAcceptableException("Specified schema must not be the same as $SOURCE_BRANCH")
        }
    }
}
