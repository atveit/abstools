/**
 * Copyright (c) 2009-2011, The HATS Consortium. All rights reserved. 
 * This file is licensed under the terms of the Modified BSD License.
 */
package abs.backend.maude;

import abs.frontend.ast.Annotation;
import abs.frontend.ast.DataTypeUse;
import abs.frontend.ast.List;
import abs.frontend.ast.PureExp;
import abs.frontend.ast.TypedAnnotation;

import java.io.PrintStream;

public class MaudeCompilerHelper {
    public static PureExp getAnnotationValue(List<Annotation> annotations, String annotationName) {
        for (Annotation a : annotations) {
            if (a instanceof TypedAnnotation) {
                TypedAnnotation ta = (TypedAnnotation)a;
                if (((DataTypeUse)ta.getAccess()).getName().equals(annotationName))
                    return ta.getValue();
            }
        }
        return null;
    }

    public static void emitPushRandomInst(PrintStream stream, int count) {
        for (int x = 0; x < count; x++) {
            stream.print("$pushrandom ; ");
        }
    }
    public static void emitPopRandomInst(PrintStream stream, int count) {
        for (int x = 0; x < count; x++) {
            stream.print("; $poprandom ");
        }
    }
}
