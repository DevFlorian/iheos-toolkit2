package gov.nist.toolkit.xdstools2.client.widgets;

import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.RadioButton;
import gov.nist.toolkit.xdstools2.client.Panel1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract public class RadioButtonGroup  {
	public String groupName;
	public Panel1 panel;
	public Map<String, RadioButton> buttonMap = new HashMap<String, RadioButton>();
	public List<RadioButton> buttons = new ArrayList<RadioButton>();

	public ValueChangeHandler<Boolean> choiceChangedHandler;

	public RadioButtonGroup(String groupName, Panel1 panel) {
		this.groupName = groupName;
		this.panel = panel;
	}

	public void addButton(String label) {
		RadioButton rb = new RadioButton(groupName, label);
		buttonMap.put(label, rb);
		buttons.add(rb);
		panel.add(rb);
		
		rb.addValueChangeHandler(choiceChangedHandler);
	}

	public void addButtons(List<String> labels) {
		for (String label : labels) {
			addButton(label);
		}
	}
	
	public String getNameForRadioButton(RadioButton r) {
		for (String name : buttonMap.keySet()) {
			RadioButton rb = buttonMap.get(name);
			if (rb == r) {
				return name;
			}
		}
		return null;
	}

}

