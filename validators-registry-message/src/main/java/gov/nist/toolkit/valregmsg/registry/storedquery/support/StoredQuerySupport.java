package gov.nist.toolkit.valregmsg.registry.storedquery.support;

import gov.nist.toolkit.errorrecording.IErrorRecorder;
import gov.nist.toolkit.errorrecording.common.XdsErrorCode;
import gov.nist.toolkit.errorrecording.xml.assertions.Assertion;
import gov.nist.toolkit.errorrecording.xml.assertions.AssertionLibrary;
import gov.nist.toolkit.registrysupport.logging.LogMessage;
import gov.nist.toolkit.valregmsg.registry.SQCodeAnd;
import gov.nist.toolkit.valregmsg.registry.SQCodedTerm;
import gov.nist.toolkit.valregmsg.registry.storedquery.generic.QueryReturnType;

import java.util.ArrayList;

public class StoredQuerySupport {
	public IErrorRecorder er;
	public LogMessage log_message;
	public SqParams params;
	public StringBuffer query;
	public QueryReturnType returnType;
	private QueryReturnType original_query_type;  // storage to allow temporary settings of return_leaf_class
	public boolean has_validation_errors = false;
	public boolean has_alternate_validation_errors = false;
	public boolean is_secure;
	public boolean runEndProcessing = true;
	private AssertionLibrary ASSERTIONLIBRARY = AssertionLibrary.getInstance();


	public void noEndProcessing() {
		runEndProcessing = false;
	}

	/**
	 * Constructor
	 * @param response
	 * @param log_message
	 */
	public StoredQuerySupport(IErrorRecorder response, LogMessage log_message)  {
		this.er = response;
		this.log_message = log_message;
		init();
	}

	/**
	 * Constructor
	 * @param params (SqParams)
	 * @param return_objects (boolean true = LeafClass)
	 * @param response (Response class)
	 * @param log_message (Message)
	 * @param is_secure
	 */
	public StoredQuerySupport(SqParams params, QueryReturnType return_objects, IErrorRecorder response, LogMessage log_message, boolean is_secure) {
		this.er = response;
		this.log_message = log_message;
		this.params = params;
		this.is_secure = is_secure;
		this.returnType = return_objects;
		init();
	}

	public boolean isLeafClass() {
		return returnType == QueryReturnType.LEAFCLASS;
	}

	public boolean isLeafClassWithDocument() {
		return returnType == QueryReturnType.LEAFCLASSWITHDOCUMENT;
	}

	public boolean isObjectRef() {
		return returnType == QueryReturnType.OBJECTREF;
	}

	void init() {
		query = new StringBuffer();
		has_validation_errors = false;
	}

	public void forceLeafClassQueryType() {
		original_query_type = returnType;
		returnType = QueryReturnType.LEAFCLASS;
	}

	public void forceObjectRefQueryType() {
		original_query_type = returnType;
		returnType = QueryReturnType.OBJECTREF;
	}

	public void restoreOriginalQueryType() {
		returnType = original_query_type;

	}

	boolean isAlternativePresent(String[] alternatives) {
		if (alternatives == null)
			return false;
		for (String alternative : alternatives) {
			Object value = params.getParm(alternative);
			if (value != null)
				return true;
		}
		return false;
	}

	String valuesAsString(String mainName, String...alternatives) {
		StringBuffer buf = new StringBuffer();
		buf.append("[");

		if (mainName != null)
			buf.append(mainName);

		if (alternatives != null)
			for (int i=0; i<alternatives.length; i++)
				buf.append(" ").append(alternatives[i]);

		buf.append("]");

		return buf.toString();
	}

