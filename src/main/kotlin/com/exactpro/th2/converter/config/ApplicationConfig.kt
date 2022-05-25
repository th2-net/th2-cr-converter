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

package com.exactpro.th2.converter.config

import com.exactpro.th2.converter.Mapper.YAML_MAPPER
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import mu.KotlinLogging
import org.apache.commons.text.StringSubstitutor
import org.apache.commons.text.lookup.StringLookupFactory
import java.io.FileInputStream
import javax.naming.ConfigurationException

object ApplicationConfig {

    private const val CONFIG_FILE_NAME = "/var/th2/config/converter-config.yml"

    private const val CONFIG_FILE_SYSTEM_PROPERTY = "converter.config"

    private val logger = KotlinLogging.logger { }

    var git: LocalGitConfig? = null

    init {
        loadConfiguration(this)
        if (git == null) {
            throw ConfigurationException("Bad configuration: property git is not specified in configuration file")
        }
    }

    private fun loadConfiguration(configObject: Any): ApplicationConfig {
        val path: String = System.getProperty(CONFIG_FILE_SYSTEM_PROPERTY, CONFIG_FILE_NAME)

        try {
            FileInputStream(path)
                .use { inputStream ->
                    val stringSubstitute =
                        StringSubstitutor(StringLookupFactory.INSTANCE.environmentVariableStringLookup())
                    val content = stringSubstitute.replace(String(inputStream.readAllBytes()))
                    return YAML_MAPPER.readerForUpdating(configObject).readValue(content)
                }
        } catch (e: UnrecognizedPropertyException) {
            logger.error(
                "Bad configuration: unknown property(\"{}\") specified in configuration file", e.propertyName
            )
            throw e
        } catch (e: JsonParseException) {
            logger.error("Bad configuration: exception while parsing configuration file")
            throw e
        }
    }
}
