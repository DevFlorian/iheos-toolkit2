/**
 * 
 */
package edu.wustl.mir.erl.ihe.xdsi.util;

import gov.nist.toolkit.utilities.xml.XmlUtil;
import org.apache.axiom.om.OMElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathExpression;
import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 *
 * 
 * @author Ralph Moulton / MIR WUSTL IHE Development Project <a
 * href="mailto:moultonr@mir.wustl.edu">moultonr@mir.wustl.edu</a>
 *
 */
public class PidDateFilenameFilter implements FilenameFilter {
   public static String DATE_DIR_FORMAT = "yyyy_MM_dd_HH_mm_ss_SSS";
   private static Logger log = null;
   private static QName[] metaDataQnames =
         { new QName("urn:oasis:names:tc:ebxml-regrep:rim:xsd:2.1", "dummy", "rim"),
            new QName("urn:oasis:names:tc:ebxml-regrep:registry:xsd:2.1", "dummy", "rs") };
   private String pid;
   private Date date;
   XPathExpression expr = null;
   
   /**
    * FilenameFilter used to find directories in xtools logs containing 
    * metadata for a particular patient id and/or transaction date
    * @param pid patient id to find, or null for any patient
    * @param date of transaction, return first logged transaction after this
    * date, or null to return any transaction date
    * @throws Exception on error.
    */
   public PidDateFilenameFilter(String pid, Date date) throws Exception {
	  log = Utility.getLog();
      this.pid = pid;
      this.date = date;
   }
   @Override
   public boolean accept(File dir, String name) {
      try {
    	 log.debug("PidDateFilenameFilter::accept " + dir.toString() + " " + name + " reference Patient ID " + pid);
         if (new File(dir, name).isDirectory() == false) return false;
         Date dirDate = new SimpleDateFormat(DATE_DIR_FORMAT).parse(name);
         if (date != null && dirDate.before(date)) {
        	 log.debug("Folder rejected on date criteria: reference date " + date.toString() + " folder date " + dirDate.toString());
        	 return false;
         }
         if (StringUtils.isBlank(pid)) {
        	 log.debug("Folder accepted, no reference PID supplied");
        	 return true;
         }
         String mds = PrsSimLogs.getSOAPMetaData(dir.toPath().resolve(name));
         OMElement md = XmlUtil.strToOM(mds);

         List<OMElement> elements = null;
         
/*         List<OMElement> elements = XmlUtil.childrenWithLocalName(md, "RegistryObjectList");
         if (elements.size() != 1) return false;
         md = elements.get(0);*/
         md = XmlUtil.firstDecendentWithLocalName(md, "RegistryObjectList");
         if (md == null) return false;
         
         elements = XmlUtil.childrenWithLocalName(md, "ExtrinsicObject");
         md = null;
         for (OMElement element : elements) {
            if(element.getAttributeValue(new QName("objectType")).equals("urn:uuid:7edca82f-054d-47f2-a032-9b2a5b5186c1")) {
               md = element;
               break;
            }
         }
         if (md == null) {
        	 log.debug("No metadata element found with objectType urn:uuid:7edca82f-054d-47f2-a032-9b2a5b5186c1");
        	 return false;
         }
         elements = XmlUtil.childrenWithLocalName(md, "ExternalIdentifier");
         if (elements == null) {
        	 log.debug("Found objectType urn:uuid:7edca82f-054d-47f2-a032-9b2a5b5186c1, no elements named ExternalIdentifier");
         }
         md = null;
         String rawPid = null;
         for (OMElement element : elements) {
        	 String identificationScheme = element.getAttributeValue(new QName("identificationScheme"));
        	 if (identificationScheme.equals("urn:uuid:58a6f841-87b3-4a3e-92fd-a8ffeff98427")) {
        		 rawPid = element.getAttributeValue(new QName("value"));
        		 log.debug("Found ExternalIdentifier with identification scheme urn:uuid:58a6f841-87b3-4a3e-92fd-a8ffeff98427 " + rawPid);
        	 } else {
        		 log.debug("External identifier ignored, identificationScheme is NOT urn:uuid:58a6f841-87b3-4a3e-92fd-a8ffeff98427, identificationScheme from metadata is " + identificationScheme);
        	 }
         }
         if (rawPid == null) {
        	 log.debug("No ExternalIdentifier elements found with proper identificationScheme");
        	 return false;
         }
         if (rawPid.equals(pid)) {
        	 log.debug("External Patient ID in this folder matches reference Patient ID " + pid);
        	 return true;
         }
         log.debug("External Patient ID in this folder (" + rawPid + ") does not match reference Patient ID: " + pid);
         return false;
      } catch (Exception e) {}
      log.debug("Exiting method because of an exception; returning false");
      return false;
   }

}
