package org.openhab.binding.edimax.internal.commands;

import java.util.List;

public class SetState extends AbstractCMDCommand<Boolean> {
	
	public SetState(Boolean newValue) {
		super(newValue);
	}
		
	@Override
	protected List<String> getPath() {
		List<String> list = super.getPath();
		list.add("Device.System.Power.State");
		return list;
	}
	
	@Override
	protected String getXPathString() {
		return "/SMARTPLUG/CMD/text()";
	}
	
	@Override
	protected Boolean unmarshal(String result) {
		if ("OK".equals(result)) {
			return Boolean.TRUE;
		}
		return null;
	}
	
	@Override
	protected String marshal(Boolean value) {
		if (Boolean.TRUE.equals(value)) {
			return "ON";
		} else if (Boolean.FALSE.equals(value)) {
			return "OFF";
		} 
		return super.marshal(value);
	}

}
