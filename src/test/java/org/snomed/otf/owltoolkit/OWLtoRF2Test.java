package org.snomed.otf.owltoolkit;

import org.snomed.otf.owltoolkit.conversion.OWLtoRF2Service;

import java.io.*;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class OWLtoRF2Test {

    public static void main(String[] args) throws Exception {
        OWLtoRF2Service service = new OWLtoRF2Service();

        //String owlFilePath = "E:\\Users\\warren\\Documents\\aPostdoc\\code\\~test-code\\normalisation-randomness-issue\\" +
        //        "left-facet-joint-ontology\\Left_facet_joint_module_after_removing_equivalences.owl";

        String owlFilePath = "E:/Users/warren/Documents/aPostdoc/code/~test-code/abstract-definitions-test/" +
                "sct/sct-july-2020-noReflexivity.owl";

        String outputPath = owlFilePath.substring(0, owlFilePath.lastIndexOf("/")+1);

        InputStream is = new FileInputStream(owlFilePath);
        InputStream owlFileStream = new BufferedInputStream(is);

        //File rf2Zip = Files.createTempFile("owl-to-rf2-directory_" + new Date().getTime(), ".zip").toFile();
        File rf2Zip = new File(outputPath + "owl-to-rf2-directory_" + new Date().getTime() + ".zip");
        service.writeToRF2(owlFileStream, new FileOutputStream(rf2Zip), new GregorianCalendar(2020, Calendar.SEPTEMBER, 3).getTime());
    }
}