	// general
	public void validate_parm(String name, boolean required, boolean multiple, boolean is_string, boolean is_code, boolean and_or_ok, String... alternatives) {
		Object value = params.getParm(name);

//		System.out.println("validate_parm: name=" + name + " value=" + value + " required=" + required + " multiple=" + multiple + " is_string=" + is_string + " is_code=" + is_code + " alternatives=" + valuesAsString(null, alternatives));

		if (value == null && alternatives == null) {
			if (required ) {
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA155");
				String detail = "Missing parameter: '" + name + "'";
				er.err(XdsErrorCode.Code.XDSRegistryError, assertion, this, "StoredQuery.java", detail, log_message);
				this.has_validation_errors = true;
				return;
			}
			return;
		}

		if (value == null && alternatives != null) {
			System.out.println("looking for alternatives");
			if (! isAlternativePresent(alternatives)) {
				if ( ! has_alternate_validation_errors) {
					Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA162");
					String detail = "One of these parameters must be present in the query: '" + valuesAsString(name, alternatives) + "'";
					er.err(XdsErrorCode.Code.XDSRegistryError, assertion, this, "StoredQuery.java", detail, log_message);
					has_alternate_validation_errors = true;  // keeps from generating multiples of this message
				}
				has_validation_errors = true;
				return;
			}
		}

		if (value == null)
			return;

		if (is_code) {
			if ( !(value instanceof SQCodedTerm)) {
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA163");
				String detail = "Parameter found: '" + name + "'";
				er.err(XdsErrorCode.Code.XDSRegistryError, assertion, this, "StoredQuery.java", detail, log_message);
				this.has_validation_errors = true;
				return;
			}

			if ( (value instanceof SQCodeAnd) && !and_or_ok) {
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA164");
				String detail = "Parameter found: '" + name + "'";
				er.err(XdsErrorCode.Code.XDSRegistryError, assertion, this, "StoredQuery.java", detail, log_message);
				this.has_validation_errors = true;
				return;
			}

		} else {

			if (multiple && !(value instanceof ArrayList)) {
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA165");
				String detail = "Parameter found: '" + name + "'";
				er.err(XdsErrorCode.Code.XDSRegistryError, assertion, this, "StoredQuery.java", detail, log_message);
				this.has_validation_errors = true;
				return;
			}
			if (!multiple && (value instanceof ArrayList)) {
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA166");
				String detail = "Parameter found: '" + name + "'";
				er.err(XdsErrorCode.Code.XDSRegistryError, assertion, this, "StoredQuery.java", detail, log_message);
				this.has_validation_errors = true;
				return;
			}
			if (multiple && (value instanceof ArrayList) && ((ArrayList) value).size() == 0) {
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA167");
				String detail = "Parameter found: '" + name + "'";
				er.err(XdsErrorCode.Code.XDSRegistryError, assertion, this, "StoredQuery.java", detail, log_message);
				this.has_validation_errors = true;
				return;
			}

			if ( ! (value instanceof ArrayList) )
				return;

			ArrayList values = (ArrayList) value;

			for (int i=0; i<values.size(); i++) {
				Object a_o = values.get(i);
				if (	is_string &&
						!(a_o instanceof String) &&
						!(     (a_o instanceof ArrayList)   &&
								((ArrayList)a_o).size() > 0    &&
								( ((ArrayList)a_o).get(0) instanceof String)
						)
						) {
					Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA168");
					String detail = "Parameter '" + name + "' is type (" + a_o.getClass().getName() + ") (single quotes missing?)";
					er.err(XdsErrorCode.Code.XDSRegistryError, assertion, this, "StoredQuery.java", detail, log_message);
					this.has_validation_errors = true;
				}
				if (!is_string && !(a_o instanceof Integer)) {
					Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA169");
					String detail = "Parameter '" + name + "' is type (" + a_o.getClass().getName() + ") (single quotes present)";
					er.err(XdsErrorCode.Code.XDSRegistryError, assertion, this, "StoredQuery.java", detail, log_message);
					this.has_validation_errors = true;
				}
			}
		}

	}

