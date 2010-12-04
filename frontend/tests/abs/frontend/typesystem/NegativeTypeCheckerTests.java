package abs.frontend.typesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import abs.frontend.FrontendTest;
import abs.frontend.analyser.ErrorMessage;
import abs.frontend.ast.Model;

public class NegativeTypeCheckerTests extends FrontendTest {

    // NEGATIVE TESTS

    @Test
    public void testNull() {
        assertTypeErrors("{ Unit u = null; } ");
    }
    
    @Test
    public void varDeclAssign() {
        assertTypeErrors("{ Unit u = True; } ");
        
    }
    
    @Test
    public void testUnresolvableType() {
        assertTypeErrors("{ I i; }");
    }

    @Test
    public void testUnresolvableType2() {
        assertTypeErrors("class C implements I { }");
    }

    @Test
    public void testUnresolvableType3() {
        assertTypeErrors("interface J extends I { }");
    }

    @Test
    public void missingInterface() {
        assertTypeErrors("class C implements I { } { I i; i = new C(); }");
    }

    @Test
    public void parametericDataTypesIllegalAssignment() {
        assertTypeErrors("interface I {} interface J extends I {} data Foo<A> = Bar(A); { J j; Foo<I> f = Bar(j); }");
    }

    @Test
    public void parametericDataTypeNoTypeArgs() {
        assertTypeErrors("data Foo<A> = Bar(A) | Nil; { Foo f = Nil; }");
    }

    @Test
    public void parametericDataTypeNoTypeArgs2() {
        assertTypeErrors("class Test { { Pair p = Pair(5,Pair(4,5)); } }");
    }

    @Test
    public void parametericDataTypeNoTypeArgs3() {
        assertNoTypeErrors("type Euro = Int; type Cent = Int; type Money = Pair<Euro,Cent>;"
                + "def Money createMoney(Euro e, Cent c) = Pair(e,c); ");
    }

    @Test
    public void testClassError() {
        assertTypeErrors("interface I {} interface J{} class C implements J {} { I i; i = new C(); }");
    }

    @Test
    public void testClassTypeUseError() {
        assertTypeErrors("class C {} { C c; c = new C(); }");
    }

    @Test
    public void testClassDuplicateFields() {
        assertTypeErrors("class C { Bool b = True; Int b = 5; } ");
    }

    @Test
    public void testClassDuplicateFields2() {
        assertTypeErrors("class C(Int b) { Bool b = True; } ");
    }

    @Test
    public void testClassFieldAccessWithoutThis() {
        assertNoTypeErrors("class C(Int b) { Int c = b; } ");
    }

    @Test
    public void testClassDuplicateMethods() {
        assertTypeErrors("class C { Unit m() {} Bool m() { return True;} } ");
    }

    @Test
    public void functionDuplicateParams() {
        assertTypeErrors("def Bool f(Bool b, Int b) = True; ");
    }

    @Test
    public void functionNestedErrors() {
        assertTypeErrors("def X f(Bool b) = True; ");
        assertTypeErrors("def Bool f(X b) = True; ");
        assertTypeErrors("def Bool f(Bool b) = Foo(); ");
    }

    @Test
    public void functionWrongReturnType() {
        assertTypeErrors("def Bool f() = 5; ");
    }

    @Test
    public void dataTypeDuplicateConstructors() {
        assertTypeErrors("data Foo = Bar | Bar ;");
    }

    @Test
    public void dataTypeNestedErrors() {
        assertTypeErrors("data Foo = Bar(X) ;");
    }

    @Test
    public void interfaceDuplicateMethods() {
        assertTypeErrors("interface I { Unit m(); Unit m(); }");
    }

    @Test
    public void methodDuplicateParams() {
        assertTypeErrors("interface I { Unit m(Bool b, Int b); }");
    }

    @Test
    public void interfaceWrongExtend() {
        assertTypeErrors("interface I extends Bool {} ");
    }

    @Test
    public void interfaceMethodOverride() {
        assertTypeErrors("interface I { Unit m(); } interface J extends I { Unit m(); }");
    }

    @Test
    public void interfaceNestedError() {
        assertTypeErrors("interface I { X m(); }");
    }

    @Test
    public void interfaceNestedParamError() {
        assertTypeErrors("interface I { Unit m(X x); }");
    }

    @Test
    public void interfaceDupParams() {
        assertTypeErrors("interface I { Unit m(Bool b, Int b); }");
    }

    @Test
    public void methodNotImplemented() {
        assertTypeErrors("interface I { Unit m(); }" + "class C implements I {  } ");
    }

    @Test
    public void methodOverrideWrongReturnType() {
        assertTypeErrors("interface I { Unit m(); }" + "class C implements I { Bool m() { return True; } } ");
    }

    @Test
    public void classMethodWrongNumParams() {
        assertTypeErrors("interface I { Unit m(); } class C implements I { Unit m(Bool b) { } }");
    }

    @Test
    public void classMethodWrongParamType() {
        assertTypeErrors("interface I { Unit m(Bool b); } class C implements I { Unit m(Int i) { } }");
    }

    @Test
    public void classDuplicateFields() {
        assertTypeErrors("class C { Bool f; Int f;}");
    }

    @Test
    public void classFieldError() {
        assertTypeErrors("class C { X f; }");
    }

    @Test
    public void classFieldAccess() {
        assertTypeErrors("class C { Bool f = True; Unit m(Int f) { this.f = 5; } }");
    }
    
    @Test
    public void classInitializerBlockError() {
        assertTypeErrors("class C { { X f; } }");
    }

    @Test
    public void classParamsError() {
        assertTypeErrors("class C(I i) { }");
    }

