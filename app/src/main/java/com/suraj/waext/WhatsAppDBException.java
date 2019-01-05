package com.suraj.waext;

public class WhatsAppDBException extends Exception {
	private String message;

	public WhatsAppDBException(Exception ex){
		super(ex);
		this.message = ex.getMessage();
	}
	public WhatsAppDBException(String message){
		this.message = message;
	}
	@Override
	public String getMessage() {
		return message;
	}
}
