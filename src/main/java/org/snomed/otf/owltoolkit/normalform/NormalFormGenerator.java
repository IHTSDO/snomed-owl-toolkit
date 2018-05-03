/*
 * Copyright 2017 SNOMED International, http://snomed.org
 * Copyright 2011-2016 B2i Healthcare Pte Ltd, http://b2i.sg
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
package org.snomed.otf.owltoolkit.normalform;

import com.google.common.collect.Ordering;
import org.snomed.otf.owltoolkit.classification.ReasonerTaxonomy;
import org.snomed.otf.owltoolkit.ontology.PropertyChain;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomy;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base class for different implementations, which generate a set of components in normal form, based on a subsumption
 * hierarchy encapsulated in a reasoner.
 * 
 * @param <T> the generated component type
 * 
 */
public abstract class NormalFormGenerator<T> {

	protected final ReasonerTaxonomy reasonerTaxonomy;
	
	protected final SnomedTaxonomy snomedTaxonomy;

	protected final Set<PropertyChain> propertyChains;

	protected boolean preprocessingComplete = false;// TODO Remove this if not needed

	public NormalFormGenerator(final ReasonerTaxonomy reasonerTaxonomy, SnomedTaxonomy snomedTaxonomy, Set<PropertyChain> propertyChains) {

		this.reasonerTaxonomy = reasonerTaxonomy;
		this.snomedTaxonomy = snomedTaxonomy;
		this.propertyChains = propertyChains;
	}
	
	/**
	 * Computes and returns all changes as a result of normal form computation.
	 * 
	 * @param processor the change processor to route changes to
	 * @param ordering an ordering defined over existing and generated components, used for detecting changes
	 * @return the total number of generated components
	 */
	public final int collectNormalFormChanges(final OntologyChangeProcessor<T> processor, final Ordering<T> ordering) {
		final List<Long> entries = reasonerTaxonomy.getConceptIds();
		int generatedComponentCount = 0;

		for (Long conceptId : entries) {
			firstNormalisationPass(conceptId);
		}

		preprocessingComplete = true;

		for (Long conceptId : entries) {
			final Collection<T> existingComponents = getExistingComponents(conceptId);
			final Collection<T> generatedComponents = secondNormalisationPass(conceptId);
			processor.apply(conceptId, existingComponents, generatedComponents, ordering);
			generatedComponentCount += generatedComponents.size();
		}

		return generatedComponentCount;
	}

	protected abstract Collection<T> getExistingComponents(final long conceptId);

	/**
	 * Computes and caches a set of components in normal form for the specified concept.
	 * The first pass uses the is-a hierarchy for normalisation.
	 * This hierarchy is available during the first pass because of the breath first order of processing concepts.
	 *
	 * @param conceptId the concept for which components should be generated
	 */
	protected abstract void firstNormalisationPass(final long conceptId);

	/**
	 * Performs additional normalisation as required before returning components in normal form for the specified concept.
	 * The second pass uses property chains and transitive properties in order to further normalise components.
	 * Other transitive hierarchies can not be guaranteed to be complete during the first pass because the super-type of a
	 * concept in a transitive property hierarchy may be at a lower level in the is-a hierarchy meaning that it's processed later during the first pass.
	 *
	 * @param conceptId the concept for which components should be generated
	 * @return the generated components of the specified concept in normal form
	 */
	protected abstract Collection<T> secondNormalisationPass(final long conceptId);

}
