/*
 * Copyright 2017 SNOMED International, http://snomed.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 *
 * Original author law223 - initial implementation in Snorocket SNOMED API
 */
package org.snomed.otf.owltoolkit.normalform.internal;

/**
 * Represents any item in an ontology which can be compared for
 * expressiveness.
 *
 * @param <T>
 *            the implementing type
 */
public interface SemanticComparable<T> {

	/**
	 * Checks if the specified item can be regarded as redundant when
	 * compared to the current item. An item is redundant with respect to
	 * another if it less specific, i.e. it describes a broader range of
	 * individuals.
	 *
	 * @param other
	 *            the item to compare against
	 *
	 * @return <code>true</code> if this item contains an equal or more
	 *         specific description when compared to the other item,
	 *         <code>false</code> otherwise
	 */
	boolean isSameOrStrongerThan(T other);
}
