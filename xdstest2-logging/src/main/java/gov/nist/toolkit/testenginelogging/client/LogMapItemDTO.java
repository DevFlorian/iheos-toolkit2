package gov.nist.toolkit.testenginelogging.client;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

public class LogMapItemDTO implements Serializable, IsSerializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -290403612300468696L;
	private String testName;
	private LogFileContentDTO log;

	public LogMapItemDTO() {}
	
	public LogMapItemDTO(String testName, LogFileContentDTO log) {
		this.testName = testName;
		this.log = log;
	}
	
	public String toString() {
		return "[LogMapItemDTO: testId=" + testName + " log=" + log.toString() + "]";
	}

	public LogFileContentDTO getLog() {
		return log;
	}

	public String getTestName() {
		return testName;
	}
}
