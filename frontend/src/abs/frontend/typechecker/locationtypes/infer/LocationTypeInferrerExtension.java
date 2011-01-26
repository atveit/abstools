package abs.frontend.typechecker.locationtypes.infer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import abs.frontend.analyser.SemanticErrorList;
import abs.frontend.ast.ASTNode;
import abs.frontend.ast.AsyncCall;
import abs.frontend.ast.Block;
import abs.frontend.ast.Call;
import abs.frontend.ast.ClassDecl;
import abs.frontend.ast.Decl;
import abs.frontend.ast.MainBlock;
import abs.frontend.ast.Model;
import abs.frontend.ast.NewExp;
import abs.frontend.ast.SyncCall;
import abs.frontend.ast.ThisExp;
import abs.frontend.typechecker.Type;
import abs.frontend.typechecker.ext.DefaultTypeSystemExtension;
import abs.frontend.typechecker.ext.TypeSystemExtension;
import abs.frontend.typechecker.locationtypes.LocationType;
import abs.frontend.typechecker.locationtypes.LocationTypeExtension;

public class LocationTypeInferrerExtension extends DefaultTypeSystemExtension {
    public static enum LocationTypingPrecision {
        BASIC,
        METHOD_LOCAL_FAR,
        CLASS_LOCAL_FAR,
        COMPILATION_UNIT_LOCAL_FAR,
        MODULE_LOCAL_FAR,
        GLOBAL_FAR
    }

    private LocationTypingPrecision precision = LocationTypingPrecision.CLASS_LOCAL_FAR;
    
    public void setLocationTypingPrecision(LocationTypingPrecision p) {
        precision = p;
    }
    
    
    private Map<LocationTypeVariable, LocationType> results;
    private LocationType defaultType = LocationType.INFER;
    
    private Map<ASTNode<?>, java.util.List<LocationType>> farTypes = new HashMap<ASTNode<?>, java.util.List<LocationType>>();
    
    public LocationTypeInferrerExtension(Model m) {
        super(m);
    }
    
    public void setDefaultType(LocationType type) {
        defaultType = type;
    }

    private Set<Constraint> constraints = new HashSet<Constraint>();
    //private List<LocationType> globalFarTypes = new ArrayList<LocationType>();
    boolean enablesStats;;
    
    public Set<Constraint> getConstraints() {
        return constraints;
    }
    
    public Map<LocationTypeVariable, LocationType> getResults() {
        return results;
    }
    
    public static LocationTypeVariable getLV(Type t) {
        LocationTypeVariable ltv = (LocationTypeVariable) t.getMetaData(LocationTypeVariable.VAR_KEY); 
        return ltv;
    }
    
    private LocationTypeVariable adaptTo(LocationTypeVariable expLocType, LocationTypeVariable adaptTo, ASTNode<?> typeNode, ASTNode<?> originatingNode) {
        LocationTypeVariable tv = LocationTypeVariable.newVar(constraints, typeNode, false, getFarTypes(originatingNode), null);
        constraints.add(Constraint.adaptConstraint(tv, expLocType, adaptTo));
        //System.out.println("Require " + tv + " = " + expLocType + " |>" + adaptTo);
        return tv;
    }
    
    
    private void adaptAndSet(Type rht, LocationTypeVariable adaptTo, ASTNode<?> originatingNode) {
        if (adaptTo != LocationTypeVariable.ALWAYS_NEAR) { // Optimization
            LocationTypeVariable rhtlv = getLV(rht);
            LocationTypeVariable tv = adaptTo(rhtlv, adaptTo, null, originatingNode);
            annotateVar(rht, tv);
        }
    }
    
    private void annotateVar(Type t, LocationTypeVariable tv) {
        t.addMetaData(LocationTypeVariable.VAR_KEY,tv);
    }
    
