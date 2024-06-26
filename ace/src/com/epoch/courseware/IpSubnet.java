/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @author tags. See the COPYRIGHT.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.epoch.courseware;

import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.commons.lang.builder.HashCodeBuilder;

/** This class allows to check if an IP address is contained in a subnet.<BR>
 * Supported Formats for the Subnets are: 1.1.1.1/255.255.255.255 or 1.1.1.1/32 (CIDR-notation)
 * and (InetAddress,Mask) where Mask is a integer for CIDR-notation or a String for Standard Mask notation<BR>
 * <BR><BR>Example1:<BR><span style='font-family=monospace'>
 * IpSubnet ips = new IpSubnet("192.168.1.0/24");<BR>
 * System.out.println("Result: "+ ips.contains("192.168.1.123"));<BR>
 * System.out.println("Result: "+ ips.contains(inetAddress2));<BR></span>
 * <BR>Example1 bis:<BR><span style='font-family=monospace'>
 * IpSubnet ips = new IpSubnet(inetAddress, 24);<BR>
 * where inetAddress is 192.168.1.0 and inetAddress2 is 192.168.1.123<BR></span>
 * <BR><BR>Example2:<BR><span style='font-family=monospace'>
 * IpSubnet ips = new IpSubnet("192.168.1.0/255.255.255.0");<BR>
 * System.out.println("Result: "+ ips.contains("192.168.1.123"));<BR>
 * System.out.println("Result: "+ ips.contains(inetAddress2));<BR></span>
 * <BR>Example2 bis:<BR><span style='font-family=monospace'>
 * IpSubnet ips = new IpSubnet(inetAddress, "255.255.255.0");<BR>
 * where inetAddress is 192.168.1.0 and inetAddress2 is 192.168.1.123<BR></span>

 * @author frederic bregier
 *
 */
public class IpSubnet {
	private static final int SUBNET_MASK = 0x80000000;
	private static final int BYTE_ADDRESS_MASK = 0xFF;

	/** Parent IP address to compare. */
	transient private InetAddress inetAddress;
	/** Subnet of parent IP address. */
	transient private int subnet;
	/** Decimal mask to get fixed bits of the IP address. */
	transient private int mask;
	/** CIDR mask to get fixed bits of the IP address. */
	transient private int cidrMask;

	/** Create IpSubnet using the CIDR or normal notation<BR>
	 * i.e.: 
	 * IpSubnet subnet = new IpSubnet("10.10.10.0/24"); or
	 * IpSubnet subnet = new IpSubnet("10.10.10.0/255.255.255.0");
	 * @param netAddress a network address as string 
	 * @throws UnknownHostException	if there is no host at the address
	 */
	public IpSubnet(String netAddress) throws UnknownHostException {
		setNetAddress(netAddress);
	} // IpSubnet(String)

	/** Create IpSubnet using the CIDR notation.
	 * @param inetAddress	an internet address
	 * @param cidrNetMask	the mask to get the subnet, in CIDR format
	 */
	public IpSubnet(InetAddress inetAddress, int cidrNetMask) {
		setNetAddress(inetAddress, cidrNetMask);
	} // IpSubnet(InetAddress, int)

	/** Create IpSubnet using the normal notation.
	 * @param inetAddress	an internet address
	 * @param netMask	the mask to get the subnet, in decmial format
	 */
	public IpSubnet(InetAddress inetAddress, String netMask) {
		setNetAddress(inetAddress, netMask);
	} // IpSubnet(InetAddress, String)

	// RBG rewritten 1/2011 to take advantage of more modern methods
	/** Sets the network address in either CIDR or decimal notation.<BR>
	 * i.e.: setNetAddress("1.1.1.1/24"); or<BR>
	 * setNetAddress("1.1.1.1/255.255.255.0");<BR>
	 * @param netAddress a network address as string 
	 * @throws UnknownHostException	if there is no host at the address
	 */
	private void setNetAddress(String netAddress) throws UnknownHostException {
		final String[] addrMask = netAddress.split("/");
		setNetId(addrMask[0]);
		if (addrMask.length > 1) {
			if (addrMask[1].indexOf('.') < 0) {
				setCidrNetMask(MathUtils.parseInt(addrMask[1]));
			} else {
				setNetMask(addrMask[1]);
			}
		} // if the address has two parts
	} // setNetAddress(String)

	/** Sets the network address in CIDR notation.
	 * @param inetAddress	an internet address
	 * @param cidrNetMask	the mask to get the subnet, in CIDR format
	 */
	private void setNetAddress(InetAddress inetAddress, int cidrNetMask) {
		setNetId(inetAddress);
		setCidrNetMask(cidrNetMask);
	} // setNetAddress(InetAddress, int)

