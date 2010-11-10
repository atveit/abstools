package abs.frontend.parser;

import static org.junit.Assert.*;

import org.junit.Test;

import abs.frontend.FrontendTest;
import abs.frontend.ast.ClassDecl;
import abs.frontend.ast.Decl;
import abs.frontend.ast.MethodImpl;
import abs.frontend.ast.Model;

public class RecoverTest extends FrontendTest {

    @Test
    public void importTest() {
        Model m = assertParseError(" x class C { }");
        assertContainsDeclWithName(m,"C");
    }

    @Test
    public void intfTest() {
        Model m = assertParseError(" interface I { class C { }");
        assertContainsDeclWithName(m,"I");
    }

    @Test
    public void intfTest2() {
        Model m = assertParseError(" interface I class C { }");
        assertContainsDeclWithName(m,"I");
    }

    @Test
    public void intfTest3() {
        Model m = assertParseError(" interface I extends J,K class C { }");
        assertContainsDeclWithName(m,"I");
    }

    @Test
    public void intfTest4() {
        Model m = assertParseError(" interface I { I m() } class C { }");
        assertContainsDeclWithName(m,"I");
    }
    
    @Test
    public void classDeclTest() {
        Model m = assertParseError(" class C { class D { }");
        assertContainsDeclWithName(m,"C");
    }

    @Test
    public void classDeclTest2() {
        Model m = assertParseError(" class C implements I { class D { }");
        assertContainsDeclWithName(m,"C");
    }
    
    @Test
    public void classDeclTest3() {
        Model m = assertParseError(" class C implements I { XXX asd statasd asdasd }");
        assertContainsDeclWithName(m,"C");
    }

    @Test
    public void methodTest3() {
        Model m = assertParseError(" class C { Unit m() { asdasd } }");
        assertContainsMethodWithName(m,"m");
    }
    
    private void assertContainsMethodWithName(Model m, String name) {
        boolean found = false;
        for (MethodImpl mi : ((ClassDecl) m.getCompilationUnit(0).getModuleDecl(0).getDecl(0)).getMethods()) {
            if (mi.getMethodSig().getName().equals(name)) {
                found = true;
            }
        }
        assertTrue("Did not found method with name "+name,found);
    }

    private void assertContainsDeclWithName(Model m, String name) {
        boolean found = false;
        for (Decl d : m.getCompilationUnit(0).getModuleDecl(0).getDecls()) {
            if (d.getName().equals(name)) {
                found = true;
            }
        }
        assertTrue("Did not found decl with name "+name,found);
    }
}