	public void validate_parm(String name, boolean required, boolean multiple, boolean is_string, String same_size_as, String... alternatives) {
		Object value = params.getParm(name);

		System.out.println("validate_parm: name=" + name + " value=" + value + " required=" + required + " multiple=" + multiple + " is_string=" + is_string + " same_size_as=" + same_size_as + " alternatives=" + valuesAsString(null, alternatives));

		if (value == null && alternatives == null) {
			if (required ) {
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA170");
				String detail = "Parameter found: '" + name + "'";
				er.err(XdsErrorCode.Code.XDSRegistryError, assertion, this, "StoredQuery.java", detail, log_message);
				this.has_validation_errors = true;
				return;
			}
			return;
		}

		if (value == null && alternatives != null) {
			System.out.println("looking for alternatives");
			if (! isAlternativePresent(alternatives)) {
				if ( ! has_alternate_validation_errors) {
					Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA171");
					String detail = "One of these parameters must be present in the query: " + valuesAsString(name, alternatives);
					er.err(XdsErrorCode.Code.XDSRegistryError, assertion, this, "StoredQuery.java", detail, log_message);
					has_alternate_validation_errors = true;  // keeps from generating multiples of this message
				}
				has_validation_errors = true;
				return;
			}
		}

		if (value == null)
			return;

		if (multiple && !(value instanceof ArrayList)) {
			Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA172");
			String detail = "Parameter found: '" + name + "'";
			er.err(XdsErrorCode.Code.XDSRegistryError, assertion, this, "StoredQuery.java", detail, log_message);
			this.has_validation_errors = true;
			return;
		}
		if (!multiple && (value instanceof ArrayList)) {
			Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA173");
			String detail = "Parameter found: '" + name + "'";
			er.err(XdsErrorCode.Code.XDSRegistryError, assertion, this, "StoredQuery.java", detail, log_message);
			this.has_validation_errors = true;
			return;
		}
		if (multiple && (value instanceof ArrayList) && ((ArrayList) value).size() == 0) {
			Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA174");
			String detail = "Parameter found: '" + name + "'";
			er.err(XdsErrorCode.Code.XDSRegistryError, assertion, this, "StoredQuery.java", detail, log_message);
			this.has_validation_errors = true;
			return;
		}

		if ( ! (value instanceof ArrayList) )
			return;

		ArrayList values = (ArrayList) value;

		for (int i=0; i<values.size(); i++) {
			Object a_o = values.get(i);
			if (	is_string &&
					!(a_o instanceof String) &&
					!(     (a_o instanceof ArrayList)   &&
							((ArrayList)a_o).size() > 0    &&
							( ((ArrayList)a_o).get(0) instanceof String)
					)
					) {
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA175");
				String detail = "Parameter '" + name + "' is type (" + a_o.getClass().getName() + ") (single quotes missing?)";
				er.err(XdsErrorCode.Code.XDSRegistryError, assertion, this, "StoredQuery.java", detail, log_message);
				this.has_validation_errors = true;
			}
			if (!is_string && !(a_o instanceof Integer)){
				Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA176");
				String detail = "Parameter '" + name + "' is type (" + a_o.getClass().getName() + ") (single quotes present)";
				er.err(XdsErrorCode.Code.XDSRegistryError, assertion, this, "StoredQuery.java", detail, log_message);
				this.has_validation_errors = true;
			}
		}

		if (same_size_as == null)
			return;

		Object same_as_value = params.getParm(same_size_as);
		if ( !(same_as_value instanceof ArrayList)) {
			Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA177");
			String detail = "Parameter, " + same_size_as + " must have same number of values as parameter " + name;
			er.err(XdsErrorCode.Code.XDSRegistryError, assertion, this, "StoredQuery.java", detail, log_message);
			this.has_validation_errors = true;
			return;
		}
		ArrayList same_as_values = (ArrayList) same_as_value;

		if ( !(value instanceof ArrayList)) {
			Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA178");
			String detail = "Parameter, " + same_size_as + " must have same number of values as parameter " + name;
			er.err(XdsErrorCode.Code.XDSRegistryError, assertion, this, "StoredQuery.java", detail, log_message);
			this.has_validation_errors = true;
			return;
		}

		if (same_as_values.size() != values.size()) {
			Assertion assertion = ASSERTIONLIBRARY.getAssertion("TA179");
			String detail = "Parameter, " + same_size_as + " must have same number of values as parameter " + name;
			er.err(XdsErrorCode.Code.XDSRegistryError, assertion, this, "StoredQuery.java", detail, log_message);
			this.has_validation_errors = true;
			return;
		}

	}

	public SqParams getParams() {
		return params;
	}

	public void setParams(SqParams params) {
		this.params = params;
	}

	static final String[] patientIdParms = {
			"$XDSDocumentEntryPatientId",
			"$XDSSubmissionSetPatientId",
			"$XDSFolderPatientId",
			"$patientId"
	};

	public boolean hasPatientIdParameter() {
		for (String parm : patientIdParms) {
			if (params.hasParm(parm))
				return true;
		}
		return false;
	}


}
