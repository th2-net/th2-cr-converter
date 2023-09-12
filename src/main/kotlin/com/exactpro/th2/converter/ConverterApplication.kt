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

package com.exactpro.th2.converter

import com.exactpro.th2.converter.conversion.LocalFilesConverter
import com.exactpro.th2.converter.util.SchemaVersion
import io.micronaut.runtime.Micronaut
import mu.KotlinLogging

open class ConverterApplication

private val logger = KotlinLogging.logger {}

private const val LOCAL_USAGE: String =
    "Usage: local <branch or directory with schemas> <current version> <target version>"

private const val ARGUMENTS_SIZE = 4
private const val SCHEMA_INDEX = 1
private const val CURRENT_VERSION_INDEX = 2
private const val TARGET_VERSION_INDEX = 3

fun main(args: Array<String>) {
    when (val mode = if (args.isNotEmpty()) args[0] else "server") {
        "server" -> {
            try {
                Micronaut
                    .build(*args)
                    .eagerInitConfiguration(true)
                    .eagerInitSingletons(true)
                    .mainClass(ConverterApplication::class.java)
                    .start()
            } catch (e: Exception) {
                val logger = KotlinLogging.logger { }
                logger.error("Exiting with exception", e)
            }
        }
        "local" -> {
            require(args.size == ARGUMENTS_SIZE) { LOCAL_USAGE }
            val currentVersion: SchemaVersion = getEnumValue(args[CURRENT_VERSION_INDEX])
            val targetVersion: SchemaVersion = getEnumValue(args[TARGET_VERSION_INDEX])
            LocalFilesConverter(args[SCHEMA_INDEX], currentVersion, targetVersion).convert()
        }
        else -> {
            logger.error("Mode: {} is not supported", mode)
        }
    }
}

private fun getEnumValue(name: String): SchemaVersion =
    SchemaVersion.values().find { it.name.equals(name, ignoreCase = true) }
        ?: error("unknown schema version $name")
