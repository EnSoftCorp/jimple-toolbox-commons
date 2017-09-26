package com.ensoftcorp.open.jimple.commons.transform.transforms;

import java.util.Map;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.jimple.commons.log.Log;

import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.util.Chain;

public class PrintSelectedMethodCFGTransform extends MethodCFGTransform {

	private AtlasSet<Node> selection;
	
	public PrintSelectedMethodCFGTransform(Node method, AtlasSet<Node> selection) {
		super("print_selected_cfg_nodes", method);
		this.selection = selection;
	}
	
	private AtlasSet<Node> getSelectedCFGNodes(){
		return Common.toQ(cfgNodes).intersection(Common.toQ(selection)).eval().nodes();
	}

	@Override
	protected void transform(Body methodBody, Map<Unit,Node> atlasCorrespondence) {
		Chain<Unit> methodBodyUnits = methodBody.getUnits();
		AtlasSet<Node> selectedCFGNodes = getSelectedCFGNodes();
		for(Unit unit : methodBodyUnits){
			if(selectedCFGNodes.contains(atlasCorrespondence.get(unit))){
				Log.info(unit.toString());
			}
		}
	}
	
}
