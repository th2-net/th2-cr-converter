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

import com.exactpro.th2.converter.controllers.errors.ErrorCode
import com.exactpro.th2.converter.controllers.errors.ServiceException
import com.exactpro.th2.converter.model.Th2Resource
import com.exactpro.th2.infrarepo.InconsistentRepositoryStateException
import com.exactpro.th2.infrarepo.ResourceType
import com.exactpro.th2.infrarepo.git.Gitter
import com.exactpro.th2.infrarepo.git.GitterContext
import com.exactpro.th2.infrarepo.repo.Repository
import com.exactpro.th2.infrarepo.repo.RepositoryResource
import mu.KotlinLogging

object RepositoryUtils {

    const val PROPAGATION_RULE = "rule"
    const val PROPAGATION_DENY = "deny"

    const val SOURCE_BRANCH = "master"

    private val logger = KotlinLogging.logger { }

    fun updateRepository(
        convertedResources: List<Th2Resource>,
        gitter: Gitter,
        func: (Gitter, RepositoryResource) -> Unit,
    ) {
        try {
            for (convertedRes in convertedResources) {
                func(
                    gitter,
                    RepositoryResource(
                        convertedRes.apiVersion,
                        convertedRes.kind,
                        convertedRes.metadata,
                        convertedRes.specWrapper.getSpecObject()
                    )
                )
            }
        } catch (irse: InconsistentRepositoryStateException) {
            handleInconsistentRepoState(gitter, irse)
        } catch (e: Exception) {
            logger.error("Exception updating repository for branch \"{}\"", gitter.branch, e)
            gitter.reset()
            throw ServiceException(
                ErrorCode.REPOSITORY_ERROR,
                e.message
            )
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
                throw ServiceException(
                    ErrorCode.REPOSITORY_ERROR,
                    e.message
                )
            }
        }
        return false
    }

    private fun handleInconsistentRepoState(gitter: Gitter, irse: InconsistentRepositoryStateException) {
        logger.error("Inconsistent repository state exception for branch \"{}\"", gitter.branch, irse)
        val se = ServiceException(ErrorCode.REPOSITORY_ERROR, irse.message)
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
                ErrorCode.UNKNOWN_ERROR,
                e.message
            )
        }
        return branches.contains(schemaName)
    }
}

data class RepositoryContext(
    val boxes: Set<RepositoryResource>,
    val links: Set<RepositoryResource>
) {
    val allResources = boxes.plus(links)

    companion object {
        fun load(gitter: Gitter): RepositoryContext {
            try {
                gitter.lock()
                val boxes = HashSet(Repository.getAllBoxesAndStores(gitter))
                val links = HashSet(Repository.getResourcesByKind(gitter, ResourceType.Th2Link))
                return RepositoryContext(boxes, links)
            } finally {
                gitter.unlock()
            }
        }
    }
}