    @Test
    public void negTestError() {
        Model m = assertParseOkStdLib(" { Bool b = ~5; }");
        assertEquals(ErrorMessage.EXPECTED_TYPE, m.typeCheck().getFirst().msg);
    }

    @Test
    public void plusError() {
        assertTypeErrors("{ Int i = 4 + True; }");
    }

    @Test
    public void getError() {
        assertTypeErrors("{ Bool f = True; f.get; }");
    }

    @Test
    public void orError() {
        assertTypeErrors("{ Bool b = True || 5; }");
    }

    @Test
    public void andError() {
        assertTypeErrors("{ Bool b = 5 && True; }");
    }

    @Test
    public void newError() {
        assertTypeErrors("interface I { } { I i; i = new I(); }");
    }

    @Test
    public void newError2() {
        assertTypeErrors("interface I { } class C(Bool b) implements I { } { I i; i = new C(); }");
    }

    @Test
    public void newError3() {
        assertTypeErrors("interface I { } class C(Bool b) implements I { } { I i; i = new C(5); }");
    }

    @Test
    public void letError() {
        assertTypeErrors("{ Bool b = let (Bool x) = 5 in x; }");
    }

    @Test
    public void caseError() {
        assertTypeErrors("{ Bool x = True; Bool b = case x { True => False; False => 5; }; }");
    }

    @Test
    public void caseErrorNoDataType() {
        assertTypeErrors("interface I { } { I i; Bool b = case i { True => False; False => 5; }; }");
    }

    @Test
    public void caseBoundVarWrongType() {
        assertTypeErrors("def Bool f(Bool x) = let (Int y) = 5 in case x { y => True; };");

    }

    @Test
    public void caseErrorConstructorNotResolvable() {
        assertTypeErrors("{ Bool x = True; Bool b = case x { Foo => False; }; }");
    }

    @Test
    public void caseErrorConstructorWrongArgNum() {
        assertTypeErrors("{ Bool x = True; Bool b = case x { True(5) => False; }; }");
    }

    @Test
    public void caseErrorConstructorWrongArgType() {
        assertTypeErrors("data Foo = Bar(Bool);  { Foo x = Bar(True); Bool b = case x { Bar(5) => False; }; }");
    }

    @Test
    public void caseErrorConstructorExpWrongArgType() {
        assertTypeErrors("data Foo = Bar(Bool); { Foo x = Bar(5); }");
    }

    @Test
    public void fnAppMissingDef() {
        assertTypeErrors("def Bool f() = x(); ");
    }

    @Test
    public void fnAppWrongArgNum() {
        assertTypeErrors("def Bool f() = f(True); ");
    }

    @Test
    public void fnAppWrongArgType() {
        assertTypeErrors("def Bool f(Bool b) = f(5); ");
    }

    @Test
    public void fnTypecheckNoCrash() {
        assertTypeErrors("def List<String> map2list<A>(Map<String,A> map) =" + "case map {" + "EmptyMap => Nil ;"
                + "Insert(Pair(b,_), tail) => Cons(b, map2list(tail)) ;" + "};");
    }

    @Test
    public void methodReturnError() {
        assertTypeErrors("class C { Unit m() { return True; } }");
    }

    @Test
    public void methodMissingReturn() {
        assertTypeErrors("class C { Bool m() {  } }");
    }

    @Test
    public void methodReturnNotLastStmt() {
        assertTypeErrors("class C { Bool m() { if (True) return True; else return False; return True;} }");
    }

    @Test
    public void syncCallWrongTarget() {
        assertTypeErrors("class C { Unit m(Bool b) { b.m();  } }");
    }

    @Test
    public void syncCallNoMethodThis() {
        assertTypeErrors("class C { Unit m() { this.n(); } }");
    }

    @Test
    public void syncCallNoMethodIntf() {
        assertTypeErrors("interface I {} { I i; i.m(); }");
    }

    @Test
    public void syncCallWrongArgNum() {
        assertTypeErrors("interface I { Unit m(); } { I i; i.m(True); }");
    }

    @Test
    public void syncCallWrongArgType() {
        assertTypeErrors("interface I { Unit m(Int i); } { I i; i.m(True); }");
    }

    @Test
    public void testVarDeclInitNoSubtypeError() {
        assertTypeErrors("interface I {} interface J {} { J j; I i = j; }");
    }

    @Test
    public void testVarDeclInitMissingError() {
        assertTypeErrors("{ Bool b; }");
    }

    @Test
    public void testIfNoBool() {
        assertTypeErrors("{ if (5) { } else { } }");
    }

    @Test
    public void testWhileNoBool() {
        assertTypeErrors("{ while (5) { } }");
    }

    @Test
    public void testAwaitNoFut() {
        assertTypeErrors("{ Bool b = True; await b?; }");
    }

    @Test
    public void testAwaitNoBool() {
        assertTypeErrors("{ await 5; }");
    }

    @Test
    public void testAwaitAndError() {
        assertTypeErrors("{ await 5 && True; }");
    }

    @Test
    public void constructorTypeArgsError() {
        assertTypeErrors("data Foo<A> = Bar(A,A); { Foo<A> o = Bar(True,5); }");
    }

    @Test
    public void missingTypArg() {
        assertTypeErrors("def List<A> map2list<A>(Map<A,B> map) = Nil;");
    }

    @Test
    public void returnInMainBlock() {
        assertTypeErrors("{ return Nil; }");
        assertTypeErrors("{ return Unit; return Unit; }");
    }

    @Test
    public void returnInInitBlock() {
        assertTypeErrors("class C { { return Nil; } }");
        assertTypeErrors("class C { { return Unit; return Unit; } }");
    }
    
}