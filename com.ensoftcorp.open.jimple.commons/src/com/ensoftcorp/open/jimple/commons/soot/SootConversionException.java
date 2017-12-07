package com.ensoftcorp.open.jimple.commons.soot;

/**
 * Throwable exception wrapper to make a runtime soot conversion exception checked
 * @author Ben Holland
 */
public class SootConversionException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public SootConversionException(Throwable t) {
		super(t);
	}
	
	public SootConversionException(String message, Throwable t) {
		super(message, t);
	}
}