    private LocationTypeVariable addNewVar(Type t, ASTNode<?> originatingNode, ASTNode<?> typeNode) {
        LocationTypeExtension.getLocationTypeFromAnnotations(t); // check consistency of annotations
        LocationTypeVariable ltv = getLV(t);
        if (ltv != null) 
            return ltv;
        LocationType lt = getLocationTypeOrDefault(t);
        LocationTypeVariable tv;
        if (lt.isInfer()) {
            tv = LocationTypeVariable.newVar(constraints, typeNode, true, getFarTypes(originatingNode), LocationTypeExtension.getLocationTypeFromAnnotations(t));
        } else if (lt.isFar() && precision != LocationTypingPrecision.BASIC){
            tv = LocationTypeVariable.newVar(constraints, typeNode, true, getFarTypes(originatingNode), LocationTypeExtension.getLocationTypeFromAnnotations(t));
            @SuppressWarnings("unchecked")
            MultiListIterable<LocationType> fars = new MultiListIterable<LocationType>(Arrays.asList(LocationType.FAR), getFarTypes(originatingNode));
            constraints.add(Constraint.constConstraint(tv, fars , Constraint.MUST_HAVE));
        } else {
            tv = LocationTypeVariable.getFromLocationType(lt);
        } 
        annotateVar(t, tv);
        return tv;
    }

    private java.util.List<LocationType> getFarTypes(ASTNode<?> originatingNode) {
        ASTNode<?> node = null;
        String prefix = "";
        if (precision == LocationTypingPrecision.GLOBAL_FAR) {
            node = originatingNode.getCompilationUnit().getModel();
            prefix = "G";
        }
        if (precision == LocationTypingPrecision.COMPILATION_UNIT_LOCAL_FAR) {
            node = originatingNode.getCompilationUnit();
            prefix = "U";
        }
        if (precision == LocationTypingPrecision.MODULE_LOCAL_FAR) {
            node = originatingNode.getModuleDecl();
            prefix = "M";
        }
        if (precision == LocationTypingPrecision.CLASS_LOCAL_FAR) {
            Decl d = originatingNode.getContextDecl();
            if (d != null && d instanceof ClassDecl) {
                node = d;
                prefix = "C";
            }
            Block b = originatingNode.getContextBlock();
            if (b != null && b instanceof MainBlock) {
                node = b;
                prefix = "C";
            }
        }
        if (precision == LocationTypingPrecision.METHOD_LOCAL_FAR) {
            Block b = originatingNode.getContextBlock();
            if (b != null) {
                node = b;
                prefix = "M";
            } 
        }
        if (node == null) {
            return Collections.emptyList();
        }
        if (farTypes.containsKey(node)) {
            return farTypes.get(node);
        } else {
            java.util.List<LocationType> result = new ArrayList<LocationType>();
            for (int i = 0; i < node.getNumberOfNewCogExpr(); i++) {
                result.add(LocationType.createParametricFar(prefix + i));
            }
            farTypes.put(node, result);
            return result;
        }
    }
    
    private LocationType getLocationTypeOrDefault(Type t) {
        LocationType lt = LocationTypeExtension.getLocationTypeFromAnnotations(t);
        if (lt == null) {
            return defaultType;
        }
        return lt;
    }

    @Override
    public void checkAssignable(Type adaptTo, Type rht, Type lht, ASTNode<?> n) {
        LocationTypeVariable sub = getLV(rht);
        LocationTypeVariable tv = getLV(lht);
        if (n instanceof NewExp && ((NewExp)n).hasCog()) {
            constraints.add(Constraint.farConstraint(sub, tv));
        } else {
            if (adaptTo != null && getLV(adaptTo) != LocationTypeVariable.ALWAYS_NEAR) { // Optimization
                sub = adaptTo(getLV(rht), getLV(adaptTo), null, n);
            }
            constraints.add(Constraint.subConstraint(sub, tv));
        } 
    }

