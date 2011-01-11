package abs.frontend.analyser;

import java.util.ArrayList;
import java.util.Collection;

import abs.frontend.ast.Annotation;
import abs.frontend.ast.DataConstructorExp;
import abs.frontend.ast.List;


public final class AnnotationHelper {
    private AnnotationHelper() {};
    
    public static java.util.List<Annotation> getAnnotationsOfType(List<Annotation> annos, String qualifiedName) {
        ArrayList<Annotation> res = new ArrayList<Annotation>();
        for (Annotation a : annos) {
            if (a.getType().getQualifiedName().equals(qualifiedName)) {
                DataConstructorExp de = (DataConstructorExp) a.getValue();
                res.add(a);
            }
        }
        return res;
    }
}