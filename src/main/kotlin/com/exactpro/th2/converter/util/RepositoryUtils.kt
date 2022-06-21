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

import com.exactpro.th2.converter.controllers.errors.NotAcceptableException
import com.exactpro.th2.converter.controllers.errors.ServiceException
import com.exactpro.th2.infrarepo.InconsistentRepositoryStateException
import com.exactpro.th2.infrarepo.git.Gitter
import com.exactpro.th2.infrarepo.git.GitterContext
import com.exactpro.th2.infrarepo.repo.GenericResource
import com.exactpro.th2.infrarepo.repo.Repository
import com.exactpro.th2.infrarepo.repo.RepositoryResource
import mu.KotlinLogging
import org.springframework.http.HttpStatus

object RepositoryUtils {

    private val logger = KotlinLogging.logger { }

    fun updateRepositoryAndPush(
        conversionResult: ConversionResult,
        gitter: Gitter,
    ) {
        try {
            for (convertedRes in conversionResult.convertedResources) {
                Repository.update(
                    gitter,
                    RepositoryResource(
                        convertedRes.apiVersion,
                        convertedRes.kind,
                        GenericResource.Metadata(convertedRes.metadata.name),
                        convertedRes.spec
                    )
                )
            }
            conversionResult.response.commitRef = gitter.commitAndPush("Schema conversion")
        } catch (irse: InconsistentRepositoryStateException) {
            handleInconsistentRepoState(gitter, irse)
        } catch (e: Exception) {
            logger.error("Exception updating repository for branch \"{}\"", gitter.branch, e)
            gitter.reset()
            throw NotAcceptableException(e.message)
        }
    }

    fun updateSchemaK8sPropagation(newK8sPropagation: String, gitter: Gitter): Boolean {
        val settings = Repository.getSettings(gitter)
        if (settings.spec.k8sPropagation != newK8sPropagation) {
            settings.spec.k8sPropagation = newK8sPropagation
            try {
                Repository.update(gitter, settings)
                return true
            } catch (irse: InconsistentRepositoryStateException) {
                handleInconsistentRepoState(gitter, irse)
            } catch (e: Exception) {
                logger.error("Exception updating repository for branch \"{}\"", gitter.branch, e)
                gitter.reset()
                throw NotAcceptableException(e.message)
            }
        }
        return false
    }

    private fun handleInconsistentRepoState(gitter: Gitter, irse: InconsistentRepositoryStateException) {
        logger.error("Inconsistent repository state exception for branch \"{}\"", gitter.branch, irse)
        val se = ServiceException(HttpStatus.INTERNAL_SERVER_ERROR, irse.message)
        se.addSuppressed(irse)
        try {
            gitter.recreateCache()
        } catch (re: Exception) {
            logger.error("Exception recreating repository's local cache for branch \"{}\"", gitter.branch, re)
            se.addSuppressed(re)
        }
        throw se
    }

    fun schemaExists(schemaName: String, ctx: GitterContext): Boolean {
        val branches: Set<String> = try {
            ctx.branches
        } catch (e: Exception) {
            throw ServiceException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                e.message
            )
        }
        return branches.contains(schemaName)
    }
}
