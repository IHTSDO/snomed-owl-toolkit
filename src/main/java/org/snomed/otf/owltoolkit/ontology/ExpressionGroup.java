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
package org.snomed.otf.owltoolkit.ontology;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;

import java.util.HashSet;
import java.util.Set;

public class ExpressionGroup {

	private Set<OWLClassExpression> members;
	private OWLObjectSomeValuesFrom hasActiveIngredientClassExpression;

	public ExpressionGroup() {
		members = new HashSet<>();
	}

	void addMember(OWLClassExpression owlClassExpression) {
		members.add(owlClassExpression);
	}

	public Set<OWLClassExpression> getMembers() {
		return members;
	}

	public void setHasActiveIngredientClassExpression(OWLObjectSomeValuesFrom hasActiveIngredientClassExpression) {
		this.hasActiveIngredientClassExpression = hasActiveIngredientClassExpression;
	}

	public OWLObjectSomeValuesFrom getHasActiveIngredientClassExpression() {
		return hasActiveIngredientClassExpression;
	}
}