    @Override
    public void annotateType(Type t, ASTNode<?> originatingNode, ASTNode<?> typeNode) {
        if (originatingNode instanceof SyncCall) {
        } else
        if (originatingNode instanceof AsyncCall) {
            AsyncCall call = (AsyncCall)originatingNode;
            adaptAndSet(t, getLV(call.getCallee().getType()), originatingNode);
        } else
        if (originatingNode instanceof ThisExp) {
            annotateVar(t, LocationTypeVariable.ALWAYS_NEAR);
        } else
        if (originatingNode instanceof NewExp) {
            NewExp newExp = (NewExp)originatingNode;
            LocationTypeVariable ltv;
            if (newExp.hasCog()) {
                //ltv = LocationTypeVariable.ALWAYS_FAR;
                ltv = LocationTypeVariable.newVar(constraints, typeNode, false, getFarTypes(originatingNode), null);
                @SuppressWarnings("unchecked")
                MultiListIterable<LocationType> mi = new MultiListIterable<LocationType>(Arrays.asList(LocationType.FAR), getFarTypes(originatingNode));
                constraints.add(Constraint.constConstraint(ltv, mi, Constraint.MUST_HAVE));
            } else {
                ltv = LocationTypeVariable.ALWAYS_NEAR;
            }
            annotateVar(t, ltv);
        } else {
            if (t.isReferenceType()) {
                addNewVar(t, originatingNode, typeNode);
            } 
            if (t.isNullType()) {
                annotateVar(t, LocationTypeVariable.ALWAYS_BOTTOM);
            }
        }
    }

    @Override
    public void checkMethodCall(Call call) {
        LocationTypeVariable lv = getLV(call.getCallee().getType());
        if (call instanceof SyncCall) {
            //checkEq(lv, LocationTypeVariable.ALWAYS_NEAR, Constraint.SHOULD_HAVE);
            constraints.add(Constraint.constConstraint(lv, LocationType.NEAR, Constraint.SHOULD_HAVE));
        } else {
        //checkNeq(getLV(call.getCallee().getType()), LocationTypeVariable.ALWAYS_BOTTOM, Constraint.SHOULD_HAVE);
            //@SuppressWarnings("unchecked")
            //Iterable<LocationType> lts = new MultiListIterable<LocationType>(Arrays.asList(LocationType.ALLCONCRETEUSERTYPES), );
            constraints.add(Constraint.constConstraint(lv, lv.allCTypes, Constraint.SHOULD_HAVE));
        }
    }

    private void checkEq(LocationTypeVariable ltv1, LocationTypeVariable ltv2, Integer importance) {
        constraints.add(Constraint.eqConstraint(ltv1, ltv2, importance));
    }

    @Override
    public void checkEq(Type t1, Type t2, ASTNode<?> node) {
        checkEq(getLV(t1), getLV(t2), Constraint.SHOULD_HAVE);
    }
    
    @Override
    public void finished() {
        if (enablesStats) {
            for (int i = 0; i < 4; i++) {
                SatGenerator satGen = new SatGenerator(constraints);
                //satGen.enableStats = enablesStats;
                results = satGen.generate(errors);
            }
        }
            
        SatGenerator satGen = new SatGenerator(constraints);
        satGen.enableStats = enablesStats;
        results = satGen.generate(errors);
        if (errors.isEmpty()) {
            SemanticErrorList sel = new SemanticErrorList();
            List<TypeSystemExtension> curr = model.getTypeExt().getTypeSystemExtensionList();
            model.getTypeExt().clearTypeSystemExtensions();
            model.getTypeExt().register(new LocationTypeExtension(model, this));
            model.typeCheck(sel);
            errors.addAll(sel);
            model.getTypeExt().clearTypeSystemExtensions();
            model.getTypeExt().registerAll(curr);
        }
    }

    public void enableStatistics() {
        enablesStats = true;
    }

    public LocationType getDefaultType() {
        return defaultType;
    }
}
