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

import com.exactpro.th2.converter.model.Th2Resource
import com.exactpro.th2.converter.model.latest.box.GenericBoxSpec
import com.exactpro.th2.converter.model.v1.link.LinkEndpoint
import com.exactpro.th2.converter.model.v1.link.LinkSpecV1
import com.exactpro.th2.converter.model.v1.link.MultiDictionary
import com.exactpro.th2.converter.model.v1.link.SingleDictionary
import com.exactpro.th2.infrarepo.repo.RepositoryResource
import com.fasterxml.jackson.module.kotlin.convertValue

object LinkUtils {
    private const val SINGLE_DICTIONARY_ALIAS = "dictionary"
    private const val MULTIPLE_DICTIONARY_ALIAS = "dictionaries"

    private fun generateResourceToLinkMap(
        links: Set<RepositoryResource>
    ): MutableMap<String, MutableMap<String, LinkTo>> {
        val resourceMap: MutableMap<String, MutableMap<String, LinkTo>> = HashMap()
        links.forEach { link ->
            val spec: LinkSpecV1 = Mapper.YAML_MAPPER.convertValue(link.spec)
            val mqLinks = spec.boxesRelation?.routerMq
            val grpcLinks = spec.boxesRelation?.routerGrpc
            val dictionaryLinks = spec.dictionariesRelation
            val multiDictionaryLinks = spec.multiDictionariesRelation

            mqLinks?.forEach { (_, from, to) ->
                resourceMap
                    .getOrPut(to.box) { HashMap() }
                    .getOrPut(to.pin) { LinkTo() }
                    .mq.add(
                        LinkEndpoint(from.box, from.pin)
                    )
            }

            grpcLinks?.forEach { (_, from, to) ->
                resourceMap
                    .getOrPut(from.box) { HashMap() }
                    .getOrPut(from.pin) { LinkTo() }
                    .grpc.add(
                        LinkEndpoint(to.box, to.pin)
                    )
            }
            dictionaryLinks?.forEach { (_, box, dictionary) ->
                resourceMap
                    .getOrPut(box) { HashMap() }
                    .getOrPut(SINGLE_DICTIONARY_ALIAS) { LinkTo() }
                    .dictionary.add(dictionary)
            }

            multiDictionaryLinks?.forEach { (_, box, dictionaries) ->
                resourceMap
                    .getOrPut(box) { HashMap() }
                    .getOrPut(MULTIPLE_DICTIONARY_ALIAS) { LinkTo() }
                    .multipleDictionary.addAll(dictionaries)
            }
        }
        return resourceMap
    }

    data class LinkTo(
        val mq: MutableList<LinkEndpoint> = ArrayList(),
        val grpc: MutableList<LinkEndpoint> = ArrayList(),
        val dictionary: MutableList<SingleDictionary> = ArrayList(),
        val multipleDictionary: MutableList<MultiDictionary> = ArrayList()
    )

    fun insertLinksIntoBoxes(convertedResources: List<Th2Resource>, links: Set<RepositoryResource>) {
        val resToLinkMap = generateResourceToLinkMap(links)
        val convertedResourcesMap = convertedResources.associateBy { it.metadata.name }
        resToLinkMap.forEach { (key, value) ->
            if (convertedResourcesMap.containsKey(key)) {
                val resource = convertedResourcesMap[key]
                val spc: GenericBoxSpec = Mapper.YAML_MAPPER.convertValue(resource!!.spec)
                val mqPinMap = spc.pins?.mq?.subscribers?.associateBy { it.name }
                val grpcPinMap = spc.pins?.grpc?.client?.associateBy { it.name }
                value.forEach { (key, value) ->
                    mqPinMap?.get(key)?.linkTo = value.mq
                    grpcPinMap?.get(key)?.linkTo = value.grpc
                }
                resource.spec = spc
            }
        }
    }
}
