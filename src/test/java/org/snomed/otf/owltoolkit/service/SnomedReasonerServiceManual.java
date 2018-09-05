package org.snomed.otf.owltoolkit.service;

import com.google.common.collect.Sets;

import java.io.File;
import java.util.Set;

import static org.snomed.otf.owltoolkit.service.SnomedReasonerService.ELK_REASONER_FACTORY;

// Utility class for manual testing
public class SnomedReasonerServiceManual {

	public static void main(String[] args) throws ReasonerServiceException, InterruptedException {

		String latestReleasePath = args[0];
		String deltaPath = args[1];

		Set<File> releases = Sets.newHashSet(
				new File(latestReleasePath)
		);
		new SnomedReasonerService().classify(
				"local",
				releases,
				new File(deltaPath),
				new File("results.zip"),
				ELK_REASONER_FACTORY,
				false
		);
	}

}
