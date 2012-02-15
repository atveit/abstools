/**
 * Copyright (c) 2009-2011, The HATS Consortium. All rights reserved. 
 * This file is licensed under the terms of the Modified BSD License.
 */
package abs.backend.prettyprint;

import java.io.PrintWriter;


public class DefaultABSFormatter implements ABSFormatter {

    private PrintWriter w;
    private static final String INDENT = "  ";
    private StringBuilder indentation = new StringBuilder();
    
    @Override
    public void setPrintWriter(PrintWriter w) {
        this.w = w;
    }

    @Override
    public void beforeOpenBrace() {
    }

    @Override
    public void afterOpenBrace() {
        indentation.append(INDENT);
    }

    @Override
    public void afterStmt() {
        w.print(indentation.toString());
    }

    @Override
    public void beforeCloseBrace() {
        indentation.deleteCharAt(indentation.length()-1);
        indentation.deleteCharAt(indentation.length()-1);
    }

    @Override
    public void afterCloseBrace() {
    }

}
