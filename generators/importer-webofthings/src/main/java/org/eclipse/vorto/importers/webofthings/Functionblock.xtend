/**
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.vorto.importers.webofthings;

import java.util.List;

class FunctionblockTemplate {

	def String create(String namespace, String version, String title, String description, List<Property> properties, List<Property> actions, List<Property> events) {
		'''
			vortolang 1.0

			namespace «namespace»
			version «version»
			displayname "«title»"
			description "«description»"

			functionblock «parseName(title)» {

				configuration {

					«FOR item : properties»
						«IF item.readonly == false»
							«item.name» as «item.type» «IF item.description != null»"«item.description»"«ENDIF»
						«ENDIF»
					«ENDFOR»
				}

				status {
					«FOR item : properties»
						«IF item.readonly == true»
						    «item.name» as «item.type» «IF item.description != null»"«item.description»"«ENDIF»
						«ENDIF»
					«ENDFOR»
				}

				events {
				    «FOR item : events»
       			        «item.name» {
       			            data as «item.type» «IF item.description != null»"«item.description»"«ENDIF»
       			        }
                    «ENDFOR»
				}

				operations {
					«FOR item : actions»
						«item.name»() «IF item.description != null»"«item.description»"«ENDIF»
					«ENDFOR»
				}
			}
		'''
	}

	def parseName(String name) {
		return name.replace(" ","").replace("/","").replace("-","")
	}

}