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
package org.snomed.otf.owltoolkit.constants;

import static java.lang.Long.parseLong;

public class Concepts {

	public static final String ROOT = "138875005";
	public static final Long ROOT_LONG = parseLong(ROOT);
	public static final String SNOMED_CT_CORE_MODULE = "900000000000207008";
	public static final String SNOMED_CT_MODEL_COMPONENT_MODULE = "900000000000012004";
	public static final String SNOMED_CT_MODEL_COMPONENT = "900000000000441003";
	public static final String IS_A = "116680003";
	public static final Long IS_A_LONG = parseLong(IS_A);

	public static final String FULLY_DEFINED = "900000000000073002";
	public static final String PRIMITIVE = "900000000000074008";
	public static final String EXISTENTIAL_RESTRICTION_MODIFIER = "900000000000451002";
	public static final String UNIVERSAL_RESTRICTION_MODIFIER = "900000000000452009";

	public static final String INFERRED_RELATIONSHIP = "900000000000011006";
	public static final String STATED_RELATIONSHIP = "900000000000010007";
	public static final String ADDITIONAL_RELATIONSHIP = "900000000000227009";
	public static final Long ADDITIONAL_RELATIONSHIP_LONG = parseLong(ADDITIONAL_RELATIONSHIP);
	public static final String DEFINING_RELATIONSHIP = "900000000000006009";

	public static final String FSN = "900000000000003001";

	public static final String ALL_PRECOORDINATED_CONTENT = "723594008";

	public static final String OWL_ONTOLOGY_REFERENCE_SET = "762103008";
	public static final String OWL_ONTOLOGY_NAMESPACE = "734146004";
	public static final String OWL_ONTOLOGY_HEADER = "734147008";
	public static final String OWL_AXIOM_REFERENCE_SET = "733073007";
	public static final String MRCM_ATTRIBUTE_DOMAIN_INTERNATIONAL_REFERENCE_SET = "723561005";

	// Concepts that require special care when classifying
	public static final String CONCEPT_MODEL_ATTRIBUTE = "410662002";
	public static final Long CONCEPT_MODEL_ATTRIBUTE_LONG = parseLong(CONCEPT_MODEL_ATTRIBUTE);
	public static final String CONCEPT_MODEL_OBJECT_ATTRIBUTE = "762705008";
	public static final Long CONCEPT_MODEL_OBJECT_ATTRIBUTE_LONG = parseLong(CONCEPT_MODEL_OBJECT_ATTRIBUTE);
	public static final String CONCEPT_MODEL_DATA_ATTRIBUTE = "762706009";
	public static final Long CONCEPT_MODEL_DATA_ATTRIBUTE_LONG = parseLong(CONCEPT_MODEL_DATA_ATTRIBUTE);
	public static final String ALL_OR_PART_OF = "733928003";
	public static final String PART_OF = "123005000";
	public static final String LATERALITY = "272741003";
	public static final Long LATERALITY_LONG = parseLong("272741003");
	public static final String HAS_ACTIVE_INGREDIENT = "127489000";
	public static final Long HAS_ACTIVE_INGREDIENT_LONG = parseLong(HAS_ACTIVE_INGREDIENT);
	public static final String IS_MODIFICATION_OF = "738774007";
	public static final String HAS_DOSE_FORM = "411116001";

}
