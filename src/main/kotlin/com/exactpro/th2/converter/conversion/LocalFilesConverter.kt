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

import com.exactpro.th2.converter.config.ApplicationConfig
import com.exactpro.th2.converter.model.Th2Resource
import com.exactpro.th2.converter.util.RepositoryUtils.updateRepository
import com.exactpro.th2.converter.util.SchemaVersion
import com.exactpro.th2.infrarepo.ResourceType
import com.exactpro.th2.infrarepo.git.GitterContext
import com.exactpro.th2.infrarepo.repo.Repository
import com.exactpro.th2.infrarepo.repo.RepositoryResource
import mu.KotlinLogging

class LocalFilesConverter(
    private val schema: String,
    private val currentVersion: SchemaVersion,
    private val targetVersion: SchemaVersion
) {
    private val logger = KotlinLogging.logger {}
    private val gitterContext = GitterContext.getContext(ApplicationConfig.git)

    fun convert() {
        val gitter = gitterContext.getGitter(schema)
        val result = Converter.convertLocal(currentVersion, targetVersion, gitter)
        val dictionaries = Repository.getResourcesByKind(gitter, ResourceType.Th2Dictionary, false)
        if (result.summary.hasErrors()) {
            logger.error("Conversion for schema {} failed due to following errors", schema)
            result.summary.errorMessages.forEach { logger.error("${it.resourceName} - ${it.error}") }
            return
        }
        logger.info("Conversion for Schema {} was successful, saving results", schema)
        saveConvertedSchema(result.convertedResources, dictionaries)
    }

    private fun saveConvertedSchema(convertedResources: List<Th2Resource>, dictionaries: Set<RepositoryResource>) {
        val convertedSchemaGitter = gitterContext.getGitter("$schema-converted")
        updateRepository(convertedResources, convertedSchemaGitter, Repository::add)
        for (dictionary in dictionaries) {
            Repository.add(convertedSchemaGitter, dictionary)
        }
    }
}
