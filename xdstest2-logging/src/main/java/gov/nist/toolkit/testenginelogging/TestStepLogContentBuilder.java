package gov.nist.toolkit.testenginelogging;

import gov.nist.toolkit.commondatatypes.MetadataSupport;
import gov.nist.toolkit.registrymetadata.Metadata;
import gov.nist.toolkit.registrymetadata.MetadataParser;
import gov.nist.toolkit.registrysupport.logging.RegistryErrorLog;
import gov.nist.toolkit.registrysupport.logging.RegistryResponseLog;
import gov.nist.toolkit.testenginelogging.client.ReportDTO;
import gov.nist.toolkit.testenginelogging.client.StepGoalsDTO;
import gov.nist.toolkit.testenginelogging.client.TestStepLogContentDTO;
import gov.nist.toolkit.testenginelogging.client.UseReportDTO;
import gov.nist.toolkit.utilities.xml.OMFormatter;
import gov.nist.toolkit.utilities.xml.Util;
import gov.nist.toolkit.utilities.xml.XmlUtil;
import gov.nist.toolkit.xdsexception.ExceptionUtil;
import gov.nist.toolkit.xdsexception.client.XdsInternalException;
import org.apache.axiom.om.OMElement;

import javax.xml.namespace.QName;
import javax.xml.parsers.FactoryConfigurationError;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class TestStepLogContentBuilder {
    private OMElement root;
    private TestStepLogContentDTO c = new TestStepLogContentDTO();
    private String id;

    private static final QName nameQname = new QName("name");
    private static final QName useAsQname = new QName("useAs");
    private static final QName reportNameQname = new QName("reportName");
    private static final QName valueQname = new QName("value");
    private static final QName testQname = new QName("test");
    private static final QName sectionQname = new QName("section");
    private static final QName stepQname = new QName("step");
    private final static QName symbolQ = new QName("symbol");
    private final static QName idQ = new QName("id");


    public TestStepLogContentBuilder(OMElement root) throws Exception {
        this.root = root;
    }

    public TestStepLogContentDTO build() throws Exception {
        String stat = root.getAttributeValue(MetadataSupport.status_qname);

        // hack until NewPatientId instructions generates proper status
        if (stat == null)
            c.setSuccess(true);
        else
            c.setSuccess("Pass".equals(stat));
        id = root.getAttributeValue(MetadataSupport.id_qname);
        c.setId(id);

        OMElement expectedStatusEle = XmlUtil.firstChildWithLocalName(root, "ExpectedStatus");
        if (expectedStatusEle == null)
            c.setExpectedSuccess(true);
        else {
            String expStat = expectedStatusEle.getText();

            String[] statuses = expStat.split(",");

            for (int cx=0; cx<statuses.length; cx++) {
                String status = statuses[cx].trim();
                if ("Success".equals(status)) {
                    c.setExpectedSuccess(true);
                    c.setExpectedWarning(false);
                } else if ("Fault".equals(status)) {
                    c.setExpectedSuccess(false);
                    c.setExpectedWarning(false);
                }  else if ("Failure".equals(status)) {
                    c.setExpectedSuccess(false);
                    c.setExpectedWarning(false);
                } else if ("Warning".equals(status)) {
                    c.setExpectedWarning(true);
                    c.setExpectedSuccess(false);
                } else if ("PartialSuccess".equals(status)) {
                    c.setExpectedWarning(false);
                    c.setExpectedSuccess(false);
                } else if (cx==statuses.length-1)
                    throw new Exception("TestStep: Error parsing log.xml file: illegal value (" + expStat + ") for ExpectedStatus element of step " + id);
            }

        }
        parseGoals();
        OMElement endpointEl = parseEndpoint();
        parseTransaction(endpointEl);
        parseDetails();
        parseErrors();
        parseFatalErrors();
        parseInHeader();
        parseOutHeader();
        parseResult();
        parseInputMetadata();
        parseRoot();
        parseUseReports();
        parseReports();
        parseIds();
        parseSoapFaults();
        parseAssertionErrors();

        return c;
    }

    private void parseIds() {
        parseIds(c.getAssignedUids(), "AssignedUids");
        parseIds(c.getAssignedIds(), "AssignedUuids");
    }

    private void parseIds(Map<String, String> map, String section) {
        List<OMElement> idEles = XmlUtil.decendentsWithLocalName(root, section);
        for (OMElement e : idEles) {
            for (Iterator i = e.getChildrenWithLocalName("Assign"); i.hasNext(); ) {
                OMElement a = (OMElement) i.next();
                String symbol = a.getAttributeValue(symbolQ);
                String id = a.getAttributeValue(idQ);
                map.put(symbol, id);
            }
        }
    }

    private void parseReports() {
        // See Issue #418 Duplicate UseReports/Reports in Conformance tool Section/Step UI (Test log.xml file contains singular values though).
        // without use of the map we get duplicates because of the Assertions
        // section of the log.xml file format
//        Map<String, String> map = new HashMap<>();
        for (OMElement ele : XmlUtil.decendentsWithLocalName(root, "Report")) {
            String name = ele.getAttributeValue(nameQname);
            String value = ele.getText();
            c.getReportsSummary().add(name + " = " + value);
            c.addReportDTO(new ReportDTO(name, value));
//            map.put(name, value);
        }
//        for(String key : map.keySet()) {
//            c.getReportsSummary().add(key + " = " + map.get(key));
//            c.addReportDTO(new ReportDTO(key, map.get(key)));
//        }

    }

    private void parseUseReports() {
        // without use of the map we get duplicates because of the Assertions
        // section of the log.xml file format
        for (OMElement ele : XmlUtil.decendentsWithLocalName(root, "UseReport")) {
            UseReportDTO u = new UseReportDTO();
            u.setUseAs(ele.getAttributeValue(useAsQname));
            u.setName(ele.getAttributeValue(reportNameQname));
            u.setValue(ele.getAttributeValue(valueQname));
            u.setTest(ele.getAttributeValue(testQname));
            u.setSection(ele.getAttributeValue(sectionQname));
            u.setStep(ele.getAttributeValue(stepQname));

            c.addUseReport(u);
        }
    }

    private void parseRoot() {
        try {
            c.setRootString(xmlFormat(root));
        } catch (Exception e) {}
    }


    private void parseInputMetadata() {
        try {
            c.setInputMetadata(getFormattedInputMetadata());
        } catch (Exception e) {
        }
    }

    private void parseResult() {
        try {
            OMElement result = XmlUtil.firstDecendentWithLocalName(root, "Result");
            if (result == null) {
                c.setResult("");
                return;
            }
            if (!hasChildElement(result)) {
                String text = result.getText();
                if (text != null) {
                    text = text.trim();
                    if (text.startsWith("{") || text.startsWith("<"))
                        c.setResult(text);
                }
                else
                    c.setResult(result.toString()); // Was c.setResult(result.getText());
                return;
            }
            OMElement copy = Util.deep_copy(result.getFirstElement());
            for (OMElement ele : XmlUtil.decendentsWithLocalName(copy, "Document", 4)) {
                String original = ele.getText();
                int size = (original == null || original.equals("")) ? 0 : original.length();
                ele.setText("Base64 contents removed by XDS Toolkit prior to display (" + size + " characters)");
            }
            c.setResult(xmlFormat(copy));
        } catch (Exception e) {
        }
    }

    private boolean hasChildElement(OMElement ele) {
        OMElement valueEle = ele.getFirstElement();
        return valueEle != null;
    }

    private void parseOutHeader() {
        try {
            OMElement hdr = XmlUtil.firstDecendentWithLocalName(root, "OutHeader");
            if (!hasChildElement(hdr)) {
                c.setOutHeader(hdr.getText());
                return;
            }
            c.setOutHeader(xmlFormat(hdr.getFirstElement()));
        } catch (Exception e) {
        }
    }

    private void parseInHeader() {
        try {
            OMElement hdr = XmlUtil.firstDecendentWithLocalName(root, "InHeader");
            if (!hasChildElement(hdr)) {
                c.setInHeader(hdr.getText());
                return;
            }
            c.setInHeader(xmlFormat(hdr.getFirstElement()));
        } catch (Exception e) {
        }
    }

    private void parseFatalErrors() {
        List<OMElement> eles = XmlUtil.decendentsWithLocalName(root, "Error");
        for (OMElement ele : eles) {
            String err = ele.getText();
            int at = err.indexOf("at gov.nist");
            if (at != -1) {
                err = err.substring(0, at);
            }
            c.getErrors().add(err);
        }
    }

    private void parseErrors() throws Exception {
        try {
            RegistryResponseLog rrl = getUnexpectedErrors();
            for (int i=0; i<rrl.size(); i++) {
                RegistryErrorLog rel = rrl.getError(i);
                c.getErrors().add(rel.getSummary());
            }
        } catch (Exception e) {}
        c.getErrors().addAll(c.getAssertionErrors());
    }

    private void parseDetails() {
        for (OMElement ele : XmlUtil.decendentsWithLocalName(root, "Detail")) {
            String detail = ele.getText();
            c.getDetails().add(detail);
        }
    }

    private OMElement parseEndpoint() {
        OMElement endpointEl = null;
        List<OMElement> endpoints = XmlUtil.decendentsWithLocalName(root, "Endpoint");
        if (!endpoints.isEmpty()) {
            endpointEl = endpoints.get(0);
            c.setEndpoint(endpointEl.getText());
        }
        return endpointEl;
    }

    /**
     * The transaction element would be the parent of the endpoint (the transaction element does not have a consistent identifier to parse directly).
     * @param child
     */
    private void parseTransaction(OMElement child) {
        if (child==null) {
            // Alternative method #2
            OMElement el = null;
            List<OMElement> transactions = XmlUtil.descendantsWithLocalNameEndsWith(root, "Transaction");
            if (!transactions.isEmpty()) {
                el = transactions.get(0);
            }

            if (el==null) {
                c.setTransaction("UnknownTx");
            } else {
                c.setTransaction(el.getLocalName());
            }

        } else { // Method #1
            c.setTransaction(((OMElement)child.getParent()).getLocalName());
        }
    }

    private void parseGoals() {
        c.setStepGoalsDTO(new StepGoalsDTO(id));

        for (OMElement ele : XmlUtil.childrenWithLocalName(root, "Goal")) {
            c.getStepGoalsDTO().getGoals().add(ele.getText());
        }

    }

    private RegistryResponseLog getUnexpectedErrors() throws Exception {
        return new RegistryResponseLog(getRegistryResponse().getErrorsDontMatch(getExpectedErrorMessage()));
    }

    private RegistryResponseLog getRegistryResponse() throws Exception {
        return new RegistryResponseLog(getRawResult());
    }

    /**
     * Get response message for this test step.
     * @return OMElement of message
     * @throws Exception if no result
     */
    @SuppressWarnings("unchecked")
    private OMElement getRawResult() throws Exception {
        for (Iterator<OMElement> it = root.getChildElements(); it.hasNext(); ) {
            OMElement ele1 = it.next();
            for (Iterator<OMElement> it2=ele1.getChildElements(); it2.hasNext(); ) {
                OMElement ele2 = it2.next();
                if ("Result".equals(ele2.getLocalName())) {
                    return ele2.getFirstElement();
                }
            }
        }
        throw new Exception("Step: " + id + " has no &ltResult/> block");
    }

    private String getExpectedErrorMessage() {
        OMElement expEle = root.getFirstChildWithName(MetadataSupport.expected_error_message_qname);
        if (expEle == null)
            return null;
        return expEle.getText();
    }

    private void parseAssertionErrors() {
        List<OMElement> errorEles = XmlUtil.decendentsWithLocalName(root, "FailedAssertion");
        List<String> errors = new ArrayList<String>();

        for (OMElement errorEle : errorEles) {
            errors.add("FailedAssertion (" + errorEle.getAttributeValue(new QName("assertionId")) + ")");
            try {
                errors.add("&nbsp;&nbsp;LeftSide Value: " + XmlUtil.firstChildWithLocalName(errorEle, "LeftSideValue").getText());
            } catch (NullPointerException e) {}
            try {
                errors.add("&nbsp;&nbsp;Operator: "  + XmlUtil.firstChildWithLocalName(errorEle, "Operator").getText());
            } catch (NullPointerException e) {}
            try {
                errors.add("&nbsp;&nbsp;RightSide Value: " + XmlUtil.firstChildWithLocalName(errorEle, "RightSideValue").getText());
            } catch (NullPointerException e) {}
        }
        c.setAssertionErrors(errors);
    }

    private String xmlFormat(OMElement ele) throws XdsInternalException, FactoryConfigurationError {
        if (ele == null)
            return "";
        try {
            return new OMFormatter(ele.toString()).toString();
        } catch (Exception e) {
            return ExceptionUtil.exception_details(e);
        }
    }

    private String getFormattedInputMetadata() throws XdsInternalException {
        OMElement ele = XmlUtil.firstDecendentWithLocalName(root, "InputMetadata");
        if (ele == null) return "";
        if (ele.getFirstElement() != null)
            return xmlFormat(ele.getFirstElement());
        return ele.toString(); // Was: return ele.getText();  -- This caused &amp;amp; to be dropped out and appear simply as &amp; Which caused wstxlazyexception unexpected character error.
    }

    private OMElement getRawInputMetadata() {
        return XmlUtil.firstDecendentWithLocalName(root, "InputMetadata").getFirstElement();
    }

    public Metadata getMetadata() throws Exception {
        return MetadataParser.parseNonSubmission(getRawResult());
    }

    public String getName() {
        return id;
    }

    private void parseSoapFaults() {
        List<String> errs = new ArrayList<String>();

        for (OMElement errEle : XmlUtil.childrenWithLocalName(root, "SOAPFault")) {
            String err = errEle.getText();
            errs.add(id + ": " + err);
        }

        c.setSoapFaults(errs);
    }

//    public Metadata getParsedInputMetadata() throws MetadataValidationException, MetadataException {
//        return MetadataParser.parseNonSubmission(getRawInputMetadata());
//    }

    public String getId() {
        return id;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();

        buf.append("[TestStepLog: ");

        buf.append(" id=" + id);
        buf.append(" success=" + c.isSuccess());
        buf.append(" endpoint=" + c.getEndpoint());

        buf.append("]");

        return buf.toString();
    }

}
