package org.projectproto.yuscope;

public class NoninBase {
	public String cmdSelectDF() {
		byte[] cmd = new byte[6];
		cmd[0] = (byte) 0x02;
		cmd[1] = (byte) 0x07;
		cmd[2] = (byte) 0x02;
		cmd[3] = (byte) 0x02;
		cmd[4] = (byte) 0x02; // int math
		cmd[5] = (byte) 0x03;
		return new String(cmd);
	}
	
	public byte handleAck(byte[] buffer, int len) {
		//Log.d(TAG, "");
		return buffer[0];
	}
}
