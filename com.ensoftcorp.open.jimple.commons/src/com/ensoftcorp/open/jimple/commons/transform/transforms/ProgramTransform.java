package com.ensoftcorp.open.jimple.commons.transform.transforms;

import soot.SceneTransformer;
import soot.Transform;

public abstract class ProgramTransform extends SceneTransformer {

	private String phaseName;
	
	public ProgramTransform(String phaseName){
		if(phaseName == null){
			throw new IllegalArgumentException("Phase name cannot be null");
		}
		this.phaseName = phaseName;
	}
	
	/**
	 * Returns this transformations phase name
	 * @return
	 */
	public String getPhaseName(){
		return phaseName;
	}
	
	/**
	 * Returns a soot Transform for this transformation
	 * @return
	 */
	public Transform getTransform(){
		return new Transform(("jtp." + phaseName), this);
	}
	
}
