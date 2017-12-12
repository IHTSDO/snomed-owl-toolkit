/*
 * Copyright 2017 SNOMED International, http://snomed.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.snomed.otf.owltoolkit.normalform.transitive;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class NodeGraph {

	private Map<Long, Node> nodeMap = new Long2ObjectOpenHashMap<>();

	public void addParent(long conceptId, long parentId) {
		if (conceptId == parentId) return;
		Node concept = nodeMap.computeIfAbsent(conceptId, Node::new);
		Node parent = nodeMap.computeIfAbsent(parentId, Node::new);
		concept.getParents().add(parent);
	}

	public Set<Long> getAncestors(long conceptId) {
		Node node = nodeMap.get(conceptId);
		if (node == null) {
			return Collections.emptySet();
		}
		return node.getAncestorIds();
	}
}
