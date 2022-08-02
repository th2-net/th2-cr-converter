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

import com.exactpro.th2.converter.model.Th2Resource

data class ConversionSummary(
    val convertedResourceNames: MutableList<String> = ArrayList(),
    val errorMessages: MutableList<ErrorMessage> = ArrayList(),
    var commitRef: String? = null
) {

    fun hasErrors(): Boolean {
        return errorMessages.size > 0
    }
}

data class ConversionResult(
    val summary: ConversionSummary,
    val convertedResources: List<Th2Resource>
)

data class ErrorMessage(
    val resourceName: String,
    val error: String?
)