	/** Sets the network address in decimal notation.
	 * @param inetAddress	an internet address
	 * @param netMask	the mask to get the subnet, in decmial format
	 */
	private void setNetAddress(InetAddress inetAddress, String netMask) {
		setNetId(inetAddress);
		setNetMask(netMask);
	} // setNetAddress(InetAddress, String)

	/** Sets the base address of the subnet.<BR>
	 * i.e.: setNetId("192.168.1.0");
	 * @param netId a network ID
	 * @throws UnknownHostException	if there is no host at the address
	 */
	private void setNetId(String netId) throws UnknownHostException {
		final InetAddress inetAddress = InetAddress.getByName(netId);
		setNetId(inetAddress);
	} // setNetId(String)

	/** Compute integer representation of InetAddress.
	 * @param inetAddress	an internet address
	 * @return the integer representation
	 */
	private int toInt(InetAddress inetAddress) {
		final byte[] address = inetAddress.getAddress();
		int net = 0;
		for (int i = 0; i < address.length; i++) {
			net <<= 8;
			net |= address[i] & BYTE_ADDRESS_MASK;
		}
		return net;
	} // toInt(InetAddress)

	/** Sets the base address of the subnet.
	 * @param inetAddr	an internet address
	 */
	private void setNetId(InetAddress inetAddr) {
		inetAddress = inetAddr;
		subnet = toInt(inetAddress);
	} // setNetId(InetAddress)

	// RBG rewritten 1/2011 to take advantage of more modern methods
	/** Sets the subnet's netmask in decimal format.<BR>
	 * i.e.: setNetMask("255.255.255.0");
	 * @param netMask a network mask in decimal notation
	 */
	private void setNetMask(String netMask) {
		final int[] netmask = 
				Utils.stringToIntArray(netMask.split("\\."));
		int mask = 0;
		for (final int part : netmask) {
			mask += Integer.bitCount(part);
		}
		setCidrNetMask(mask);
	} // setNetMask(String)

	/** Sets the CIDR netmask<BR>
	 * i.e.: setCidrNetMask(24);
	 * @param cidrNetMask a netmask in CIDR notation 
	 */
	private void setCidrNetMask(int cidrNetMask) {
		cidrMask = cidrNetMask;
		mask = SUBNET_MASK >> (cidrMask - 1);
	} // setCidrNetMask(int)

	/** Compares the given IP address against the subnet and returns true if
	 * the ip is in the subnet-ip-range and false if not.
	 * @param ipAddr an ipaddress
	 * @return returns true if the given IP address is inside the currently
	 * set network
	 * @throws UnknownHostException	if there is no host at the address
	 */
	public boolean contains(String ipAddr) throws UnknownHostException {
		return contains(InetAddress.getByName(ipAddr));
	} // contains(String)

	/** Compares the given InetAddress against the subnet and returns true if
	 * the ip is in the subnet-ip-range and false if not.
	 * @param	inetAddress	the given InetAddress
	 * @return returns true if the given IP address is inside the currently
	 * set network
	 */
	public boolean contains(InetAddress inetAddress) {
		return mask == -1 || (toInt(inetAddress) & mask) == subnet;
	} // contains(InetAddress)

	/** Converts the address to a readable string.
	 * @return	the readable string
	 */
	@Override
	public String toString() {
		return inetAddress.getHostAddress() + "/" + cidrMask;
	} // toString()

	/** Determines whether this IP address is equal to the given object.
	 * @param	o	the given object
	 * @return	true if the object is an IP address that equals this one
	 */
	@Override
	public boolean equals(Object o) {
		boolean isEqual = false;
		if (o instanceof IpSubnet) {
			final IpSubnet ipSubnet = (IpSubnet) o;
			isEqual = ipSubnet.subnet == subnet 
					&& ipSubnet.cidrMask == cidrMask;
		} // if instanceof
		return isEqual;
	} // equals(Object)

	/** Creates a hash code describing this object.
	 * @return the hash code
	 */
	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(subnet).append(cidrMask).toHashCode();
	} // hashCode

	/** Simple test functions
	 * @param args 
	 *		  where args[0] is the netmask (standard or CIDR notation) and optional args[1] is
	 *		  the inetAddress to test with this IpSubnet
	 */
	public static void main(String[] args) {
		if (args.length != 0) {
			IpSubnet ipSubnet = null;
			try {
				ipSubnet = new IpSubnet(args[0]);
			} catch (UnknownHostException e) {
				return;
			}
			if (args.length > 1) {
				try {
					System.out.println("Is IN: "+args[1]+" "+ipSubnet.contains(args[1]));
				} catch (UnknownHostException e) {
					;
				}
			}
		}
	} // main(String[])

} // IpSubnet
