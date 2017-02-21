package gov.nist.toolkit.valregmetadata.field

import gov.nist.toolkit.registrymetadata.Metadata
import gov.nist.toolkit.registrymetadata.MetadataParser
import gov.nist.toolkit.utilities.io.Io
import gov.nist.toolkit.valsupport.client.ValidationContext
import org.apache.log4j.BasicConfigurator
import spock.lang.Specification

// Imports for both ErrorRecorder versions, new and old. Comment out as needed.
// 1/ Old imports for GwtErrorRecorder, TextErrorRecorder
// import gov.nist.toolkit.errorrecordingold.ErrorRecorder
// import gov.nist.toolkit.errorrecordingold.GwtErrorRecorder
// import gov.nist.toolkit.errorrecordingold.TextErrorRecorder
// 2/ New imports for XMLErrorRecorder
import gov.nist.toolkit.errorrecording.ErrorRecorder
import gov.nist.toolkit.errorrecording.xml.XMLErrorRecorder


class XMLErrorRecorderTest extends Specification {

    def 'Run'() {
        setup:
        BasicConfigurator.configure()

        when:
        // InputStream metadataStream = getClass().getResourceAsStream('/PnR1Doc.xml')
        InputStream metadataStream = getClass().getResourceAsStream('/PnR1Doc_errors.xml')
        // InputStream metadataStream = getClass().getResourceAsStream('/RegisterDocumentSet-b-response.bytes')
        // InputStream metadataStream = getClass().getResourceAsStream('/RegistryStoredQuery-response.txt')
        //InputStream metadataStream = getClass().getResourceAsStream('/Message.bytes')


        String metadataString = Io.getStringFromInputStream(metadataStream)
        Metadata m = MetadataParser.parse(metadataString)

        ValidationContext vc = new ValidationContext()
        vc.isRequest = true
        vc.isPnR = true
        vc.codesFilename = getClass().getResource('/codes.xml').file

        // ErrorRecorder er = new GwtErrorRecorder()
        // ErrorRecorder er = new TextErrorRecorder()
        ErrorRecorder er = new XMLErrorRecorder()

        MetadataValidator val = new MetadataValidator(m, vc, null)
        val.run(er)

        println ("\n--- XML output ---\n" + er.toString())

        then:
        true
    }
}
