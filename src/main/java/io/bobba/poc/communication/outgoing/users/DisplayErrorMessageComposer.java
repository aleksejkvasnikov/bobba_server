package io.bobba.poc.communication.outgoing.users;

import io.bobba.poc.communication.protocol.ServerMessage;
import io.bobba.poc.communication.protocol.ServerOpCodes;

public class DisplayErrorMessageComposer extends ServerMessage {
	public DisplayErrorMessageComposer(String message) {
		super(ServerOpCodes.DISPLAY_ERROR_MESSAGE);
		appendString(message);
	}
}
