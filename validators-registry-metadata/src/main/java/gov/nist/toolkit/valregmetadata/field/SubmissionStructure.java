package gov.nist.toolkit.valregmetadata.field;

import gov.nist.toolkit.errorrecording.IErrorRecorder;
import gov.nist.toolkit.errorrecording.common.XdsErrorCode;
import gov.nist.toolkit.errorrecording.xml.assertions.Assertion;
import gov.nist.toolkit.errorrecording.xml.assertions.AssertionLibrary;
import gov.nist.toolkit.registrymetadata.Metadata;
import gov.nist.toolkit.commondatatypes.MetadataSupport;
import gov.nist.toolkit.valsupport.client.ValidationContext;
import gov.nist.toolkit.valsupport.registry.RegistryValidationInterface;
import org.apache.axiom.om.OMElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SubmissionStructure {
	Metadata m;
	RegistryValidationInterface rvi;
	boolean hasmember_error = false;
	private AssertionLibrary ASSERTIONLIBRARY = AssertionLibrary.getInstance();



	public SubmissionStructure(Metadata m, RegistryValidationInterface rvi)  {
		this.m = m;
		this.rvi = rvi;
	}

	public void run(IErrorRecorder er, ValidationContext vc)   {
		submission_structure(er, vc);

	}


	void submission_structure(IErrorRecorder er, ValidationContext vc)   {
		if (vc.isSubmit() && vc.isRequest)
			er.sectionHeading("Submission Structure");
		ss_doc_fol_must_have_ids(er, vc);
		if (vc.isSubmit() && vc.isRequest) {
			has_single_ss(er, vc);
			all_docs_linked_to_ss(er, vc);
			all_fols_linked_to_ss(er, vc);
			symbolic_refs_not_in_submission(er);
			eval_assocs(er, vc); // Verifications from ITI TF-3 4.2.2.2.6 Rev. 12.1 are in the ProcessMetadataForRegister class

			ss_status_single_value(er, vc);

			new PatientId(m, er).run();
		}
		if (hasmember_error)
			log_hasmember_usage(er);
	}

	String assocDescription(OMElement obj) {
		return assocDescription(m.getId(obj));
	}

	String assocDescription(String id) {
		return "Association(" + id + ")";
	}

	String docEntryDescription(OMElement obj) {
		return docEntryDescription(m.getId(obj));
	}

	String docEntryDescription(String id) {
		return "DocumentEntry(" + id + ")";
	}

	String folderDescription(OMElement obj) {
		return folderDescription(m.getId(obj));
	}

	String folderDescription(String id) {
		return "Folder(" + id + ")";
	}

	String ssDescription(OMElement obj) {
		return "SubmissionSet(" + m.getId(obj) + ")";
	}

	boolean isSubmissionSet(String id) {
		if (id == null)
			return false;
		try {
			if (m.getId(m.getSubmissionSet()).equals(id))
				return true;
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	boolean isDocumentEntry(String id) {
		if (id == null)
			return false;
		return m.getExtrinsicObjectIds().contains(id);
	}

	boolean isAssoc(String id) {
		if (id == null)
			return false;
		return m.getAssociationIds().contains(id);
	}

	OMElement getObjectById(String id) {
		try {
			return m.getObjectById(id);
		} catch (Exception e) {
			return null;
		}
	}

	boolean submissionContains(String id) {
		return getObjectById(id) != null;
	}

	void validateFolderHasMemberAssoc(IErrorRecorder er, String assocId) {
		OMElement assoc = getObjectById(assocId);
		if (simpleAssocType(m.getAssocType(assoc)).equals("HasMember")) {
			// must relate folder to docentry
			String source = m.getAssocSource(assoc);
			String target = m.getAssocTarget(assoc);
			if (source == null || target == null)
				return;
			// try to verify that source is a Folder
			if (m.isFolder(source)) {
				// is folder
			} else if (source.startsWith("urn:uuid:")) {
				// may be folder
				er.externalChallenge(source + " must be shown to be a Folder already in the registry");
			} else {
				// is not folder
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA053");
				String detail = "Association source found: '" + source + "'";
				er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
				hasmember_error = true;
			}
			// try to verify that target is a DocumentEntry
			if (m.isDocument(target)) {
				// is DocumentEntry
			} else if (target.startsWith("urn:uuid:")) {
				// may be DocumentEntry
				er.externalChallenge(source + " must be shown to be a DocumentEntry already in the registry");
			} else {
				// is not DocumentEntry
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA054");
				String detail = "Association source found: '" + source + "'";
				er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
				hasmember_error = true;
			}
		} else {
			Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA055");
			String detail = "Association found: '" + assocDescription(assocId) + "'";
			er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
			hasmember_error = true;
		}
	}

	boolean isMemberOfSS(String id) {
		String ssid = m.getSubmissionSetId();
		return haveAssoc("HasMember", ssid, id);
	}

	void log_hasmember_usage(IErrorRecorder er) {

		er.detail("A HasMember association can be used to do the following:");
		er.detail("  Link the SubmissionSet to a DocumentEntry in the submission (if it has SubmissionSetStatus value of Original)");
		er.detail("  Link the SubmissionSet to a DocumentEntry already in the registry (if it has SubmissionSetStatus value of Reference)");
		er.detail("  Link the SubmissionSet to a Folder in the submission");
		er.detail("  Link the SubmissionSet to a HasMember association that links a Folder to a DocumentEntry.");
		er.detail("    The Folder and the DocumentEntry can be in the submisison or already in the registry");

	}

	boolean haveAssoc(String type, String source, String target) {
		String simpleType = simpleAssocType(type);
		for (OMElement assoc : m.getAssociations()) {
			if (!simpleType.equals(simpleAssocType(m.getAssocType(assoc))))
				continue;
			if (!source.equals(m.getAssocSource(assoc)))
				continue;
			if (!target.equals(m.getAssocTarget(assoc)))
				continue;
			return true;
		}
		return false;
	}

	boolean is_ss_to_de_hasmember(OMElement assoc) {
		String source = m.getAssocSource(assoc);
		String target = m.getAssocTarget(assoc);
		String type = getSimpleAssocType(assoc);

		if (source == null || target == null || type == null)
			return false;

		if (!type.equals("HasMember"))
			return false;

		if (!source.equals(m.getSubmissionSetId()))
			return false;

		if (!m.getExtrinsicObjectIds().contains(target))
			return false;

		if (!is_sss_original(assoc))
			return false;
		return true;
	}

	public boolean is_fol_to_de_hasmember(OMElement assoc) {
		String source = m.getAssocSource(assoc);
		String target = m.getAssocTarget(assoc);
		String type = getSimpleAssocType(assoc);

		if (source == null || target == null || type == null)
			return false;

		if (!type.equals("HasMember"))
			return false;

		if (!m.getFolderIds().contains(source)) {
			if (isUUID(source)) {
				if (rvi != null && !rvi.isFolder(source))
					return false;
			} else {
				return false;
			}
		}

		if (!m.getExtrinsicObjectIds().contains(target)) {
			if (isUUID(target)) {
				if (rvi != null && !rvi.isDocumentEntry(target))
					return false;
			} else {
				return false;
			}
		}

		return true;
	}

	boolean is_ss_to_existing_de_hasmember(OMElement assoc) {
		String source = m.getAssocSource(assoc);
		String target = m.getAssocTarget(assoc);
		String type = getSimpleAssocType(assoc);

		if (source == null || target == null || type == null)
			return false;

		if (!type.equals("HasMember"))
			return false;

		if (!source.equals(m.getSubmissionSetId()))
			return false;

		if (submissionContains(target) || !isUUID(target))
			return false;

		if (!is_sss_reference(assoc))
			return false;
		return true;
	}

	boolean is_ss_to_folder_hasmember(OMElement assoc) {
		String source = m.getAssocSource(assoc);
		String target = m.getAssocTarget(assoc);
		String type = getSimpleAssocType(assoc);

		if (source == null || target == null || type == null)
			return false;

		if (!type.equals("HasMember"))
			return false;

		if (!source.equals(m.getSubmissionSetId()))
			return false;

		if (!m.getFolderIds().contains(target))
			return false;

		return true;

	}

	boolean is_ss_to_folder_hasmember_hasmember(OMElement assoc) {
		String source = m.getAssocSource(assoc);
		String target = m.getAssocTarget(assoc);
		String type = getSimpleAssocType(assoc);

		if (source == null || target == null || type == null)
			return false;

		if (!type.equals("HasMember"))
			return false;

		if (!source.equals(m.getSubmissionSetId()))
			return false;

		if (!m.getAssociationIds().contains(target))
			return false;

		// target association - should link a folder and a documententry
		// folder can be in submission or registry
		// same for documententry
		OMElement tassoc = getObjectById(target);

		// both source and target of tassoc have to be uuids and not in submission
		// hopefully in registry

		String ttarget = m.getAssocTarget(tassoc);
		String tsource = m.getAssocSource(tassoc);


		// for both the target and source
		// if points to an object in submission, can be symbolic or uuid
		//     but object must be HasMember Association
		// if points to an object in registry, must be uuid

		if (submissionContains(tsource)) {
			// tsource must be folder
			if (!m.getFolderIds().contains(tsource))
				return false;
		} else {
			// in registry?
			if (isUUID(tsource)) {
				if (rvi != null && !rvi.isFolder(tsource))
					return false;
			} else {
				return false;
			}
		}

		if (submissionContains(ttarget)) {
			// ttarget must be a DocumentEntry
			if (!m.getExtrinsicObjectIds().contains(ttarget))
				return false;
		} else {
			// in registry?
			if (isUUID(ttarget)) {
				if (rvi != null && !rvi.isDocumentEntry(ttarget))
					return false;
			} else {
				return false;
			}
		}


		// registry contents validation needed here
		// to show that the tsource references a folder
		// and ttarget references a non-deprecated docentry

		return true;
	}

	boolean isUUID(String id) {
		return id != null && id.startsWith("urn:uuid:");
	}

	String objectType(String id) {
		if (id == null)
			return "null";
		if (m.getSubmissionSetIds().contains(id))
			return "SubmissionSet";
		if (m.getExtrinsicObjectIds().contains(id))
			return "DocumentEntry";
		if (m.getFolderIds().contains(id))
			return "Folder";
		if (m.getAssociationIds().contains(id))
			return "Association";
		return "Unknown";
	}

	String objectDescription(String id) {
		return objectType(id) + "(" + id + ")";
	}

	String objectDescription(OMElement ele) {
		return objectDescription(m.getId(ele));
	}

	String assocsRef = "ITI Tf-3: 4.1";

	void evalHasMember(IErrorRecorder er, OMElement assoc) {
		String source = m.getAssocSource(assoc);
		String target = m.getAssocTarget(assoc);
		String type = m.getAssocType(assoc);

		if (source == null || target == null || type == null)
			return;

		if (is_ss_to_de_hasmember(assoc)) {
			er.detail(assocDescription(assoc) + ": is a SubmissionSet to DocmentEntry HasMember association");
		} else if (is_ss_to_existing_de_hasmember(assoc)) {
			er.detail(assocDescription(assoc) + ": is a SubmissionSet to existing DocmentEntry HasMember (ByReference) association");
		} else if (is_ss_to_folder_hasmember(assoc)) {
			er.detail(assocDescription(assoc) + ": is a SubmissionSet to Folder HasMember association");
		} else if (is_ss_to_folder_hasmember_hasmember(assoc)) {
			er.detail(assocDescription(assoc) + ": is a SubmissionSet to Folder-HasMember HasMember association (adds existing DocumentEntry to existing Folder)");
		} else if (is_fol_to_de_hasmember(assoc)) {
			er.detail(assocDescription(assoc) + ": is a Folder to DocumentEntry HasMember association");
		} else {
			Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA056");
			String detail = "Do not understand this HasMember association: '" + assocDescription(assoc) + "'. SourceObject is '"
					+ objectDescription(source) + "' and targetObject is '" + objectDescription(target) +"'.";
			er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
			hasmember_error = true;
		}
	}

	void evalRelationship(IErrorRecorder er, OMElement assoc, ValidationContext vc) {
		String source = m.getAssocSource(assoc);
		String target = m.getAssocTarget(assoc);
		String type = m.getAssocType(assoc);

		if (source == null || target == null || type == null)
			return;

		if (!isDocumentEntry(source) && !vc.isMU) {
			Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA057");
			String detail = objectDescription(assoc) + ": with type '" + simpleAssocType(type) + "' must reference a DocumentEntry in " +
					"submission with its sourceObject attribute, it references '" + objectDescription(source) + "'";
			er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
		}
		if (containsObject(target)) {
			// This only checks for a circular reference but not the registry collection
			Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA058");
			String detail = objectDescription(assoc) + ": with type '" + simpleAssocType(type) + "' must reference a DocumentEntry in the registry " +
					"with its targetObject attribute, it references '" + objectDescription(target) + "' which is in the submission";
			er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
		}
		if (!isUUID(target)) {
			Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA059");
			String detail = objectDescription(assoc) + ": with type '" + simpleAssocType(type) +
					"' must reference a DocumentEntry in the registry with its targetObject attribute, it references '"
					+ objectDescription(target) + "' which is a symbolic ID that cannot reference an object in the registry";
			er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
		}
	}



	static List<String> relationships =
			Arrays.asList(
					"HasMember",
					"RPLC",
					"XFRM",
					"XFRM_RPLC",
					"APND",
					"IsSnapshotOf",
					"signs"
			);

	static List<String> mu_relationships =
			Arrays.asList(
					"UpdateAvailabilityStatus",
					"SubmitAssociation"
			);


	void eval_assocs(IErrorRecorder er, ValidationContext vc) {
		for (OMElement assoc : m.getAssociations()) {
			String type = m.getAssocType(assoc);
			if (type == null)
				continue;
			type = simpleAssocType(type);
			if (type.equals("HasMember")) {
				evalHasMember(er, assoc);
			} else if(relationships.contains(type) || (vc.isMU && mu_relationships.contains(type))) {
				evalRelationship(er, assoc, vc);
			} else {
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA060");
				String detail = "Association type found: '" + type + "'";
				er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
			}
		}
	}

	String simpleAssocType(String qualifiedType) {
		if (qualifiedType == null)
			return "";
		int i = qualifiedType.lastIndexOf(':');
		if (i == -1)
			return qualifiedType;
		try {
			return qualifiedType.substring(i+1);
		} catch (Exception e) {
			return qualifiedType;
		}
	}

	void cannotValidate(IErrorRecorder er, String context) {
		Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA061");
		er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, context, "");
	}

	void has_single_ss(IErrorRecorder er, ValidationContext vc) {
		List<OMElement> ssEles = m.getSubmissionSets();
		if (ssEles.size() == 0) {
			Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA062");
			er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", "");
		} else if (ssEles.size() > 1) {
			List<String> doc = new ArrayList<String>();
			for (String ssid : m.getSubmissionSetIds())
				doc.add(objectDescription(ssid));
			Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA063");
			String detail = "SubmissionSets found: " + doc;
			er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
		} else
			er.detail(ssDescription(ssEles.get(0)) + ": SubmissionSet found");
	}

	void symbolic_refs_not_in_submission(IErrorRecorder er) {
		List<OMElement> assocs = m.getAssociations();

		for (int i=0; i<assocs.size(); i++) {
			OMElement assoc = (OMElement) assocs.get(i);
			String target = assoc.getAttributeValue(MetadataSupport.target_object_qname);
			String type = assoc.getAttributeValue(MetadataSupport.association_type_qname);
			String source = assoc.getAttributeValue(MetadataSupport.source_object_qname);

			if (target == null || source == null || type == null)
				continue;

			if (!isUUID(source) && !submissionContains(source)) {
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA064");
				String detail = objectDescription(assoc) + ": sourceObject has value " + source +
						" which is not in the submission but cannot be in registry since it is not in UUID format";
				er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
			}

			if (!isUUID(target) && !submissionContains(target)) {
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA065");
				String detail = objectDescription(assoc) + ": targetObject has value " + target +
						" which is not in the submission but cannot be in registry since it is not in UUID format";
				er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
			}
		}

	}


	void sss_relates_to_ss(IErrorRecorder er, ValidationContext vc) {
		String ss_id = m.getSubmissionSetId();
		List<OMElement> assocs = m.getAssociations();

		for (int i=0; i<assocs.size(); i++) {
			OMElement assoc = (OMElement) assocs.get(i);
			String a_target = assoc.getAttributeValue(MetadataSupport.target_object_qname);
			String a_type = assoc.getAttributeValue(MetadataSupport.association_type_qname);
			String a_source = assoc.getAttributeValue(MetadataSupport.source_object_qname);
			if (a_target == null) {
				cannotValidate(er, "Association(" + assoc.getAttributeValue(MetadataSupport.id_qname) + ") - targetObject");
				return;
			}
			if (a_source == null) {
				cannotValidate(er, "Association(" + assoc.getAttributeValue(MetadataSupport.id_qname) + ") - sourceObject");
				return;
			}
			if (a_type == null) {
				cannotValidate(er, "Association(" + assoc.getAttributeValue(MetadataSupport.id_qname) + ") - associationType");
				return;
			}

			boolean target_is_included_is_doc = m.getExtrinsicObjectIds().contains(a_target);

			if (a_source.equals(ss_id)) {
				String hm = assoc_type("HasMember");
				if ( !a_type.equals(hm)) {
					Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA066");
					String detail = "Association referencing SubmissionSet has type " + a_type + " but only type "
							+ assoc_type("HasMember") + " is allowed";
					er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
					hasmember_error = true;
				}
				if (target_is_included_is_doc) {
					if ( ! m.hasSlot(assoc, "SubmissionSetStatus")) {
						Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA067");
						String detail = "Association(" + assoc.getAttributeValue(MetadataSupport.id_qname) +
								") has sourceObject pointing to SubmissionSet and targetObject pointing " +
								"to a DocumentEntry but contains no SubmissionSetStatus Slot";
						er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
					}
				} else if (m.getFolderIds().contains(a_target)) {

				} else {

				}
			}
			else {
				if ( m.hasSlot(assoc, "SubmissionSetStatus") && !"Reference".equals(m.getSlotValue(assoc, "SubmissionSetStatus", 0))) {
					Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA068");
					String detail = "Association " + assoc.getAttributeValue(MetadataSupport.id_qname) +
							" does not have sourceObject pointing to SubmissionSet but contains SubmissionSetStatus Slot with value Original";
					er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
				}
			}
		}
	}

	void ss_doc_fol_must_have_ids(IErrorRecorder er, ValidationContext vc) {
		List<OMElement> docs = m.getExtrinsicObjects();
		List<OMElement> rps = m.getRegistryPackages();

		for (int i=0; i<docs.size(); i++) {
			OMElement doc = (OMElement) docs.get(i);
			String id = doc.getAttributeValue(MetadataSupport.id_qname);
			if (	id == null ||
					id.equals("")
					) {
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA069");
				er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", "");
				return;
			}
		}
		for (int i=0; i<rps.size(); i++) {
			OMElement rp = (OMElement) rps.get(i);
			String id = rp.getAttributeValue(MetadataSupport.id_qname);
			if (	id == null ||
					id.equals("")
					) {
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA070");
				er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", "");
				return;
			}
		}
	}

	void by_value_assoc_in_submission(IErrorRecorder er, ValidationContext vc)  {
		List<OMElement> assocs = m.getAssociations();
		String ss_id = m.getSubmissionSetId();

		for (int i=0; i<assocs.size(); i++) {
			OMElement assoc = (OMElement) assocs.get(i);
			String source = assoc.getAttributeValue(MetadataSupport.source_object_qname);
			String target = assoc.getAttributeValue(MetadataSupport.target_object_qname);
			if (source == null)
				continue;
			if (target == null)
				continue;

			if ( !source.equals(ss_id))
				continue;

			boolean target_is_included_doc = m.getExtrinsicObjectIds().contains(target);

			if (m.getSlot(assoc, "SubmissionSetStatus") == null)
				return;

			String ss_status = m.getSlotValue(assoc, "SubmissionSetStatus", 0);

			if ( target_is_included_doc ) {

				if (ss_status == null || ss_status.equals("")) {
					Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA071");
					er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", "");
				} else if (	ss_status.equals("Original")) {
					if ( !containsObject(target)) {
						Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA072");
						String detail = "TargetObject found: '" + target + "'";
						er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
					}
				} else if (	ss_status.equals("Reference")) {
					if (containsObject(target)) {
						Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA073");
						String detail = "TargetObject found: '" + target + "'";
						er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
					}
				} else {
					Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA074");
					String detail = "SubmissionSetStatus value found: '" + ss_status + "'";
					er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
				}
			} else {
				if (ss_status != null && !ss_status.equals("Reference")) {
					Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA075");
					er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", "");
				}
			}
		}
	}

	boolean containsObject(String id) {
		try {
			if (m.containsObject(id))
				return true;
			return false;
		} catch (Exception e) {
			return false;
		}
	}

//	boolean mustBeInRegistry(String id) {
//		return !containsObject(id) && isUUID(id);
//	}

	void ss_status_single_value(IErrorRecorder er, ValidationContext vc) {
		List<OMElement> assocs = m.getAssociations();
		String ss_id = m.getSubmissionSetId();

		for (int i=0; i<assocs.size(); i++) {
			OMElement assoc = (OMElement) assocs.get(i);
			String source = assoc.getAttributeValue(MetadataSupport.source_object_qname);
			if (source == null)
				continue;

			if ( !source.equals(ss_id))
				continue;

			String ss_status = m.getSlotValue(assoc, "SubmissionSetStatus", 1);
			if (ss_status != null) {
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA076");
				er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", "");
			}
		}
	}

//	void ss_implies_doc_or_fol_or_assoc(ErrorRecorder er, ValidationContext vc) {
//		if (	m.getSubmissionSet() != null &&
//				! (
//						m.getExtrinsicObjects().size() > 0 ||
//						m.getFolders().size() > 0 ||
//						m.getAssociations().size() > 0
//				))
//			er.err("Submission contains a SubmissionSet but no DocumentEntries or Folders or Associations", "ITI TF-3: 4.1.3.1");
//	}


	// does this id represent a folder in this metadata or in registry?
	// ammended to only check current submission since this code is in common
	public boolean isFolder(String id)  {
		if (id == null)
			return false;
		return m.getFolderIds().contains(id);
	}

	// Folder Assocs must be linked to SS by a secondary Assoc
	void folder_assocs(IErrorRecorder er, ValidationContext vc)  {
		String ssId = m.getSubmissionSetId();
		List<OMElement> non_ss_assocs = null;
		for (OMElement a : m.getAssociations()) {
			String sourceId = m.getAssocSource(a);
			if (m.getAssocTarget(a) == ssId) {
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA077");
				er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", "");
			}
			// if sourceId points to a SubmissionSet in this metadata then no further work is needed
			// if sourceId points to a Folder (in or out of this metadata) then secondary Assoc required
			if (sourceId.equals(ssId))
				continue;
			if (isFolder(sourceId)) {
				if (non_ss_assocs == null)
					non_ss_assocs = new ArrayList<OMElement>();
				non_ss_assocs.add(a);
			}
		}
		if (non_ss_assocs == null) return;

		// Show that the non-ss associations are linked to ss via a HasMember association
		// This only applies when the association's sourceObject is a Folder
		for (OMElement a : non_ss_assocs) {
			String aId = a.getAttributeValue(MetadataSupport.id_qname);
			boolean good = false;
			for (OMElement a2 : m.getAssociations()) {
				if (m.getAssocSource(a2).equals(ssId) &&
						m.getAssocTarget(a2).equals(aId) &&
						getSimpleAssocType(a2).equals("HasMember")) {
					if (good) {
						Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA078");
						String detail = "SubmissionSet ID: '" + ssId + "'; Association ID: '" + aId + "'";
						er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
					} else {
						good = true;
					}

				}
			}
			if (good == false) {
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA079");
				String detail = "SubmissionSet ID: '" + ssId + "'; Association: '" + a + "'";
				er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
			}
		}

	}

	String getSimpleAssocType(OMElement a) {
		try {
			return m.getSimpleAssocType(a);
		} catch (Exception e) {
			return "";
		}
	}

	void all_docs_linked_to_ss(IErrorRecorder er, ValidationContext vc) {
		List<OMElement> docs = m.getExtrinsicObjects();

		for (int i=0; i<docs.size(); i++) {
			OMElement doc = (OMElement) docs.get(i);

			OMElement assoc = find_assoc(m.getSubmissionSetId(), assoc_type("HasMember"), doc.getAttributeValue(MetadataSupport.id_qname));

			if ( assoc == null) {
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA080");
				String detail = "DocumentEntry(" + doc.getAttributeValue(MetadataSupport.id_qname) +
						") is not linked to the SubmissionSet with a " + assoc_type("HasMember") + " Association";
				er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
			} else {
				if (!has_sss_slot(assoc)) {
					Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA081");
					String detail = "Association found: " + assocDescription(assoc);
					er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
					hasmember_error = true;
				} else if (!is_sss_original(assoc)) {
					Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA082");
					String detail = "Association found: " + assocDescription(assoc);
					er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
					hasmember_error = true;
				}
			}

		}
	}

	void all_fols_linked_to_ss(IErrorRecorder er, ValidationContext vc) {
		List<OMElement> fols = m.getFolders();

		for (int i=0; i<fols.size(); i++) {
			OMElement fol = (OMElement) fols.get(i);

			if ( !has_assoc(m.getSubmissionSetId(), assoc_type("HasMember"), m.getId(fol))) {
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA083");
				String detail = "Folder(" + fol.getAttributeValue(MetadataSupport.id_qname) +
						") is not linked to the SubmissionSet with a " + assoc_type("HasMember") + " Association";
				er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
				hasmember_error = true;
			}

		}
	}

	String associationSimpleType(String assocType) {
		String[] parts = assocType.split(":");
		if (parts.length < 2)
			return assocType;
		return parts[parts.length-1];
	}

	//	void assocs_have_proper_namespace() {
	//		List<OMElement> assocs = m.getAssociations();
	//
	//		for (OMElement a_ele : assocs) {
	//			String a_type = a_ele.getAttributeValue(MetadataSupport.association_type_qname);
	//			if (m.isVersion2() && a_type.startsWith("urn"))
	//				err("XDS.a does not accept namespace prefix on association type:  found " + a_type);
	//			if ( ! m.isVersion2()) {
	//				String simpleType = associationSimpleType(a_type);
	//				if (Metadata.iheAssocTypes.contains(simpleType)) {
	//					if ( !a_type.startsWith(MetadataSupport.xdsB_ihe_assoc_namespace_uri))
	//						err("XDS.b requires namespace prefix urn:ihe:iti:2007:AssociationType on association type " + simpleType )	;
	//				} else {
	//					if ( !a_type.startsWith(MetadataSupport.xdsB_eb_assoc_namespace_uri))
	//						err("XDS.b requires namespace prefix urn:oasis:names:tc:ebxml-regrep:AssociationType on association type " + simpleType )	;
	//
	//				}
	//			}
	//		}
	//	}

	void rplced_doc_not_in_submission(IErrorRecorder er, ValidationContext vc)  {
		List<OMElement> assocs = m.getAssociations();

		for (int i=0; i<assocs.size(); i++) {
			OMElement assoc = (OMElement) assocs.get(i);
			String id = assoc.getAttributeValue(MetadataSupport.target_object_qname);
			String type = assoc.getAttributeValue(MetadataSupport.association_type_qname);
			if (MetadataSupport.relationship_associations.contains(type) && ! isReferencedObject(id)) {
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA084");
				String detail = "The following objects were found in the submission:\n" +  getIdsOfReferencedObjects().toString() + "\nAuthorized Associations of relationship style: " + MetadataSupport.relationship_associations;
				er.err(XdsErrorCode.Code.XDSRegistryMetadataError, assertion, this, "", detail);
			}
		}
	}

	List<String> getIdsOfReferencedObjects() {
		try {
			return m.getIdsOfReferencedObjects();
		} catch (Exception e) {
			return new ArrayList<String>();
		}
	}

	boolean isReferencedObject(String id) {
		try {
			return m.isReferencedObject(id);
		} catch (Exception e) {
			return false;
		}
	}

	boolean is_sss_original(OMElement assoc) {
		OMElement sss = get_sss_slot(assoc);
		if (sss == null)
			return false;
		String value = m.getSlotValue(assoc, "SubmissionSetStatus", 0);
		if (value == null)
			return false;
		if (value.equals("Original"))
			return true;
		return false;
	}

	boolean is_sss_reference(OMElement assoc) {
		OMElement sss = get_sss_slot(assoc);
		if (sss == null)
			return false;
		String value = m.getSlotValue(assoc, "SubmissionSetStatus", 0);
		if (value == null)
			return false;
		if (value.equals("Reference"))
			return true;
		return false;
	}

	boolean has_sss_slot(OMElement assoc) {
		return get_sss_slot(assoc) != null;
	}

	OMElement get_sss_slot(OMElement assoc) {
		return m.getSlot(assoc, "SubmissionSetStatus");
	}

	OMElement find_ss_hasmember_assoc(String target) {
		return find_assoc(m.getSubmissionSetId(), "HasMember", target);
	}

	boolean has_ss_hasmember_assoc(String target) {
		if (find_ss_hasmember_assoc(target) == null)
			return false;
		return true;
	}

	boolean has_assoc(String source, String type, String target) {
		if (find_assoc(source, type, target) == null)
			return false;
		return true;
	}

	OMElement find_assoc(String source, String type, String target) {

		if (source == null || type == null || target == null)
			return null;

		List<OMElement> assocs = m.getAssociations();

		type = simpleAssocType(type);

		for (int i=0; i<assocs.size(); i++) {
			OMElement assoc = (OMElement) assocs.get(i);
			String a_target = m.getAssocTarget(assoc);
			String a_type = simpleAssocType(m.getAssocType(assoc));
			String a_source = m.getAssocSource(assoc);

			if (source.equals(a_source) &&
					target.equals(a_target) &&
					type.equals(a_type))
				return assoc;

		}
		return null;
	}

	String assoc_type(String type) {
		if (m.isVersion2())
			return type;
		if (type.equals("HasMember"))
			return "urn:oasis:names:tc:ebxml-regrep:AssociationType:" + type;
		if (type.equals("RPLC") ||
				type.equals("XFRM") ||
				type.equals("APND") ||
				type.equals("XFRM_RPLC") ||
				type.equals("signs"))
			return "urn:ihe:iti:2007:AssociationType:" + type;
		return "";
	}

	//	void err(String msg) {
	//		rel.add_error(MetadataSupport.XDSRegistryMetadataError, msg, "Structure.java", null);
	//	}
}
