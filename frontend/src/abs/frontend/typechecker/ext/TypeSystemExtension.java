package abs.frontend.typechecker.ext;

import abs.frontend.analyser.SemanticErrorList;
import abs.frontend.ast.ASTNode;
import abs.frontend.ast.Call;
import abs.frontend.typechecker.Type;

public interface TypeSystemExtension {

    void checkAssignable(Type adaptTo, Type rht, Type lht);

    void annotateType(Type t, ASTNode<?> n);

    void checkMethodCall(Call call);

    void checkEq(Type lt, Type t);

    void setSemanticErrorList(SemanticErrorList errors);

    void finished();

}