package com.vmware.vcloud.model;

import java.util.List;

public class SubnetParticipation {
	private String gateway;
	private String netmask;
	private List<IpRange> ipRanges;
	
	public SubnetParticipation() {
		super();
		// TODO Auto-generated constructor stub
	}

	public SubnetParticipation(String gateway, String netmask, List<IpRange> ipRanges) {
		super();
		this.gateway = gateway;
		this.netmask = netmask;
	}

	public String getGateway() {
		return gateway;
	}

	public void setGateway(String gateway) {
		this.gateway = gateway;
	}

	public String getNetmask() {
		return netmask;
	}

	public void setNetmask(String netmask) {
		this.netmask = netmask;
	}

	public List<IpRange> getIpRanges() {
		return ipRanges;
	}

	public void setIpRanges(List<IpRange> ipRanges) {
		this.ipRanges = ipRanges;
	}
	
}
