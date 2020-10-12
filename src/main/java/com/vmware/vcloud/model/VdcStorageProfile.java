package com.vmware.vcloud.model;

public class VdcStorageProfile {
    private Boolean enabled;
    private Boolean def;
    private Integer limit;
    private String units;
    
	public VdcStorageProfile() {
		super();
		// TODO Auto-generated constructor stub
	}

	public VdcStorageProfile(Boolean enabled, Boolean def, Integer limit, String units) {
		super();
		this.enabled = enabled;
		this.def = def;
		this.limit = limit;
		this.units = units;
	}

	public Boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Boolean isDef() {
		return def;
	}

	public void setDef(Boolean def) {
		this.def = def;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public String getUnits() {
		return units;
	}

	public void setUnits(String units) {
		this.units = units;
	}
    
}
